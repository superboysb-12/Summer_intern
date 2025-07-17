package com.XuebaoMaster.backend.PrivateMessage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {

    // Find messages where the user is either sender or recipient
    @Query("SELECT pm FROM PrivateMessage pm " +
            "WHERE (pm.sender.id = :userId AND pm.senderDeleted = false) " +
            "OR (pm.recipient.id = :userId AND pm.recipientDeleted = false) " +
            "ORDER BY pm.sentAt DESC")
    List<PrivateMessage> findMessagesByUserId(@Param("userId") Long userId);

    // Find inbox messages (received) for a user
    @Query("SELECT pm FROM PrivateMessage pm " +
            "WHERE pm.recipient.id = :userId AND pm.recipientDeleted = false " +
            "ORDER BY pm.sentAt DESC")
    List<PrivateMessage> findInboxMessagesByUserId(@Param("userId") Long userId);

    // Find outbox messages (sent) for a user
    @Query("SELECT pm FROM PrivateMessage pm " +
            "WHERE pm.sender.id = :userId AND pm.senderDeleted = false " +
            "ORDER BY pm.sentAt DESC")
    List<PrivateMessage> findOutboxMessagesByUserId(@Param("userId") Long userId);

    // Find unread messages for a user
    @Query("SELECT pm FROM PrivateMessage pm " +
            "WHERE pm.recipient.id = :userId AND pm.read = false AND pm.recipientDeleted = false " +
            "ORDER BY pm.sentAt DESC")
    List<PrivateMessage> findUnreadMessagesByUserId(@Param("userId") Long userId);

    // Count unread messages for a user
    @Query("SELECT COUNT(pm) FROM PrivateMessage pm " +
            "WHERE pm.recipient.id = :userId AND pm.read = false AND pm.recipientDeleted = false")
    Long countUnreadMessagesByUserId(@Param("userId") Long userId);
}