"""
全局配置
所有配置项统一在此定义，直接修改此文件即可，不依赖环境变量
其他模块统一通过 `from config import settings` 获取
"""
from dataclasses import dataclass


@dataclass
class Settings:
    # ── RabbitMQ ──────────────────────────────────────────────────────────
    rabbitmq_url: str = "amqp://rabbitmq:yangdp@8.145.63.127:5672/"
    mq_exchange: str = "yearark.album"
    mq_request_queue: str = "album.generation.request"
    mq_result_queue: str = "album.generation.result"

    # ── 阿里云百炼 ────────────────────────────────────────────────────────
    dashscope_api_key: str = "sk-0b9d72248a4f4a968cd5ebeb831dcc0f"
    dashscope_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    vision_model: str = "qwen3.5-plus"
    text_model: str = "qwen3.5-plus"
    image_model_enable_thinking: bool = False
    text_model_enable_thinking: bool = False

    # ── 应用 ──────────────────────────────────────────────────────────────
    app_host: str = "0.0.0.0"
    app_port: int = 8001
    max_image_analysis_workers: int = 20


settings = Settings()
