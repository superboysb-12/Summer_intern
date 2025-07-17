package com.XuebaoMaster.backend.QuestionGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/question-generator")
public class QuestionGeneratorController {

    @Autowired
    private QuestionGeneratorService questionGeneratorService;

    /**
     * 生成题目
     * 
     * @param request 包含query和可选的questionType的请求体
     * @return 返回任务ID
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateQuestion(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        String questionType = request.get("questionType");

        if (query == null || query.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检索词不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Long taskId = questionGeneratorService.generateQuestion(query, questionType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "题目生成任务已提交");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "题目生成任务提交失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 使用指定的RAG ID生成题目
     * 
     * @param request 包含query、ragId和可选的questionType的请求体
     * @return 返回任务ID
     */
    @PostMapping("/generate-with-rag")
    public ResponseEntity<Map<String, Object>> generateQuestionWithRag(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        String questionType = (String) request.get("questionType");
        Long ragId = null;

        // 解析ragId
        if (request.containsKey("ragId")) {
            if (request.get("ragId") instanceof Integer) {
                ragId = ((Integer) request.get("ragId")).longValue();
            } else if (request.get("ragId") instanceof Long) {
                ragId = (Long) request.get("ragId");
            } else if (request.get("ragId") instanceof String) {
                try {
                    ragId = Long.parseLong((String) request.get("ragId"));
                } catch (NumberFormatException e) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "无效的RAG ID格式");
                    return ResponseEntity.badRequest().body(response);
                }
            }
        }

        if (query == null || query.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检索词不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        if (ragId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "RAG ID不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Long taskId = questionGeneratorService.generateQuestionWithRagId(query, ragId, questionType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "题目生成任务已提交");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "题目生成任务提交失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 使用指定的RAG名称生成题目
     * 
     * @param request 包含query、ragName和可选的questionType的请求体
     * @return 返回任务ID
     */
    @PostMapping("/generate-with-rag-name")
    public ResponseEntity<Map<String, Object>> generateQuestionWithRagName(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        String ragName = request.get("ragName");
        String questionType = request.get("questionType");

        if (query == null || query.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检索词不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        if (ragName == null || ragName.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "RAG名称不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Long taskId = questionGeneratorService.generateQuestionWithRagName(query, ragName, questionType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "题目生成任务已提交");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "题目生成任务提交失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取题目生成状态
     * 
     * @param id 题目生成任务ID
     * @return 返回任务状态
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getQuestionStatus(@PathVariable Long id) {
        try {
            Optional<QuestionGenerator> entity = questionGeneratorService.getQuestionGeneratorById(id);

            if (entity.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "找不到指定的题目生成任务");
                return ResponseEntity.notFound().build();
            }

            QuestionGenerator questionGenerator = entity.get();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", questionGenerator.getId());
            response.put("query", questionGenerator.getQuery());
            response.put("questionType", questionGenerator.getQuestionType());
            response.put("status", questionGenerator.getStatus());
            response.put("statusMessage", questionGenerator.getStatusMessage());
            response.put("ragId", questionGenerator.getRagId());
            response.put("ragName", questionGenerator.getRagName());

            if ("COMPLETED".equals(questionGenerator.getStatus())) {
                response.put("questionJson", questionGenerator.getQuestionJson());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取题目状态失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取所有题目生成任务
     * 
     * @return 返回所有题目生成任务列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllQuestions() {
        try {
            List<QuestionGenerator> questions = questionGeneratorService.getAllQuestionGenerators();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("questions", questions);
            response.put("total", questions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取题目列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取可用的题目类型
     * 
     * @return 返回题目类型列表
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getQuestionTypes() {
        try {
            String[] types = questionGeneratorService.getAvailableQuestionTypes();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("types", types);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取题目类型列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新题目JSON内容（修改题目）
     * 
     * @param id      题目生成任务ID
     * @param request 包含更新后的questionJson的请求体
     * @return 返回更新后的题目信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateQuestion(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String questionJson = request.get("questionJson");

        if (questionJson == null || questionJson.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "题目JSON内容不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            QuestionGenerator updatedEntity = questionGeneratorService.updateQuestionJson(id, questionJson);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", updatedEntity.getId());
            response.put("query", updatedEntity.getQuery());
            response.put("questionType", updatedEntity.getQuestionType());
            response.put("status", updatedEntity.getStatus());
            response.put("questionJson", updatedEntity.getQuestionJson());
            response.put("message", "题目更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新题目失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新题目属性
     * 
     * @param id      题目生成任务ID
     * @param request 包含更新内容的请求体
     * @return 返回更新后的题目信息
     */
    @PutMapping("/{id}/properties")
    public ResponseEntity<Map<String, Object>> updateQuestionProperties(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String query = request.get("query");
        String questionType = request.get("questionType");
        String questionJson = request.get("questionJson");

        try {
            QuestionGenerator updatedEntity = questionGeneratorService.updateQuestionProperties(id, query, questionType,
                    questionJson);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", updatedEntity.getId());
            response.put("query", updatedEntity.getQuery());
            response.put("questionType", updatedEntity.getQuestionType());
            response.put("status", updatedEntity.getStatus());
            response.put("questionJson", updatedEntity.getQuestionJson());
            response.put("message", "题目属性更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新题目属性失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 生成题目解答
     * 
     * @param id 题目生成任务ID
     * @return 返回题目解答
     */
    @GetMapping("/{id}/solution")
    public ResponseEntity<Map<String, Object>> generateSolution(@PathVariable Long id) {
        try {
            Map<String, Object> result = questionGeneratorService.generateSolution(id);

            // 如果结果包含success字段且为false，表示生成失败
            if (result.containsKey("success") && !((Boolean) result.get("success"))) {
                return ResponseEntity.badRequest().body(result);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "生成题目解答失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 批量删除题目
     * 
     * @param ids 题目ID列表
     * @return 返回删除结果
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> deleteQuestions(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");

        if (ids == null || ids.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "未提供要删除的题目ID");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            List<Long> deletedIds = questionGeneratorService.deleteQuestions(ids);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedIds", deletedIds);
            response.put("deletedCount", deletedIds.size());
            response.put("message", String.format("成功删除%d个题目", deletedIds.size()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除题目失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 开始在线设计课后练习
     *
     * @param id 题目生成任务ID
     * @return 包含题目内容的响应
     */
    @GetMapping("/{id}/design")
    public ResponseEntity<Map<String, Object>> startDesignQuestion(@PathVariable Long id) {
        try {
            Map<String, Object> response = questionGeneratorService.startDesign(id);

            if (response.containsKey("success") && (Boolean) response.get("success")) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "开始设计失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 保存设计中的课后练习内容
     *
     * @param id      题目生成任务ID
     * @param request 包含设计内容的请求
     * @return 保存结果
     */
    @PostMapping("/{id}/design/save")
    public ResponseEntity<Map<String, Object>> saveDesignContent(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String designContent = request.get("content");
        if (designContent == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "设计内容不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        Map<String, Object> response = questionGeneratorService.saveDesignContent(id, designContent);

        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 完成课后练习设计并计算效率指数
     *
     * @param id 题目生成任务ID
     * @return 包含效率指数的响应
     */
    @PostMapping("/{id}/design/finish")
    public ResponseEntity<Map<String, Object>> finishDesignQuestion(@PathVariable Long id) {
        Map<String, Object> response = questionGeneratorService.finishDesign(id);

        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取课后练习设计效率统计数据
     *
     * @return 包含效率统计的响应
     */
    @GetMapping("/design/efficiency/statistics")
    public ResponseEntity<Map<String, Object>> getDesignEfficiencyStatistics() {
        Map<String, Object> response = questionGeneratorService.getEfficiencyStatistics();
        return ResponseEntity.ok(response);
    }

    /**
     * 获取课后练习优化建议
     *
     * @param id 题目生成任务ID
     * @return 包含优化建议的响应
     */
    @GetMapping("/{id}/optimization")
    public ResponseEntity<Map<String, Object>> getQuestionOptimizationSuggestions(@PathVariable Long id) {
        Map<String, Object> response = questionGeneratorService.getOptimizationSuggestions(id);

        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}