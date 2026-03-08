"""
纪念册生成中间大纲模型
贯穿整个 pipeline，各 service 逐步填充
"""
from dataclasses import dataclass, field
from typing import Optional


@dataclass
class ImageFeature:
    """单张图片的 AI 分析特征"""
    media_id: int
    url: str
    scene: str = ""        # 场景：地铁、景区、室内...
    people: str = ""       # 人物：独处、合影、微笑...
    color_tone: str = ""   # 色彩：暖色调、冷色调、夜景...
    composition: str = ""  # 构图：自拍、特写、风景...
    summary: str = ""      # 一句话描述


@dataclass
class OutlinePage:
    """大纲中的一个页面（已匹配模板页 + 待填充数据）"""
    template_page_id: int
    image_count: int
    text_count: int
    schema_content: Optional[str]
    images: list[ImageFeature] = field(default_factory=list)   # 分配到本页的图片
    texts: dict[str, str] = field(default_factory=dict)        # 生成的文案 {slot_id: content}


@dataclass
class OutlineChapter:
    """大纲中的一个章节"""
    title: str
    description: str
    images: list[ImageFeature] = field(default_factory=list)   # 本章所有图片
    pages: list[OutlinePage] = field(default_factory=list)     # 本章拆分出的页面


@dataclass
class AlbumOutline:
    """纪念册完整大纲，pipeline 各阶段逐步填充"""
    album_title: str = ""
    cover_page: Optional[OutlinePage] = None
    chapters: list[OutlineChapter] = field(default_factory=list)
    text_only_pages: list[OutlinePage] = field(default_factory=list)
