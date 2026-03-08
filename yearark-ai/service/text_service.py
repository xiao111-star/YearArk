"""
文案生成服务
职责：解析 schemaContent，按 slot label/maxLength 调用 LLM 生成文案
"""
import json
import logging
from domain.outline import OutlinePage, AlbumOutline
from core.llm.client import chat

logger = logging.getLogger(__name__)

_PROMPT_TEMPLATE = """你是一个纪念册文案编辑，请根据以下信息生成文字内容。

纪念册标题：{album_title}
当前章节：{chapter_title}
章节描述：{chapter_desc}
用户上传的文字素材（可参考）：{user_texts}

需要生成的文字槽位：
{slots_desc}

请严格按以下 JSON 格式返回，不要输出任何其他内容：
{{{slots_json_keys}}}"""


def _parse_text_slots(schema_content: str | None) -> list[dict]:
    if not schema_content:
        return []
    try:
        slots = json.loads(schema_content).get("slots", [])
        return [s for s in slots if s.get("type") == "text"]
    except Exception:
        return []


class TextService:

    def fill_page_texts(
        self,
        page: OutlinePage,
        album_title: str,
        chapter_title: str,
        chapter_desc: str,
        user_texts: list[str],
    ) -> None:
        """为单个页面生成所有 text slot 内容，结果写入 page.texts"""
        slot_defs = _parse_text_slots(page.schema_content)
        if not slot_defs:
            return

        slots_desc = "\n".join(
            f'- {s["id"]}：{s.get("label", s["id"])}，不超过{s.get("maxLength", 100)}个字'
            for s in slot_defs
        )
        slots_json_keys = ",\n  ".join(f'"{s["id"]}": "生成的内容"' for s in slot_defs)

        prompt = _PROMPT_TEMPLATE.format(
            album_title=album_title,
            chapter_title=chapter_title,
            chapter_desc=chapter_desc,
            user_texts="、".join(user_texts) if user_texts else "无",
            slots_desc=slots_desc,
            slots_json_keys="\n  " + slots_json_keys + "\n",
        )

        try:
            raw = chat([{"role": "user", "content": prompt}])
            start, end = raw.find("{"), raw.rfind("}") + 1
            result = json.loads(raw[start:end])
            # 截断超长内容
            for s in slot_defs:
                sid, max_len = s["id"], s.get("maxLength", 100)
                if sid in result:
                    page.texts[sid] = result[sid][:max_len]
        except Exception as e:
            logger.warning("文案生成失败 templatePageId=%s: %s", page.template_page_id, e)

    def fill_outline_texts(self, outline: AlbumOutline, user_texts: list[str]) -> None:
        """遍历大纲所有页面，批量生成文案"""
        # 封面
        if outline.cover_page:
            self.fill_page_texts(
                outline.cover_page, outline.album_title, "封面", outline.album_title, user_texts
            )
        # 各章节页
        for chapter in outline.chapters:
            for page in chapter.pages:
                self.fill_page_texts(
                    page, outline.album_title, chapter.title, chapter.description, user_texts
                )
        # 纯文字页：直接用用户素材填充，不调 AI
        txt_idx = 0
        for page in outline.text_only_pages:
            slot_defs = _parse_text_slots(page.schema_content)
            for s in slot_defs:
                if txt_idx < len(user_texts):
                    page.texts[s["id"]] = user_texts[txt_idx]
                    txt_idx += 1
