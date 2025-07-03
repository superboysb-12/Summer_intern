package com.XuebaoMaster.backend.LessonNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/lesson-nodes")
public class LessonNodeController {
    @Autowired
    private LessonNodeService lessonNodeService;

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
     * 根据ID获取课时节点
     * 
     * @param id 课时节点ID
     * @return 课时节点信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<LessonNode> getLessonNodeById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonNodeService.getLessonNodeById(id));
    }

    /**
     * 根据课程ID获取课时节点
     * 
     * @param courseId 课程ID
     * @return 课时节点列表
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<LessonNode>> getLessonNodesByCourseId(@PathVariable Long courseId) {
        return ResponseEntity.ok(lessonNodeService.getLessonNodesByCourseId(courseId));
    }
    
    /**
     * 根据课程ID获取按节点顺序排序的课时节点
     * 
     * @param courseId 课程ID
     * @return 课时节点列表
     */
    @GetMapping("/course/{courseId}/ordered")
    public ResponseEntity<List<LessonNode>> getLessonNodesByCourseIdOrdered(@PathVariable Long courseId) {
        return ResponseEntity.ok(lessonNodeService.getLessonNodesByCourseIdOrdered(courseId));
    }
    
    /**
     * 根据课程ID和节点顺序获取课时节点
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
     * 更新课时节点信息
     * 
     * @param id 课时节点ID
     * @param lessonNode 课时节点信息
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
        return ResponseEntity.ok().build();
    }

    /**
     * 搜索课时节点
     * 
     * @param keyword 关键词
     * @return 课时节点列表
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
            
            // 设置源文件路径 - 使用正斜杠而非反斜杠
            String sourcePath = "D:/大三下/综合实习/Summer_intern/work_flow/kg_rag_service/source_article";
            
            // 调用服务生成RAG
            String ragDir = lessonNodeService.generateRag(sourcePath);
            
            // 更新课时节点的pathToNodes字段
            lessonNode.setPathToNodes(ragDir);
            lessonNodeService.updateLessonNode(lessonNode);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "RAG生成成功并已更新课时节点");
            response.put("data", lessonNode);
            response.put("rag_dir", ragDir);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 生成RAG（带路径参数）
     * 
     * @param id 课时节点ID
     * @param sourcePath 源文件路径
     * @return RAG生成结果
     */
    @GetMapping("/generate-rag-with-path")
    public ResponseEntity<Map<String, Object>> generateRagWithPath(
            @RequestParam Long id, 
            @RequestParam String sourcePath) {
        try {
            // 获取课时节点
            LessonNode lessonNode = lessonNodeService.getLessonNodeById(id);
            
            // 调用服务生成RAG
            String ragDir = lessonNodeService.generateRag(sourcePath);
            
            // 更新课时节点的pathToNodes字段
            lessonNode.setPathToNodes(ragDir);
            lessonNodeService.updateLessonNode(lessonNode);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "RAG生成成功并已更新课时节点");
            response.put("data", lessonNode);
            response.put("rag_dir", ragDir);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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
            
            // 调用服务查询RAG
            String result = lessonNodeService.queryRag(query, ragPath);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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
            
            // 获取节点的RAG路径
            String ragPath = lessonNode.getPathToNodes();
            
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
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 发送聊天消息
     * 
     * @param request 请求体，包含消息内容和附加参数
     * @return 聊天响应
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> sendChatMessage(@RequestBody Map<String, Object> request) {
        try {
            // 从请求体中提取消息内容
            String message = (String) request.get("message");
            if (message == null || message.isEmpty()) {
                throw new IllegalArgumentException("消息内容不能为空");
            }
            
            // 提取附加参数（如果有）
            Map<String, Object> additionalParams = new HashMap<>();
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (!entry.getKey().equals("message")) {
                    additionalParams.put(entry.getKey(), entry.getValue());
                }
            }
            
            // 调用服务发送聊天消息
            String result = lessonNodeService.sendChatMessage(message, additionalParams);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 