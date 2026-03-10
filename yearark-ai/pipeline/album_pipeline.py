"""
纪念册生成 Pipeline
职责：编排各 Service，驱动从图片到完整纪念册数据
流程：视觉分组 → 模板匹配 → 两轮文案生成 → 组装结果
"""
import logging
from domain.request import GenerationRequest
from domain.result import GenerationResult, PageDataItem
from domain.outline import AlbumOutline, OutlinePage
from service.outline_service import OutlineService
from service.matching_service import MatchingService
from service.text_service import TextService

logger = logging.getLogger(__name__)


def _build_data_map(page: OutlinePage) -> dict:
    data: dict = {}
    for i, img in enumerate(page.images, 1):
        data[f"image_{i}"] = {"url": img.url, "focus_x": 0.5, "focus_y": 0.5, "scale": 1.0}
    data.update(page.texts)
    return data


class AlbumPipeline:

    def __init__(self):
        self._outline_svc = OutlineService()
        self._matching_svc = MatchingService()
        self._text_svc = TextService()

    def process(self, request: GenerationRequest) -> GenerationResult:
        try:
            images = [m for m in request.mediaList if m.type == 2]
            user_texts = [m.content for m in request.mediaList if m.type == 1]

            # Step 1: 视觉分组（1次视觉LLM调用）
            logger.info("[Step1] 视觉分析 %d 张图片并分组", len(images))
            outline: AlbumOutline = self._outline_svc.build_outline(images)

            # Step 2: 模板匹配（纯算法）
            logger.info("[Step2] 匹配模板页")
            self._matching_svc.match(outline, request.templatePages)

            # Step 3: 两轮文案生成（第一轮内容页并发，第二轮封面+章节页并发）
            logger.info("[Step3] 生成文案")
            self._text_svc.fill_outline_texts(outline, user_texts)

            # Step 4: 组装结果
            logger.info("[Step4] 组装结果")
            pages: list[PageDataItem] = []

            if outline.cover_page:
                pages.append(PageDataItem(
                    templatePageId=outline.cover_page.template_page_id,
                    dataMap=_build_data_map(outline.cover_page),
                ))

            for chapter in outline.chapters:
                for page in chapter.pages:
                    pages.append(PageDataItem(
                        templatePageId=page.template_page_id,
                        dataMap=_build_data_map(page),
                    ))

            for page in outline.text_only_pages:
                if page.texts:
                    pages.append(PageDataItem(
                        templatePageId=page.template_page_id,
                        dataMap=_build_data_map(page),
                    ))

            logger.info("[Done] 生成完成，共 %d 页", len(pages))
            return GenerationResult(
                correlationId=request.correlationId,
                albumId=request.albumId,
                status="success",
                pages=pages,
            )

        except Exception as e:
            logger.exception("Pipeline 执行失败: %s", e)
            return GenerationResult(
                correlationId=request.correlationId,
                albumId=request.albumId,
                status="failed",
                errorMessage=str(e),
            )
