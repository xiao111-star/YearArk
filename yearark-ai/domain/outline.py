"""
纪念册生成中间大纲模型
贯穿整个 pipeline，各 service 逐步填充
"""
from dataclasses import dataclass, field
from typing import Optional


@dataclass
class ImageFeature:
    """图片基础信息，含视觉模型识别的焦点坐标"""
    media_id: int
    url: str
    focus_x: float = 0.5   # 主体水平位置（0.0=最左 ~ 1.0=最右）
    focus_y: float = 0.5   # 主体垂直位置（0.0=最上 ~ 1.0=最下）


@dataclass
class OutlinePage:
    """大纲中的一个页面（已匹配模板页 + 待填充数据）"""
    template_page_id: int
    image_count: int
    text_count: int
    schema_content: Optional[str]
    page_type: str = "content"  # cover / chapter / content / text_only
    images: list[ImageFeature] = field(default_factory=list)
    texts: dict[str, str] = field(default_factory=dict)


@dataclass
class OutlineChapter:
    """大纲中的一个章节"""
    title: str
    description: str
    images: list[ImageFeature] = field(default_factory=list)
    pages: list[OutlinePage] = field(default_factory=list)


@dataclass
class AlbumOutline:
    """纪念册完整大纲，pipeline 各阶段逐步填充"""
    album_title: str = ""
    cover_page: Optional[OutlinePage] = None
    chapters: list[OutlineChapter] = field(default_factory=list)
    text_only_pages: list[OutlinePage] = field(default_factory=list)
