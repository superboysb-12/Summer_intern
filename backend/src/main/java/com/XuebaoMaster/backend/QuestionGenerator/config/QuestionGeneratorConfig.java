package com.XuebaoMaster.backend.QuestionGenerator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class QuestionGeneratorConfig {

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.api.key:sk-6bfa57029fbc44b9ac4eb38dd61bc695}")
    private String token;

    @Value("${deepseek.api.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.api.userid:user-1}")
    private String userId;

    @Value("${deepseek.api.temperature:0.7}")
    private Double temperature;

    @Value("${deepseek.api.max-tokens:2048}")
    private Integer maxTokens;

    // 题目类型定义
    private static final String[] QUESTION_TYPES = {
            "选择题",
            "判断题",
            "问答题",
            "编程题"
    };

    // 系统提示词模板
    private static final String SYSTEM_PROMPT_TEMPLATE = "你是一个专业的课程题目生成专家。请根据给定的信息生成高质量的{question_type}题目。";

    // 提示词模板
    private static final Map<String, String> QUESTION_TYPE_PROMPTS = new HashMap<>();

    {
        // 选择题提示词模板
        QUESTION_TYPE_PROMPTS.put("选择题", """
                请根据以下信息生成一道选择题：

                查询词：{query}
                知识图谱信息：
                {rag_results}

                要求：
                1. 生成一个与主题相关的选择题
                2. 提供4个选项，只有1个正确答案
                3. 解释为什么其他选项不正确
                4. 输出JSON格式如下:
                {
                    "type": "选择题",
                    "question": "问题描述",
                    "options": {
                        "A": "选项A",
                        "B": "选项B",
                        "C": "选项C",
                        "D": "选项D"
                    },
                    "answer": "正确选项字母",
                    "explanation": "解析"
                }
                """);

        // 判断题提示词模板
        QUESTION_TYPE_PROMPTS.put("判断题", """
                请根据以下信息生成一道判断题：

                查询词：{query}
                知识图谱信息：
                {rag_results}

                要求：
                1. 生成一个与主题相关的判断题
                2. 判断题的答案应该是"正确"或"错误"
                3. 给出为什么判断正确或错误的解释
                4. 输出JSON格式如下:
                {
                    "type": "判断题",
                    "statement": "判断陈述",
                    "answer": "正确或错误",
                    "explanation": "解析"
                }
                """);

        // 问答题提示词模板
        QUESTION_TYPE_PROMPTS.put("问答题", """
                请根据以下信息生成一道问答题：

                查询词：{query}
                知识图谱信息：
                {rag_results}

                要求：
                1. 生成一个与主题相关的开放式问答题
                2. 提供一个标准答案
                3. 输出JSON格式如下:
                {
                    "type": "问答题",
                    "question": "问题描述",
                    "answer": "标准答案",
                    "key_points": ["关键点1", "关键点2", "关键点3"]
                }
                """);

        // 编程题提示词模板
        QUESTION_TYPE_PROMPTS.put("编程题", """
                请根据以下信息生成一道编程题：

                查询词：{query}
                知识图谱信息：
                {rag_results}

                要求：
                1. 生成一个与主题相关的编程题
                2. 提供问题描述、输入输出要求和示例
                3. 提供解题思路和参考代码
                4. 输出JSON格式如下:
                {
                    "type": "编程题",
                    "title": "题目标题",
                    "description": "问题描述",
                    "input_format": "输入格式说明",
                    "output_format": "输出格式说明",
                    "examples": [
                        {"input": "示例输入1", "output": "示例输出1"},
                        {"input": "示例输入2", "output": "示例输出2"}
                    ],
                    "solution_approach": "解题思路",
                    "reference_code": "参考代码（可包含多种语言实现）"
                }
                """);
    }

    @Bean(name = "questionGeneratorTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("QuestionGen-");
        executor.initialize();
        return executor;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getToken() {
        return token;
    }

    public String getModel() {
        return model;
    }

    public String getUserId() {
        return userId;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public static String[] getQuestionTypes() {
        return QUESTION_TYPES;
    }

    public static String getSystemPromptTemplate() {
        return SYSTEM_PROMPT_TEMPLATE;
    }

    public static Map<String, String> getQuestionTypePrompts() {
        return QUESTION_TYPE_PROMPTS;
    }

    public static String getPromptForType(String questionType, String query, String ragResults) {
        String promptTemplate = QUESTION_TYPE_PROMPTS.getOrDefault(questionType,
                QUESTION_TYPE_PROMPTS.get("问答题")); // 默认使用问答题模板

        return promptTemplate
                .replace("{query}", query)
                .replace("{rag_results}", ragResults);
    }

    public static String getSystemPrompt(String questionType) {
        return SYSTEM_PROMPT_TEMPLATE.replace("{question_type}", questionType);
    }
}