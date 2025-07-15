package com.XuebaoMaster.backend.StudentCourse;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_courses")
@Data
public class StudentCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    // 学习进度（百分比）
    @Column(name = "progress")
    private Integer progress = 0;

    // 状态：enrolled(已选课), completed(已完成), withdrawn(已退课)
    @Column(length = 20)
    private String status = "enrolled";

    @Column(nullable = false)
    private LocalDateTime enrollDate;

    @Column
    private LocalDateTime lastAccessDate;

    @PrePersist
    protected void onCreate() {
        this.enrollDate = LocalDateTime.now();
        this.lastAccessDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastAccessDate = LocalDateTime.now();
    }
}