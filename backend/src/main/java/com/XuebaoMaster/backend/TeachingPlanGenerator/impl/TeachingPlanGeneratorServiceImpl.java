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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Apache POI 依赖
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

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

    @Override
    public Map<String, Object> startOnlineEdit(Long id) {
        Map<String, Object> response = new HashMap<>();

        logger.info("开始在线编辑教案, ID: {}", id);

        Optional<TeachingPlanGenerator> optionalEntity = teachingPlanGeneratorRepository.findById(id);
        if (optionalEntity.isEmpty()) {
            logger.warn("找不到指定的教案生成任务, ID: {}", id);
            response.put("success", false);
            response.put("message", "找不到指定的教案生成任务");
            return response;
        }

        TeachingPlanGenerator entity = optionalEntity.get();
        logger.info("找到教案生成任务, ID: {}, 状态: {}", id, entity.getStatus());

        if (!"COMPLETED".equals(entity.getStatus())) {
            logger.warn("教案尚未生成完成, ID: {}, 当前状态: {}", id, entity.getStatus());
            response.put("success", false);
            response.put("message", "教案尚未生成完成，当前状态: " + entity.getStatus());
            return response;
        }

        try {
            // 读取docx文件内容
            String filePath = entity.getFilePath();
            logger.info("准备读取文件: {}", filePath);

            if (filePath == null) {
                logger.warn("教案文件路径为空, ID: {}", id);
                response.put("success", false);
                response.put("message", "教案文件路径为空");
                return response;
            }

            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                logger.warn("教案文件不存在, ID: {}, 路径: {}", id, filePath);
                response.put("success", false);
                response.put("message", "教案文件不存在: " + filePath);
                return response;
            }

            // 检查文件是否可读
            if (!Files.isReadable(path)) {
                logger.warn("教案文件无法读取, ID: {}, 路径: {}", id, filePath);
                response.put("success", false);
                response.put("message", "教案文件无法读取，请检查文件权限: " + filePath);
                return response;
            }

            // 开始编辑时间
            logger.info("设置编辑开始时间, ID: {}", id);
            entity.setEditStartTime(LocalDateTime.now());
            entity.setEditDuration(0L);

            try {
                entity = teachingPlanGeneratorRepository.save(entity);
                logger.info("已保存编辑开始时间, ID: {}, 时间: {}", id, entity.getEditStartTime());
            } catch (Exception e) {
                logger.error("保存编辑开始时间失败: {}", e.getMessage(), e);
                response.put("success", false);
                response.put("message", "数据库操作失败: " + e.getMessage());
                return response;
            }

            String fileContent;
            // 尝试使用Apache POI读取文件，如果失败则使用备用方法
            try {
                logger.info("开始读取文件内容, ID: {}, 路径: {}", id, filePath);
                fileContent = readDocxContent(filePath);
                logger.info("成功读取文件内容, ID: {}, 内容长度: {}", id, fileContent.length());
            } catch (Exception e) {
                logger.warn("无法使用Apache POI读取docx文件: {}, 尝试使用备用方法", e.getMessage(), e);
                // 备用方法：使用简单文本
                fileContent = "无法解析文档内容，请联系管理员。\n原因: " + e.getMessage();
            }

            // 如果已有编辑内容，则返回编辑内容而不是文件内容
            if (entity.getEditContent() != null && !entity.getEditContent().isEmpty()) {
                logger.info("使用已有的编辑内容, ID: {}, 内容长度: {}", id, entity.getEditContent().length());
                response.put("content", entity.getEditContent());
            } else {
                logger.info("初始化编辑内容, ID: {}", id);
                response.put("content", fileContent);
                // 初始化编辑内容
                entity.setEditContent(fileContent);
                try {
                    entity = teachingPlanGeneratorRepository.save(entity);
                    logger.info("已保存初始编辑内容, ID: {}", id);
                } catch (Exception e) {
                    logger.error("保存初始编辑内容失败: {}", e.getMessage(), e);
                    // 即使保存失败，也继续返回内容给前端
                    response.put("warning", "保存初始编辑内容到数据库失败，但您仍可以编辑。请确保手动保存您的更改。");
                }
            }

            response.put("success", true);
            response.put("id", entity.getId());
            response.put("fileName", entity.getFileName());
            response.put("editStartTime", entity.getEditStartTime());
            logger.info("成功启动编辑流程, ID: {}", id);

        } catch (Exception e) {
            logger.error("读取教案文件内容失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "读取教案文件内容失败: " + e.getMessage());
        }

        return response;
    }

    @Override
    public Map<String, Object> saveOnlineEdit(Long id, String editContent) {
        Map<String, Object> response = new HashMap<>();

        Optional<TeachingPlanGenerator> optionalEntity = teachingPlanGeneratorRepository.findById(id);
        if (optionalEntity.isEmpty()) {
            response.put("success", false);
            response.put("message", "找不到指定的教案生成任务");
            return response;
        }

        TeachingPlanGenerator entity = optionalEntity.get();

        // 保存编辑内容
        entity.setEditContent(editContent);
        teachingPlanGeneratorRepository.save(entity);

        response.put("success", true);
        response.put("message", "编辑内容已保存");
        response.put("id", entity.getId());

        return response;
    }

    @Override
    public Map<String, Object> finishOnlineEdit(Long id) {
        Map<String, Object> response = new HashMap<>();

        Optional<TeachingPlanGenerator> optionalEntity = teachingPlanGeneratorRepository.findById(id);
        if (optionalEntity.isEmpty()) {
            response.put("success", false);
            response.put("message", "找不到指定的教案生成任务");
            return response;
        }

        TeachingPlanGenerator entity = optionalEntity.get();
        if (entity.getEditStartTime() == null) {
            response.put("success", false);
            response.put("message", "尚未开始编辑");
            return response;
        }

        // 计算编辑时长（秒）
        LocalDateTime endTime = LocalDateTime.now();
        entity.setEditEndTime(endTime);
        long editDurationSeconds = java.time.Duration.between(entity.getEditStartTime(), endTime).getSeconds();
        entity.setEditDuration(editDurationSeconds);

        // 计算效率指数
        // 公式：基础分(100) - 编辑时长分(根据时长计算，最多-50) + 内容质量分(根据内容长度和复杂度计算，最多+20)
        double baseScore = 100.0;
        double timeScore = Math.min(50.0, editDurationSeconds / 60.0); // 每分钟减1分，最多减50分

        // 根据编辑内容的长度和复杂度估算内容质量分
        String editContent = entity.getEditContent();
        double contentScore = 0.0;
        if (editContent != null && !editContent.isEmpty()) {
            // 简单的基于长度和特定关键词的评分
            contentScore = Math.min(20.0, editContent.length() / 500.0); // 每500字符加1分，最多加20分
        }

        double efficiencyIndex = baseScore - timeScore + contentScore;
        entity.setEfficiencyIndex(efficiencyIndex);

        // 生成优化建议
        StringBuilder suggestions = new StringBuilder();
        if (timeScore > 30) {
            suggestions.append("- 备课时间较长，建议优化备课流程。\n");
        }
        if (contentScore < 10) {
            suggestions.append("- 教案内容可以进一步丰富，增加实例和练习。\n");
        }

        // 保存优化建议
        entity.setOptimizationSuggestions(suggestions.toString());

        // 保存编辑后的文件
        try {
            if (entity.getEditContent() != null && !entity.getEditContent().isEmpty()) {
                // 生成新的文件名（添加"_edited"后缀）
                String originalFilePath = entity.getFilePath();
                String originalFileName = entity.getFileName();

                String newFileName = originalFileName.replaceFirst("\\.(\\w+)$", "_edited.$1");
                String newFilePath = originalFilePath.replaceFirst(originalFileName, newFileName);

                // 保存编辑内容到新的docx文件
                saveContentToDocx(entity.getEditContent(), newFilePath);

                // 更新实体的文件路径和文件名
                entity.setFilePath(newFilePath);
                entity.setFileName(newFileName);
            }
        } catch (Exception e) {
            logger.error("保存编辑后的文件失败: {}", e.getMessage());
            response.put("warning", "编辑内容已保存，但生成新文件失败: " + e.getMessage());
        }

        teachingPlanGeneratorRepository.save(entity);

        response.put("success", true);
        response.put("message", "编辑已完成并保存");
        response.put("id", entity.getId());
        response.put("editDuration", editDurationSeconds);
        response.put("efficiencyIndex", efficiencyIndex);
        response.put("optimizationSuggestions", entity.getOptimizationSuggestions());

        return response;
    }

    @Override
    public Map<String, Object> getEfficiencyStatistics() {
        Map<String, Object> response = new HashMap<>();

        // 获取所有已编辑过的教案
        List<TeachingPlanGenerator> editedPlans = teachingPlanGeneratorRepository.findAll().stream()
                .filter(plan -> plan.getEditDuration() != null && plan.getEditDuration() > 0)
                .collect(Collectors.toList());

        if (editedPlans.isEmpty()) {
            response.put("success", true);
            response.put("message", "暂无教学效率数据");
            response.put("hasData", false);
            return response;
        }

        // 计算平均编辑时长
        double avgEditDuration = editedPlans.stream()
                .mapToLong(TeachingPlanGenerator::getEditDuration)
                .average()
                .orElse(0);

        // 计算平均效率指数
        double avgEfficiencyIndex = editedPlans.stream()
                .mapToDouble(TeachingPlanGenerator::getEfficiencyIndex)
                .average()
                .orElse(0);

        // 找出效率最高和最低的教案
        TeachingPlanGenerator mostEfficient = editedPlans.stream()
                .max(Comparator.comparing(TeachingPlanGenerator::getEfficiencyIndex))
                .orElse(null);

        TeachingPlanGenerator leastEfficient = editedPlans.stream()
                .min(Comparator.comparing(TeachingPlanGenerator::getEfficiencyIndex))
                .orElse(null);

        // 按月统计效率趋势
        Map<String, Double> monthlyEfficiency = new HashMap<>();
        editedPlans.forEach(plan -> {
            if (plan.getEditEndTime() != null) {
                String monthKey = plan.getEditEndTime().getYear() + "-" +
                        String.format("%02d", plan.getEditEndTime().getMonthValue());
                monthlyEfficiency.merge(monthKey, plan.getEfficiencyIndex(), Double::sum);
            }
        });

        response.put("success", true);
        response.put("hasData", true);
        response.put("totalEditedPlans", editedPlans.size());
        response.put("avgEditDuration", avgEditDuration); // 单位：秒
        response.put("avgEfficiencyIndex", avgEfficiencyIndex);

        if (mostEfficient != null) {
            Map<String, Object> mostEfficientData = new HashMap<>();
            mostEfficientData.put("id", mostEfficient.getId());
            mostEfficientData.put("prompt", mostEfficient.getPrompt());
            mostEfficientData.put("efficiencyIndex", mostEfficient.getEfficiencyIndex());
            response.put("mostEfficient", mostEfficientData);
        }

        if (leastEfficient != null) {
            Map<String, Object> leastEfficientData = new HashMap<>();
            leastEfficientData.put("id", leastEfficient.getId());
            leastEfficientData.put("prompt", leastEfficient.getPrompt());
            leastEfficientData.put("efficiencyIndex", leastEfficient.getEfficiencyIndex());
            response.put("leastEfficient", leastEfficientData);
        }

        response.put("monthlyEfficiency", monthlyEfficiency);

        return response;
    }

    @Override
    public Map<String, Object> getCourseOptimizationSuggestions(Long id) {
        Map<String, Object> response = new HashMap<>();

        Optional<TeachingPlanGenerator> optionalEntity = teachingPlanGeneratorRepository.findById(id);
        if (optionalEntity.isEmpty()) {
            response.put("success", false);
            response.put("message", "找不到指定的教案生成任务");
            return response;
        }

        TeachingPlanGenerator entity = optionalEntity.get();
        if (entity.getEfficiencyIndex() == null) {
            response.put("success", false);
            response.put("message", "此教案尚未完成编辑或计算效率指数");
            return response;
        }

        response.put("success", true);
        response.put("id", entity.getId());
        response.put("prompt", entity.getPrompt());
        response.put("efficiencyIndex", entity.getEfficiencyIndex());
        response.put("editDuration", entity.getEditDuration());
        response.put("optimizationSuggestions", entity.getOptimizationSuggestions());

        return response;
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

    // 辅助方法：读取docx文件内容
    private String readDocxContent(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(filePath);
                XWPFDocument document = new XWPFDocument(fis)) {

            // 处理段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                content.append(paragraph.getText()).append("\n");
            }

            // 处理表格
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        // 获取单元格中的段落
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            content.append(paragraph.getText()).append(" ");
                        }
                    }
                    content.append("\n");
                }
                content.append("\n");
            }

            logger.info("成功读取docx文件: {}", filePath);
        } catch (Exception e) {
            logger.error("解析docx文件失败: {}", e.getMessage());
            throw new IOException("解析docx文件失败: " + e.getMessage());
        }

        return content.toString();
    }

    // 辅助方法：将内容保存为docx文件
    private void saveContentToDocx(String content, String filePath) throws IOException {
        try {
            // 创建新的文档
            XWPFDocument document = new XWPFDocument();

            // 按行分割内容
            String[] lines = content.split("\n");

            // 将每行内容添加为段落
            for (String line : lines) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(line);
                // 设置基本格式
                run.setFontFamily("宋体");
                run.setFontSize(12);
            }

            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                document.write(fos);
            }

            logger.info("成功保存docx文件: {}", filePath);
        } catch (Exception e) {
            logger.error("保存docx文件失败: {}", e.getMessage());
            throw new IOException("保存docx文件失败: " + e.getMessage());
        }
    }
}