"""
Configuration settings for the data generation pipeline.
"""

import os
from typing import Dict, List, Any

# API Configuration
RAG_API_URL = "http://localhost:8000/rag"
DEEPSEEK_API_KEY = "sk-6bfa57029fbc44b9ac4eb38dd61bc695"
DEEPSEEK_BASE_URL = "https://api.deepseek.com"
DEEPSEEK_MODEL = "deepseek-chat"

# Concurrency settings
MAX_CONCURRENT_REQUESTS = 10

# Input/Output settings
INPUT_FILE = "input_queries.jsonl"
OUTPUT_FILE = "sft_dataset.jsonl"

# Prompt templates for different question types
SYSTEM_PROMPT = """你是一个专业的课程题目生成专家。请根据给定的信息生成高质量的{question_type}题目。"""

# 确保这里的值与命令行参数匹配
QUESTION_TYPES = [
    "选择题",
    "判断题", 
    "问答题", 
    "编程题"
]

# Format specifications for each question type
QUESTION_TYPE_PROMPTS = {
    "选择题": """
请根据以下信息生成一道选择题：

查询词：{query}
知识图谱信息：
{rag_results}

要求：
1. 生成一个与主题相关的选择题
2. 提供4个选项，只有1个正确答案
3. 解释为什么其他选项不正确
4. 输出JSON格式如下:
{{
    "type": "选择题",
    "question": "问题描述",
    "options": {{
        "A": "选项A",
        "B": "选项B",
        "C": "选项C",
        "D": "选项D"
    }},
    "answer": "正确选项字母",
    "explanation": "解析"
}}
""",
    "判断题": """
请根据以下信息生成一道判断题：

查询词：{query}
知识图谱信息：
{rag_results}

要求：
1. 生成一个与主题相关的判断题
2. 判断题的答案应该是"正确"或"错误"
3. 给出为什么判断正确或错误的解释
4. 输出JSON格式如下:
{{
    "type": "判断题",
    "statement": "判断陈述",
    "answer": "正确或错误",
    "explanation": "解析"
}}
""",
    "问答题": """
请根据以下信息生成一道问答题：

查询词：{query}
知识图谱信息：
{rag_results}

要求：
1. 生成一个与主题相关的开放式问答题
2. 提供一个标准答案
3. 输出JSON格式如下:
{{
    "type": "问答题",
    "question": "问题描述",
    "answer": "标准答案",
    "key_points": ["关键点1", "关键点2", "关键点3"]
}}
""",
    "编程题": """
请根据以下信息生成一道编程题：

查询词：{query}
知识图谱信息：
{rag_results}

要求：
1. 生成一个与主题相关的编程题
2. 提供问题描述、输入输出要求和示例
3. 提供解题思路和参考代码
4. 输出JSON格式如下:
{{
    "type": "编程题",
    "title": "题目标题",
    "description": "问题描述",
    "input_format": "输入格式说明",
    "output_format": "输出格式说明",
    "examples": [
        {{"input": "示例输入1", "output": "示例输出1"}},
        {{"input": "示例输入2", "output": "示例输出2"}}
    ],
    "solution_approach": "解题思路",
    "reference_code": "参考代码（可包含多种语言实现）"
}}
"""
}

def format_rag_results(results: List[Dict[str, Any]]) -> str:
    """Format RAG results into a readable string format for prompts."""
    formatted_text = ""
    
    for idx, result in enumerate(results, 1):
        formatted_text += f"知识点 {idx}: {result['subject']} {result['relation']} {result['object']}\n"
        
        if result.get("outgoing_relations"):
            formatted_text += "相关知识:\n"
            for rel in result["outgoing_relations"]:
                formatted_text += f"- {result['subject']} {rel['relation']} {rel['target']}\n"
        
        formatted_text += "\n"
    
    return formatted_text 