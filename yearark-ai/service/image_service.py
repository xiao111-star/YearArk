"""
图片分析服务
职责：调用视觉 LLM，提取每张图片的场景/人物/色彩/构图特征
"""
import json
import logging
from concurrent.futures import ThreadPoolExecutor
from domain.request import MediaItem
from domain.outline import ImageFeature
from core.llm.client import vision_chat
from config import settings

logger = logging.getLogger(__name__)

_PROMPT = """请分析这张照片，以 JSON 格式返回以下字段，不要输出任何其他内容：
{
  "scene": "场景描述，如：地铁、景区、室内、街道、海边等",
  "people": "人物状态，如：独处、合影、微笑、陪伴、无人等",
  "color_tone": "色彩氛围，如：暖色调、冷色调、明亮、昏暗、夜景等",
  "composition": "构图特点，如：自拍、特写、风景、合影、全身等",
  "action": "动作或事件，如：正在野餐、在沙滩奔跑、吹蜡烛等",
  "emotion": "情感氛围，如：温馨、欢快、热烈、平静等",
  "summary": "一句话描述这张照片的内容"
}"""


class ImageService:

    def analyze(self, media: MediaItem) -> ImageFeature:
        """分析单张图片，失败时返回空特征，不中断流程"""
        try:
            messages = [{
                "role": "user",
                "content": [
                    {"type": "image_url", "image_url": {"url": media.content}},
                    {"type": "text", "text": _PROMPT},
                ],
            }]
            raw = vision_chat(messages)
            start, end = raw.find("{"), raw.rfind("}") + 1
            data = json.loads(raw[start:end])
            return ImageFeature(
                media_id=media.id,
                url=media.content,
                scene=data.get("scene", ""),
                people=data.get("people", ""),
                color_tone=data.get("color_tone", ""),
                composition=data.get("composition", ""),
                action=data.get("action", ""),
                emotion=data.get("emotion", ""),
                summary=data.get("summary", ""),
            )
        except Exception as e:
            logger.warning("图片分析失败 media_id=%s: %s", media.id, e)
            return ImageFeature(media_id=media.id, url=media.content)

    def analyze_all(self, images: list[MediaItem]) -> list[ImageFeature]:
        """批量分析，返回特征列表（顺序与入参一致）"""
        with ThreadPoolExecutor(max_workers=settings.max_image_analysis_workers) as executor:
            # executor.map 会保证返回的顺序与传入的 images 顺序完全一致
            results = list(executor.map(self.analyze, images))
        return results
