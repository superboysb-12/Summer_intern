package com.XuebaoMaster.backend.StudentEmotion.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.StudentEmotion.StudentEmotion;
import com.XuebaoMaster.backend.StudentEmotion.StudentEmotionRepository;
import com.XuebaoMaster.backend.StudentEmotion.StudentEmotionService;
import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.User.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class StudentEmotionServiceImpl implements StudentEmotionService {
    @Autowired
    private StudentEmotionRepository studentEmotionRepository;
    @Autowired
    private UserRepository userRepository;
    @Override
    public StudentEmotion createStudentEmotion(StudentEmotion studentEmotion) {
        validateMark(studentEmotion.getMark());
        return studentEmotionRepository.save(studentEmotion);
    }
    @Override
    public StudentEmotion createStudentEmotion(Long userId, Integer mark) {
        validateMark(mark);
        StudentEmotion studentEmotion = new StudentEmotion();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        studentEmotion.setUser(user);
        studentEmotion.setMark(mark);
        return studentEmotionRepository.save(studentEmotion);
    }
    @Override
    public StudentEmotion updateStudentEmotion(StudentEmotion studentEmotion) {
        validateMark(studentEmotion.getMark());
        StudentEmotion existingEmotion = studentEmotionRepository.findById(studentEmotion.getId())
                .orElseThrow(() -> new RuntimeException("Student emotion not found"));
        existingEmotion.setUser(studentEmotion.getUser());
        existingEmotion.setMark(studentEmotion.getMark());
        return studentEmotionRepository.save(existingEmotion);
    }
    @Override
    public void deleteStudentEmotion(Long id) {
        studentEmotionRepository.deleteById(id);
    }
    @Override
    public StudentEmotion getStudentEmotionById(Long id) {
        return studentEmotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student emotion not found"));
    }
    @Override
    public List<StudentEmotion> getStudentEmotionsByUserId(Long userId) {
        return studentEmotionRepository.findByUserId(userId);
    }
    @Override
    public List<StudentEmotion> getStudentEmotionsByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return studentEmotionRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }
    @Override
    public List<StudentEmotion> getStudentEmotionsByMarkGreaterThanEqual(Integer minMark) {
        return studentEmotionRepository.findByMarkGreaterThanEqual(minMark);
    }
    @Override
    public List<StudentEmotion> getStudentEmotionsByMarkLessThanEqual(Integer maxMark) {
        return studentEmotionRepository.findByMarkLessThanEqual(maxMark);
    }
    @Override
    public List<StudentEmotion> getStudentEmotionsByMarkBetween(Integer minMark, Integer maxMark) {
        return studentEmotionRepository.findByMarkBetween(minMark, maxMark);
    }
    @Override
    public List<StudentEmotion> getAllStudentEmotions() {
        return studentEmotionRepository.findAll();
    }
    @Override
    public Map<String, Object> getEmotionStatisticsByUserId(Long userId) {
        Map<String, Object> statistics = new HashMap<>();
        List<StudentEmotion> emotions = studentEmotionRepository.findByUserId(userId);
        Double averageMark = studentEmotionRepository.getAverageMarkByUserId(userId);
        statistics.put("recordCount", emotions.size());
        statistics.put("averageMark", averageMark != null ? averageMark : 0.0);
        if (!emotions.isEmpty()) {
            int maxMark = emotions.stream().mapToInt(StudentEmotion::getMark).max().orElse(0);
            int minMark = emotions.stream().mapToInt(StudentEmotion::getMark).min().orElse(0);
            statistics.put("maxMark", maxMark);
            statistics.put("minMark", minMark);
        } else {
            statistics.put("maxMark", 0);
            statistics.put("minMark", 0);
        }
        return statistics;
    }
    @Override
    public Map<String, Object> getEmotionStatisticsByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> statistics = new HashMap<>();
        List<StudentEmotion> emotions = studentEmotionRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime);
        Double averageMark = studentEmotionRepository.getAverageMarkByUserIdAndCreatedAtBetween(userId, startTime, endTime);
        statistics.put("recordCount", emotions.size());
        statistics.put("averageMark", averageMark != null ? averageMark : 0.0);
        statistics.put("startTime", startTime);
        statistics.put("endTime", endTime);
        if (!emotions.isEmpty()) {
            int maxMark = emotions.stream().mapToInt(StudentEmotion::getMark).max().orElse(0);
            int minMark = emotions.stream().mapToInt(StudentEmotion::getMark).min().orElse(0);
            statistics.put("maxMark", maxMark);
            statistics.put("minMark", minMark);
        } else {
            statistics.put("maxMark", 0);
            statistics.put("minMark", 0);
        }
        return statistics;
    }
    @Override
    public Map<String, Object> getOverallEmotionStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        List<StudentEmotion> emotions = studentEmotionRepository.findAll();
        Double averageMark = studentEmotionRepository.getAverageMark();
        statistics.put("recordCount", emotions.size());
        statistics.put("averageMark", averageMark != null ? averageMark : 0.0);
        if (!emotions.isEmpty()) {
            int maxMark = emotions.stream().mapToInt(StudentEmotion::getMark).max().orElse(0);
            int minMark = emotions.stream().mapToInt(StudentEmotion::getMark).min().orElse(0);
            statistics.put("maxMark", maxMark);
            statistics.put("minMark", minMark);
        } else {
            statistics.put("maxMark", 0);
            statistics.put("minMark", 0);
        }
        return statistics;
    }
    private void validateMark(Integer mark) {
        if (mark < 0 || mark > 100) {
            throw new IllegalArgumentException("Emotion mark must be between 0 and 100");
        }
    }
} 
