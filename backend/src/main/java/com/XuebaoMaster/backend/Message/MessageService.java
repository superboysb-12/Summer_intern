package com.XuebaoMaster.backend.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.SchoolClass.SchoolClassService;
import com.XuebaoMaster.backend.StudentCourse.StudentCourseService;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SchoolClassService schoolClassService;

    @Autowired
    private StudentCourseService studentCourseService;

    // Create a new message
    public Message createMessage(Message message) {
        return messageRepository.save(message);
    }

    // Get all messages (for admin)
    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    // Get message by ID
    public Optional<Message> getMessageById(Long id) {
        return messageRepository.findById(id);
    }

    // Update message
    public Message updateMessage(Message message) {
        // Check if message exists
        if (message.getId() != null && messageRepository.existsById(message.getId())) {
            return messageRepository.save(message);
        }
        throw new RuntimeException("Message not found with id: " + message.getId());
    }

    // Delete message
    public void deleteMessage(Long id) {
        messageRepository.deleteById(id);
    }

    // Search messages by keyword in title or content
    public List<Message> searchMessages(String keyword) {
        return messageRepository.findByTitleContainingOrContentContainingOrderByCreatedAtDesc(keyword, keyword);
    }

    // Get messages for a specific user based on their role
    public List<Message> getMessagesForUser(User user) {
        LocalDateTime now = LocalDateTime.now();
        Message.MessageTargetType targetType;

        // Determine target type based on user role
        switch (user.getUserRole()) {
            case ADMIN:
                targetType = Message.MessageTargetType.ADMINS;
                break;
            case TEACHER:
                targetType = Message.MessageTargetType.TEACHERS;
                break;
            case STUDENT:
                targetType = Message.MessageTargetType.STUDENTS;
                break;
            default:
                targetType = Message.MessageTargetType.ALL;
                break;
        }

        List<Message> userMessages = messageRepository.findMessagesForUser(now, targetType, user.getId().toString());

        // 如果用户是学生，查找他们所属班级的消息
        if (user.getUserRole() == User.UserRoleType.STUDENT) {
            // 获取班级消息
            if (user.getSchoolClass() != null) {
                String classId = user.getSchoolClass().getId().toString();
                List<Message> classMessages = messageRepository.findActiveMessagesByTargetType(
                        now, Message.MessageTargetType.CLASS, classId);
                userMessages.addAll(classMessages);
            }

            // 获取课程消息
            List<Long> userCourseIds = studentCourseService.getStudentCourses(user.getId())
                    .stream()
                    .map(sc -> sc.getCourseId())
                    .collect(Collectors.toList());

            for (Long courseId : userCourseIds) {
                List<Message> courseMessages = messageRepository.findActiveMessagesByTargetType(
                        now, Message.MessageTargetType.COURSE, courseId.toString());
                userMessages.addAll(courseMessages);
            }
        }

        return userMessages;
    }

    // Get active messages (not expired and active flag is true)
    public List<Message> getActiveMessages() {
        return messageRepository.findActiveMessages(LocalDateTime.now());
    }

    // Toggle message active status
    public Message toggleMessageStatus(Long id) {
        Optional<Message> messageOpt = messageRepository.findById(id);
        if (messageOpt.isPresent()) {
            Message message = messageOpt.get();
            message.setActive(!message.isActive());
            return messageRepository.save(message);
        }
        throw new RuntimeException("Message not found with id: " + id);
    }
}