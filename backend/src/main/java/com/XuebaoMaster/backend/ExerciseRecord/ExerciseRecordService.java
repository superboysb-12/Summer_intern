package com.XuebaoMaster.backend.ExerciseRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ExerciseRecordService {

    /**
     * 保存学生的练习记录
     * 
     * @param exerciseRecord 练习记录实体
     * @return 保存后的练习记录
     */
    ExerciseRecord saveExerciseRecord(ExerciseRecord exerciseRecord);

    /**
     * 提交学生的练习答案
     * 
     * @param studentId     学生ID
     * @param questionId    问题ID
     * @param homeworkId    作业ID（可选）
     * @param answerContent 答案内容
     * @return 保存的练习记录
     */
    ExerciseRecord submitAnswer(Long studentId, Long questionId, Long homeworkId, String answerContent);

    /**
     * 使用AI对答案进行评分
     * 
     * @param recordId 练习记录ID
     * @return 评分后的练习记录
     */
    ExerciseRecord gradeExercise(Long recordId);

    /**
     * 获取特定练习记录
     * 
     * @param id 练习记录ID
     * @return 练习记录（如果存在）
     */
    Optional<ExerciseRecord> getExerciseRecordById(Long id);

    /**
     * 获取学生的所有练习记录
     * 
     * @param studentId 学生ID
     * @return 练习记录列表
     */
    List<ExerciseRecord> getExerciseRecordsByStudentId(Long studentId);

    /**
     * 获取特定作业的所有练习记录
     * 
     * @param homeworkId 作业ID
     * @return 练习记录列表
     */
    List<ExerciseRecord> getExerciseRecordsByHomeworkId(Long homeworkId);

    /**
     * 获取特定问题的所有练习记录
     * 
     * @param questionId 问题ID
     * @return 练习记录列表
     */
    List<ExerciseRecord> getExerciseRecordsByQuestionId(Long questionId);

    /**
     * 获取学生在特定课程的练习统计数据
     * 
     * @param studentId 学生ID
     * @param courseId  课程ID
     * @return 统计数据
     */
    Map<String, Object> getStudentCourseStatistics(Long studentId, Long courseId);

    /**
     * 获取班级在特定作业的练习统计数据
     * 
     * @param classId    班级ID
     * @param homeworkId 作业ID
     * @return 统计数据
     */
    Map<String, Object> getClassHomeworkStatistics(Long classId, Long homeworkId);

    /**
     * 获取课程的练习统计数据
     * 
     * @param courseId 课程ID
     * @return 统计数据
     */
    Map<String, Object> getCourseStatistics(Long courseId);
}