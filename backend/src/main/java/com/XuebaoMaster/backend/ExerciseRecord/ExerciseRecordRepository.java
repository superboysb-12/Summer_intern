package com.XuebaoMaster.backend.ExerciseRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseRecordRepository extends JpaRepository<ExerciseRecord, Long> {
    // 根据学生ID查询练习记录
    List<ExerciseRecord> findByStudentId(Long studentId);

    // 根据作业ID查询练习记录
    List<ExerciseRecord> findByHomeworkId(Long homeworkId);

    // 根据问题ID查询练习记录
    List<ExerciseRecord> findByQuestionId(Long questionId);

    // 根据学生ID和作业ID查询练习记录
    List<ExerciseRecord> findByStudentIdAndHomeworkId(Long studentId, Long homeworkId);

    // 根据课程ID查询练习记录
    List<ExerciseRecord> findByCourseId(Long courseId);

    // 根据班级ID查询练习记录
    List<ExerciseRecord> findByClassId(Long classId);

    // 根据作业ID和班级ID查询练习记录
    List<ExerciseRecord> findByHomeworkIdAndClassId(Long homeworkId, Long classId);

    // 根据学生ID和课程ID查询练习记录
    List<ExerciseRecord> findByStudentIdAndCourseId(Long studentId, Long courseId);

    // 获取班级某次作业的平均分
    @Query("SELECT AVG(e.score) FROM ExerciseRecord e WHERE e.homeworkId = ?1 AND e.classId = ?2 AND e.status = 'GRADED'")
    Double getClassHomeworkAverageScore(Long homeworkId, Long classId);

    // 获取某学生在某课程的所有练习记录的平均分
    @Query("SELECT AVG(e.score) FROM ExerciseRecord e WHERE e.studentId = ?1 AND e.courseId = ?2 AND e.status = 'GRADED'")
    Double getStudentCourseAverageScore(Long studentId, Long courseId);

    // 获取某课程下所有已评分题目的平均分
    @Query("SELECT AVG(e.score) FROM ExerciseRecord e WHERE e.courseId = ?1 AND e.status = 'GRADED'")
    Double getCourseAverageScore(Long courseId);

    // 获取某问题类型的平均分
    @Query("SELECT AVG(e.score) FROM ExerciseRecord e WHERE e.questionType = ?1 AND e.status = 'GRADED'")
    Double getQuestionTypeAverageScore(String questionType);

    // 统计某班级已完成某作业的学生人数
    @Query("SELECT COUNT(DISTINCT e.studentId) FROM ExerciseRecord e WHERE e.homeworkId = ?1 AND e.classId = ?2")
    Long countStudentsCompletedHomework(Long homeworkId, Long classId);
}