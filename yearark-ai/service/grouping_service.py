"""
分组服务
职责：根据图片特征调用 LLM 分组，生成纪念册大纲骨架
"""
import json
import logging
from domain.outline import AlbumOutline, OutlineChapter, ImageFeature
from core.llm.client import chat

logger = logging.getLogger(__name__)

_PROMPT_TEMPLATE = """你是一个专业的纪念册编辑。请根据以下按时间顺序排列的图片特征，将它们划分为不超过5个章节。

【核心分组原则】（请在内部严格遵循）：
1. 连贯性优先：必须将特征相似且序号相邻（顺延）的图片分在一组，切忌跨序号跳跃拼凑。
2. 场景与事件：优先以“场景的转换”或“核心事件的发展阶段”（如：出发、游玩、聚餐、返程）作为划分边界。
3. 均衡性：尽量避免某一组只有1张图而另一组有10张图，除非遇到极其明显的场景断层。

图片特征列表（按真实顺序排列）：
{features_json}

请结合上述特征，给出整本纪念册的名称，以及每个章节的精炼标题（4-8字）和一句话情感描述。
【警告】你只需要输出合法的 JSON，不要输出任何额外的解释或思考过程代码块！请严格按以下 JSON 格式返回：
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
              "action": f.action, "emotion": f.emotion,
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
