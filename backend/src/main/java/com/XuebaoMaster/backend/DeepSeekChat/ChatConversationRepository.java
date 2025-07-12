package com.XuebaoMaster.backend.DeepSeekChat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天会话数据访问接口
 */
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    /**
     * 查询用户的所有会话
     * 
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatConversation> findByUserId(Long userId);

    /**
     * 查询用户在指定时间范围内的会话
     * 
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 会话列表
     */
    @Query("SELECT c FROM ChatConversation c WHERE c.userId = :userId AND c.createdAt BETWEEN :startTime AND :endTime ORDER BY c.updatedAt DESC")
    List<ChatConversation> findByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询用户最近更新的会话
     * 
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 会话列表
     */
    @Query("SELECT c FROM ChatConversation c WHERE c.userId = :userId ORDER BY c.updatedAt DESC")
    List<ChatConversation> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}