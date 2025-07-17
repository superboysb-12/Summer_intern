package com.XuebaoMaster.backend.PrivateMessage;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import com.XuebaoMaster.backend.User.User;

@Entity
@Table(name = "private_messages")
@Data
public class PrivateMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(nullable = true)
    private LocalDateTime readAt;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false)
    private boolean senderDeleted = false;

    @Column(nullable = false)
    private boolean recipientDeleted = false;

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
    }
}