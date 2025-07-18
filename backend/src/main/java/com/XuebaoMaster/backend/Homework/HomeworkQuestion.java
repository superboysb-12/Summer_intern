package com.XuebaoMaster.backend.Homework;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

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

    // 题目在作业中的顺序号
    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    // 题目在作业总分中的分值权重
    @Column(name = "score_weight", nullable = false)
    private Integer scoreWeight = 1;

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