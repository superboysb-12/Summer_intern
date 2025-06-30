package com.XuebaoMaster.backend.PracticeRecord;

import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.Question.Question;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "practice_records")
@Data
public class PracticeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stu_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private LocalDateTime finishTime;

    @Column(nullable = false)
    private Boolean isRight;
} 