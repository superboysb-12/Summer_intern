import json
import networkx as nx
from collections import defaultdict
import os
import glob
from pathlib import Path

def process_knowledge_graph(input_dir='knowledge_graph_data', output_dir='knowledge_graph_data/output', pattern='*_relations.json'):
    """
    处理知识图谱关系文件
    
    参数:
        input_dir: 输入文件目录
        output_dir: 输出文件目录
        pattern: 文件匹配模式
    """
    # 获取所有关系文件
    print("正在查找关系文件...")
    relation_files = glob.glob(f'{input_dir}/{pattern}')
    
    if not relation_files:
        print(f"错误：在 {input_dir} 中未找到匹配 {pattern} 的文件！")
        return
    
    print(f"找到 {len(relation_files)} 个关系文件")
    
    # 确保输出目录存在
    os.makedirs(output_dir, exist_ok=True)
    
    # 处理每个文件
    for file_path in relation_files:
        file_name = os.path.basename(file_path)
        print(f"\n正在处理 {file_name}...")
        
        # 加载知识图谱数据
        with open(file_path, 'r', encoding='utf-8') as f:
            relations_list = json.load(f)
        
        # 将列表格式转换为字典格式，以便于处理
        kg_data = {"关系": relations_list}
        
        # 分析图结构
        print("正在分析图结构...")
        isolated_nodes, _ = analyze_graph(kg_data)
        
        # 关系推理
        print("正在进行关系推理...")
        inferred_data = infer_relations(kg_data)
        
        # 将处理后的数据转换回列表格式
        result_list = []
        for section, triples in inferred_data.items():
            result_list.extend(triples)
        
        # 生成输出文件名
        output_file = os.path.join(output_dir, f"optimized_{file_name}")
        
        # 保存推理后的数据
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(result_list, f, ensure_ascii=False, indent=2)
        
        print(f"处理完成！优化后的知识图谱已保存到 {output_file}")
    
    print("\n所有文件处理完毕！")

def analyze_graph(kg_data):
    """分析知识图谱，返回孤立节点和统计信息"""
    G = nx.Graph()
    
    # 提取所有实体
    all_entities = set()
    
    for section, triples in kg_data.items():
        for triple in triples:
            # 确保只使用前三个元素，忽略多余的元素
            if len(triple) >= 3:
                subject, relation, object_ = triple[:3]
                all_entities.add(subject)
                all_entities.add(object_)
                G.add_node(subject)
                G.add_node(object_)
                G.add_edge(subject, object_, label=relation)
    
    # 找出孤立节点
    isolated_nodes = list(nx.isolates(G))
    
    # 返回基本统计信息
    stats = {
        "节点数量": len(G.nodes),
        "关系数量": len(G.edges),
        "孤立节点数量": len(isolated_nodes),
        "连通分量数量": nx.number_connected_components(G)
    }
    
    return isolated_nodes, stats

def infer_relations(kg_data):
    """基于现有关系推理新关系，通用方法"""
    inferred_data = kg_data.copy()
    
    # 收集各类关系
    relation_types = defaultdict(lambda: defaultdict(list))
    
    # 通用关系类型映射
    # 可以根据不同领域扩展此映射
    relation_categories = {
        "包含类关系": ["包含", "组成部分", "由...组成", "包括"],
        "从属类关系": ["属于", "是...的一部分", "从属于"],
        "用途类关系": ["用途", "使用方法", "用于", "应用于"],
        "功能类关系": ["功能", "作用", "可以"]
    }
    
    # 关系映射到更抽象的类别
    def map_relation_to_category(relation):
        for category, relations in relation_categories.items():
            if any(r in relation for r in relations):
                return category
        return "其他关系"
    
    # 收集并分类所有关系
    for section, triples in kg_data.items():
        for triple in triples:
            if len(triple) >= 3:
                subject, relation, object_ = triple[:3]
                relation_category = map_relation_to_category(relation)
                relation_types[relation_category][subject].append(object_)
    
    # 创建推理关系部分
    if "推理关系" not in inferred_data:
        inferred_data["推理关系"] = []
    
    # 应用通用推理规则
    
    # 规则1: 传递性规则 - 例如"包含"关系的传递性
    if "包含类关系" in relation_types:
        contains = relation_types["包含类关系"]
        for entity, contained_items in contains.items():
            for item in contained_items:
                if item in contains:
                    for sub_item in contains[item]:
                        new_relation = [entity, "间接包含", sub_item]
                        if new_relation not in inferred_data["推理关系"]:
                            inferred_data["推理关系"].append(new_relation)
    
    # 规则2: 关联规则 - 例如，如果A属于B，B有用途C，则A可能有用途C
    if "从属类关系" in relation_types and "用途类关系" in relation_types:
        belongs_to = relation_types["从属类关系"]
        uses = relation_types["用途类关系"]
        
        for entity, categories in belongs_to.items():
            for category in categories:
                if category in uses:
                    for use in uses[category]:
                        new_relation = [entity, "可能用途", use]
                        if new_relation not in inferred_data["推理关系"]:
                            inferred_data["推理关系"].append(new_relation)
    
    # 规则3: 共同属性规则 - 如果多个实体都与同一个对象有相同关系，它们可能有共同特性
    common_relations = defaultdict(lambda: defaultdict(list))
    
    for section, triples in kg_data.items():
        for triple in triples:
            if len(triple) >= 3:
                subject, relation, object_ = triple[:3]
                common_relations[object_][relation].append(subject)
    
    for object_, relations in common_relations.items():
        for relation, subjects in relations.items():
            if len(subjects) >= 2:  # 至少两个实体有相同关系
                for i, subject1 in enumerate(subjects):
                    for subject2 in subjects[i+1:]:
                        new_relation = [subject1, "可能与...相关", subject2]
                        if new_relation not in inferred_data["推理关系"]:
                            inferred_data["推理关系"].append(new_relation)
    
    return inferred_data 