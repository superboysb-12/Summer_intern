import os
import json
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
        
        # Neo4j数据库配置
        self.neo4j_config = {
            "uri": self.config.get("neo4j_uri", "bolt://localhost:7687"),
            "user": self.config.get("neo4j_user", "neo4j"),
            "password": self.config.get("neo4j_password", "password"),
            "database": self.config.get("neo4j_database", "neo4j"),
            "kg_dir": self.config.get("kg_dir", "knowledge_graph_data"),
            "multi_graph": self.config.get("multi_graph", False)  # 是否为每个文件创建独立的图
        }
        
        # 初始化Neo4j驱动
        self.neo4j_driver = None
        if NEO4J_AVAILABLE:
            try:
                self.neo4j_driver = GraphDatabase.driver(
                    self.neo4j_config["uri"],
                    auth=(self.neo4j_config["user"], self.neo4j_config["password"])
                )
                print(f"成功连接到Neo4j数据库: {self.neo4j_config['uri']}")
            except Exception as e:
                print(f"连接Neo4j数据库失败: {e}")
                print("请检查Neo4j数据库是否运行，以及连接参数是否正确")
    
    def import_knowledge_graph(self, kg_data=None, kg_file_path=None):
        """将知识图谱导入到Neo4j数据库
        
        Args:
            kg_data: 知识图谱数据字典，如果为None则从文件加载
            kg_file_path: 知识图谱数据文件路径，如果为None则使用默认路径
            
        Returns:
            bool: 导入是否成功
        """
        if not NEO4J_AVAILABLE:
            print("错误: Neo4j驱动未安装，无法执行导入")
            print("提示: 运行 'pip install neo4j' 安装Neo4j驱动后重试")
            return False
            
        if not self.neo4j_driver:
            print("错误: 未连接到Neo4j数据库，请检查配置")
            return False
            
        try:
            # 如果没有提供数据，则从文件加载
            if kg_data is None:
                if kg_file_path is None:
                    # 尝试找到优化后的知识图谱数据
                    kg_file_path = f"{self.neo4j_config['kg_dir']}/output/inferred_relations.json"
                    if not os.path.exists(kg_file_path):
                        kg_file_path = f"{self.neo4j_config['kg_dir']}/all_relations.json"
                        
                if not os.path.exists(kg_file_path):
                    print(f"错误: 找不到知识图谱数据文件 {kg_file_path}")
                    return False
                    
                print(f"从文件加载知识图谱数据: {kg_file_path}")
                with open(kg_file_path, 'r', encoding='utf-8') as f:
                    kg_data = json.load(f)
                
            # 执行导入
            return self._import_kg_to_neo4j(kg_data)
                
        except Exception as e:
            print(f"导入Neo4j时出错: {e}")
            return False
    
    def import_all_files_as_separate_graphs(self):
        """导入每个文件作为独立的图"""
        if not NEO4J_AVAILABLE or not self.neo4j_driver:
            print("错误: Neo4j连接不可用")
            return False
            
        # 查找所有单独文件的关系数据
        relation_files = list(Path(self.neo4j_config['kg_dir']).glob('*_relations.json'))
        if not relation_files:
            print(f"未找到任何关系文件，请先运行关系提取步骤")
            return False
            
        print(f"找到 {len(relation_files)} 个关系文件，将每个文件导入为独立图...")
        
        # 清空数据库
        print("清空现有数据...")
        with self.neo4j_driver.session(database=self.neo4j_config["database"]) as session:
            session.run("MATCH (n) DETACH DELETE n")
        
        # 创建约束
        self._create_constraints()
        
        # 为每个文件创建独立图
        success_count = 0
        for file_path in relation_files:
            try:
                file_name = file_path.stem.replace('_relations', '')
                print(f"导入文件 {file_name} 作为独立图...")
                
                with open(file_path, 'r', encoding='utf-8') as f:
                    relations = json.load(f)
                
                # 导入单个文件的关系
                if self._import_file_as_graph(relations, file_name):
                    success_count += 1
                    print(f"成功导入 {file_name} 作为独立图")
                else:
                    print(f"导入 {file_name} 失败")
            
            except Exception as e:
                print(f"处理文件 {file_path} 时出错: {e}")
        
        print(f"完成导入，成功导入 {success_count}/{len(relation_files)} 个文件作为独立图")
        return success_count > 0
    
    def _create_constraints(self):
        """创建数据库约束"""
        print("创建实体唯一约束...")
        with self.neo4j_driver.session(database=self.neo4j_config["database"]) as session:
            try:
                session.run("CREATE CONSTRAINT entity_name IF NOT EXISTS FOR (n:Entity) REQUIRE n.name IS UNIQUE")
            except:
                # 旧版Neo4j语法
                try:
                    session.run("CREATE CONSTRAINT ON (n:Entity) ASSERT n.name IS UNIQUE")
                except:
                    print("警告: 无法创建实体约束，可能会导致重复节点")
                    
            # 创建索引以提高查询性能
            print("创建索引以提高查询性能...")
            try:
                session.run("CREATE INDEX entity_name_idx IF NOT EXISTS FOR (n:Entity) ON (n.name)")
                session.run("CREATE INDEX entity_source_idx IF NOT EXISTS FOR (n:Entity) ON (n.source)")
                session.run("CREATE INDEX relation_type_idx IF NOT EXISTS FOR ()-[r]-() ON (type(r))")
            except:
                print("警告: 无法创建索引，这可能会影响查询性能")
    
    def _import_file_as_graph(self, relations, file_name):
        """将单个文件的关系导入为独立图"""
        if not isinstance(relations, list):
            print(f"警告: {file_name} 的数据格式不正确，应为关系列表")
            return False
            
        try:
            with self.neo4j_driver.session(database=self.neo4j_config["database"]) as session:
                # 处理关系三元组
                batch_relations = []
                # 每次处理50个关系
                for i in range(0, len(relations), 50):
                    batch = relations[i:i+50]
                    for triple in batch:
                        if len(triple) == 3:
                            subject, relation, object_ = triple
                            batch_relations.append((subject, relation, object_))
                    
                    if batch_relations:
                        self._create_batch_relations_for_file(session, batch_relations, file_name)
                        batch_relations = []
                
                print(f"文件 {file_name} 导入完成，包含 {len(relations)} 个关系")
                return True
                
        except Exception as e:
            print(f"导入文件 {file_name} 时出错: {e}")
            return False
    
    def _import_kg_to_neo4j(self, kg_data):
        """将知识图谱数据导入到Neo4j数据库"""
        if not self.neo4j_driver:
            return False
            
        try:
            print("开始导入知识图谱到Neo4j数据库...")
            
            # 清空数据库
            print("清空现有数据...")
            with self.neo4j_driver.session(database=self.neo4j_config["database"]) as session:
                session.run("MATCH (n) DETACH DELETE n")
                
            # 创建约束
            self._create_constraints()
            
            # 检查是否需要导入每个文件为独立图
            if self.neo4j_config.get("multi_graph", False):
                return self.import_all_files_as_separate_graphs()
            
            # 否则按标准方式导入所有关系
            # 导入所有实体和关系
            print("导入实体和关系...")
            with self.neo4j_driver.session(database=self.neo4j_config["database"]) as session:
                # 统计要导入的数据
                entity_count = 0
                relation_count = 0
                
                # 处理每个部分的关系三元组
                for section, triples in kg_data.items():
                    print(f"处理 {section} 部分...")
                    # 如果是列表，直接处理三元组
                    if isinstance(triples, list):
                        batch_relations = []
                        # 每次处理50个关系
                        for i in range(0, len(triples), 50):
                            batch = triples[i:i+50]
                            for triple in batch:
                                if len(triple) == 3:
                                    subject, relation, object_ = triple
                                    batch_relations.append((subject, relation, object_))
                            
                            if batch_relations:
                                self._create_batch_relations(session, batch_relations, section)
                                entity_count += len(set([r[0] for r in batch_relations] + [r[2] for r in batch_relations]))
                                relation_count += len(batch_relations)
                                batch_relations = []
                
                print(f"已导入 {entity_count} 个实体和 {relation_count} 个关系到Neo4j数据库")
                
            print("知识图谱成功导入Neo4j数据库！")
            print(f"浏览器访问: http://localhost:7474/ 登录查看知识图谱")
            return True
            
        except Exception as e:
            print(f"导入Neo4j时出错: {e}")
            return False
    
    def _create_batch_relations_for_file(self, session, relations, file_name):
        """为独立图批量创建实体和关系"""
        # 标准化文件名作为标签
        graph_label = self._normalize_label(file_name)
        
        # 检查是否支持APOC
        try:
            # 使用APOC创建带有文件标签的实体和关系
            query = """
            UNWIND $relations as rel
            MERGE (s:Entity {name: rel[0]})
            ON CREATE SET s.source = $file_name, s:`""" + graph_label + """`
            ON MATCH SET s:`""" + graph_label + """`
            MERGE (o:Entity {name: rel[2]})
            ON CREATE SET o.source = $file_name, o:`""" + graph_label + """`
            ON MATCH SET o:`""" + graph_label + """`
            WITH s, o, rel
            CALL apoc.create.relationship(s, rel[1], {source: $file_name}, o)
            YIELD rel as relationship
            RETURN count(relationship)
            """
            
            result = session.run(query, relations=relations, file_name=file_name)
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
                    ON CREATE SET s.source = $file_name, s:{graph_label}
                    ON MATCH SET s:{graph_label}
                    MERGE (o:Entity {{name: $obj}})
                    ON CREATE SET o.source = $file_name, o:{graph_label}
                    ON MATCH SET o:{graph_label}
                    MERGE (s)-[r:{rel_type} {{source: $file_name}}]->(o)
                    """
                    session.run(basic_query, subj=subj, obj=obj, file_name=file_name)
                return True
            except Exception as e:
                print(f"创建关系时出错: {e}")
                return False
    
    def _normalize_label(self, label):
        """标准化Neo4j标签名称"""
        # 移除非法字符，替换空格和连字符
        label = ''.join(c for c in label if c.isalnum() or c in [' ', '-', '_'])
        label = label.replace(' ', '_').replace('-', '_')
        
        # 确保标签名称以字母开头
        if not label[0].isalpha():
            label = 'F_' + label
            
        return label
    
    def _create_batch_relations(self, session, relations, section):
        """批量创建实体和关系"""
        # 构建Cypher查询
        query = """
        UNWIND $relations as rel
        MERGE (s:Entity {name: rel[0]})
        MERGE (o:Entity {name: rel[2]})
        WITH s, o, rel
        CALL apoc.create.relationship(s, rel[1], {section: $section}, o)
        YIELD rel as relationship
        RETURN count(relationship)
        """
        
        # 检查是否支持APOC
        try:
            result = session.run(query, relations=relations, section=section)
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
                    MERGE (s)-[r:{rel_type} {{section: $section}}]->(o)
                    """
                    session.run(basic_query, subj=subj, obj=obj, section=section)
                return True
            except Exception as e:
                print(f"创建关系时出错: {e}")
                return False
    
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
        "neo4j_database": "KnowledgeGraph",  # Neo4j数据库名称
        "kg_dir": "knowledge_graph_data",  # 知识图谱数据目录
        "multi_graph": True  # 启用多图模式，每个文件形成独立图
    }
    
    # 创建导入器
    importer = Neo4jImporter(config)
    
    # 导入知识图谱（多图模式）
    importer.import_knowledge_graph()
    
    # 关闭连接
    importer.close()


if __name__ == "__main__":
    main() 