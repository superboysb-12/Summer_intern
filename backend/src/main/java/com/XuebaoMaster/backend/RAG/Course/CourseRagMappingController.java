package com.XuebaoMaster.backend.RAG.Course;

import com.XuebaoMaster.backend.Course.Course;
import com.XuebaoMaster.backend.RAG.RAG;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course-rag")
public class CourseRagMappingController {

    @Autowired
    private CourseRagMappingService courseRagMappingService;

    @PostMapping("/map")
    public ResponseEntity<?> createMapping(@RequestParam Long courseId, @RequestParam Long ragId) {
        try {
            CourseRagMapping mapping = courseRagMappingService.createMapping(courseId, ragId);
            return new ResponseEntity<>(mapping, HttpStatus.CREATED);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/unmap")
    public ResponseEntity<?> deleteMapping(@RequestParam Long courseId, @RequestParam Long ragId) {
        try {
            courseRagMappingService.deleteMappingByCourseAndRag(courseId, ragId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "映射关系已成功删除");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/course/{courseId}/rags")
    public ResponseEntity<List<RAG>> getRagsByCourse(@PathVariable Long courseId) {
        List<RAG> rags = courseRagMappingService.findRagsByCourseId(courseId);
        return new ResponseEntity<>(rags, HttpStatus.OK);
    }

    @GetMapping("/rag/{ragId}/courses")
    public ResponseEntity<List<Course>> getCoursesByRag(@PathVariable Long ragId) {
        List<Course> courses = courseRagMappingService.findCoursesByRagId(ragId);
        return new ResponseEntity<>(courses, HttpStatus.OK);
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> mappingExists(@RequestParam Long courseId, @RequestParam Long ragId) {
        boolean exists = courseRagMappingService.mappingExists(courseId, ragId);
        return new ResponseEntity<>(exists, HttpStatus.OK);
    }
}