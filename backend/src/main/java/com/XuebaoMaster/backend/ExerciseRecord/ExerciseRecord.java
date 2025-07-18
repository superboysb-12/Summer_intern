package com.XuebaoMaster.backend.ExerciseRecord;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "exercise_records")
@Data
public class ExerciseRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    // 作业ID，可为空，表示这是一道独立练习题而非作业题
    @Column(name = "homework_id")
    private Long homeworkId;

    // 对应的问题类型：选择题、填空题、问答题、编程题
    @Column(name = "question_type", nullable = false)
    private String questionType;

    // 学生的答案内容，对于编程题和问答题可能较长
    @Column(name = "answer_content", columnDefinition = "LONGTEXT")
    private String answerContent;

    // 分数（百分制）
    @Column(name = "score")
    private Integer score;

    // AI反馈内容
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    // 原始评分JSON数据（存储AI评分的详细信息）
    @Column(name = "grading_data", columnDefinition = "LONGTEXT")
    private String gradingData;

    // 提交时间
    @Column(name = "submission_time", nullable = false)
    private LocalDateTime submissionTime;

    // 状态：SUBMITTED（已提交）, GRADED（已评分）
    @Column(length = 20)
    private String status = "SUBMITTED";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 所属课程ID
    @Column(name = "course_id")
    private Long courseId;

    // 所属班级ID
    @Column(name = "class_id")
    private Long classId;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.submissionTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}