import os
import json
import datetime
import time
from pathlib import Path

try:
    from neo4j import GraphDatabase
    NEO4J_AVAILABLE = True
except ImportError:
    NEO4J_AVAILABLE = False
    print("警告: Neo4j驱动未安装，无法使用Neo4j数据库功能")
    print("提示: 可以通过运行 'pip install neo4j' 安装Neo4j驱动")

class Neo4jImporter:
    """知识图谱Neo4j数据库导入工具"""
    
    def __init__(self, config=None):
        """初始化Neo4j导入器
        
        Args:
            config: 配置字典，包含Neo4j连接参数
        """
        self.config = config or {}
        
        # 使用时间戳创建新数据库名称（使用合法字符）
        timestamp = datetime.datetime.now().strftime("%Y%m%d%H%M")
        database_name = f"kg{timestamp}"
        
        # Neo4j数据库配置
        self.neo4j_config = {
            "uri": self.config.get("neo4j_uri", "bolt://localhost:7687"),
            "user": self.config.get("neo4j_user", "neo4j"),
            "password": self.config.get("neo4j_password", "password"),
            "database": self.config.get("neo4j_database", database_name),  # 如果配置中提供了数据库名称，则使用它
            "kg_dir": self.config.get("kg_dir", "knowledge_graph_data")
        }
        
        # 如果配置中没有提供数据库名称，使用时间戳生成的名称
        if self.neo4j_config["database"] is None:
            self.neo4j_config["database"] = database_name
            
        # 初始化数据库创建状态
        self.database_created = False
        
        print(f"使用Neo4j数据库: {self.neo4j_config['database']}")
        
        # 初始化Neo4j驱动
        self.neo4j_driver = None
        if NEO4J_AVAILABLE:
            try:
                self.neo4j_driver = GraphDatabase.driver(
                    self.neo4j_config["uri"],
                    auth=(self.neo4j_config["user"], self.neo4j_config["password"])
                )
                print(f"成功连接到Neo4j数据库: {self.neo4j_config['uri']}")
                
                # 创建新数据库
                self.database_created = self._create_database()
                if not self.database_created:
                    # 如果创建失败，使用默认数据库
                    self.neo4j_config["database"] = "neo4j"
                    print(f"使用默认数据库: {self.neo4j_config['database']}")
                    
                # 等待数据库创建完成
                if self.database_created:
                    print(f"等待数据库 {self.neo4j_config['database']} 启动...")
                    time.sleep(5)  # 等待数据库启动
                
            except Exception as e:
                print(f"连接Neo4j数据库失败: {e}")
                print("请检查Neo4j数据库是否运行，以及连接参数是否正确")
    
    def _create_database(self):
        """创建新的数据库
        
        Returns:
            bool: 数据库是否创建成功
        """
        if not self.neo4j_driver:
            return False
            
        try:
            # 连接到系统数据库创建新的数据库
            with self.neo4j_driver.session(database="system") as session:
                # 检查数据库是否已存在
                result = session.run("SHOW DATABASES")
                databases = [record["name"] for record in result]
                
                if self.neo4j_config["database"] not in databases:
                    print(f"创建新数据库: {self.neo4j_config['database']}")
                    try:
                        # 创建数据库
                        session.run(f"CREATE DATABASE {self.neo4j_config['database']} IF NOT EXISTS")
                        
                        # 验证数据库是否创建成功
                        time.sleep(2)  # 等待创建完成
                        result = session.run("SHOW DATABASES")
                        databases = [record["name"] for record in result]
                        
                        if self.neo4j_config["database"] in databases:
                            print(f"数据库 {self.neo4j_config['database']} 创建成功")
                            return True
                        else:
                            print(f"数据库 {self.neo4j_config['database']} 创建验证失败")
                            return False
                    except Exception as e:
                        print(f"创建数据库时出错: {e}")
                        return False
                else:
                    print(f"数据库 {self.neo4j_config['database']} 已存在")
                    return True
        except Exception as e:
            print(f"创建数据库时出错: {e}")
            return False
    
    def import_knowledge_graph(self, kg_data=None, kg_file_path=None):
        """将知识图谱导入到Neo4j数据库
        
        Args:
            kg_data: 知识图谱数据字典，如果为None则从文件加载
            kg_file_path: 知识图谱数据文件路径，如果为None则使用默认路径
            
        Returns:
            tuple: (导入是否成功, 数据库名称)
        """
        if not NEO4J_AVAILABLE:
            print("错误: Neo4j驱动未安装，无法执行导入")
            print("提示: 运行 'pip install neo4j' 安装Neo4j驱动后重试")
            return False, None
            
        if not self.neo4j_driver:
            print("错误: 未连接到Neo4j数据库，请检查配置")
            return False, None
            
        try:
            # 如果没有提供数据，则从文件加载
            if kg_data is None:
                if kg_file_path is None:
                    # 尝试在默认目录中查找所有知识图谱文件
                    kg_data = []
                    
                    # 首先查找优化后的文件
                    optimized_files = list(Path(self.neo4j_config['kg_dir']).glob('output/optimized_*.json'))
                    if optimized_files:
                        for file_path in optimized_files:
                            print(f"从文件加载知识图谱数据: {file_path}")
                            with open(file_path, 'r', encoding='utf-8') as f:
                                file_data = json.load(f)
                                if file_data:
                                    kg_data.append(file_data)
                    else:
                        # 没有找到优化后的文件，查找原始文件
                        relation_files = list(Path(self.neo4j_config['kg_dir']).glob('*_relations.json'))
                        if relation_files:
                            for file_path in relation_files:
                                print(f"从文件加载知识图谱数据: {file_path}")
                                with open(file_path, 'r', encoding='utf-8') as f:
                                    file_data = json.load(f)
                                    if file_data:
                                        kg_data.append(file_data)
                        else:
                            print(f"错误: 在 {self.neo4j_config['kg_dir']} 中未找到知识图谱数据文件")
                            return False, None
                else:
                    # 如果是字符串路径，可能是单个文件或通配符
                    if isinstance(kg_file_path, str):
                        if '*' in kg_file_path:
                            # 通配符路径，查找所有匹配的文件
                            pattern_dir = os.path.dirname(kg_file_path)
                            pattern_file = os.path.basename(kg_file_path)
                            matching_files = list(Path(pattern_dir).glob(pattern_file))
                            
                            if not matching_files:
                                print(f"错误: 未找到匹配 {kg_file_path} 的文件")
                                return False, None
                            
                            kg_data = []
                            for file_path in matching_files:
                                print(f"从文件加载知识图谱数据: {file_path}")
                                with open(file_path, 'r', encoding='utf-8') as f:
                                    file_data = json.load(f)
                                    if file_data:
                                        kg_data.append(file_data)
                        else:
                            # 单个文件路径
                            print(f"从文件加载知识图谱数据: {kg_file_path}")
                            with open(kg_file_path, 'r', encoding='utf-8') as f:
                                file_data = json.load(f)
                                kg_data = [file_data]
                    elif isinstance(kg_file_path, list):
                        # 如果是文件路径列表，加载所有文件
                        kg_data = []
                        for file_path in kg_file_path:
                            print(f"从文件加载知识图谱数据: {file_path}")
                            with open(file_path, 'r', encoding='utf-8') as f:
                                file_data = json.load(f)
                                if file_data:
                                    kg_data.append(file_data)
                
            # 执行导入
            success = self._import_to_neo4j(kg_data)
            return success, self.neo4j_config["database"]
                
        except Exception as e:
            print(f"导入Neo4j时出错: {e}")
            return False, None
    
    def import_multiple_kg_files(self, file_paths):
        """导入多个知识图谱文件到Neo4j数据库
        
        Args:
            file_paths: 知识图谱文件路径列表
            
        Returns:
            tuple: (导入是否成功, 数据库名称)
        """
        if not file_paths:
            print("错误: 未提供知识图谱文件路径")
            return False, None
            
        return self.import_knowledge_graph(kg_file_path=file_paths)
    
    def _create_constraints(self):
        """创建数据库约束"""
        print("创建实体唯一约束...")
        with self.neo4j_driver.session(database=self.neo4j_config["database"]) as session:
            try:
                session.run("CREATE CONSTRAINT entity_name IF NOT EXISTS FOR (n:Entity) REQUIRE n.name IS UNIQUE")
            except:
                # 尝试旧版Neo4j语法
                try:
                    session.run("CREATE CONSTRAINT ON (n:Entity) ASSERT n.name IS UNIQUE")
                except:
                    print("警告: 无法创建实体约束，可能会导致重复节点")
                    
            # 创建索引以提高查询性能
            print("创建索引以提高查询性能...")
            try:
                session.run("CREATE INDEX entity_name_idx IF NOT EXISTS FOR (n:Entity) ON (n.name)")
                session.run("CREATE INDEX relation_type_idx IF NOT EXISTS FOR ()-[r]-() ON (type(r))")
            except:
                print("警告: 无法创建索引，这可能会影响查询性能")
    
    def _import_to_neo4j(self, kg_data_list):
        """将知识图谱数据导入到Neo4j数据库
        
        Args:
            kg_data_list: 知识图谱数据列表，每个元素可以是字典或列表
        """
        if not self.neo4j_driver:
            return False
            
        # 验证数据库是否存在
        try:
            with self.neo4j_driver.session(database="system") as session:
                result = session.run("SHOW DATABASES")
                databases = [record["name"] for record in result]
                
                if self.neo4j_config["database"] not in databases:
                    print(f"错误: 数据库 {self.neo4j_config['database']} 不存在")
                    if "neo4j" in databases:
                        print("使用默认数据库 'neo4j'")
                        self.neo4j_config["database"] = "neo4j"
                    else:
                        print("错误: 无法找到可用的数据库")
                        return False
        except Exception as e:
            print(f"验证数据库时出错: {e}")
            print("尝试使用默认数据库 'neo4j'")
            self.neo4j_config["database"] = "neo4j"
            
        try:
            print(f"开始导入知识图谱到Neo4j数据库 {self.neo4j_config['database']}...")
            
            # 清空数据库
            print("清空现有数据...")
            with self.neo4j_driver.session(database=self.neo4j_config["database"]) as session:
                session.run("MATCH (n) DETACH DELETE n")
                
            # 创建约束
            self._create_constraints()
            
            # 导入所有实体和关系
            print("导入实体和关系...")
            with self.neo4j_driver.session(database=self.neo4j_config["database"]) as session:
                # 处理关系三元组
                all_relations = []
                
                # 处理所有知识图谱数据
                for kg_data in kg_data_list:
                    # 如果是列表，直接处理
                    if isinstance(kg_data, list):
                        for triple in kg_data:
                            if len(triple) >= 3:
                                subject, relation, object_ = triple[:3]
                                all_relations.append((subject, relation, object_))
                    else:
                        # 如果是字典，则遍历所有部分
                        for section, triples in kg_data.items():
                            if isinstance(triples, list):
                                for triple in triples:
                                    if len(triple) >= 3:
                                        subject, relation, object_ = triple[:3]
                                        all_relations.append((subject, relation, object_))
                
                # 批量导入关系
                if all_relations:
                    total_relations = len(all_relations)
                    print(f"准备导入 {total_relations} 个关系...")
                    
                    # 分批导入关系，每次50个
                    batch_size = 50
                    for i in range(0, total_relations, batch_size):
                        batch = all_relations[i:i+batch_size]
                        self._create_batch_relations(session, batch)
                        print(f"已导入 {min(i+batch_size, total_relations)}/{total_relations} 个关系")
                
                print(f"已导入 {len(all_relations)} 个关系到Neo4j数据库 {self.neo4j_config['database']}")
                
            print("知识图谱成功导入Neo4j数据库！")
            print(f"浏览器访问: http://localhost:7474/ 登录查看知识图谱")
            return True
            
        except Exception as e:
            print(f"导入Neo4j时出错: {e}")
            return False
    
    def _create_batch_relations(self, session, relations):
        """批量创建实体和关系"""
        # 构建Cypher查询
        query = """
        UNWIND $relations as rel
        MERGE (s:Entity {name: rel[0]})
        MERGE (o:Entity {name: rel[2]})
        WITH s, o, rel
        CALL apoc.create.relationship(s, rel[1], {}, o)
        YIELD rel as relationship
        RETURN count(relationship)
        """
        
        # 检查是否支持APOC
        try:
            result = session.run(query, relations=relations)
            result.single()
            return True
        except:
            # 如果APOC不可用，使用基本的关系创建方法
            try:
                for subj, rel_type, obj in relations:
                    # 清理关系类型，确保它是有效的Neo4j关系类型
                    rel_type = rel_type.replace(" ", "_").replace("-", "_")
                    if not rel_type:
                        rel_type = "RELATED_TO"
                    
                    # 创建关系的查询
                    basic_query = f"""
                    MERGE (s:Entity {{name: $subj}})
                    MERGE (o:Entity {{name: $obj}})
                    MERGE (s)-[r:{rel_type}]->(o)
                    """
                    session.run(basic_query, subj=subj, obj=obj)
                return True
            except Exception as e:
                print(f"创建关系时出错: {e}")
                return False
    
    def get_database_name(self):
        """获取当前数据库名称"""
        return self.neo4j_config["database"]
    
    def close(self):
        """关闭Neo4j连接"""
        if self.neo4j_driver:
            self.neo4j_driver.close()
            print("已关闭Neo4j数据库连接")


def main():
    """主函数，演示如何使用Neo4jImporter导入知识图谱"""
    # 配置Neo4j连接参数
    config = {
        "neo4j_uri": "bolt://localhost:7687",  # Neo4j连接URI
        "neo4j_user": "neo4j",  # Neo4j用户名
        "neo4j_password": "summer123456",  # Neo4j密码
        "kg_dir": "knowledge_graph_data",  # 知识图谱数据目录
    }
    
    # 创建导入器（会自动创建带时间戳的数据库）
    importer = Neo4jImporter(config)
    
    # 导入知识图谱（多图模式）
    success, db_name = importer.import_knowledge_graph()
    
    if success:
        print(f"成功导入到数据库: {db_name}")
    
    # 关闭连接
    importer.close()


if __name__ == "__main__":
    main() 