"""
模板匹配服务
职责：将模板页按类型分类，为大纲各位置分配模板
页面序列：封面 → [章节页 → 内容页×N] × 章节数 → 纯文字页
"""
import logging
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

            matched = self._greedy_match(chapter.images, content_pages)
            chapter.pages.extend(matched)

        # 纯文字页
        outline.text_only_pages = [_to_outline_page(tp, "text_only") for tp in text_only]

        return outline

    def _greedy_match(self, images: list[ImageFeature], content_pages: list[TemplatePageItem]) -> list[OutlinePage]:
        usable = sorted([p for p in content_pages if p.imageCount > 0],
                        key=lambda p: p.imageCount, reverse=True)
        remaining = list(images)
        result = []

        while remaining and usable:
            tp = next((p for p in usable if p.imageCount <= len(remaining)), None)
            if tp is None:
                # 没有能容纳剩余图片数量的模板，剩余图片无法安全填充必填 slot，直接丢弃
                logger.info("剩余 %d 张图片无匹配模板（最小需要 %d 张），已丢弃",
                            len(remaining), usable[-1].imageCount)
                break
            cap = tp.imageCount
            result.append(_to_outline_page(tp, "content", remaining[:cap]))
            remaining = remaining[cap:]

        if remaining:
            logger.info("有 %d 张图片因无合适模板被舍弃", len(remaining))
        return result
