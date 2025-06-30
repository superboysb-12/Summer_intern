package com.XuebaoMaster.backend.PracticeRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/practice-records")
public class PracticeRecordController {
    @Autowired
    private PracticeRecordService practiceRecordService;

    /**
     * 创建练习记录
     * 
     * @param practiceRecord 练习记录信息
     * @return 创建的练习记录
     */
    @PostMapping
    public ResponseEntity<PracticeRecord> createPracticeRecord(@RequestBody PracticeRecord practiceRecord) {
        // 如果没有设置完成时间，则设置为当前时间
        if (practiceRecord.getFinishTime() == null) {
            practiceRecord.setFinishTime(LocalDateTime.now());
        }
        return ResponseEntity.ok(practiceRecordService.createPracticeRecord(practiceRecord));
    }

    /**
     * 获取所有练习记录
     * 
     * @return 练习记录列表
     */
    @GetMapping
    public ResponseEntity<List<PracticeRecord>> getAllPracticeRecords() {
        return ResponseEntity.ok(practiceRecordService.getAllPracticeRecords());
    }

    /**
     * 根据ID获取练习记录
     * 
     * @param id 练习记录ID
     * @return 练习记录信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<PracticeRecord> getPracticeRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(practiceRecordService.getPracticeRecordById(id));
    }

    /**
     * 根据学生ID获取练习记录
     * 
     * @param studentId 学生ID
     * @return 练习记录列表
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<PracticeRecord>> getPracticeRecordsByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(practiceRecordService.getPracticeRecordsByStudentId(studentId));
    }
    
    /**
     * 根据问题ID获取练习记录
     * 
     * @param questionId 问题ID
     * @return 练习记录列表
     */
    @GetMapping("/question/{questionId}")
    public ResponseEntity<List<PracticeRecord>> getPracticeRecordsByQuestionId(@PathVariable Long questionId) {
        return ResponseEntity.ok(practiceRecordService.getPracticeRecordsByQuestionId(questionId));
    }
    
    /**
     * 根据学生ID和问题ID获取练习记录
     * 
     * @param studentId 学生ID
     * @param questionId 问题ID
     * @return 练习记录列表
     */
    @GetMapping("/student/{studentId}/question/{questionId}")
    public ResponseEntity<List<PracticeRecord>> getPracticeRecordsByStudentIdAndQuestionId(
            @PathVariable Long studentId, @PathVariable Long questionId) {
        return ResponseEntity.ok(practiceRecordService.getPracticeRecordsByStudentIdAndQuestionId(studentId, questionId));
    }
    
    /**
     * 根据正确性获取练习记录
     * 
     * @param isRight 是否正确
     * @return 练习记录列表
     */
    @GetMapping("/right/{isRight}")
    public ResponseEntity<List<PracticeRecord>> getPracticeRecordsByIsRight(@PathVariable Boolean isRight) {
        return ResponseEntity.ok(practiceRecordService.getPracticeRecordsByIsRight(isRight));
    }
    
    /**
     * 根据学生ID和正确性获取练习记录
     * 
     * @param studentId 学生ID
     * @param isRight 是否正确
     * @return 练习记录列表
     */
    @GetMapping("/student/{studentId}/right/{isRight}")
    public ResponseEntity<List<PracticeRecord>> getPracticeRecordsByStudentIdAndIsRight(
            @PathVariable Long studentId, @PathVariable Boolean isRight) {
        return ResponseEntity.ok(practiceRecordService.getPracticeRecordsByStudentIdAndIsRight(studentId, isRight));
    }
    
    /**
     * 根据问题ID和正确性获取练习记录
     * 
     * @param questionId 问题ID
     * @param isRight 是否正确
     * @return 练习记录列表
     */
    @GetMapping("/question/{questionId}/right/{isRight}")
    public ResponseEntity<List<PracticeRecord>> getPracticeRecordsByQuestionIdAndIsRight(
            @PathVariable Long questionId, @PathVariable Boolean isRight) {
        return ResponseEntity.ok(practiceRecordService.getPracticeRecordsByQuestionIdAndIsRight(questionId, isRight));
    }

    /**
     * 更新练习记录信息
     * 
     * @param id 练习记录ID
     * @param practiceRecord 练习记录信息
     * @return 更新后的练习记录
     */
    @PutMapping("/{id}")
    public ResponseEntity<PracticeRecord> updatePracticeRecord(@PathVariable Long id, @RequestBody PracticeRecord practiceRecord) {
        practiceRecord.setId(id);
        return ResponseEntity.ok(practiceRecordService.updatePracticeRecord(practiceRecord));
    }

    /**
     * 删除练习记录
     * 
     * @param id 练习记录ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePracticeRecord(@PathVariable Long id) {
        practiceRecordService.deletePracticeRecord(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取学生统计信息
     * 
     * @param studentId 学生ID
     * @return 统计信息
     */
    @GetMapping("/student/{studentId}/statistics")
    public ResponseEntity<Map<String, Long>> getStudentStatistics(@PathVariable Long studentId) {
        return ResponseEntity.ok(practiceRecordService.getStudentStatistics(studentId));
    }
} 