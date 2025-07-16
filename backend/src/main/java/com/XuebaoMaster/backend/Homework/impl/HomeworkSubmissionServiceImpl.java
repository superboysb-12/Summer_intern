package com.XuebaoMaster.backend.Homework.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.Homework.Homework;
import com.XuebaoMaster.backend.Homework.HomeworkRepository;
import com.XuebaoMaster.backend.Homework.HomeworkSubmission;
import com.XuebaoMaster.backend.Homework.HomeworkSubmissionRepository;
import com.XuebaoMaster.backend.Homework.HomeworkSubmissionService;
import com.XuebaoMaster.backend.Message.Message;
import com.XuebaoMaster.backend.Message.MessageService;
import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.User.UserService;
import com.XuebaoMaster.backend.Course.CourseService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HomeworkSubmissionServiceImpl implements HomeworkSubmissionService {
    @Autowired
    private HomeworkSubmissionRepository submissionRepository;

    @Autowired
    private HomeworkRepository homeworkRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @Override
    public HomeworkSubmission createSubmission(HomeworkSubmission submission) {
        // 检查是否为重复提交
        submissionRepository.findByStudentIdAndHomeworkId(submission.getStudentId(), submission.getHomeworkId())
                .ifPresent(existingSubmission -> {
                    throw new RuntimeException("你已经提交过该作业，请使用更新功能");
                });

        // 获取作业信息，检查截止日期
        Homework homework = homeworkRepository.findById(submission.getHomeworkId())
                .orElseThrow(() -> new RuntimeException("作业不存在"));

        // 如果有截止日期且当前时间晚于截止日期，标记为迟交
        if (homework.getDueDate() != null && LocalDateTime.now().isAfter(homework.getDueDate())) {
            submission.setIsLate(true);
        } else {
            submission.setIsLate(false);
        }

        // 设置默认状态为已提交
        submission.setStatus("SUBMITTED");

        // 保存提交
        HomeworkSubmission savedSubmission = submissionRepository.save(submission);

        // 发送通知给教师
        sendSubmissionNotification(savedSubmission, homework, "CREATED");

        return savedSubmission;
    }

    @Override
    public HomeworkSubmission updateSubmission(HomeworkSubmission submission) {
        // 检查提交是否存在
        HomeworkSubmission existingSubmission = submissionRepository.findById(submission.getId())
                .orElseThrow(() -> new RuntimeException("提交记录不存在"));

        // 获取作业信息
        Homework homework = homeworkRepository.findById(existingSubmission.getHomeworkId())
                .orElseThrow(() -> new RuntimeException("作业不存在"));

        // 更新内容和文件ID
        existingSubmission.setContent(submission.getContent());
        if (submission.getFileId() != null) {
            existingSubmission.setFileId(submission.getFileId());
        }

        // 更新提交时间
        existingSubmission.setSubmissionDate(LocalDateTime.now());

        // 如果有截止日期且当前时间晚于截止日期，标记为迟交
        if (homework.getDueDate() != null && LocalDateTime.now().isAfter(homework.getDueDate())) {
            existingSubmission.setIsLate(true);
        }

        // 更新状态为已提交（如果之前是已评分或已退回）
        if ("GRADED".equals(existingSubmission.getStatus()) || "RETURNED".equals(existingSubmission.getStatus())) {
            existingSubmission.setStatus("SUBMITTED");
        }

        // 保存更新
        HomeworkSubmission updatedSubmission = submissionRepository.save(existingSubmission);

        // 发送通知给教师
        sendSubmissionNotification(updatedSubmission, homework, "UPDATED");

        return updatedSubmission;
    }

    @Override
    public void deleteSubmission(Long submissionId) {
        // 检查提交是否存在
        HomeworkSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("提交记录不存在"));

        // 删除提交
        submissionRepository.deleteById(submissionId);
    }

    @Override
    public HomeworkSubmission getSubmissionById(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("提交记录不存在"));
    }

    @Override
    public List<HomeworkSubmission> getAllSubmissions() {
        return submissionRepository.findAll();
    }

    @Override
    public List<HomeworkSubmission> getSubmissionsByHomework(Long homeworkId) {
        return submissionRepository.findByHomeworkId(homeworkId);
    }

    @Override
    public List<HomeworkSubmission> getSubmissionsByStudent(Long studentId) {
        return submissionRepository.findByStudentId(studentId);
    }

    @Override
    public HomeworkSubmission getSubmissionByStudentAndHomework(Long studentId, Long homeworkId) {
        return submissionRepository.findByStudentIdAndHomeworkId(studentId, homeworkId)
                .orElseThrow(() -> new RuntimeException("未找到该学生对该作业的提交记录"));
    }

    @Override
    public HomeworkSubmission gradeSubmission(Long submissionId, Integer score, String feedback) {
        // 检查提交是否存在
        HomeworkSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("提交记录不存在"));

        // 更新分数和反馈
        submission.setScore(score);
        submission.setFeedback(feedback);
        submission.setStatus("GRADED");

        // 保存更新
        HomeworkSubmission gradedSubmission = submissionRepository.save(submission);

        // 获取作业信息
        Homework homework = homeworkRepository.findById(submission.getHomeworkId())
                .orElseThrow(() -> new RuntimeException("作业不存在"));

        // 发送通知给学生
        sendGradeNotification(gradedSubmission, homework);

        return gradedSubmission;
    }

    @Override
    public List<HomeworkSubmission> getSubmissionsByStatus(String status) {
        return submissionRepository.findByStatus(status);
    }

    @Override
    public List<HomeworkSubmission> getSubmissionsByHomeworkAndStatus(Long homeworkId, String status) {
        return submissionRepository.findByHomeworkIdAndStatus(homeworkId, status);
    }

    @Override
    public List<HomeworkSubmission> getSubmissionsByStudentAndStatus(Long studentId, String status) {
        return submissionRepository.findByStudentIdAndStatus(studentId, status);
    }

    @Override
    public HomeworkSubmission updateSubmissionStatus(Long submissionId, String status) {
        // 检查提交是否存在
        HomeworkSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("提交记录不存在"));

        // 更新状态
        submission.setStatus(status);

        // 保存更新
        return submissionRepository.save(submission);
    }

    /**
     * 发送作业提交通知给教师
     * 
     * @param submission 提交记录
     * @param homework   作业
     * @param action     操作类型（CREATED, UPDATED）
     */
    private void sendSubmissionNotification(HomeworkSubmission submission, Homework homework, String action) {
        try {
            // 获取学生信息
            User student = userService.getUserById(submission.getStudentId());

            // 创建通知内容
            String title = "";
            String content = "";

            switch (action) {
                case "CREATED":
                    title = "新作业提交: " + homework.getTitle();
                    content = String.format("学生 %s (ID: %d) 提交了作业 '%s'",
                            student.getUsername(), student.getId(), homework.getTitle());
                    break;
                case "UPDATED":
                    title = "作业提交更新: " + homework.getTitle();
                    content = String.format("学生 %s (ID: %d) 更新了对作业 '%s' 的提交",
                            student.getUsername(), student.getId(), homework.getTitle());
                    break;
                default:
                    title = "作业提交通知";
                    content = String.format("学生 %s 的作业 '%s' 有更新",
                            student.getUsername(), homework.getTitle());
            }

            if (submission.getIsLate()) {
                content += "\n注意: 此提交已超过截止日期。";
            }

            // 创建消息
            Message message = new Message();
            message.setTitle(title);
            message.setContent(content);
            message.setTargetType(Message.MessageTargetType.SPECIFIC);

            // 获取教师ID（通过课程ID）
            Long teacherId = courseService.getCourseById(homework.getCourseId()).getTeacherId();
            message.setTargetIds(teacherId.toString());

            message.setActive(true);

            // 保存消息
            messageService.createMessage(message);
        } catch (Exception e) {
            // 记录错误但不影响作业提交
            System.err.println("发送作业提交通知失败: " + e.getMessage());
        }
    }

    /**
     * 发送评分通知给学生
     * 
     * @param submission 提交记录
     * @param homework   作业
     */
    private void sendGradeNotification(HomeworkSubmission submission, Homework homework) {
        try {
            // 创建通知内容
            String title = "作业评分通知: " + homework.getTitle();
            String content = String.format("你的作业 '%s' 已评分。\n分数: %d\n反馈: %s",
                    homework.getTitle(), submission.getScore(), submission.getFeedback());

            // 创建消息
            Message message = new Message();
            message.setTitle(title);
            message.setContent(content);
            message.setTargetType(Message.MessageTargetType.SPECIFIC);
            message.setTargetIds(submission.getStudentId().toString());
            message.setActive(true);

            // 保存消息
            messageService.createMessage(message);
        } catch (Exception e) {
            // 记录错误但不影响评分过程
            System.err.println("发送评分通知失败: " + e.getMessage());
        }
    }
}