package com.XuebaoMaster.backend.LearningJournalAnalysis.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.LearningJournalAnalysis.LearningJournalAnalysis;
import com.XuebaoMaster.backend.LearningJournalAnalysis.LearningJournalAnalysisRepository;
import com.XuebaoMaster.backend.LearningJournalAnalysis.LearningJournalAnalysisService;
import com.XuebaoMaster.backend.Question.Question;
import com.XuebaoMaster.backend.Question.QuestionRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class LearningJournalAnalysisServiceImpl implements LearningJournalAnalysisService {
    @Autowired
    private LearningJournalAnalysisRepository journalAnalysisRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Override
    public LearningJournalAnalysis createJournalAnalysis(LearningJournalAnalysis journalAnalysis) {
        if (journalAnalysis.getContent() == null || journalAnalysis.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Journal content cannot be empty");
        }
        return journalAnalysisRepository.save(journalAnalysis);
    }
    @Override
    public LearningJournalAnalysis createJournalAnalysis(Long questionId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Journal content cannot be empty");
        }
        LearningJournalAnalysis journalAnalysis = new LearningJournalAnalysis();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        journalAnalysis.setQuestion(question);
        journalAnalysis.setContent(content);
        return journalAnalysisRepository.save(journalAnalysis);
    }
    @Override
    public LearningJournalAnalysis updateJournalAnalysis(LearningJournalAnalysis journalAnalysis) {
        if (journalAnalysis.getContent() == null || journalAnalysis.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Journal content cannot be empty");
        }
        LearningJournalAnalysis existingJournalAnalysis = journalAnalysisRepository.findById(journalAnalysis.getId())
                .orElseThrow(() -> new RuntimeException("Journal analysis not found"));
        existingJournalAnalysis.setQuestion(journalAnalysis.getQuestion());
        existingJournalAnalysis.setContent(journalAnalysis.getContent());
        return journalAnalysisRepository.save(existingJournalAnalysis);
    }
    @Override
    public void deleteJournalAnalysis(Long id) {
        journalAnalysisRepository.deleteById(id);
    }
    @Override
    public LearningJournalAnalysis getJournalAnalysisById(Long id) {
        return journalAnalysisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Journal analysis not found"));
    }
    @Override
    public List<LearningJournalAnalysis> getJournalAnalysesByQuestionId(Long questionId) {
        return journalAnalysisRepository.findByQuestionId(questionId);
    }
    @Override
    public List<LearningJournalAnalysis> getJournalAnalysesByLessonNodeId(Long lessonNodeId) {
        return journalAnalysisRepository.findByLessonNodeId(lessonNodeId);
    }
    @Override
    public List<LearningJournalAnalysis> getJournalAnalysesByCourseId(Long courseId) {
        return journalAnalysisRepository.findByCourseId(courseId);
    }
    @Override
    public List<LearningJournalAnalysis> getJournalAnalysesByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return journalAnalysisRepository.findByCreatedAtBetween(startTime, endTime);
    }
    @Override
    public List<LearningJournalAnalysis> getJournalAnalysesByQuestionIdAndCreatedAtBetween(
            Long questionId, LocalDateTime startTime, LocalDateTime endTime) {
        return journalAnalysisRepository.findByQuestionIdAndCreatedAtBetween(questionId, startTime, endTime);
    }
    @Override
    public List<LearningJournalAnalysis> getJournalAnalysesByContentContaining(String keyword) {
        return journalAnalysisRepository.findByContentContainingIgnoreCase(keyword);
    }
    @Override
    public List<LearningJournalAnalysis> getAllJournalAnalyses() {
        return journalAnalysisRepository.findAll();
    }
    @Override
    public Map<String, Object> getJournalAnalysisStatisticsByQuestionId(Long questionId) {
        Map<String, Object> statistics = new HashMap<>();
        List<LearningJournalAnalysis> journalAnalyses = journalAnalysisRepository.findByQuestionId(questionId);
        Long journalCount = journalAnalysisRepository.countByQuestionId(questionId);
        statistics.put("questionId", questionId);
        statistics.put("journalCount", journalCount);
        double avgContentLength = journalAnalyses.stream()
                .mapToInt(journal -> journal.getContent().length())
                .average()
                .orElse(0);
        statistics.put("averageContentLength", avgContentLength);
        Optional<LearningJournalAnalysis> latestJournal = journalAnalyses.stream()
                .max(Comparator.comparing(LearningJournalAnalysis::getCreatedAt));
        if (latestJournal.isPresent()) {
            statistics.put("latestJournalId", latestJournal.get().getId());
            statistics.put("latestJournalCreatedAt", latestJournal.get().getCreatedAt());
        }
        return statistics;
    }
    @Override
    public Map<String, Object> getJournalAnalysisStatisticsByLessonNodeId(Long lessonNodeId) {
        Map<String, Object> statistics = new HashMap<>();
        List<LearningJournalAnalysis> journalAnalyses = journalAnalysisRepository.findByLessonNodeId(lessonNodeId);
        Long journalCount = journalAnalysisRepository.countByLessonNodeId(lessonNodeId);
        statistics.put("lessonNodeId", lessonNodeId);
        statistics.put("journalCount", journalCount);
        double avgContentLength = journalAnalyses.stream()
                .mapToInt(journal -> journal.getContent().length())
                .average()
                .orElse(0);
        statistics.put("averageContentLength", avgContentLength);
        Map<Long, Long> journalCountByQuestion = journalAnalyses.stream()
                .collect(Collectors.groupingBy(
                        journal -> journal.getQuestion().getId(),
                        Collectors.counting()
                ));
        statistics.put("journalCountByQuestion", journalCountByQuestion);
        return statistics;
    }
    @Override
    public Map<String, Object> getJournalAnalysisStatisticsByCourseId(Long courseId) {
        Map<String, Object> statistics = new HashMap<>();
        List<LearningJournalAnalysis> journalAnalyses = journalAnalysisRepository.findByCourseId(courseId);
        Long journalCount = journalAnalysisRepository.countByCourseId(courseId);
        statistics.put("courseId", courseId);
        statistics.put("journalCount", journalCount);
        double avgContentLength = journalAnalyses.stream()
                .mapToInt(journal -> journal.getContent().length())
                .average()
                .orElse(0);
        statistics.put("averageContentLength", avgContentLength);
        Map<Long, Long> journalCountByLessonNode = journalAnalyses.stream()
                .collect(Collectors.groupingBy(
                        journal -> journal.getQuestion().getLessonNode().getId(),
                        Collectors.counting()
                ));
        statistics.put("journalCountByLessonNode", journalCountByLessonNode);
        return statistics;
    }
    @Override
    public Map<String, Object> getOverallJournalAnalysisStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        List<LearningJournalAnalysis> journalAnalyses = journalAnalysisRepository.findAll();
        statistics.put("totalJournalCount", journalAnalyses.size());
        double avgContentLength = journalAnalyses.stream()
                .mapToInt(journal -> journal.getContent().length())
                .average()
                .orElse(0);
        statistics.put("averageContentLength", avgContentLength);
        Map<Long, Long> journalCountByQuestion = journalAnalyses.stream()
                .collect(Collectors.groupingBy(
                        journal -> journal.getQuestion().getId(),
                        Collectors.counting()
                ));
        List<Map.Entry<Long, Long>> topQuestions = journalCountByQuestion.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());
        Map<Long, Long> topQuestionsMap = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> entry : topQuestions) {
            topQuestionsMap.put(entry.getKey(), entry.getValue());
        }
        statistics.put("topQuestionsByJournalCount", topQuestionsMap);
        return statistics;
    }
    @Override
    public Map<String, Object> analyzeJournalContentKeywords(Long questionId) {
        Map<String, Object> analysis = new HashMap<>();
        List<LearningJournalAnalysis> journalAnalyses = journalAnalysisRepository.findByQuestionId(questionId);
        Map<String, Integer> wordFrequency = new HashMap<>();
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "鐨?, "浜?, "鍜?, "鏄?, "鍦?, "鎴?, "鏈?, "杩?, "涓?, "閭?, "灏?, "涓?, "浜?, "閮?, "涓€", "涓?, "浠?, "瑕?, "涓?, "浠?, "鑰?, "浣?, "涔?, "浣?, "鍒?, "濡?, "琚?, "瀹?, "鎵€", "绛?, "涔?, "涓?, "鍙?, "鑳?, "鏉?, "瀵?, "鏃?, "鑷?, "浼?, "鐢?, "琛?, "鏂?, "杩?, "浣?, "鐢?, "鑳?, "鍘?, "閲?, "鍚?, "澶?, "涔?, "寰?, "璇?, "浜?, "濂?, "鐪?, "鎯?, "涔?, "娌?, "鍒?, "寰?, "鍐?, "鍚?, "鎶?, "缁?, "璁?, "浣?, "鍗?, "浠€", "鐫€", "鍙?, "浜?, "濡?, "濂?, "鍥?, "鏈?, "浠€", "涔?, "鏍?, "宸?, "浠?, "鍋?, "涓?, "鎴?, "鐢?, "杩?, "鍏?, "鏈€", "浠?, "骞?, "鍙?, "涓?, "浣?, "澶?, "澶?, "灏?, "灏?, "姣?, "鏇?, "姣?, "鐐?, "鍙?, "骞?, "姝?, "鍋?, "鎵€", "鐒?, "褰?, "涓?, "鎶?, "琚?, "鍚?, "宸?, "浜?, "鍏?, "浜?, "涓?, "鍓?, "鍚?, "鍙?, "杩?, "閲?, "澶?, "鍐?, "涓?));
        for (LearningJournalAnalysis journal : journalAnalyses) {
            String content = journal.getContent();
            String[] words = content.replaceAll("[\\p{P}\\p{S}]", " ").split("\\s+");
            for (String word : words) {
                word = word.trim().toLowerCase();
                if (!word.isEmpty() && !stopWords.contains(word) && word.length() > 1) {
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }
        List<Map.Entry<String, Integer>> topKeywords = wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
        Map<String, Integer> topKeywordsMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : topKeywords) {
            topKeywordsMap.put(entry.getKey(), entry.getValue());
        }
        analysis.put("questionId", questionId);
        analysis.put("journalCount", journalAnalyses.size());
        analysis.put("topKeywords", topKeywordsMap);
        return analysis;
    }
    @Override
    public Map<String, Object> analyzeJournalContentSentiment(Long questionId) {
        Map<String, Object> analysis = new HashMap<>();
        List<LearningJournalAnalysis> journalAnalyses = journalAnalysisRepository.findByQuestionId(questionId);
        Set<String> positiveWords = new HashSet<>(Arrays.asList(
                "濂?, "鍠滄", "妫?, "浼樼", "娓呮櫚", "鐞嗚В", "鎺屾彙", "杩涙", "鎻愰珮", "鎴愬姛", "鏈夎叮", "鏈夌敤", "甯姪", "瀛︿細", "鏄庣櫧", "鎰熻阿", "婊℃剰", "寮€蹇?, "蹇箰", "楂樺叴", "绮惧僵", "浼樿川", "鏄撴噦", "绠€鍗?, "渚夸簬", "鍙楃泭", "鏀惰幏", "鍚彂", "绐佺牬", "瑙ｅ喅"));
        Set<String> negativeWords = new HashSet<>(Arrays.asList(
                "闅?, "涓嶆噦", "鍥伴毦", "澶嶆潅", "娣蜂贡", "鍥版儜", "杩锋儜", "澶辫触", "鎸姌", "涓嶆槑鐧?, "涓嶇悊瑙?, "涓嶄細", "涓嶆竻妤?, "涓嶆弧", "宸?, "绯熺硶", "鐤戞儜", "闂", "閿欒", "缂洪櫡", "涓嶈冻", "寮辩偣", "闅剧偣", "闅滅", "鐡堕", "涓嶅枩娆?, "璁ㄥ帉", "鍘岀儲", "鏃犺亰", "鏃犵敤"));
        int totalPositiveCount = 0;
        int totalNegativeCount = 0;
        for (LearningJournalAnalysis journal : journalAnalyses) {
            String content = journal.getContent();
            String[] words = content.replaceAll("[\\p{P}\\p{S}]", " ").split("\\s+");
            int positiveCount = 0;
            int negativeCount = 0;
            for (String word : words) {
                word = word.trim();
                if (!word.isEmpty()) {
                    if (positiveWords.contains(word)) {
                        positiveCount++;
                    } else if (negativeWords.contains(word)) {
                        negativeCount++;
                    }
                }
            }
            totalPositiveCount += positiveCount;
            totalNegativeCount += negativeCount;
        }
        double sentimentScore = 0.0;
        if (totalPositiveCount + totalNegativeCount > 0) {
            sentimentScore = (double) (totalPositiveCount - totalNegativeCount) / (totalPositiveCount + totalNegativeCount);
        }
        analysis.put("questionId", questionId);
        analysis.put("journalCount", journalAnalyses.size());
        analysis.put("positiveWordCount", totalPositiveCount);
        analysis.put("negativeWordCount", totalNegativeCount);
        analysis.put("sentimentScore", sentimentScore); 
        String sentimentCategory;
        if (sentimentScore > 0.3) {
            sentimentCategory = "绉瀬";
        } else if (sentimentScore < -0.3) {
            sentimentCategory = "娑堟瀬";
        } else {
            sentimentCategory = "涓€?;
        }
        analysis.put("sentimentCategory", sentimentCategory);
        return analysis;
    }
} 
