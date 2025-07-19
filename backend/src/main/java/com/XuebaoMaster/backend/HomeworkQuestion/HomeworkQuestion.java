package com.XuebaoMaster.backend.HomeworkQuestion;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 作业题目关联表
 * 实现一个作业包含多个题目的关联关系
 */
@Entity
@Table(name = "homework_questions")
@Data
public class HomeworkQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "homework_id", nullable = false)
    private Long homeworkId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    // 排序索引，用于题目的排序展示
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    // 题目分值权重，默认为1
    @Column(name = "weight", nullable = false)
    private Double weight = 1.0;

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