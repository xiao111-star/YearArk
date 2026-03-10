"""
本地调试启动入口
配置项统一写在 yearark-ai/.env 文件中，不要在此硬编码
"""
import uvicorn
from config import settings

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.app_host,
        port=settings.app_port,
        reload=False,  # 调试时必须关闭，否则断点失效
    )
