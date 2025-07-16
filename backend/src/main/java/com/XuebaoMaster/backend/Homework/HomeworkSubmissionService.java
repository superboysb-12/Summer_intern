package com.XuebaoMaster.backend.Homework;

import java.util.List;

public interface HomeworkSubmissionService {
    // 基本CRUD操作
    HomeworkSubmission createSubmission(HomeworkSubmission submission);

    HomeworkSubmission updateSubmission(HomeworkSubmission submission);

    void deleteSubmission(Long submissionId);

    HomeworkSubmission getSubmissionById(Long submissionId);

    List<HomeworkSubmission> getAllSubmissions();

    // 特殊查询
    List<HomeworkSubmission> getSubmissionsByHomework(Long homeworkId);

    List<HomeworkSubmission> getSubmissionsByStudent(Long studentId);

    HomeworkSubmission getSubmissionByStudentAndHomework(Long studentId, Long homeworkId);

    // 评分相关
    HomeworkSubmission gradeSubmission(Long submissionId, Integer score, String feedback);

    // 状态相关
    List<HomeworkSubmission> getSubmissionsByStatus(String status);

    List<HomeworkSubmission> getSubmissionsByHomeworkAndStatus(Long homeworkId, String status);

    List<HomeworkSubmission> getSubmissionsByStudentAndStatus(Long studentId, String status);

    HomeworkSubmission updateSubmissionStatus(Long submissionId, String status);
}