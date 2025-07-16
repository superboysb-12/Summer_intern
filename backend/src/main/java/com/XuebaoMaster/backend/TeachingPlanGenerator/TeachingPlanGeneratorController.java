package com.XuebaoMaster.backend.TeachingPlanGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teaching-plan-generator")
public class TeachingPlanGeneratorController {

    @Autowired
    private TeachingPlanGeneratorService teachingPlanGeneratorService;

    /**
     * 生成教案
     *
     * @param request 包含提示词和可选的RAG配置的请求
     * @return 返回任务ID
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTeachingPlan(@RequestBody Map<String, Object> request) {
        String prompt = (String) request.get("query");
        if (prompt == null || prompt.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "提示词不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        // 提取RAG相关配置
        Map<String, Object> inputs = new HashMap<>();
        if (request.containsKey("inputs") && request.get("inputs") instanceof Map) {
            inputs = (Map<String, Object>) request.get("inputs");
        }

        Long taskId = teachingPlanGeneratorService.generateTeachingPlan(prompt, inputs);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("taskId", taskId);
        response.put("message", "教案生成任务已提交");
        return ResponseEntity.ok(response);
    }

    /**
     * 通过RAG ID生成教案
     *
     * @param request 包含提示词和RAG ID的请求
     * @return 返回任务ID
     */
    @PostMapping("/generate-with-rag")
    public ResponseEntity<Map<String, Object>> generateTeachingPlanWithRag(@RequestBody Map<String, Object> request) {
        String prompt = (String) request.get("query");
        if (prompt == null || prompt.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "提示词不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        // 获取RAG ID
        Long ragId = null;
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

        if (ragId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "RAG ID不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        // 提取其他可能的配置
        Map<String, Object> inputs = new HashMap<>();
        if (request.containsKey("inputs") && request.get("inputs") instanceof Map) {
            inputs = (Map<String, Object>) request.get("inputs");
        }

        Long taskId = teachingPlanGeneratorService.generateTeachingPlanWithRagId(prompt, ragId, inputs);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("taskId", taskId);
        response.put("message", "教案生成任务已提交");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取教案生成状态
     *
     * @param id 任务ID
     * @return 返回任务状态
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTeachingPlanStatus(@PathVariable Long id) {
        TeachingPlanGenerator entity = teachingPlanGeneratorService.getTeachingPlanGeneratorById(id);
        Map<String, Object> response = new HashMap<>();

        if (entity == null) {
            response.put("success", false);
            response.put("message", "找不到指定的教案生成任务");
            return ResponseEntity.notFound().build();
        }

        response.put("success", true);
        response.put("id", entity.getId());
        response.put("prompt", entity.getPrompt());
        response.put("status", entity.getStatus());

        if ("COMPLETED".equals(entity.getStatus())) {
            response.put("fileName", entity.getFileName());
            response.put("filePath", entity.getFilePath());
            response.put("conversationId", entity.getConversationId());
            response.put("messageId", entity.getMessageId());
        } else if ("FAILED".equals(entity.getStatus())) {
            response.put("error", entity.getMessageId());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 下载教案文件
     *
     * @param id 教案生成任务ID
     * @return 返回文件资源
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadTeachingPlan(@PathVariable Long id, HttpServletRequest request) {
        TeachingPlanGenerator entity = teachingPlanGeneratorService.getTeachingPlanGeneratorById(id);

        if (entity == null || !"COMPLETED".equals(entity.getStatus()) ||
                entity.getFilePath() == null || entity.getFileName() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            // 构造文件的完整路径
            Path filePath = Paths.get(entity.getFilePath());
            Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());

            if (resource.exists()) {
                // 确定内容类型
                String contentType = null;
                try {
                    contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
                } catch (IOException ex) {
                    // 日志记录错误
                }

                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + entity.getFileName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取所有教案生成任务
     *
     * @return 返回所有教案生成任务列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllTeachingPlans() {
        List<TeachingPlanGenerator> teachingPlans = teachingPlanGeneratorService.getAllTeachingPlanGenerators();

        List<Map<String, Object>> plansList = teachingPlans.stream().map(entity -> {
            Map<String, Object> plan = new HashMap<>();
            plan.put("id", entity.getId());
            plan.put("prompt", entity.getPrompt());
            plan.put("status", entity.getStatus());
            plan.put("createdAt", entity.getCreatedAt());
            plan.put("updatedAt", entity.getUpdatedAt());

            if ("COMPLETED".equals(entity.getStatus())) {
                plan.put("fileName", entity.getFileName());
                plan.put("filePath", entity.getFilePath());
                plan.put("conversationId", entity.getConversationId());
                plan.put("messageId", entity.getMessageId());

                // 添加编辑和效率相关字段
                if (entity.getEditDuration() != null && entity.getEditDuration() > 0) {
                    plan.put("editDuration", entity.getEditDuration());
                    plan.put("editStartTime", entity.getEditStartTime());
                    plan.put("editEndTime", entity.getEditEndTime());
                    plan.put("efficiencyIndex", entity.getEfficiencyIndex());
                    plan.put("hasBeenEdited", true);
                } else {
                    plan.put("hasBeenEdited", false);
                }
            }

            return plan;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("teachingPlans", plansList);
        response.put("count", plansList.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 开始在线编辑教案
     *
     * @param id 教案生成任务ID
     * @return 包含教案内容的响应
     */
    @GetMapping("/{id}/edit")
    public ResponseEntity<Map<String, Object>> startEditTeachingPlan(@PathVariable Long id) {
        try {
            Map<String, Object> response = teachingPlanGeneratorService.startOnlineEdit(id);

            if (response.containsKey("success") && (Boolean) response.get("success")) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "开始编辑失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 保存编辑中的教案内容
     *
     * @param id      教案生成任务ID
     * @param request 包含编辑内容的请求
     * @return 保存结果
     */
    @PostMapping("/{id}/edit/save")
    public ResponseEntity<Map<String, Object>> saveEditedContent(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String editContent = request.get("content");
        if (editContent == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "编辑内容不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        Map<String, Object> response = teachingPlanGeneratorService.saveOnlineEdit(id, editContent);

        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 完成教案编辑并计算效率指数
     *
     * @param id 教案生成任务ID
     * @return 包含效率指数的响应
     */
    @PostMapping("/{id}/edit/finish")
    public ResponseEntity<Map<String, Object>> finishEditTeachingPlan(@PathVariable Long id) {
        Map<String, Object> response = teachingPlanGeneratorService.finishOnlineEdit(id);

        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取教学效率统计数据
     *
     * @return 包含效率统计的响应
     */
    @GetMapping("/efficiency/statistics")
    public ResponseEntity<Map<String, Object>> getEfficiencyStatistics() {
        Map<String, Object> response = teachingPlanGeneratorService.getEfficiencyStatistics();
        return ResponseEntity.ok(response);
    }

    /**
     * 获取课程优化建议
     *
     * @param id 教案生成任务ID
     * @return 包含优化建议的响应
     */
    @GetMapping("/{id}/optimization")
    public ResponseEntity<Map<String, Object>> getCourseOptimizationSuggestions(@PathVariable Long id) {
        Map<String, Object> response = teachingPlanGeneratorService.getCourseOptimizationSuggestions(id);

        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 测试文件路径是否可访问（仅用于调试）
     *
     * @param id 教案生成任务ID
     * @return 包含文件信息的响应
     */
    @GetMapping("/{id}/test-file")
    public ResponseEntity<Map<String, Object>> testFilePath(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            TeachingPlanGenerator entity = teachingPlanGeneratorService.getTeachingPlanGeneratorById(id);
            if (entity == null) {
                response.put("success", false);
                response.put("message", "找不到指定的教案生成任务");
                return ResponseEntity.ok(response);
            }

            String filePath = entity.getFilePath();
            if (filePath == null) {
                response.put("success", false);
                response.put("message", "文件路径为空");
                return ResponseEntity.ok(response);
            }

            Path path = Paths.get(filePath);
            boolean exists = Files.exists(path);
            boolean isReadable = Files.isReadable(path);
            boolean isFile = Files.isRegularFile(path);

            response.put("success", true);
            response.put("filePath", filePath);
            response.put("exists", exists);
            response.put("isReadable", isReadable);
            response.put("isFile", isFile);
            if (exists) {
                response.put("size", Files.size(path));
                response.put("lastModified", Files.getLastModifiedTime(path).toString());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "检查文件失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 检查教案生成任务详情（调试用）
     *
     * @param id 教案生成任务ID
     * @return 返回教案生成任务详情
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<Map<String, Object>> getTeachingPlanDetails(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        TeachingPlanGenerator entity = teachingPlanGeneratorService.getTeachingPlanGeneratorById(id);
        if (entity == null) {
            response.put("success", false);
            response.put("message", "找不到指定的教案生成任务");
            return ResponseEntity.ok(response);
        }

        response.put("success", true);
        response.put("id", entity.getId());
        response.put("prompt", entity.getPrompt());
        response.put("status", entity.getStatus());
        response.put("fileName", entity.getFileName());
        response.put("filePath", entity.getFilePath());
        response.put("editStartTime", entity.getEditStartTime());
        response.put("editEndTime", entity.getEditEndTime());
        response.put("editDuration", entity.getEditDuration());

        // 检查editContent字段
        if (entity.getEditContent() != null) {
            response.put("editContentLength", entity.getEditContent().length());
            response.put("editContentSample",
                    entity.getEditContent().length() > 100 ? entity.getEditContent().substring(0, 100) + "..."
                            : entity.getEditContent());
        } else {
            response.put("editContentLength", 0);
            response.put("editContentSample", null);
        }

        response.put("efficiencyIndex", entity.getEfficiencyIndex());
        response.put("optimizationSuggestions", entity.getOptimizationSuggestions());
        response.put("createdAt", entity.getCreatedAt());
        response.put("updatedAt", entity.getUpdatedAt());

        return ResponseEntity.ok(response);
    }
}