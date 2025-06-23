import os
import json
import torch
import numpy as np
from tqdm import tqdm
from sentence_transformers import SentenceTransformer
from typing import List, Dict, Tuple, Union, Optional
from pathlib import Path

class KGEmbedder:
    """知识图谱嵌入工具，负责为实体和关系生成嵌入向量"""
    
    def __init__(self, model_name: str = "bge-small-zh-v1.5"):
        """初始化知识图谱嵌入工具
        
        Args:
            model_name: 嵌入模型名称，默认使用中文BGE大模型
        """
        self.model_name = model_name
        print(f"正在加载嵌入模型: {model_name}...")
        self.model = SentenceTransformer(model_name)
        print("嵌入模型加载完成")
        
        # 使用GPU加速（如果可用）
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        self.model.to(self.device)
        print(f"使用设备: {self.device}")
        
    def generate_entity_embeddings(self, 
                                   kg_data: List[List[str]], 
                                   batch_size: int = 32) -> Dict[str, np.ndarray]:
        """为知识图谱中的实体生成嵌入向量
        
        Args:
            kg_data: 知识图谱数据，三元组列表
            batch_size: 批处理大小
            
        Returns:
            实体名称到嵌入向量的映射字典
        """
        print("正在为实体生成嵌入向量...")
        
        # 提取所有不重复的实体
        entities = set()
        for triple in kg_data:
            if len(triple) >= 3:
                subject, _, object_ = triple[:3]
                entities.add(subject)
                entities.add(object_)
                
        entities = list(entities)
        print(f"共找到 {len(entities)} 个不重复实体")
        
        # 生成嵌入向量
        embeddings = {}
        
        for i in tqdm(range(0, len(entities), batch_size), desc="生成实体嵌入"):
            batch = entities[i:i+batch_size]
            batch_embeddings = self.model.encode(batch, show_progress_bar=False)
            
            for j, entity in enumerate(batch):
                embeddings[entity] = batch_embeddings[j]
        
        print(f"已为 {len(embeddings)} 个实体生成嵌入向量")
        return embeddings
    
    def generate_relation_embeddings(self, 
                                    kg_data: List[List[str]], 
                                    batch_size: int = 32) -> Dict[str, np.ndarray]:
        """为知识图谱中的关系生成嵌入向量
        
        Args:
            kg_data: 知识图谱数据，三元组列表
            batch_size: 批处理大小
            
        Returns:
            关系名称到嵌入向量的映射字典
        """
        print("正在为关系生成嵌入向量...")
        
        # 提取所有不重复的关系
        relations = set()
        for triple in kg_data:
            if len(triple) >= 3:
                _, relation, _ = triple[:3]
                relations.add(relation)
                
        relations = list(relations)
        print(f"共找到 {len(relations)} 个不重复关系")
        
        # 生成嵌入向量
        embeddings = {}
        
        for i in tqdm(range(0, len(relations), batch_size), desc="生成关系嵌入"):
            batch = relations[i:i+batch_size]
            batch_embeddings = self.model.encode(batch, show_progress_bar=False)
            
            for j, relation in enumerate(batch):
                embeddings[relation] = batch_embeddings[j]
        
        print(f"已为 {len(embeddings)} 个关系生成嵌入向量")
        return embeddings
    
    def generate_triple_embeddings(self, 
                                  kg_data: List[List[str]], 
                                  batch_size: int = 32) -> Dict[str, np.ndarray]:
        """为知识图谱中的三元组生成嵌入向量
        
        Args:
            kg_data: 知识图谱数据，三元组列表
            batch_size: 批处理大小
            
        Returns:
            三元组文本到嵌入向量的映射字典
        """
        print("正在为三元组生成嵌入向量...")
        
        # 构建三元组文本
        triple_texts = []
        for triple in kg_data:
            if len(triple) >= 3:
                subject, relation, object_ = triple[:3]
                triple_text = f"{subject} {relation} {object_}"
                triple_texts.append(triple_text)
                
        print(f"共构建 {len(triple_texts)} 个三元组文本")
        
        # 生成嵌入向量
        embeddings = {}
        
        for i in tqdm(range(0, len(triple_texts), batch_size), desc="生成三元组嵌入"):
            batch = triple_texts[i:i+batch_size]
            batch_embeddings = self.model.encode(batch, show_progress_bar=False)
            
            for j, triple_text in enumerate(batch):
                embeddings[triple_text] = batch_embeddings[j]
        
        print(f"已为 {len(embeddings)} 个三元组生成嵌入向量")
        return embeddings
    
    def save_embeddings(self, 
                       embeddings: Dict[str, np.ndarray], 
                       output_file: str):
        """保存嵌入向量到文件
        
        Args:
            embeddings: 嵌入向量字典
            output_file: 输出文件路径
        """
        print(f"正在将嵌入向量保存到: {output_file}")
        
        # 确保输出目录存在
        os.makedirs(os.path.dirname(output_file), exist_ok=True)
        
        # 将嵌入向量转换为可序列化的格式
        serializable_embeddings = {}
        for key, embedding in embeddings.items():
            serializable_embeddings[key] = embedding.tolist()
            
        # 保存到文件
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(serializable_embeddings, f, ensure_ascii=False)
            
        print(f"嵌入向量已保存")
        
    def load_embeddings(self, input_file: str) -> Dict[str, np.ndarray]:
        """从文件加载嵌入向量
        
        Args:
            input_file: 输入文件路径
            
        Returns:
            嵌入向量字典
        """
        print(f"正在从文件加载嵌入向量: {input_file}")
        
        # 从文件加载
        with open(input_file, 'r', encoding='utf-8') as f:
            serializable_embeddings = json.load(f)
            
        # 将嵌入向量转换回numpy数组
        embeddings = {}
        for key, embedding_list in serializable_embeddings.items():
            embeddings[key] = np.array(embedding_list)
            
        print(f"已加载 {len(embeddings)} 个嵌入向量")
        return embeddings


