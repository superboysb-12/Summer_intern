package com.XuebaoMaster.backend.LearningJournalAnalysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learning-journal-analyses")
public class LearningJournalAnalysisController {

    @Autowired
    private LearningJournalAnalysisService journalAnalysisService;

    /**
     * 创建学习日志分析记录
     * 
     * @param request 包含questionId和content的请求体
     * @return 创建的学习日志分析记录
     */
    @PostMapping
    public ResponseEntity<LearningJournalAnalysis> createJournalAnalysis(@RequestBody Map<String, Object> request) {
        try {
            Long questionId = Long.valueOf(request.get("questionId").toString());
            String content = request.get("content").toString();
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            LearningJournalAnalysis createdJournalAnalysis = journalAnalysisService.createJournalAnalysis(questionId, content);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdJournalAnalysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新学习日志分析记录
     * 
     * @param id 学习日志分析记录ID
     * @param journalAnalysis 学习日志分析信息
     * @return 更新后的学习日志分析记录
     */
    @PutMapping("/{id}")
    public ResponseEntity<LearningJournalAnalysis> updateJournalAnalysis(
            @PathVariable Long id,
            @RequestBody LearningJournalAnalysis journalAnalysis) {
        try {
            journalAnalysis.setId(id);
            LearningJournalAnalysis updatedJournalAnalysis = journalAnalysisService.updateJournalAnalysis(journalAnalysis);
            return ResponseEntity.ok(updatedJournalAnalysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除学习日志分析记录
     * 
     * @param id 学习日志分析记录ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJournalAnalysis(@PathVariable Long id) {
        try {
            journalAnalysisService.deleteJournalAnalysis(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 根据ID获取学习日志分析记录
     * 
     * @param id 学习日志分析记录ID
     * @return 学习日志分析记录信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<LearningJournalAnalysis> getJournalAnalysisById(@PathVariable Long id) {
        try {
            LearningJournalAnalysis journalAnalysis = journalAnalysisService.getJournalAnalysisById(id);
            return ResponseEntity.ok(journalAnalysis);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 根据问题ID获取学习日志分析记录
     * 
     * @param questionId 问题ID
     * @return 学习日志分析记录列表
     */
    @GetMapping("/question/{questionId}")
    public ResponseEntity<List<LearningJournalAnalysis>> getJournalAnalysesByQuestionId(@PathVariable Long questionId) {
        try {
            List<LearningJournalAnalysis> journalAnalyses = journalAnalysisService.getJournalAnalysesByQuestionId(questionId);
            return ResponseEntity.ok(journalAnalyses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据课时节点ID获取学习日志分析记录
     * 
     * @param lessonNodeId 课时节点ID
     * @return 学习日志分析记录列表
     */
    @GetMapping("/lesson-node/{lessonNodeId}")
    public ResponseEntity<List<LearningJournalAnalysis>> getJournalAnalysesByLessonNodeId(@PathVariable Long lessonNodeId) {
        try {
            List<LearningJournalAnalysis> journalAnalyses = journalAnalysisService.getJournalAnalysesByLessonNodeId(lessonNodeId);
            return ResponseEntity.ok(journalAnalyses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据课程ID获取学习日志分析记录
     * 
     * @param courseId 课程ID
     * @return 学习日志分析记录列表
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<LearningJournalAnalysis>> getJournalAnalysesByCourseId(@PathVariable Long courseId) {
        try {
            List<LearningJournalAnalysis> journalAnalyses = journalAnalysisService.getJournalAnalysesByCourseId(courseId);
            return ResponseEntity.ok(journalAnalyses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据时间范围获取学习日志分析记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 学习日志分析记录列表
     */
    @GetMapping("/time-range")
    public ResponseEntity<List<LearningJournalAnalysis>> getJournalAnalysesByCreatedAtBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<LearningJournalAnalysis> journalAnalyses = journalAnalysisService.getJournalAnalysesByCreatedAtBetween(startTime, endTime);
            return ResponseEntity.ok(journalAnalyses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据问题ID和时间范围获取学习日志分析记录
     * 
     * @param questionId 问题ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 学习日志分析记录列表
     */
    @GetMapping("/question/{questionId}/time-range")
    public ResponseEntity<List<LearningJournalAnalysis>> getJournalAnalysesByQuestionIdAndCreatedAtBetween(
            @PathVariable Long questionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<LearningJournalAnalysis> journalAnalyses = journalAnalysisService.getJournalAnalysesByQuestionIdAndCreatedAtBetween(
                    questionId, startTime, endTime);
            return ResponseEntity.ok(journalAnalyses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据内容关键词搜索学习日志分析记录
     * 
     * @param keyword 关键词
     * @return 学习日志分析记录列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<LearningJournalAnalysis>> getJournalAnalysesByContentContaining(
            @RequestParam String keyword) {
        try {
            List<LearningJournalAnalysis> journalAnalyses = journalAnalysisService.getJournalAnalysesByContentContaining(keyword);
            return ResponseEntity.ok(journalAnalyses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取所有学习日志分析记录
     * 
     * @return 学习日志分析记录列表
     */
    @GetMapping
    public ResponseEntity<List<LearningJournalAnalysis>> getAllJournalAnalyses() {
        try {
            List<LearningJournalAnalysis> journalAnalyses = journalAnalysisService.getAllJournalAnalyses();
            return ResponseEntity.ok(journalAnalyses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取问题相关的学习日志分析统计信息
     * 
     * @param questionId 问题ID
     * @return 统计信息
     */
    @GetMapping("/statistics/question/{questionId}")
    public ResponseEntity<Map<String, Object>> getJournalAnalysisStatisticsByQuestionId(@PathVariable Long questionId) {
        try {
            Map<String, Object> statistics = journalAnalysisService.getJournalAnalysisStatisticsByQuestionId(questionId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取课时节点相关的学习日志分析统计信息
     * 
     * @param lessonNodeId 课时节点ID
     * @return 统计信息
     */
    @GetMapping("/statistics/lesson-node/{lessonNodeId}")
    public ResponseEntity<Map<String, Object>> getJournalAnalysisStatisticsByLessonNodeId(@PathVariable Long lessonNodeId) {
        try {
            Map<String, Object> statistics = journalAnalysisService.getJournalAnalysisStatisticsByLessonNodeId(lessonNodeId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取课程相关的学习日志分析统计信息
     * 
     * @param courseId 课程ID
     * @return 统计信息
     */
    @GetMapping("/statistics/course/{courseId}")
    public ResponseEntity<Map<String, Object>> getJournalAnalysisStatisticsByCourseId(@PathVariable Long courseId) {
        try {
            Map<String, Object> statistics = journalAnalysisService.getJournalAnalysisStatisticsByCourseId(courseId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取整体学习日志分析统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/statistics/overall")
    public ResponseEntity<Map<String, Object>> getOverallJournalAnalysisStatistics() {
        try {
            Map<String, Object> statistics = journalAnalysisService.getOverallJournalAnalysisStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 分析问题相关学习日志内容的关键词
     * 
     * @param questionId 问题ID
     * @return 关键词分析结果
     */
    @GetMapping("/analyze/keywords/{questionId}")
    public ResponseEntity<Map<String, Object>> analyzeJournalContentKeywords(@PathVariable Long questionId) {
        try {
            Map<String, Object> analysis = journalAnalysisService.analyzeJournalContentKeywords(questionId);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 分析问题相关学习日志内容的情感倾向
     * 
     * @param questionId 问题ID
     * @return 情感分析结果
     */
    @GetMapping("/analyze/sentiment/{questionId}")
    public ResponseEntity<Map<String, Object>> analyzeJournalContentSentiment(@PathVariable Long questionId) {
        try {
            Map<String, Object> analysis = journalAnalysisService.analyzeJournalContentSentiment(questionId);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 