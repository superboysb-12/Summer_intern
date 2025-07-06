package com.XuebaoMaster.backend.LearningJournalAnalysis;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
public interface LearningJournalAnalysisService {
    LearningJournalAnalysis createJournalAnalysis(LearningJournalAnalysis journalAnalysis);
    LearningJournalAnalysis createJournalAnalysis(Long questionId, String content);
    LearningJournalAnalysis updateJournalAnalysis(LearningJournalAnalysis journalAnalysis);
    void deleteJournalAnalysis(Long id);
    LearningJournalAnalysis getJournalAnalysisById(Long id);
    List<LearningJournalAnalysis> getJournalAnalysesByQuestionId(Long questionId);
    List<LearningJournalAnalysis> getJournalAnalysesByLessonNodeId(Long lessonNodeId);
    List<LearningJournalAnalysis> getJournalAnalysesByCourseId(Long courseId);
    List<LearningJournalAnalysis> getJournalAnalysesByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<LearningJournalAnalysis> getJournalAnalysesByQuestionIdAndCreatedAtBetween(
            Long questionId, LocalDateTime startTime, LocalDateTime endTime);
    List<LearningJournalAnalysis> getJournalAnalysesByContentContaining(String keyword);
    List<LearningJournalAnalysis> getAllJournalAnalyses();
    Map<String, Object> getJournalAnalysisStatisticsByQuestionId(Long questionId);
    Map<String, Object> getJournalAnalysisStatisticsByLessonNodeId(Long lessonNodeId);
    Map<String, Object> getJournalAnalysisStatisticsByCourseId(Long courseId);
    Map<String, Object> getOverallJournalAnalysisStatistics();
    Map<String, Object> analyzeJournalContentKeywords(Long questionId);
    Map<String, Object> analyzeJournalContentSentiment(Long questionId);
} 
