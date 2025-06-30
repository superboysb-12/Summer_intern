package com.XuebaoMaster.backend.Question;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questions")
public class QuestionController {
    @Autowired
    private QuestionService questionService;

    /**
     * 创建问题
     * 
     * @param question 问题信息
     * @return 创建的问题
     */
    @PostMapping
    public ResponseEntity<Question> createQuestion(@RequestBody Question question) {
        return ResponseEntity.ok(questionService.createQuestion(question));
    }

    /**
     * 获取所有问题
     * 
     * @return 问题列表
     */
    @GetMapping
    public ResponseEntity<List<Question>> getAllQuestions() {
        return ResponseEntity.ok(questionService.getAllQuestions());
    }

    /**
     * 根据ID获取问题
     * 
     * @param id 问题ID
     * @return 问题信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getQuestionById(id));
    }

    /**
     * 根据课时节点ID获取问题
     * 
     * @param lessonNodeId 课时节点ID
     * @return 问题列表
     */
    @GetMapping("/lesson-node/{lessonNodeId}")
    public ResponseEntity<List<Question>> getQuestionsByLessonNodeId(@PathVariable Long lessonNodeId) {
        return ResponseEntity.ok(questionService.getQuestionsByLessonNodeId(lessonNodeId));
    }
    
    /**
     * 根据课时节点ID获取单个问题
     * 
     * @param lessonNodeId 课时节点ID
     * @return 问题信息
     */
    @GetMapping("/lesson-node/{lessonNodeId}/one")
    public ResponseEntity<?> getOneQuestionByLessonNodeId(@PathVariable Long lessonNodeId) {
        return questionService.getOneQuestionByLessonNodeId(lessonNodeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据生成状态获取问题
     * 
     * @param status 生成状态
     * @return 问题列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Question>> getQuestionsByStatus(@PathVariable String status) {
        Question.GenerationStatus generationStatus = Question.GenerationStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(questionService.getQuestionsByStatus(generationStatus));
    }

    /**
     * 更新问题信息
     * 
     * @param id 问题ID
     * @param question 问题信息
     * @return 更新后的问题
     */
    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question question) {
        question.setId(id);
        return ResponseEntity.ok(questionService.updateQuestion(question));
    }
    
    /**
     * 更新问题生成状态
     * 
     * @param id 问题ID
     * @param status 生成状态
     * @return 更新后的问题
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Question> updateGenerationStatus(@PathVariable Long id, @RequestParam String status) {
        Question.GenerationStatus generationStatus = Question.GenerationStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(questionService.updateGenerationStatus(id, generationStatus));
    }

    /**
     * 删除问题
     * 
     * @param id 问题ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok().build();
    }
} 