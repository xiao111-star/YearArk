from pydantic import BaseModel, field_validator
from typing import List, Optional


class MediaItem(BaseModel):
    id: int
    type: int  # 2=image, 1=text
    content: str
    sort: Optional[int] = 0

    @field_validator("sort", mode="before")
    @classmethod
    def default_sort(cls, v):
        return v if v is not None else 0


class TemplatePageItem(BaseModel):
    templatePageId: int
    schemaId: int
    imageCount: int
    textCount: int


class GenerationRequest(BaseModel):
    correlationId: str
    albumId: int
    mediaList: List[MediaItem]
    templatePages: List[TemplatePageItem]
