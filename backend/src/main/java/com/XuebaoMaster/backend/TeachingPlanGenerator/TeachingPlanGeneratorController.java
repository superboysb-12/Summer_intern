package com.XuebaoMaster.backend.TeachingPlanGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * @param request 包含提示词的请求
     * @return 返回任务ID
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTeachingPlan(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "提示词不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        Long taskId = teachingPlanGeneratorService.generateTeachingPlan(prompt);
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