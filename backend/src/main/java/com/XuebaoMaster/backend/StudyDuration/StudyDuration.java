package com.XuebaoMaster.backend.StudyDuration;

import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.Course.Course;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.Course.Course;


@Entity
@Table(name = "study_durations")
@Data
public class StudyDuration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime currentTimeStamp;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDateTime lessonStartTimeStamp;

    @Column(nullable = false)
<<<<<<< HEAD
    private Integer length; // 学习时长（单位：分钟）

    @PrePersist
    protected void onCreate() {
        this.currentTimeStamp = LocalDateTime.now();
    }
} 
=======
    private Integer length;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
}
>>>>>>> 2fc5eadc03377134f52c7bfb671dfcce6baa7fe0
