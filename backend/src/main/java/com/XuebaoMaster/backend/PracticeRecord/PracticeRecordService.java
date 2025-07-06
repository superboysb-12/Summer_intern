package com.XuebaoMaster.backend.PracticeRecord;
import java.util.List;
import java.util.Map;
public interface PracticeRecordService {
    PracticeRecord createPracticeRecord(PracticeRecord practiceRecord);
    PracticeRecord updatePracticeRecord(PracticeRecord practiceRecord);
    void deletePracticeRecord(Long id);
    PracticeRecord getPracticeRecordById(Long id);
    List<PracticeRecord> getPracticeRecordsByStudentId(Long studentId);
    List<PracticeRecord> getPracticeRecordsByQuestionId(Long questionId);
    List<PracticeRecord> getPracticeRecordsByStudentIdAndQuestionId(Long studentId, Long questionId);
    List<PracticeRecord> getPracticeRecordsByIsRight(Boolean isRight);
    List<PracticeRecord> getPracticeRecordsByStudentIdAndIsRight(Long studentId, Boolean isRight);
    List<PracticeRecord> getPracticeRecordsByQuestionIdAndIsRight(Long questionId, Boolean isRight);
    List<PracticeRecord> getAllPracticeRecords();
    Map<String, Long> getStudentStatistics(Long studentId);
} 
