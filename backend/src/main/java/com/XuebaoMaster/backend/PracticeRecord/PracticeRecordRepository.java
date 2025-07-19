package com.XuebaoMaster.backend.PracticeRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Repository
public interface PracticeRecordRepository extends JpaRepository<PracticeRecord, Long> {

        // 基本查询方法

        // 根据学生ID查询练习记录
        List<PracticeRecord> findByStudentId(Long studentId);

        // 根据作业ID查询练习记录
        List<PracticeRecord> findByHomeworkId(Long homeworkId);

        // 根据题目ID查询练习记录
        List<PracticeRecord> findByQuestionId(Long questionId);

        // 根据学生ID和作业ID查询练习记录
        List<PracticeRecord> findByStudentIdAndHomeworkId(Long studentId, Long homeworkId);

        // 根据学生ID和题目ID查询练习记录
        List<PracticeRecord> findByStudentIdAndQuestionId(Long studentId, Long questionId);

        // 根据学生ID、作业ID和题目ID查询最新的练习记录
        Optional<PracticeRecord> findTopByStudentIdAndHomeworkIdAndQuestionIdOrderBySubmittedAtDesc(
                        Long studentId, Long homeworkId, Long questionId);

        // 高级统计查询

        // 班级维度：查询特定作业的平均分
        @Query("SELECT AVG(p.score) FROM PracticeRecord p WHERE p.homeworkId = :homeworkId")
        Double calculateAverageScoreByHomeworkId(@Param("homeworkId") Long homeworkId);

        // 班级维度：查询特定作业的完成率（提交答案的学生数 / 班级总人数）
        @Query(value = "SELECT COUNT(DISTINCT p.student_id) FROM practice_records p WHERE p.homework_id = :homeworkId", nativeQuery = true)
        Long countDistinctStudentsByHomeworkId(@Param("homeworkId") Long homeworkId);

        // 班级维度：查询特定题目的正确率
        @Query("SELECT COUNT(p) FROM PracticeRecord p WHERE p.questionId = :questionId AND p.isCorrect = true")
        Long countCorrectAnswersByQuestionId(@Param("questionId") Long questionId);

        @Query("SELECT COUNT(p) FROM PracticeRecord p WHERE p.questionId = :questionId")
        Long countTotalAnswersByQuestionId(@Param("questionId") Long questionId);

        // 课程维度：查询课程下所有作业的完成情况
        @Query(value = "SELECT p.homework_id, COUNT(DISTINCT p.student_id) as student_count, " +
                        "AVG(p.score) as avg_score FROM practice_records p " +
                        "JOIN homeworks h ON p.homework_id = h.id " +
                        "WHERE h.course_id = :courseId " +
                        "GROUP BY p.homework_id", nativeQuery = true)
        List<Object[]> getHomeworkStatsByCourseId(@Param("courseId") Long courseId);

        // 学生个人维度：查询学生在特定时间段内的练习记录
        List<PracticeRecord> findByStudentIdAndSubmittedAtBetween(
                        Long studentId, LocalDateTime startDate, LocalDateTime endDate);

        // 学生个人维度：查询学生的平均分
        @Query("SELECT AVG(p.score) FROM PracticeRecord p WHERE p.studentId = :studentId")
        Double calculateAverageScoreByStudentId(@Param("studentId") Long studentId);

        // 学生个人维度：查询学生的练习趋势（按时间分组）
        @Query(value = "SELECT DATE(p.submitted_at) as practice_date, AVG(p.score) as avg_score " +
                        "FROM practice_records p " +
                        "WHERE p.student_id = :studentId " +
                        "GROUP BY DATE(p.submitted_at) " +
                        "ORDER BY practice_date", nativeQuery = true)
        List<Object[]> getStudentPracticeTrend(@Param("studentId") Long studentId);

        // 查询最薄弱的题目（正确率最低的题目）
        @Query(value = "SELECT p.question_id, " +
                        "SUM(CASE WHEN p.is_correct = true THEN 1 ELSE 0 END) / COUNT(*) as correct_rate " +
                        "FROM practice_records p " +
                        "GROUP BY p.question_id " +
                        "ORDER BY correct_rate ASC " +
                        "LIMIT :limit", nativeQuery = true)
        List<Object[]> findWeakestQuestions(@Param("limit") int limit);

        // 查询学生最薄弱的题目
        @Query(value = "SELECT p.question_id, " +
                        "MIN(p.score) as min_score " +
                        "FROM practice_records p " +
                        "WHERE p.student_id = :studentId " +
                        "GROUP BY p.question_id " +
                        "ORDER BY min_score ASC " +
                        "LIMIT :limit", nativeQuery = true)
        List<Object[]> findStudentWeakestQuestions(@Param("studentId") Long studentId, @Param("limit") int limit);

        // 学生知识点掌握情况
        @Query(value = "SELECT qg.query as knowledge_point, " +
                        "AVG(pr.score) as average_score, " +
                        "COUNT(pr.id) as attempt_count " +
                        "FROM practice_records pr " +
                        "JOIN homework_questions hq ON pr.question_id = hq.question_id " +
                        "JOIN question_generator qg ON hq.question_id = qg.id " +
                        "WHERE pr.student_id = :studentId " +
                        "GROUP BY qg.query " +
                        "ORDER BY average_score ASC", nativeQuery = true)
        List<Map<String, Object>> findStudentKnowledgePointStats(@Param("studentId") Long studentId);

        // 班级知识点掌握情况
        @Query(value = "SELECT qg.query as knowledge_point, " +
                        "AVG(pr.score) as average_score, " +
                        "COUNT(pr.id) as attempt_count " +
                        "FROM practice_records pr " +
                        "JOIN homework_questions hq ON pr.question_id = hq.question_id " +
                        "JOIN question_generator qg ON hq.question_id = qg.id " +
                        "JOIN users u ON pr.student_id = u.id " +
                        "WHERE u.class_id = :classId " +
                        "GROUP BY qg.query " +
                        "ORDER BY average_score ASC", nativeQuery = true)
        List<Map<String, Object>> findClassKnowledgePointStats(@Param("classId") Long classId);

        // 课程知识点掌握情况
        @Query(value = "SELECT qg.query as knowledge_point, " +
                        "AVG(pr.score) as average_score, " +
                        "COUNT(pr.id) as attempt_count " +
                        "FROM practice_records pr " +
                        "JOIN homework_questions hq ON pr.question_id = hq.question_id " +
                        "JOIN question_generator qg ON hq.question_id = qg.id " +
                        "JOIN homeworks hw ON pr.homework_id = hw.id " +
                        "WHERE hw.course_id = :courseId " +
                        "GROUP BY qg.query " +
                        "ORDER BY average_score ASC", nativeQuery = true)
        List<Map<String, Object>> findCourseKnowledgePointStats(@Param("courseId") Long courseId);
}