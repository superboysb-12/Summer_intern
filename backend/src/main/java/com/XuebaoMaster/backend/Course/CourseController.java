package com.XuebaoMaster.backend.Course;

import com.XuebaoMaster.backend.RAG.Course.CourseRagMappingService;
import com.XuebaoMaster.backend.RAG.RAG;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.XuebaoMaster.backend.StudentCourse.StudentCourseService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private StudentCourseService studentCourseService;

    @Autowired
    private CourseRagMappingService courseRagMappingService;

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

    /**
     * 获取课程关联的所有RAG资源
     * 
     * @param courseId 课程ID
     * @return RAG列表
     */
    @GetMapping("/{courseId}/rags")
    public ResponseEntity<?> getCourseRAGs(@PathVariable Long courseId) {
        try {
            List<RAG> rags = courseRagMappingService.findRagsByCourseId(courseId);
            return ResponseEntity.ok(rags);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "获取课程关联RAG失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 将RAG关联到课程
     * 
     * @param courseId 课程ID
     * @param ragId    RAG ID
     * @return 操作结果
     */
    @PostMapping("/{courseId}/rags/{ragId}")
    public ResponseEntity<?> associateRAGWithCourse(@PathVariable Long courseId, @PathVariable Long ragId) {
        try {
            if (courseRagMappingService.mappingExists(courseId, ragId)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "该RAG已与课程关联");
                return ResponseEntity.badRequest().body(error);
            }

            courseRagMappingService.createMapping(courseId, ragId);

            Map<String, String> success = new HashMap<>();
            success.put("message", "RAG成功关联到课程");
            return ResponseEntity.ok(success);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "关联RAG到课程失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 解除RAG与课程的关联
     * 
     * @param courseId 课程ID
     * @param ragId    RAG ID
     * @return 操作结果
     */
    @DeleteMapping("/{courseId}/rags/{ragId}")
    public ResponseEntity<?> disassociateRAGFromCourse(@PathVariable Long courseId, @PathVariable Long ragId) {
        try {
            if (!courseRagMappingService.mappingExists(courseId, ragId)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "该RAG未与课程关联");
                return ResponseEntity.badRequest().body(error);
            }

            courseRagMappingService.deleteMappingByCourseAndRag(courseId, ragId);

            Map<String, String> success = new HashMap<>();
            success.put("message", "RAG成功从课程移除");
            return ResponseEntity.ok(success);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "从课程移除RAG失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 检查RAG是否与课程关联
     * 
     * @param courseId 课程ID
     * @param ragId    RAG ID
     * @return 是否关联
     */
    @GetMapping("/{courseId}/rags/{ragId}/exists")
    public ResponseEntity<Boolean> checkRAGAssociation(@PathVariable Long courseId, @PathVariable Long ragId) {
        boolean exists = courseRagMappingService.mappingExists(courseId, ragId);
        return ResponseEntity.ok(exists);
    }
}