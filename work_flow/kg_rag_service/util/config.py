# 知识图谱流水线配置文件

# 默认配置
CONFIG = {
    # 目录配置
    "source_dir": "source_article",  # 源文档目录
    "segmented_dir": "segmented_output",  # 分段输出目录
    "cleaned_dir": "wash_dir",  # 清洗后输出目录
    "kg_dir": "knowledge_graph_data",  # 知识图谱数据目录
    
    # API配置
    "api_key": "sk-6bfa57029fbc44b9ac4eb38dd61bc695", # 替换为你的API密钥
    "model": "deepseek-chat",
    "temperature": 0.1,
    "batch_size": 16,
    "max_retries": 3,
    "max_concurrency": 5, # 最大并发请求数
    
    # 关系提取配置
    "relations_to_extract": [
        "是什么", "包含", "用途", "特点", "组成部分", 
        "执行步骤", "优点", "缺点", "属于", "比较",
        "工作原理", "使用方法", "示例"
    ],
    
    # Neo4j配置
    "use_neo4j": False,  # 是否使用Neo4j数据库
    "neo4j_uri": "bolt://localhost:7687",  # Neo4j连接URI
    "neo4j_user": "neo4j",  # Neo4j用户名
    "neo4j_password": "summer123456",  # Neo4j密码
    "neo4j_database": "knowledgegraphs",  # Neo4j数据库名称
} 