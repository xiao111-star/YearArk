import os
import uvicorn

# 本地调试时在这里填入 API Key，生产环境通过系统环境变量注入
os.environ.setdefault("DASHSCOPE_API_KEY", "sk-xxxxxxxxxxxxxxxx")
os.environ.setdefault("RABBITMQ_URL", "amqp://rabbitmq:yangdp@36.137.121.41:5672/")

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8001,
        reload=False,  # 调试时必须关闭，否则断点失效
    )
