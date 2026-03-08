from models.request import GenerationRequest, MediaItem
from models.result import GenerationResult, PageDataItem
from strategy.base import GroupingStrategy


class MockGroupingStrategy(GroupingStrategy):
    """
    Mock 实现：按 sort 顺序将素材填充到模板页。
    素材不足时跳过无法完全填满的模板页，绝不使用占位数据。
    """

    def process(self, request: GenerationRequest) -> GenerationResult:
        images: list[MediaItem] = sorted(
            [m for m in request.mediaList if m.type == 2], key=lambda m: m.sort
        )
        texts: list[MediaItem] = sorted(
            [m for m in request.mediaList if m.type == 1], key=lambda m: m.sort
        )

        img_idx = 0
        txt_idx = 0
        pages: list[PageDataItem] = []

        for tp in request.templatePages:
            # 检查素材是否足够填满本页
            if img_idx + tp.imageCount > len(images):
                continue
            if txt_idx + tp.textCount > len(texts):
                continue

            data_map: dict = {}

            for i in range(1, tp.imageCount + 1):
                media = images[img_idx]
                data_map[f"image_{i}"] = {
                    "url": media.content,
                    "focus_x": 0.5,
                    "focus_y": 0.5,
                    "scale": 1.0,
                }
                img_idx += 1

            for i in range(1, tp.textCount + 1):
                data_map[f"text_{i}"] = texts[txt_idx].content
                txt_idx += 1

            pages.append(PageDataItem(templatePageId=tp.templatePageId, dataMap=data_map))

        return GenerationResult(
            correlationId=request.correlationId,
            albumId=request.albumId,
            status="success",
            pages=pages,
        )
