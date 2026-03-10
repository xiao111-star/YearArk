"""
文案生成服务
职责：两轮生成策略
  第一轮：所有内容页并发生成文案
  第二轮：封面页和章节页携带内容页文案，并发生成（确保概括性文案贴合实际内容）
"""
import json
import logging
from concurrent.futures import ThreadPoolExecutor, as_completed
from domain.outline import OutlinePage, AlbumOutline
from core.llm.client import chat

logger = logging.getLogger(__name__)


def _parse_text_slots(schema_content: str | None) -> list[dict]:
    if not schema_content:
        return []
    try:
        slots = json.loads(schema_content).get("slots", [])
        return [s for s in slots if s.get("type") == "text"]
    except Exception:
        return []


def _build_slots_desc(slot_defs: list[dict]) -> str:
    return "\n".join(
        f'- {s["id"]}（{s.get("label", s["id"])}）：不超过{s.get("maxLength", 100)}个字'
        for s in slot_defs
    )


def _build_slots_json_hint(slot_defs: list[dict]) -> str:
    pairs = ",\n  ".join(f'"{s["id"]}": "生成的内容"' for s in slot_defs)
    return "{\n  " + pairs + "\n}"


def _call_and_fill(page: OutlinePage, prompt: str, slot_defs: list[dict]) -> None:
    """调用 LLM 并将结果写入 page.texts"""
    try:
        raw = chat([{"role": "user", "content": prompt}])
        start, end = raw.find("{"), raw.rfind("}") + 1
        result = json.loads(raw[start:end])
        for s in slot_defs:
            sid, max_len = s["id"], s.get("maxLength", 100)
            if sid in result:
                page.texts[sid] = str(result[sid])[:max_len]
    except Exception as e:
        logger.warning("文案生成失败 templatePageId=%s: %s", page.template_page_id, e)


# ── 内容页 prompt ──────────────────────────────────────────────────────
_CONTENT_PROMPT = """角色：你是一位纪念册文案编辑，文风温暖、有画面感。

背景信息：
- 纪念册标题：{album_title}
- 当前章节：{chapter_title}
- 章节描述：{chapter_desc}
- 本页包含 {image_count} 张照片
- 用户提供的文字素材：{user_texts}

任务：为本页的文字槽位生成文案。每个槽位有类型说明和字数限制，请严格遵守。

槽位要求：
{slots_desc}

要求：
- 文案要贴合照片所在章节的主题和情感
- 标题类槽位要简洁有力，描述类槽位要有温度和画面感
- 严格遵守每个槽位的字数限制

只输出 JSON，不要输出任何其他内容：
{slots_json_hint}"""


# ── 章节页 prompt ──────────────────────────────────────────────────────
_CHAPTER_PROMPT = """角色：你是一位纪念册文案编辑，文风温暖、有画面感。

背景信息：
- 纪念册标题：{album_title}
- 当前章节：{chapter_title}
- 章节描述：{chapter_desc}
- 用户提供的文字素材：{user_texts}

本章内容页已生成的文案（供你参考，确保章节页文案能概括这些内容）：
{content_texts_summary}

任务：为本章节页的文字槽位生成文案。章节页是本章的开篇，文案应起到引领和概括作用。

槽位要求：
{slots_desc}

要求：
- 章节标题要能概括本章所有内容页的主题
- 描述性文案要能引出后续内容，有承上启下的感觉
- 严格遵守每个槽位的字数限制

只输出 JSON，不要输出任何其他内容：
{slots_json_hint}"""


# ── 封面页 prompt ──────────────────────────────────────────────────────
_COVER_PROMPT = """角色：你是一位纪念册文案编辑，文风温暖、有画面感。

背景信息：
- 纪念册标题：{album_title}
- 用户提供的文字素材：{user_texts}

整本纪念册各章节的内容摘要：
{all_chapters_summary}

任务：为封面页的文字槽位生成文案。封面是整本纪念册的门面，文案应概括全书主题。

槽位要求：
{slots_desc}

要求：
- 封面标题要有诗意或画面感，能概括整本纪念册
- 副标题或描述要简洁温暖
- 严格遵守每个槽位的字数限制

只输出 JSON，不要输出任何其他内容：
{slots_json_hint}"""


class TextService:

    def fill_outline_texts(self, outline: AlbumOutline, user_texts: list[str]) -> None:
        """两轮生成：先内容页并发，再章节页+封面页并发"""
        user_texts_str = "、".join(user_texts) if user_texts else "无"

        # ── 第一轮：所有内容页并发 ──
        logger.info("[文案-第1轮] 并发生成内容页文案")
        content_tasks = []
        for chapter in outline.chapters:
            for page in chapter.pages:
                if page.page_type == "content":
                    slot_defs = _parse_text_slots(page.schema_content)
                    if not slot_defs:
                        continue
                    prompt = _CONTENT_PROMPT.format(
                        album_title=outline.album_title,
                        chapter_title=chapter.title,
                        chapter_desc=chapter.description,
                        image_count=len(page.images),
                        user_texts=user_texts_str,
                        slots_desc=_build_slots_desc(slot_defs),
                        slots_json_hint=_build_slots_json_hint(slot_defs),
                    )
                    content_tasks.append((page, prompt, slot_defs))

        self._run_parallel(content_tasks)

        # ── 第二轮：章节页 + 封面页并发（携带内容页文案） ──
        logger.info("[文案-第2轮] 并发生成章节页和封面页文案")
        summary_tasks = []

        # 章节页
        for chapter in outline.chapters:
            for page in chapter.pages:
                if page.page_type == "chapter":
                    slot_defs = _parse_text_slots(page.schema_content)
                    if not slot_defs:
                        continue
                    # 收集本章内容页已生成的文案
                    content_summary = self._summarize_chapter_texts(chapter)
                    prompt = _CHAPTER_PROMPT.format(
                        album_title=outline.album_title,
                        chapter_title=chapter.title,
                        chapter_desc=chapter.description,
                        user_texts=user_texts_str,
                        content_texts_summary=content_summary or "暂无",
                        slots_desc=_build_slots_desc(slot_defs),
                        slots_json_hint=_build_slots_json_hint(slot_defs),
                    )
                    summary_tasks.append((page, prompt, slot_defs))

        # 封面页
        if outline.cover_page:
            slot_defs = _parse_text_slots(outline.cover_page.schema_content)
            if slot_defs:
                all_summary = self._summarize_all_chapters(outline)
                prompt = _COVER_PROMPT.format(
                    album_title=outline.album_title,
                    user_texts=user_texts_str,
                    all_chapters_summary=all_summary or "暂无",
                    slots_desc=_build_slots_desc(slot_defs),
                    slots_json_hint=_build_slots_json_hint(slot_defs),
                )
                summary_tasks.append((outline.cover_page, prompt, slot_defs))

        self._run_parallel(summary_tasks)

        # ── 纯文字页：直接用用户素材填充 ──
        txt_idx = 0
        for page in outline.text_only_pages:
            slot_defs = _parse_text_slots(page.schema_content)
            for s in slot_defs:
                if txt_idx < len(user_texts):
                    page.texts[s["id"]] = user_texts[txt_idx]
                    txt_idx += 1

    def _run_parallel(self, tasks: list[tuple]) -> None:
        """并发执行文案生成任务"""
        if not tasks:
            return
        with ThreadPoolExecutor(max_workers=8) as executor:
            futures = {
                executor.submit(_call_and_fill, page, prompt, slot_defs): page
                for page, prompt, slot_defs in tasks
            }
            for future in as_completed(futures):
                future.result()

    def _summarize_chapter_texts(self, chapter) -> str:
        """收集本章内容页已生成的文案，作为章节页的参考"""
        lines = []
        for page in chapter.pages:
            if page.page_type == "content" and page.texts:
                texts = " / ".join(f"{k}: {v}" for k, v in page.texts.items())
                lines.append(f"  - {texts}")
        return "\n".join(lines)

    def _summarize_all_chapters(self, outline: AlbumOutline) -> str:
        """收集所有章节的标题和内容页文案，作为封面页的参考"""
        lines = []
        for chapter in outline.chapters:
            lines.append(f"【{chapter.title}】{chapter.description}")
            for page in chapter.pages:
                if page.page_type == "content" and page.texts:
                    texts = " / ".join(f"{k}: {v}" for k, v in page.texts.items())
                    lines.append(f"  - {texts}")
        return "\n".join(lines)
