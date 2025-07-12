package com.XuebaoMaster.backend.DeepSeekChat;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天会话实体类
 * 每个会话包含多条消息，代表一次完整的对话
 */
@Entity
@Table(name = "chat_conversations")
@Data
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title")
    private String title;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("messageIndex ASC")
    @JsonManagedReference
    private List<ChatMessageEntity> messages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 添加消息到会话中
     * 
     * @param message 消息实体
     */
    public void addMessage(ChatMessageEntity message) {
        message.setConversation(this);
        message.setMessageIndex(this.messages.size());
        this.messages.add(message);
    }

    /**
     * 生成会话标题（基于第一条消息）
     */
    public void generateTitle() {
        if (messages != null && !messages.isEmpty()) {
            ChatMessageEntity firstMessage = messages.get(0);
            String content = firstMessage.getContent();
            if (content.length() > 50) {
                content = content.substring(0, 50) + "...";
            }
            this.title = content;
        } else {
            this.title = "新会话 " + LocalDateTime.now();
        }
    }
}