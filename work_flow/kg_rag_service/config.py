# 知识图谱流水线配置文件

import os
from pathlib import Path

# 获取当前文件的绝对路径
current_file = Path(__file__).resolve()

# 获取项目根目录（假设是当前文件的上三级目录）
project_root = current_file.parent
print(project_root)

# 配置字典
CONFIG = {
    # 目录配置
    "source_dir": os.path.join(project_root, "source_article"),  # 源文档目录
    "segmented_dir": os.path.join(project_root, "doc","segmented"),    # 分段后文本存储目录
    "cleaned_dir": os.path.join(project_root, "doc","cleaned"),        # 清洗后文本存储目录
    "kg_dir": os.path.join(project_root, "doc","knowledge_graph"),     # 知识图谱存储目录
    "rag_dir": os.path.join(project_root, "doc","rag_data"),           # RAG数据存储目录
    
    # 文本分割配置
    "chunk_size": 500,       # 文本块大小
    "chunk_overlap": 50,     # 文本块重叠大小
    
    # 关系提取配置
    "api_key": "sk-6bfa57029fbc44b9ac4eb38dd61bc695",           # OpenAI API密钥
    "model": "deepseek-chat",  # 使用的模型
    "temperature": 0.0,      # 温度参数（越低越确定性）
    "batch_size": 10,        # 批处理大小
    "max_retries": 1,        # 最大重试次数
    "max_concurrency": 5,    # 最大并发请求数
    "request_timeout": 180,  # 请求超时时间（秒）
    
    # 要提取的关系类型
    "relations_to_extract": [
        "定义",
        "包含",
        "属于",
        "是",
        "特点",
        "用途",
        "组成部分",
        "步骤",
        "优点",
        "缺点",
        "区别",
        "相似",
        "示例",
        "条件",
        "结果"
    ],
    
    # Neo4j配置
    "use_neo4j": True,      # 是否使用Neo4j
    "neo4j_uri": "bolt://localhost:7687",  # Neo4j连接URI
    "neo4j_user": "neo4j",   # Neo4j用户名
    "neo4j_password": "summer123456",  # Neo4j密码
    "neo4j_database": None,  # 设置为None，使用Neo4jImporter自动生成的时间戳数据库名称
    
    # 知识图谱嵌入配置
    "use_embeddings": True,  # 是否生成嵌入向量
    "embedding_model": "bge-small-zh-v1.5",  # 嵌入模型名称
    "embedding_batch_size": 32,  # 嵌入批处理大小
    
    # RAG配置
    "use_rag": True,  # 是否处理为RAG格式


    "depth" : 3,
    "top_k" : 1,
}

def ensure_directories_exist():
    """确保所有配置的目录都存在，在需要时调用此函数"""
    for dir_path in [CONFIG["source_dir"], CONFIG["segmented_dir"], CONFIG["cleaned_dir"], CONFIG["kg_dir"], CONFIG["rag_dir"]]:
        os.makedirs(dir_path, exist_ok=True)

# 注意：导入此模块时不会自动创建目录，需要显式调用 ensure_directories_exist() 函数 