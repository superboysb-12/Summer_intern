package com.XuebaoMaster.backend.TeachingPlanGenerator.impl;

import com.XuebaoMaster.backend.RAG.RAG;
import com.XuebaoMaster.backend.RAG.RAGRepository;
import com.XuebaoMaster.backend.TeachingPlanGenerator.TeachingPlanGenerator;
import com.XuebaoMaster.backend.TeachingPlanGenerator.TeachingPlanGeneratorRepository;
import com.XuebaoMaster.backend.TeachingPlanGenerator.TeachingPlanGeneratorService;
import com.XuebaoMaster.backend.TeachingPlanGenerator.config.TeachingPlanGeneratorConfig;
import com.XuebaoMaster.backend.TeachingPlanGenerator.model.ChatMessageRequest;
import com.XuebaoMaster.backend.TeachingPlanGenerator.model.ChatMessageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TeachingPlanGeneratorServiceImpl implements TeachingPlanGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(TeachingPlanGeneratorServiceImpl.class);

    @Autowired
    private TeachingPlanGeneratorRepository teachingPlanGeneratorRepository;

    @Autowired
    private TeachingPlanGeneratorConfig config;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RAGRepository ragRepository;

    @Override
    public Long generateTeachingPlan(String prompt) {
        return generateTeachingPlan(prompt, new HashMap<>());
    }

    @Override
    public Long generateTeachingPlan(String prompt, Map<String, Object> inputs) {
        // 创建并保存教案生成任务
        TeachingPlanGenerator teachingPlanGenerator = new TeachingPlanGenerator();
        teachingPlanGenerator.setPrompt(prompt);
        teachingPlanGenerator.setStatus("PENDING");
        TeachingPlanGenerator savedEntity = teachingPlanGeneratorRepository.save(teachingPlanGenerator);

        // 异步执行教案生成
        generateTeachingPlanAsync(savedEntity.getId(), prompt, inputs);

        return savedEntity.getId();
    }

    @Override
    public Long generateTeachingPlanWithRagId(String prompt, Long ragId, Map<String, Object> inputs) {
        // 查找指定的RAG
        Optional<RAG> optionalRag = ragRepository.findById(ragId);
        if (optionalRag.isEmpty()) {
            // 创建并保存失败的教案生成任务
            TeachingPlanGenerator teachingPlanGenerator = new TeachingPlanGenerator();
            teachingPlanGenerator.setPrompt(prompt);
            teachingPlanGenerator.setStatus("FAILED");
            teachingPlanGenerator.setMessageId("找不到ID为" + ragId + "的RAG");
            TeachingPlanGenerator savedEntity = teachingPlanGeneratorRepository.save(teachingPlanGenerator);
            return savedEntity.getId();
        }

        RAG rag = optionalRag.get();
        if (!RAG.RAGStatus.COMPLETED.equals(rag.getStatus())) {
            // 创建并保存失败的教案生成任务
            TeachingPlanGenerator teachingPlanGenerator = new TeachingPlanGenerator();
            teachingPlanGenerator.setPrompt(prompt);
            teachingPlanGenerator.setStatus("FAILED");
            teachingPlanGenerator.setMessageId("指定的RAG尚未完成生成，当前状态: " + rag.getStatus());
            TeachingPlanGenerator savedEntity = teachingPlanGeneratorRepository.save(teachingPlanGenerator);
            return savedEntity.getId();
        }

        // 设置RAG路径
        if (inputs == null) {
            inputs = new HashMap<>();
        }
        inputs.put("ragURL", rag.getRagPath());

        // 创建并保存教案生成任务
        TeachingPlanGenerator teachingPlanGenerator = new TeachingPlanGenerator();
        teachingPlanGenerator.setPrompt(prompt);
        teachingPlanGenerator.setStatus("PENDING");
        TeachingPlanGenerator savedEntity = teachingPlanGeneratorRepository.save(teachingPlanGenerator);

        // 异步执行教案生成
        generateTeachingPlanAsync(savedEntity.getId(), prompt, inputs);

        return savedEntity.getId();
    }

    @Override
    public TeachingPlanGenerator getTeachingPlanGeneratorById(Long id) {
        return teachingPlanGeneratorRepository.findById(id).orElse(null);
    }

    @Override
    public List<TeachingPlanGenerator> getAllTeachingPlanGenerators() {
        return teachingPlanGeneratorRepository.findAll();
    }

    @Async("teachingPlanGeneratorTaskExecutor")
    protected void generateTeachingPlanAsync(Long id, String prompt, Map<String, Object> inputs) {
        TeachingPlanGenerator entity = teachingPlanGeneratorRepository.findById(id).orElse(null);
        if (entity == null) {
            logger.error("无法找到教案生成任务: {}", id);
            return;
        }

        try {
            // 准备请求
            String url = config.getApiBaseUrl() + config.getApiEndpoint();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getToken());

            // 构建请求体
            ChatMessageRequest request = new ChatMessageRequest();
            request.setQuery(prompt);
            request.setResponse_mode("streaming");
            request.setConversation_id("");
            request.setUser(config.getUserId());

            // 设置RAG相关参数
            if (inputs != null) {
                // 如果有depth参数
                if (inputs.containsKey("depth")) {
                    request.addInput("depth", inputs.get("depth"));
                }

                // 如果有ragURL参数
                if (inputs.containsKey("ragURL")) {
                    request.addInput("ragURL", inputs.get("ragURL"));
                }

                // 添加其他可能的输入参数
                for (Map.Entry<String, Object> entry : inputs.entrySet()) {
                    if (!entry.getKey().equals("depth") && !entry.getKey().equals("ragURL")) {
                        request.addInput(entry.getKey(), entry.getValue());
                    }
                }
            }

            logger.info("开始向 {} 发送请求生成教案，提示词: {}, RAG配置: {}", url, prompt, inputs);

            // 发送请求
            HttpEntity<ChatMessageRequest> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
                    String.class);

            // 处理响应，寻找最后一个 message_end 事件
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                String responseBody = responseEntity.getBody();
                if (responseBody != null) {
                    // 处理流式响应，找到最后一个 message_end 事件
                    String[] events = responseBody.split("\n\n");
                    ChatMessageResponse finalResponse = null;

                    logger.info("接收到响应，事件数量: {}", events.length);

                    for (String event : events) {
                        if (event.startsWith("data: ")) {
                            String data = event.substring("data: ".length());
                            try {
                                ChatMessageResponse response = objectMapper.readValue(data, ChatMessageResponse.class);
                                if ("message_end".equals(response.getEvent())) {
                                    finalResponse = response;
                                    logger.info("找到 message_end 事件: {}", response.getMessage_id());
                                }
                            } catch (JsonProcessingException e) {
                                logger.warn("无法解析事件数据: {}", e.getMessage());
                            }
                        }
                    }

                    if (finalResponse != null && finalResponse.getFiles() != null
                            && !finalResponse.getFiles().isEmpty()) {
                        // 获取文件信息
                        ChatMessageResponse.FileInfo fileInfo = finalResponse.getFiles().get(0);
                        String fileUrl = config.getApiBaseUrl() + fileInfo.getUrl();
                        String fileName = fileInfo.getFilename();

                        logger.info("获取到文件信息: {}, URL: {}", fileName, fileUrl);

                        // 确保存储目录存在
                        Path storagePath = Paths.get(config.getStorageDirectory());
                        if (!Files.exists(storagePath)) {
                            Files.createDirectories(storagePath);
                            logger.info("创建存储目录: {}", storagePath);
                        }

                        // 构建本地文件路径
                        String filePath = Paths.get(storagePath.toString(), fileName).toString();

                        // 下载文件
                        logger.info("开始下载文件到: {}", filePath);
                        downloadFile(fileUrl, filePath);
                        logger.info("文件下载完成");

                        // 更新实体
                        entity.setFileName(fileName);
                        entity.setFilePath(filePath);
                        entity.setConversationId(finalResponse.getConversation_id());
                        entity.setMessageId(finalResponse.getMessage_id());
                        entity.setStatus("COMPLETED");
                        teachingPlanGeneratorRepository.save(entity);

                        logger.info("教案生成任务完成: {}", id);
                    } else {
                        logger.error("未找到文件信息");
                        entity.setStatus("FAILED");
                        entity.setMessageId("无法获取文件信息");
                        teachingPlanGeneratorRepository.save(entity);
                    }
                } else {
                    logger.error("响应内容为空");
                    entity.setStatus("FAILED");
                    entity.setMessageId("响应内容为空");
                    teachingPlanGeneratorRepository.save(entity);
                }
            } else {
                logger.error("API请求失败: {}", responseEntity.getStatusCode());
                entity.setStatus("FAILED");
                entity.setMessageId("请求失败: " + responseEntity.getStatusCode());
                teachingPlanGeneratorRepository.save(entity);
            }
        } catch (Exception e) {
            logger.error("生成教案过程中发生错误: {}", e.getMessage(), e);
            entity.setStatus("FAILED");
            entity.setMessageId("错误: " + e.getMessage());
            teachingPlanGeneratorRepository.save(entity);
        }
    }

    @Async("teachingPlanGeneratorTaskExecutor")
    protected void generateTeachingPlanAsync(Long id, String prompt) {
        generateTeachingPlanAsync(id, prompt, new HashMap<>());
    }

    private void downloadFile(String fileUrl, String destinationPath) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new URL(fileUrl).openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(destinationPath)) {

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
    }
}