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
}