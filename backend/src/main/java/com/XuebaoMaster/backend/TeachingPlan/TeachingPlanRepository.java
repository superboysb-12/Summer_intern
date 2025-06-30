package com.XuebaoMaster.backend.TeachingPlan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TeachingPlanRepository extends JpaRepository<TeachingPlan, Long> {
    @Query("SELECT t FROM TeachingPlan t WHERE t.lessonNode.id = :lessonNodeId")
    List<TeachingPlan> findByLessonNodeId(@Param("lessonNodeId") Long lessonNodeId);
    
    @Query("SELECT t FROM TeachingPlan t WHERE t.lessonNode.id = :lessonNodeId")
    Optional<TeachingPlan> findOneByLessonNodeId(@Param("lessonNodeId") Long lessonNodeId);
    
    List<TeachingPlan> findByGenerationStatus(TeachingPlan.GenerationStatus status);
} 