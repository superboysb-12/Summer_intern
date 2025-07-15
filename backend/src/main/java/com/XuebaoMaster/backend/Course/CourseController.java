package com.XuebaoMaster.backend.Course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.XuebaoMaster.backend.StudentCourse.StudentCourseService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private StudentCourseService studentCourseService;

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        return ResponseEntity.ok(courseService.createCourse(course));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        course.setCourseId(id);
        return ResponseEntity.ok(courseService.updateCourse(course));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
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

    /**
     * 获取指定学生的课程列表
     * 
     * @param studentId 学生ID
     * @return 该学生的课程列表
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Course>> getCoursesByStudentId(@PathVariable Long studentId) {
        try {
            // 尝试使用学生-课程关联服务获取课程
            return ResponseEntity.ok(studentCourseService.getStudentCourses(studentId).stream()
                    .map(sc -> {
                        Course course = courseService.getCourseById(sc.getCourseId());
                        course.setProgress(sc.getProgress());
                        course.setStatus(sc.getStatus());
                        return course;
                    })
                    .collect(java.util.stream.Collectors.toList()));
        } catch (Exception e) {
            // 如果出错，回退到返回所有课程（临时方案）
            return ResponseEntity.ok(courseService.getAllCourses());
        }
    }

    /**
     * 获取指定教师的课程列表
     * 
     * @param teacherId 教师ID
     * @return 该教师的课程列表
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<Course>> getCoursesByTeacherId(@PathVariable Long teacherId) {
        // 使用新添加的方法获取教师课程列表
        return ResponseEntity.ok(courseService.getCoursesByTeacherId(teacherId));
    }
}