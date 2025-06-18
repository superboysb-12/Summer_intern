import os
import json
import time
import asyncio
from pathlib import Path

from TextSegmenter import TextSegmenter
from clean_heading_chunks import HeadingChunksCleaner
from extract_relations import RelationExtractor
from optimize_knowledge_graph import process_knowledge_graph

# 尝试导入Neo4jImporter
try:
    from neo4j_importer import Neo4jImporter
    NEO4J_AVAILABLE = True
except ImportError:
    NEO4J_AVAILABLE = False
    print("警告: 无法导入Neo4jImporter，Neo4j功能将不可用")

class KnowledgeGraphPipeline:
    """知识图谱处理流水线，顺序执行文本分割、标题清洗、关系提取和知识图谱优化"""
    
    def __init__(self, config=None):
        """初始化知识图谱处理流水线
        
        Args:
            config: 配置字典，包含各种处理参数
        """
        self.config = config or {}
        
        # 设置默认目录
        self.source_dir = self.config.get("source_dir", "source_article")
        self.segmented_dir = self.config.get("segmented_dir", "segmented_output")
        self.cleaned_dir = self.config.get("cleaned_dir", "wash_dir")
        self.kg_dir = self.config.get("kg_dir", "knowledge_graph_data")
        
        # 确保所有目录存在
        for dir_path in [self.source_dir, self.segmented_dir, self.cleaned_dir, self.kg_dir]:
            os.makedirs(dir_path, exist_ok=True)
            
        # 深度学习API配置
        self.api_config = {
            "api_key": self.config.get("api_key", "sk-6bfa57029fbc44b9ac4eb38dd61bc695"),
            "model": self.config.get("model", "deepseek-chat"),
            "temperature": self.config.get("temperature", 0.1),
            "batch_size": self.config.get("batch_size", 16),
            "max_retries": self.config.get("max_retries", 3),
            "max_concurrency": self.config.get("max_concurrency", 5),  # 最大并发请求数
            "relations_to_extract": self.config.get("relations_to_extract", [
                "是什么", "包含", "用途", "特点", "组成部分", 
                "执行步骤", "优点", "缺点", "属于", "比较",
                "工作原理", "使用方法", "示例"
            ])
        }
        
        # 初始化Neo4j导入器
        self.neo4j_importer = None
        if NEO4J_AVAILABLE and self.config.get("use_neo4j", False):
            neo4j_config = {
                "neo4j_uri": self.config.get("neo4j_uri", "bolt://localhost:7687"),
                "neo4j_user": self.config.get("neo4j_user", "neo4j"),
                "neo4j_password": self.config.get("neo4j_password", "summer123456"),
                "neo4j_database": self.config.get("neo4j_database", "KnowledgeGraph"),
                "kg_dir": self.kg_dir,
                "multi_graph": self.config.get("multi_graph", False)  # 是否为每个文件创建独立的图
            }
            self.neo4j_importer = Neo4jImporter(neo4j_config)
        
    def step1_segment_text(self):
        """步骤1：将文档分割为文本块"""
        print("\n" + "="*50)
        print("步骤1：文本分割")
        print("="*50)
        
        segmenter = TextSegmenter(
            text_dir=self.source_dir,
            output_dir=self.segmented_dir
        )
        
        result = segmenter.process_files()
        print(f"文本分割完成，共处理 {len(result)} 个文件")
        return result
    
    def step2_clean_chunks(self):
        """步骤2：清洗文本块，去除无用信息"""
        print("\n" + "="*50)
        print("步骤2：文本块清洗")
        print("="*50)
        
        cleaner = HeadingChunksCleaner(
            input_dir=self.segmented_dir,
            output_dir=self.cleaned_dir
        )
        
        cleaner.process_directory()
        print(f"文本块清洗完成，结果保存在 {self.cleaned_dir} 目录")
    
    async def step3_extract_relations_async(self):
        """步骤3：从清洗后的文本块中异步提取实体关系"""
        print("\n" + "="*50)
        print("步骤3：关系提取（异步）")
        print("="*50)
        
        # 组装配置
        config = self.api_config.copy()
        config["input_dir"] = self.cleaned_dir
        config["output_dir"] = self.kg_dir
        
        extractor = RelationExtractor(config)
        all_relations = await extractor.process_directory_async()
        
        # 将所有关系合并到一个文件中
        self._merge_relations(all_relations)
        
        print(f"关系提取完成，共提取 {len(all_relations)} 个关系，结果保存在 {self.kg_dir} 目录")
        return all_relations
    
    def step3_extract_relations(self):
        """步骤3：从清洗后的文本块中提取实体关系（同步包装异步函数）"""
        return asyncio.run(self.step3_extract_relations_async())
    
    def step4_optimize_graph(self):
        """步骤4：优化知识图谱，增加层次结构和推理关系"""
        print("\n" + "="*50)
        print("步骤4：知识图谱优化")
        print("="*50)
        
        optimized_data = process_knowledge_graph()
        print(f"知识图谱优化完成，结果保存在 {self.kg_dir}/output 目录")
        return optimized_data
    
    def step5_import_to_neo4j(self):
        """步骤5：将知识图谱导入到Neo4j数据库"""
        print("\n" + "="*50)
        print("步骤5：导入Neo4j数据库")
        print("="*50)
        
        if not NEO4J_AVAILABLE:
            print("错误: Neo4jImporter不可用，无法执行导入")
            return False
            
        if not self.neo4j_importer:
            print("错误: Neo4j导入器未初始化，请检查配置")
            return False
            
        # 如果启用了多图模式，每个文件将作为独立图导入
        multi_graph = self.config.get("multi_graph", False)
        if multi_graph:
            print("启用多图模式，每个文件将作为独立图导入...")
            import_success = self.neo4j_importer.import_all_files_as_separate_graphs()
            
            if import_success:
                print(f"成功将每个文件作为独立图导入Neo4j数据库")
                return True
            else:
                print(f"导入独立图到Neo4j数据库失败")
                return False
        else:
            # 单图模式 - 尝试导入优化后的知识图谱数据
            print("使用单图模式导入所有数据...")
            kg_file_path = f"{self.kg_dir}/output/inferred_relations.json"
            if not os.path.exists(kg_file_path):
                kg_file_path = f"{self.kg_dir}/all_relations.json"
                
            if not os.path.exists(kg_file_path):
                print(f"错误: 找不到知识图谱数据文件")
                return False
                
            # 使用Neo4jImporter导入数据
            import_success = self.neo4j_importer.import_knowledge_graph(kg_file_path=kg_file_path)
            
            if import_success:
                print(f"成功将知识图谱导入Neo4j数据库")
                return True
            else:
                print(f"导入Neo4j数据库失败")
                return False
    
    def _merge_relations(self, all_relations):
        """将所有提取的关系合并到一个文件中"""
        # 从目录中读取所有关系文件
        relation_files = list(Path(self.kg_dir).glob('*_relations.json'))
        
        merged_relations = {}
        for file_path in relation_files:
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                
                section_name = file_path.stem.replace('_relations', '')
                merged_relations[section_name] = data
            except Exception as e:
                print(f"合并关系文件 {file_path} 时出错: {e}")
        
        # 保存合并后的关系
        with open(f'{self.kg_dir}/all_relations.json', 'w', encoding='utf-8') as f:
            json.dump(merged_relations, f, ensure_ascii=False, indent=2)
    
    async def run_pipeline_async(self, include_neo4j=False):
        """异步运行完整的知识图谱处理流水线
        
        Args:
            include_neo4j: 是否包含Neo4j导入步骤
        """
        print("\n" + "*"*70)
        print("  启动知识图谱处理流水线（异步版）")
        print("*"*70)
        
        start_time = time.time()
        
        # 步骤1：文本分割
        self.step1_segment_text()
        
        # 步骤2：文本块清洗
        self.step2_clean_chunks()
        
        # 步骤3：关系提取（异步）
        await self.step3_extract_relations_async()
        
        # 步骤4：知识图谱优化
        self.step4_optimize_graph()
        
        # 步骤5：导入Neo4j（可选）
        if include_neo4j:
            self.step5_import_to_neo4j()
        
        end_time = time.time()
        duration = end_time - start_time
        
        print("\n" + "*"*70)
        print(f"  知识图谱处理流水线执行完成！总耗时: {duration:.2f}秒")
        print("*"*70)
        print(f"\n结果文件：")
        print(f"- 文本分割结果: {self.segmented_dir}/*.json")
        print(f"- 文本清洗结果: {self.cleaned_dir}/*.json")
        print(f"- 关系提取结果: {self.kg_dir}/*.json")
        print(f"- 知识图谱优化结果: {self.kg_dir}/output/*")
        print(f"- 知识图谱可视化: {self.kg_dir}/output/optimized_graph.html")
        
        if include_neo4j and self.neo4j_importer:
            print(f"- Neo4j浏览器访问: http://localhost:7474/ 登录查看知识图谱")
            if self.config.get("multi_graph", False):
                print(f"  提示：在Neo4j浏览器中可以使用以下查询查看特定文件的知识图谱：")
                print(f"  MATCH (n:Entity) WHERE n.source='文件名' RETURN n")
                print(f"  或者使用文件名标签：")
                print(f"  MATCH (n:文件名) RETURN n")
        
        # 关闭Neo4j连接
        if include_neo4j and self.neo4j_importer:
            self.neo4j_importer.close()
    
    def run_pipeline(self, include_neo4j=False):
        """运行完整的知识图谱处理流水线（同步包装异步函数）
        
        Args:
            include_neo4j: 是否包含Neo4j导入步骤
        """
        asyncio.run(self.run_pipeline_async(include_neo4j))


def main():
    """主函数，创建并运行知识图谱处理流水线"""
    # 自定义配置
    config = {
        "source_dir": "source_article",  # 源文档目录
        "segmented_dir": "segmented_output",  # 分段输出目录
        "cleaned_dir": "wash_dir",  # 清洗后输出目录
        "kg_dir": "knowledge_graph_data",  # 知识图谱数据目录
        
        # API配置
        "api_key": "sk-6bfa57029fbc44b9ac4eb38dd61bc695",  # 替换为你的API密钥
        "model": "deepseek-chat",
        "temperature": 0.1,
        "batch_size": 16,
        "max_retries": 3,
        "max_concurrency": 20,  # 最大并发请求数
        
        # Neo4j配置
        "use_neo4j": True,  # 是否使用Neo4j数据库
        "neo4j_uri": "bolt://localhost:7687",  # Neo4j连接URI
        "neo4j_user": "neo4j",  # Neo4j用户名
        "neo4j_password": "summer123456",  # Neo4j密码
        "neo4j_database": "knowledgegraphs",  # Neo4j数据库名称
        "multi_graph": True  # 是否为每个文件创建独立的图
    }
    
    # 创建并运行流水线
    pipeline = KnowledgeGraphPipeline(config)
    pipeline.run_pipeline(include_neo4j=config.get("use_neo4j", False))


if __name__ == "__main__":
    main() 