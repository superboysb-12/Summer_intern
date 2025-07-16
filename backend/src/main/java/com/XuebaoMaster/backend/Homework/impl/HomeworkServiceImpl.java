package com.XuebaoMaster.backend.Homework.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.Homework.Homework;
import com.XuebaoMaster.backend.Homework.HomeworkRepository;
import com.XuebaoMaster.backend.Homework.HomeworkService;
import com.XuebaoMaster.backend.Message.Message;
import com.XuebaoMaster.backend.Message.MessageService;
import com.XuebaoMaster.backend.Course.CourseService;
import com.XuebaoMaster.backend.Course.Course;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class HomeworkServiceImpl implements HomeworkService {
    @Autowired
    private HomeworkRepository homeworkRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private CourseService courseService;

    @Override
    public Homework createHomework(Homework homework) {
        // Set default values if not provided
        if (homework.getStatus() == null) {
            homework.setStatus("PUBLISHED");
        }

        // Save the homework
        Homework savedHomework = homeworkRepository.save(homework);

        // Send notification to students in the course
        sendHomeworkNotification(savedHomework, "CREATED");

        return savedHomework;
    }

    @Override
    public Homework updateHomework(Homework homework) {
        // Check if homework exists
        Homework existingHomework = homeworkRepository.findById(homework.getId())
                .orElseThrow(() -> new RuntimeException("Homework not found"));

        // Update fields
        existingHomework.setTitle(homework.getTitle());
        existingHomework.setDescription(homework.getDescription());
        existingHomework.setDueDate(homework.getDueDate());

        // Only update status if provided
        if (homework.getStatus() != null) {
            existingHomework.setStatus(homework.getStatus());
        }

        // Save updated homework
        Homework updatedHomework = homeworkRepository.save(existingHomework);

        // Send notification to students in the course
        sendHomeworkNotification(updatedHomework, "UPDATED");

        return updatedHomework;
    }

    @Override
    public void deleteHomework(Long homeworkId) {
        // Check if homework exists
        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new RuntimeException("Homework not found"));

        // Delete the homework
        homeworkRepository.deleteById(homeworkId);

        // Send notification to students in the course
        sendHomeworkNotification(homework, "DELETED");
    }

    @Override
    public Homework getHomeworkById(Long homeworkId) {
        return homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new RuntimeException("Homework not found"));
    }

    @Override
    public List<Homework> getAllHomeworks() {
        return homeworkRepository.findAll();
    }

    @Override
    public List<Homework> getHomeworksByCourse(Long courseId) {
        return homeworkRepository.findByCourseId(courseId);
    }

    @Override
    public List<Homework> getHomeworksByStatus(String status) {
        return homeworkRepository.findByStatus(status);
    }

    @Override
    public Homework updateHomeworkStatus(Long homeworkId, String status) {
        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new RuntimeException("Homework not found"));

        homework.setStatus(status);
        Homework updatedHomework = homeworkRepository.save(homework);

        // Send notification to students in the course
        sendHomeworkNotification(updatedHomework, "STATUS_CHANGED");

        return updatedHomework;
    }

    @Override
    public List<Homework> getHomeworksByCourseAndStatus(Long courseId, String status) {
        return homeworkRepository.findByCourseIdAndStatus(courseId, status);
    }

    /**
     * Helper method to send homework notifications to students in a course
     * 
     * @param homework The homework entity
     * @param action   The action performed (CREATED, UPDATED, DELETED,
     *                 STATUS_CHANGED)
     */
    private void sendHomeworkNotification(Homework homework, String action) {
        try {
            // Get the course details
            Course course = courseService.getCourseById(homework.getCourseId());

            // Create message content based on action
            String title = "";
            String content = "";

            switch (action) {
                case "CREATED":
                    title = "新作业通知: " + homework.getTitle();
                    content = String.format("课程 '%s' 新增作业: %s\n\n描述: %s\n\n截止日期: %s",
                            course.getName(), homework.getTitle(), homework.getDescription(),
                            homework.getDueDate() != null ? homework.getDueDate().toString() : "无");
                    break;
                case "UPDATED":
                    title = "作业更新通知: " + homework.getTitle();
                    content = String.format("课程 '%s' 的作业 '%s' 已更新\n\n描述: %s\n\n截止日期: %s",
                            course.getName(), homework.getTitle(), homework.getDescription(),
                            homework.getDueDate() != null ? homework.getDueDate().toString() : "无");
                    break;
                case "DELETED":
                    title = "作业取消通知: " + homework.getTitle();
                    content = String.format("课程 '%s' 的作业 '%s' 已取消",
                            course.getName(), homework.getTitle());
                    break;
                case "STATUS_CHANGED":
                    title = "作业状态更新: " + homework.getTitle();
                    content = String.format("课程 '%s' 的作业 '%s' 状态已更新为: %s",
                            course.getName(), homework.getTitle(), homework.getStatus());
                    break;
                default:
                    title = "作业通知: " + homework.getTitle();
                    content = String.format("课程 '%s' 的作业 '%s' 有更新",
                            course.getName(), homework.getTitle());
            }

            // Create the message
            Message message = new Message();
            message.setTitle(title);
            message.setContent(content);
            message.setTargetType(Message.MessageTargetType.COURSE);
            message.setTargetIds(homework.getCourseId().toString());
            message.setActive(true);

            // Add expiration if homework has due date
            if (homework.getDueDate() != null) {
                message.setExpiresAt(homework.getDueDate().plusDays(1)); // Expire one day after due date
            }

            // Save the message
            messageService.createMessage(message);
        } catch (Exception e) {
            // Log error but don't fail the homework operation
            System.err.println("Failed to send homework notification: " + e.getMessage());
        }
    }
}