package com.XuebaoMaster.backend.StudentCourse.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.StudentCourse.StudentCourse;
import com.XuebaoMaster.backend.StudentCourse.StudentCourseRepository;
import com.XuebaoMaster.backend.StudentCourse.StudentCourseService;
import java.util.List;

@Service
public class StudentCourseServiceImpl implements StudentCourseService {

    @Autowired
    private StudentCourseRepository studentCourseRepository;

    @Override
    public StudentCourse enrollCourse(Long studentId, Long courseId) {
        // 检查是否已经选过此课程
        if (isEnrolled(studentId, courseId)) {
            throw new RuntimeException("已经选过该课程");
        }

        // 创建选课记录
        StudentCourse studentCourse = new StudentCourse();
        studentCourse.setStudentId(studentId);
        studentCourse.setCourseId(courseId);
        studentCourse.setProgress(0);
        studentCourse.setStatus("enrolled");

        return studentCourseRepository.save(studentCourse);
    }

    @Override
    public StudentCourse updateProgress(Long studentId, Long courseId, Integer progress) {
        StudentCourse studentCourse = studentCourseRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new RuntimeException("未找到选课记录"));

        // 验证进度值是否有效
        if (progress < 0 || progress > 100) {
            throw new RuntimeException("进度值必须在0-100之间");
        }

        studentCourse.setProgress(progress);

        // 如果进度达到100%，自动将状态设为已完成
        if (progress == 100) {
            studentCourse.setStatus("completed");
        }

        return studentCourseRepository.save(studentCourse);
    }

    @Override
    public StudentCourse updateStatus(Long studentId, Long courseId, String status) {
        StudentCourse studentCourse = studentCourseRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new RuntimeException("未找到选课记录"));

        // 验证状态是否有效
        if (!status.equals("enrolled") && !status.equals("completed") && !status.equals("withdrawn")) {
            throw new RuntimeException("无效的状态值");
        }

        studentCourse.setStatus(status);
        return studentCourseRepository.save(studentCourse);
    }

    @Override
    public List<StudentCourse> getStudentCourses(Long studentId) {
        return studentCourseRepository.findByStudentId(studentId);
    }

    @Override
    public List<StudentCourse> getStudentCoursesByStatus(Long studentId, String status) {
        return studentCourseRepository.findByStudentIdAndStatus(studentId, status);
    }

    @Override
    public List<StudentCourse> getCourseStudents(Long courseId) {
        return studentCourseRepository.findByCourseId(courseId);
    }

    @Override
    public boolean isEnrolled(Long studentId, Long courseId) {
        return studentCourseRepository.findByStudentIdAndCourseId(studentId, courseId).isPresent();
    }

    @Override
    public void deleteEnrollment(Long id) {
        studentCourseRepository.deleteById(id);
    }

    @Override
    public List<StudentCourse> getAllEnrollments() {
        return studentCourseRepository.findAll();
    }
}