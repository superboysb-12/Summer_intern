package com.XuebaoMaster.backend.StudyDuration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface StudyDurationRepository extends JpaRepository<StudyDuration, Long> {
    List<StudyDuration> findByCurrentTimeStampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    List<StudyDuration> findByLessonStartTimeStampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT s FROM StudyDuration s WHERE s.length >= :minLength")
    List<StudyDuration> findByLengthGreaterThanEqual(@Param("minLength") Integer minLength);
    
    @Query("SELECT s FROM StudyDuration s WHERE s.length <= :maxLength")
    List<StudyDuration> findByLengthLessThanEqual(@Param("maxLength") Integer maxLength);
    
    @Query("SELECT s FROM StudyDuration s WHERE s.length BETWEEN :minLength AND :maxLength")
    List<StudyDuration> findByLengthBetween(@Param("minLength") Integer minLength, @Param("maxLength") Integer maxLength);
    
    @Query("SELECT SUM(s.length) FROM StudyDuration s")
    Integer getTotalStudyDuration();
    
    @Query("SELECT SUM(s.length) FROM StudyDuration s WHERE s.currentTimeStamp BETWEEN :startTime AND :endTime")
    Integer getTotalStudyDurationBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT AVG(s.length) FROM StudyDuration s")
    Double getAverageStudyDuration();
    
    // 按用户ID查询
    List<StudyDuration> findByUserId(Long userId);
    
    // 按用户名查询
    @Query("SELECT s FROM StudyDuration s WHERE s.user.name = :userName")
    List<StudyDuration> findByUserName(@Param("userName") String userName);
    
    // 按课程名查询
    @Query("SELECT s FROM StudyDuration s WHERE s.course.name = :courseName")
    List<StudyDuration> findByCourseName(@Param("courseName") String courseName);
    
    // 按用户ID和课程名查询
    @Query("SELECT s FROM StudyDuration s WHERE s.user.id = :userId AND s.course.name = :courseName")
    List<StudyDuration> findByUserIdAndCourseName(@Param("userId") Long userId, @Param("courseName") String courseName);
    
    // 获取特定用户的总学习时长
    @Query("SELECT SUM(s.length) FROM StudyDuration s WHERE s.user.id = :userId")
    Integer getTotalStudyDurationByUserId(@Param("userId") Long userId);
    
    // 获取特定课程的总学习时长
    @Query("SELECT SUM(s.length) FROM StudyDuration s WHERE s.course.name = :courseName")
    Integer getTotalStudyDurationByCourseName(@Param("courseName") String courseName);
} 