package com.XuebaoMaster.backend.StudentCourse;

import java.util.List;

public interface StudentCourseService {
    // 学生选课
    StudentCourse enrollCourse(Long studentId, Long courseId);

    // 更新学习进度
    StudentCourse updateProgress(Long studentId, Long courseId, Integer progress);

    // 更改课程状态(完成课程/退课)
    StudentCourse updateStatus(Long studentId, Long courseId, String status);

    // 获取学生选课记录
    List<StudentCourse> getStudentCourses(Long studentId);

    // 获取学生特定状态的课程
    List<StudentCourse> getStudentCoursesByStatus(Long studentId, String status);

    // 获取课程的所有学生
    List<StudentCourse> getCourseStudents(Long courseId);

    // 检查学生是否已选某课程
    boolean isEnrolled(Long studentId, Long courseId);

    // 删除选课记录（管理员功能）
    void deleteEnrollment(Long id);

    // 获取所有选课记录（管理员功能）
    List<StudentCourse> getAllEnrollments();
}