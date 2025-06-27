import os
import json
import time
import asyncio
from pathlib import Path

from util.TextSegmenter import TextSegmenter
from util.clean_heading_chunks import HeadingChunksCleaner
from util.extract_relations import RelationExtractor
from util.optimize_knowledge_graph import process_knowledge_graph
from config import CONFIG, ensure_directories_exist
from util.neo4j_importer import Neo4jImporter
from util.kg_embedding import KGEmbedder
from util.kg_processor import KnowledgeGraphProcessor
from util.kg_rag import KGRAGGenerator


class KnowledgeGraphPipeline:
    """知识图谱处理流水线，顺序执行文本分割、标题清洗、关系提取和知识图谱优化"""
    
    def __init__(self, config=None):
        """初始化知识图谱处理流水线
        
        Args:
            config: 配置字典，包含各种处理参数
        """
        # 如果提供了自定义配置，更新CONFIG
        if config:
            for key, value in config.items():
                CONFIG[key] = value
            
        # 确保所有目录存在（显式调用函数）
        ensure_directories_exist()
            
        # 初始化Neo4j导入器
        self.neo4j_importer = None
        neo4j_config = {
            "neo4j_uri": CONFIG["neo4j_uri"],
            "neo4j_user": CONFIG["neo4j_user"],
            "neo4j_password": CONFIG["neo4j_password"],
            "neo4j_database": CONFIG["neo4j_database"],
            "kg_dir": CONFIG["kg_dir"]
        }
        self.neo4j_importer = Neo4jImporter(neo4j_config)
        
        # 初始化嵌入模型
        self.embedder = None
        self.kg_processor = None
        self.kg_rag_generator = None
        
    def step1_segment_text(self):
        """步骤1：将文档分割为文本块"""
        print("\n" + "="*50)
        print("步骤1：文本分割")
        print("="*50)
        
        segmenter = TextSegmenter(
            text_dir=CONFIG["source_dir"],
            output_dir=CONFIG["segmented_dir"]
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
            input_dir=CONFIG["segmented_dir"],
            output_dir=CONFIG["cleaned_dir"]
        )
        
        cleaner.process_directory()
        print(f"文本块清洗完成，结果保存在 {CONFIG['cleaned_dir']} 目录")
    
    async def step3_extract_relations_async(self):
        """步骤3：从清洗后的文本块中异步提取实体关系"""
        print("\n" + "="*50)
        print("步骤3：关系提取（异步）")
        print("="*50)
        
        # 组装配置
        config = {
            "api_key": CONFIG["api_key"],
            "model": CONFIG["model"],
            "temperature": CONFIG["temperature"],
            "batch_size": CONFIG["batch_size"],
            "max_retries": CONFIG["max_retries"],
            "max_concurrency": CONFIG["max_concurrency"],
            "relations_to_extract": CONFIG["relations_to_extract"],
            "input_dir": CONFIG["cleaned_dir"],
            "output_dir": CONFIG["kg_dir"]
        }
        
        extractor = RelationExtractor(config)
        all_relations = await extractor.process_directory_async()
        
        print(f"关系提取完成，共提取 {len(all_relations)} 个关系，结果保存在 {CONFIG['kg_dir']} 目录")
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
            input_dir=CONFIG["kg_dir"], 
            output_dir=f"{CONFIG['kg_dir']}/output"
        )
        print(f"知识图谱优化完成，结果保存在 {CONFIG['kg_dir']}/output 目录")
        return optimized_data
    
    def step5_import_to_neo4j(self):
        """步骤5：将知识图谱导入到Neo4j数据库"""
        print("\n" + "="*50)
        print("步骤5：导入Neo4j数据库")
        print("="*50)
        
        # 查找所有可用的知识图谱文件
        kg_files = []
        
        # 首先查找优化后的文件
        optimized_files = list(Path(f"{CONFIG['kg_dir']}/output").glob('optimized_*.json'))
        if optimized_files:
            kg_files.extend([str(f) for f in optimized_files])
            print(f"找到 {len(optimized_files)} 个优化后的知识图谱文件")
        
        # 如果没有优化后的文件，则查找原始关系文件
        if not kg_files:
            relation_files = list(Path(CONFIG["kg_dir"]).glob('*_relations.json'))
            if relation_files:
                kg_files.extend([str(f) for f in relation_files])
                print(f"找到 {len(relation_files)} 个原始知识图谱文件")
        
        if not kg_files:
            print("错误: 找不到知识图谱数据文件")
            return False, None
            
        print(f"准备导入 {len(kg_files)} 个知识图谱文件到Neo4j数据库")
        for file in kg_files:
            print(f"  - {file}")
            
        # 导入所有知识图谱文件
        import_success, db_name = self.neo4j_importer.import_multiple_kg_files(kg_files)
        
        if import_success:
            print(f"成功将 {len(kg_files)} 个知识图谱文件导入Neo4j数据库: {db_name}")
            # 返回所有文件，而不仅仅是第一个文件
            return True, (kg_files, db_name)
        else:
            print(f"导入Neo4j数据库失败")
            return False, None
    
    def step6_process_kg_for_rag(self):
        """步骤6：知识图谱RAG处理（整合原来的步骤6和7）"""
        print("\n" + "="*50)
        print("步骤6：知识图谱RAG处理")
        print("="*50)
        
        # 初始化嵌入模型（如果尚未初始化）
        if self.embedder is None:
            self.embedder = KGEmbedder(model_name=CONFIG.get("embedding_model", "bge-small-zh-v1.5"))
        
        # 初始化知识图谱处理器和RAG生成器
        self.kg_processor = KnowledgeGraphProcessor(embedder=self.embedder)
        
        # 设置输出目录
        rag_output_dir = os.path.join(CONFIG["rag_dir"], "processed")
        os.makedirs(rag_output_dir, exist_ok=True)
        
        # 查找所有可用的知识图谱文件
        kg_files = []
        
        # 首先查找优化后的文件
        optimized_files = list(Path(f"{CONFIG['kg_dir']}/output").glob('optimized_*.json'))
        if optimized_files:
            kg_files.extend([str(f) for f in optimized_files])
            print(f"找到 {len(optimized_files)} 个优化后的知识图谱文件")
        
        # 如果没有优化后的文件，则查找原始关系文件
        if not kg_files:
            relation_files = list(Path(CONFIG["kg_dir"]).glob('*_relations.json'))
            if relation_files:
                kg_files.extend([str(f) for f in relation_files])
                print(f"找到 {len(relation_files)} 个原始知识图谱文件")
        
        if not kg_files:
            print("错误: 找不到知识图谱数据文件")
            return False
        
        print(f"准备处理 {len(kg_files)} 个知识图谱文件")
        for file in kg_files:
            print(f"  - {file}")
            
        # 处理所有知识图谱文件
        all_kg_data = []
        for kg_file in kg_files:
            print(f"加载知识图谱文件: {kg_file}")
            kg_data = self.kg_processor.load_knowledge_graph(kg_file)
            all_kg_data.extend(kg_data)
        
        # 合并所有数据后一次性处理
        print(f"合并后共有 {len(all_kg_data)} 条知识图谱关系")
        
        # 构建图结构
        self.kg_processor.build_graph(all_kg_data)
        
        # 生成嵌入向量
        batch_size = CONFIG.get("embedding_batch_size", 32)
        self.kg_processor.generate_embeddings(all_kg_data, batch_size=batch_size)
        
        # 构建FAISS索引
        self.kg_processor.build_faiss_index()
        
        # 保存处理后的数据
        self.kg_processor.save_processed_data(rag_output_dir)
        
        print(f"知识图谱RAG处理完成，结果保存在 {rag_output_dir}")
        return True
    
    async def run_pipeline_async(self, source_dir=None, force_rebuild=False):
        """异步运行完整的知识图谱处理流水线
        
        Args:
            source_dir: 可选的源文档目录路径
            force_rebuild: 是否强制重新构建知识图谱，即使已存在处理结果
        """
        print("\n" + "*"*70)
        print("  启动知识图谱处理流水线")
        print("*"*70)
        
        # 如果提供了source_dir，更新CONFIG
        if source_dir:
            CONFIG["source_dir"] = source_dir
            # 确保目录存在
            ensure_directories_exist()
        
        # 检查是否已存在处理结果，如果不强制重建则跳过处理
        rag_output_dir = os.path.join(CONFIG["rag_dir"], "processed")
        if not force_rebuild and os.path.exists(rag_output_dir):
            # 检查是否存在所有必要的RAG文件
            required_files = [
                "entity_embeddings.json", 
                "entity_mapping.json", 
                "graph_structure.json",
                "relation_embeddings.json", 
                "relation_mapping.json", 
                "triple_embeddings.json"
            ]
            all_files_exist = all(os.path.exists(os.path.join(rag_output_dir, f)) for f in required_files)
            
            if all_files_exist:
                print(f"检测到已存在的RAG处理结果，且force_rebuild=False，跳过处理流程")
                print(f"如需重新处理，请设置force_rebuild=True")
                return True, CONFIG["rag_dir"]
        
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
        if CONFIG.get("use_neo4j", False):
            self.step5_import_to_neo4j()
            
        # 步骤6：知识图谱RAG处理（整合原来的步骤6和7）
        if CONFIG.get("use_rag", True):
            self.step6_process_kg_for_rag()
        
        end_time = time.time()
        duration = end_time - start_time
        
        print("\n" + "*"*70)
        print(f"  知识图谱处理流水线执行完成！总耗时: {duration:.2f}秒")
        print("*"*70)
        print(f"\n结果文件：")
        print(f"- 文本分割结果: {CONFIG['segmented_dir']}/*.json")
        print(f"- 文本清洗结果: {CONFIG['cleaned_dir']}/*.json")
        print(f"- 关系提取结果: {CONFIG['kg_dir']}/*.json")
        print(f"- 知识图谱优化结果: {CONFIG['kg_dir']}/output/*")
        
        if CONFIG.get("use_neo4j", False) and self.neo4j_importer:
            print(f"- Neo4j浏览器访问: http://localhost:7474/ 登录查看知识图谱")
            # 关闭Neo4j连接
            self.neo4j_importer.close()
            
        if CONFIG.get("use_rag", True):
            print(f"- 知识图谱RAG处理结果: {CONFIG['rag_dir']}/processed/*")
        
        # 返回成功标志
        return True, CONFIG["rag_dir"]
    
    def run_pipeline(self, source_dir=None, force_rebuild=False):
        """运行完整的知识图谱处理流水线（同步包装异步函数）
        
        Args:
            source_dir: 可选的源文档目录路径
            force_rebuild: 是否强制重新构建知识图谱，即使已存在处理结果
        """
        if source_dir:
            CONFIG["source_dir"] = source_dir
            # 确保目录存在
            ensure_directories_exist()
        return asyncio.run(self.run_pipeline_async(source_dir=source_dir, force_rebuild=force_rebuild))


def main():
    """主函数，创建并运行知识图谱处理流水线"""
    
    # 创建并运行流水线
    pipeline = KnowledgeGraphPipeline()
    success,rag_dir = pipeline.run_pipeline()
    
    if success:
        print("\n知识图谱处理流水线执行成功")
    else:
        print("\n知识图谱处理流水线执行失败")
    
    return success


if __name__ == "__main__":
    main() 