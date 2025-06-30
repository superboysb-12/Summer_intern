package com.XuebaoMaster.backend.TeachingPlan;

import java.util.List;
import java.util.Optional;

public interface TeachingPlanService {
    TeachingPlan createTeachingPlan(TeachingPlan teachingPlan);

    TeachingPlan updateTeachingPlan(TeachingPlan teachingPlan);

    void deleteTeachingPlan(Long id);

    TeachingPlan getTeachingPlanById(Long id);

    List<TeachingPlan> getTeachingPlansByLessonNodeId(Long lessonNodeId);
    
    Optional<TeachingPlan> getOneTeachingPlanByLessonNodeId(Long lessonNodeId);
    
    List<TeachingPlan> getTeachingPlansByStatus(TeachingPlan.GenerationStatus status);

    List<TeachingPlan> getAllTeachingPlans();
    
    TeachingPlan updateGenerationStatus(Long id, TeachingPlan.GenerationStatus status);
} 