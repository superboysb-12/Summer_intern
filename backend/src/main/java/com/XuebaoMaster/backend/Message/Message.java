package com.XuebaoMaster.backend.Message;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "messages")
@Data
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    // Target audience for the message - can be specific to certain roles
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageTargetType targetType;

    @Column(nullable = true, length = 100)
    private String targetIds; // Comma-separated IDs if targeting specific entities

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Optional expiration date for the message
    @Column(nullable = true)
    private LocalDateTime expiresAt;

    // Whether the message is active/published
    @Column(nullable = false)
    private boolean active = true;

    public enum MessageTargetType {
        ALL, // All users
        ADMINS, // Only admins
        TEACHERS, // Only teachers
        STUDENTS, // Only students
        CLASS, // Students in specific class
        COURSE, // Students in specific course
        SPECIFIC // Specific users (referenced by targetIds)
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}