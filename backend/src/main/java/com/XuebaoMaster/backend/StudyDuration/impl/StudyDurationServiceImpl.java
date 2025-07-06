package com.XuebaoMaster.backend.StudyDuration.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.StudyDuration.StudyDuration;
import com.XuebaoMaster.backend.StudyDuration.StudyDurationRepository;
import com.XuebaoMaster.backend.StudyDuration.StudyDurationService;
import com.XuebaoMaster.backend.User.UserService;
import com.XuebaoMaster.backend.Course.CourseService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class StudyDurationServiceImpl implements StudyDurationService {
    @Autowired
    private StudyDurationRepository studyDurationRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private CourseService courseService;
    @Override
    public StudyDuration createStudyDuration(StudyDuration studyDuration) {
        if (studyDuration.getCurrentTimeStamp() == null) {
            studyDuration.setCurrentTimeStamp(LocalDateTime.now());
        }
        return studyDurationRepository.save(studyDuration);
    }
    @Override
    public StudyDuration createStudyDuration(LocalDateTime lessonStartTimeStamp, Integer length) {
        throw new UnsupportedOperationException(
                "This method is no longer supported. Please use createStudyDuration(LocalDateTime, Integer, Long, Long) instead.");
    }
    @Override
    public StudyDuration createStudyDuration(LocalDateTime lessonStartTimeStamp, Integer length, Long userId,
            Long courseId) {
        StudyDuration studyDuration = new StudyDuration();
        studyDuration.setCurrentTimeStamp(LocalDateTime.now());
        studyDuration.setLessonStartTimeStamp(lessonStartTimeStamp);
        studyDuration.setLength(length);
        studyDuration.setUser(userService.getUserById(userId));
        studyDuration.setCourse(courseService.getCourseById(courseId));
        return studyDurationRepository.save(studyDuration);
    }
    @Override
    public StudyDuration updateStudyDuration(StudyDuration studyDuration) {
        StudyDuration existingDuration = studyDurationRepository.findById(studyDuration.getId())
                .orElseThrow(() -> new RuntimeException("Study duration not found"));
        existingDuration.setCurrentTimeStamp(studyDuration.getCurrentTimeStamp());
        existingDuration.setLessonStartTimeStamp(studyDuration.getLessonStartTimeStamp());
        existingDuration.setLength(studyDuration.getLength());
        if (studyDuration.getUser() != null) {
            existingDuration.setUser(studyDuration.getUser());
        }
        if (studyDuration.getCourse() != null) {
            existingDuration.setCourse(studyDuration.getCourse());
        }
        return studyDurationRepository.save(existingDuration);
    }
    @Override
    public void deleteStudyDuration(Long id) {
        studyDurationRepository.deleteById(id);
    }
    @Override
    public StudyDuration getStudyDurationById(Long id) {
        return studyDurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Study duration not found"));
    }
    @Override
    public List<StudyDuration> getStudyDurationsByCurrentTimeStampBetween(LocalDateTime startTime,
            LocalDateTime endTime) {
        return studyDurationRepository.findByCurrentTimeStampBetween(startTime, endTime);
    }
    @Override
    public List<StudyDuration> getStudyDurationsByLessonStartTimeStampBetween(LocalDateTime startTime,
            LocalDateTime endTime) {
        return studyDurationRepository.findByLessonStartTimeStampBetween(startTime, endTime);
    }
    @Override
    public List<StudyDuration> getStudyDurationsByUserId(Long userId) {
        return studyDurationRepository.findByUser_Id(userId);
    }
    @Override
    public List<StudyDuration> getStudyDurationsByCourseId(Long courseId) {
        return studyDurationRepository.findByCourse_CourseId(courseId);
    }
    @Override
    public List<StudyDuration> getStudyDurationsByClassId(Long classId) {
        return studyDurationRepository.findByUser_SchoolClass_Id(classId);
    }
    @Override
    public List<StudyDuration> getStudyDurationsByClassIdAndCourseId(Long classId, Long courseId) {
        return studyDurationRepository.findByUser_SchoolClass_IdAndCourse_CourseId(classId, courseId);
    }
    @Override
    public List<StudyDuration> getStudyDurationsByLengthGreaterThanEqual(Integer minLength) {
        return studyDurationRepository.findByLengthGreaterThanEqual(minLength);
    }
    @Override
    public List<StudyDuration> getStudyDurationsByLengthLessThanEqual(Integer maxLength) {
        return studyDurationRepository.findByLengthLessThanEqual(maxLength);
    }
    @Override
    public List<StudyDuration> getStudyDurationsByLengthBetween(Integer minLength, Integer maxLength) {
        return studyDurationRepository.findByLengthBetween(minLength, maxLength);
    }
    @Override
    public List<StudyDuration> getAllStudyDurations() {
        return studyDurationRepository.findAll();
    }
    @Override
    public Map<String, Object> getStudyStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        Integer totalDuration = studyDurationRepository.getTotalStudyDuration();
        Double averageDuration = studyDurationRepository.getAverageStudyDuration();
        statistics.put("totalDuration", totalDuration != null ? totalDuration : 0);
        statistics.put("averageDuration", averageDuration != null ? averageDuration : 0.0);
        statistics.put("recordCount", studyDurationRepository.count());
        return statistics;
    }
    @Override
    public Map<String, Object> getStudyStatisticsBetween(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> statistics = new HashMap<>();
        Integer totalDuration = studyDurationRepository.getTotalStudyDurationBetween(startTime, endTime);
        List<StudyDuration> durations = studyDurationRepository.findByCurrentTimeStampBetween(startTime, endTime);
        statistics.put("totalDuration", totalDuration != null ? totalDuration : 0);
        statistics.put("recordCount", durations.size());
        if (!durations.isEmpty()) {
            double sum = durations.stream().mapToInt(StudyDuration::getLength).sum();
            statistics.put("averageDuration", sum / durations.size());
        } else {
            statistics.put("averageDuration", 0.0);
        }
        return statistics;
    }
    @Override
    public Map<String, Object> getUserStudyStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();
        List<StudyDuration> userDurations = studyDurationRepository.findByUser_Id(userId);
        int totalDuration = userDurations.stream().mapToInt(StudyDuration::getLength).sum();
        double averageDuration = userDurations.isEmpty() ? 0.0 : totalDuration / (double) userDurations.size();
        statistics.put("totalDuration", totalDuration);
        statistics.put("averageDuration", averageDuration);
        statistics.put("recordCount", userDurations.size());
        return statistics;
    }
    @Override
    public Map<String, Object> getCourseStudyStatistics(Long courseId) {
        Map<String, Object> statistics = new HashMap<>();
        List<StudyDuration> courseDurations = studyDurationRepository.findByCourse_CourseId(courseId);
        int totalDuration = courseDurations.stream().mapToInt(StudyDuration::getLength).sum();
        double averageDuration = courseDurations.isEmpty() ? 0.0 : totalDuration / (double) courseDurations.size();
        statistics.put("totalDuration", totalDuration);
        statistics.put("averageDuration", averageDuration);
        statistics.put("recordCount", courseDurations.size());
        return statistics;
    }
    @Override
    public Map<String, Object> getClassStudyStatistics(Long classId) {
        Map<String, Object> statistics = new HashMap<>();
        List<StudyDuration> classDurations = studyDurationRepository.findByUser_SchoolClass_Id(classId);
        int totalDuration = classDurations.stream().mapToInt(StudyDuration::getLength).sum();
        double averageDuration = classDurations.isEmpty() ? 0.0 : totalDuration / (double) classDurations.size();
        Integer studentCount = studyDurationRepository.getStudentCountByClassId(classId);
        statistics.put("totalDuration", totalDuration);
        statistics.put("averageDuration", averageDuration);
        statistics.put("recordCount", classDurations.size());
        statistics.put("studentCount", studentCount != null ? studentCount : 0);
        statistics.put("averageDurationPerStudent",
                studentCount != null && studentCount > 0 ? totalDuration / (double) studentCount : 0.0);
        return statistics;
    }
    @Override
    public Map<String, Object> getClassCourseStudyStatistics(Long classId, Long courseId) {
        Map<String, Object> statistics = new HashMap<>();
        List<StudyDuration> classDurations = studyDurationRepository
                .findByUser_SchoolClass_IdAndCourse_CourseId(classId, courseId);
        int totalDuration = classDurations.stream().mapToInt(StudyDuration::getLength).sum();
        double averageDuration = classDurations.isEmpty() ? 0.0 : totalDuration / (double) classDurations.size();
        Integer studentCount = studyDurationRepository.getStudentCountByClassIdAndCourseId(classId, courseId);
        statistics.put("totalDuration", totalDuration);
        statistics.put("averageDuration", averageDuration);
        statistics.put("recordCount", classDurations.size());
        statistics.put("studentCount", studentCount != null ? studentCount : 0);
        statistics.put("averageDurationPerStudent",
                studentCount != null && studentCount > 0 ? totalDuration / (double) studentCount : 0.0);
        return statistics;
    }
}
