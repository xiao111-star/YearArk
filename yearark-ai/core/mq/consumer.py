"""
RabbitMQ 消费者
基础设施层，负责监听队列、反序列化消息、调用 pipeline 处理、发布结果

关键设计：
1. pipeline.process() 是同步阻塞（含多次 AI 调用），放到线程池执行避免阻塞事件循环
2. 外层无限循环保证连接断开后能自动恢复消费
"""
import asyncio
import json
import logging
import aio_pika
from domain.request import GenerationRequest
from pipeline.album_pipeline import AlbumPipeline
from core.mq.publisher import publish_result
from config import settings

logger = logging.getLogger(__name__)
_pipeline = AlbumPipeline()

RECONNECT_DELAY = 5  # 秒


async def start_consumer(connection: aio_pika.abc.AbstractConnection):
    loop = asyncio.get_running_loop()

    while True:
        try:
            channel = await connection.channel()
            await channel.set_qos(prefetch_count=1)

            exchange = await channel.declare_exchange(
                settings.mq_exchange, aio_pika.ExchangeType.DIRECT, durable=True
            )
            queue = await channel.declare_queue(
                settings.mq_request_queue, durable=True, arguments={"x-message-ttl": 300000}
            )
            await queue.bind(exchange, routing_key=settings.mq_request_queue)

            logger.info("消费者就绪，开始监听队列 %s", settings.mq_request_queue)

            async with queue.iterator() as q:
                async for message in q:
                    async with message.process():
                        try:
                            data = json.loads(message.body)
                            request = GenerationRequest(**data)
                            result = await loop.run_in_executor(None, _pipeline.process, request)
                            await publish_result(connection, result)
                            logger.info("处理完成 albumId=%s correlationId=%s pages=%d",
                                        request.albumId, request.correlationId,
                                        len(result.pages) if result.pages else 0)
                        except Exception as e:
                            logger.exception("消息处理失败: %s", e)

        except asyncio.CancelledError:
            logger.info("消费者被取消，退出")
            return
        except Exception as e:
            logger.warning("消费者异常，%d秒后重新连接: %s", RECONNECT_DELAY, e)
            await asyncio.sleep(RECONNECT_DELAY)
