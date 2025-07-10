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
            }

            return plan;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("teachingPlans", plansList);
        response.put("count", plansList.size());

        return ResponseEntity.ok(response);
    }
}