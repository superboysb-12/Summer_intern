package com.XuebaoMaster.backend.PracticeRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 练习记录控制器
 * 处理学生答题记录和统计数据
 */
@RestController
@RequestMapping("/practice-records")
public class PracticeRecordController {

    @Autowired
    private PracticeRecordService practiceRecordService;

    /**
     * 创建练习记录
     */
    @PostMapping
    public ResponseEntity<PracticeRecord> createPracticeRecord(@RequestBody PracticeRecord practiceRecord) {
        return ResponseEntity.ok(practiceRecordService.createPracticeRecord(practiceRecord));
    }

    /**
     * 提交练习答案
     */
    @PostMapping("/submit")
    public ResponseEntity<PracticeRecord> submitPracticeAnswer(@RequestBody Map<String, Object> requestBody) {
        Long studentId = Long.valueOf(requestBody.get("studentId").toString());
        Long homeworkId = Long.valueOf(requestBody.get("homeworkId").toString());
        Long questionId = Long.valueOf(requestBody.get("questionId").toString());
        Double score = Double.valueOf(requestBody.get("score").toString());
        String answerData = requestBody.get("answerData") != null ? requestBody.get("answerData").toString() : null;
        Integer timeSpent = requestBody.get("timeSpent") != null
                ? Integer.valueOf(requestBody.get("timeSpent").toString())
                : null;

        return ResponseEntity.ok(practiceRecordService.submitPracticeAnswer(
                studentId, homeworkId, questionId, score, answerData, timeSpent));
    }

    /**
     * 更新练习记录
     */
    @PutMapping("/{id}")
    public ResponseEntity<PracticeRecord> updatePracticeRecord(
            @PathVariable Long id, @RequestBody PracticeRecord practiceRecord) {
        practiceRecord.setId(id);
        return ResponseEntity.ok(practiceRecordService.updatePracticeRecord(practiceRecord));
    }

    /**
     * 获取练习记录详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<PracticeRecord> getPracticeRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(practiceRecordService.getPracticeRecordById(id));
    }

    /**
     * 删除练习记录
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePracticeRecord(@PathVariable Long id) {
        practiceRecordService.deletePracticeRecord(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取学生的所有练习记录
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<PracticeRecord>> getStudentPracticeRecords(@PathVariable Long studentId) {
        return ResponseEntity.ok(practiceRecordService.getStudentPracticeRecords(studentId));
    }

    /**
     * 获取学生在特定作业的练习记录
     */
    @GetMapping("/student/{studentId}/homework/{homeworkId}")
    public ResponseEntity<List<PracticeRecord>> getStudentHomeworkRecords(
            @PathVariable Long studentId, @PathVariable Long homeworkId) {
        return ResponseEntity.ok(practiceRecordService.getStudentHomeworkRecords(studentId, homeworkId));
    }

    /**
     * 获取特定题目的所有练习记录
     */
    @GetMapping("/question/{questionId}")
    public ResponseEntity<List<PracticeRecord>> getQuestionPracticeRecords(@PathVariable Long questionId) {
        return ResponseEntity.ok(practiceRecordService.getQuestionPracticeRecords(questionId));
    }

    /**
     * 计算学生在特定作业上的总分和完成率
     */
    @GetMapping("/stats/student/{studentId}/homework/{homeworkId}")
    public ResponseEntity<Map<String, Object>> calculateStudentHomeworkStats(
            @PathVariable Long studentId, @PathVariable Long homeworkId) {
        return ResponseEntity.ok(practiceRecordService.calculateStudentHomeworkStats(studentId, homeworkId));
    }

    /**
     * 计算特定作业的班级统计数据
     */
    @GetMapping("/stats/class/{classId}/homework/{homeworkId}")
    public ResponseEntity<Map<String, Object>> calculateHomeworkClassStats(
            @PathVariable Long classId, @PathVariable Long homeworkId) {
        return ResponseEntity.ok(practiceRecordService.calculateHomeworkClassStats(homeworkId, classId));
    }

    /**
     * 获取特定课程的作业统计数据
     */
    @GetMapping("/stats/course/{courseId}")
    public ResponseEntity<List<Map<String, Object>>> getCourseHomeworkStats(@PathVariable Long courseId) {
        return ResponseEntity.ok(practiceRecordService.getCourseHomeworkStats(courseId));
    }

    /**
     * 获取学生的练习趋势（按天统计）
     */
    @GetMapping("/stats/student/{studentId}/trend")
    public ResponseEntity<List<Map<String, Object>>> getStudentPracticeTrend(@PathVariable Long studentId) {
        return ResponseEntity.ok(practiceRecordService.getStudentPracticeTrend(studentId));
    }

    /**
     * 获取学生最薄弱的题目（得分最低的题目）
     */
    @GetMapping("/stats/student/{studentId}/weak-questions")
    public ResponseEntity<List<Map<String, Object>>> getStudentWeakestQuestions(
            @PathVariable Long studentId, @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(practiceRecordService.getStudentWeakestQuestions(studentId, limit));
    }

    /**
     * 获取班级最薄弱的题目（正确率最低的题目）
     */
    @GetMapping("/stats/class/{classId}/weak-questions")
    public ResponseEntity<List<Map<String, Object>>> getClassWeakestQuestions(
            @PathVariable Long classId, @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(practiceRecordService.getClassWeakestQuestions(classId, limit));
    }

    /**
     * 计算学生在特定题目上的最佳得分
     */
    @GetMapping("/stats/student/{studentId}/question/{questionId}/best-score")
    public ResponseEntity<Double> getStudentBestScoreOnQuestion(
            @PathVariable Long studentId, @PathVariable Long questionId) {
        return ResponseEntity.ok(practiceRecordService.getStudentBestScoreOnQuestion(studentId, questionId));
    }

    /**
     * 获取特定时间段内的练习记录
     */
    @GetMapping("/student/{studentId}/time-range")
    public ResponseEntity<List<PracticeRecord>> getPracticeRecordsByTimeRange(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(practiceRecordService.getPracticeRecordsByTimeRange(studentId, startDate, endDate));
    }

    /**
     * 生成练习报告
     */
    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> generatePracticeReport(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        // 至少需要一个ID参数
        if (studentId == null && classId == null && courseId == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "至少需要提供studentId、classId或courseId其中一个参数");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // 如果未指定时间范围，默认为过去30天
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        return ResponseEntity.ok(practiceRecordService.generatePracticeReport(
                studentId, classId, courseId, startDate, endDate));
    }

    /**
     * 获取学生知识点掌握情况
     */
    @GetMapping("/stats/student/{studentId}/knowledge-points")
    public ResponseEntity<List<Map<String, Object>>> getStudentKnowledgePointStats(@PathVariable Long studentId) {
        return ResponseEntity.ok(practiceRecordService.getStudentKnowledgePointStats(studentId));
    }

    /**
     * 获取班级知识点掌握情况
     */
    @GetMapping("/stats/class/{classId}/knowledge-points")
    public ResponseEntity<List<Map<String, Object>>> getClassKnowledgePointStats(@PathVariable Long classId) {
        return ResponseEntity.ok(practiceRecordService.getClassKnowledgePointStats(classId));
    }

    /**
     * 获取课程知识点掌握情况
     */
    @GetMapping("/stats/course/{courseId}/knowledge-points")
    public ResponseEntity<List<Map<String, Object>>> getCourseKnowledgePointStats(@PathVariable Long courseId) {
        return ResponseEntity.ok(practiceRecordService.getCourseKnowledgePointStats(courseId));
    }
}