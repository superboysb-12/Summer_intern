package com.XuebaoMaster.backend.Homework;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HomeworkRepository extends JpaRepository<Homework, Long> {
    // Find homeworks by course ID
    List<Homework> findByCourseId(Long courseId);

    // Find homeworks by status
    List<Homework> findByStatus(String status);

    // Find homeworks by course ID and status
    List<Homework> findByCourseIdAndStatus(Long courseId, String status);
}