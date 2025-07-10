package com.XuebaoMaster.backend.LoginRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
public interface LoginRecordRepository extends JpaRepository<LoginRecord, Long> {
    @Query("SELECT l FROM LoginRecord l WHERE l.user.id = :userId")
    List<LoginRecord> findByUserId(@Param("userId") Long userId);
    @Query("SELECT l FROM LoginRecord l WHERE l.time BETWEEN :startTime AND :endTime")
    List<LoginRecord> findByTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    @Query("SELECT l FROM LoginRecord l WHERE l.user.id = :userId AND l.time BETWEEN :startTime AND :endTime")
    List<LoginRecord> findByUserIdAndTimeBetween(
            @Param("userId") Long userId, 
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    @Query("SELECT COUNT(l) FROM LoginRecord l WHERE l.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    @Query("SELECT COUNT(l) FROM LoginRecord l WHERE l.time BETWEEN :startTime AND :endTime")
    Long countByTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
} 
