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
        
        // 计算平均内容长度
        double avgContentLength = journalAnalyses.stream()
                .mapToInt(journal -> journal.getContent().length())
                .average()
                .orElse(0);
        statistics.put("averageContentLength", avgContentLength);
        
        // 获取最新的日志分析
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
        
        // 计算平均内容长度
        double avgContentLength = journalAnalyses.stream()
                .mapToInt(journal -> journal.getContent().length())
                .average()
                .orElse(0);
        statistics.put("averageContentLength", avgContentLength);
        
        // 按问题ID分组统计
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
        
        // 计算平均内容长度
        double avgContentLength = journalAnalyses.stream()
                .mapToInt(journal -> journal.getContent().length())
                .average()
                .orElse(0);
        statistics.put("averageContentLength", avgContentLength);
        
        // 按课时节点ID分组统计
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
        
        // 计算平均内容长度
        double avgContentLength = journalAnalyses.stream()
                .mapToInt(journal -> journal.getContent().length())
                .average()
                .orElse(0);
        statistics.put("averageContentLength", avgContentLength);
        
        // 按问题ID分组统计
        Map<Long, Long> journalCountByQuestion = journalAnalyses.stream()
                .collect(Collectors.groupingBy(
                        journal -> journal.getQuestion().getId(),
                        Collectors.counting()
                ));
        
        // 获取日志最多的前5个问题
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
        
        // 简单的关键词分析（实际应用中可能需要更复杂的NLP处理）
        Map<String, Integer> wordFrequency = new HashMap<>();
        
        // 停用词列表（实际应用中可能需要更完整的停用词表）
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "的", "了", "和", "是", "在", "我", "有", "这", "个", "那", "就", "不", "人", "都", "一", "为", "以", "要", "上", "他", "而", "使", "也", "你", "到", "如", "被", "它", "所", "等", "之", "与", "可", "能", "来", "对", "时", "自", "会", "用", "行", "方", "还", "作", "生", "能", "去", "里", "后", "多", "么", "得", "说", "于", "好", "看", "想", "么", "没", "到", "很", "再", "吗", "把", "给", "让", "但", "却", "什", "着", "只", "些", "如", "她", "因", "有", "什", "么", "样", "已", "从", "做", "中", "或", "由", "这", "其", "最", "们", "并", "可", "为", "何", "太", "大", "小", "将", "每", "更", "比", "点", "又", "并", "此", "做", "所", "然", "当", "与", "把", "被", "向", "已", "于", "其", "些", "下", "前", "后", "又", "过", "里", "外", "内", "中"));
        
        for (LearningJournalAnalysis journal : journalAnalyses) {
            String content = journal.getContent();
            // 简单分词（按空格和标点符号分割）
            String[] words = content.replaceAll("[\\p{P}\\p{S}]", " ").split("\\s+");
            
            for (String word : words) {
                word = word.trim().toLowerCase();
                if (!word.isEmpty() && !stopWords.contains(word) && word.length() > 1) {
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }
        
        // 获取出现频率最高的前10个关键词
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
        
        // 简单的情感分析（实际应用中可能需要更复杂的NLP处理或第三方API）
        // 这里使用简单的积极词汇和消极词汇列表进行匹配
        
        Set<String> positiveWords = new HashSet<>(Arrays.asList(
                "好", "喜欢", "棒", "优秀", "清晰", "理解", "掌握", "进步", "提高", "成功", "有趣", "有用", "帮助", "学会", "明白", "感谢", "满意", "开心", "快乐", "高兴", "精彩", "优质", "易懂", "简单", "便于", "受益", "收获", "启发", "突破", "解决"));
        
        Set<String> negativeWords = new HashSet<>(Arrays.asList(
                "难", "不懂", "困难", "复杂", "混乱", "困惑", "迷惑", "失败", "挫折", "不明白", "不理解", "不会", "不清楚", "不满", "差", "糟糕", "疑惑", "问题", "错误", "缺陷", "不足", "弱点", "难点", "障碍", "瓶颈", "不喜欢", "讨厌", "厌烦", "无聊", "无用"));
        
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
        analysis.put("sentimentScore", sentimentScore); // 范围从-1（非常消极）到1（非常积极）
        
        String sentimentCategory;
        if (sentimentScore > 0.3) {
            sentimentCategory = "积极";
        } else if (sentimentScore < -0.3) {
            sentimentCategory = "消极";
        } else {
            sentimentCategory = "中性";
        }
        analysis.put("sentimentCategory", sentimentCategory);
        
        return analysis;
    }
} 