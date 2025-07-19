package com.XuebaoMaster.backend.PracticeRecord.impl;

import com.XuebaoMaster.backend.Homework.Homework;
import com.XuebaoMaster.backend.Homework.HomeworkRepository;
import com.XuebaoMaster.backend.HomeworkQuestion.HomeworkQuestion;
import com.XuebaoMaster.backend.HomeworkQuestion.HomeworkQuestionService;
import com.XuebaoMaster.backend.PracticeRecord.PracticeRecord;
import com.XuebaoMaster.backend.PracticeRecord.PracticeRecordRepository;
import com.XuebaoMaster.backend.PracticeRecord.PracticeRecordService;
import com.XuebaoMaster.backend.SchoolClass.SchoolClassService;
import com.XuebaoMaster.backend.StudentCourse.StudentCourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PracticeRecordServiceImpl implements PracticeRecordService {

    @Autowired
    private PracticeRecordRepository practiceRecordRepository;

    @Autowired
    private HomeworkQuestionService homeworkQuestionService;

    @Autowired
    private SchoolClassService schoolClassService;

    @Autowired
    private StudentCourseRepository studentCourseRepository;

    @Autowired
    private HomeworkRepository homeworkRepository;

    @Override
    public PracticeRecord createPracticeRecord(PracticeRecord practiceRecord) {
        // 验证分数范围
        validateScore(practiceRecord.getScore());

        // 设置提交时间（如果未指定）
        if (practiceRecord.getSubmittedAt() == null) {
            practiceRecord.setSubmittedAt(LocalDateTime.now());
        }

        // 根据分数设置是否正确（>= 60分视为正确）
        if (practiceRecord.getIsCorrect() == null) {
            practiceRecord.setIsCorrect(practiceRecord.getScore() >= 60);
        }

        return practiceRecordRepository.save(practiceRecord);
    }

    @Override
    @Transactional
    public PracticeRecord updatePracticeRecord(PracticeRecord practiceRecord) {
        // 验证分数范围
        validateScore(practiceRecord.getScore());

        // 检查记录是否存在
        PracticeRecord existingRecord = practiceRecordRepository.findById(practiceRecord.getId())
                .orElseThrow(() -> new RuntimeException("练习记录不存在，ID: " + practiceRecord.getId()));

        // 更新记录信息
        existingRecord.setScore(practiceRecord.getScore());
        existingRecord.setAnswerData(practiceRecord.getAnswerData());
        existingRecord.setTimeSpent(practiceRecord.getTimeSpent());
        existingRecord.setIsCorrect(practiceRecord.getScore() >= 60);
        existingRecord.setAttemptCount(practiceRecord.getAttemptCount());

        return practiceRecordRepository.save(existingRecord);
    }

    @Override
    @Transactional
    public PracticeRecord submitPracticeAnswer(
            Long studentId, Long homeworkId, Long questionId,
            Double score, String answerData, Integer timeSpent) {

        // 验证分数
        validateScore(score);

        // 检查题目是否属于该作业
        if (!homeworkQuestionService.isQuestionInHomework(homeworkId, questionId)) {
            throw new IllegalArgumentException("该题目不属于指定的作业");
        }

        // 查找该学生该题目的最新练习记录
        Optional<PracticeRecord> latestRecord = practiceRecordRepository
                .findTopByStudentIdAndHomeworkIdAndQuestionIdOrderBySubmittedAtDesc(
                        studentId, homeworkId, questionId);

        PracticeRecord practiceRecord = new PracticeRecord();
        practiceRecord.setStudentId(studentId);
        practiceRecord.setHomeworkId(homeworkId);
        practiceRecord.setQuestionId(questionId);
        practiceRecord.setScore(score);
        practiceRecord.setAnswerData(answerData);
        practiceRecord.setTimeSpent(timeSpent);
        practiceRecord.setSubmittedAt(LocalDateTime.now());
        practiceRecord.setIsCorrect(score >= 60);

        // 设置尝试次数
        practiceRecord.setAttemptCount(
                latestRecord.map(PracticeRecord::getAttemptCount).orElse(0) + 1);

        return practiceRecordRepository.save(practiceRecord);
    }

    @Override
    public List<PracticeRecord> getStudentPracticeRecords(Long studentId) {
        return practiceRecordRepository.findByStudentId(studentId);
    }

    @Override
    public List<PracticeRecord> getStudentHomeworkRecords(Long studentId, Long homeworkId) {
        return practiceRecordRepository.findByStudentIdAndHomeworkId(studentId, homeworkId);
    }

    @Override
    public List<PracticeRecord> getQuestionPracticeRecords(Long questionId) {
        return practiceRecordRepository.findByQuestionId(questionId);
    }

    @Override
    public PracticeRecord getPracticeRecordById(Long id) {
        return practiceRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("练习记录不存在，ID: " + id));
    }

    @Override
    public void deletePracticeRecord(Long id) {
        practiceRecordRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deletePracticeRecordsByHomeworkId(Long homeworkId) {
        List<PracticeRecord> records = practiceRecordRepository.findByHomeworkId(homeworkId);
        practiceRecordRepository.deleteAll(records);
    }

    @Override
    public Map<String, Object> calculateStudentHomeworkStats(Long studentId, Long homeworkId) {
        Map<String, Object> stats = new HashMap<>();

        // 获取作业中的题目列表
        List<HomeworkQuestion> homeworkQuestions = homeworkQuestionService.getQuestionsByHomeworkId(homeworkId);
        int totalQuestions = homeworkQuestions.size();

        // 获取学生在该作业中的所有练习记录
        List<PracticeRecord> studentRecords = practiceRecordRepository
                .findByStudentIdAndHomeworkId(studentId, homeworkId);

        // 按题目ID分组，找出每个题目的最高分
        Map<Long, PracticeRecord> bestRecords = new HashMap<>();

        for (PracticeRecord record : studentRecords) {
            Long questionId = record.getQuestionId();
            if (!bestRecords.containsKey(questionId) ||
                    bestRecords.get(questionId).getScore() < record.getScore()) {
                bestRecords.put(questionId, record);
            }
        }

        // 计算总分和完成题目数量
        double totalScore = 0.0;
        int completedCount = bestRecords.size();

        for (PracticeRecord record : bestRecords.values()) {
            totalScore += record.getScore();
        }

        // 平均分（已完成的题目）
        double averageScore = completedCount > 0 ? totalScore / completedCount : 0;

        // 作业总分（按百分比计算）
        double homeworkScore = totalQuestions > 0 ? (totalScore / totalQuestions) : 0;

        // 完成率
        double completionRate = totalQuestions > 0 ? (double) completedCount / totalQuestions : 0;

        // 添加到统计结果
        stats.put("total_score", homeworkScore);
        stats.put("average_score", averageScore);
        stats.put("completed_count", completedCount);
        stats.put("total_count", totalQuestions);
        stats.put("completion_rate", completionRate);

        return stats;
    }

    @Override
    public Map<String, Object> calculateHomeworkClassStats(Long homeworkId, Long classId) {
        Map<String, Object> stats = new HashMap<>();

        // 获取班级中所有学生ID
        List<Long> studentIds = schoolClassService.getStudentIdsByClassId(classId);
        int totalStudents = studentIds.size();

        // 获取提交了该作业的学生数量
        Long submittedStudentsCount = practiceRecordRepository.countDistinctStudentsByHomeworkId(homeworkId);

        // 计算作业平均分
        Double averageScore = practiceRecordRepository.calculateAverageScoreByHomeworkId(homeworkId);
        if (averageScore == null)
            averageScore = 0.0;

        // 计算完成率
        double completionRate = totalStudents > 0 ? (double) submittedStudentsCount / totalStudents : 0;

        // 添加到统计结果
        stats.put("average_score", averageScore);
        stats.put("student_count", submittedStudentsCount);
        stats.put("total_students", totalStudents);
        stats.put("completion_rate", completionRate);

        return stats;
    }

    @Override
    public List<Map<String, Object>> getCourseHomeworkStats(Long courseId) {
        long totalStudentsInCourse = studentCourseRepository.countByCourseId(courseId);

        List<Object[]> homeworkStats = practiceRecordRepository.getHomeworkStatsByCourseId(courseId);

        return homeworkStats.stream().map(stat -> {
            Map<String, Object> homeworkStat = new HashMap<>();
            Long homeworkId = ((Number) stat[0]).longValue();
            long studentCount = ((Number) stat[1]).longValue();
            double averageScore = (Double) stat[2];

            Homework homework = homeworkRepository.findById(homeworkId).orElse(null);
            String homeworkTitle = (homework != null) ? homework.getTitle() : "未知作业";

            double completionRate = (totalStudentsInCourse > 0) ? (double) studentCount / totalStudentsInCourse : 0;

            homeworkStat.put("homeworkId", homeworkId);
            homeworkStat.put("homeworkTitle", homeworkTitle);
            homeworkStat.put("studentCount", studentCount);
            homeworkStat.put("averageScore", averageScore);
            homeworkStat.put("completionRate", completionRate);

            return homeworkStat;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getStudentPracticeTrend(Long studentId) {
        List<Object[]> trend = practiceRecordRepository.getStudentPracticeTrend(studentId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] dataPoint : trend) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", dataPoint[0].toString());
            point.put("average_score", (Double) dataPoint[1]);
            result.add(point);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getStudentWeakestQuestions(Long studentId, int limit) {
        List<Object[]> weakQuestions = practiceRecordRepository.findStudentWeakestQuestions(studentId, limit);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] question : weakQuestions) {
            Map<String, Object> questionMap = new HashMap<>();
            questionMap.put("question_id", ((Number) question[0]).longValue());
            questionMap.put("min_score", (Double) question[1]);
            result.add(questionMap);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getClassWeakestQuestions(Long classId, int limit) {
        // 获取所有习题的正确率
        List<Object[]> weakQuestions = practiceRecordRepository.findWeakestQuestions(limit);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] question : weakQuestions) {
            Map<String, Object> questionMap = new HashMap<>();
            questionMap.put("question_id", ((Number) question[0]).longValue());
            questionMap.put("correct_rate", (Double) question[1]);
            result.add(questionMap);
        }

        return result;
    }

    @Override
    public Double getStudentBestScoreOnQuestion(Long studentId, Long questionId) {
        List<PracticeRecord> records = practiceRecordRepository.findByStudentIdAndQuestionId(studentId, questionId);

        return records.stream()
                .mapToDouble(PracticeRecord::getScore)
                .max()
                .orElse(0.0);
    }

    @Override
    public List<PracticeRecord> getPracticeRecordsByTimeRange(Long studentId, LocalDateTime startDate,
            LocalDateTime endDate) {
        return practiceRecordRepository.findByStudentIdAndSubmittedAtBetween(studentId, startDate, endDate);
    }

    @Override
    public Map<String, Object> generatePracticeReport(
            Long studentId, Long classId, Long courseId,
            LocalDateTime startDate, LocalDateTime endDate) {

        Map<String, Object> report = new HashMap<>();

        if (studentId != null) {
            // 学生个人报告

            // 基本信息
            report.put("student_id", studentId);
            report.put("report_type", "student");
            report.put("period_start", startDate);
            report.put("period_end", endDate);

            // 练习次数
            List<PracticeRecord> periodRecords = getPracticeRecordsByTimeRange(studentId, startDate, endDate);
            report.put("practice_count", periodRecords.size());

            // 平均分
            double avgScore = periodRecords.stream()
                    .mapToDouble(PracticeRecord::getScore)
                    .average()
                    .orElse(0.0);
            report.put("average_score", avgScore);

            // 练习趋势
            report.put("trend", getStudentPracticeTrend(studentId));

            // 薄弱环节
            report.put("weak_questions", getStudentWeakestQuestions(studentId, 5));

        } else if (classId != null) {
            // 班级报告

            // 基本信息
            report.put("class_id", classId);
            report.put("report_type", "class");
            report.put("period_start", startDate);
            report.put("period_end", endDate);

            // 获取班级学生ID列表
            List<Long> studentIds = schoolClassService.getStudentIdsByClassId(classId);
            report.put("student_count", studentIds.size());

            // TODO: 班级报告其他统计数据
            // 班级平均分
            // 完成度统计
            // 薄弱环节

        } else if (courseId != null) {
            // 课程报告

            // 基本信息
            report.put("course_id", courseId);
            report.put("report_type", "course");

            // 作业统计
            report.put("homework_stats", getCourseHomeworkStats(courseId));

            // TODO: 课程报告其他统计数据
        }

        return report;
    }

    @Override
    public List<Map<String, Object>> getStudentKnowledgePointStats(Long studentId) {
        return practiceRecordRepository.findStudentKnowledgePointStats(studentId);
    }

    @Override
    public List<Map<String, Object>> getClassKnowledgePointStats(Long classId) {
        return practiceRecordRepository.findClassKnowledgePointStats(classId);
    }

    @Override
    public List<Map<String, Object>> getCourseKnowledgePointStats(Long courseId) {
        return practiceRecordRepository.findCourseKnowledgePointStats(courseId);
    }

    /**
     * 验证分数范围是否有效（0-100）
     */
    private void validateScore(Double score) {
        if (score == null || score < 0 || score > 100) {
            throw new IllegalArgumentException("分数必须在0-100之间");
        }
    }
}