package com.XuebaoMaster.backend.StudyDuration;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "study_durations")
@Data
public class StudyDuration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime currentTimeStamp;

    @Column(nullable = false)
    private LocalDateTime lessonStartTimeStamp;

    @Column(nullable = false)
    private Integer length; // 学习时长（单位：分钟）
} 