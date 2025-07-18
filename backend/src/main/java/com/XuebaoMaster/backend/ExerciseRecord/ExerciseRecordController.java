package com.XuebaoMaster.backend.ExerciseRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/exercise-records")
public class ExerciseRecordController {

    @Autowired
    private ExerciseRecordService exerciseRecordService;

    /**
     * 提交学生的练习答案
     * 
     * @param request 包含学生ID、问题ID、作业ID和答案内容的请求体
     * @return 保存的练习记录
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitAnswer(@RequestBody Map<String, Object> request) {
        Long studentId = getLongValue(request.get("studentId"));
        Long questionId = getLongValue(request.get("questionId"));
        Long homeworkId = getLongValue(request.get("homeworkId"));
        String answerContent = (String) request.get("answerContent");

        if (studentId == null || questionId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "学生ID和问题ID不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            ExerciseRecord record = exerciseRecordService.submitAnswer(studentId, questionId, homeworkId,
                    answerContent);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "答案提交成功");
            response.put("recordId", record.getId());
            response.put("status", record.getStatus());

            // 如果已经评分，返回分数和反馈
            if ("GRADED".equals(record.getStatus())) {
                response.put("score", record.getScore());
                response.put("feedback", record.getFeedback());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "答案提交失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取练习记录详情
     * 
     * @param id 练习记录ID
     * @return 练习记录详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getExerciseRecord(@PathVariable Long id) {
        try {
            Optional<ExerciseRecord> recordOpt = exerciseRecordService.getExerciseRecordById(id);

            if (recordOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "找不到指定的练习记录");
                return ResponseEntity.notFound().build();
            }

            ExerciseRecord record = recordOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("record", record);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取练习记录失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 对未评分的练习进行评分
     * 
     * @param id 练习记录ID
     * @return 评分后的练习记录
     */
    @PostMapping("/{id}/grade")
    public ResponseEntity<Map<String, Object>> gradeExercise(@PathVariable Long id) {
        try {
            ExerciseRecord record = exerciseRecordService.gradeExercise(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "评分完成");
            response.put("record", record);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "评分失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取学生的所有练习记录
     * 
     * @param studentId 学生ID
     * @return 练习记录列表
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<Map<String, Object>> getStudentExercises(@PathVariable Long studentId) {
        try {
            List<ExerciseRecord> records = exerciseRecordService.getExerciseRecordsByStudentId(studentId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("records", records);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取学生练习记录失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取作业的所有练习记录
     * 
     * @param homeworkId 作业ID
     * @return 练习记录列表
     */
    @GetMapping("/homework/{homeworkId}")
    public ResponseEntity<Map<String, Object>> getHomeworkExercises(@PathVariable Long homeworkId) {
        try {
            List<ExerciseRecord> records = exerciseRecordService.getExerciseRecordsByHomeworkId(homeworkId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("records", records);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取作业练习记录失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取问题的所有练习记录
     * 
     * @param questionId 问题ID
     * @return 练习记录列表
     */
    @GetMapping("/question/{questionId}")
    public ResponseEntity<Map<String, Object>> getQuestionExercises(@PathVariable Long questionId) {
        try {
            List<ExerciseRecord> records = exerciseRecordService.getExerciseRecordsByQuestionId(questionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("records", records);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取问题练习记录失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取学生在某课程的练习统计数据
     * 
     * @param studentId 学生ID
     * @param courseId  课程ID
     * @return 统计数据
     */
    @GetMapping("/statistics/student/{studentId}/course/{courseId}")
    public ResponseEntity<Map<String, Object>> getStudentCourseStatistics(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        try {
            Map<String, Object> statistics = exerciseRecordService.getStudentCourseStatistics(studentId, courseId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取学生课程统计失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取班级在某作业的练习统计数据
     * 
     * @param classId    班级ID
     * @param homeworkId 作业ID
     * @return 统计数据
     */
    @GetMapping("/statistics/class/{classId}/homework/{homeworkId}")
    public ResponseEntity<Map<String, Object>> getClassHomeworkStatistics(
            @PathVariable Long classId,
            @PathVariable Long homeworkId) {
        try {
            Map<String, Object> statistics = exerciseRecordService.getClassHomeworkStatistics(classId, homeworkId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取班级作业统计失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取课程的练习统计数据
     * 
     * @param courseId 课程ID
     * @return 统计数据
     */
    @GetMapping("/statistics/course/{courseId}")
    public ResponseEntity<Map<String, Object>> getCourseStatistics(@PathVariable Long courseId) {
        try {
            Map<String, Object> statistics = exerciseRecordService.getCourseStatistics(courseId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取课程统计失败: " + e.getMessage());
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
}