"""
AI驱动测试任务管理系统 - AI服务
FastAPI主应用
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import logging
import sys
from pathlib import Path
from logging.handlers import RotatingFileHandler

from config import settings, ai_config
from api import recommendation, testcase
from data.redis_client import redis_client

# 创建日志目录
log_dir = Path(__file__).parent / 'logs'
log_dir.mkdir(exist_ok=True)
log_file = log_dir / 'ai-service.log'

# 配置日志格式
log_format = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
date_format = '%Y-%m-%d %H:%M:%S'

# 创建日志处理器
handlers = [
    # 控制台输出
    logging.StreamHandler(sys.stdout),
    # 文件输出（带轮转）
    RotatingFileHandler(
        log_file,
        maxBytes=10 * 1024 * 1024,  # 10MB
        backupCount=5,  # 保留5个备份文件
        encoding='utf-8'
    )
]

# 配置日志
logging.basicConfig(
    level=getattr(logging, settings.log_level),
    format=log_format,
    datefmt=date_format,
    handlers=handlers,
    force=True  # 强制覆盖已存在的配置
)

# 设置 uvicorn 相关 logger 的级别
logging.getLogger("uvicorn").setLevel(getattr(logging, settings.log_level))
logging.getLogger("uvicorn.access").setLevel(getattr(logging, settings.log_level))
logging.getLogger("uvicorn.error").setLevel(getattr(logging, settings.log_level))

logger = logging.getLogger(__name__)

# 创建FastAPI应用
app = FastAPI(
    title=settings.app_name,
    description="AI服务用于测试用例生成、智能调度推荐和风险评估",
    version=settings.version,
    debug=settings.debug
)

# CORS配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# 启动事件
@app.on_event("startup")
async def startup_event():
    """应用启动时执行"""
    logger.info(f"Starting {settings.app_name} v{settings.version}")
    logger.info(f"LLM Provider: {ai_config.LLM_PROVIDER}")
    logger.info(f"Debug Mode: {settings.debug}")
    logger.info(f"Log Level: {settings.log_level}")
    logger.info(f"Log File: {log_file.absolute()}")

    # 测试缓存连接
    try:
        if redis_client._client is not None:
            logger.info("✓ Redis connection established - Using Redis cache")
        else:
            cache_reason = "disabled by configuration" if not ai_config.ENABLE_REDIS else "connection failed"
            logger.info(f"ℹ Using in-memory cache (Redis {cache_reason})")
    except Exception as e:
        logger.warning(f"Redis connection check failed: {e}, using in-memory cache")


# 关闭事件
@app.on_event("shutdown")
async def shutdown_event():
    """应用关闭时执行"""
    logger.info("Shutting down AI Service")

    # 关闭缓存连接
    try:
        redis_client.close()
        logger.info("Redis connection closed")
    except Exception as e:
        logger.error(f"Error closing Redis: {e}")


# 注册路由
app.include_router(
    recommendation.router,
    prefix=ai_config.API_PREFIX
)

app.include_router(
    testcase.router,
    prefix=ai_config.API_PREFIX
)


@app.get("/health")
async def health_check():
    """健康检查端点"""
    return {
        "status": "UP",
        "service": "ai-service",
        "version": settings.version,
        "llm_provider": ai_config.LLM_PROVIDER
    }


@app.get("/")
async def root():
    """根路径"""
    return {
        "message": settings.app_name,
        "version": settings.version,
        "docs": "/docs",
        "health": "/health",
        "api_prefix": ai_config.API_PREFIX,
        "endpoints": {
            "recommendation": f"{ai_config.API_PREFIX}/recommendation",
            "testcase": f"{ai_config.API_PREFIX}/testcase"
        }
    }

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level=settings.log_level.lower(),  # 添加日志级别配置
        log_config=None  # 禁用 uvicorn 默认日志配置，使用我们的配置
    )
