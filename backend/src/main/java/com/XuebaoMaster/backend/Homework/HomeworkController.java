package com.XuebaoMaster.backend.Homework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/homeworks")
public class HomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    // Create a new homework
    @PostMapping
    public ResponseEntity<Homework> createHomework(@RequestBody Homework homework) {
        return ResponseEntity.ok(homeworkService.createHomework(homework));
    }

    // Get all homeworks (admin function)
    @GetMapping
    public ResponseEntity<List<Homework>> getAllHomeworks() {
        return ResponseEntity.ok(homeworkService.getAllHomeworks());
    }

    // Get homework by ID
    @GetMapping("/{id}")
    public ResponseEntity<Homework> getHomeworkById(@PathVariable Long id) {
        return ResponseEntity.ok(homeworkService.getHomeworkById(id));
    }

    // Update homework
    @PutMapping("/{id}")
    public ResponseEntity<Homework> updateHomework(@PathVariable Long id, @RequestBody Homework homework) {
        homework.setId(id);
        return ResponseEntity.ok(homeworkService.updateHomework(homework));
    }

    // Delete homework
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHomework(@PathVariable Long id) {
        homeworkService.deleteHomework(id);
        return ResponseEntity.ok().build();
    }

    // Get homeworks by course ID
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Homework>> getHomeworksByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(homeworkService.getHomeworksByCourse(courseId));
    }

    // Get homeworks by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Homework>> getHomeworksByStatus(@PathVariable String status) {
        return ResponseEntity.ok(homeworkService.getHomeworksByStatus(status));
    }

    // Get homeworks by course ID and status
    @GetMapping("/course/{courseId}/status/{status}")
    public ResponseEntity<List<Homework>> getHomeworksByCourseAndStatus(
            @PathVariable Long courseId,
            @PathVariable String status) {
        return ResponseEntity.ok(homeworkService.getHomeworksByCourseAndStatus(courseId, status));
    }

    // Update homework status
    @PutMapping("/{id}/status")
    public ResponseEntity<Homework> updateHomeworkStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        String status = statusUpdate.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(homeworkService.updateHomeworkStatus(id, status));
    }
}