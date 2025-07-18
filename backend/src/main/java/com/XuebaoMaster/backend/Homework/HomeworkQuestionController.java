package com.XuebaoMaster.backend.Homework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/homework-questions")
public class HomeworkQuestionController {

    @Autowired
    private HomeworkQuestionService homeworkQuestionService;

    /**
     * 为作业添加问题
     * 
     * @param request 包含作业ID、问题ID、顺序号和分值权重的请求体
     * @return 保存的关联记录
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addQuestionToHomework(@RequestBody Map<String, Object> request) {
        Long homeworkId = getLongValue(request.get("homeworkId"));
        Long questionId = getLongValue(request.get("questionId"));
        Integer questionOrder = getIntegerValue(request.get("questionOrder"));
        Integer scoreWeight = getIntegerValue(request.get("scoreWeight"));

        if (homeworkId == null || questionId == null || questionOrder == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "作业ID、问题ID和顺序号不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            HomeworkQuestion homeworkQuestion = homeworkQuestionService.addQuestionToHomework(
                    homeworkId, questionId, questionOrder, scoreWeight);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "问题添加成功");
            response.put("homeworkQuestion", homeworkQuestion);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "问题添加失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 批量为作业添加问题
     * 
     * @param homeworkId 作业ID
     * @param request    包含问题列表的请求体
     * @return 保存的关联记录列表
     */
    @PostMapping("/{homeworkId}/batch-add")
    public ResponseEntity<Map<String, Object>> addQuestionsToHomework(
            @PathVariable Long homeworkId,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) request.get("questions");

        if (questions == null || questions.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "问题列表不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            List<HomeworkQuestion> homeworkQuestions = homeworkQuestionService.addQuestionsToHomework(homeworkId,
                    questions);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量添加问题成功");
            response.put("homeworkQuestions", homeworkQuestions);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量添加问题失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 从作业中移除问题
     * 
     * @param homeworkId 作业ID
     * @param questionId 问题ID
     * @return 操作结果
     */
    @DeleteMapping("/{homeworkId}/questions/{questionId}")
    public ResponseEntity<Map<String, Object>> removeQuestionFromHomework(
            @PathVariable Long homeworkId,
            @PathVariable Long questionId) {
        try {
            boolean success = homeworkQuestionService.removeQuestionFromHomework(homeworkId, questionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "问题移除成功" : "问题移除失败");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "问题移除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新作业问题的顺序号或分值权重
     * 
     * @param id      关联记录ID
     * @param request 包含新的顺序号和分值权重的请求体
     * @return 更新后的关联记录
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateHomeworkQuestion(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Integer questionOrder = getIntegerValue(request.get("questionOrder"));
        Integer scoreWeight = getIntegerValue(request.get("scoreWeight"));

        try {
            HomeworkQuestion homeworkQuestion = homeworkQuestionService.updateHomeworkQuestion(id, questionOrder,
                    scoreWeight);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "更新成功");
            response.put("homeworkQuestion", homeworkQuestion);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取作业中的所有问题
     * 
     * @param homeworkId 作业ID
     * @return 问题列表
     */
    @GetMapping("/homework/{homeworkId}")
    public ResponseEntity<Map<String, Object>> getHomeworkQuestions(@PathVariable Long homeworkId) {
        try {
            List<HomeworkQuestion> homeworkQuestions = homeworkQuestionService.getHomeworkQuestions(homeworkId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("homeworkQuestions", homeworkQuestions);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取作业问题失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取包含特定问题的所有作业
     * 
     * @param questionId 问题ID
     * @return 作业列表
     */
    @GetMapping("/question/{questionId}")
    public ResponseEntity<Map<String, Object>> getHomeworksByQuestion(@PathVariable Long questionId) {
        try {
            List<HomeworkQuestion> homeworkQuestions = homeworkQuestionService.getHomeworksByQuestionId(questionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("homeworkQuestions", homeworkQuestions);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取问题关联作业失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 清空作业中的所有问题
     * 
     * @param homeworkId 作业ID
     * @return 操作结果
     */
    @DeleteMapping("/{homeworkId}/clear")
    public ResponseEntity<Map<String, Object>> clearHomeworkQuestions(@PathVariable Long homeworkId) {
        try {
            boolean success = homeworkQuestionService.clearHomeworkQuestions(homeworkId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "清空作业问题成功" : "清空作业问题失败");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "清空作业问题失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 辅助方法：将Object转换为Long
    private Long getLongValue(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        } else if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // 辅助方法：将Object转换为Integer
    private Integer getIntegerValue(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Long) {
            return ((Long) obj).intValue();
        } else if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}