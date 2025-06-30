package com.XuebaoMaster.backend.Course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {
    @Autowired
    private CourseService courseService;

    /**
     * 创建课程
     * 
     * @param course 课程信息
     * @return 创建的课程
     */
    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        return ResponseEntity.ok(courseService.createCourse(course));
    }

    /**
     * 获取所有课程
     * 
     * @return 课程列表
     */
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    /**
     * 根据ID获取课程
     * 
     * @param courseId 课程ID
     * @return 课程信息
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseById(courseId));
    }

    /**
     * 更新课程信息
     * 
     * @param courseId 课程ID
     * @param course 课程信息
     * @return 更新后的课程
     */
    @PutMapping("/{courseId}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long courseId, @RequestBody Course course) {
        course.setCourseId(courseId);
        return ResponseEntity.ok(courseService.updateCourse(course));
    }

    /**
     * 删除课程
     * 
     * @param courseId 课程ID
     * @return 无内容响应
     */
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.ok().build();
    }

    /**
     * 搜索课程
     * 
     * @param keyword 关键词
     * @return 课程列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<Course>> searchCourses(@RequestParam String keyword) {
        return ResponseEntity.ok(courseService.searchCourses(keyword));
    }

    /**
     * 检查课程名是否存在
     * 
     * @param name 课程名
     * @return 是否存在
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkNameExists(@RequestParam String name) {
        return ResponseEntity.ok(courseService.checkNameExists(name));
    }
} 