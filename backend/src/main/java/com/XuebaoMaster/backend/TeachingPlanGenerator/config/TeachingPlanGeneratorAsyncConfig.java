package com.XuebaoMaster.backend.TeachingPlanGenerator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class TeachingPlanGeneratorAsyncConfig {

    @Bean("teachingPlanGeneratorTaskExecutor")
    public Executor teachingPlanGeneratorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("teaching-plan-generator-");
        executor.initialize();
        return executor;
    }
}