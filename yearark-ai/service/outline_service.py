"""
大纲生成服务（视觉一步到位）
职责：将所有图片一次性传给视觉模型，直接完成分组 + 起标题，输出大纲骨架
"""
import json
import logging
from domain.request import MediaItem
from domain.outline import AlbumOutline, OutlineChapter, ImageFeature
from core.llm.client import vision_chat

logger = logging.getLogger(__name__)

_PROMPT = """角色：你是一位资深纪念册策划编辑，擅长从照片中捕捉故事线并组织成有温度的章节。

任务：观察所有照片，为它们策划一本纪念册的大纲。你需要完成两件事：
1. 先逐张理解每张照片的内容
2. 再将照片分成 2~5 个章节

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
第一步：逐张理解（在心中完成，不需要输出）

对每张照片，快速判断：
- 在哪里？（室内/户外/餐厅/景区/海边/街道/学校/家中...）
- 在做什么？（聚餐/游玩/拍照/运动/庆祝/散步/工作...）
- 什么时候？（白天/夜晚/清晨/黄昏）
- 什么感觉？（欢快/温馨/安静/热闹/感动/轻松...）

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
第二步：分组策划（输出结果）

根据第一步的理解，按以下规则将照片分组：

规则1 - 同一场景归一组：在同一个地方拍的照片放一起
  例：#1#2#3 都在海边 → 归为一组"海风与浪花"

规则2 - 同一事件归一组：属于同一个活动的不同瞬间放一起
  例：#4 点蜡烛、#5 吹蜡烛、#6 切蛋糕 → 归为一组"生日的甜蜜"

规则3 - 序号相邻优先：编号挨着的照片优先放一组，不要把 #1 和 #8 跳着凑一起

规则4 - 数量均衡：避免一组 1 张另一组 8 张

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
输出要求：

- album_title：纪念册总标题，4~10字，有诗意或画面感
- 每个章节的 title：4~8字，能概括这组照片的主题
- 每个章节的 description：一句话，带有情感温度，15~30字
- media_ids：该章节包含的照片编号数组

示例输出（仅供格式参考，内容请根据实际照片生成）：
{
  "album_title": "山海间的笑声",
  "chapters": [
    {
      "title": "出发的早晨",
      "description": "背上行囊的那一刻，期待已经写在每个人脸上",
      "media_ids": [1, 2]
    },
    {
      "title": "海边的午后",
      "description": "浪花拍打脚踝，笑声比海风还响亮",
      "media_ids": [3, 4, 5]
    },
    {
      "title": "夜晚的篝火",
      "description": "火光映着每张脸，这一刻值得被永远记住",
      "media_ids": [6, 7]
    }
  ]
}

警告：只输出合法 JSON，不要输出任何解释、思考过程或代码块标记。"""


class OutlineService:

    def build_outline(self, images: list[MediaItem]) -> AlbumOutline:
        """
        一次视觉调用：传入所有图片，直接输出分组大纲
        """
        media_map = {img.id: img for img in images}

        # 构建多图消息
        content = []
        for img in images:
            content.append({"type": "text", "text": f"图片 #{img.id}："})
            content.append({"type": "image_url", "image_url": {"url": img.content}})
        content.append({"type": "text", "text": _PROMPT})

        messages = [{"role": "user", "content": content}]
        raw = vision_chat(messages)

        start, end = raw.find("{"), raw.rfind("}") + 1
        data = json.loads(raw[start:end])

        outline = AlbumOutline(album_title=data.get("album_title", "我们的纪念册"))
        for ch in data.get("chapters", []):
            chapter_images = []
            for mid in ch.get("media_ids", []):
                if mid in media_map:
                    m = media_map[mid]
                    chapter_images.append(ImageFeature(media_id=m.id, url=m.content))
            chapter = OutlineChapter(
                title=ch.get("title", ""),
                description=ch.get("description", ""),
                images=chapter_images,
            )
            outline.chapters.append(chapter)

        logger.info("大纲生成完成：《%s》共 %d 章节", outline.album_title, len(outline.chapters))
        return outline
