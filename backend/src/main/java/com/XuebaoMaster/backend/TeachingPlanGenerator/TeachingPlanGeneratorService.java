package com.XuebaoMaster.backend.TeachingPlanGenerator;

import java.util.List;
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