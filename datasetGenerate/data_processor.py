"""
Process and format input/output data for the data generation pipeline.
"""

import json
import logging
import random
from typing import Dict, Any, List, Tuple, Optional
import asyncio
import os
import re

from config import (
    QUESTION_TYPES, 
    QUESTION_TYPE_PROMPTS, 
    SYSTEM_PROMPT,
    format_rag_results
)
from rag_client import fetch_rag_results
from deepseek_client import generate_question


class DataProcessor:
    """Process and format data for the data generation pipeline."""
    
    @staticmethod
    def parse_json_from_answer(answer: str) -> Dict[str, Any]:
        """从DeepSeek回答中解析JSON对象。
        
        Args:
            answer: DeepSeek返回的原始答案文本。
            
        Returns:
            解析后的JSON对象。如果解析失败，返回原始文本。
        """
        # 尝试提取JSON对象
        try:
            # 尝试直接解析整个文本
            return json.loads(answer)
        except json.JSONDecodeError:
            # 如果直接解析失败，尝试从文本中提取JSON部分
            try:
                # 寻找以{开始，以}结束的部分
                json_pattern = r'({[\s\S]*})'
                match = re.search(json_pattern, answer)
                if match:
                    json_str = match.group(1)
                    return json.loads(json_str)
                else:
                    logging.warning("无法从回答中提取JSON对象")
                    return {"raw_text": answer}  # 返回原始文本
            except json.JSONDecodeError as e:
                logging.warning(f"JSON解析错误: {e}")
                return {"raw_text": answer}  # 返回原始文本
    
    @staticmethod
    async def process_query(query: str, question_type: Optional[str] = None) -> Dict[str, Any]:
        """Process a single query.
        
        Args:
            query: The search query.
            question_type: The type of question to generate. If None, a random type will be selected.
            
        Returns:
            Dictionary containing the processed data.
        """
        try:
            # Fetch RAG results
            rag_response = await fetch_rag_results(query)
            results = rag_response.get("results", [])
            
            if not results:
                logging.warning(f"No RAG results for query '{query}'")
            
            # Format RAG results for prompts
            formatted_results = format_rag_results(results)
            
            # Select question type if not specified
            if question_type is None:
                question_type = random.choice(QUESTION_TYPES)
            
            # Format prompts
            system_prompt = SYSTEM_PROMPT.format(question_type=question_type)
            user_prompt = QUESTION_TYPE_PROMPTS[question_type].format(
                query=query,
                rag_results=formatted_results
            )
            
            # Generate question
            answer = await generate_question(system_prompt, user_prompt)
            
            # Check if the answer is an error message
            if answer.startswith("ERROR:"):
                raise Exception(answer[6:])  # Strip "ERROR: " prefix
            
            # 处理模型返回的答案，如果是JSON格式，解析并重新格式化
            try:
                # 尝试解析答案中的JSON
                parsed_answer = DataProcessor.parse_json_from_answer(answer)
                
                # 如果成功解析为JSON，将其转换为格式化的JSON字符串
                if "raw_text" not in parsed_answer:
                    formatted_answer = json.dumps(parsed_answer, ensure_ascii=False, indent=2)
                else:
                    formatted_answer = answer  # 保持原始文本
            except Exception as e:
                logging.warning(f"格式化JSON答案出错: {e}")
                formatted_answer = answer  # 如果出错，保持原始文本
            
            # 记录生成的内容以便调试
            logging.debug(f"Generated formatted answer for query '{query}': {formatted_answer}")
            
            return {
                "query": query,
                "question_type": question_type,
                "rag_response": rag_response,
                "formatted_results": formatted_results,
                "system_prompt": system_prompt,
                "user_prompt": user_prompt,
                "answer": formatted_answer  # 使用格式化后的答案
            }
        except Exception as e:
            error_msg = str(e)
            logging.error(f"Error processing query '{query}': {error_msg}")
            # 重新抛出异常，但提供更明确的错误信息
            raise Exception(f"处理查询 '{query}' 时出错: {error_msg}")
    
    @staticmethod
    def load_queries(input_file: str) -> List[str]:
        """Load queries from a JSONL file.
        
        Args:
            input_file: Path to the input file.
            
        Returns:
            List of queries.
        """
        queries = []
        try:
            with open(input_file, 'r', encoding='utf-8') as f:
                for line_num, line in enumerate(f, 1):
                    line = line.strip()
                    if not line:
                        continue
                    
                    try:
                        # Try parsing as JSON first
                        data = json.loads(line)
                        query = data.get("query", "")
                    except json.JSONDecodeError:
                        # If it's not valid JSON, treat the line as the query
                        query = line
                    
                    if query:
                        queries.append(query)
                    else:
                        logging.warning(f"在第 {line_num} 行找到空查询，已跳过")
        except Exception as e:
            logging.error(f"Error loading queries from {input_file}: {str(e)}")
            raise
        
        return queries
    
    @staticmethod
    def save_results(results: List[Dict[str, Any]], output_file: str) -> None:
        """Save results to a JSONL file.
        
        Args:
            results: List of result dictionaries.
            output_file: Path to the output file.
        """
        try:
            os.makedirs(os.path.dirname(output_file) or '.', exist_ok=True)
            
            # 创建一个单独的文本文件而不是JSONL，以保持格式
            with open(output_file, 'w', encoding='utf-8') as f:
                for idx, result in enumerate(results):
                    # 我们将使用Markdown格式分隔每个样本
                    if idx > 0:
                        f.write("\n\n---\n\n")  # 样本之间的分隔符
                    
                    f.write(f"## 样本 {idx+1}\n\n")
                    
                    # 写入问题部分
                    f.write("### 问题\n\n```\n")
                    f.write(result["user_prompt"])
                    f.write("\n```\n\n")
                    
                    # 写入答案部分
                    f.write("### 答案\n\n```json\n")
                    f.write(result["answer"])
                    f.write("\n```\n\n")
                    
                    # 写入元数据
                    f.write("### 元数据\n\n```json\n")
                    metadata = {
                        "query": result["query"],
                        "question_type": result["question_type"]
                    }
                    f.write(json.dumps(metadata, ensure_ascii=False, indent=2))
                    f.write("\n```\n")
            
            # 同时保存一个标准的JSONL文件以供处理
            jsonl_output_file = output_file + ".jsonl"
            with open(jsonl_output_file, 'w', encoding='utf-8') as f:
                for result in results:
                    qa_pair = {
                        "question": result["user_prompt"],
                        "answer": result["answer"],
                        "metadata": {
                            "query": result["query"],
                            "question_type": result["question_type"]
                        }
                    }
                    f.write(json.dumps(qa_pair, ensure_ascii=False) + '\n')
            
            logging.info(f"保存了可读格式文件到 {output_file} 和JSONL格式到 {jsonl_output_file}")
        except Exception as e:
            logging.error(f"Error saving results to {output_file}: {str(e)}")
            raise


async def process_single_query(query: str, question_type: Optional[str] = None) -> Dict[str, Any]:
    """Process a single query.
    
    Args:
        query: The search query.
        question_type: The type of question to generate. If None, a random type will be selected.
        
    Returns:
        Dictionary containing the processed data.
    """
    return await DataProcessor.process_query(query, question_type) 