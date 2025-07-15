"""
简单的测试脚本，用于验证RAG API和DeepSeek API是否正常工作。
"""

import asyncio
import json
import logging
import aiohttp
from openai import AsyncOpenAI
import argparse
import sys
from typing import Dict, Any, Optional

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler()]
)
logger = logging.getLogger(__name__)

# API配置
RAG_API_URL = "http://localhost:8000/rag"
DEEPSEEK_API_KEY = "sk-6bfa57029fbc44b9ac4eb38dd61bc695"
DEEPSEEK_BASE_URL = "https://api.deepseek.com"
DEEPSEEK_MODEL = "deepseek-chat"


async def test_rag_api(query: str = "TensorFlow.js") -> None:
    """测试RAG API连接。"""
    logger.info(f"测试RAG API: {RAG_API_URL}")
    logger.info(f"查询: {query}")
    
    try:
        async with aiohttp.ClientSession() as session:
            async with session.get(f"{RAG_API_URL}?query={query}") as response:
                if response.status == 200:
                    result = await response.json()
                    success = result.get("success", False)
                    message = result.get("message", "无消息")
                    results = result.get("results", [])
                    
                    logger.info(f"状态码: {response.status}")
                    logger.info(f"成功: {success}")
                    logger.info(f"消息: {message}")
                    logger.info(f"结果数量: {len(results)}")
                    
                    if results:
                        logger.info("\n首个结果示例:")
                        logger.info(f"  主题: {results[0].get('subject', 'N/A')}")
                        logger.info(f"  关系: {results[0].get('relation', 'N/A')}")
                        logger.info(f"  对象: {results[0].get('object', 'N/A')}")
                        logger.info(f"  相似度: {results[0].get('similarity', 'N/A')}")
                        
                        outgoing = results[0].get('outgoing_relations', [])
                        if outgoing:
                            logger.info(f"  外部关系数: {len(outgoing)}")
                            logger.info(f"  示例外部关系: {outgoing[0]['relation']} -> {outgoing[0]['target']}")
                    
                    logger.info("RAG API 测试成功!")
                    return True
                else:
                    error_text = await response.text()
                    logger.error(f"RAG API请求失败: 状态码 {response.status}, 错误: {error_text}")
                    return False
    except Exception as e:
        logger.error(f"RAG API请求异常: {str(e)}")
        return False


async def test_deepseek_api() -> None:
    """测试DeepSeek API连接。"""
    logger.info(f"测试DeepSeek API: {DEEPSEEK_BASE_URL}")
    
    try:
        client = AsyncOpenAI(
            api_key=DEEPSEEK_API_KEY,
            base_url=DEEPSEEK_BASE_URL
        )
        
        response = await client.chat.completions.create(
            model=DEEPSEEK_MODEL,
            messages=[
                {"role": "system", "content": "你是一个测试助手。"},
                {"role": "user", "content": "请回复'DeepSeek API 测试成功'。"}
            ],
            temperature=0.7,
            max_tokens=50,
            stream=False
        )
        
        content = response.choices[0].message.content
        logger.info(f"DeepSeek API 返回: {content}")
        logger.info("DeepSeek API 测试成功!")
        return True
    except Exception as e:
        logger.error(f"DeepSeek API请求异常: {str(e)}")
        return False


async def main() -> None:
    """主函数。"""
    parser = argparse.ArgumentParser(description="测试RAG API和DeepSeek API连接")
    parser.add_argument("--rag-only", action="store_true", help="仅测试RAG API")
    parser.add_argument("--deepseek-only", action="store_true", help="仅测试DeepSeek API")
    parser.add_argument("--query", type=str, default="TensorFlow.js", help="用于测试RAG API的查询")
    
    args = parser.parse_args()
    
    if not args.deepseek_only:
        rag_ok = await test_rag_api(args.query)
        if not rag_ok:
            logger.error("RAG API测试失败，请检查服务是否运行。")
    
    if not args.rag_only:
        deepseek_ok = await test_deepseek_api()
        if not deepseek_ok:
            logger.error("DeepSeek API测试失败，请检查API密钥和网络连接。")


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logger.info("用户中断，退出测试")
        sys.exit(0)
    except Exception as e:
        logger.error(f"测试执行错误: {str(e)}")
        sys.exit(1) 