package com.XuebaoMaster.backend.PrivateMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.User.UserService;
import com.XuebaoMaster.backend.util.JwtUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/private-messages")
public class PrivateMessageController {

    @Autowired
    private PrivateMessageService privateMessageService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // DTO for sending a new message
    static class SendMessageRequest {
        private Long recipientId;
        private String subject;
        private String content;

        public Long getRecipientId() {
            return recipientId;
        }

        public void setRecipientId(Long recipientId) {
            this.recipientId = recipientId;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    // Send a new message
    @PostMapping
    public ResponseEntity<PrivateMessage> sendMessage(@RequestBody SendMessageRequest request) {
        // Get the authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);

        PrivateMessage message = privateMessageService.sendMessage(
                currentUser.getId(),
                request.getRecipientId(),
                request.getSubject(),
                request.getContent());

        return ResponseEntity.ok(message);
    }

    // Get all messages for the current user
    @GetMapping
    public ResponseEntity<List<PrivateMessage>> getAllMessages() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);

        return ResponseEntity.ok(privateMessageService.getAllMessagesForUser(currentUser.getId()));
    }

    // Get inbox messages for the current user
    @GetMapping("/inbox")
    public ResponseEntity<List<PrivateMessage>> getInboxMessages() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);

        return ResponseEntity.ok(privateMessageService.getInboxMessagesForUser(currentUser.getId()));
    }

    // Get outbox messages for the current user
    @GetMapping("/outbox")
    public ResponseEntity<List<PrivateMessage>> getOutboxMessages() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);

        return ResponseEntity.ok(privateMessageService.getOutboxMessagesForUser(currentUser.getId()));
    }

    // Get unread messages for the current user
    @GetMapping("/unread")
    public ResponseEntity<List<PrivateMessage>> getUnreadMessages() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);

        return ResponseEntity.ok(privateMessageService.getUnreadMessagesForUser(currentUser.getId()));
    }

    // Get unread message count for the current user
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadMessageCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);

        Long count = privateMessageService.getUnreadMessageCountForUser(currentUser.getId());

        Map<String, Long> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(response);
    }

    // Get a specific message by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getMessageById(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);

        return privateMessageService.getMessageById(id)
                .map(message -> {
                    // Check if the user is either the sender or recipient
                    if (!message.getSender().getId().equals(currentUser.getId()) &&
                            !message.getRecipient().getId().equals(currentUser.getId())) {
                        return ResponseEntity.status(403).build();
                    }
                    return ResponseEntity.ok(message);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Mark a message as read
    @PutMapping("/{id}/read")
    public ResponseEntity<PrivateMessage> markAsRead(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);

        try {
            PrivateMessage message = privateMessageService.markAsRead(id, currentUser.getId());
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    // Delete a message for the current user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);

        try {
            privateMessageService.deleteMessageForUser(id, currentUser.getId());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }
}