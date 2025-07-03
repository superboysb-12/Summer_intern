package com.XuebaoMaster.backend.StudentEmotion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student-emotions")
public class StudentEmotionController {

    @Autowired
    private StudentEmotionService studentEmotionService;

    /**
     * 创建学生情感记录
     * 
     * @param request 包含userId和mark的请求体
     * @return 创建的学生情感记录
     */
    @PostMapping
    public ResponseEntity<StudentEmotion> createStudentEmotion(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Integer mark = Integer.valueOf(request.get("mark").toString());

            // 验证情绪评分在0-100范围内
            if (mark < 0 || mark > 100) {
                return ResponseEntity.badRequest().build();
            }

            StudentEmotion createdEmotion = studentEmotionService.createStudentEmotion(userId, mark);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdEmotion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新学生情感记录
     * 
     * @param id             学生情感记录ID
     * @param studentEmotion 学生情感信息
     * @return 更新后的学生情感记录
     */
    @PutMapping("/{id}")
    public ResponseEntity<StudentEmotion> updateStudentEmotion(
            @PathVariable Long id,
            @RequestBody StudentEmotion studentEmotion) {
        try {
            studentEmotion.setId(id);
            StudentEmotion updatedEmotion = studentEmotionService.updateStudentEmotion(studentEmotion);
            return ResponseEntity.ok(updatedEmotion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除学生情感记录
     * 
     * @param id 学生情感记录ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudentEmotion(@PathVariable Long id) {
        try {
            studentEmotionService.deleteStudentEmotion(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 根据ID获取学生情感记录
     * 
     * @param id 学生情感记录ID
     * @return 学生情感记录信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudentEmotion> getStudentEmotionById(@PathVariable Long id) {
        try {
            StudentEmotion studentEmotion = studentEmotionService.getStudentEmotionById(id);
            return ResponseEntity.ok(studentEmotion);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 根据用户ID获取学生情感记录
     * 
     * @param userId 用户ID
     * @return 学生情感记录列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<StudentEmotion>> getStudentEmotionsByUserId(@PathVariable Long userId) {
        try {
            List<StudentEmotion> emotions = studentEmotionService.getStudentEmotionsByUserId(userId);
            return ResponseEntity.ok(emotions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 根据用户ID和时间范围获取学生情感记录
     * 
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 学生情感记录列表
     */
    @GetMapping("/user/{userId}/period")
    public ResponseEntity<List<StudentEmotion>> getStudentEmotionsByUserIdAndPeriod(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<StudentEmotion> emotions = studentEmotionService.getStudentEmotionsByUserIdAndCreatedAtBetween(
                    userId, startTime, endTime);
            return ResponseEntity.ok(emotions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取大于等于指定评分的情感记录
     * 
     * @param minMark 最小评分
     * @return 学生情感记录列表
     */
    @GetMapping("/mark/min/{minMark}")
    public ResponseEntity<List<StudentEmotion>> getStudentEmotionsByMarkGreaterThanEqual(
            @PathVariable Integer minMark) {
        try {
            List<StudentEmotion> emotions = studentEmotionService.getStudentEmotionsByMarkGreaterThanEqual(minMark);
            return ResponseEntity.ok(emotions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取小于等于指定评分的情感记录
     * 
     * @param maxMark 最大评分
     * @return 学生情感记录列表
     */
    @GetMapping("/mark/max/{maxMark}")
    public ResponseEntity<List<StudentEmotion>> getStudentEmotionsByMarkLessThanEqual(@PathVariable Integer maxMark) {
        try {
            List<StudentEmotion> emotions = studentEmotionService.getStudentEmotionsByMarkLessThanEqual(maxMark);
            return ResponseEntity.ok(emotions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取指定评分范围内的情感记录
     * 
     * @param minMark 最小评分
     * @param maxMark 最大评分
     * @return 学生情感记录列表
     */
    @GetMapping("/mark/range")
    public ResponseEntity<List<StudentEmotion>> getStudentEmotionsByMarkBetween(
            @RequestParam Integer minMark,
            @RequestParam Integer maxMark) {
        try {
            List<StudentEmotion> emotions = studentEmotionService.getStudentEmotionsByMarkBetween(minMark, maxMark);
            return ResponseEntity.ok(emotions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取所有学生情感记录
     * 
     * @return 学生情感记录列表
     */
    @GetMapping
    public ResponseEntity<List<StudentEmotion>> getAllStudentEmotions() {
        try {
            List<StudentEmotion> emotions = studentEmotionService.getAllStudentEmotions();
            return ResponseEntity.ok(emotions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取用户情感统计信息
     * 
     * @param userId 用户ID
     * @return 情感统计信息
     */
    @GetMapping("/statistics/user/{userId}")
    public ResponseEntity<Map<String, Object>> getEmotionStatisticsByUserId(@PathVariable Long userId) {
        try {
            Map<String, Object> statistics = studentEmotionService.getEmotionStatisticsByUserId(userId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取指定用户和时间范围内的情感统计信息
     * 
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 情感统计信息
     */
    @GetMapping("/statistics/user/{userId}/period")
    public ResponseEntity<Map<String, Object>> getEmotionStatisticsByUserIdAndPeriod(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            Map<String, Object> statistics = studentEmotionService.getEmotionStatisticsByUserIdAndCreatedAtBetween(
                    userId, startTime, endTime);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取整体情感统计信息
     * 
     * @return 整体情感统计信息
     */
    @GetMapping("/statistics/overall")
    public ResponseEntity<Map<String, Object>> getOverallEmotionStatistics() {
        try {
            Map<String, Object> statistics = studentEmotionService.getOverallEmotionStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}