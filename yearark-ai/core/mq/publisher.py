"""
RabbitMQ 发布者
基础设施层，负责将结果序列化并发送到结果队列
带重试机制，应对连接刚恢复的场景
"""
import asyncio
import logging
import aio_pika
from domain.result import GenerationResult
from config import settings

logger = logging.getLogger(__name__)

MAX_RETRIES = 3
RETRY_DELAY = 5  # 秒


async def publish_result(connection: aio_pika.abc.AbstractConnection, result: GenerationResult):
    body = result.model_dump_json().encode()

    for attempt in range(1, MAX_RETRIES + 1):
        try:
            async with connection.channel() as channel:
                exchange = await channel.declare_exchange(
                    settings.mq_exchange, aio_pika.ExchangeType.DIRECT, durable=True
                )
                queue = await channel.declare_queue(
                    settings.mq_result_queue, durable=True, arguments={"x-message-ttl": 300000}
                )
                await queue.bind(exchange, routing_key=settings.mq_result_queue)

                await exchange.publish(
                    aio_pika.Message(body=body, content_type="application/json"),
                    routing_key=settings.mq_result_queue,
                )
            logger.info("发布结果 albumId=%s correlationId=%s pages=%d",
                        result.albumId, result.correlationId,
                        len(result.pages) if result.pages else 0)
            return
        except Exception as e:
            logger.warning("发布失败 (第%d次): %s", attempt, e)
            if attempt < MAX_RETRIES:
                await asyncio.sleep(RETRY_DELAY)
            else:
                logger.error("发布最终失败，已重试%d次", MAX_RETRIES)
                raise
