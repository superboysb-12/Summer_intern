package com.XuebaoMaster.backend.QuestionGenerator;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface QuestionGeneratorService {

    /**
     * 异步生成题目
     * 
     * @param query        检索词
     * @param questionType 题目类型
     * @return 返回题目生成任务ID
     */
    Long generateQuestion(String query, String questionType);

    /**
     * 使用指定的RAG异步生成题目
     * 
     * @param query        检索词
     * @param ragId        RAG的ID
     * @param questionType 题目类型
     * @return 返回题目生成任务ID
     */
    Long generateQuestionWithRagId(String query, Long ragId, String questionType);

    /**
     * 使用指定的RAG名称异步生成题目
     * 
     * @param query        检索词
     * @param ragName      RAG的名称
     * @param questionType 题目类型
     * @return 返回题目生成任务ID
     */
    Long generateQuestionWithRagName(String query, String ragName, String questionType);

    /**
     * 获取题目生成任务
     * 
     * @param id 题目生成任务ID
     * @return 题目生成实体
     */
    Optional<QuestionGenerator> getQuestionGeneratorById(Long id);

    /**
     * 获取所有题目生成任务
     * 
     * @return 所有题目生成任务列表
     */
    List<QuestionGenerator> getAllQuestionGenerators();

    /**
     * 获取所有可用的题目类型
     * 
     * @return 题目类型列表
     */
    String[] getAvailableQuestionTypes();

    /**
     * 更新题目JSON内容
     * 
     * @param id           题目生成任务ID
     * @param questionJson 新的题目JSON内容
     * @return 更新后的题目生成实体
     */
    QuestionGenerator updateQuestionJson(Long id, String questionJson);

    /**
     * 更新题目属性（包括检索词、题目类型等）
     * 
     * @param id           题目生成任务ID
     * @param query        更新后的检索词
     * @param questionType 更新后的题目类型
     * @param questionJson 更新后的题目JSON内容
     * @return 更新后的题目生成实体
     */
    QuestionGenerator updateQuestionProperties(Long id, String query, String questionType, String questionJson);

    /**
     * 使用DeepSeek生成题目解答
     * 
     * @param id 题目生成任务ID
     * @return 包含解答内容的Map
     */
    Map<String, Object> generateSolution(Long id);

    /**
     * 批量删除题目
     * 
     * @param ids 要删除的题目ID列表
     * @return 删除成功的题目ID列表
     */
    List<Long> deleteQuestions(List<Long> ids);

    /**
     * 开始在线设计课后练习
     * 
     * @param id 题目生成任务ID
     * @return 包含题目内容的响应
     */
    Map<String, Object> startDesign(Long id);

    /**
     * 保存设计中的课后练习内容
     * 
     * @param id            题目生成任务ID
     * @param designContent 设计内容
     * @return 保存结果
     */
    Map<String, Object> saveDesignContent(Long id, String designContent);

    /**
     * 完成课后练习设计并计算效率指数
     * 
     * @param id 题目生成任务ID
     * @return 包含效率指数的响应
     */
    Map<String, Object> finishDesign(Long id);

    /**
     * 获取课后练习设计效率统计数据
     * 
     * @return 效率统计数据
     */
    Map<String, Object> getEfficiencyStatistics();

    /**
     * 获取课后练习优化建议
     * 
     * @param id 题目生成任务ID
     * @return 优化建议
     */
    Map<String, Object> getOptimizationSuggestions(Long id);
}