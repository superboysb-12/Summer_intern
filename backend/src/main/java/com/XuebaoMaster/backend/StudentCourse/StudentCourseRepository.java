package com.XuebaoMaster.backend.StudentCourse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentCourseRepository extends JpaRepository<StudentCourse, Long> {
    // 查询学生的所有课程关系
    List<StudentCourse> findByStudentId(Long studentId);

    // 查询某个课程的所有学生关系
    List<StudentCourse> findByCourseId(Long courseId);

    // 查询特定学生的特定课程关系
    Optional<StudentCourse> findByStudentIdAndCourseId(Long studentId, Long courseId);

    // 按状态查询学生课程关系
    List<StudentCourse> findByStudentIdAndStatus(Long studentId, String status);
}