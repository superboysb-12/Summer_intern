package com.XuebaoMaster.backend.StudyDuration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface StudyDurationService {
    StudyDuration createStudyDuration(StudyDuration studyDuration);

    StudyDuration createStudyDuration(LocalDateTime lessonStartTimeStamp, Integer length);

    StudyDuration createStudyDuration(LocalDateTime lessonStartTimeStamp, Integer length, Long userId, Long courseId);

    StudyDuration updateStudyDuration(StudyDuration studyDuration);

    void deleteStudyDuration(Long id);

    StudyDuration getStudyDurationById(Long id);

    List<StudyDuration> getStudyDurationsByCurrentTimeStampBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<StudyDuration> getStudyDurationsByLessonStartTimeStampBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<StudyDuration> getStudyDurationsByUserId(Long userId);

    List<StudyDuration> getStudyDurationsByCourseId(Long courseId);

    List<StudyDuration> getStudyDurationsByClassId(Long classId);

    List<StudyDuration> getStudyDurationsByClassIdAndCourseId(Long classId, Long courseId);

    List<StudyDuration> getStudyDurationsByLengthGreaterThanEqual(Integer minLength);

    List<StudyDuration> getStudyDurationsByLengthLessThanEqual(Integer maxLength);

    List<StudyDuration> getStudyDurationsByLengthBetween(Integer minLength, Integer maxLength);

    List<StudyDuration> getAllStudyDurations();

    Map<String, Object> getStudyStatistics();

    Map<String, Object> getStudyStatisticsBetween(LocalDateTime startTime, LocalDateTime endTime);
<<<<<<< HEAD
    
    // 按用户ID查询
    List<StudyDuration> getStudyDurationsByUserId(Long userId);
    
    // 按用户名查询
    List<StudyDuration> getStudyDurationsByUserName(String userName);
    
    // 按课程名查询
    List<StudyDuration> getStudyDurationsByCourseName(String courseName);
    
    // 按用户ID和课程名查询
    List<StudyDuration> getStudyDurationsByUserIdAndCourseName(Long userId, String courseName);
    
    // 获取特定用户的学习统计
    Map<String, Object> getStudyStatisticsByUserId(Long userId);
    
    // 获取特定课程的学习统计
    Map<String, Object> getStudyStatisticsByCourseName(String courseName);
} 
=======

    Map<String, Object> getUserStudyStatistics(Long userId);

    Map<String, Object> getCourseStudyStatistics(Long courseId);

    Map<String, Object> getClassStudyStatistics(Long classId);

    Map<String, Object> getClassCourseStudyStatistics(Long classId, Long courseId);
}
>>>>>>> 2fc5eadc03377134f52c7bfb671dfcce6baa7fe0
