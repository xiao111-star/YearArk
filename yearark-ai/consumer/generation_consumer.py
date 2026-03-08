import json
import logging
import aio_pika
from models.request import GenerationRequest
from models.result import GenerationResult
from strategy.mock_strategy import MockGroupingStrategy
from publisher.result_publisher import publish_result
from config import EXCHANGE_NAME, REQUEST_QUEUE

logger = logging.getLogger(__name__)
_strategy = MockGroupingStrategy()


async def start_consumer(connection: aio_pika.abc.AbstractConnection):
    channel = await connection.channel()
    await channel.set_qos(prefetch_count=1)

    exchange = await channel.declare_exchange(
        EXCHANGE_NAME, aio_pika.ExchangeType.DIRECT, durable=True
    )
    queue = await channel.declare_queue(
        REQUEST_QUEUE, durable=True, arguments={"x-message-ttl": 300000}
    )
    await queue.bind(exchange, routing_key=REQUEST_QUEUE)

    async with queue.iterator() as q:
        async for message in q:
            async with message.process():
                try:
                    data = json.loads(message.body)
                    request = GenerationRequest(**data)
                    result = _strategy.process(request)
                    await publish_result(channel, result)
                    logger.info("Processed albumId=%s correlationId=%s pages=%d",
                                request.albumId, request.correlationId,
                                len(result.pages) if result.pages else 0)
                except Exception as e:
                    logger.exception("Failed to process message: %s", e)
