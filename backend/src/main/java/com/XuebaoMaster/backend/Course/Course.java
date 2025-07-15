package com.XuebaoMaster.backend.Course;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "courses")
@Data
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 500)
    private String description;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 添加教师ID字段
    @Column(name = "teacher_id")
    private Long teacherId;

    // 添加课程状态字段
    @Column(length = 20)
    private String status = "active";

    // 临时字段：学习进度（百分比，不存入数据库）
    @Transient
    private Integer progress = 0;

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
