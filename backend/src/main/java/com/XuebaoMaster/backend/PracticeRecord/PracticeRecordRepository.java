package com.XuebaoMaster.backend.PracticeRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PracticeRecordRepository extends JpaRepository<PracticeRecord, Long> {
    @Query("SELECT p FROM PracticeRecord p WHERE p.student.id = :studentId")
    List<PracticeRecord> findByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT p FROM PracticeRecord p WHERE p.question.id = :questionId")
    List<PracticeRecord> findByQuestionId(@Param("questionId") Long questionId);
    
    @Query("SELECT p FROM PracticeRecord p WHERE p.student.id = :studentId AND p.question.id = :questionId")
    List<PracticeRecord> findByStudentIdAndQuestionId(@Param("studentId") Long studentId, @Param("questionId") Long questionId);
    
    List<PracticeRecord> findByIsRight(Boolean isRight);
    
    @Query("SELECT p FROM PracticeRecord p WHERE p.student.id = :studentId AND p.isRight = :isRight")
    List<PracticeRecord> findByStudentIdAndIsRight(@Param("studentId") Long studentId, @Param("isRight") Boolean isRight);
    
    @Query("SELECT p FROM PracticeRecord p WHERE p.question.id = :questionId AND p.isRight = :isRight")
    List<PracticeRecord> findByQuestionIdAndIsRight(@Param("questionId") Long questionId, @Param("isRight") Boolean isRight);
    
    @Query("SELECT COUNT(p) FROM PracticeRecord p WHERE p.student.id = :studentId")
    Long countByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT COUNT(p) FROM PracticeRecord p WHERE p.student.id = :studentId AND p.isRight = true")
    Long countCorrectByStudentId(@Param("studentId") Long studentId);
} 