package com.XuebaoMaster.backend.StudentEmotion.config;

import com.XuebaoMaster.backend.StudentEmotion.AutoEmotionScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 情绪评分定时任务配置
 * 定期自动计算用户情绪评分
 */
@Configuration
@EnableScheduling
public class EmotionScoreSchedulerConfig {

    private static final Logger logger = LoggerFactory.getLogger(EmotionScoreSchedulerConfig.class);

    @Autowired
    private AutoEmotionScoreService autoEmotionScoreService;

    /**
     * 每天晚上23:00执行情绪评分计算
     */
    @Scheduled(cron = "0 0 23 * * ?")
    public void scheduleDailyEmotionScoring() {
        logger.info("开始执行定时情绪评分任务");
        autoEmotionScoreService.calculateDailyEmotionScores();
        logger.info("定时情绪评分任务完成");
    }

    /**
     * 系统启动后延迟5分钟执行一次评分（可选）
     * 适用于开发测试或系统恢复后补充评分
     */
    @Scheduled(initialDelay = 300000, fixedDelay = Long.MAX_VALUE)
    public void scheduleStartupEmotionScoring() {
        logger.info("系统启动后执行情绪评分初始化");
        autoEmotionScoreService.calculateDailyEmotionScores();
        logger.info("情绪评分初始化完成");
    }
}