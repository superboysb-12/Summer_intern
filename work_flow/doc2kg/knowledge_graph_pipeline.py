import os
import json
import time
import asyncio
from pathlib import Path

from TextSegmenter import TextSegmenter
from clean_heading_chunks import HeadingChunksCleaner
from extract_relations import RelationExtractor
from optimize_knowledge_graph import process_knowledge_graph
from config import CONFIG

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
        # 合并默认配置和用户配置
        self.config = CONFIG.copy()
        if config:
            self.config.update(config)
        
        # 设置目录
        self.source_dir = self.config["source_dir"]
        self.segmented_dir = self.config["segmented_dir"]
        self.cleaned_dir = self.config["cleaned_dir"]
        self.kg_dir = self.config["kg_dir"]
        
        # 确保所有目录存在
        for dir_path in [self.source_dir, self.segmented_dir, self.cleaned_dir, self.kg_dir]:
            os.makedirs(dir_path, exist_ok=True)
            
        # 初始化Neo4j导入器
        self.neo4j_importer = None
        if NEO4J_AVAILABLE and self.config["use_neo4j"]:
            neo4j_config = {
                "neo4j_uri": self.config["neo4j_uri"],
                "neo4j_user": self.config["neo4j_user"],
                "neo4j_password": self.config["neo4j_password"],
                "neo4j_database": self.config["neo4j_database"],
                "kg_dir": self.kg_dir
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
        config = {
            "api_key": self.config["api_key"],
            "model": self.config["model"],
            "temperature": self.config["temperature"],
            "batch_size": self.config["batch_size"],
            "max_retries": self.config["max_retries"],
            "max_concurrency": self.config["max_concurrency"],
            "relations_to_extract": self.config["relations_to_extract"],
            "input_dir": self.cleaned_dir,
            "output_dir": self.kg_dir
        }
        
        extractor = RelationExtractor(config)
        all_relations = await extractor.process_directory_async()
        
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
        
        optimized_data = process_knowledge_graph(
            input_dir=self.kg_dir, 
            output_dir=f"{self.kg_dir}/output"
        )
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
            
        # 尝试导入优化后的知识图谱数据
        kg_file_path = f"{self.kg_dir}/output/optimized_all_relations.json"
        if not os.path.exists(kg_file_path):
            print(f"尝试查找其他优化后的文件...")
            optimized_files = list(Path(f"{self.kg_dir}/output").glob('optimized_*.json'))
            if optimized_files:
                kg_file_path = str(optimized_files[0])
            else:
                print("未找到优化后的文件，尝试使用原始关系文件...")
                relation_files = list(Path(self.kg_dir).glob('*_relations.json'))
                if relation_files:
                    kg_file_path = str(relation_files[0])
                else:
                    print(f"错误: 找不到知识图谱数据文件")
                    return False
                
        print(f"使用文件 {kg_file_path} 导入Neo4j...")
        import_success = self.neo4j_importer.import_knowledge_graph(kg_file_path=kg_file_path)
        
        if import_success:
            print(f"成功将知识图谱导入Neo4j数据库")
            return True
        else:
            print(f"导入Neo4j数据库失败")
            return False
    
    async def run_pipeline_async(self):
        """异步运行完整的知识图谱处理流水线"""
        print("\n" + "*"*70)
        print("  启动知识图谱处理流水线")
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
        if self.config["use_neo4j"]:
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
        
        if self.config["use_neo4j"] and self.neo4j_importer:
            print(f"- Neo4j浏览器访问: http://localhost:7474/ 登录查看知识图谱")
            # 关闭Neo4j连接
            self.neo4j_importer.close()
    
    def run_pipeline(self):
        """运行完整的知识图谱处理流水线（同步包装异步函数）"""
        asyncio.run(self.run_pipeline_async())


def main():
    """主函数，创建并运行知识图谱处理流水线"""
    # 可以在这里修改配置
    config = {
        # 如果需要覆盖默认配置，可以在这里添加自定义设置
    }
    
    # 创建并运行流水线
    pipeline = KnowledgeGraphPipeline(config)
    pipeline.run_pipeline()


if __name__ == "__main__":
    main() 