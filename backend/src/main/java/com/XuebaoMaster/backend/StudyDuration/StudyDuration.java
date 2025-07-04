package com.XuebaoMaster.backend.StudyDuration;

import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.Course.Course;
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDateTime lessonStartTimeStamp;

    @Column(nullable = false)
    private Integer length;
}
