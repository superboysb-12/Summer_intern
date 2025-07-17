package com.XuebaoMaster.backend.QuestionGenerator.impl;

import com.XuebaoMaster.backend.QuestionGenerator.QuestionGenerator;
import com.XuebaoMaster.backend.QuestionGenerator.QuestionGeneratorRepository;
import com.XuebaoMaster.backend.QuestionGenerator.QuestionGeneratorService;
import com.XuebaoMaster.backend.QuestionGenerator.config.QuestionGeneratorConfig;
import com.XuebaoMaster.backend.QuestionGenerator.model.DeepSeekRequest;
import com.XuebaoMaster.backend.QuestionGenerator.model.DeepSeekResponse;
import com.XuebaoMaster.backend.RAG.RAG;
import com.XuebaoMaster.backend.RAG.RAGRepository;
import com.XuebaoMaster.backend.RAG.RAGResponse;
import com.XuebaoMaster.backend.RAG.RAGService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class QuestionGeneratorServiceImpl implements QuestionGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(QuestionGeneratorServiceImpl.class);

    @Autowired
    private QuestionGeneratorRepository questionGeneratorRepository;

    @Autowired
    private RAGRepository ragRepository;

    @Autowired
    private RAGService ragService;

    @Autowired
    private QuestionGeneratorConfig config;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Long generateQuestion(String query, String questionType) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("检索词不能为空");
        }

        // 如果未指定题目类型，随机选择一个
        if (questionType == null || questionType.trim().isEmpty()) {
            String[] availableTypes = getAvailableQuestionTypes();
            questionType = availableTypes[new Random().nextInt(availableTypes.length)];
        }

        // 创建并保存题目生成任务
        QuestionGenerator questionGenerator = new QuestionGenerator();
        questionGenerator.setQuery(query);
        questionGenerator.setQuestionType(questionType);
        questionGenerator.setStatus("PENDING");
        questionGenerator.setStatusMessage("题目生成任务已提交，等待处理");
        QuestionGenerator savedEntity = questionGeneratorRepository.save(questionGenerator);

        // 异步执行题目生成
        generateQuestionAsync(savedEntity.getId(), query, null, null, questionType);

        return savedEntity.getId();
    }

    @Override
    public Long generateQuestionWithRagId(String query, Long ragId, String questionType) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("检索词不能为空");
        }

        if (ragId == null) {
            throw new IllegalArgumentException("RAG ID不能为空");
        }

        // 查找指定的RAG
        Optional<RAG> optionalRag = ragRepository.findById(ragId);
        if (optionalRag.isEmpty()) {
            throw new IllegalArgumentException("找不到ID为" + ragId + "的RAG");
        }

        RAG rag = optionalRag.get();
        if (!RAG.RAGStatus.COMPLETED.equals(rag.getStatus())) {
            throw new IllegalArgumentException("指定的RAG尚未完成生成，当前状态: " + rag.getStatus());
        }

        // 如果未指定题目类型，随机选择一个
        if (questionType == null || questionType.trim().isEmpty()) {
            String[] availableTypes = getAvailableQuestionTypes();
            questionType = availableTypes[new Random().nextInt(availableTypes.length)];
        }

        // 创建并保存题目生成任务
        QuestionGenerator questionGenerator = new QuestionGenerator();
        questionGenerator.setQuery(query);
        questionGenerator.setRagId(ragId);
        questionGenerator.setRagName(rag.getName());
        questionGenerator.setQuestionType(questionType);
        questionGenerator.setStatus("PENDING");
        questionGenerator.setStatusMessage("题目生成任务已提交，等待处理");
        QuestionGenerator savedEntity = questionGeneratorRepository.save(questionGenerator);

        // 异步执行题目生成
        generateQuestionAsync(savedEntity.getId(), query, ragId, null, questionType);

        return savedEntity.getId();
    }

    @Override
    public Long generateQuestionWithRagName(String query, String ragName, String questionType) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("检索词不能为空");
        }

        if (ragName == null || ragName.trim().isEmpty()) {
            throw new IllegalArgumentException("RAG名称不能为空");
        }

        // 查找指定的RAG
        Optional<RAG> optionalRag = ragRepository.findByName(ragName);
        if (optionalRag.isEmpty()) {
            throw new IllegalArgumentException("找不到名为" + ragName + "的RAG");
        }

        RAG rag = optionalRag.get();
        if (!RAG.RAGStatus.COMPLETED.equals(rag.getStatus())) {
            throw new IllegalArgumentException("指定的RAG尚未完成生成，当前状态: " + rag.getStatus());
        }

        // 如果未指定题目类型，随机选择一个
        if (questionType == null || questionType.trim().isEmpty()) {
            String[] availableTypes = getAvailableQuestionTypes();
            questionType = availableTypes[new Random().nextInt(availableTypes.length)];
        }

        // 创建并保存题目生成任务
        QuestionGenerator questionGenerator = new QuestionGenerator();
        questionGenerator.setQuery(query);
        questionGenerator.setRagId(rag.getId());
        questionGenerator.setRagName(ragName);
        questionGenerator.setQuestionType(questionType);
        questionGenerator.setStatus("PENDING");
        questionGenerator.setStatusMessage("题目生成任务已提交，等待处理");
        QuestionGenerator savedEntity = questionGeneratorRepository.save(questionGenerator);

        // 异步执行题目生成
        generateQuestionAsync(savedEntity.getId(), query, rag.getId(), ragName, questionType);

        return savedEntity.getId();
    }

    @Override
    public Optional<QuestionGenerator> getQuestionGeneratorById(Long id) {
        return questionGeneratorRepository.findById(id);
    }

    @Override
    public List<QuestionGenerator> getAllQuestionGenerators() {
        return questionGeneratorRepository.findAll();
    }

    @Override
    public String[] getAvailableQuestionTypes() {
        return QuestionGeneratorConfig.getQuestionTypes();
    }

    @Override
    public QuestionGenerator updateQuestionJson(Long id, String questionJson) {
        Optional<QuestionGenerator> optionalEntity = questionGeneratorRepository.findById(id);
        if (optionalEntity.isEmpty()) {
            throw new IllegalArgumentException("找不到ID为" + id + "的题目生成任务");
        }

        QuestionGenerator entity = optionalEntity.get();

        // 校验JSON格式
        try {
            JsonNode jsonNode = objectMapper.readTree(questionJson);
            // 这里可以添加更多的JSON校验逻辑
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("提供的JSON格式无效: " + e.getMessage());
        }

        entity.setQuestionJson(questionJson);
        return questionGeneratorRepository.save(entity);
    }

    @Override
    public QuestionGenerator updateQuestionProperties(Long id, String query, String questionType, String questionJson) {
        Optional<QuestionGenerator> optionalEntity = questionGeneratorRepository.findById(id);
        if (optionalEntity.isEmpty()) {
            throw new IllegalArgumentException("找不到ID为" + id + "的题目生成任务");
        }

        QuestionGenerator entity = optionalEntity.get();

        // 更新检索词
        if (query != null && !query.trim().isEmpty()) {
            entity.setQuery(query);
        }

        // 更新题目类型
        if (questionType != null && !questionType.trim().isEmpty()) {
            // 验证题目类型是否有效
            boolean isValidType = Arrays.asList(getAvailableQuestionTypes()).contains(questionType);
            if (!isValidType) {
                throw new IllegalArgumentException("无效的题目类型: " + questionType);
            }
            entity.setQuestionType(questionType);
        }

        // 更新题目JSON
        if (questionJson != null && !questionJson.trim().isEmpty()) {
            // 校验JSON格式
            try {
                JsonNode jsonNode = objectMapper.readTree(questionJson);
                entity.setQuestionJson(questionJson);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("提供的JSON格式无效: " + e.getMessage());
            }
        }

        return questionGeneratorRepository.save(entity);
    }

    @Override
    public Map<String, Object> generateSolution(Long id) {
        // 查找题目
        Optional<QuestionGenerator> optionalEntity = questionGeneratorRepository.findById(id);
        if (optionalEntity.isEmpty()) {
            throw new IllegalArgumentException("找不到ID为" + id + "的题目生成任务");
        }

        QuestionGenerator entity = optionalEntity.get();

        // 确认题目已生成完成
        if (!"COMPLETED".equals(entity.getStatus()) || entity.getQuestionJson() == null
                || entity.getQuestionJson().isEmpty()) {
            throw new IllegalArgumentException("题目尚未生成完成或缺少题目内容");
        }

        try {
            // 解析题目JSON
            JsonNode questionNode = objectMapper.readTree(entity.getQuestionJson());
            String questionType = questionNode.has("type") ? questionNode.get("type").asText()
                    : entity.getQuestionType();

            // 构建提示词，要求解答题目
            String prompt = buildSolutionPrompt(questionType, entity.getQuestionJson());

            // 准备向DeepSeek发送请求
            DeepSeekRequest request = new DeepSeekRequest();
            request.setModel(config.getModel());
            request.setTemperature(0.5); // 降低温度，使解答更确定性
            request.setMax_tokens(config.getMaxTokens());

            // 构建消息
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", "你是一个专业的教育助手，善于解答各类题目。请提供详细的解题过程和思路。"));
            messages.add(Map.of("role", "user", "content", prompt));
            request.setMessages(messages);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getToken());
            HttpEntity<DeepSeekRequest> requestEntity = new HttpEntity<>(request, headers);

            // 发送请求
            logger.info("开始调用DeepSeek API生成题目解答");
            ResponseEntity<DeepSeekResponse> responseEntity = restTemplate.exchange(
                    config.getApiUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    DeepSeekResponse.class);

            // 处理响应
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                DeepSeekResponse response = responseEntity.getBody();
                if (response != null && response.getChoices() != null && response.getChoices().length > 0) {
                    String solution = response.getChoices()[0].getMessage().getContent();

                    // 返回结果
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("id", entity.getId());
                    result.put("questionType", questionType);
                    result.put("solution", solution);
                    return result;
                }
            }

            throw new RuntimeException("调用解答服务失败");
        } catch (Exception e) {
            logger.error("生成题目解答时发生错误: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "生成题目解答失败: " + e.getMessage());
            return errorResult;
        }
    }

    @Override
    public List<Long> deleteQuestions(List<Long> ids) {
        List<Long> deletedIds = new ArrayList<>();

        for (Long id : ids) {
            try {
                if (questionGeneratorRepository.existsById(id)) {
                    questionGeneratorRepository.deleteById(id);
                    deletedIds.add(id);
                }
            } catch (Exception e) {
                logger.error("删除题目 {} 时发生错误: {}", id, e.getMessage(), e);
            }
        }

        return deletedIds;
    }

    @Async("questionGeneratorTaskExecutor")
    protected void generateQuestionAsync(Long id, String query, Long ragId, String ragName, String questionType) {
        QuestionGenerator entity = questionGeneratorRepository.findById(id).orElse(null);
        if (entity == null) {
            logger.error("无法找到题目生成任务: {}", id);
            return;
        }

        try {
            logger.info("开始执行题目生成任务, ID: {}, 检索词: {}, 题目类型: {}", id, query, questionType);
            entity.setStatusMessage("正在从RAG获取知识图谱信息...");
            questionGeneratorRepository.save(entity);

            // 第一步：调用RAG API获取相关知识
            RAGResponse ragResponse;

            // 根据提供的RAG信息选择查询方式
            if (ragId != null) {
                logger.info("使用RAG ID执行查询: {}, 检索词: {}", ragId, query);
                ragResponse = ragService.performQuery(query, ragId, 5, true, 2);
            } else if (ragName != null && !ragName.isEmpty()) {
                logger.info("使用RAG名称执行查询: {}, 检索词: {}", ragName, query);
                ragResponse = ragService.performQueryByName(query, ragName, 5, true, 2);
            } else {
                // 如果没有指定RAG，尝试使用第一个可用的RAG
                logger.info("未指定RAG，尝试使用默认RAG执行查询, 检索词: {}", query);
                List<RAG> availableRags = ragRepository.findAll();
                if (availableRags.isEmpty()) {
                    logger.error("没有可用的RAG");
                    entity.setStatus("FAILED");
                    entity.setStatusMessage("没有可用的RAG");
                    questionGeneratorRepository.save(entity);
                    return;
                }

                // 使用第一个已完成的RAG
                Optional<RAG> completedRag = availableRags.stream()
                        .filter(rag -> RAG.RAGStatus.COMPLETED.equals(rag.getStatus()))
                        .findFirst();

                if (completedRag.isEmpty()) {
                    logger.error("没有已完成生成的RAG");
                    entity.setStatus("FAILED");
                    entity.setStatusMessage("没有已完成生成的RAG");
                    questionGeneratorRepository.save(entity);
                    return;
                }

                RAG rag = completedRag.get();
                ragId = rag.getId();
                ragName = rag.getName();
                entity.setRagId(ragId);
                entity.setRagName(ragName);

                logger.info("选择默认RAG: {} (ID: {}), 检索词: {}", ragName, ragId, query);
                ragResponse = ragService.performQuery(query, ragId, 5, true, 2);
            }

            // 检查RAG查询结果
            if (ragResponse == null || !ragResponse.isSuccess()) {
                String errorMessage = ragResponse != null ? ragResponse.getMessage() : "未知错误";
                logger.error("RAG查询失败: {}", errorMessage);
                entity.setStatus("FAILED");
                entity.setStatusMessage("RAG查询失败: " + errorMessage);
                questionGeneratorRepository.save(entity);
                return;
            }

            // 格式化RAG结果
            String formattedRagResults = formatRagResults(ragResponse);
            logger.info("RAG查询成功，已格式化结果");

            // 更新状态：准备生成题目
            entity.setStatusMessage("正在使用DeepSeek API生成题目...");
            questionGeneratorRepository.save(entity);

            // 第二步：准备DeepSeek API的请求
            String systemPrompt = QuestionGeneratorConfig.getSystemPrompt(questionType);
            String userPrompt = QuestionGeneratorConfig.getPromptForType(questionType, query, formattedRagResults);

            logger.info("准备调用DeepSeek API, URL: {}", config.getApiUrl());

            // 构建请求体
            DeepSeekRequest request = new DeepSeekRequest();
            request.setModel(config.getModel());
            request.setTemperature(config.getTemperature());
            request.setMax_tokens(config.getMaxTokens());
            request.setTop_p(0.95);

            // 构建消息
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));
            request.setMessages(messages);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getToken());
            HttpEntity<DeepSeekRequest> requestEntity = new HttpEntity<>(request, headers);

            // 发送请求
            logger.info("开始调用DeepSeek API生成题目");
            ResponseEntity<DeepSeekResponse> responseEntity = restTemplate.exchange(
                    config.getApiUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    DeepSeekResponse.class);

            // 处理响应
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                DeepSeekResponse response = responseEntity.getBody();
                if (response != null && response.getChoices() != null && response.getChoices().length > 0) {
                    DeepSeekResponse.Message message = response.getChoices()[0].getMessage();
                    String content = message.getContent();

                    logger.debug("DeepSeek API返回内容: {}", content);

                    // 提取JSON内容
                    String extractedJson = extractJsonFromText(content);
                    if (extractedJson != null) {
                        // 校验JSON格式
                        try {
                            JsonNode parsedJson = objectMapper.readTree(extractedJson);

                            // 更新实体
                            entity.setQuestionJson(extractedJson);
                            entity.setStatus("COMPLETED");
                            entity.setStatusMessage("题目生成成功");
                            questionGeneratorRepository.save(entity);
                            logger.info("题目生成成功, ID: {}", id);
                        } catch (JsonProcessingException e) {
                            logger.error("解析生成的JSON失败: {}", e.getMessage());
                            entity.setStatus("FAILED");
                            entity.setStatusMessage("解析生成的JSON失败: " + e.getMessage());
                            questionGeneratorRepository.save(entity);
                        }
                    } else {
                        logger.error("无法从DeepSeek响应中提取JSON");
                        entity.setStatus("FAILED");
                        entity.setStatusMessage("无法从DeepSeek响应中提取JSON");
                        entity.setQuestionJson(content); // 存储原始内容以便调试
                        questionGeneratorRepository.save(entity);
                    }
                } else {
                    logger.error("DeepSeek响应格式无效");
                    entity.setStatus("FAILED");
                    entity.setStatusMessage("DeepSeek响应格式无效");
                    questionGeneratorRepository.save(entity);
                }
            } else {
                logger.error("DeepSeek API请求失败: {}", responseEntity.getStatusCode());
                entity.setStatus("FAILED");
                entity.setStatusMessage("DeepSeek API请求失败: " + responseEntity.getStatusCode());
                questionGeneratorRepository.save(entity);
            }
        } catch (Exception e) {
            logger.error("生成题目时发生错误: {}", e.getMessage(), e);
            entity.setStatus("FAILED");
            entity.setStatusMessage("生成题目时发生错误: " + e.getMessage());
            questionGeneratorRepository.save(entity);
        }
    }

    /**
     * 格式化RAG查询结果为提示词所需格式
     */
    private String formatRagResults(RAGResponse ragResponse) {
        StringBuilder formattedText = new StringBuilder();

        try {
            // 获取结果列表，使用toString()方法避免类型转换问题
            List<Map<String, Object>> results = new ArrayList<>();
            Object rawData = ragResponse.getData();

            // 使用JSON序列化和反序列化来安全地转换数据类型
            String jsonString = objectMapper.writeValueAsString(rawData);
            Map<String, Object> data = objectMapper.readValue(jsonString, Map.class);

            if (data != null && data.containsKey("results")) {
                // 将results转换为List<Map<String, Object>>
                List<Object> resultsList = objectMapper.convertValue(data.get("results"), List.class);

                int idx = 0;
                for (Object resultObj : resultsList) {
                    idx++;
                    Map<String, Object> result = objectMapper.convertValue(resultObj, Map.class);

                    // 添加主要知识点
                    formattedText.append("知识点 ").append(idx).append(": ");
                    formattedText.append(result.get("subject")).append(" ");
                    formattedText.append(result.get("relation")).append(" ");
                    formattedText.append(result.get("object")).append("\n");

                    // 添加关联知识
                    if (result.containsKey("outgoing_relations")) {
                        List<Object> relationsList = objectMapper.convertValue(
                                result.get("outgoing_relations"), List.class);

                        if (relationsList != null && !relationsList.isEmpty()) {
                            formattedText.append("相关知识:\n");

                            for (Object relationObj : relationsList) {
                                Map<String, Object> relation = objectMapper.convertValue(relationObj, Map.class);

                                formattedText.append("- ");
                                formattedText.append(result.get("subject")).append(" ");
                                formattedText.append(relation.get("relation")).append(" ");
                                formattedText.append(relation.get("target")).append("\n");
                            }
                        }
                    }
                    formattedText.append("\n");
                }
            }
        } catch (Exception e) {
            logger.error("格式化RAG结果出错: {}", e.getMessage(), e);
            formattedText.append("无法格式化RAG结果: ").append(e.getMessage());
        }

        return formattedText.toString();
    }

    /**
     * 从文本中提取JSON内容
     */
    private String extractJsonFromText(String text) {
        // 首先尝试直接解析整个文本
        try {
            objectMapper.readTree(text);
            return text; // 整个文本就是有效的JSON
        } catch (JsonProcessingException e) {
            // 如果整个文本不是JSON，尝试提取JSON部分
        }

        // 使用正则表达式寻找JSON对象
        Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String jsonString = matcher.group();
            // 校验提取的JSON是否有效
            try {
                objectMapper.readTree(jsonString);
                return jsonString;
            } catch (JsonProcessingException e) {
                logger.warn("提取的JSON格式无效: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * 构建题目解答的提示词
     */
    private String buildSolutionPrompt(String questionType, String questionJson) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请解答以下").append(questionType).append("，并提供详细的解题思路和过程：\n\n");
        prompt.append(questionJson);

        // 根据题目类型添加特定的解答要求
        switch (questionType) {
            case "选择题":
                prompt.append("\n\n请解释为什么选择该答案，并分析其他选项错误的原因。");
                break;
            case "判断题":
                prompt.append("\n\n请详细说明判断的理由，并提供相关的知识点解释。");
                break;
            case "问答题":
                prompt.append("\n\n请提供完整详细的答案，并列出相关的知识点和概念。");
                break;
            case "编程题":
                prompt.append("\n\n请提供详细的解题思路、算法分析，以及多种语言的完整实现代码。");
                break;
        }

        return prompt.toString();
    }
}