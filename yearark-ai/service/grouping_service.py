"""
分组服务
职责：根据图片特征调用 LLM 分组，生成纪念册大纲骨架
"""
import json
import logging
from domain.outline import AlbumOutline, OutlineChapter, ImageFeature
from core.llm.client import chat

logger = logging.getLogger(__name__)

_PROMPT_TEMPLATE = """你是一个纪念册编辑，请根据以下图片的特征信息，将它们分成不超过5组，每组代表一个章节主题。

图片特征列表（JSON）：
{features_json}

要求：
1. 分组数量不超过5组，尽量让每组图片数量均衡
2. 每组给出一个简短的章节标题（4-8个字）和一句话描述
3. 每张图片只能属于一个组，用 media_id 标识
4. 同时为整本纪念册起一个标题

请严格按以下 JSON 格式返回，不要输出任何其他内容：
{{
  "album_title": "纪念册总标题",
  "chapters": [
    {{
      "title": "章节标题",
      "description": "章节一句话描述",
      "media_ids": [1, 2, 3]
    }}
  ]
}}"""


class GroupingService:

    def build_outline(self, features: list[ImageFeature]) -> AlbumOutline:
        """
        调用 AI 分组，返回填充了 album_title 和 chapters 的大纲骨架
        后续 pipeline 步骤继续填充 pages、texts 等
        """
        feature_map = {f.media_id: f for f in features}

        features_json = json.dumps(
            [{"media_id": f.media_id, "scene": f.scene, "people": f.people,
              "color_tone": f.color_tone, "composition": f.composition,
              "summary": f.summary}
             for f in features],
            ensure_ascii=False, indent=2
        )

        raw = chat([{"role": "user", "content": _PROMPT_TEMPLATE.format(features_json=features_json)}])
        start, end = raw.find("{"), raw.rfind("}") + 1
        data = json.loads(raw[start:end])

        outline = AlbumOutline(album_title=data.get("album_title", "我们的纪念册"))
        for ch in data.get("chapters", []):
            chapter = OutlineChapter(
                title=ch.get("title", ""),
                description=ch.get("description", ""),
                images=[feature_map[mid] for mid in ch.get("media_ids", []) if mid in feature_map],
            )
            outline.chapters.append(chapter)

        logger.info("大纲生成完成：《%s》共 %d 章节", outline.album_title, len(outline.chapters))
        return outline
