package com.XuebaoMaster.backend.StudentEmotion;

import com.XuebaoMaster.backend.User.User;

/**
 * 自动情绪评分服务接口
 * 基于用户行为数据自动计算情绪评分
 */
public interface AutoEmotionScoreService {
    /**
     * 计算所有活跃用户的每日情绪评分
     */
    void calculateDailyEmotionScores();

    /**
     * 计算单个用户的情绪评分
     * 
     * @param user 用户对象
     * @return 情绪评分（0-100）
     */
    int calculateUserEmotionScore(User user);
}