import os
import json
import glob
import numpy as np
from typing import List, Dict, Tuple, Union, Optional, Set, Any
from pathlib import Path
import faiss
from tqdm import tqdm
import torch
from sentence_transformers import SentenceTransformer
import networkx as nx

from kg_embedding import KGEmbedder
from kg_processor import KnowledgeGraphProcessor

class KGRAGGenerator:
    """知识图谱增强的RAG系统生成器，负责将知识图谱处理为RAG格式"""
    
    def __init__(self, embedder: KGEmbedder = None):
        """初始化知识图谱RAG生成器
        
        Args:
            embedder: 知识图谱嵌入工具，如果为None则创建默认工具
        """
        self.embedder = embedder if embedder else KGEmbedder()
        self.processor = KnowledgeGraphProcessor(self.embedder)
        
    def process_kg_file(self, kg_file_path: str, output_dir: str) -> bool:
        """处理单个知识图谱文件为RAG格式
        
        Args:
            kg_file_path: 知识图谱文件路径
            output_dir: 输出目录
            
        Returns:
            处理是否成功
        """
        print(f"处理知识图谱文件: {kg_file_path}")
        
        # 确保输出目录存在
        os.makedirs(output_dir, exist_ok=True)
        
        # 使用处理器进行处理
        success = self.processor.process_knowledge_graph_to_rag(kg_file_path, output_dir)
        
        return success
    
    def process_kg_directory(self, kg_dir: str, output_base_dir: str, pattern: str = "*_relations.json") -> Dict[str, bool]:
        """处理目录中的所有知识图谱文件为RAG格式
        
        Args:
            kg_dir: 知识图谱目录
            output_base_dir: 输出基础目录
            pattern: 文件匹配模式
            
        Returns:
            文件路径到处理结果的映射字典
        """
        print(f"处理知识图谱目录: {kg_dir}，文件模式: {pattern}")
        
        # 获取所有匹配的文件
        kg_files = glob.glob(os.path.join(kg_dir, pattern))
        
        if not kg_files:
            print(f"警告: 在 {kg_dir} 中未找到匹配 {pattern} 的文件")
            return {}
        
        print(f"找到 {len(kg_files)} 个知识图谱文件")
        
        # 处理每个文件
        results = {}
        
        for kg_file in kg_files:
            file_name = os.path.basename(kg_file)
            # 去除扩展名
            base_name = os.path.splitext(file_name)[0]
            
            # 如果文件名以"optimized_"开头，则去除
            if base_name.startswith("optimized_"):
                base_name = base_name[len("optimized_"):]
            
            # 如果文件名以"_relations"结尾，则去除
            if base_name.endswith("_relations"):
                base_name = base_name[:-len("_relations")]
            
            # 构建输出目录
            output_dir = os.path.join(output_base_dir, base_name)
            
            # 处理文件
            try:
                success = self.process_kg_file(kg_file, output_dir)
                results[kg_file] = success
            except Exception as e:
                print(f"处理 {kg_file} 时出错: {e}")
                results[kg_file] = False
        
        # 打印处理结果
        successful = sum(1 for success in results.values() if success)
        print(f"处理完成，成功: {successful}/{len(results)}")
        
        return results


class KGRAGService:
    """知识图谱增强的RAG服务，提供检索功能"""
    
    def __init__(self, model_name: str = "bge-small-zh-v1.5"):
        """初始化知识图谱RAG服务
        
        Args:
            model_name: 嵌入模型名称
        """
        self.model_name = model_name
        print(f"正在加载嵌入模型: {model_name}...")
        self.model = SentenceTransformer(model_name)
        
        # 使用GPU加速（如果可用）
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        self.model.to(self.device)
        print(f"使用设备: {self.device}")
        
        # 存储加载的数据
        self.loaded_data = {}
        self.entity_index = None
        self.relation_index = None
        self.triple_index = None
        
        # 图结构
        self.graph = nx.DiGraph()
        
        # 索引名称到ID的映射
        self.triple_texts = []
        self.entity_to_id = {}
        self.id_to_entity = {}
        self.relation_to_id = {}
        self.id_to_relation = {}
    
    def load_rag_data(self, input_dir: str) -> bool:
        """加载RAG数据
        
        Args:
            input_dir: 输入目录
            
        Returns:
            加载是否成功
        """
        print(f"加载RAG数据: {input_dir}")
        
        # 检查必要的文件是否存在
        required_files = [
            "entity_embeddings.json",
            "triple_embeddings.json",
            "graph_structure.json"
        ]
        
        for file in required_files:
            if not os.path.exists(os.path.join(input_dir, file)):
                print(f"错误: 缺少必要的文件 {file}")
                return False
        
        try:
            # 加载嵌入向量
            with open(os.path.join(input_dir, "entity_embeddings.json"), 'r', encoding='utf-8') as f:
                entity_embeddings_data = json.load(f)
                entity_embeddings = {k: np.array(v) for k, v in entity_embeddings_data.items()}
            
            with open(os.path.join(input_dir, "triple_embeddings.json"), 'r', encoding='utf-8') as f:
                triple_embeddings_data = json.load(f)
                triple_embeddings = {k: np.array(v) for k, v in triple_embeddings_data.items()}
            
            # 可选: 加载关系嵌入
            relation_embeddings = {}
            relation_embeddings_file = os.path.join(input_dir, "relation_embeddings.json")
            if os.path.exists(relation_embeddings_file):
                with open(relation_embeddings_file, 'r', encoding='utf-8') as f:
                    relation_embeddings_data = json.load(f)
                    relation_embeddings = {k: np.array(v) for k, v in relation_embeddings_data.items()}
            
            # 加载图结构
            with open(os.path.join(input_dir, "graph_structure.json"), 'r', encoding='utf-8') as f:
                graph_structure = json.load(f)
            
            # 加载映射
            with open(os.path.join(input_dir, "entity_mapping.json"), 'r', encoding='utf-8') as f:
                entity_mapping = json.load(f)
                self.entity_to_id = entity_mapping["entity_to_id"]
                self.id_to_entity = {int(k): v for k, v in entity_mapping["id_to_entity"].items()}
            
            if os.path.exists(os.path.join(input_dir, "relation_mapping.json")):
                with open(os.path.join(input_dir, "relation_mapping.json"), 'r', encoding='utf-8') as f:
                    relation_mapping = json.load(f)
                    self.relation_to_id = relation_mapping["relation_to_id"]
                    self.id_to_relation = {int(k): v for k, v in relation_mapping["id_to_relation"].items()}
            
            # 保存数据
            self.loaded_data = {
                "entity_embeddings": entity_embeddings,
                "relation_embeddings": relation_embeddings,
                "triple_embeddings": triple_embeddings,
                "graph_structure": graph_structure
            }
            
            # 构建图结构
            self.graph = nx.DiGraph()
            
            # 添加节点
            for node in graph_structure:
                self.graph.add_node(node, type="entity")
            
            # 添加边
            for node, connections in graph_structure.items():
                for successor in connections["successors"]:
                    self.graph.add_edge(node, successor["target"], relation=successor["relation"])
            
            # 存储三元组文本，以便索引检索
            self.triple_texts = list(triple_embeddings.keys())
            
            # 构建FAISS索引
            self._build_indices()
            
            print(f"RAG数据加载成功，包含 {len(entity_embeddings)} 个实体，{len(triple_embeddings)} 个三元组")
            return True
            
        except Exception as e:
            print(f"加载RAG数据时出错: {e}")
            return False
    
    def _build_indices(self):
        """构建FAISS索引"""
        print("构建FAISS索引...")
        
        # 构建实体索引
        if self.loaded_data["entity_embeddings"]:
            entity_vectors = np.array(list(self.loaded_data["entity_embeddings"].values())).astype('float32')
            entity_dim = entity_vectors.shape[1]
            
            self.entity_index = faiss.IndexFlatL2(entity_dim)
            self.entity_index.add(entity_vectors)
            print(f"实体索引构建完成，包含 {len(self.loaded_data['entity_embeddings'])} 个实体")
        
        # 构建关系索引
        if self.loaded_data["relation_embeddings"]:
            relation_vectors = np.array(list(self.loaded_data["relation_embeddings"].values())).astype('float32')
            relation_dim = relation_vectors.shape[1]
            
            self.relation_index = faiss.IndexFlatL2(relation_dim)
            self.relation_index.add(relation_vectors)
            print(f"关系索引构建完成，包含 {len(self.loaded_data['relation_embeddings'])} 个关系")
        
        # 构建三元组索引
        if self.loaded_data["triple_embeddings"]:
            triple_vectors = np.array(list(self.loaded_data["triple_embeddings"].values())).astype('float32')
            triple_dim = triple_vectors.shape[1]
            
            self.triple_index = faiss.IndexFlatL2(triple_dim)
            self.triple_index.add(triple_vectors)
            print(f"三元组索引构建完成，包含 {len(self.loaded_data['triple_embeddings'])} 个三元组")
    
    def retrieve(self, query: str, top_k: int = 5, include_graph_context: bool = True, context_depth: int = 1) -> List[Dict[str, Any]]:
        """检索与查询相关的知识
        
        Args:
            query: 查询文本
            top_k: 返回结果数量
            include_graph_context: 是否包含图结构上下文
            context_depth: 图结构上下文搜索深度
            
        Returns:
            检索结果列表
        """
        if not self.triple_index:
            print("错误: 索引未构建，请先加载RAG数据")
            return []
        
        print(f"检索查询: {query}")
        
        # 生成查询嵌入
        query_embedding = self.model.encode([query])[0].astype('float32')
        
        # 初始检索：基于三元组的语义相似度
        D, I = self.triple_index.search(np.array([query_embedding]), top_k)
        
        # 获取初始检索结果
        results = []
        for i, (dist, idx) in enumerate(zip(D[0], I[0])):
            if idx < len(self.triple_texts):
                triple_text = self.triple_texts[idx]
                similarity = 1.0 / (1.0 + dist)  # 转换距离为相似度
                
                # 解析三元组
                parts = triple_text.split(" ", 2)
                if len(parts) >= 3:
                    subject, relation, object_ = parts
                    
                    result = {
                        "triple": triple_text,
                        "subject": subject,
                        "relation": relation,
                        "object": object_,
                        "similarity": similarity,
                        "rank": i + 1
                    }
                    
                    results.append(result)
        
        # 如果需要包含图结构上下文
        if include_graph_context and results:
            # 找出检索到的实体
            retrieved_entities = set()
            for result in results:
                retrieved_entities.add(result["subject"])
                retrieved_entities.add(result["object"])
            
            # 为每个实体获取图结构上下文
            context_triples = []
            for entity in retrieved_entities:
                entity_context = self._extract_graph_context(entity, depth=context_depth)
                context_triples.extend(entity_context)
            
            # 添加上下文三元组到结果中（去重）
            existing_triples = set(r["triple"] for r in results)
            
            for triple in context_triples:
                triple_text = f"{triple[0]} {triple[1]} {triple[2]}"
                
                if triple_text not in existing_triples:
                    # 计算与查询的相似度
                    triple_embedding = self.model.encode([triple_text])[0].astype('float32')
                    dist = np.linalg.norm(triple_embedding - query_embedding)
                    similarity = 1.0 / (1.0 + dist)
                    
                    result = {
                        "triple": triple_text,
                        "subject": triple[0],
                        "relation": triple[1],
                        "object": triple[2],
                        "similarity": similarity,
                        "rank": len(results) + 1,
                        "is_context": True
                    }
                    
                    results.append(result)
                    existing_triples.add(triple_text)
        
        # 根据相似度重新排序
        results.sort(key=lambda x: x["similarity"], reverse=True)
        
        # 更新排名
        for i, result in enumerate(results):
            result["rank"] = i + 1
        
        return results[:top_k]
    
    def _extract_graph_context(self, entity: str, depth: int = 1) -> List[List[str]]:
        """提取实体的图结构上下文
        
        Args:
            entity: 实体名称
            depth: 搜索深度
            
        Returns:
            与实体相关的三元组列表
        """
        context_triples = []
        
        # 检查实体是否在图中
        if entity not in self.graph:
            return context_triples
        
        # BFS搜索相关节点
        visited = set([entity])
        queue = [(entity, 0)]  # (节点, 深度)
        
        while queue:
            current_entity, current_depth = queue.pop(0)
            
            # 如果达到最大深度，则不再继续搜索
            if current_depth >= depth:
                continue
            
            # 获取出边（当前实体作为主体）
            for neighbor in self.graph.successors(current_entity):
                # 添加三元组
                edge_data = self.graph.get_edge_data(current_entity, neighbor)
                relation = edge_data['relation']
                context_triples.append([current_entity, relation, neighbor])
                
                # 如果邻居节点未访问过，则加入队列
                if neighbor not in visited:
                    visited.add(neighbor)
                    queue.append((neighbor, current_depth + 1))
            
            # 获取入边（当前实体作为客体）
            for predecessor in self.graph.predecessors(current_entity):
                # 添加三元组
                edge_data = self.graph.get_edge_data(predecessor, current_entity)
                relation = edge_data['relation']
                context_triples.append([predecessor, relation, current_entity])
                
                # 如果前驱节点未访问过，则加入队列
                if predecessor not in visited:
                    visited.add(predecessor)
                    queue.append((predecessor, current_depth + 1))
        
        return context_triples
    
    def get_entity_info(self, entity: str) -> Dict[str, Any]:
        """获取实体的详细信息
        
        Args:
            entity: 实体名称
            
        Returns:
            实体信息字典
        """
        if entity not in self.graph:
            return {"error": f"实体 '{entity}' 不存在"}
        
        # 获取实体相关的三元组
        outgoing = []
        for successor in self.graph.successors(entity):
            edge_data = self.graph.get_edge_data(entity, successor)
            relation = edge_data['relation']
            outgoing.append({
                "relation": relation,
                "target": successor
            })
        
        incoming = []
        for predecessor in self.graph.predecessors(entity):
            edge_data = self.graph.get_edge_data(predecessor, entity)
            relation = edge_data['relation']
            incoming.append({
                "source": predecessor,
                "relation": relation
            })
        
        # 统计关系类型
        relation_types = set()
        for edge in outgoing:
            relation_types.add(edge["relation"])
        for edge in incoming:
            relation_types.add(edge["relation"])
        
        return {
            "entity": entity,
            "outgoing_relations": outgoing,
            "incoming_relations": incoming,
            "relation_types": list(relation_types),
            "degree": len(outgoing) + len(incoming),
            "out_degree": len(outgoing),
            "in_degree": len(incoming)
        }

