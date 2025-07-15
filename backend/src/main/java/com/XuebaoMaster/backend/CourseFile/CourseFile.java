package com.XuebaoMaster.backend.CourseFile;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.XuebaoMaster.backend.Course.Course;
import com.XuebaoMaster.backend.File.FileEntity;

@Entity
@Table(name = "course_files")
@Data
public class CourseFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @Column(length = 50)
    private String resourceType; // 资源类型：lecture(讲义), assignment(作业), reference(参考资料), other(其他)

    @Column(length = 255)
    private String description; // 资源描述

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean isVisible = true; // 是否对学生可见

    @Column(nullable = false)
    private int displayOrder = 0; // 显示顺序

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