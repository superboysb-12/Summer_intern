package com.XuebaoMaster.backend.Homework;

import java.util.List;

public interface HomeworkService {
    // CRUD operations
    Homework createHomework(Homework homework);

    Homework updateHomework(Homework homework);

    void deleteHomework(Long homeworkId);

    Homework getHomeworkById(Long homeworkId);

    List<Homework> getAllHomeworks();

    // Course-specific operations
    List<Homework> getHomeworksByCourse(Long courseId);

    // Status operations
    List<Homework> getHomeworksByStatus(String status);

    Homework updateHomeworkStatus(Long homeworkId, String status);

    // Course and status
    List<Homework> getHomeworksByCourseAndStatus(Long courseId, String status);
}