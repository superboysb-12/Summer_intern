package com.XuebaoMaster.backend.TeacherStudentInteraction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher-student-interactions")
public class TeacherStudentInteractionController {

    @Autowired
    private TeacherStudentInteractionService interactionService;

    /**
     * 创建师生互动记录
     * 
     * @param request 包含teacherId、studentId和content的请求体
     * @return 创建的师生互动记录
     */
    @PostMapping
    public ResponseEntity<TeacherStudentInteraction> createInteraction(@RequestBody Map<String, Object> request) {
        try {
            Long teacherId = Long.valueOf(request.get("teacherId").toString());
            Long studentId = Long.valueOf(request.get("studentId").toString());
            String content = request.get("content").toString();
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            TeacherStudentInteraction createdInteraction = interactionService.createInteraction(teacherId, studentId, content);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdInteraction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新师生互动记录
     * 
     * @param id 师生互动记录ID
     * @param interaction 师生互动信息
     * @return 更新后的师生互动记录
     */
    @PutMapping("/{id}")
    public ResponseEntity<TeacherStudentInteraction> updateInteraction(
            @PathVariable Long id,
            @RequestBody TeacherStudentInteraction interaction) {
        try {
            interaction.setId(id);
            TeacherStudentInteraction updatedInteraction = interactionService.updateInteraction(interaction);
            return ResponseEntity.ok(updatedInteraction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除师生互动记录
     * 
     * @param id 师生互动记录ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInteraction(@PathVariable Long id) {
        try {
            interactionService.deleteInteraction(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 根据ID获取师生互动记录
     * 
     * @param id 师生互动记录ID
     * @return 师生互动记录信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeacherStudentInteraction> getInteractionById(@PathVariable Long id) {
        try {
            TeacherStudentInteraction interaction = interactionService.getInteractionById(id);
            return ResponseEntity.ok(interaction);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 根据教师ID获取师生互动记录
     * 
     * @param teacherId 教师ID
     * @return 师生互动记录列表
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<TeacherStudentInteraction>> getInteractionsByTeacherId(@PathVariable Long teacherId) {
        try {
            List<TeacherStudentInteraction> interactions = interactionService.getInteractionsByTeacherId(teacherId);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据学生ID获取师生互动记录
     * 
     * @param studentId 学生ID
     * @return 师生互动记录列表
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<TeacherStudentInteraction>> getInteractionsByStudentId(@PathVariable Long studentId) {
        try {
            List<TeacherStudentInteraction> interactions = interactionService.getInteractionsByStudentId(studentId);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据教师ID和学生ID获取师生互动记录
     * 
     * @param teacherId 教师ID
     * @param studentId 学生ID
     * @return 师生互动记录列表
     */
    @GetMapping("/teacher/{teacherId}/student/{studentId}")
    public ResponseEntity<List<TeacherStudentInteraction>> getInteractionsByTeacherIdAndStudentId(
            @PathVariable Long teacherId, @PathVariable Long studentId) {
        try {
            List<TeacherStudentInteraction> interactions = interactionService.getInteractionsByTeacherIdAndStudentId(teacherId, studentId);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据时间范围获取师生互动记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 师生互动记录列表
     */
    @GetMapping("/time-range")
    public ResponseEntity<List<TeacherStudentInteraction>> getInteractionsByCreatedAtBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<TeacherStudentInteraction> interactions = interactionService.getInteractionsByCreatedAtBetween(startTime, endTime);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据教师ID和时间范围获取师生互动记录
     * 
     * @param teacherId 教师ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 师生互动记录列表
     */
    @GetMapping("/teacher/{teacherId}/time-range")
    public ResponseEntity<List<TeacherStudentInteraction>> getInteractionsByTeacherIdAndCreatedAtBetween(
            @PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<TeacherStudentInteraction> interactions = interactionService.getInteractionsByTeacherIdAndCreatedAtBetween(
                    teacherId, startTime, endTime);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据学生ID和时间范围获取师生互动记录
     * 
     * @param studentId 学生ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 师生互动记录列表
     */
    @GetMapping("/student/{studentId}/time-range")
    public ResponseEntity<List<TeacherStudentInteraction>> getInteractionsByStudentIdAndCreatedAtBetween(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<TeacherStudentInteraction> interactions = interactionService.getInteractionsByStudentIdAndCreatedAtBetween(
                    studentId, startTime, endTime);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据教师ID、学生ID和时间范围获取师生互动记录
     * 
     * @param teacherId 教师ID
     * @param studentId 学生ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 师生互动记录列表
     */
    @GetMapping("/teacher/{teacherId}/student/{studentId}/time-range")
    public ResponseEntity<List<TeacherStudentInteraction>> getInteractionsByTeacherIdAndStudentIdAndCreatedAtBetween(
            @PathVariable Long teacherId,
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<TeacherStudentInteraction> interactions = interactionService.getInteractionsByTeacherIdAndStudentIdAndCreatedAtBetween(
                    teacherId, studentId, startTime, endTime);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取所有师生互动记录
     * 
     * @return 师生互动记录列表
     */
    @GetMapping
    public ResponseEntity<List<TeacherStudentInteraction>> getAllInteractions() {
        try {
            List<TeacherStudentInteraction> interactions = interactionService.getAllInteractions();
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取教师互动统计信息
     * 
     * @param teacherId 教师ID
     * @return 互动统计信息
     */
    @GetMapping("/statistics/teacher/{teacherId}")
    public ResponseEntity<Map<String, Object>> getInteractionStatisticsByTeacherId(@PathVariable Long teacherId) {
        try {
            Map<String, Object> statistics = interactionService.getInteractionStatisticsByTeacherId(teacherId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取学生互动统计信息
     * 
     * @param studentId 学生ID
     * @return 互动统计信息
     */
    @GetMapping("/statistics/student/{studentId}")
    public ResponseEntity<Map<String, Object>> getInteractionStatisticsByStudentId(@PathVariable Long studentId) {
        try {
            Map<String, Object> statistics = interactionService.getInteractionStatisticsByStudentId(studentId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取特定教师和学生之间的互动统计信息
     * 
     * @param teacherId 教师ID
     * @param studentId 学生ID
     * @return 互动统计信息
     */
    @GetMapping("/statistics/teacher/{teacherId}/student/{studentId}")
    public ResponseEntity<Map<String, Object>> getInteractionStatisticsByTeacherIdAndStudentId(
            @PathVariable Long teacherId, @PathVariable Long studentId) {
        try {
            Map<String, Object> statistics = interactionService.getInteractionStatisticsByTeacherIdAndStudentId(teacherId, studentId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取整体互动统计信息
     * 
     * @return 整体互动统计信息
     */
    @GetMapping("/statistics/overall")
    public ResponseEntity<Map<String, Object>> getOverallInteractionStatistics() {
        try {
            Map<String, Object> statistics = interactionService.getOverallInteractionStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 