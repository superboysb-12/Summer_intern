package com.XuebaoMaster.backend.StudentEmotion;
import com.XuebaoMaster.backend.User.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
@Entity
@Table(name = "student_emotions")
@Data
public class StudentEmotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false)
    private Integer mark; 
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
} 
