package com.XuebaoMaster.backend.TeachingPlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teaching-plans")
public class TeachingPlanController {
    @Autowired
    private TeachingPlanService teachingPlanService;

    /**
     * 创建教案
     * 
     * @param teachingPlan 教案信息
     * @return 创建的教案
     */
    @PostMapping
    public ResponseEntity<TeachingPlan> createTeachingPlan(@RequestBody TeachingPlan teachingPlan) {
        return ResponseEntity.ok(teachingPlanService.createTeachingPlan(teachingPlan));
    }

    /**
     * 获取所有教案
     * 
     * @return 教案列表
     */
    @GetMapping
    public ResponseEntity<List<TeachingPlan>> getAllTeachingPlans() {
        return ResponseEntity.ok(teachingPlanService.getAllTeachingPlans());
    }

    /**
     * 根据ID获取教案
     * 
     * @param id 教案ID
     * @return 教案信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeachingPlan> getTeachingPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(teachingPlanService.getTeachingPlanById(id));
    }

    /**
     * 根据课时节点ID获取教案
     * 
     * @param lessonNodeId 课时节点ID
     * @return 教案列表
     */
    @GetMapping("/lesson-node/{lessonNodeId}")
    public ResponseEntity<List<TeachingPlan>> getTeachingPlansByLessonNodeId(@PathVariable Long lessonNodeId) {
        return ResponseEntity.ok(teachingPlanService.getTeachingPlansByLessonNodeId(lessonNodeId));
    }
    
    /**
     * 根据课时节点ID获取单个教案
     * 
     * @param lessonNodeId 课时节点ID
     * @return 教案信息
     */
    @GetMapping("/lesson-node/{lessonNodeId}/one")
    public ResponseEntity<?> getOneTeachingPlanByLessonNodeId(@PathVariable Long lessonNodeId) {
        return teachingPlanService.getOneTeachingPlanByLessonNodeId(lessonNodeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据生成状态获取教案
     * 
     * @param status 生成状态
     * @return 教案列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TeachingPlan>> getTeachingPlansByStatus(@PathVariable String status) {
        TeachingPlan.GenerationStatus generationStatus = TeachingPlan.GenerationStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(teachingPlanService.getTeachingPlansByStatus(generationStatus));
    }

    /**
     * 更新教案信息
     * 
     * @param id 教案ID
     * @param teachingPlan 教案信息
     * @return 更新后的教案
     */
    @PutMapping("/{id}")
    public ResponseEntity<TeachingPlan> updateTeachingPlan(@PathVariable Long id, @RequestBody TeachingPlan teachingPlan) {
        teachingPlan.setId(id);
        return ResponseEntity.ok(teachingPlanService.updateTeachingPlan(teachingPlan));
    }
    
    /**
     * 更新教案生成状态
     * 
     * @param id 教案ID
     * @param status 生成状态
     * @return 更新后的教案
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<TeachingPlan> updateGenerationStatus(@PathVariable Long id, @RequestParam String status) {
        TeachingPlan.GenerationStatus generationStatus = TeachingPlan.GenerationStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(teachingPlanService.updateGenerationStatus(id, generationStatus));
    }

    /**
     * 删除教案
     * 
     * @param id 教案ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeachingPlan(@PathVariable Long id) {
        teachingPlanService.deleteTeachingPlan(id);
        return ResponseEntity.ok().build();
    }
} 