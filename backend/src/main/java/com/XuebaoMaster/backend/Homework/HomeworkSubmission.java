package com.XuebaoMaster.backend.Homework;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "homework_submissions")
@Data
public class HomeworkSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "homework_id", nullable = false)
    private Long homeworkId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    // 可能为空，如果是文件提交则内容可能在文件中
    @Column(columnDefinition = "TEXT")
    private String content;

    // 可能为空，如果是纯文本提交则没有文件
    @Column(name = "file_id")
    private Long fileId;

    // 提交时间
    @Column(name = "submission_date", nullable = false)
    private LocalDateTime submissionDate;

    // 状态：SUBMITTED（已提交）, GRADED（已评分）, RETURNED（已退回）
    @Column(length = 20)
    private String status = "SUBMITTED";

    // 分数
    @Column
    private Integer score;

    // 教师反馈
    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 是否迟交
    @Column(name = "is_late")
    private Boolean isLate = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.submissionDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}