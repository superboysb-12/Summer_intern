package com.XuebaoMaster.backend.Homework;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "homeworks")
@Data
public class Homework {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    // Optional due date
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    // Status: DRAFT, PUBLISHED, CLOSED
    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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