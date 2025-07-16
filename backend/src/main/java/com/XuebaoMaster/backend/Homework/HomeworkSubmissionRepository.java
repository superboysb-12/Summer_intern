package com.XuebaoMaster.backend.Homework;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, Long> {
    // 根据作业ID查询所有提交
    List<HomeworkSubmission> findByHomeworkId(Long homeworkId);

    // 根据学生ID查询所有提交
    List<HomeworkSubmission> findByStudentId(Long studentId);

    // 根据学生ID和作业ID查询提交
    Optional<HomeworkSubmission> findByStudentIdAndHomeworkId(Long studentId, Long homeworkId);

    // 根据状态查询提交
    List<HomeworkSubmission> findByStatus(String status);

    // 根据作业ID和状态查询提交
    List<HomeworkSubmission> findByHomeworkIdAndStatus(Long homeworkId, String status);

    // 根据学生ID和状态查询提交
    List<HomeworkSubmission> findByStudentIdAndStatus(Long studentId, String status);
}