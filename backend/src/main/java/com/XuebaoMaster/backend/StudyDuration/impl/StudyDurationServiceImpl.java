package com.XuebaoMaster.backend.StudyDuration.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.StudyDuration.StudyDuration;
import com.XuebaoMaster.backend.StudyDuration.StudyDurationRepository;
import com.XuebaoMaster.backend.StudyDuration.StudyDurationService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudyDurationServiceImpl implements StudyDurationService {

    @Autowired
    private StudyDurationRepository studyDurationRepository;

    @Override
    public StudyDuration createStudyDuration(StudyDuration studyDuration) {
        // 如果没有设置当前时间戳，则设置为当前时间
        if (studyDuration.getCurrentTimeStamp() == null) {
            studyDuration.setCurrentTimeStamp(LocalDateTime.now());
        }
        return studyDurationRepository.save(studyDuration);
    }
    
    @Override
    public StudyDuration createStudyDuration(LocalDateTime lessonStartTimeStamp, Integer length) {
        StudyDuration studyDuration = new StudyDuration();
        studyDuration.setCurrentTimeStamp(LocalDateTime.now());
        studyDuration.setLessonStartTimeStamp(lessonStartTimeStamp);
        studyDuration.setLength(length);
        
        return studyDurationRepository.save(studyDuration);
    }

    @Override
    public StudyDuration updateStudyDuration(StudyDuration studyDuration) {
        StudyDuration existingDuration = studyDurationRepository.findById(studyDuration.getId())
                .orElseThrow(() -> new RuntimeException("Study duration not found"));

        existingDuration.setCurrentTimeStamp(studyDuration.getCurrentTimeStamp());
        existingDuration.setLessonStartTimeStamp(studyDuration.getLessonStartTimeStamp());
        existingDuration.setLength(studyDuration.getLength());

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
    public List<StudyDuration> getStudyDurationsByCurrentTimeStampBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return studyDurationRepository.findByCurrentTimeStampBetween(startTime, endTime);
    }
    
    @Override
    public List<StudyDuration> getStudyDurationsByLessonStartTimeStampBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return studyDurationRepository.findByLessonStartTimeStampBetween(startTime, endTime);
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
        
        // 计算平均时长
        if (!durations.isEmpty()) {
            double sum = durations.stream().mapToInt(StudyDuration::getLength).sum();
            statistics.put("averageDuration", sum / durations.size());
        } else {
            statistics.put("averageDuration", 0.0);
        }
        
        return statistics;
    }
} 