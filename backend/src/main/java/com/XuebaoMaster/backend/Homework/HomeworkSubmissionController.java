package com.XuebaoMaster.backend.Homework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/homework-submissions")
public class HomeworkSubmissionController {

    @Autowired
    private HomeworkSubmissionService submissionService;

    // 创建作业提交
    @PostMapping
    public ResponseEntity<HomeworkSubmission> createSubmission(@RequestBody HomeworkSubmission submission) {
        // 确保设置提交日期
        submission.setSubmissionDate(LocalDateTime.now());
        return ResponseEntity.ok(submissionService.createSubmission(submission));
    }

    // 获取所有提交（管理员功能）
    @GetMapping
    public ResponseEntity<List<HomeworkSubmission>> getAllSubmissions() {
        return ResponseEntity.ok(submissionService.getAllSubmissions());
    }

    // 根据ID获取提交
    @GetMapping("/{id}")
    public ResponseEntity<HomeworkSubmission> getSubmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getSubmissionById(id));
    }

    // 更新提交
    @PutMapping("/{id}")
    public ResponseEntity<HomeworkSubmission> updateSubmission(@PathVariable Long id,
            @RequestBody HomeworkSubmission submission) {
        submission.setId(id);
        // 确保更新提交日期
        submission.setSubmissionDate(LocalDateTime.now());
        return ResponseEntity.ok(submissionService.updateSubmission(submission));
    }

    // 删除提交
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id) {
        submissionService.deleteSubmission(id);
        return ResponseEntity.ok().build();
    }

    // 根据作业ID获取提交
    @GetMapping("/homework/{homeworkId}")
    public ResponseEntity<List<HomeworkSubmission>> getSubmissionsByHomework(@PathVariable Long homeworkId) {
        return ResponseEntity.ok(submissionService.getSubmissionsByHomework(homeworkId));
    }

    // 根据学生ID获取提交
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<HomeworkSubmission>> getSubmissionsByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(submissionService.getSubmissionsByStudent(studentId));
    }

    // 根据学生ID和作业ID获取提交
    @GetMapping("/student/{studentId}/homework/{homeworkId}")
    public ResponseEntity<HomeworkSubmission> getSubmissionByStudentAndHomework(
            @PathVariable Long studentId,
            @PathVariable Long homeworkId) {
        try {
            return ResponseEntity.ok(submissionService.getSubmissionByStudentAndHomework(studentId, homeworkId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 评分
    @PutMapping("/{id}/grade")
    public ResponseEntity<HomeworkSubmission> gradeSubmission(
            @PathVariable Long id,
            @RequestBody Map<String, Object> gradeData) {
        Integer score = ((Number) gradeData.get("score")).intValue();
        String feedback = (String) gradeData.get("feedback");
        return ResponseEntity.ok(submissionService.gradeSubmission(id, score, feedback));
    }

    // 根据状态获取提交
    @GetMapping("/status/{status}")
    public ResponseEntity<List<HomeworkSubmission>> getSubmissionsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(submissionService.getSubmissionsByStatus(status));
    }

    // 根据作业ID和状态获取提交
    @GetMapping("/homework/{homeworkId}/status/{status}")
    public ResponseEntity<List<HomeworkSubmission>> getSubmissionsByHomeworkAndStatus(
            @PathVariable Long homeworkId,
            @PathVariable String status) {
        return ResponseEntity.ok(submissionService.getSubmissionsByHomeworkAndStatus(homeworkId, status));
    }

    // 根据学生ID和状态获取提交
    @GetMapping("/student/{studentId}/status/{status}")
    public ResponseEntity<List<HomeworkSubmission>> getSubmissionsByStudentAndStatus(
            @PathVariable Long studentId,
            @PathVariable String status) {
        return ResponseEntity.ok(submissionService.getSubmissionsByStudentAndStatus(studentId, status));
    }

    // 更新提交状态
    @PutMapping("/{id}/status")
    public ResponseEntity<HomeworkSubmission> updateSubmissionStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        String status = statusUpdate.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(submissionService.updateSubmissionStatus(id, status));
    }
}