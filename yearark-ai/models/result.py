from pydantic import BaseModel
from typing import List, Dict, Any, Optional


class PageDataItem(BaseModel):
    templatePageId: int
    dataMap: Dict[str, Any]


class GenerationResult(BaseModel):
    correlationId: str
    albumId: int
    status: str  # "success" or "failed"
    pages: Optional[List[PageDataItem]] = None
    errorMessage: Optional[str] = None
