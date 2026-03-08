import asyncio
import logging
import aio_pika
from contextlib import asynccontextmanager
from fastapi import FastAPI
from core.mq.consumer import start_consumer
from config import RABBITMQ_URL

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    connection = await aio_pika.connect_robust(RABBITMQ_URL)
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
