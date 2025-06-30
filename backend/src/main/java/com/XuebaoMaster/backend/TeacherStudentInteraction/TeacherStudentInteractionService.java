package com.XuebaoMaster.backend.TeacherStudentInteraction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TeacherStudentInteractionService {
    TeacherStudentInteraction createInteraction(TeacherStudentInteraction interaction);
    
    TeacherStudentInteraction createInteraction(Long teacherId, Long studentId, String content);

    TeacherStudentInteraction updateInteraction(TeacherStudentInteraction interaction);

    void deleteInteraction(Long id);

    TeacherStudentInteraction getInteractionById(Long id);
    
    List<TeacherStudentInteraction> getInteractionsByTeacherId(Long teacherId);
    
    List<TeacherStudentInteraction> getInteractionsByStudentId(Long studentId);
    
    List<TeacherStudentInteraction> getInteractionsByTeacherIdAndStudentId(Long teacherId, Long studentId);
    
    List<TeacherStudentInteraction> getInteractionsByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    List<TeacherStudentInteraction> getInteractionsByTeacherIdAndCreatedAtBetween(
            Long teacherId, LocalDateTime startTime, LocalDateTime endTime);
    
    List<TeacherStudentInteraction> getInteractionsByStudentIdAndCreatedAtBetween(
            Long studentId, LocalDateTime startTime, LocalDateTime endTime);
    
    List<TeacherStudentInteraction> getInteractionsByTeacherIdAndStudentIdAndCreatedAtBetween(
            Long teacherId, Long studentId, LocalDateTime startTime, LocalDateTime endTime);

    List<TeacherStudentInteraction> getAllInteractions();
    
    Map<String, Object> getInteractionStatisticsByTeacherId(Long teacherId);
    
    Map<String, Object> getInteractionStatisticsByStudentId(Long studentId);
    
    Map<String, Object> getInteractionStatisticsByTeacherIdAndStudentId(Long teacherId, Long studentId);
    
    Map<String, Object> getOverallInteractionStatistics();
} 