import os
import json
import networkx as nx
import numpy as np
from typing import List, Dict, Tuple, Union, Optional, Set
from pathlib import Path
import faiss
from tqdm import tqdm

from util.kg_embedding import KGEmbedder

class KnowledgeGraphProcessor:
    """知识图谱处理器，负责将知识图谱转化为RAG系统可用的格式"""
    
    def __init__(self, embedder: KGEmbedder = None):
        """初始化知识图谱处理器
        
        Args:
            embedder: 知识图谱嵌入工具，如果为None则创建默认工具
        """
        self.embedder = embedder if embedder else KGEmbedder()
        self.graph = nx.DiGraph()  # 有向图
        self.entity_to_id = {}  # 实体到ID的映射
        self.id_to_entity = {}  # ID到实体的映射
        self.relation_to_id = {}  # 关系到ID的映射
        self.id_to_relation = {}  # ID到关系的映射
        
        # 嵌入向量
        self.entity_embeddings = {}
        self.relation_embeddings = {}
        self.triple_embeddings = {}
        
        # FAISS索引
        self.entity_index = None
        self.relation_index = None
        self.triple_index = None
        
    def load_knowledge_graph(self, kg_file_path: str) -> List[List[str]]:
        """加载知识图谱数据
        
        Args:
            kg_file_path: 知识图谱文件路径
            
        Returns:
            知识图谱三元组列表
        """
        print(f"正在加载知识图谱数据: {kg_file_path}")
        
        # 从文件加载
        with open(kg_file_path, 'r', encoding='utf-8') as f:
            kg_data = json.load(f)
        
        print(f"已加载 {len(kg_data)} 条知识图谱关系")
        return kg_data
    
    def build_graph(self, kg_data: List[List[str]]):
        """构建知识图谱网络
        
        Args:
            kg_data: 知识图谱三元组列表
        """
        print("正在构建知识图谱网络...")
        
        # 重置图结构
        self.graph = nx.DiGraph()
        
        # 为实体和关系分配ID
        entity_set = set()
        relation_set = set()
        
        for triple in kg_data:
            if len(triple) >= 3:
                subject, relation, object_ = triple[:3]
                entity_set.add(subject)
                entity_set.add(object_)
                relation_set.add(relation)
        
        # 构建映射
        self.entity_to_id = {entity: i for i, entity in enumerate(sorted(entity_set))}
        self.id_to_entity = {i: entity for entity, i in self.entity_to_id.items()}
        
        self.relation_to_id = {relation: i for i, relation in enumerate(sorted(relation_set))}
        self.id_to_relation = {i: relation for relation, i in self.relation_to_id.items()}
        
        # 构建图结构
        for triple in kg_data:
            if len(triple) >= 3:
                subject, relation, object_ = triple[:3]
                
                # 添加节点
                if not self.graph.has_node(subject):
                    self.graph.add_node(subject, type="entity")
                    
                if not self.graph.has_node(object_):
                    self.graph.add_node(object_, type="entity")
                
                # 添加边
                self.graph.add_edge(subject, object_, relation=relation)
        
        print(f"知识图谱网络构建完成，包含 {self.graph.number_of_nodes()} 个节点和 {self.graph.number_of_edges()} 条边")
    
    def generate_embeddings(self, kg_data: List[List[str]], batch_size: int = 32):
        """生成知识图谱嵌入
        
        Args:
            kg_data: 知识图谱三元组列表
            batch_size: 批处理大小
        """
        print("正在生成知识图谱嵌入...")
        
        # 生成实体嵌入
        self.entity_embeddings = self.embedder.generate_entity_embeddings(kg_data, batch_size)
        
        # 生成关系嵌入
        self.relation_embeddings = self.embedder.generate_relation_embeddings(kg_data, batch_size)
        
        # 生成三元组嵌入
        self.triple_embeddings = self.embedder.generate_triple_embeddings(kg_data, batch_size)
    
    def build_faiss_index(self):
        """构建FAISS索引，用于快速检索"""
        print("正在构建FAISS索引...")
        
        # 构建实体索引
        if self.entity_embeddings:
            entity_vectors = np.array(list(self.entity_embeddings.values())).astype('float32')
            entity_dim = entity_vectors.shape[1]
            
            self.entity_index = faiss.IndexFlatL2(entity_dim)
            self.entity_index.add(entity_vectors)
            print(f"实体索引构建完成，包含 {len(self.entity_embeddings)} 个实体")
        
        # 构建关系索引
        if self.relation_embeddings:
            relation_vectors = np.array(list(self.relation_embeddings.values())).astype('float32')
            relation_dim = relation_vectors.shape[1]
            
            self.relation_index = faiss.IndexFlatL2(relation_dim)
            self.relation_index.add(relation_vectors)
            print(f"关系索引构建完成，包含 {len(self.relation_embeddings)} 个关系")
        
        # 构建三元组索引
        if self.triple_embeddings:
            triple_vectors = np.array(list(self.triple_embeddings.values())).astype('float32')
            triple_dim = triple_vectors.shape[1]
            
            self.triple_index = faiss.IndexFlatL2(triple_dim)
            self.triple_index.add(triple_vectors)
            print(f"三元组索引构建完成，包含 {len(self.triple_embeddings)} 个三元组")
    
    def extract_graph_context(self, entity: str, depth: int = 1) -> List[List[str]]:
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
    
    def retrieve_relevant_entities(self, query: str, top_k: int = 5) -> List[Tuple[str, float]]:
        """检索与查询相关的实体
        
        Args:
            query: 查询文本
            top_k: 返回结果数量
            
        Returns:
            实体名称和相似度分数的列表
        """
        if not self.entity_index:
            print("错误: 实体索引未构建，请先调用build_faiss_index方法")
            return []
        
        # 生成查询嵌入
        query_embedding = self.embedder.model.encode([query])[0].astype('float32')
        
        # 执行检索
        D, I = self.entity_index.search(np.array([query_embedding]), top_k)
        
        # 获取结果
        results = []
        for i, (dist, idx) in enumerate(zip(D[0], I[0])):
            if idx < len(self.id_to_entity):
                entity = self.id_to_entity[idx]
                similarity = 1.0 / (1.0 + dist)  # 转换距离为相似度
                results.append((entity, similarity))
        
        return results
    
    def retrieve_relevant_relations(self, query: str, top_k: int = 5) -> List[Tuple[str, float]]:
        """检索与查询相关的关系
        
        Args:
            query: 查询文本
            top_k: 返回结果数量
            
        Returns:
            关系名称和相似度分数的列表
        """
        if not self.relation_index:
            print("错误: 关系索引未构建，请先调用build_faiss_index方法")
            return []
        
        # 生成查询嵌入
        query_embedding = self.embedder.model.encode([query])[0].astype('float32')
        
        # 执行检索
        D, I = self.relation_index.search(np.array([query_embedding]), top_k)
        
        # 获取结果
        results = []
        for i, (dist, idx) in enumerate(zip(D[0], I[0])):
            if idx < len(self.id_to_relation):
                relation = self.id_to_relation[idx]
                similarity = 1.0 / (1.0 + dist)  # 转换距离为相似度
                results.append((relation, similarity))
        
        return results
    
    def retrieve_relevant_triples(self, query: str, top_k: int = 5) -> List[Tuple[str, float]]:
        """检索与查询相关的三元组
        
        Args:
            query: 查询文本
            top_k: 返回结果数量
            
        Returns:
            三元组文本和相似度分数的列表
        """
        if not self.triple_index:
            print("错误: 三元组索引未构建，请先调用build_faiss_index方法")
            return []
        
        # 生成查询嵌入
        query_embedding = self.embedder.model.encode([query])[0].astype('float32')
        
        # 执行检索
        D, I = self.triple_index.search(np.array([query_embedding]), top_k)
        
        # 获取结果
        results = []
        triple_texts = list(self.triple_embeddings.keys())
        for i, (dist, idx) in enumerate(zip(D[0], I[0])):
            if idx < len(triple_texts):
                triple_text = triple_texts[idx]
                similarity = 1.0 / (1.0 + dist)  # 转换距离为相似度
                results.append((triple_text, similarity))
        
        return results
    
    def save_processed_data(self, output_dir: str):
        """保存处理后的数据
        
        Args:
            output_dir: 输出目录
        """
        print(f"正在保存处理后的数据到: {output_dir}")
        
        # 确保输出目录存在
        os.makedirs(output_dir, exist_ok=True)
        
        # 保存实体映射
        with open(f"{output_dir}/entity_mapping.json", 'w', encoding='utf-8') as f:
            json.dump({
                "entity_to_id": self.entity_to_id,
                "id_to_entity": {str(k): v for k, v in self.id_to_entity.items()}
            }, f, ensure_ascii=False, indent=2)
        
        # 保存关系映射
        with open(f"{output_dir}/relation_mapping.json", 'w', encoding='utf-8') as f:
            json.dump({
                "relation_to_id": self.relation_to_id,
                "id_to_relation": {str(k): v for k, v in self.id_to_relation.items()}
            }, f, ensure_ascii=False, indent=2)
        
        # 保存图结构（邻接表）
        adjacency_dict = {}
        for node in self.graph.nodes():
            adjacency_dict[node] = {
                "successors": [],
                "predecessors": []
            }
            
            # 获取出边
            for successor in self.graph.successors(node):
                edge_data = self.graph.get_edge_data(node, successor)
                adjacency_dict[node]["successors"].append({
                    "target": successor,
                    "relation": edge_data["relation"]
                })
            
            # 获取入边
            for predecessor in self.graph.predecessors(node):
                edge_data = self.graph.get_edge_data(predecessor, node)
                adjacency_dict[node]["predecessors"].append({
                    "source": predecessor,
                    "relation": edge_data["relation"]
                })
        
        with open(f"{output_dir}/graph_structure.json", 'w', encoding='utf-8') as f:
            json.dump(adjacency_dict, f, ensure_ascii=False, indent=2)
        
        # 保存嵌入向量
        if self.entity_embeddings:
            self.embedder.save_embeddings(self.entity_embeddings, f"{output_dir}/entity_embeddings.json")
        
        if self.relation_embeddings:
            self.embedder.save_embeddings(self.relation_embeddings, f"{output_dir}/relation_embeddings.json")
        
        if self.triple_embeddings:
            self.embedder.save_embeddings(self.triple_embeddings, f"{output_dir}/triple_embeddings.json")
        
        print(f"处理后的数据已保存到: {output_dir}")
    
    def load_processed_data(self, input_dir: str):
        """加载处理后的数据
        
        Args:
            input_dir: 输入目录
        """
        print(f"正在加载处理后的数据: {input_dir}")
        
        # 加载实体映射
        entity_mapping_file = f"{input_dir}/entity_mapping.json"
        if os.path.exists(entity_mapping_file):
            with open(entity_mapping_file, 'r', encoding='utf-8') as f:
                entity_mapping = json.load(f)
                self.entity_to_id = entity_mapping["entity_to_id"]
                self.id_to_entity = {int(k): v for k, v in entity_mapping["id_to_entity"].items()}
        
        # 加载关系映射
        relation_mapping_file = f"{input_dir}/relation_mapping.json"
        if os.path.exists(relation_mapping_file):
            with open(relation_mapping_file, 'r', encoding='utf-8') as f:
                relation_mapping = json.load(f)
                self.relation_to_id = relation_mapping["relation_to_id"]
                self.id_to_relation = {int(k): v for k, v in relation_mapping["id_to_relation"].items()}
        
        # 加载图结构
        graph_structure_file = f"{input_dir}/graph_structure.json"
        if os.path.exists(graph_structure_file):
            with open(graph_structure_file, 'r', encoding='utf-8') as f:
                adjacency_dict = json.load(f)
                
                # 重建图
                self.graph = nx.DiGraph()
                
                # 添加节点
                for node in adjacency_dict:
                    self.graph.add_node(node, type="entity")
                
                # 添加边
                for node, connections in adjacency_dict.items():
                    for successor in connections["successors"]:
                        self.graph.add_edge(node, successor["target"], relation=successor["relation"])
        
        # 加载嵌入向量
        entity_embeddings_file = f"{input_dir}/entity_embeddings.json"
        if os.path.exists(entity_embeddings_file):
            self.entity_embeddings = self.embedder.load_embeddings(entity_embeddings_file)
        
        relation_embeddings_file = f"{input_dir}/relation_embeddings.json"
        if os.path.exists(relation_embeddings_file):
            self.relation_embeddings = self.embedder.load_embeddings(relation_embeddings_file)
        
        triple_embeddings_file = f"{input_dir}/triple_embeddings.json"
        if os.path.exists(triple_embeddings_file):
            self.triple_embeddings = self.embedder.load_embeddings(triple_embeddings_file)
        
        print(f"处理后的数据加载完成")
        
        # 如果加载了嵌入向量，则重建索引
        if self.entity_embeddings or self.relation_embeddings or self.triple_embeddings:
            self.build_faiss_index()
    
    def process_knowledge_graph_to_rag(self, kg_file_path: str, output_dir: str):
        """处理知识图谱为RAG系统可用的格式
        
        Args:
            kg_file_path: 知识图谱文件路径
            output_dir: 输出目录
        """
        print(f"正在处理知识图谱为RAG系统格式: {kg_file_path}")
        
        # 加载知识图谱
        kg_data = self.load_knowledge_graph(kg_file_path)
        
        # 构建图结构
        self.build_graph(kg_data)
        
        # 生成嵌入
        self.generate_embeddings(kg_data)
        
        # 构建索引
        self.build_faiss_index()
        
        # 保存处理后的数据
        self.save_processed_data(output_dir)
        
        print(f"知识图谱已处理为RAG系统格式，结果保存在: {output_dir}")
        return True

