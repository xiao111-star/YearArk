"""
纪念册生成 Pipeline
职责：编排各 Service，驱动大纲从骨架到完整数据，最终输出 GenerationResult
不含任何业务逻辑，只负责流程串联
"""
import logging
from domain.request import GenerationRequest
from domain.result import GenerationResult, PageDataItem
from domain.outline import AlbumOutline, OutlinePage
from service.image_service import ImageService
from service.grouping_service import GroupingService
from service.matching_service import MatchingService
from service.text_service import TextService

logger = logging.getLogger(__name__)


def _build_data_map(page: OutlinePage) -> dict:
    """将 OutlinePage 的图片和文案组装成 dataMap"""
    data: dict = {}
    for i, img in enumerate(page.images, 1):
        data[f"image_{i}"] = {"url": img.url, "focus_x": 0.5, "focus_y": 0.5, "scale": 1.0}
    data.update(page.texts)
    return data


class AlbumPipeline:

    def __init__(self):
        self._image_svc = ImageService()
        self._grouping_svc = GroupingService()
        self._matching_svc = MatchingService()
        self._text_svc = TextService()

    def process(self, request: GenerationRequest) -> GenerationResult:
        try:
            images = [m for m in request.mediaList if m.type == 2]
            user_texts = [m.content for m in request.mediaList if m.type == 1]

            # Step 1: 分析图片特征
            logger.info("[Step1] 分析 %d 张图片", len(images))
            features = self._image_svc.analyze_all(images)

            # Step 2: AI 分组，生成大纲骨架
            logger.info("[Step2] 生成大纲")
            outline: AlbumOutline = self._grouping_svc.build_outline(features)

            # Step 3: 模板页匹配，填充大纲页面结构
            logger.info("[Step3] 匹配模板页")
            self._matching_svc.match(outline, request.templatePages)

            # Step 4: 生成文案，填充大纲文字内容
            logger.info("[Step4] 生成文案")
            self._text_svc.fill_outline_texts(outline, user_texts)

            # Step 5: 将大纲转换为 GenerationResult
            logger.info("[Step5] 组装结果")
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
