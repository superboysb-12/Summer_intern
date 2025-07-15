"""
Main script for the data generation pipeline.
"""

import asyncio
import logging
import argparse
import time
import os
import random
from typing import List, Dict, Any, Optional
import json
import sys
import traceback

from config import (
    INPUT_FILE,
    OUTPUT_FILE,
    MAX_CONCURRENT_REQUESTS,
    QUESTION_TYPES
)
from data_processor import DataProcessor, process_single_query


# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('pipeline.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


async def process_single_query_with_retry(query: str, question_type: Optional[str] = None, max_retries: int = 1) -> Dict[str, Any]:
    """Process a single query with retry mechanism.
    
    Args:
        query: The search query.
        question_type: The type of question to generate.
        max_retries: Maximum number of retry attempts.
        
    Returns:
        Dictionary containing the processed data or an error.
    """
    retries = 0
    last_error = None
    
    while retries <= max_retries:
        try:
            result = await process_single_query(query, question_type)
            return result
        except Exception as e:
            last_error = e
            retries += 1
            if retries <= max_retries:
                logger.warning(f"重试 {retries}/{max_retries} 查询 '{query}': {str(e)}")
                await asyncio.sleep(1)  # 添加延迟以避免过快重试
    
    return {
        "error": str(last_error),
        "query": query,
        "question_type": question_type
    }


async def process_batch(queries: List[str], question_types: Optional[List[str]] = None) -> List[Dict[str, Any]]:
    """Process a batch of queries concurrently.
    
    Args:
        queries: List of queries to process.
        question_types: Optional list of question types to use for each query.
        
    Returns:
        List of processed results.
    """
    if question_types is None:
        # If no question types specified, generate random types
        question_types = [random.choice(QUESTION_TYPES) for _ in queries]
    
    # Make sure question_types has the same length as queries
    if len(question_types) < len(queries):
        question_types.extend([random.choice(QUESTION_TYPES) for _ in range(len(queries) - len(question_types))])
    
    tasks = []
    for query, q_type in zip(queries, question_types):
        tasks.append(process_single_query_with_retry(query, q_type))
    
    return await asyncio.gather(*tasks)


async def test_rag_connection():
    """测试RAG API连接是否正常工作。"""
    from rag_client import fetch_rag_results
    try:
        logger.info("测试RAG API连接...")
        test_query = "TensorFlow.js"
        result = await fetch_rag_results(test_query)
        if result.get("success") is True:
            logger.info(f"RAG API连接成功! 查询'{test_query}'返回了 {len(result.get('results', []))} 条结果。")
            return True
        else:
            logger.error(f"RAG API返回错误: {result.get('message', '未知错误')}")
            return False
    except Exception as e:
        logger.error(f"RAG API连接测试失败: {str(e)}")
        return False


async def test_deepseek_connection():
    """测试DeepSeek API连接是否正常工作。"""
    from deepseek_client import generate_question
    try:
        logger.info("测试DeepSeek API连接...")
        system_prompt = "你是一个测试助手。"
        user_prompt = "返回'测试成功'三个字。"
        result = await generate_question(system_prompt, user_prompt)
        
        if result.startswith("ERROR:"):
            logger.error(f"DeepSeek API连接失败: {result[6:]}")
            return False
        else:
            logger.info(f"DeepSeek API连接成功! 返回结果: {result[:50]}...")
            return True
    except Exception as e:
        logger.error(f"DeepSeek API连接测试失败: {str(e)}")
        return False


async def process_queries(queries: List[str], output_file: str, batch_size: int = MAX_CONCURRENT_REQUESTS) -> None:
    """Process all queries with rate limiting.
    
    Args:
        queries: List of queries to process.
        output_file: Path to the output file.
        batch_size: Maximum number of concurrent requests.
    """
    results = []
    total_queries = len(queries)
    
    # 测试API连接
    rag_ok = await test_rag_connection()
    deepseek_ok = await test_deepseek_connection()
    
    if not rag_ok:
        logger.error("RAG API连接失败，请确保RAG服务正在运行。")
    if not deepseek_ok:
        logger.error("DeepSeek API连接失败，请检查API密钥和网络连接。")
    
    if not rag_ok or not deepseek_ok:
        logger.error("由于API连接问题，无法继续处理查询。请修复API连接问题后重试。")
        return
    
    # Process queries in batches to limit concurrency
    for i in range(0, total_queries, batch_size):
        batch = queries[i:i+batch_size]
        logger.info(f"Processing batch {i//batch_size + 1}/{(total_queries + batch_size - 1)//batch_size} ({len(batch)} queries)")
        
        batch_start_time = time.time()
        batch_results = await process_batch(batch)
        batch_end_time = time.time()
        
        # Filter out errors and log them
        valid_results = []
        for j, result in enumerate(batch_results):
            if "error" in result:
                logger.error(f"Error processing query '{batch[j]}': {result['error']}")
            else:
                valid_results.append(result)
        
        results.extend(valid_results)
        
        # Log batch progress
        logger.info(
            f"Batch {i//batch_size + 1} completed: {len(valid_results)}/{len(batch)} successful, "
            f"{len(batch) - len(valid_results)} failed, "
            f"{batch_end_time - batch_start_time:.2f}s total, "
            f"{(batch_end_time - batch_start_time) / len(batch):.2f}s per query"
        )
        
        # Save intermediate results
        if valid_results:
            # Create a temporary file name to avoid overwriting the main output file
            temp_output_file = f"{output_file}.temp_{i//batch_size + 1}"
            logger.info(f"Saving intermediate results to {temp_output_file}")
            DataProcessor.save_results(valid_results, temp_output_file)
    
    # Save final results
    logger.info(f"Processing completed: {len(results)}/{total_queries} successful, {total_queries - len(results)} failed")
    if results:
        logger.info(f"Saving final results to {output_file}")
        DataProcessor.save_results(results, output_file)
    else:
        logger.warning("No results to save")


async def process_single_test_query(query: str, question_type: Optional[str] = None) -> None:
    """处理单个查询并显示详细结果，用于测试。
    
    Args:
        query: 要处理的查询。
        question_type: 问题类型。
    """
    try:
        # 打印测试参数以便调试
        logger.debug(f"测试参数 - query: '{query}', question_type: '{question_type}'")
        logger.info(f"测试处理单个查询: '{query}', 类型: {question_type or '随机'}")
        
        # 确保question_type是有效的值
        if question_type and question_type not in QUESTION_TYPES:
            logger.error(f"无效的问题类型: '{question_type}'. 有效类型: {QUESTION_TYPES}")
            return
        
        # 测试RAG API
        logger.info("1. 测试RAG API...")
        from rag_client import fetch_rag_results
        rag_response = await fetch_rag_results(query)
        if rag_response.get("success") is True:
            logger.info(f"RAG API成功! 返回了 {len(rag_response.get('results', []))} 条结果。")
        else:
            logger.error(f"RAG API失败: {rag_response.get('message', '未知错误')}")
            return
        
        # 格式化RAG结果
        logger.info("2. 格式化RAG结果...")
        from config import format_rag_results
        formatted_results = format_rag_results(rag_response.get("results", []))
        logger.info(f"RAG结果格式化成功!")
        
        # 生成提示词
        logger.info("3. 生成提示词...")
        if question_type is None:
            question_type = random.choice(QUESTION_TYPES)
        from config import SYSTEM_PROMPT, QUESTION_TYPE_PROMPTS
        system_prompt = SYSTEM_PROMPT.format(question_type=question_type)
        user_prompt = QUESTION_TYPE_PROMPTS[question_type].format(
            query=query, 
            rag_results=formatted_results
        )
        logger.info(f"提示词生成成功! 问题类型: {question_type}")
        
        # 调用DeepSeek API
        logger.info("4. 调用DeepSeek API...")
        from deepseek_client import generate_question
        answer = await generate_question(system_prompt, user_prompt)
        if answer.startswith("ERROR:"):
            logger.error(f"DeepSeek API失败: {answer[6:]}")
            return
        else:
            logger.info(f"DeepSeek API成功! 返回结果长度: {len(answer)}")
            logger.info(f"输出结果: \n{answer}")
        
        logger.info("测试完成！单个查询处理成功。")
    except Exception as e:
        logger.error(f"测试失败: {str(e)}")
        traceback.print_exc()


async def main(
    input_file: str, 
    output_file: str, 
    batch_size: int, 
    test_mode: bool = False, 
    test_query: Optional[str] = None,
    test_question_type: Optional[str] = None
) -> None:
    """Main entry point.
    
    Args:
        input_file: Path to the input JSONL file.
        output_file: Path to the output JSONL file.
        batch_size: Maximum number of concurrent requests.
        test_mode: Whether to run in test mode (just test a single query).
        test_query: Query to test in test mode.
        test_question_type: Question type to use for test query.
    """
    logger.info(f"Starting data generation pipeline")
    logger.info(f"Input file: {input_file}")
    logger.info(f"Output file: {output_file}")
    logger.info(f"Batch size: {batch_size}")
    
    # 如果是测试模式，只处理一个查询
    if test_mode:
        if test_query:
            await process_single_test_query(test_query, test_question_type)
        else:
            logger.error("测试模式需要提供查询参数。使用 --test-query 参数。")
        return
    
    # Load queries
    logger.info(f"Loading queries from {input_file}")
    
    # Create an example input file if it doesn't exist
    if not os.path.exists(input_file):
        logger.warning(f"Input file {input_file} does not exist. Creating an example input file.")
        example_queries = [
            "TensorFlow.js",
            "嵌入式Python开发",
            "TensorFlow Lite",
            "机器学习基础",
            "深度学习模型"
        ]
        with open(input_file, 'w', encoding='utf-8') as f:
            for query in example_queries:
                f.write(json.dumps({"query": query}, ensure_ascii=False) + '\n')
        logger.info(f"Created example input file with {len(example_queries)} queries")
    
    # 加载查询    
    queries = DataProcessor.load_queries(input_file)
    logger.info(f"Loaded {len(queries)} queries")
    
    if not queries:
        logger.error(f"No queries found in {input_file}")
        return
    
    # Process queries
    await process_queries(queries, output_file, batch_size)
    
    logger.info(f"Data generation pipeline completed")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate SFT training data for course question generation")
    parser.add_argument("--input", "-i", type=str, default=INPUT_FILE, help="Input JSONL file")
    parser.add_argument("--output", "-o", type=str, default=OUTPUT_FILE, help="Output JSONL file")
    parser.add_argument("--batch-size", "-b", type=int, default=MAX_CONCURRENT_REQUESTS, help="Batch size for concurrent processing")
    parser.add_argument("--test", action="store_true", help="运行测试模式，只处理一个查询")
    parser.add_argument("--test-query", type=str, help="测试模式下要处理的查询")
    parser.add_argument("--test-question-type", type=str, choices=QUESTION_TYPES, help="测试模式下使用的问题类型")
    parser.add_argument("--debug", action="store_true", help="启用调试日志级别")
    
    args = parser.parse_args()
    
    # 设置日志级别
    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)
        logger.info("已启用调试日志级别")
        # 打印命令行参数用于调试
        logger.debug(f"命令行参数: {args}")
    
    try:
        asyncio.run(main(
            args.input, 
            args.output, 
            args.batch_size, 
            args.test,
            args.test_query,
            args.test_question_type
        ))
    except KeyboardInterrupt:
        logger.info("用户中断，停止处理")
        sys.exit(0)
    except Exception as e:
        logger.error(f"程序执行出错: {str(e)}")
        traceback.print_exc()
        sys.exit(1) 