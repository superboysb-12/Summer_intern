package com.XuebaoMaster.backend.PracticeRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 练习记录服务接口
 */
public interface PracticeRecordService {

        /**
         * 创建练习记录
         */
        PracticeRecord createPracticeRecord(PracticeRecord practiceRecord);

        /**
         * 更新练习记录
         */
        PracticeRecord updatePracticeRecord(PracticeRecord practiceRecord);

        /**
         * 提交练习答案
         * 
         * @param studentId  学生ID
         * @param homeworkId 作业ID
         * @param questionId 题目ID
         * @param score      得分（百分制）
         * @param answerData 答案数据（JSON格式）
         * @param timeSpent  用时（秒）
         * @return 创建的练习记录
         */
        PracticeRecord submitPracticeAnswer(
                        Long studentId, Long homeworkId, Long questionId,
                        Double score, String answerData, Integer timeSpent);

        /**
         * 获取学生的所有练习记录
         */
        List<PracticeRecord> getStudentPracticeRecords(Long studentId);

        /**
         * 获取学生在特定作业的练习记录
         */
        List<PracticeRecord> getStudentHomeworkRecords(Long studentId, Long homeworkId);

        /**
         * 获取特定题目的所有练习记录
         */
        List<PracticeRecord> getQuestionPracticeRecords(Long questionId);

        /**
         * 获取特定练习记录
         */
        PracticeRecord getPracticeRecordById(Long id);

        /**
         * 删除练习记录
         */
        void deletePracticeRecord(Long id);

        /**
         * 批量删除练习记录（例如删除一个作业的所有记录）
         */
        void deletePracticeRecordsByHomeworkId(Long homeworkId);

        /**
         * 计算学生在特定作业上的总分和完成率
         * 
         * @return Map包含：total_score（总分）, completed_count（已完成题目数）, total_count（总题目数）,
         *         completion_rate（完成率）
         */
        Map<String, Object> calculateStudentHomeworkStats(Long studentId, Long homeworkId);

        /**
         * 计算特定作业的班级统计数据
         * 
         * @return Map包含：average_score（平均分）, student_count（提交学生数）, completion_rate（完成率）
         */
        Map<String, Object> calculateHomeworkClassStats(Long homeworkId, Long classId);

        /**
         * 获取特定课程的作业统计数据
         */
        List<Map<String, Object>> getCourseHomeworkStats(Long courseId);

        /**
         * 获取学生的练习趋势（按天统计）
         */
        List<Map<String, Object>> getStudentPracticeTrend(Long studentId);

        /**
         * 获取学生最薄弱的题目（得分最低的题目）
         */
        List<Map<String, Object>> getStudentWeakestQuestions(Long studentId, int limit);

        /**
         * 获取班级最薄弱的题目（正确率最低的题目）
         */
        List<Map<String, Object>> getClassWeakestQuestions(Long classId, int limit);

        /**
         * 计算学生在特定题目上的最佳得分
         */
        Double getStudentBestScoreOnQuestion(Long studentId, Long questionId);

        /**
         * 获取特定时间段内的练习记录
         */
        List<PracticeRecord> getPracticeRecordsByTimeRange(Long studentId, LocalDateTime startDate,
                        LocalDateTime endDate);

        /**
         * 导出练习报告（为特定学生或班级）
         * 
         * @return 报告数据（可以是JSON格式，前端负责渲染）
         */
        Map<String, Object> generatePracticeReport(Long studentId, Long classId, Long courseId, LocalDateTime startDate,
                        LocalDateTime endDate);

        /**
         * 获取学生知识点掌握情况
         * 
         * @param studentId 学生ID
         * @return 知识点统计列表，每项包含 knowledge_point, average_score, attempt_count
         */
        List<Map<String, Object>> getStudentKnowledgePointStats(Long studentId);

        /**
         * 获取班级知识点掌握情况
         */
        List<Map<String, Object>> getClassKnowledgePointStats(Long classId);

        /**
         * 获取课程知识点掌握情况
         */
        List<Map<String, Object>> getCourseKnowledgePointStats(Long courseId);
}