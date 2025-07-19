package com.XuebaoMaster.backend.PracticeRecord;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 练习记录表
 * 记录学生的答题情况和得分
 */
@Entity
@Table(name = "practice_records")
@Data
public class PracticeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 学生ID
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    // 作业ID
    @Column(name = "homework_id", nullable = false)
    private Long homeworkId;

    // 题目ID
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    // 分数（百分制）
    @Column(name = "score", nullable = false)
    private Double score;

    // 答案数据（JSON格式）
    @Column(name = "answer_data", columnDefinition = "TEXT")
    private String answerData;

    // 提交时间
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    // 尝试次数
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 1;

    // 用时（秒）
    @Column(name = "time_spent")
    private Integer timeSpent;

    // 正确性（是/否）
    @Column(name = "is_correct")
    private Boolean isCorrect;

    // 创建和更新时间
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}