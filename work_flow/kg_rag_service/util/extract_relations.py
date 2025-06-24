import json
import os
from pathlib import Path
import time
import asyncio
import aiohttp
from tqdm import tqdm
from openai import AsyncOpenAI

class RelationExtractor:
    def __init__(self, config):
        self.config = config
        self.output_dir = Path(config["output_dir"])
        self.output_dir.mkdir(exist_ok=True)
        self.max_concurrency = config.get("max_concurrency", 5)
        # 添加默认超时设置，如果配置文件中未提供则使用30秒
        self.timeout = config.get("request_timeout", 30)
        
    async def create_client(self):
        """创建异步OpenAI客户端"""
        return AsyncOpenAI(
            api_key=self.config["api_key"], 
            base_url="https://api.deepseek.com",
            timeout=self.timeout  # 设置请求超时时间
        )
        
    async def extract_relations_from_text(self, client, text_chunk):
        """从文本块中提取实体关系"""
        system_prompt = f"""
        你是一个专业的知识图谱关系提取专家。你的任务是从提供的技术文本中提取实体和它们之间的关系。

        请严格按照以下步骤进行：
        1. 仔细阅读整个文本内容，识别出所有重要实体
        2. 分析实体之间的关系，特别关注以下关系类型：{", ".join(self.config["relations_to_extract"])}
        3. 确保每个关系都是有意义且准确的，不要创造不存在的关系
        4. 对于每个识别出的关系，构建完整的三元组

        提取的关系必须符合以下要求：
        - 主体实体必须是明确、具体的技术概念或专有名词
        - 关系类型必须是预定义列表中的一种
        - 客体实体应当是对主体实体有补充说明的内容
        - 避免提取过于宽泛或模糊的实体和关系
        - 保持客体实体的完整性，确保其包含足够的信息

        请按以下格式返回结果：
        [
            ["主体实体1", "关系类型1", "客体实体1的详细描述"],
            ["主体实体2", "关系类型2", "客体实体2的详细描述"],
            ...
        ]

        关系类型示例及说明：
        - "是什么": 描述实体的定义或本质，如 ["React", "是什么", "一个用于构建用户界面的JavaScript库"]
        - "包含": 描述整体与部分的关系，如 ["神经网络", "包含", "输入层、隐藏层和输出层"]
        - "用途": 描述实体的应用场景，如 ["梯度下降", "用途", "优化神经网络中的权重参数"]
        - "特点": 描述实体的特征，如 ["CNN", "特点", "具有局部连接和权重共享的特性"]
        - "组成部分": 列举实体的组件，如 ["计算机系统", "组成部分", "硬件和软件"]
        - "执行步骤": 描述过程或方法的步骤，如 ["K-means算法", "执行步骤", "选择K个初始中心点，分配样本到最近的簇，重新计算簇中心，重复直到收敛"]
        - "工作原理": 解释实体如何工作，如 ["注意力机制", "工作原理", "通过计算查询和键的相似度来确定对值的关注权重"]

        请只返回JSON数组，不要包含任何额外的解释或说明。如果文本中没有找到任何关系，请返回空数组 []。
        """
        
        try:
            for retry in range(self.config["max_retries"]):
                try:
                    response = await client.chat.completions.create(
                        model=self.config["model"],
                        messages=[
                            {"role": "system", "content": system_prompt},
                            {"role": "user", "content": f"请从以下文本中提取实体关系:\n\n{text_chunk}"}
                        ],
                        temperature=self.config["temperature"],
                        stream=False,
                        timeout=self.timeout  # 设置每个请求的超时时间
                    )
                    
                    result = response.choices[0].message.content
                    
                    # 尝试解析JSON
                    try:
                        # 处理可能的非标准JSON格式
                        result = result.strip()
                        if result.startswith("```json"):
                            result = result.replace("```json", "", 1)
                        if result.endswith("```"):
                            result = result.rsplit("```", 1)[0]
                        
                        relations = json.loads(result.strip())
                        return relations
                    except json.JSONDecodeError:
                        print(f"无法解析API返回的JSON: {result}")
                        if retry < self.config["max_retries"] - 1:
                            await asyncio.sleep(1)  # 失败后短暂等待再重试
                        continue
                        
                except Exception as e:
                    print(f"API调用失败: {e}")
                    if retry < self.config["max_retries"] - 1:
                        await asyncio.sleep(1)  # 失败后短暂等待再重试
                    continue
                    
            # 如果所有重试都失败
            return []
                
        except Exception as e:
            print(f"提取关系时发生异常: {e}")
            return []

    async def process_file(self, file_path):
        """处理单个文件中的所有文本块"""
        print(f"处理文件: {file_path}")
        all_relations = []
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                
            chunks = data.get('chunks', [])
            
            # 创建客户端
            client = await self.create_client()
            
            # 使用信号量限制并发请求数
            semaphore = asyncio.Semaphore(self.max_concurrency)
            
            async def process_chunk_with_semaphore(chunk):
                async with semaphore:
                    return await self.extract_relations_from_text(client, chunk)
            
            # 创建所有文本块的处理任务
            tasks = [process_chunk_with_semaphore(chunk) for chunk in chunks]
            
            # 使用tqdm创建进度条并跟踪任务完成情况
            pbar = tqdm(total=len(chunks), desc="处理文本块", unit="块")
            
            # 处理每个任务的结果
            for future in asyncio.as_completed(tasks):
                relations = await future
                if relations:
                    all_relations.extend(relations)
                pbar.update(1)
            
            pbar.close()
        
        except Exception as e:
            print(f"处理文件 {file_path} 时出错: {e}")
        
        return all_relations

    async def process_directory_async(self):
        """异步处理目录中的所有JSON文件"""
        input_dir = Path(self.config["input_dir"])
        json_files = list(input_dir.glob('*.json'))
        
        # 过滤掉汇总文件
        json_files = [f for f in json_files if not f.name.startswith('all_segments')]
        
        all_results = []
        
        # 处理每个文件
        for i, file_path in enumerate(json_files):
            file_name = file_path.stem
            print(f"开始处理文件 {i+1}/{len(json_files)}: {file_name}")
            
            relations = await self.process_file(file_path)
            
            if relations:
                all_results.extend(relations)
                
                # 保存单个文件的关系
                output_file = self.output_dir / f"{file_name}_relations.json"
                with open(output_file, 'w', encoding='utf-8') as f:
                    json.dump(relations, f, ensure_ascii=False, indent=2)
                    
                print(f"已保存 {len(relations)} 个关系到 {output_file}")
        
        return all_results
    
    def process_directory(self):
        """处理目录中的所有JSON文件 - 同步包装异步函数"""
        return asyncio.run(self.process_directory_async())

    def convert_to_visualization_format(self, all_relations):
        """将关系转换为可视化格式"""
        nodes = set()
        links = []
        
        print(f"开始处理 {len(all_relations)} 个关系用于可视化...")
        
        # 创建节点和连接
        for relation in tqdm(all_relations, desc="构建可视化数据", unit="关系"):
            if len(relation) == 3:
                source, rel_type, target = relation
                nodes.add(source)
                nodes.add(target)
                links.append({
                    "source": source,
                    "target": target,
                    "relation": rel_type
                })
        
        # 转换为列表
        nodes_list = [{"id": node, "name": node} for node in nodes]
        
        graph_data = {
            "nodes": nodes_list,
            "links": links
        }
        
        # 保存可视化数据
        vis_file = self.output_dir / "visualization_data.json"
        with open(vis_file, 'w', encoding='utf-8') as f:
            json.dump(graph_data, f, ensure_ascii=False, indent=2)
            
        print(f"已保存可视化数据到 {vis_file}，包含 {len(nodes_list)} 个节点和 {len(links)} 个关系")

async def main_async():
    print("开始提取知识图谱关系...")
    extractor = RelationExtractor(CONFIG)
    all_relations = await extractor.process_directory_async()
    if all_relations:
        extractor.convert_to_visualization_format(all_relations)
    print("关系提取完成！")

def main():
    asyncio.run(main_async())

if __name__ == "__main__":
    main() 