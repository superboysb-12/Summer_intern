package com.XuebaoMaster.backend.LearningJournalAnalysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface LearningJournalAnalysisRepository extends JpaRepository<LearningJournalAnalysis, Long> {
    @Query("SELECT l FROM LearningJournalAnalysis l WHERE l.question.id = :questionId")
    List<LearningJournalAnalysis> findByQuestionId(@Param("questionId") Long questionId);
    
    @Query("SELECT l FROM LearningJournalAnalysis l WHERE l.question.lessonNode.id = :lessonNodeId")
    List<LearningJournalAnalysis> findByLessonNodeId(@Param("lessonNodeId") Long lessonNodeId);
    
    @Query("SELECT l FROM LearningJournalAnalysis l WHERE l.question.lessonNode.course.id = :courseId")
    List<LearningJournalAnalysis> findByCourseId(@Param("courseId") Long courseId);
    
    @Query("SELECT l FROM LearningJournalAnalysis l WHERE l.createdAt BETWEEN :startTime AND :endTime")
    List<LearningJournalAnalysis> findByCreatedAtBetween(
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT l FROM LearningJournalAnalysis l WHERE l.question.id = :questionId AND l.createdAt BETWEEN :startTime AND :endTime")
    List<LearningJournalAnalysis> findByQuestionIdAndCreatedAtBetween(
            @Param("questionId") Long questionId,
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT COUNT(l) FROM LearningJournalAnalysis l WHERE l.question.id = :questionId")
    Long countByQuestionId(@Param("questionId") Long questionId);
    
    @Query("SELECT COUNT(l) FROM LearningJournalAnalysis l WHERE l.question.lessonNode.id = :lessonNodeId")
    Long countByLessonNodeId(@Param("lessonNodeId") Long lessonNodeId);
    
    @Query("SELECT COUNT(l) FROM LearningJournalAnalysis l WHERE l.question.lessonNode.course.id = :courseId")
    Long countByCourseId(@Param("courseId") Long courseId);
    
    @Query("SELECT l FROM LearningJournalAnalysis l WHERE LOWER(l.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<LearningJournalAnalysis> findByContentContainingIgnoreCase(@Param("keyword") String keyword);
} 