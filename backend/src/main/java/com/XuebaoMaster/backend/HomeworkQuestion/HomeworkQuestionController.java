package com.XuebaoMaster.backend.HomeworkQuestion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 作业题目关联控制器
 * 处理作业与题目的关联管理
 */
@RestController
@RequestMapping("/homework-questions")
public class HomeworkQuestionController {

    @Autowired
    private HomeworkQuestionService homeworkQuestionService;

    /**
     * 获取作业中的所有题目（按顺序）
     */
    @GetMapping("/homework/{homeworkId}")
    public ResponseEntity<List<HomeworkQuestion>> getQuestionsByHomeworkId(@PathVariable Long homeworkId) {
        return ResponseEntity.ok(homeworkQuestionService.getQuestionsByHomeworkId(homeworkId));
    }

    /**
     * 创建作业题目关联
     */
    @PostMapping
    public ResponseEntity<HomeworkQuestion> createHomeworkQuestion(@RequestBody HomeworkQuestion homeworkQuestion) {
        return ResponseEntity.ok(homeworkQuestionService.createHomeworkQuestion(homeworkQuestion));
    }

    /**
     * 批量添加题目到作业
     */
    @PostMapping("/homework/{homeworkId}/batch")
    public ResponseEntity<List<HomeworkQuestion>> addQuestionsToHomework(
            @PathVariable Long homeworkId,
            @RequestBody Map<String, List<Long>> requestBody) {
        List<Long> questionIds = requestBody.get("questionIds");
        if (questionIds == null || questionIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(homeworkQuestionService.addQuestionsToHomework(homeworkId, questionIds));
    }

    /**
     * 更新题目顺序
     */
    @PutMapping("/{id}/order")
    public ResponseEntity<HomeworkQuestion> updateQuestionOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> requestBody) {
        Integer newOrderIndex = requestBody.get("orderIndex");
        if (newOrderIndex == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(homeworkQuestionService.updateQuestionOrder(id, newOrderIndex));
    }

    /**
     * 调整作业中多个题目的顺序
     */
    @PutMapping("/homework/{homeworkId}/reorder")
    public ResponseEntity<List<HomeworkQuestion>> reorderHomeworkQuestions(
            @PathVariable Long homeworkId,
            @RequestBody Map<String, List<Long>> requestBody) {
        List<Long> questionIdsInOrder = requestBody.get("questionIds");
        if (questionIdsInOrder == null || questionIdsInOrder.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(homeworkQuestionService.reorderHomeworkQuestions(homeworkId, questionIdsInOrder));
    }

    /**
     * 从作业中删除题目
     */
    @DeleteMapping("/homework/{homeworkId}/question/{questionId}")
    public ResponseEntity<Void> removeQuestionFromHomework(
            @PathVariable Long homeworkId,
            @PathVariable Long questionId) {
        homeworkQuestionService.removeQuestionFromHomework(homeworkId, questionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除作业中的所有题目
     */
    @DeleteMapping("/homework/{homeworkId}/questions")
    public ResponseEntity<Void> removeAllQuestionsFromHomework(@PathVariable Long homeworkId) {
        homeworkQuestionService.removeAllQuestionsFromHomework(homeworkId);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取作业中的题目数量
     */
    @GetMapping("/homework/{homeworkId}/count")
    public ResponseEntity<Long> getQuestionCountByHomeworkId(@PathVariable Long homeworkId) {
        return ResponseEntity.ok(homeworkQuestionService.getQuestionCountByHomeworkId(homeworkId));
    }

    /**
     * 检查题目是否存在于作业中
     */
    @GetMapping("/homework/{homeworkId}/contains/{questionId}")
    public ResponseEntity<Boolean> isQuestionInHomework(
            @PathVariable Long homeworkId,
            @PathVariable Long questionId) {
        return ResponseEntity.ok(homeworkQuestionService.isQuestionInHomework(homeworkId, questionId));
    }
}