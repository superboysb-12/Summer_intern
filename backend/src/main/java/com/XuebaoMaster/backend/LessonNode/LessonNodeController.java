package com.XuebaoMaster.backend.LessonNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/lesson-nodes")
public class LessonNodeController {
    @Autowired
    private LessonNodeService lessonNodeService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建课时节点
     * 
     * @param lessonNode 课时节点信息
     * @return 创建的课时节点
     */
    @PostMapping
    public ResponseEntity<LessonNode> createLessonNode(@RequestBody LessonNode lessonNode) {
        return ResponseEntity.ok(lessonNodeService.createLessonNode(lessonNode));
    }

    /**
     * 获取所有课时节点
     * 
     * @return 课时节点列表
     */
    @GetMapping
    public ResponseEntity<List<LessonNode>> getAllLessonNodes() {
        return ResponseEntity.ok(lessonNodeService.getAllLessonNodes());
    }

    /**
     * 获取指定ID的课时节点
     * 
     * @param id 课时节点ID
     * @return 课时节点信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<LessonNode> getLessonNodeById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonNodeService.getLessonNodeById(id));
    }

    /**
     * 获取指定课程的所有课时节点
     * 
     * @param courseId 课程ID
     * @return 课时节点列表
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<LessonNode>> getLessonNodesByCourseId(@PathVariable Long courseId) {
        return ResponseEntity.ok(lessonNodeService.getLessonNodesByCourseId(courseId));
    }

    /**
     * 获取指定课程的所有课时节点（按顺序排列）
     * 
     * @param courseId 课程ID
     * @return 课时节点列表（按顺序排列）
     */
    @GetMapping("/course/{courseId}/ordered")
    public ResponseEntity<List<LessonNode>> getLessonNodesByCourseIdOrdered(@PathVariable Long courseId) {
        return ResponseEntity.ok(lessonNodeService.getLessonNodesByCourseIdOrdered(courseId));
    }

    /**
     * 获取指定课程和顺序的课时节点
     * 
     * @param courseId 课程ID
     * @param nodeOrder 节点顺序
     * @return 课时节点信息
     */
    @GetMapping("/course/{courseId}/order/{nodeOrder}")
    public ResponseEntity<LessonNode> getLessonNodeByCourseIdAndOrder(
            @PathVariable Long courseId, @PathVariable Integer nodeOrder) {
        return ResponseEntity.ok(lessonNodeService.getLessonNodeByCourseIdAndOrder(courseId, nodeOrder));
    }

    /**
     * 更新课时节点
     * 
     * @param id 课时节点ID
     * @param lessonNode 更新的课时节点信息
     * @return 更新后的课时节点
     */
    @PutMapping("/{id}")
    public ResponseEntity<LessonNode> updateLessonNode(@PathVariable Long id, @RequestBody LessonNode lessonNode) {
        lessonNode.setId(id);
        return ResponseEntity.ok(lessonNodeService.updateLessonNode(lessonNode));
    }

    /**
     * 删除课时节点
     * 
     * @param id 课时节点ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLessonNode(@PathVariable Long id) {
        lessonNodeService.deleteLessonNode(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 搜索课时节点
     * 
     * @param keyword 关键字
     * @return 匹配的课时节点列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<LessonNode>> searchLessonNodes(@RequestParam String keyword) {
        return ResponseEntity.ok(lessonNodeService.searchLessonNodes(keyword));
    }

    /**
     * 生成RAG
     * 
     * @param id 课时节点ID
     * @return RAG生成结果
     */
    @GetMapping("/generate-rag")
    public ResponseEntity<Map<String, Object>> generateRag(@RequestParam Long id) {
        try {
            // 获取课时节点
            LessonNode lessonNode = lessonNodeService.getLessonNodeById(id);
            System.out.println("获取到课时节点: " + lessonNode.getId() + ", 标题: " + lessonNode.getTitle() + ", 当前pathToNodes: " + lessonNode.getPathToNodes());
            
            // 调用服务生成RAG
            String ragDir = lessonNodeService.generateRag(lessonNode.getPathToNodes());
            System.out.println("生成的RAG路径: " + ragDir);
            
            try {
            // 更新课时节点的pathToNodes字段
                System.out.println("准备设置pathToNodes为: " + ragDir);
            lessonNode.setPathToNodes(ragDir);
                
                // 打印更新前的节点信息
                System.out.println("更新前的课时节点: ID=" + lessonNode.getId() 
                    + ", 标题=" + lessonNode.getTitle() 
                    + ", pathToNodes=" + lessonNode.getPathToNodes());
                
                LessonNode updatedNode = lessonNodeService.updateLessonNode(lessonNode);
                
                // 打印更新后的节点信息
                System.out.println("更新后的课时节点: ID=" + updatedNode.getId() 
                    + ", 标题=" + updatedNode.getTitle() 
                    + ", pathToNodes=" + updatedNode.getPathToNodes());
                
                // 再次从数据库获取节点，确认更新是否持久化
                LessonNode verifiedNode = lessonNodeService.getLessonNodeById(id);
                System.out.println("从数据库重新获取的节点: ID=" + verifiedNode.getId() 
                    + ", 标题=" + verifiedNode.getTitle() 
                    + ", pathToNodes=" + verifiedNode.getPathToNodes());
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("rag_dir", ragDir);
                response.put("data", verifiedNode);
            
            return ResponseEntity.ok(response);
            } catch (Exception e) {
                System.out.println("更新课时节点时出错: " + e.getMessage());
                e.printStackTrace();
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "partial_success");
                errorResponse.put("rag_dir", ragDir);
                errorResponse.put("message", "RAG生成成功，但更新课时节点失败: " + e.getMessage());
                errorResponse.put("error_details", e.getMessage());
                
                return ResponseEntity.ok(errorResponse);
            }
        } catch (Exception e) {
            System.out.println("生成RAG出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 生成RAG（带路径参数）
     * 
     * @param id 课时节点ID (可选)
     * @param sourcePath 源文件路径
     * @return RAG生成结果
     */
    @GetMapping("/generate-rag-with-path")
    public ResponseEntity<Map<String, Object>> generateRagWithPath(
            @RequestParam(required = false) Long id, 
            @RequestParam String sourcePath) {
        try {
            System.out.println("开始生成RAG，源路径: " + sourcePath + ", 节点ID: " + id);
            
            // 调用服务生成RAG
            String ragDir = lessonNodeService.generateRag(sourcePath);
            System.out.println("生成的RAG路径: " + ragDir);
            
            // 构建基本响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("rag_dir", ragDir);
            
            // 如果提供了ID，则更新对应的课时节点
            if (id != null) {
                try {
                LessonNode lessonNode = lessonNodeService.getLessonNodeById(id);
                    System.out.println("获取到课时节点: " + lessonNode.getId() + ", 标题: " + lessonNode.getTitle() + ", 当前pathToNodes: " + lessonNode.getPathToNodes());
                
                // 更新课时节点的pathToNodes字段
                    System.out.println("准备设置pathToNodes为: " + ragDir);
                lessonNode.setPathToNodes(ragDir);
                    
                    // 打印更新前的节点信息
                    System.out.println("更新前的课时节点: ID=" + lessonNode.getId() 
                        + ", 标题=" + lessonNode.getTitle() 
                        + ", pathToNodes=" + lessonNode.getPathToNodes());
                    
                LessonNode updatedNode = lessonNodeService.updateLessonNode(lessonNode);
                    
                    // 打印更新后的节点信息
                    System.out.println("更新后的课时节点: ID=" + updatedNode.getId() 
                        + ", 标题=" + updatedNode.getTitle() 
                        + ", pathToNodes=" + updatedNode.getPathToNodes());
                    
                    // 再次从数据库获取节点，确认更新是否持久化
                    LessonNode verifiedNode = lessonNodeService.getLessonNodeById(id);
                    System.out.println("从数据库重新获取的节点: ID=" + verifiedNode.getId() 
                        + ", 标题=" + verifiedNode.getTitle() 
                        + ", pathToNodes=" + verifiedNode.getPathToNodes());
                
                response.put("message", "RAG生成成功并已更新课时节点");
                    response.put("data", verifiedNode);
                } catch (Exception e) {
                    System.out.println("更新课时节点时出错: " + e.getMessage());
                    e.printStackTrace();
                    
                    response.put("message", "RAG生成成功，但更新课时节点失败: " + e.getMessage());
                    response.put("error_details", e.getMessage());
                }
            } else {
                response.put("message", "RAG生成成功");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("生成RAG出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 查询RAG
     * 
     * @param query 查询内容
     * @param ragPath RAG数据路径
     * @return 查询结果
     */
    @GetMapping("/query-rag")
    public ResponseEntity<Map<String, Object>> queryRag(
            @RequestParam String query,
            @RequestParam(required = false) String ragPath) {
        try {
            // 如果未提供ragPath，使用默认路径
            if (ragPath == null || ragPath.isEmpty()) {
                ragPath = "D:/大三下/综合实习/Summer_intern/work_flow/kg_rag_service/doc/2025-07-02_15-29-04/rag_data/processed";
            }
            
            System.out.println("查询RAG，查询内容: " + query + ", 路径: " + ragPath);
            
            // 调用服务查询RAG
            String result = lessonNodeService.queryRag(query, ragPath);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("查询RAG出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 查询特定课时节点的RAG
     * 
     * @param id 课时节点ID
     * @param query 查询内容
     * @return 查询结果
     */
    @GetMapping("/{id}/query-rag")
    public ResponseEntity<Map<String, Object>> queryNodeRag(
            @PathVariable Long id,
            @RequestParam String query) {
        try {
            // 获取课时节点
            LessonNode lessonNode = lessonNodeService.getLessonNodeById(id);
            System.out.println("获取到课时节点: " + lessonNode.getId() + ", 标题: " + lessonNode.getTitle());
            
            // 获取节点的RAG路径
            String ragPath = lessonNode.getPathToNodes();
            System.out.println("课时节点的RAG路径: " + ragPath);
            
            // 如果路径为空，返回错误
            if (ragPath == null || ragPath.isEmpty()) {
                throw new RuntimeException("该课时节点没有关联的RAG数据");
            }
            
            // 调用服务查询RAG
            String result = lessonNodeService.queryRag(query, ragPath);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", result);
            response.put("lessonNode", lessonNode);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("查询节点RAG出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 发送聊天消息
     * 
     * @param request 请求体，符合Dify API格式
     * @return 聊天响应
     */
    @PostMapping("/chat")
    public ResponseEntity<Object> chatMessage(@RequestBody ChatMessageRequest request) {
        try {
            // 从请求体中提取查询内容
            String query = request.getQuery();
            if (query == null || query.isEmpty()) {
                throw new IllegalArgumentException("查询内容不能为空");
            }
            
            System.out.println("发送聊天消息: " + query);
            
            // 创建附加参数
            Map<String, Object> additionalParams = new HashMap<>();
            additionalParams.put("response_mode", request.getResponse_mode());
            additionalParams.put("conversation_id", request.getConversation_id());
            additionalParams.put("user", request.getUser());
            additionalParams.put("files", request.getFiles());
            additionalParams.put("inputs", request.getInputs());
            
            // 调用服务发送聊天消息
            String result = lessonNodeService.sendChatMessage(query, additionalParams);
            
            // 尝试解析结果为JSON
            try {
                JsonNode jsonResponse = objectMapper.readTree(result);
                return ResponseEntity.ok(jsonResponse);
            } catch (Exception e) {
                // 如果解析失败，返回原始字符串
                System.out.println("JSON解析失败，返回原始响应: " + e.getMessage());
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("response", result);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            System.out.println("发送聊天消息出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 创建带有RAG路径的课时节点
     * 
     * @param courseId 课程ID
     * @param nodeOrder 节点顺序
     * @param title 节点标题
     * @param ragPath RAG路径
     * @return 创建的课时节点
     */
    @PostMapping("/create-with-rag")
    public ResponseEntity<Map<String, Object>> createLessonNodeWithRag(
            @RequestParam Long courseId,
            @RequestParam Integer nodeOrder,
            @RequestParam String title,
            @RequestParam String ragPath) {
        try {
            System.out.println("创建带有RAG路径的课时节点: 课程ID=" + courseId + ", 顺序=" + nodeOrder + ", 标题=" + title + ", RAG路径=" + ragPath);
            
            // 创建新的课时节点
            LessonNode lessonNode = new LessonNode();
            
            // 设置课程
            com.XuebaoMaster.backend.Course.Course course = new com.XuebaoMaster.backend.Course.Course();
            course.setCourseId(courseId);
            lessonNode.setCourse(course);
            
            // 设置其他属性
            lessonNode.setNodeOrder(nodeOrder);
            lessonNode.setTitle(title);
            lessonNode.setPathToNodes(ragPath);
            
            // 保存节点
            LessonNode createdNode = lessonNodeService.createLessonNode(lessonNode);
            System.out.println("创建的课时节点: ID=" + createdNode.getId() 
                + ", 标题=" + createdNode.getTitle() 
                + ", pathToNodes=" + createdNode.getPathToNodes());
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "课时节点创建成功");
            response.put("data", createdNode);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("创建课时节点出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 生成RAG并创建新节点
     * 
     * @param courseId 课程ID
     * @param nodeOrder 节点顺序
     * @param title 节点标题
     * @param sourcePath 源文件路径
     * @return 创建的课时节点
     */
    @PostMapping("/generate-and-create")
    public ResponseEntity<Map<String, Object>> generateRagAndCreateNode(
            @RequestParam Long courseId,
            @RequestParam Integer nodeOrder,
            @RequestParam String title,
            @RequestParam String sourcePath) {
        try {
            System.out.println("生成RAG并创建节点: 课程ID=" + courseId + ", 顺序=" + nodeOrder + ", 标题=" + title + ", 源路径=" + sourcePath);
            
            // 调用服务生成RAG
            String ragDir = lessonNodeService.generateRag(sourcePath);
            System.out.println("生成的RAG路径: " + ragDir);
            
            // 创建新的课时节点
            LessonNode lessonNode = new LessonNode();
            
            // 设置课程
            com.XuebaoMaster.backend.Course.Course course = new com.XuebaoMaster.backend.Course.Course();
            course.setCourseId(courseId);
            lessonNode.setCourse(course);
            
            // 设置其他属性
            lessonNode.setNodeOrder(nodeOrder);
            lessonNode.setTitle(title);
            lessonNode.setPathToNodes(ragDir);
            
            // 保存节点
            LessonNode createdNode = lessonNodeService.createLessonNode(lessonNode);
            System.out.println("创建的课时节点: ID=" + createdNode.getId() 
                + ", 标题=" + createdNode.getTitle() 
                + ", pathToNodes=" + createdNode.getPathToNodes());
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "RAG生成成功并创建了课时节点");
            response.put("rag_dir", ragDir);
            response.put("data", createdNode);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("生成RAG并创建节点出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 更新课时节点的知识图谱路径
     * 
     * @param id 课时节点ID
     * @param graphPath 知识图谱路径
     * @return 更新后的课时节点
     */
    @PutMapping("/{id}/update-graph-path")
    public ResponseEntity<Map<String, Object>> updateGraphPath(
            @PathVariable Long id,
            @RequestParam String graphPath) {
        try {
            // 处理路径中的反斜杠问题
            graphPath = graphPath.replace('\\', '/');
            System.out.println("更新课时节点的知识图谱路径: ID=" + id + ", 图谱路径=" + graphPath);
            
            // 获取课时节点
            LessonNode lessonNode = lessonNodeService.getLessonNodeById(id);
            System.out.println("获取到课时节点: ID=" + lessonNode.getId() 
                + ", 标题=" + lessonNode.getTitle()
                + ", 当前pathToGraph=" + lessonNode.getPathToGraph());
            
            // 更新知识图谱路径
            lessonNode.setPathToGraph(graphPath);
            
            // 保存更新
            LessonNode updatedNode = lessonNodeService.updateLessonNode(lessonNode);
            System.out.println("更新后的课时节点: ID=" + updatedNode.getId() 
                + ", 标题=" + updatedNode.getTitle() 
                + ", pathToGraph=" + updatedNode.getPathToGraph());
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "知识图谱路径更新成功");
            response.put("data", updatedNode);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("更新知识图谱路径出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 创建带有知识图谱路径的课时节点
     * 
     * @param courseId 课程ID
     * @param nodeOrder 节点顺序
     * @param title 节点标题
     * @param ragPath RAG路径
     * @param graphPath 知识图谱路径
     * @return 创建的课时节点
     */
    @PostMapping("/create-with-graph")
    public ResponseEntity<Map<String, Object>> createLessonNodeWithGraph(
            @RequestParam Long courseId,
            @RequestParam Integer nodeOrder,
            @RequestParam String title,
            @RequestParam(required = false) String ragPath,
            @RequestParam String graphPath) {
        try {
            // 处理路径中的反斜杠问题
            if (ragPath != null) {
                ragPath = ragPath.replace('\\', '/');
            }
            graphPath = graphPath.replace('\\', '/');
            
            System.out.println("创建带有知识图谱路径的课时节点: 课程ID=" + courseId 
                + ", 顺序=" + nodeOrder 
                + ", 标题=" + title 
                + ", RAG路径=" + ragPath
                + ", 图谱路径=" + graphPath);
            
            // 创建新的课时节点
            LessonNode lessonNode = new LessonNode();
            
            // 设置课程
            com.XuebaoMaster.backend.Course.Course course = new com.XuebaoMaster.backend.Course.Course();
            course.setCourseId(courseId);
            lessonNode.setCourse(course);
            
            // 设置其他属性
            lessonNode.setNodeOrder(nodeOrder);
            lessonNode.setTitle(title);
            lessonNode.setPathToNodes(ragPath);
            lessonNode.setPathToGraph(graphPath);
            
            // 保存节点
            LessonNode createdNode = lessonNodeService.createLessonNode(lessonNode);
            System.out.println("创建的课时节点: ID=" + createdNode.getId() 
                + ", 标题=" + createdNode.getTitle() 
                + ", pathToNodes=" + createdNode.getPathToNodes()
                + ", pathToGraph=" + createdNode.getPathToGraph());
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "课时节点创建成功");
            response.put("data", createdNode);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("创建课时节点出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取课时节点的知识图谱路径
     * 
     * @param id 课时节点ID
     * @return 知识图谱路径信息
     */
    @GetMapping("/{id}/graph-path")
    public ResponseEntity<Map<String, Object>> getGraphPath(@PathVariable Long id) {
        try {
            // 获取课时节点
            LessonNode lessonNode = lessonNodeService.getLessonNodeById(id);
            System.out.println("获取课时节点的知识图谱路径: ID=" + id);
            
            // 获取知识图谱路径
            String graphPath = lessonNode.getPathToGraph();
            System.out.println("课时节点的知识图谱路径: " + graphPath);
            
            // 如果路径为空，返回错误
            if (graphPath == null || graphPath.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "该课时节点没有关联的知识图谱数据");
                return ResponseEntity.ok(errorResponse);
            }
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("lessonNodeId", lessonNode.getId());
            response.put("title", lessonNode.getTitle());
            response.put("graphPath", graphPath);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("获取知识图谱路径出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 获取课程下所有课时节点的知识图谱路径
     * 
     * @param courseId 课程ID
     * @return 所有知识图谱路径信息
     */
    @GetMapping("/course/{courseId}/graph-paths")
    public ResponseEntity<Map<String, Object>> getGraphPathsByCourse(@PathVariable Long courseId) {
        try {
            // 获取课程下的所有课时节点
            List<LessonNode> lessonNodes = lessonNodeService.getLessonNodesByCourseIdOrdered(courseId);
            System.out.println("获取课程的所有知识图谱路径: 课程ID=" + courseId);
            
            // 构建知识图谱路径列表
            List<Map<String, Object>> graphPaths = new ArrayList<>();
            for (LessonNode node : lessonNodes) {
                String graphPath = node.getPathToGraph();
                if (graphPath != null && !graphPath.isEmpty()) {
                    Map<String, Object> nodeInfo = new HashMap<>();
                    nodeInfo.put("lessonNodeId", node.getId());
                    nodeInfo.put("title", node.getTitle());
                    nodeInfo.put("nodeOrder", node.getNodeOrder());
                    nodeInfo.put("graphPath", graphPath);
                    graphPaths.add(nodeInfo);
                }
            }
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("courseId", courseId);
            response.put("graphPaths", graphPaths);
            response.put("count", graphPaths.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("获取课程知识图谱路径出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取output文件夹下的所有文件路径
     * 
     * @param basePath 基础路径（可选，默认为特定路径）
     * @return 文件路径列表
     */
    @GetMapping("/output-files")
    public ResponseEntity<Map<String, Object>> getOutputFiles(
            @RequestParam(required = false) String basePath,
            HttpServletRequest request) {
        try {
            // 如果未提供基础路径，使用默认路径
            if (basePath == null || basePath.isEmpty()) {
                basePath = "D:/大三下/综合实习/Summer_intern/work_flow/kg_rag_service/output";
            } else {
                // 处理路径中的反斜杠问题
                basePath = basePath.replace('\\', '/');
            }
            
            System.out.println("获取output文件夹下的所有文件路径: " + basePath);
            
            // 获取文件路径列表
            List<String> filePaths = lessonNodeService.getFilesInFolder(basePath);
            
            // 将所有路径中的反斜杠转换为正斜杠，以便前端处理
            List<String> formattedPaths = filePaths.stream()
                .map(path -> path.replace('\\', '/'))
                .collect(Collectors.toList());
            
            // 获取服务器基础URL
            String baseUrl = getBaseUrl(request);
            
            // 构建文件访问URL列表
            List<Map<String, String>> fileInfoList = new ArrayList<>();
            for (String path : formattedPaths) {
                Map<String, String> fileInfo = new HashMap<>();
                fileInfo.put("path", path);
                fileInfo.put("name", new File(path).getName());
                // 对文件路径进行URL编码
                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
                fileInfo.put("url", baseUrl + "/lesson-nodes/file-content?filePath=" + encodedPath);
                fileInfoList.add(fileInfo);
            }
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("basePath", basePath);
            response.put("files", fileInfoList);
            response.put("count", fileInfoList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("获取output文件列表出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 获取指定文件夹下的所有文件路径
     * 
     * @param folderPath 文件夹路径
     * @return 文件路径列表
     */
    @GetMapping("/folder-files")
    public ResponseEntity<Map<String, Object>> getFolderFiles(
            @RequestParam String folderPath,
            HttpServletRequest request) {
        try {
            // 处理路径中的反斜杠问题
            folderPath = folderPath.replace('\\', '/');
            System.out.println("获取指定文件夹下的所有文件路径: " + folderPath);
            
            // 获取文件路径列表
            List<String> filePaths = lessonNodeService.getFilesInFolder(folderPath);
            
            // 将所有路径中的反斜杠转换为正斜杠，以便前端处理
            List<String> formattedPaths = filePaths.stream()
                .map(path -> path.replace('\\', '/'))
                .collect(Collectors.toList());
            
            // 获取服务器基础URL
            String baseUrl = getBaseUrl(request);
            
            // 构建文件访问URL列表
            List<Map<String, String>> fileInfoList = new ArrayList<>();
            for (String path : formattedPaths) {
                Map<String, String> fileInfo = new HashMap<>();
                fileInfo.put("path", path);
                fileInfo.put("name", new File(path).getName());
                // 对文件路径进行URL编码
                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
                fileInfo.put("url", baseUrl + "/lesson-nodes/file-content?filePath=" + encodedPath);
                fileInfoList.add(fileInfo);
            }
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("folderPath", folderPath);
            response.put("files", fileInfoList);
            response.put("count", fileInfoList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("获取文件夹文件列表出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 获取服务器基础URL
     * 
     * @param request HTTP请求
     * @return 基础URL
     */
    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        
        // 构建基础URL
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        
        // 如果端口不是默认端口，则添加端口号
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        
        url.append(contextPath);
        return url.toString();
    }

    /**
     * 获取指定文件的内容
     * 
     * @param filePath 文件路径
     * @return 文件内容
     */
    @GetMapping("/file-content")
    public ResponseEntity<?> getFileContent(@RequestParam String filePath) {
        try {
            // 处理路径中的反斜杠问题
            filePath = filePath.replace('\\', '/');
            System.out.println("获取文件内容: " + filePath);
            
            // 创建文件对象
            File file = new File(filePath);
            
            // 检查文件是否存在
            if (!file.exists() || !file.isFile()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "文件不存在");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 获取文件名
            String fileName = file.getName();
            
            // 确定文件的媒体类型
            String contentType = determineContentType(fileName);
            
            // 读取文件内容
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            // 设置HTTP头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setPragma("public");
            
            // 返回文件内容
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
        } catch (Exception e) {
            System.out.println("获取文件内容出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 根据文件名确定内容类型
     * 
     * @param fileName 文件名
     * @return 内容类型
     */
    private String determineContentType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
        }
        
        switch (extension) {
            case "txt":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "pdf":
                return "application/pdf";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "svg":
                return "image/svg+xml";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "mp4":
                return "video/mp4";
            case "mp3":
                return "audio/mpeg";
            case "zip":
                return "application/zip";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default:
                return "application/octet-stream";
        }
    }
} 