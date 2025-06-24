import os
import json
from typing import Dict, Any, List, Optional
from pathlib import Path

from fastapi import FastAPI, Query, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from pydantic import BaseModel
import numpy as np

from util.kg_rag import KGRAGService
from config import CONFIG

# 默认数据路径
DEFAULT_RAG_DATA_PATH = r"D:\summer_intern\Summer_intern\work_flow\kg_rag_service\doc\rag_data\processed"

# 创建FastAPI应用
app = FastAPI(
    title="知识图谱增强检索系统API",
    description="基于知识图谱的检索增强生成(RAG)系统API",
    version="1.0.0"
)

# 添加CORS中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 允许所有来源
    allow_credentials=True,
    allow_methods=["*"],  # 允许所有方法
    allow_headers=["*"],  # 允许所有头
)

# 初始化RAG服务
rag_service = KGRAGService()
is_service_initialized = False

# 响应模型
class RagResponse(BaseModel):
    query: str
    results: List[Dict[str, Any]]
    data_path: str
    success: bool
    message: str

def convert_numpy_types(obj):
    """递归转换numpy类型为Python标准类型"""
    if isinstance(obj, np.integer):
        return int(obj)
    elif isinstance(obj, np.floating):
        return float(obj)
    elif isinstance(obj, np.ndarray):
        return obj.tolist()
    elif isinstance(obj, dict):
        return {k: convert_numpy_types(v) for k, v in obj.items()}
    elif isinstance(obj, list) or isinstance(obj, tuple):
        return [convert_numpy_types(i) for i in obj]
    else:
        return obj

@app.get("/", response_model=Dict[str, str])
async def root():
    """API根路径，返回简单的欢迎信息"""
    return {"message": "欢迎使用知识图谱增强检索系统API"}

@app.get("/rag", response_model=RagResponse)
async def retrieve(
    query: str = Query(..., description="查询文本"),
    path: Optional[str] = Query(None, description="RAG数据路径，默认使用系统配置路径"),
    top_k: int = Query(CONFIG.get("top_k", 10), description="返回结果数量"),
    include_graph_context: bool = Query(True, description="是否包含图结构上下文"),
    context_depth: int = Query(CONFIG.get("depth", 3), description="图结构上下文搜索深度")
):
    """
    知识图谱增强检索接口
    
    - **query**: 查询文本
    - **path**: 可选的RAG数据路径
    - **top_k**: 返回结果数量
    - **include_graph_context**: 是否包含图结构上下文
    - **context_depth**: 图结构上下文搜索深度
    """
    global is_service_initialized
    
    # 确定数据路径
    data_path = path if path else DEFAULT_RAG_DATA_PATH
    
    # 检查路径是否存在
    if not os.path.exists(data_path):
        return RagResponse(
            query=query,
            results=[],
            data_path=data_path,
            success=False,
            message=f"指定的数据路径不存在: {data_path}"
        )
    
    # 如果数据路径变化或首次加载，则加载数据
    if not is_service_initialized:
        load_success = rag_service.load_rag_data(data_path)
        if not load_success:
            return RagResponse(
                query=query,
                results=[],
                data_path=data_path,
                success=False,
                message=f"从路径加载RAG数据失败: {data_path}"
            )
        is_service_initialized = True
    
    try:
        # 执行检索
        results = rag_service.retrieve(
            query=query,
            top_k=top_k,
            include_graph_context=include_graph_context,
            context_depth=context_depth
        )
        
        # 添加实体的出边和入边关系信息
        for result in results:
            entity = result.get("subject")
            if entity and entity in rag_service.graph:
                # 获取实体详细信息
                entity_info = rag_service.get_entity_info(entity)
                result["outgoing_relations"] = entity_info.get("outgoing_relations", [])
                result["incoming_relations"] = entity_info.get("incoming_relations", [])
        
        # 转换numpy类型为Python标准类型
        processed_results = convert_numpy_types(results)
        
        return RagResponse(
            query=query,
            results=processed_results,
            data_path=data_path,
            success=True,
            message="检索成功"
        )
    except Exception as e:
        import traceback
        error_trace = traceback.format_exc()
        print(f"错误详情: {error_trace}")
        
        return RagResponse(
            query=query,
            results=[],
            data_path=data_path,
            success=False,
            message=f"检索过程中发生错误: {str(e)}"
        )

if __name__ == "__main__":
    # 启动服务器
    uvicorn.run("RagApi:app", host="0.0.0.0", port=8000, reload=True)
