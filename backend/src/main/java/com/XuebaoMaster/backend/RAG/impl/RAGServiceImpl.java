package com.XuebaoMaster.backend.RAG.impl;
import com.XuebaoMaster.backend.RAG.RAG;
import com.XuebaoMaster.backend.RAG.RAGRepository;
import com.XuebaoMaster.backend.RAG.RAGResponse;
import com.XuebaoMaster.backend.RAG.RAGService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@Service
public class RAGServiceImpl implements RAGService {
    private static final Logger logger = LoggerFactory.getLogger(RAGServiceImpl.class);
    private static final String RAG_API_BASE_URL = "http://localhost:5000";
    @Autowired
    private RAGRepository ragRepository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Override
    public RAG saveRAG(RAG rag) {
        return ragRepository.save(rag);
    }
    @Override
    public List<RAG> getAllRAGs() {
        return ragRepository.findAll();
    }
    @Override
    public Optional<RAG> getRAGById(Long id) {
        return ragRepository.findById(id);
    }
    @Override
    public Optional<RAG> getRAGByName(String name) {
        return ragRepository.findByName(name);
    }
    @Override
    public void deleteRAG(Long id) {
        ragRepository.deleteById(id);
    }
    @Override
    public RAGResponse generateRAG(String sourceDir, String ragName, boolean forceRebuild) {
        try {
            if (sourceDir == null || sourceDir.isEmpty()) {
                return RAGResponse.error("源文件夹路径不能为空");
            }
            File sourceDirFile = new File(sourceDir);
            if (!sourceDirFile.exists() || !sourceDirFile.isDirectory()) {
                return RAGResponse.error("源文件夹不存在或不是一个有效的目录");
            }
            String url = UriComponentsBuilder.fromHttpUrl(RAG_API_BASE_URL + "/generate_rag")
                    .queryParam("source_dir", sourceDir)
                    .queryParam("force_rebuild", forceRebuild)
                    .build()
                    .toUriString();
            ResponseEntity<HashMap> response = restTemplate.getForEntity(url, HashMap.class);
            HashMap<String, Object> responseBody = response.getBody();
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                String ragDir = (String) responseBody.get("rag_dir");
                Path ragDirPath = Paths.get(ragDir);
                String parentDir = ragDirPath.getParent().toString();
                String knowledgeGraphPath = Paths.get(parentDir, "knowledge_graph", "output").toString();
                Path kgPath = Paths.get(knowledgeGraphPath);
                if (!Files.exists(kgPath)) {
                    logger.info("知识图谱路径不存在（一级上溯）: {}", knowledgeGraphPath);
                    try {
                        String grandParentDir = ragDirPath.getParent().getParent().toString();
                        String alternativePath = Paths.get(grandParentDir, "knowledge_graph", "output").toString();
                        logger.info("尝试替代知识图谱路径（两级上溯）: {}", alternativePath);
                        if (Files.exists(Paths.get(alternativePath))) {
                            logger.info("使用替代知识图谱路径");
                            knowledgeGraphPath = alternativePath;
                        }
                    } catch (Exception e) {
                        logger.warn("尝试替代路径时出错", e);
                    }
                }
                RAG newRag = new RAG(ragName, knowledgeGraphPath, ragDir);
                newRag.setStatus(RAG.RAGStatus.COMPLETED);
                RAG savedRag = saveRAG(newRag);
                return RAGResponse.success("RAG数据生成成功", savedRag);
            } else {
                String errorMessage = responseBody != null ? (String) responseBody.get("message") : "未知错误";
                String errorDetails = responseBody != null ? (String) responseBody.get("error_details") : null;
                return RAGResponse.error(errorMessage, errorDetails);
            }
        } catch (RestClientException e) {
            logger.error("调用RAG服务API出错", e);
            return RAGResponse.error("调用RAG服务API出错: " + e.getMessage());
        } catch (Exception e) {
            logger.error("生成RAG数据时发生错误", e);
            return RAGResponse.error("生成RAG数据时发生错误: " + e.getMessage());
        }
    }
    @Override
    public RAGResponse generateRAGAsync(String sourceDir, String ragName, boolean forceRebuild) {
        try {
            if (sourceDir == null || sourceDir.isEmpty()) {
                return RAGResponse.error("源文件夹路径不能为空");
            }
            File sourceDirFile = new File(sourceDir);
            if (!sourceDirFile.exists() || !sourceDirFile.isDirectory()) {
                return RAGResponse.error("源文件夹不存在或不是一个有效的目录");
            }
            RAG newRag = new RAG(ragName, "", "");
            newRag.setStatus(RAG.RAGStatus.PENDING);
            newRag.setStatusMessage("RAG生成任务已提交，等待处理");
            RAG savedRag = saveRAG(newRag);
            processRAGGeneration(savedRag.getId(), sourceDir, forceRebuild);
            return RAGResponse.success("RAG生成任务已提交，请稍后查询状态", savedRag);
        } catch (Exception e) {
            logger.error("提交RAG生成任务时发生错误", e);
            return RAGResponse.error("提交RAG生成任务时发生错误: " + e.getMessage());
        }
    }
    @Override
    public RAGResponse checkRAGGenerationStatus(Long ragId) {
        try {
            Optional<RAG> ragOpt = getRAGById(ragId);
            if (ragOpt.isEmpty()) {
                return RAGResponse.error("找不到指定ID的RAG");
            }
            RAG rag = ragOpt.get();
            HashMap<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("status", rag.getStatus().name());
            statusInfo.put("statusMessage", rag.getStatusMessage());
            statusInfo.put("createdAt", rag.getCreatedAt());
            statusInfo.put("updatedAt", rag.getUpdatedAt());
            statusInfo.put("ragInfo", rag);
            return RAGResponse.success("获取RAG状态成功", statusInfo);
        } catch (Exception e) {
            logger.error("检查RAG生成状态时发生错误", e);
            return RAGResponse.error("检查RAG生成状态时发生错误: " + e.getMessage());
        }
    }
    @Async("ragTaskExecutor")
    public void processRAGGeneration(Long ragId, String sourceDir, boolean forceRebuild) {
        logger.info("开始异步生成RAG数据，ID: {}, 源目录: {}", ragId, sourceDir);
        try {
            Optional<RAG> ragOpt = getRAGById(ragId);
            if (ragOpt.isEmpty()) {
                logger.error("找不到指定ID的RAG: {}", ragId);
                return;
            }
            
            RAG rag = ragOpt.get();
            rag.setStatus(RAG.RAGStatus.GENERATING);
            rag.setStatusMessage("正在生成RAG数据...");
            saveRAG(rag);
            
            String url = UriComponentsBuilder.fromHttpUrl(RAG_API_BASE_URL + "/generate_rag")
                    .queryParam("source_dir", sourceDir)
                    .queryParam("force_rebuild", forceRebuild)
                    .build()
                    .toUriString();
            
            logger.info("调用RAG API: {}", url);
            ResponseEntity<HashMap> response = restTemplate.getForEntity(url, HashMap.class);
            HashMap<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                String ragDir = (String) responseBody.get("rag_dir");
                Path ragDirPath = Paths.get(ragDir);
                String parentDir = ragDirPath.getParent().toString();
                String knowledgeGraphPath = Paths.get(parentDir, "knowledge_graph", "output").toString();
                
                Path kgPath = Paths.get(knowledgeGraphPath);
                if (!Files.exists(kgPath)) {
                    logger.info("知识图谱路径不存在（一级上溯）: {}", knowledgeGraphPath);
                    try {
                        String grandParentDir = ragDirPath.getParent().getParent().toString();
                        String alternativePath = Paths.get(grandParentDir, "knowledge_graph", "output").toString();
                        logger.info("尝试替代知识图谱路径（两级上溯）: {}", alternativePath);
                        if (Files.exists(Paths.get(alternativePath))) {
                            logger.info("使用替代知识图谱路径");
                            knowledgeGraphPath = alternativePath;
                        }
                    } catch (Exception e) {
                        logger.warn("尝试替代路径时出错", e);
                    }
                }
                
                rag.setRagPath(ragDir);
                rag.setKnowledgeGraphPath(knowledgeGraphPath);
                rag.setStatus(RAG.RAGStatus.COMPLETED);
                rag.setStatusMessage("RAG数据生成成功");
                saveRAG(rag);
                logger.info("RAG数据生成成功，ID: {}", ragId);
            } else {
                String errorMessage = responseBody != null ? (String) responseBody.get("message") : "未知错误";
                String errorDetails = responseBody != null ? (String) responseBody.get("error_details") : null;
                rag.setStatus(RAG.RAGStatus.FAILED);
                rag.setStatusMessage("RAG数据生成失败: " + errorMessage);
                saveRAG(rag);
                logger.error("RAG数据生成失败，ID: {}，错误: {}", ragId, errorMessage);
            }
        } catch (Exception e) {
            logger.error("异步生成RAG数据时发生错误，ID: {}", ragId, e);
            try {
                Optional<RAG> ragOpt = getRAGById(ragId);
                if (ragOpt.isPresent()) {
                    RAG rag = ragOpt.get();
                    rag.setStatus(RAG.RAGStatus.FAILED);
                    rag.setStatusMessage("RAG数据生成失败: " + e.getMessage());
                    saveRAG(rag);
                }
            } catch (Exception ex) {
                logger.error("更新RAG状态时发生错误", ex);
            }
        }
    }
    @Override
    public RAGResponse performQuery(String query, Long ragId, Integer topK, Boolean includeGraphContext,
            Integer contextDepth) {
        try {
            if (query == null || query.isEmpty()) {
                return RAGResponse.error("查询文本不能为空");
            }
            Optional<RAG> ragOpt = getRAGById(ragId);
            if (ragOpt.isEmpty()) {
                return RAGResponse.error("找不到指定ID的RAG");
            }
            RAG rag = ragOpt.get();
            if (rag.getStatus() != RAG.RAGStatus.COMPLETED) {
                return RAGResponse.error("RAG数据尚未生成完成，当前状态: " + rag.getStatus() + ", " + rag.getStatusMessage());
            }
            return executeRagQuery(query, rag.getRagPath(), topK, includeGraphContext, contextDepth);
        } catch (Exception e) {
            logger.error("执行RAG查询时发生错误", e);
            return RAGResponse.error("执行RAG查询时发生错误: " + e.getMessage());
        }
    }
    @Override
    public RAGResponse performQueryByName(String query, String ragName, Integer topK, Boolean includeGraphContext,
            Integer contextDepth) {
        try {
            if (query == null || query.isEmpty()) {
                return RAGResponse.error("查询文本不能为空");
            }
            if (ragName == null || ragName.isEmpty()) {
                return RAGResponse.error("RAG名称不能为空");
            }
            Optional<RAG> ragOpt = getRAGByName(ragName);
            if (ragOpt.isEmpty()) {
                return RAGResponse.error("找不到名为 '" + ragName + "' 的RAG");
            }
            RAG rag = ragOpt.get();
            if (rag.getStatus() != RAG.RAGStatus.COMPLETED) {
                return RAGResponse.error("RAG数据尚未生成完成，当前状态: " + rag.getStatus() + ", " + rag.getStatusMessage());
            }
            return executeRagQuery(query, rag.getRagPath(), topK, includeGraphContext, contextDepth);
        } catch (Exception e) {
            logger.error("执行RAG查询时发生错误", e);
            return RAGResponse.error("执行RAG查询时发生错误: " + e.getMessage());
        }
    }
    @Override
    public RAGResponse getKnowledgeGraph(Long ragId) {
        try {
            Optional<RAG> ragOpt = getRAGById(ragId);
            if (ragOpt.isEmpty()) {
                return RAGResponse.error("找不到指定ID的RAG");
            }
            RAG rag = ragOpt.get();
            if (rag.getStatus() != RAG.RAGStatus.COMPLETED) {
                return RAGResponse.error("RAG数据尚未生成完成，当前状态: " + rag.getStatus() + ", " + rag.getStatusMessage());
            }
            return readAndMergeKnowledgeGraphFiles(rag.getKnowledgeGraphPath());
        } catch (Exception e) {
            logger.error("获取知识图谱数据时发生错误", e);
            return RAGResponse.error("获取知识图谱数据时发生错误: " + e.getMessage());
        }
    }
    @Override
    public RAGResponse getKnowledgeGraphByName(String ragName) {
        try {
            if (ragName == null || ragName.isEmpty()) {
                return RAGResponse.error("RAG名称不能为空");
            }
            Optional<RAG> ragOpt = getRAGByName(ragName);
            if (ragOpt.isEmpty()) {
                return RAGResponse.error("找不到名为 '" + ragName + "' 的RAG");
            }
            RAG rag = ragOpt.get();
            if (rag.getStatus() != RAG.RAGStatus.COMPLETED) {
                return RAGResponse.error("RAG数据尚未生成完成，当前状态: " + rag.getStatus() + ", " + rag.getStatusMessage());
            }
            return readAndMergeKnowledgeGraphFiles(rag.getKnowledgeGraphPath());
        } catch (Exception e) {
            logger.error("获取知识图谱数据时发生错误", e);
            return RAGResponse.error("获取知识图谱数据时发生错误: " + e.getMessage());
        }
    }
    private RAGResponse readAndMergeKnowledgeGraphFiles(String knowledgeGraphPath) {
        try {
            logger.info("尝试读取知识图谱，路径: {}", knowledgeGraphPath);
            Path dirPath = Paths.get(knowledgeGraphPath);
            if (!Files.exists(dirPath)) {
                logger.error("知识图谱路径不存在: {}", knowledgeGraphPath);
                return RAGResponse.error("知识图谱路径不存在: " + knowledgeGraphPath);
            }
            if (!Files.isDirectory(dirPath)) {
                logger.error("知识图谱路径不是一个有效的目录: {}", knowledgeGraphPath);
                return RAGResponse.error("知识图谱路径不是一个有效的目录: " + knowledgeGraphPath);
            }
            List<Path> jsonFiles;
            try (Stream<Path> paths = Files.list(dirPath)) {
                jsonFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                        .collect(Collectors.toList());
            }
            if (jsonFiles.isEmpty()) {
                logger.error("知识图谱目录中没有找到JSON文件: {}", knowledgeGraphPath);
                return RAGResponse.error("知识图谱目录中没有找到JSON文件: " + knowledgeGraphPath);
            }
            logger.info("找到 {} 个JSON文件在路径: {}", jsonFiles.size(), knowledgeGraphPath);
            List<List<String>> mergedTriples = new ArrayList<>();
            for (Path jsonFile : jsonFiles) {
                try {
                    List<List<String>> fileTriples = objectMapper.readValue(
                            jsonFile.toFile(),
                            new TypeReference<List<List<String>>>() {
                            });
                    mergedTriples.addAll(fileTriples);
                } catch (IOException e) {
                    logger.warn("读取文件时发生错误: " + jsonFile, e);
                }
            }
            if (mergedTriples.isEmpty()) {
                return RAGResponse.error("从知识图谱JSON文件中没有读取到有效数据");
            }
            HashMap<String, Object> result = new HashMap<>();
            result.put("triples", mergedTriples);
            result.put("totalCount", mergedTriples.size());
            result.put("knowledgeGraphPath", knowledgeGraphPath);
            return RAGResponse.success("获取知识图谱数据成功", result);
        } catch (IOException e) {
            logger.error("读取知识图谱文件时发生错误", e);
            return RAGResponse.error("读取知识图谱文件时发生错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("处理知识图谱数据时发生错误", e);
            return RAGResponse.error("处理知识图谱数据时发生错误: " + e.getMessage());
        }
    }
    
    private RAGResponse executeRagQuery(String query, String ragPath, Integer topK, Boolean includeGraphContext,
            Integer contextDepth) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(RAG_API_BASE_URL + "/rag")
                    .queryParam("query", query)
                    .queryParam("path", ragPath);
            if (topK != null) {
                builder.queryParam("top_k", topK);
            }
            if (includeGraphContext != null) {
                builder.queryParam("include_graph_context", includeGraphContext);
            }
            if (contextDepth != null) {
                builder.queryParam("context_depth", contextDepth);
            }
            String url = builder.build().toUriString();
            ResponseEntity<HashMap> response = restTemplate.getForEntity(url, HashMap.class);
            HashMap<String, Object> responseBody = response.getBody();
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                return RAGResponse.success("查询成功", responseBody);
            } else {
                String errorMessage = responseBody != null ? (String) responseBody.get("message") : "未知错误";
                String errorDetails = responseBody != null ? (String) responseBody.get("error_details") : null;
                return RAGResponse.error(errorMessage, errorDetails);
            }
        } catch (RestClientException e) {
            logger.error("调用RAG服务API出错", e);
            return RAGResponse.error("调用RAG服务API出错: " + e.getMessage());
        } catch (Exception e) {
            logger.error("执行RAG查询时发生错误", e);
            return RAGResponse.error("执行RAG查询时发生错误: " + e.getMessage());
        }
    }
    @Override
    public RAGResponse fixKnowledgeGraphPath(Long ragId) {
        try {
            Optional<RAG> ragOpt = getRAGById(ragId);
            if (ragOpt.isEmpty()) {
                return RAGResponse.error("找不到指定ID的RAG");
            }
            RAG rag = ragOpt.get();
            String ragPath = rag.getRagPath();
            if (ragPath == null || ragPath.isEmpty()) {
                return RAGResponse.error("RAG路径为空，无法修复");
            }
            logger.info("修复RAG (ID: {}, 名称: {}) 的知识图谱路径", rag.getId(), rag.getName());
            logger.info("原始RAG路径: {}", ragPath);
            logger.info("原始知识图谱路径: {}", rag.getKnowledgeGraphPath());
            
            Path ragPathObj = Paths.get(ragPath);
            String parentDir1 = ragPathObj.getParent().toString();
            String kgPath1 = Paths.get(parentDir1, "knowledge_graph", "output").toString();
            logger.info("尝试路径1: {}", kgPath1);
            
            String parentDir2 = ragPathObj.getParent().getParent().toString();
            String kgPath2 = Paths.get(parentDir2, "knowledge_graph", "output").toString();
            logger.info("尝试路径2: {}", kgPath2);
            
            Path kgPathObj1 = Paths.get(kgPath1);
            Path kgPathObj2 = Paths.get(kgPath2);
            String correctKnowledgeGraphPath;
            
            if (Files.exists(kgPathObj1) && Files.isDirectory(kgPathObj1)) {
                logger.info("路径1存在且是目录，使用路径1");
                correctKnowledgeGraphPath = kgPath1;
            } else if (Files.exists(kgPathObj2) && Files.isDirectory(kgPathObj2)) {
                logger.info("路径2存在且是目录，使用路径2");
                correctKnowledgeGraphPath = kgPath2;
            } else {
                logger.error("无法找到有效的知识图谱路径");
                logger.error("路径1: {} 存在={}, 是目录={}", kgPath1, Files.exists(kgPathObj1),
                        Files.exists(kgPathObj1) && Files.isDirectory(kgPathObj1));
                logger.error("路径2: {} 存在={}, 是目录={}", kgPath2, Files.exists(kgPathObj2),
                        Files.exists(kgPathObj2) && Files.isDirectory(kgPathObj2));
                return RAGResponse.error("无法找到有效的知识图谱路径。尝试的路径: \n" + kgPath1 + "\n" + kgPath2);
            }
            
            logger.info("新知识图谱路径: {}", correctKnowledgeGraphPath);
            rag.setKnowledgeGraphPath(correctKnowledgeGraphPath);
            saveRAG(rag);
            
            HashMap<String, Object> result = new HashMap<>();
            result.put("id", rag.getId());
            result.put("name", rag.getName());
            result.put("ragPath", ragPath);
            result.put("oldKnowledgeGraphPath", rag.getKnowledgeGraphPath());
            result.put("newKnowledgeGraphPath", correctKnowledgeGraphPath);
            return RAGResponse.success("知识图谱路径已更新", result);
        } catch (Exception e) {
            logger.error("修复知识图谱路径时发生错误", e);
            return RAGResponse.error("修复知识图谱路径时发生错误: " + e.getMessage());
        }
    }
    @Override
    public RAGResponse fixAllKnowledgeGraphPaths() {
        try {
            List<RAG> allRags = getAllRAGs();
            List<HashMap<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;
            logger.info("开始批量修复 {} 个RAG的知识图谱路径", allRags.size());
            for (RAG rag : allRags) {
                try {
                    if (rag.getRagPath() == null || rag.getRagPath().isEmpty()) {
                        logger.warn("RAG (ID: {}, 名称: {}) 的路径为空，跳过", rag.getId(), rag.getName());
                        continue;
                    }
                    String ragPath = rag.getRagPath();
                    String originalKgPath = rag.getKnowledgeGraphPath();
                    Path ragPathObj = Paths.get(ragPath);
                    String parentDir1 = ragPathObj.getParent().toString();
                    String kgPath1 = Paths.get(parentDir1, "knowledge_graph", "output").toString();
                    
                    String parentDir2;
                    String kgPath2;
                    try {
                        parentDir2 = ragPathObj.getParent().getParent().toString();
                        kgPath2 = Paths.get(parentDir2, "knowledge_graph", "output").toString();
                    } catch (Exception e) {
                        parentDir2 = parentDir1;
                        kgPath2 = kgPath1;
                    }
                    
                    Path kgPathObj1 = Paths.get(kgPath1);
                    Path kgPathObj2 = Paths.get(kgPath2);
                    String correctKnowledgeGraphPath = null;
                    
                    if (Files.exists(kgPathObj1) && Files.isDirectory(kgPathObj1)) {
                        correctKnowledgeGraphPath = kgPath1;
                    } else if (Files.exists(kgPathObj2) && Files.isDirectory(kgPathObj2)) {
                        correctKnowledgeGraphPath = kgPath2;
                    }
                    
                    HashMap<String, Object> result = new HashMap<>();
                    result.put("id", rag.getId());
                    result.put("name", rag.getName());
                    result.put("ragPath", ragPath);
                    result.put("oldKnowledgeGraphPath", originalKgPath);
                    
                    if (correctKnowledgeGraphPath != null) {
                        rag.setKnowledgeGraphPath(correctKnowledgeGraphPath);
                        saveRAG(rag);
                        result.put("newKnowledgeGraphPath", correctKnowledgeGraphPath);
                        result.put("status", "success");
                        successCount++;
                    } else {
                        result.put("status", "failed");
                        result.put("message", "无法找到有效的知识图谱路径");
                        failCount++;
                    }
                    results.add(result);
                } catch (Exception e) {
                    HashMap<String, Object> result = new HashMap<>();
                    result.put("id", rag.getId());
                    result.put("name", rag.getName());
                    result.put("status", "error");
                    result.put("message", e.getMessage());
                    results.add(result);
                    failCount++;
                    logger.error("修复RAG (ID: {}) 的知识图谱路径时出错", rag.getId(), e);
                }
            }
            HashMap<String, Object> summary = new HashMap<>();
            summary.put("totalCount", allRags.size());
            summary.put("successCount", successCount);
            summary.put("failCount", failCount);
            summary.put("results", results);
            return RAGResponse.success("批量修复知识图谱路径完成", summary);
        } catch (Exception e) {
            logger.error("批量修复知识图谱路径时发生错误", e);
            return RAGResponse.error("批量修复知识图谱路径时发生错误: " + e.getMessage());
        }
    }
    
    @PostConstruct
    public void initializeAndFixKnowledgeGraphPaths() {
        try {
            logger.info("系统启动，开始自动修复RAG知识图谱路径...");
            List<RAG> allRags = getAllRAGs();
            if (allRags.isEmpty()) {
                logger.info("没有找到需要修复的RAG");
                return;
            }
            logger.info("找到 {} 个RAG需要检查知识图谱路径", allRags.size());
            int fixedCount = 0;
            int errorCount = 0;
            
            for (RAG rag : allRags) {
                try {
                    if (rag.getRagPath() == null || rag.getRagPath().isEmpty()) {
                        logger.warn("RAG (ID: {}, 名称: {}) 的路径为空，跳过", rag.getId(), rag.getName());
                        continue;
                    }
                    
                    String ragPath = rag.getRagPath();
                    String originalKgPath = rag.getKnowledgeGraphPath();
                    Path originalPath = Paths.get(originalKgPath);
                    
                    if (Files.exists(originalPath) && Files.isDirectory(originalPath)) {
                        logger.info("RAG (ID: {}, 名称: {}) 的知识图谱路径正常，无需修复", rag.getId(), rag.getName());
                        continue;
                    }
                    
                    logger.info("修复RAG (ID: {}, 名称: {}) 的知识图谱路径", rag.getId(), rag.getName());
                    Path ragPathObj = Paths.get(ragPath);
                    String parentDir1 = ragPathObj.getParent().toString();
                    String kgPath1 = Paths.get(parentDir1, "knowledge_graph", "output").toString();
                    
                    String parentDir2;
                    String kgPath2;
                    try {
                        parentDir2 = ragPathObj.getParent().getParent().toString();
                        kgPath2 = Paths.get(parentDir2, "knowledge_graph", "output").toString();
                    } catch (Exception e) {
                        parentDir2 = parentDir1;
                        kgPath2 = kgPath1;
                    }
                    
                    Path kgPathObj1 = Paths.get(kgPath1);
                    Path kgPathObj2 = Paths.get(kgPath2);
                    String correctKnowledgeGraphPath = null;
                    
                    if (Files.exists(kgPathObj1) && Files.isDirectory(kgPathObj1)) {
                        correctKnowledgeGraphPath = kgPath1;
                        logger.info("使用路径1: {}", kgPath1);
                    } else if (Files.exists(kgPathObj2) && Files.isDirectory(kgPathObj2)) {
                        correctKnowledgeGraphPath = kgPath2;
                        logger.info("使用路径2: {}", kgPath2);
                    }
                    
                    if (correctKnowledgeGraphPath != null) {
                        rag.setKnowledgeGraphPath(correctKnowledgeGraphPath);
                        saveRAG(rag);
                        fixedCount++;
                        logger.info("成功修复RAG (ID: {}, 名称: {}) 的知识图谱路径", rag.getId(), rag.getName());
                    } else {
                        logger.warn("无法找到有效的知识图谱路径，RAG (ID: {}, 名称: {})", rag.getId(), rag.getName());
                        errorCount++;
                    }
                } catch (Exception e) {
                    logger.error("修复RAG (ID: {}) 的知识图谱路径时出错", rag.getId(), e);
                    errorCount++;
                }
            }
            logger.info("自动修复RAG知识图谱路径完成，总数: {}，成功: {}，失败: {}",
                    allRags.size(), fixedCount, errorCount);
        } catch (Exception e) {
            logger.error("自动修复知识图谱路径时发生错误", e);
        }
    }
}
