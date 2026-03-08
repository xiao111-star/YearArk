import json
import logging
import aio_pika
from models.result import GenerationResult
from config import EXCHANGE_NAME, RESULT_QUEUE

logger = logging.getLogger(__name__)


async def publish_result(channel: aio_pika.abc.AbstractChannel, result: GenerationResult):
    exchange = await channel.declare_exchange(
        EXCHANGE_NAME, aio_pika.ExchangeType.DIRECT, durable=True
    )
    # 声明队列并绑定到 exchange（缺少这一步消息无法路由到队列）
    queue = await channel.declare_queue(
        RESULT_QUEUE, durable=True, arguments={"x-message-ttl": 300000}
    )
    await queue.bind(exchange, routing_key=RESULT_QUEUE)

    body = result.model_dump_json().encode()
    logger.info(
        "Publishing result to exchange=%s routing_key=%s albumId=%s correlationId=%s pages=%d body=%s",
        EXCHANGE_NAME, RESULT_QUEUE,
        result.albumId, result.correlationId,
        len(result.pages) if result.pages else 0,
        body.decode()[:500],
    )
    await exchange.publish(
        aio_pika.Message(body=body, content_type="application/json"),
        routing_key=RESULT_QUEUE,
    )
    logger.info("Result published successfully albumId=%s", result.albumId)
