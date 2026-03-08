"""
阿里云百炼 LLM 客户端
基础设施层，只负责 API 调用，不含任何业务逻辑
"""
import os
from openai import OpenAI

_client = OpenAI(
    api_key=os.getenv("DASHSCOPE_API_KEY", ""),
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)

VISION_MODEL = os.getenv("VISION_MODEL", "qwen-vl-plus")
TEXT_MODEL = os.getenv("TEXT_MODEL", "qwen-plus")


def chat(messages: list[dict], model: str = TEXT_MODEL) -> str:
    """文本对话"""
    resp = _client.chat.completions.create(model=model, messages=messages)
    return resp.choices[0].message.content


def vision_chat(messages: list[dict], model: str = VISION_MODEL) -> str:
    """视觉对话（含图片）"""
    resp = _client.chat.completions.create(model=model, messages=messages)
    return resp.choices[0].message.content
