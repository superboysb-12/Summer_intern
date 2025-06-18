import json
import networkx as nx
from collections import defaultdict
import re
import os
from pyvis.network import Network

def process_knowledge_graph():
    # 第一步：加载知识图谱数据
    print("正在加载知识图谱数据...")
    with open('knowledge_graph_data/all_relations.json', 'r', encoding='utf-8') as f:
        kg_data = json.load(f)
    
    # 创建输出目录
    os.makedirs('knowledge_graph_data/output', exist_ok=True)
    
    # 第二步：分析原始图谱中的孤立节点
    print("分析原始知识图谱...")
    original_isolated_nodes, original_stats = analyze_graph(kg_data)
    print(f"原始知识图谱统计: {original_stats}")
    print(f"发现 {len(original_isolated_nodes)} 个孤立节点")
    
    # 将原始孤立节点保存到文件
    with open('knowledge_graph_data/output/original_isolated_nodes.json', 'w', encoding='utf-8') as f:
        json.dump(original_isolated_nodes, f, ensure_ascii=False, indent=2)
    
    # 第三步：实体标准化
    print("正在进行实体标准化...")
    normalized_data = normalize_entities(kg_data)
    
    # 保存标准化后的数据
    with open('knowledge_graph_data/output/normalized_relations.json', 'w', encoding='utf-8') as f:
        json.dump(normalized_data, f, ensure_ascii=False, indent=2)
    
    # 分析标准化后的孤立节点
    normalized_isolated_nodes, normalized_stats = analyze_graph(normalized_data)
    print(f"标准化后统计: {normalized_stats}")
    print(f"标准化后还有 {len(normalized_isolated_nodes)} 个孤立节点")
    
    # 第四步：创建层次结构
    print("正在构建层次结构...")
    hierarchical_data = build_hierarchy(normalized_data, normalized_isolated_nodes)
    
    # 保存层次化后的数据
    with open('knowledge_graph_data/output/hierarchical_relations.json', 'w', encoding='utf-8') as f:
        json.dump(hierarchical_data, f, ensure_ascii=False, indent=2)
    
    # 分析层次化后的孤立节点
    hierarchical_isolated_nodes, hierarchical_stats = analyze_graph(hierarchical_data)
    print(f"层次化后统计: {hierarchical_stats}")
    print(f"层次化后还有 {len(hierarchical_isolated_nodes)} 个孤立节点")
    
    # 第五步：关系推理
    print("正在进行关系推理...")
    inferred_data = infer_relations(hierarchical_data)
    
    # 保存推理后的数据
    with open('knowledge_graph_data/output/inferred_relations.json', 'w', encoding='utf-8') as f:
        json.dump(inferred_data, f, ensure_ascii=False, indent=2)
    
    # 分析推理后的孤立节点
    inferred_isolated_nodes, inferred_stats = analyze_graph(inferred_data)
    print(f"推理后统计: {inferred_stats}")
    print(f"推理后还有 {len(inferred_isolated_nodes)} 个孤立节点")
    
    # 第六步：生成可视化
    print("正在生成可视化图表...")
    visualize_graph(kg_data, "knowledge_graph_data/output/original_graph.html")
    visualize_graph(inferred_data, "knowledge_graph_data/output/optimized_graph.html")
    
    # 第七步：生成对比报告
    print("正在生成优化报告...")
    generate_report(original_stats, normalized_stats, hierarchical_stats, inferred_stats, 
                   original_isolated_nodes, inferred_isolated_nodes)
    
    print("处理完成！所有输出文件已保存到 knowledge_graph_data/output/ 目录")
    return inferred_data

def analyze_graph(kg_data):
    """分析知识图谱，返回孤立节点和统计信息"""
    G = nx.Graph()
    
    # 提取所有实体
    all_entities = set()
    relation_count = 0
    
    for section, triples in kg_data.items():
        relation_count += len(triples)
        for triple in triples:
            # 确保只使用前三个元素，忽略多余的元素
            if len(triple) >= 3:
                subject, relation, object_ = triple[:3]
                all_entities.add(subject)
                all_entities.add(object_)
                G.add_node(subject)
                G.add_node(object_)
                G.add_edge(subject, object_, label=relation)
            else:
                print(f"警告: 发现不完整的三元组: {triple}，已跳过")
    
    # 找出孤立节点
    isolated_nodes = list(nx.isolates(G))
    
    # 计算基本统计数据
    stats = {
        "实体总数": len(all_entities),
        "关系总数": relation_count,
        "孤立节点数": len(isolated_nodes),
        "孤立节点比例": round(len(isolated_nodes) / len(all_entities) * 100, 2) if all_entities else 0,
        "连通分量数": nx.number_connected_components(G)
    }
    
    return isolated_nodes, stats

def normalize_entities(kg_data):
    """标准化实体名称"""
    # 定义标准化规则
    normalization_rules = {
        r'tensorflow\.?js': 'TensorFlow.js',
        r'tensorflow\.?lite': 'TensorFlow Lite',
        r'tensorflow': 'TensorFlow',
        r'jetson\s*nano': 'NVIDIA Jetson Nano',
        r'raspberry\s*pi': '树莓派',
        r'python': 'Python',
        r'javascript': 'JavaScript',
        r'opencv': 'OpenCV',
        r'jupyter': 'Jupyter'
    }
    
    # 实体规范化函数
    def normalize_entity(entity):
        entity_lower = entity.lower()
        for pattern, replacement in normalization_rules.items():
            if re.search(pattern, entity_lower):
                return replacement
        return entity
    
    # 应用规范化并构建新的关系三元组
    normalized_data = {}
    for section, triples in kg_data.items():
        normalized_triples = []
        for triple in triples:
            normalized_triple = [
                normalize_entity(triple[0]),
                triple[1],
                normalize_entity(triple[2])
            ]
            normalized_triples.append(normalized_triple)
        normalized_data[section] = normalized_triples
    
    return normalized_data

def build_hierarchy(kg_data, isolated_nodes):
    """为孤立节点构建层次结构"""
    hierarchical_data = kg_data.copy()
    
    # 创建主题领域分类
    domain_categories = {
        "深度学习框架": ["TensorFlow", "TensorFlow.js", "TensorFlow Lite", "PyTorch", "Keras", "MobileNet"],
        "硬件设备": ["树莓派", "NVIDIA Jetson Nano", "MCU", "GPU", "CSI", "GPIO"],
        "软件工具": ["OpenCV", "Jupyter", "Python", "JavaScript", "GStreamer"],
        "机器学习概念": ["张量", "卷积", "激活函数", "池化", "模型", "训练", "推理"],
        "图像处理": ["人脸识别", "物体检测", "图像分类", "特征提取"],
        "移动端开发": ["Android", "iOS", "移动应用", "边缘计算"]
    }
    
    # 为孤立节点添加领域关系
    if "领域分类" not in hierarchical_data:
        hierarchical_data["领域分类"] = []
    
    for node in isolated_nodes:
        assigned = False
        for domain, keywords in domain_categories.items():
            for keyword in keywords:
                if keyword.lower() in node.lower():
                    hierarchical_data["领域分类"].append([node, "属于", domain])
                    assigned = True
                    break
            if assigned:
                break
        
        # 如果无法自动分配，添加到"未分类"
        if not assigned:
            hierarchical_data["领域分类"].append([node, "需要分类", "未分类实体"])
    
    # 添加领域之间的关系
    if "领域关系" not in hierarchical_data:
        hierarchical_data["领域关系"] = []
    
    # 添加一些领域之间的关系
    domain_relations = [
        ["深度学习框架", "应用于", "机器学习概念"],
        ["硬件设备", "运行", "软件工具"],
        ["软件工具", "支持", "深度学习框架"],
        ["机器学习概念", "用于", "图像处理"],
        ["图像处理", "应用于", "移动端开发"],
        ["深度学习框架", "部署于", "硬件设备"]
    ]
    hierarchical_data["领域关系"].extend(domain_relations)
    
    return hierarchical_data

def infer_relations(kg_data):
    """基于现有关系推理新关系"""
    inferred_data = kg_data.copy()
    
    # 收集包含关系
    contains_relations = defaultdict(list)
    uses_relations = defaultdict(list)
    is_part_of_relations = defaultdict(list)
    
    for section, triples in kg_data.items():
        for triple in triples:
            # 确保只使用前三个元素，忽略多余的元素
            if len(triple) >= 3:
                subject, relation, object_ = triple[:3]
                
                # 收集各类关系
                if relation == "包含" or relation == "组成部分":
                    contains_relations[subject].append(object_)
                elif relation == "用途" or relation == "使用方法":
                    uses_relations[subject].append(object_)
                elif relation == "属于":
                    is_part_of_relations[subject].append(object_)
            else:
                print(f"警告: 在关系推理中跳过不完整的三元组: {triple}")
    
    # 创建推理关系部分
    if "推理关系" not in inferred_data:
        inferred_data["推理关系"] = []
    
    # 应用传递规则 - 包含关系的传递性
    for entity, contained_items in contains_relations.items():
        for item in contained_items:
            if item in contains_relations:
                for sub_item in contains_relations[item]:
                    new_relation = [entity, "间接包含", sub_item]
                    if new_relation not in inferred_data["推理关系"]:
                        inferred_data["推理关系"].append(new_relation)
    
    # 应用关联规则 - 如果A属于B，B有用途C，则A可能有用途C
    for entity, categories in is_part_of_relations.items():
        for category in categories:
            if category in uses_relations:
                for use in uses_relations[category]:
                    new_relation = [entity, "可能用途", use]
                    if new_relation not in inferred_data["推理关系"]:
                        inferred_data["推理关系"].append(new_relation)
    
    return inferred_data

def visualize_graph(kg_data, output_file):
    """生成交互式知识图谱可视化"""
    G = nx.Graph()
    
    # 添加节点和边
    for section, triples in kg_data.items():
        for triple in triples:
            # 确保只使用前三个元素，忽略多余的元素
            if len(triple) >= 3:
                subject, relation, object_ = triple[:3]
                if subject not in G:
                    G.add_node(subject)
                if object_ not in G:
                    G.add_node(object_)
                G.add_edge(subject, object_, title=relation)
            else:
                print(f"警告: 在可视化中跳过不完整的三元组: {triple}")
    
    # 找出孤立节点
    isolated_nodes = list(nx.isolates(G))
    
    # 使用pyvis生成交互式HTML图
    nt = Network(height="800px", width="100%", bgcolor="#222222", font_color="white")
    nt.barnes_hut()
    
    # 添加节点和边
    for node in G.nodes:
        if node in isolated_nodes:
            nt.add_node(node, color="#FF5733", title=f"孤立节点: {node}")
        elif any(keyword in node for keyword in ["TensorFlow", "Python", "树莓派", "Jetson"]):
            # 突出显示重要节点
            nt.add_node(node, color="#33A8FF", title=node, size=25)
        elif "领域" in node:
            # 突出显示领域节点
            nt.add_node(node, color="#33FF57", title=f"领域: {node}", size=30)
        else:
            nt.add_node(node, title=node)
    
    # 添加边
    for u, v, attr in G.edges(data=True):
        nt.add_edge(u, v, title=attr.get('title', '关联'))
    
    # 保存为HTML文件
    nt.save_graph(output_file)

def generate_report(original_stats, normalized_stats, hierarchical_stats, inferred_stats, 
                  original_isolated, inferred_isolated):
    """生成优化效果报告"""
    report = {
        "优化前统计": original_stats,
        "标准化后统计": normalized_stats,
        "层次化后统计": hierarchical_stats,
        "关系推理后统计": inferred_stats,
        "优化效果": {
            "孤立节点减少数量": len(original_isolated) - len(inferred_isolated),
            "孤立节点减少比例": round((len(original_isolated) - len(inferred_isolated)) / len(original_isolated) * 100, 2) if original_isolated else 0,
            "连通性提升": original_stats["连通分量数"] - inferred_stats["连通分量数"]
        },
        "剩余孤立节点": inferred_isolated[:20] + (["..."] if len(inferred_isolated) > 20 else [])
    }
    
    with open('knowledge_graph_data/output/optimization_report.json', 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    # 生成简单的文本报告
    with open('knowledge_graph_data/output/optimization_report.txt', 'w', encoding='utf-8') as f:
        f.write("知识图谱优化报告\n")
        f.write("=" * 50 + "\n\n")
        
        f.write("1. 原始知识图谱统计\n")
        for key, value in original_stats.items():
            f.write(f"   - {key}: {value}\n")
        f.write("\n")
        
        f.write("2. 优化后知识图谱统计\n")
        for key, value in inferred_stats.items():
            f.write(f"   - {key}: {value}\n")
        f.write("\n")
        
        f.write("3. 优化效果\n")
        for key, value in report["优化效果"].items():
            f.write(f"   - {key}: {value}\n")
        f.write("\n")
        
        f.write("4. 处理过程\n")
        f.write(f"   - 实体标准化: {original_stats['孤立节点数']} -> {normalized_stats['孤立节点数']} 孤立节点\n")
        f.write(f"   - 层次结构构建: {normalized_stats['孤立节点数']} -> {hierarchical_stats['孤立节点数']} 孤立节点\n")
        f.write(f"   - 关系推理: {hierarchical_stats['孤立节点数']} -> {inferred_stats['孤立节点数']} 孤立节点\n")
        f.write("\n")
        
        f.write("5. 结论与建议\n")
        reduction_percent = report["优化效果"]["孤立节点减少比例"]
        if reduction_percent > 70:
            f.write("   优化效果显著，已解决大部分孤立节点问题。\n")
        elif reduction_percent > 40:
            f.write("   优化效果良好，但仍有部分孤立节点需要手动处理。\n")
        else:
            f.write("   优化效果一般，建议进一步完善知识图谱或调整优化策略。\n")

# 执行处理
if __name__ == "__main__":
    process_knowledge_graph() 