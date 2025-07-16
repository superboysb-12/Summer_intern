package com.XuebaoMaster.backend.TeachingPlanGenerator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface TeachingPlanGeneratorService {

    /**
     * 异步生成教案
     * 
     * @param prompt 提示词
     * @return 返回教案生成任务ID
     */
    Long generateTeachingPlan(String prompt);

    /**
     * 异步生成教案（支持RAG配置）
     * 
     * @param prompt 提示词
     * @param inputs 额外的输入参数，包含RAG配置
     * @return 返回教案生成任务ID
     */
    Long generateTeachingPlan(String prompt, Map<String, Object> inputs);

    /**
     * 通过RAG ID异步生成教案
     * 
     * @param prompt 提示词
     * @param ragId  RAG的ID
     * @param inputs 其他额外的输入参数
     * @return 返回教案生成任务ID
     */
    Long generateTeachingPlanWithRagId(String prompt, Long ragId, Map<String, Object> inputs);

    /**
     * 获取教案生成状态
     * 
     * @param id 教案生成任务ID
     * @return 教案生成实体
     */
    TeachingPlanGenerator getTeachingPlanGeneratorById(Long id);

    /**
     * 获取所有教案生成任务
     * 
     * @return 所有教案生成任务列表
     */
    List<TeachingPlanGenerator> getAllTeachingPlanGenerators();

    /**
     * 开始在线编辑教案
     * 
     * @param id 教案生成任务ID
     * @return 包含教案内容的实体
     */
    Map<String, Object> startOnlineEdit(Long id);

    /**
     * 保存在线编辑的教案
     * 
     * @param id          教案生成任务ID
     * @param editContent 编辑后的内容
     * @return 更新后的实体
     */
    Map<String, Object> saveOnlineEdit(Long id, String editContent);

    /**
     * 结束在线编辑教案并计算效率指数
     * 
     * @param id 教案生成任务ID
     * @return 更新后的实体，包含效率指数
     */
    Map<String, Object> finishOnlineEdit(Long id);

    /**
     * 获取教学效率统计数据
     * 
     * @return 效率统计数据
     */
    Map<String, Object> getEfficiencyStatistics();

    /**
     * 获取课程优化建议
     * 
     * @param id 教案生成任务ID
     * @return 优化建议
     */
    Map<String, Object> getCourseOptimizationSuggestions(Long id);
}