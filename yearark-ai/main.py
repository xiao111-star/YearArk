import asyncio
import logging
import aio_pika
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI
from core.mq.consumer import start_consumer
from config import settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    connection = await aio_pika.connect_robust(settings.rabbitmq_url, heartbeat=600)
    task = asyncio.create_task(start_consumer(connection))
    logger.info("RabbitMQ consumer started")
    yield
    task.cancel()
    await connection.close()
    logger.info("RabbitMQ connection closed")


app = FastAPI(title="YearArk AI Service", lifespan=lifespan)


@app.get("/health")
async def health():
    return {"status": "ok"}

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.app_host,
        port=settings.app_port,
        reload=False,  # 调试时必须关闭，否则断点失效
    )

