package com.XuebaoMaster.backend.PrivateMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.User.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PrivateMessageService {

    @Autowired
    private PrivateMessageRepository privateMessageRepository;

    @Autowired
    private UserService userService;

    // Create a new message
    @Transactional
    public PrivateMessage sendMessage(Long senderId, Long recipientId, String subject, String content) {
        User sender = userService.getUserById(senderId);
        User recipient = userService.getUserById(recipientId);

        PrivateMessage message = new PrivateMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setSubject(subject);
        message.setContent(content);

        return privateMessageRepository.save(message);
    }

    // Get a message by ID
    public Optional<PrivateMessage> getMessageById(Long id) {
        return privateMessageRepository.findById(id);
    }

    // Mark a message as read
    @Transactional
    public PrivateMessage markAsRead(Long messageId, Long userId) {
        PrivateMessage message = privateMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Only the recipient can mark the message as read
        if (!message.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("User not authorized to mark this message as read");
        }

        message.setRead(true);
        message.setReadAt(LocalDateTime.now());

        return privateMessageRepository.save(message);
    }

    // Delete a message for a user (soft delete)
    @Transactional
    public void deleteMessageForUser(Long messageId, Long userId) {
        PrivateMessage message = privateMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (message.getSender().getId().equals(userId)) {
            message.setSenderDeleted(true);
        } else if (message.getRecipient().getId().equals(userId)) {
            message.setRecipientDeleted(true);
        } else {
            throw new RuntimeException("User not authorized to delete this message");
        }

        // If both sender and recipient have deleted, mark as fully deleted
        if (message.isSenderDeleted() && message.isRecipientDeleted()) {
            message.setDeleted(true);
        }

        privateMessageRepository.save(message);
    }

    // Get all messages for a user (both sent and received)
    public List<PrivateMessage> getAllMessagesForUser(Long userId) {
        return privateMessageRepository.findMessagesByUserId(userId);
    }

    // Get inbox messages for a user
    public List<PrivateMessage> getInboxMessagesForUser(Long userId) {
        return privateMessageRepository.findInboxMessagesByUserId(userId);
    }

    // Get outbox messages for a user
    public List<PrivateMessage> getOutboxMessagesForUser(Long userId) {
        return privateMessageRepository.findOutboxMessagesByUserId(userId);
    }

    // Get unread messages for a user
    public List<PrivateMessage> getUnreadMessagesForUser(Long userId) {
        return privateMessageRepository.findUnreadMessagesByUserId(userId);
    }

    // Get count of unread messages for a user
    public Long getUnreadMessageCountForUser(Long userId) {
        return privateMessageRepository.countUnreadMessagesByUserId(userId);
    }
}