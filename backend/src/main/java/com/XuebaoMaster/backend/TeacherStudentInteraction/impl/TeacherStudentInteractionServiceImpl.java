package com.XuebaoMaster.backend.TeacherStudentInteraction.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.TeacherStudentInteraction.TeacherStudentInteraction;
import com.XuebaoMaster.backend.TeacherStudentInteraction.TeacherStudentInteractionRepository;
import com.XuebaoMaster.backend.TeacherStudentInteraction.TeacherStudentInteractionService;
import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.User.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class TeacherStudentInteractionServiceImpl implements TeacherStudentInteractionService {
    @Autowired
    private TeacherStudentInteractionRepository interactionRepository;
    @Autowired
    private UserRepository userRepository;
    @Override
    public TeacherStudentInteraction createInteraction(TeacherStudentInteraction interaction) {
        return interactionRepository.save(interaction);
    }
    @Override
    public TeacherStudentInteraction createInteraction(Long teacherId, Long studentId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Interaction content cannot be empty");
        }
        TeacherStudentInteraction interaction = new TeacherStudentInteraction();
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        interaction.setTeacher(teacher);
        interaction.setStudent(student);
        interaction.setContent(content);
        return interactionRepository.save(interaction);
    }
    @Override
    public TeacherStudentInteraction updateInteraction(TeacherStudentInteraction interaction) {
        if (interaction.getContent() == null || interaction.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Interaction content cannot be empty");
        }
        TeacherStudentInteraction existingInteraction = interactionRepository.findById(interaction.getId())
                .orElseThrow(() -> new RuntimeException("Interaction not found"));
        existingInteraction.setTeacher(interaction.getTeacher());
        existingInteraction.setStudent(interaction.getStudent());
        existingInteraction.setContent(interaction.getContent());
        return interactionRepository.save(existingInteraction);
    }
    @Override
    public void deleteInteraction(Long id) {
        interactionRepository.deleteById(id);
    }
    @Override
    public TeacherStudentInteraction getInteractionById(Long id) {
        return interactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interaction not found"));
    }
    @Override
    public List<TeacherStudentInteraction> getInteractionsByTeacherId(Long teacherId) {
        return interactionRepository.findByTeacherId(teacherId);
    }
    @Override
    public List<TeacherStudentInteraction> getInteractionsByStudentId(Long studentId) {
        return interactionRepository.findByStudentId(studentId);
    }
    @Override
    public List<TeacherStudentInteraction> getInteractionsByTeacherIdAndStudentId(Long teacherId, Long studentId) {
        return interactionRepository.findByTeacherIdAndStudentId(teacherId, studentId);
    }
    @Override
    public List<TeacherStudentInteraction> getInteractionsByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return interactionRepository.findByCreatedAtBetween(startTime, endTime);
    }
    @Override
    public List<TeacherStudentInteraction> getInteractionsByTeacherIdAndCreatedAtBetween(
            Long teacherId, LocalDateTime startTime, LocalDateTime endTime) {
        return interactionRepository.findByTeacherIdAndCreatedAtBetween(teacherId, startTime, endTime);
    }
    @Override
    public List<TeacherStudentInteraction> getInteractionsByStudentIdAndCreatedAtBetween(
            Long studentId, LocalDateTime startTime, LocalDateTime endTime) {
        return interactionRepository.findByStudentIdAndCreatedAtBetween(studentId, startTime, endTime);
    }
    @Override
    public List<TeacherStudentInteraction> getInteractionsByTeacherIdAndStudentIdAndCreatedAtBetween(
            Long teacherId, Long studentId, LocalDateTime startTime, LocalDateTime endTime) {
        return interactionRepository.findByTeacherIdAndStudentIdAndCreatedAtBetween(
                teacherId, studentId, startTime, endTime);
    }
    @Override
    public List<TeacherStudentInteraction> getAllInteractions() {
        return interactionRepository.findAll();
    }
    @Override
    public Map<String, Object> getInteractionStatisticsByTeacherId(Long teacherId) {
        Map<String, Object> statistics = new HashMap<>();
        List<TeacherStudentInteraction> interactions = interactionRepository.findByTeacherId(teacherId);
        Long interactionCount = interactionRepository.countByTeacherId(teacherId);
        statistics.put("teacherId", teacherId);
        statistics.put("interactionCount", interactionCount);
        long uniqueStudentCount = interactions.stream()
                .map(interaction -> interaction.getStudent().getId())
                .distinct()
                .count();
        statistics.put("uniqueStudentCount", uniqueStudentCount);
        return statistics;
    }
    @Override
    public Map<String, Object> getInteractionStatisticsByStudentId(Long studentId) {
        Map<String, Object> statistics = new HashMap<>();
        List<TeacherStudentInteraction> interactions = interactionRepository.findByStudentId(studentId);
        Long interactionCount = interactionRepository.countByStudentId(studentId);
        statistics.put("studentId", studentId);
        statistics.put("interactionCount", interactionCount);
        long uniqueTeacherCount = interactions.stream()
                .map(interaction -> interaction.getTeacher().getId())
                .distinct()
                .count();
        statistics.put("uniqueTeacherCount", uniqueTeacherCount);
        return statistics;
    }
    @Override
    public Map<String, Object> getInteractionStatisticsByTeacherIdAndStudentId(Long teacherId, Long studentId) {
        Map<String, Object> statistics = new HashMap<>();
        Long interactionCount = interactionRepository.countByTeacherIdAndStudentId(teacherId, studentId);
        statistics.put("teacherId", teacherId);
        statistics.put("studentId", studentId);
        statistics.put("interactionCount", interactionCount);
        return statistics;
    }
    @Override
    public Map<String, Object> getOverallInteractionStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        List<TeacherStudentInteraction> interactions = interactionRepository.findAll();
        statistics.put("totalInteractionCount", interactions.size());
        long uniqueTeacherCount = interactions.stream()
                .map(interaction -> interaction.getTeacher().getId())
                .distinct()
                .count();
        statistics.put("uniqueTeacherCount", uniqueTeacherCount);
        long uniqueStudentCount = interactions.stream()
                .map(interaction -> interaction.getStudent().getId())
                .distinct()
                .count();
        statistics.put("uniqueStudentCount", uniqueStudentCount);
        return statistics;
    }
} 
