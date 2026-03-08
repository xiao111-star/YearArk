"""
RabbitMQ 发布者
基础设施层，负责将结果序列化并发送到结果队列
"""
import logging
import aio_pika
from domain.result import GenerationResult
from config import EXCHANGE_NAME, RESULT_QUEUE

logger = logging.getLogger(__name__)


async def publish_result(channel: aio_pika.abc.AbstractChannel, result: GenerationResult):
    exchange = await channel.declare_exchange(
        EXCHANGE_NAME, aio_pika.ExchangeType.DIRECT, durable=True
    )
    queue = await channel.declare_queue(
        RESULT_QUEUE, durable=True, arguments={"x-message-ttl": 300000}
    )
    await queue.bind(exchange, routing_key=RESULT_QUEUE)

    body = result.model_dump_json().encode()
    logger.info("发布结果 albumId=%s correlationId=%s pages=%d",
                result.albumId, result.correlationId,
                len(result.pages) if result.pages else 0)
    await exchange.publish(
        aio_pika.Message(body=body, content_type="application/json"),
        routing_key=RESULT_QUEUE,
    )
