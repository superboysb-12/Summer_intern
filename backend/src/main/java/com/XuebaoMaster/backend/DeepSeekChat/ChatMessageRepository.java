package com.XuebaoMaster.backend.DeepSeekChat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息数据访问接口
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    /**
     * 根据用户ID查询聊天消息
     * 
     * @param userId 用户ID
     * @return 聊天消息列表
     */
    @Query("SELECT c FROM ChatMessageEntity c WHERE c.userId = :userId")
    List<ChatMessageEntity> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和时间范围查询聊天消息
     * 
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 聊天消息列表
     */
    @Query("SELECT c FROM ChatMessageEntity c WHERE c.userId = :userId AND c.createdAt BETWEEN :startTime AND :endTime")
    List<ChatMessageEntity> findByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定时间范围内的所有聊天消息
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 聊天消息列表
     */
    @Query("SELECT c FROM ChatMessageEntity c WHERE c.createdAt BETWEEN :startTime AND :endTime")
    List<ChatMessageEntity> findByCreatedAtBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}