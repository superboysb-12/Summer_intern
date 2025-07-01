package com.XuebaoMaster.backend.StudentEmotion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface StudentEmotionRepository extends JpaRepository<StudentEmotion, Long> {
    @Query("SELECT e FROM StudentEmotion e WHERE e.user.id = :userId")
    List<StudentEmotion> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT e FROM StudentEmotion e WHERE e.user.id = :userId AND e.createdAt BETWEEN :startTime AND :endTime")
    List<StudentEmotion> findByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT e FROM StudentEmotion e WHERE e.mark >= :minMark")
    List<StudentEmotion> findByMarkGreaterThanEqual(@Param("minMark") Integer minMark);
    
    @Query("SELECT e FROM StudentEmotion e WHERE e.mark <= :maxMark")
    List<StudentEmotion> findByMarkLessThanEqual(@Param("maxMark") Integer maxMark);
    
    @Query("SELECT e FROM StudentEmotion e WHERE e.mark BETWEEN :minMark AND :maxMark")
    List<StudentEmotion> findByMarkBetween(@Param("minMark") Integer minMark, @Param("maxMark") Integer maxMark);
    
    @Query("SELECT AVG(e.mark) FROM StudentEmotion e WHERE e.user.id = :userId")
    Double getAverageMarkByUserId(@Param("userId") Long userId);
    
    @Query("SELECT AVG(e.mark) FROM StudentEmotion e WHERE e.user.id = :userId AND e.createdAt BETWEEN :startTime AND :endTime")
    Double getAverageMarkByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT AVG(e.mark) FROM StudentEmotion e")
    Double getAverageMark();
} 