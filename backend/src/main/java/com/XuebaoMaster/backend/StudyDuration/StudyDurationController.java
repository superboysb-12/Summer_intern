package com.XuebaoMaster.backend.StudyDuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/study-durations")
public class StudyDurationController {
    @Autowired
    private StudyDurationService studyDurationService;

    /**
     * 创建学习时长记录
     * 
     * @param studyDuration 学习时长信息
     * @return 创建的学习时长记录
     */
    @PostMapping
    public ResponseEntity<StudyDuration> createStudyDuration(@RequestBody StudyDuration studyDuration) {
        return ResponseEntity.ok(studyDurationService.createStudyDuration(studyDuration));
    }
    
    /**
     * 创建学习时长记录（简化版）
     * 
     * @param lessonStartTimeStamp 课程开始时间戳
     * @param length 学习时长（分钟）
     * @return 创建的学习时长记录
     */
    @PostMapping("/simple")
    public ResponseEntity<StudyDuration> createSimpleStudyDuration(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lessonStartTimeStamp,
            @RequestParam Integer length) {
        return ResponseEntity.ok(studyDurationService.createStudyDuration(lessonStartTimeStamp, length));
    }

    /**
     * 获取所有学习时长记录
     * 
     * @return 学习时长记录列表
     */
    @GetMapping
    public ResponseEntity<List<StudyDuration>> getAllStudyDurations() {
        return ResponseEntity.ok(studyDurationService.getAllStudyDurations());
    }

    /**
     * 根据ID获取学习时长记录
     * 
     * @param id 学习时长记录ID
     * @return 学习时长记录信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudyDuration> getStudyDurationById(@PathVariable Long id) {
        return ResponseEntity.ok(studyDurationService.getStudyDurationById(id));
    }

    /**
     * 根据当前时间戳范围获取学习时长记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 学习时长记录列表
     */
    @GetMapping("/current-time")
    public ResponseEntity<List<StudyDuration>> getStudyDurationsByCurrentTimeStampBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(studyDurationService.getStudyDurationsByCurrentTimeStampBetween(startTime, endTime));
    }
    
    /**
     * 根据课程开始时间戳范围获取学习时长记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 学习时长记录列表
     */
    @GetMapping("/lesson-start-time")
    public ResponseEntity<List<StudyDuration>> getStudyDurationsByLessonStartTimeStampBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(studyDurationService.getStudyDurationsByLessonStartTimeStampBetween(startTime, endTime));
    }
    
    /**
     * 获取大于等于指定时长的学习记录
     * 
     * @param minLength 最小时长（分钟）
     * @return 学习时长记录列表
     */
    @GetMapping("/min-length/{minLength}")
    public ResponseEntity<List<StudyDuration>> getStudyDurationsByLengthGreaterThanEqual(@PathVariable Integer minLength) {
        return ResponseEntity.ok(studyDurationService.getStudyDurationsByLengthGreaterThanEqual(minLength));
    }
    
    /**
     * 获取小于等于指定时长的学习记录
     * 
     * @param maxLength 最大时长（分钟）
     * @return 学习时长记录列表
     */
    @GetMapping("/max-length/{maxLength}")
    public ResponseEntity<List<StudyDuration>> getStudyDurationsByLengthLessThanEqual(@PathVariable Integer maxLength) {
        return ResponseEntity.ok(studyDurationService.getStudyDurationsByLengthLessThanEqual(maxLength));
    }
    
    /**
     * 获取指定时长范围内的学习记录
     * 
     * @param minLength 最小时长（分钟）
     * @param maxLength 最大时长（分钟）
     * @return 学习时长记录列表
     */
    @GetMapping("/length-range")
    public ResponseEntity<List<StudyDuration>> getStudyDurationsByLengthBetween(
            @RequestParam Integer minLength,
            @RequestParam Integer maxLength) {
        return ResponseEntity.ok(studyDurationService.getStudyDurationsByLengthBetween(minLength, maxLength));
    }

    /**
     * 更新学习时长记录
     * 
     * @param id 学习时长记录ID
     * @param studyDuration 学习时长信息
     * @return 更新后的学习时长记录
     */
    @PutMapping("/{id}")
    public ResponseEntity<StudyDuration> updateStudyDuration(@PathVariable Long id, @RequestBody StudyDuration studyDuration) {
        studyDuration.setId(id);
        return ResponseEntity.ok(studyDurationService.updateStudyDuration(studyDuration));
    }

    /**
     * 删除学习时长记录
     * 
     * @param id 学习时长记录ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudyDuration(@PathVariable Long id) {
        studyDurationService.deleteStudyDuration(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取学习统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStudyStatistics() {
        return ResponseEntity.ok(studyDurationService.getStudyStatistics());
    }
    
    /**
     * 获取指定时间范围内的学习统计信息
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息
     */
    @GetMapping("/statistics/time-range")
    public ResponseEntity<Map<String, Object>> getStudyStatisticsBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(studyDurationService.getStudyStatisticsBetween(startTime, endTime));
    }
} 