package com.XuebaoMaster.backend.TeacherStudentInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
public interface TeacherStudentInteractionRepository extends JpaRepository<TeacherStudentInteraction, Long> {
    @Query("SELECT i FROM TeacherStudentInteraction i WHERE i.teacher.id = :teacherId")
    List<TeacherStudentInteraction> findByTeacherId(@Param("teacherId") Long teacherId);
    @Query("SELECT i FROM TeacherStudentInteraction i WHERE i.student.id = :studentId")
    List<TeacherStudentInteraction> findByStudentId(@Param("studentId") Long studentId);
    @Query("SELECT i FROM TeacherStudentInteraction i WHERE i.teacher.id = :teacherId AND i.student.id = :studentId")
    List<TeacherStudentInteraction> findByTeacherIdAndStudentId(
            @Param("teacherId") Long teacherId, 
            @Param("studentId") Long studentId);
    @Query("SELECT i FROM TeacherStudentInteraction i WHERE i.createdAt BETWEEN :startTime AND :endTime")
    List<TeacherStudentInteraction> findByCreatedAtBetween(
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    @Query("SELECT i FROM TeacherStudentInteraction i WHERE i.teacher.id = :teacherId AND i.createdAt BETWEEN :startTime AND :endTime")
    List<TeacherStudentInteraction> findByTeacherIdAndCreatedAtBetween(
            @Param("teacherId") Long teacherId,
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    @Query("SELECT i FROM TeacherStudentInteraction i WHERE i.student.id = :studentId AND i.createdAt BETWEEN :startTime AND :endTime")
    List<TeacherStudentInteraction> findByStudentIdAndCreatedAtBetween(
            @Param("studentId") Long studentId,
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    @Query("SELECT i FROM TeacherStudentInteraction i WHERE i.teacher.id = :teacherId AND i.student.id = :studentId AND i.createdAt BETWEEN :startTime AND :endTime")
    List<TeacherStudentInteraction> findByTeacherIdAndStudentIdAndCreatedAtBetween(
            @Param("teacherId") Long teacherId,
            @Param("studentId") Long studentId,
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    @Query("SELECT COUNT(i) FROM TeacherStudentInteraction i WHERE i.teacher.id = :teacherId")
    Long countByTeacherId(@Param("teacherId") Long teacherId);
    @Query("SELECT COUNT(i) FROM TeacherStudentInteraction i WHERE i.student.id = :studentId")
    Long countByStudentId(@Param("studentId") Long studentId);
    @Query("SELECT COUNT(i) FROM TeacherStudentInteraction i WHERE i.teacher.id = :teacherId AND i.student.id = :studentId")
    Long countByTeacherIdAndStudentId(
            @Param("teacherId") Long teacherId, 
            @Param("studentId") Long studentId);
} 
