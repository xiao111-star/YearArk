"""
模板匹配服务
职责：将模板页按类型分类，并为每章图片贪心匹配内容页
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


def _to_outline_page(tp: TemplatePageItem, images: list[ImageFeature] = None) -> OutlinePage:
    return OutlinePage(
        template_page_id=tp.templatePageId,
        image_count=tp.imageCount,
        text_count=tp.textCount,
        schema_content=tp.schemaContent,
        images=images or [],
    )


class MatchingService:

    def match(self, outline: AlbumOutline, template_pages: list[TemplatePageItem]) -> AlbumOutline:
        """
        将模板页分配到大纲各位置，原地修改 outline 并返回
        """
        covers = [p for p in template_pages if _is_type(p, _COVER_KW)]
        chapter_pool = [p for p in template_pages if _is_type(p, _CHAPTER_KW)]
        text_only = [p for p in template_pages if _is_type(p, _TEXT_KW)]
        content_pages = [p for p in template_pages
                         if not _is_type(p, _COVER_KW + _CHAPTER_KW + _TEXT_KW)]

        # 封面页
        all_images = [img for ch in outline.chapters for img in ch.images]
        if covers and all_images:
            cover_tp = covers[0]
            cover_imgs = all_images[:cover_tp.imageCount] if cover_tp.imageCount > 0 else []
            outline.cover_page = _to_outline_page(cover_tp, cover_imgs)

        # 章节页 + 内容页
        for chapter in outline.chapters:
            # 章节页
            if chapter_pool:
                cp = chapter_pool.pop(0)
                ch_imgs = chapter.images[:cp.imageCount] if cp.imageCount > 0 else []
                chapter.pages.append(_to_outline_page(cp, ch_imgs))

            # 内容页贪心匹配
            matched = self._greedy_match(chapter.images, content_pages)
            chapter.pages.extend(matched)

        # 纯文字页
        outline.text_only_pages = [_to_outline_page(tp) for tp in text_only]

        return outline

    def _greedy_match(self, images: list[ImageFeature], content_pages: list[TemplatePageItem]) -> list[OutlinePage]:
        """
        按 imageCount 降序贪心匹配，只按图片数量匹配，文案由 AI 后续生成。
        每次从剩余图片中取 imageCount 张，直到图片耗尽或无可用模板。
        允许最后一页图片不足（用实际剩余数量填充）。
        """
        # 只保留有图片槽的模板，按容量降序
        usable = sorted([p for p in content_pages if p.imageCount > 0],
                        key=lambda p: p.imageCount, reverse=True)
        remaining = list(images)
        result = []

        while remaining and usable:
            # 找到不超过剩余数量的最大容量模板
            tp = next((p for p in usable if p.imageCount <= len(remaining)), None)
            if tp is None:
                # 所有模板容量都大于剩余图片，取容量最小的模板，用实际剩余填充
                tp = usable[-1]
            cap = min(tp.imageCount, len(remaining))
            result.append(_to_outline_page(tp, remaining[:cap]))
            remaining = remaining[cap:]

        if remaining:
            logger.info("有 %d 张图片因无合适模板被舍弃", len(remaining))
        return result
