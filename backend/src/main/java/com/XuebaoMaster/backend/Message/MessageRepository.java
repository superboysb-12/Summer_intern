package com.XuebaoMaster.backend.Message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

        // Find messages by target type (e.g., ALL, ADMINS, TEACHERS, STUDENTS)
        List<Message> findByTargetTypeOrderByCreatedAtDesc(Message.MessageTargetType targetType);

        // Find active messages (not expired and active flag is true)
        @Query("SELECT m FROM Message m WHERE m.active = true AND (m.expiresAt IS NULL OR m.expiresAt > :currentTime) ORDER BY m.createdAt DESC")
        List<Message> findActiveMessages(@Param("currentTime") LocalDateTime currentTime);

        // Find messages for a specific user based on role and specific targeting
        @Query("SELECT m FROM Message m WHERE m.active = true AND " +
                        "(m.expiresAt IS NULL OR m.expiresAt > :currentTime) AND " +
                        "(m.targetType = :targetType OR m.targetType = 'ALL' OR " +
                        "(m.targetType = 'SPECIFIC' AND m.targetIds LIKE CONCAT('%', :userId, '%'))) " +
                        "ORDER BY m.createdAt DESC")
        List<Message> findMessagesForUser(@Param("currentTime") LocalDateTime currentTime,
                        @Param("targetType") Message.MessageTargetType targetType,
                        @Param("userId") String userId);

        // Find active messages for a specific target type and target ID (for CLASS or
        // COURSE)
        @Query("SELECT m FROM Message m WHERE m.active = true AND " +
                        "(m.expiresAt IS NULL OR m.expiresAt > :currentTime) AND " +
                        "m.targetType = :targetType AND m.targetIds LIKE CONCAT('%', :targetId, '%') " +
                        "ORDER BY m.createdAt DESC")
        List<Message> findActiveMessagesByTargetType(@Param("currentTime") LocalDateTime currentTime,
                        @Param("targetType") Message.MessageTargetType targetType,
                        @Param("targetId") String targetId);

        // Search messages by title or content
        List<Message> findByTitleContainingOrContentContainingOrderByCreatedAtDesc(String title, String content);
}