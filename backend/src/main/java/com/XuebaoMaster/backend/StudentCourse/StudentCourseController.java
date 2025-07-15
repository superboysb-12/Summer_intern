package com.XuebaoMaster.backend.StudentCourse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.XuebaoMaster.backend.Course.Course;
import com.XuebaoMaster.backend.Course.CourseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student-courses")
public class StudentCourseController {

    @Autowired
    private StudentCourseService studentCourseService;

    @Autowired
    private CourseService courseService;

    /**
     * 获取所有选课记录（管理员功能）
     * 
     * @return 所有选课记录列表
     */
    @GetMapping
    public ResponseEntity<List<StudentCourse>> getAllEnrollments() {
        try {
            List<StudentCourse> allEnrollments = studentCourseService.getAllEnrollments();
            return ResponseEntity.ok(allEnrollments);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "获取选课记录失败: " + e.getMessage());
        }
    }

    /**
     * 学生选课
     * 
     * @param requestBody 包含学生ID和课程ID的请求体
     * @return 创建的选课记录
     */
    @PostMapping("/enroll")
    public ResponseEntity<StudentCourse> enrollCourse(@RequestBody Map<String, Long> requestBody) {
        try {
            Long studentId = requestBody.get("studentId");
            Long courseId = requestBody.get("courseId");

            if (studentId == null || courseId == null) {
                return ResponseEntity.badRequest().build();
            }

            StudentCourse studentCourse = studentCourseService.enrollCourse(studentId, courseId);
            return ResponseEntity.ok(studentCourse);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 更新学习进度
     * 
     * @param requestBody 包含学生ID、课程ID和进度的请求体
     * @return 更新后的选课记录
     */
    @PutMapping("/progress")
    public ResponseEntity<StudentCourse> updateProgress(@RequestBody Map<String, Object> requestBody) {
        try {
            Long studentId = Long.valueOf(requestBody.get("studentId").toString());
            Long courseId = Long.valueOf(requestBody.get("courseId").toString());
            Integer progress = Integer.valueOf(requestBody.get("progress").toString());

            StudentCourse studentCourse = studentCourseService.updateProgress(studentId, courseId, progress);
            return ResponseEntity.ok(studentCourse);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 更改课程状态
     * 
     * @param requestBody 包含学生ID、课程ID和状态的请求体
     * @return 更新后的选课记录
     */
    @PutMapping("/status")
    public ResponseEntity<StudentCourse> updateStatus(@RequestBody Map<String, Object> requestBody) {
        try {
            Long studentId = Long.valueOf(requestBody.get("studentId").toString());
            Long courseId = Long.valueOf(requestBody.get("courseId").toString());
            String status = requestBody.get("status").toString();

            StudentCourse studentCourse = studentCourseService.updateStatus(studentId, courseId, status);
            return ResponseEntity.ok(studentCourse);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 获取学生的所有课程详情（包含课程信息）
     * 
     * @param studentId 学生ID
     * @return 课程列表
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Course>> getStudentCourses(@PathVariable Long studentId) {
        try {
            List<StudentCourse> studentCourses = studentCourseService.getStudentCourses(studentId);
            List<Course> courses = new ArrayList<>();

            // 获取每个关联记录对应的课程详情
            for (StudentCourse sc : studentCourses) {
                Course course = courseService.getCourseById(sc.getCourseId());
                // 将进度信息添加到课程信息中
                course.setProgress(sc.getProgress());
                // 将选课状态添加到课程状态中
                course.setStatus(sc.getStatus());
                courses.add(course);
            }

            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "获取学生课程失败: " + e.getMessage());
        }
    }

    /**
     * 获取课程的所有学生
     * 
     * @param courseId 课程ID
     * @return 选课记录列表
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<StudentCourse>> getCourseStudents(@PathVariable Long courseId) {
        return ResponseEntity.ok(studentCourseService.getCourseStudents(courseId));
    }

    /**
     * 删除选课记录
     * 
     * @param id 选课记录ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnrollment(@PathVariable Long id) {
        studentCourseService.deleteEnrollment(id);
        return ResponseEntity.ok().build();
    }
}