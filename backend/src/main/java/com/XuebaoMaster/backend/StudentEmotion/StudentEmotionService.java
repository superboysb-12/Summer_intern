package com.XuebaoMaster.backend.StudentEmotion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface StudentEmotionService {
    StudentEmotion createStudentEmotion(StudentEmotion studentEmotion);
    
    StudentEmotion createStudentEmotion(Long userId, Integer mark);

    StudentEmotion updateStudentEmotion(StudentEmotion studentEmotion);

    void deleteStudentEmotion(Long id);

    StudentEmotion getStudentEmotionById(Long id);
    
    List<StudentEmotion> getStudentEmotionsByUserId(Long userId);
    
    List<StudentEmotion> getStudentEmotionsByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    List<StudentEmotion> getStudentEmotionsByMarkGreaterThanEqual(Integer minMark);
    
    List<StudentEmotion> getStudentEmotionsByMarkLessThanEqual(Integer maxMark);
    
    List<StudentEmotion> getStudentEmotionsByMarkBetween(Integer minMark, Integer maxMark);

    List<StudentEmotion> getAllStudentEmotions();
    
    Map<String, Object> getEmotionStatisticsByUserId(Long userId);
    
    Map<String, Object> getEmotionStatisticsByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    Map<String, Object> getOverallEmotionStatistics();
} 