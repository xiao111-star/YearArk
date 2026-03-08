import os

RABBITMQ_URL = os.getenv("RABBITMQ_URL", "amqp://rabbitmq:yangdp@36.137.121.41:5672/")

REQUEST_QUEUE = "album.generation.request"
RESULT_QUEUE = "album.generation.result"
EXCHANGE_NAME = "yearark.album"

# 阿里云百炼 API Key，从环境变量读取
DASHSCOPE_API_KEY = os.getenv("DASHSCOPE_API_KEY", )
