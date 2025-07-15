package com.XuebaoMaster.backend.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.User.UserService;
import com.XuebaoMaster.backend.util.JwtUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // Create a new message (admin only)
    @PostMapping
    public ResponseEntity<Message> createMessage(@RequestBody Message message) {
        return ResponseEntity.ok(messageService.createMessage(message));
    }

    // Get all messages (admin only)
    @GetMapping
    public ResponseEntity<List<Message>> getAllMessages() {
        return ResponseEntity.ok(messageService.getAllMessages());
    }

    // Get message by ID
    @GetMapping("/{id}")
    public ResponseEntity<Message> getMessageById(@PathVariable Long id) {
        return messageService.getMessageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update message (admin only)
    @PutMapping("/{id}")
    public ResponseEntity<Message> updateMessage(@PathVariable Long id, @RequestBody Message message) {
        message.setId(id);
        return ResponseEntity.ok(messageService.updateMessage(message));
    }

    // Delete message (admin only)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id);
        return ResponseEntity.ok().build();
    }

    // Search messages by keyword
    @GetMapping("/search")
    public ResponseEntity<List<Message>> searchMessages(@RequestParam String keyword) {
        return ResponseEntity.ok(messageService.searchMessages(keyword));
    }

    // Toggle message active status
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<Message> toggleMessageStatus(@PathVariable Long id) {
        return ResponseEntity.ok(messageService.toggleMessageStatus(id));
    }

    // Get messages for current user
    @GetMapping("/my-messages")
    public ResponseEntity<List<Message>> getMessagesForCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        String username = jwtUtil.extractUsername(token);
        User user = userService.getUserByUsername(username);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Message> messages = messageService.getMessagesForUser(user);
        return ResponseEntity.ok(messages);
    }

    // Get count of unread messages for current user
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getMessageCount(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        String username = jwtUtil.extractUsername(token);
        User user = userService.getUserByUsername(username);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Message> messages = messageService.getMessagesForUser(user);

        Map<String, Object> response = new HashMap<>();
        response.put("count", messages.size());

        return ResponseEntity.ok(response);
    }
}