import os

RABBITMQ_URL = os.getenv("RABBITMQ_URL", "amqp://rabbitmq:yangdp@36.137.121.41:5672/")

REQUEST_QUEUE = "album.generation.request"
RESULT_QUEUE = "album.generation.result"
EXCHANGE_NAME = "yearark.album"
