package com.XuebaoMaster.backend.LessonNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lesson-nodes")
public class LessonNodeController {
    @Autowired
    private LessonNodeService lessonNodeService;

    /**
     * 创建课时节点
     * 
     * @param lessonNode 课时节点信息
     * @return 创建的课时节点
     */
    @PostMapping
    public ResponseEntity<LessonNode> createLessonNode(@RequestBody LessonNode lessonNode) {
        return ResponseEntity.ok(lessonNodeService.createLessonNode(lessonNode));
    }

    /**
     * 获取所有课时节点
     * 
     * @return 课时节点列表
     */
    @GetMapping
    public ResponseEntity<List<LessonNode>> getAllLessonNodes() {
        return ResponseEntity.ok(lessonNodeService.getAllLessonNodes());
    }

    /**
     * 根据ID获取课时节点
     * 
     * @param id 课时节点ID
     * @return 课时节点信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<LessonNode> getLessonNodeById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonNodeService.getLessonNodeById(id));
    }

    /**
     * 根据课程ID获取课时节点
     * 
     * @param courseId 课程ID
     * @return 课时节点列表
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<LessonNode>> getLessonNodesByCourseId(@PathVariable Long courseId) {
        return ResponseEntity.ok(lessonNodeService.getLessonNodesByCourseId(courseId));
    }
    
    /**
     * 根据课程ID获取按节点顺序排序的课时节点
     * 
     * @param courseId 课程ID
     * @return 课时节点列表
     */
    @GetMapping("/course/{courseId}/ordered")
    public ResponseEntity<List<LessonNode>> getLessonNodesByCourseIdOrdered(@PathVariable Long courseId) {
        return ResponseEntity.ok(lessonNodeService.getLessonNodesByCourseIdOrdered(courseId));
    }
    
    /**
     * 根据课程ID和节点顺序获取课时节点
     * 
     * @param courseId 课程ID
     * @param nodeOrder 节点顺序
     * @return 课时节点信息
     */
    @GetMapping("/course/{courseId}/order/{nodeOrder}")
    public ResponseEntity<LessonNode> getLessonNodeByCourseIdAndOrder(
            @PathVariable Long courseId, @PathVariable Integer nodeOrder) {
        return ResponseEntity.ok(lessonNodeService.getLessonNodeByCourseIdAndOrder(courseId, nodeOrder));
    }

    /**
     * 更新课时节点信息
     * 
     * @param id 课时节点ID
     * @param lessonNode 课时节点信息
     * @return 更新后的课时节点
     */
    @PutMapping("/{id}")
    public ResponseEntity<LessonNode> updateLessonNode(@PathVariable Long id, @RequestBody LessonNode lessonNode) {
        lessonNode.setId(id);
        return ResponseEntity.ok(lessonNodeService.updateLessonNode(lessonNode));
    }

    /**
     * 删除课时节点
     * 
     * @param id 课时节点ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLessonNode(@PathVariable Long id) {
        lessonNodeService.deleteLessonNode(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 搜索课时节点
     * 
     * @param keyword 关键词
     * @return 课时节点列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<LessonNode>> searchLessonNodes(@RequestParam String keyword) {
        return ResponseEntity.ok(lessonNodeService.searchLessonNodes(keyword));
    }
} 