package com.XuebaoMaster.backend.TeachingPlan.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.TeachingPlan.TeachingPlan;
import com.XuebaoMaster.backend.TeachingPlan.TeachingPlanRepository;
import com.XuebaoMaster.backend.TeachingPlan.TeachingPlanService;

import java.util.List;
import java.util.Optional;

@Service
public class TeachingPlanServiceImpl implements TeachingPlanService {

    @Autowired
    private TeachingPlanRepository teachingPlanRepository;

    @Override
    public TeachingPlan createTeachingPlan(TeachingPlan teachingPlan) {
        return teachingPlanRepository.save(teachingPlan);
    }

    @Override
    public TeachingPlan updateTeachingPlan(TeachingPlan teachingPlan) {
        TeachingPlan existingPlan = teachingPlanRepository.findById(teachingPlan.getId())
                .orElseThrow(() -> new RuntimeException("Teaching plan not found"));

        existingPlan.setLessonNode(teachingPlan.getLessonNode());
        existingPlan.setContent(teachingPlan.getContent());
        existingPlan.setGenerationStatus(teachingPlan.getGenerationStatus());

        return teachingPlanRepository.save(existingPlan);
    }

    @Override
    public void deleteTeachingPlan(Long id) {
        teachingPlanRepository.deleteById(id);
    }

    @Override
    public TeachingPlan getTeachingPlanById(Long id) {
        return teachingPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teaching plan not found"));
    }

    @Override
    public List<TeachingPlan> getTeachingPlansByLessonNodeId(Long lessonNodeId) {
        return teachingPlanRepository.findByLessonNodeId(lessonNodeId);
    }
    
    @Override
    public Optional<TeachingPlan> getOneTeachingPlanByLessonNodeId(Long lessonNodeId) {
        return teachingPlanRepository.findOneByLessonNodeId(lessonNodeId);
    }
    
    @Override
    public List<TeachingPlan> getTeachingPlansByStatus(TeachingPlan.GenerationStatus status) {
        return teachingPlanRepository.findByGenerationStatus(status);
    }

    @Override
    public List<TeachingPlan> getAllTeachingPlans() {
        return teachingPlanRepository.findAll();
    }
    
    @Override
    public TeachingPlan updateGenerationStatus(Long id, TeachingPlan.GenerationStatus status) {
        TeachingPlan plan = teachingPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teaching plan not found"));
        
        plan.setGenerationStatus(status);
        return teachingPlanRepository.save(plan);
    }
} 