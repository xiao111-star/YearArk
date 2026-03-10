"""
阿里云百炼 LLM 客户端
基础设施层，只负责 API 调用，不含任何业务逻辑
"""
from openai import OpenAI
from config import settings

_client = OpenAI(
    api_key=settings.dashscope_api_key,
    base_url=settings.dashscope_base_url,
)


def chat(messages: list[dict], model: str = None) -> str:
    """文本对话"""
    resp = _client.chat.completions.create(
        model=model or settings.text_model,
        messages=messages,
    )
    return resp.choices[0].message.content


def vision_chat(messages: list[dict], model: str = None) -> str:
    """视觉对话（含图片）"""
    resp = _client.chat.completions.create(
        model=model or settings.vision_model,
        messages=messages,
        extra_body ={"enable_thinking": settings.image_model_enable_thinking}
    )
    a = resp.choices[0].message.content
    return a
