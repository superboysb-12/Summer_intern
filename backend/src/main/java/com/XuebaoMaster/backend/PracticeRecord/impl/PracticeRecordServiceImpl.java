package com.XuebaoMaster.backend.PracticeRecord.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.PracticeRecord.PracticeRecord;
import com.XuebaoMaster.backend.PracticeRecord.PracticeRecordRepository;
import com.XuebaoMaster.backend.PracticeRecord.PracticeRecordService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class PracticeRecordServiceImpl implements PracticeRecordService {
    @Autowired
    private PracticeRecordRepository practiceRecordRepository;
    @Override
    public PracticeRecord createPracticeRecord(PracticeRecord practiceRecord) {
        return practiceRecordRepository.save(practiceRecord);
    }
    @Override
    public PracticeRecord updatePracticeRecord(PracticeRecord practiceRecord) {
        PracticeRecord existingRecord = practiceRecordRepository.findById(practiceRecord.getId())
                .orElseThrow(() -> new RuntimeException("Practice record not found"));
        existingRecord.setStudent(practiceRecord.getStudent());
        existingRecord.setQuestion(practiceRecord.getQuestion());
        existingRecord.setFinishTime(practiceRecord.getFinishTime());
        existingRecord.setIsRight(practiceRecord.getIsRight());
        return practiceRecordRepository.save(existingRecord);
    }
    @Override
    public void deletePracticeRecord(Long id) {
        practiceRecordRepository.deleteById(id);
    }
    @Override
    public PracticeRecord getPracticeRecordById(Long id) {
        return practiceRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Practice record not found"));
    }
    @Override
    public List<PracticeRecord> getPracticeRecordsByStudentId(Long studentId) {
        return practiceRecordRepository.findByStudentId(studentId);
    }
    @Override
    public List<PracticeRecord> getPracticeRecordsByQuestionId(Long questionId) {
        return practiceRecordRepository.findByQuestionId(questionId);
    }
    @Override
    public List<PracticeRecord> getPracticeRecordsByStudentIdAndQuestionId(Long studentId, Long questionId) {
        return practiceRecordRepository.findByStudentIdAndQuestionId(studentId, questionId);
    }
    @Override
    public List<PracticeRecord> getPracticeRecordsByIsRight(Boolean isRight) {
        return practiceRecordRepository.findByIsRight(isRight);
    }
    @Override
    public List<PracticeRecord> getPracticeRecordsByStudentIdAndIsRight(Long studentId, Boolean isRight) {
        return practiceRecordRepository.findByStudentIdAndIsRight(studentId, isRight);
    }
    @Override
    public List<PracticeRecord> getPracticeRecordsByQuestionIdAndIsRight(Long questionId, Boolean isRight) {
        return practiceRecordRepository.findByQuestionIdAndIsRight(questionId, isRight);
    }
    @Override
    public List<PracticeRecord> getAllPracticeRecords() {
        return practiceRecordRepository.findAll();
    }
    @Override
    public Map<String, Long> getStudentStatistics(Long studentId) {
        Map<String, Long> statistics = new HashMap<>();
        Long totalCount = practiceRecordRepository.countByStudentId(studentId);
        Long correctCount = practiceRecordRepository.countCorrectByStudentId(studentId);
        statistics.put("totalCount", totalCount);
        statistics.put("correctCount", correctCount);
        statistics.put("incorrectCount", totalCount - correctCount);
        return statistics;
    }
} 
