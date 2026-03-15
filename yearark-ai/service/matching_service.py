"""
模板匹配服务
职责：将模板页按类型分类，为大纲各位置分配模板
页面序列：封面 → [章节页 → 内容页×N] × 章节数 → 纯文字页

分配策略：
- 内容页按 imageCount 分桶，每桶内轮转使用模板（尽量不重复）
- 每次选桶时用加权随机，权重 = 桶内模板种类数，避免固定的 2-3-4 循环
- 连续两页不选同一个桶（除非只剩一种桶可选）
"""
import logging
import random
from collections import defaultdict
from domain.request import TemplatePageItem
from domain.outline import AlbumOutline, OutlinePage, ImageFeature

logger = logging.getLogger(__name__)

_COVER_KW = ["封面"]
_CHAPTER_KW = ["章节", "章节页"]
_TEXT_KW = ["文字", "纯文字", "文本"]


def _is_type(page: TemplatePageItem, keywords: list[str]) -> bool:
    name = (page.pageTypeName or "").strip()
    return any(kw in name for kw in keywords)


def _to_outline_page(tp: TemplatePageItem, page_type: str, images: list[ImageFeature] = None) -> OutlinePage:
    return OutlinePage(
        template_page_id=tp.templatePageId,
        image_count=tp.imageCount,
        text_count=tp.textCount,
        schema_content=tp.schemaContent,
        page_type=page_type,
        images=images or [],
    )


class _TemplateBucket:
    """同一 imageCount 的模板桶，内部轮转 + shuffle 保证不重复"""

    def __init__(self, templates: list[TemplatePageItem]):
        self.templates = list(templates)
        random.shuffle(self.templates)
        self._idx = 0

    @property
    def variety(self) -> int:
        """桶内模板种类数，用于加权"""
        return len(self.templates)

    def next(self) -> TemplatePageItem:
        tp = self.templates[self._idx % len(self.templates)]
        self._idx += 1
        # 一轮用完后重新 shuffle，避免下一轮顺序相同
        if self._idx % len(self.templates) == 0:
            random.shuffle(self.templates)
        return tp


class MatchingService:

    def match(self, outline: AlbumOutline, template_pages: list[TemplatePageItem]) -> AlbumOutline:
        covers = [p for p in template_pages if _is_type(p, _COVER_KW)]
        chapter_pool = [p for p in template_pages if _is_type(p, _CHAPTER_KW)]
        text_only = [p for p in template_pages if _is_type(p, _TEXT_KW)]
        content_pages = [p for p in template_pages
                         if not _is_type(p, _COVER_KW + _CHAPTER_KW + _TEXT_KW)]

        # 封面页
        all_images = [img for ch in outline.chapters for img in ch.images]
        if covers and all_images:
            cover_tp = covers[0]
            if cover_tp.imageCount > 0 and len(all_images) >= cover_tp.imageCount:
                cover_imgs = all_images[:cover_tp.imageCount]
                outline.cover_page = _to_outline_page(cover_tp, "cover", cover_imgs)
            elif cover_tp.imageCount == 0:
                outline.cover_page = _to_outline_page(cover_tp, "cover", [])

        # 章节页 + 内容页
        for chapter in outline.chapters:
            if chapter_pool:
                cp = chapter_pool.pop(0)
                if cp.imageCount == 0 or len(chapter.images) >= cp.imageCount:
                    ch_imgs = chapter.images[:cp.imageCount] if cp.imageCount > 0 else []
                    chapter.pages.append(_to_outline_page(cp, "chapter", ch_imgs))
                else:
                    logger.info("章节页 [templatePageId=%d] 需要 %d 张图但只有 %d 张，跳过",
                                cp.templatePageId, cp.imageCount, len(chapter.images))

            matched = self._diverse_match(chapter.images, content_pages)
            chapter.pages.extend(matched)

        # 纯文字页
        outline.text_only_pages = [_to_outline_page(tp, "text_only") for tp in text_only]

        return outline

    # ------------------------------------------------------------------
    # 核心：加权随机 + 桶内轮转
    # ------------------------------------------------------------------
    def _diverse_match(self, images: list[ImageFeature],
                       content_pages: list[TemplatePageItem]) -> list[OutlinePage]:
        """
        策略：
        1. 按 imageCount 分桶，每桶内 shuffle 后轮转
        2. 每步从「能放下剩余图片」的桶中加权随机选一个桶
           权重 = 桶内模板种类数（模板多的类型被选中概率更高）
        3. 连续两页尽量不选同一个桶（anti-repeat）
        """
        # 分桶
        by_count: dict[int, list[TemplatePageItem]] = defaultdict(list)
        for p in content_pages:
            if p.imageCount > 0:
                by_count[p.imageCount].append(p)

        if not by_count:
            return []

        buckets: dict[int, _TemplateBucket] = {
            k: _TemplateBucket(v) for k, v in by_count.items()
        }

        remaining = list(images)
        result: list[OutlinePage] = []
        last_count: int | None = None  # 上一页选的 imageCount，用于 anti-repeat

        while remaining:
            # 找出所有能放下的桶
            eligible = {k: b for k, b in buckets.items() if k <= len(remaining)}
            if not eligible:
                min_needed = min(buckets.keys())
                logger.info("剩余 %d 张图片无匹配模板（最小需要 %d 张），已丢弃",
                            len(remaining), min_needed)
                break

            chosen_count = self._pick_bucket(eligible, last_count)
            tp = buckets[chosen_count].next()

            result.append(_to_outline_page(tp, "content", remaining[:chosen_count]))
            remaining = remaining[chosen_count:]
            last_count = chosen_count

        if remaining:
            logger.info("有 %d 张图片因无合适模板被舍弃", len(remaining))
        return result

    @staticmethod
    def _pick_bucket(eligible: dict[int, '_TemplateBucket'],
                     last_count: int | None) -> int:
        """
        加权随机选桶，连续两页尽量不同。
        - 如果有多个桶可选，把上次选过的桶权重减半（soft anti-repeat）
        - 权重 = 桶内模板种类数
        """
        counts = list(eligible.keys())
        if len(counts) == 1:
            return counts[0]

        weights = []
        for c in counts:
            w = eligible[c].variety
            # soft anti-repeat：上次用过的桶权重减半
            if c == last_count:
                w = max(1, w // 2)
            weights.append(w)

        return random.choices(counts, weights=weights, k=1)[0]
