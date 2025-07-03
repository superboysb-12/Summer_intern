package com.XuebaoMaster.backend.StudyDuration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface StudyDurationRepository extends JpaRepository<StudyDuration, Long> {
        List<StudyDuration> findByCurrentTimeStampBetween(LocalDateTime startTime, LocalDateTime endTime);

        List<StudyDuration> findByLessonStartTimeStampBetween(LocalDateTime startTime, LocalDateTime endTime);

        List<StudyDuration> findByUser_Id(Long userId);

        List<StudyDuration> findByCourse_CourseId(Long courseId);

        List<StudyDuration> findByUser_SchoolClass_Id(Long classId);

        List<StudyDuration> findByUser_SchoolClass_IdAndCourse_CourseId(Long classId, Long courseId);

        @Query("SELECT s FROM StudyDuration s WHERE s.length >= :minLength")
        List<StudyDuration> findByLengthGreaterThanEqual(@Param("minLength") Integer minLength);

        @Query("SELECT s FROM StudyDuration s WHERE s.length <= :maxLength")
        List<StudyDuration> findByLengthLessThanEqual(@Param("maxLength") Integer maxLength);

        @Query("SELECT s FROM StudyDuration s WHERE s.length BETWEEN :minLength AND :maxLength")
        List<StudyDuration> findByLengthBetween(@Param("minLength") Integer minLength,
                        @Param("maxLength") Integer maxLength);

        @Query("SELECT SUM(s.length) FROM StudyDuration s")
        Integer getTotalStudyDuration();

        @Query("SELECT SUM(s.length) FROM StudyDuration s WHERE s.currentTimeStamp BETWEEN :startTime AND :endTime")
        Integer getTotalStudyDurationBetween(@Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        @Query("SELECT AVG(s.length) FROM StudyDuration s")
        Double getAverageStudyDuration();

        @Query("SELECT SUM(s.length) FROM StudyDuration s WHERE s.user.id = :userId")
        Integer getTotalStudyDurationByUserId(@Param("userId") Long userId);

        @Query("SELECT SUM(s.length) FROM StudyDuration s WHERE s.course.courseId = :courseId")
        Integer getTotalStudyDurationByCourseId(@Param("courseId") Long courseId);

        @Query("SELECT SUM(s.length) FROM StudyDuration s WHERE s.user.schoolClass.id = :classId")
        Integer getTotalStudyDurationByClassId(@Param("classId") Long classId);

        @Query("SELECT SUM(s.length) FROM StudyDuration s WHERE s.user.schoolClass.id = :classId AND s.course.courseId = :courseId")
        Integer getTotalStudyDurationByClassIdAndCourseId(@Param("classId") Long classId,
                        @Param("courseId") Long courseId);

        @Query("SELECT COUNT(DISTINCT s.user.id) FROM StudyDuration s WHERE s.user.schoolClass.id = :classId")
        Integer getStudentCountByClassId(@Param("classId") Long classId);

        @Query("SELECT COUNT(DISTINCT s.user.id) FROM StudyDuration s WHERE s.user.schoolClass.id = :classId AND s.course.courseId = :courseId")
        Integer getStudentCountByClassIdAndCourseId(@Param("classId") Long classId, @Param("courseId") Long courseId);
}