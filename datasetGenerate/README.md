# SFT Dataset Generator for Course Question Generation

This pipeline generates a supervised fine-tuning (SFT) dataset for training a model to create course questions in different formats based on search queries and RAG (Retrieval-Augmented Generation) results.

## Overview

The pipeline takes a list of search queries from a JSONL file, retrieves relevant knowledge graph information using a RAG API, and then uses DeepSeek's API to generate high-quality course questions in four formats:

1. Multiple choice questions
2. True/false questions
3. Q&A questions
4. Programming questions

The generated dataset consists of question-answer pairs where:
- **Question**: Includes the search query, RAG results, and prompting instructions
- **Answer**: Contains the generated question in JSON format

## Requirements

```
openai>=1.0.0
aiohttp>=3.8.0
```

## Directory Structure

```
.
├── README.md           # This documentation
├── config.py           # Configuration settings (API URLs, prompts, etc.)
├── rag_client.py       # Client for interacting with the RAG API
├── deepseek_client.py  # Client for interacting with the DeepSeek API
├── data_processor.py   # Processes and formats input/output data
├── main.py             # Main script to run the pipeline
├── input_queries.jsonl # Input file with search queries
└── sft_dataset.jsonl   # Output dataset file
```

## Input Format

The input file should be a JSONL file where each line is either:

1. A string query:
```
"TensorFlow.js"
```

2. Or a JSON object with a "query" field:
```json
{"query": "TensorFlow.js"}
```

## Output Format

The output file is a JSONL file where each line contains a question-answer pair:

```json
{
  "question": "请根据以下信息生成一道选择题：\n\n查询词：TensorFlow.js\n知识图谱信息：...",
  "answer": "{\"type\": \"选择题\", \"question\": \"TensorFlow.js主要用于哪种环境下的深度学习开发？\", ...}",
  "metadata": {
    "query": "TensorFlow.js",
    "question_type": "选择题"
  }
}
```

## Configuration

You can modify the following in `config.py`:
- API URLs and keys
- Question type prompts
- System prompt
- Concurrency settings
- Input/output file paths

## Usage

1. Create an input JSONL file with your search queries (or use the default example file)
2. Run the pipeline:

```bash
python main.py --input input_queries.jsonl --output sft_dataset.jsonl --batch-size 5
```

### Command-line Arguments

- `--input`, `-i`: Input JSONL file path
- `--output`, `-o`: Output JSONL file path
- `--batch-size`, `-b`: Number of concurrent requests to process

If the input file does not exist, an example file will be created automatically.

## RAG API

The RAG API should be running at the configured URL (`http://localhost:8000/rag` by default) and accept GET requests with a `query` parameter.

Example request:
```
GET http://localhost:8000/rag?query=TensorFlow.js
```

The API should return results in the following JSON format:
```json
{
    "query": "TensorFlow.js",
    "results": [
        {
            "triple": "...",
            "subject": "...",
            "relation": "...",
            "object": "...",
            "similarity": 0.8,
            "rank": 1,
            "outgoing_relations": [
                {"relation": "...", "target": "..."},
                ...
            ],
            "incoming_relations": []
        }
    ],
    "success": true,
    "message": "检索成功"
}
``` 