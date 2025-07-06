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
    private static final String RAG_API_BASE_URL = "http:
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
                return RAGResponse.error("婧愭枃浠跺す璺緞涓嶈兘涓虹┖");
            }
            if (!sourceDirFile.exists() || !sourceDirFile.isDirectory()) {
                return RAGResponse.error("婧愭枃浠跺す涓嶅瓨鍦ㄦ垨涓嶆槸涓€涓湁鏁堢殑鐩綍");
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
                Path kgPath = Paths.get(knowledgeGraphPath);
                if (!Files.exists(kgPath)) {
                    logger.info("鐭ヨ瘑鍥捐氨璺緞涓嶅瓨鍦紙涓€绾т笂婧級: {}", knowledgeGraphPath);
                        String grandParentDir = ragDirPath.getParent().getParent().toString();
                        String alternativePath = Paths.get(grandParentDir, "knowledge_graph", "output").toString();
                        logger.info("灏濊瘯鏇夸唬鐭ヨ瘑鍥捐氨璺緞锛堜袱绾т笂婧級: {}", alternativePath);
                        if (Files.exists(Paths.get(alternativePath))) {
                            logger.info("浣跨敤鏇夸唬鐭ヨ瘑鍥捐氨璺緞");
                            knowledgeGraphPath = alternativePath;
                        }
                    } catch (Exception e) {
                        logger.warn("灏濊瘯鏇夸唬璺緞鏃跺嚭閿?, e);
                    }
                }
                RAG newRag = new RAG(ragName, knowledgeGraphPath, ragDir);
                newRag.setStatus(RAG.RAGStatus.COMPLETED);
                RAG savedRag = saveRAG(newRag);
                return RAGResponse.success("RAG鏁版嵁鐢熸垚鎴愬姛", savedRag);
            } else {
                String errorMessage = responseBody != null ? (String) responseBody.get("message") : "鏈煡閿欒";
                String errorDetails = responseBody != null ? (String) responseBody.get("error_details") : null;
                return RAGResponse.error(errorMessage, errorDetails);
            }
        } catch (RestClientException e) {
            logger.error("璋冪敤RAG鏈嶅姟API鍑洪敊", e);
            return RAGResponse.error("璋冪敤RAG鏈嶅姟API鍑洪敊: " + e.getMessage());
        } catch (Exception e) {
            logger.error("鐢熸垚RAG鏁版嵁鏃跺彂鐢熼敊璇?, e);
            return RAGResponse.error("鐢熸垚RAG鏁版嵁鏃跺彂鐢熼敊璇? " + e.getMessage());
        }
    }
    @Override
    public RAGResponse generateRAGAsync(String sourceDir, String ragName, boolean forceRebuild) {
        try {
            if (sourceDir == null || sourceDir.isEmpty()) {
                return RAGResponse.error("婧愭枃浠跺す璺緞涓嶈兘涓虹┖");
            }
            if (!sourceDirFile.exists() || !sourceDirFile.isDirectory()) {
                return RAGResponse.error("婧愭枃浠跺す涓嶅瓨鍦ㄦ垨涓嶆槸涓€涓湁鏁堢殑鐩綍");
            }
            RAG newRag = new RAG(ragName, "", "");
            newRag.setStatus(RAG.RAGStatus.PENDING);
            newRag.setStatusMessage("RAG鐢熸垚浠诲姟宸叉彁浜わ紝绛夊緟澶勭悊");
            RAG savedRag = saveRAG(newRag);
            return RAGResponse.success("RAG鐢熸垚浠诲姟宸叉彁浜わ紝璇风◢鍚庢煡璇㈢姸鎬?, savedRag);
        } catch (Exception e) {
            logger.error("鎻愪氦RAG鐢熸垚浠诲姟鏃跺彂鐢熼敊璇?, e);
            return RAGResponse.error("鎻愪氦RAG鐢熸垚浠诲姟鏃跺彂鐢熼敊璇? " + e.getMessage());
        }
    }
    @Override
    public RAGResponse checkRAGGenerationStatus(Long ragId) {
        try {
            Optional<RAG> ragOpt = getRAGById(ragId);
            if (ragOpt.isEmpty()) {
                return RAGResponse.error("鎵句笉鍒版寚瀹欼D鐨凴AG");
            }
            RAG rag = ragOpt.get();
            HashMap<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("status", rag.getStatus().name());
            statusInfo.put("statusMessage", rag.getStatusMessage());
            statusInfo.put("createdAt", rag.getCreatedAt());
            statusInfo.put("updatedAt", rag.getUpdatedAt());
            statusInfo.put("ragInfo", rag);
            return RAGResponse.success("鑾峰彇RAG鐘舵€佹垚鍔?, statusInfo);
        } catch (Exception e) {
            logger.error("妫€鏌AG鐢熸垚鐘舵€佹椂鍙戠敓閿欒", e);
            return RAGResponse.error("妫€鏌AG鐢熸垚鐘舵€佹椂鍙戠敓閿欒: " + e.getMessage());
        }
    }
    @Async("ragTaskExecutor")
    public void processRAGGeneration(Long ragId, String sourceDir, boolean forceRebuild) {
        logger.info("寮€濮嬪紓姝ョ敓鎴怰AG鏁版嵁锛孖D: {}, 婧愮洰褰? {}", ragId, sourceDir);
        try {
            if (ragOpt.isEmpty()) {
                logger.error("鎵句笉鍒版寚瀹欼D鐨凴AG: {}", ragId);
                return;
            }
            RAG rag = ragOpt.get();
            rag.setStatus(RAG.RAGStatus.GENERATING);
            rag.setStatusMessage("姝ｅ湪鐢熸垚RAG鏁版嵁...");
            saveRAG(rag);
            String url = UriComponentsBuilder.fromHttpUrl(RAG_API_BASE_URL + "/generate_rag")
                    .queryParam("source_dir", sourceDir)
                    .queryParam("force_rebuild", forceRebuild)
                    .build()
                    .toUriString();
            logger.info("璋冪敤RAG API: {}", url);
            ResponseEntity<HashMap> response = restTemplate.getForEntity(url, HashMap.class);
            HashMap<String, Object> responseBody = response.getBody();
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                String ragDir = (String) responseBody.get("rag_dir");
                Path ragDirPath = Paths.get(ragDir);
                String parentDir = ragDirPath.getParent().toString();
                Path kgPath = Paths.get(knowledgeGraphPath);
                if (!Files.exists(kgPath)) {
                    logger.info("鐭ヨ瘑鍥捐氨璺緞涓嶅瓨鍦紙涓€绾т笂婧級: {}", knowledgeGraphPath);
                        String grandParentDir = ragDirPath.getParent().getParent().toString();
                        String alternativePath = Paths.get(grandParentDir, "knowledge_graph", "output").toString();
                        logger.info("灏濊瘯鏇夸唬鐭ヨ瘑鍥捐氨璺緞锛堜袱绾т笂婧級: {}", alternativePath);
                        if (Files.exists(Paths.get(alternativePath))) {
                            logger.info("浣跨敤鏇夸唬鐭ヨ瘑鍥捐氨璺緞");
                            knowledgeGraphPath = alternativePath;
                        }
                    } catch (Exception e) {
                        logger.warn("灏濊瘯鏇夸唬璺緞鏃跺嚭閿?, e);
                    }
                }
                rag.setRagPath(ragDir);
                rag.setKnowledgeGraphPath(knowledgeGraphPath);
                rag.setStatus(RAG.RAGStatus.COMPLETED);
                rag.setStatusMessage("RAG鏁版嵁鐢熸垚鎴愬姛");
                saveRAG(rag);
                logger.info("RAG鏁版嵁鐢熸垚鎴愬姛锛孖D: {}", ragId);
            } else {
                String errorMessage = responseBody != null ? (String) responseBody.get("message") : "鏈煡閿欒";
                String errorDetails = responseBody != null ? (String) responseBody.get("error_details") : null;
                rag.setStatus(RAG.RAGStatus.FAILED);
                rag.setStatusMessage("RAG鏁版嵁鐢熸垚澶辫触: " + errorMessage);
                saveRAG(rag);
                logger.error("RAG鏁版嵁鐢熸垚澶辫触锛孖D: {}锛岄敊璇? {}", ragId, errorMessage);
            }
        } catch (Exception e) {
            logger.error("寮傛鐢熸垚RAG鏁版嵁鏃跺彂鐢熼敊璇紝ID: {}", ragId, e);
            try {
                Optional<RAG> ragOpt = getRAGById(ragId);
                if (ragOpt.isPresent()) {
                    RAG rag = ragOpt.get();
                    rag.setStatus(RAG.RAGStatus.FAILED);
                    rag.setStatusMessage("RAG鏁版嵁鐢熸垚澶辫触: " + e.getMessage());
                    saveRAG(rag);
                }
            } catch (Exception ex) {
                logger.error("鏇存柊RAG鐘舵€佹椂鍙戠敓閿欒", ex);
            }
        }
    }
    @Override
    public RAGResponse performQuery(String query, Long ragId, Integer topK, Boolean includeGraphContext,
            Integer contextDepth) {
        try {
            if (query == null || query.isEmpty()) {
                return RAGResponse.error("鏌ヨ鏂囨湰涓嶈兘涓虹┖");
            }
            Optional<RAG> ragOpt = getRAGById(ragId);
            if (ragOpt.isEmpty()) {
                return RAGResponse.error("鎵句笉鍒版寚瀹欼D鐨凴AG");
            }
            RAG rag = ragOpt.get();
            if (rag.getStatus() != RAG.RAGStatus.COMPLETED) {
                return RAGResponse.error("RAG鏁版嵁灏氭湭鐢熸垚瀹屾垚锛屽綋鍓嶇姸鎬? " + rag.getStatus() + ", " + rag.getStatusMessage());
            }
            return executeRagQuery(query, rag.getRagPath(), topK, includeGraphContext, contextDepth);
        } catch (Exception e) {
            logger.error("鎵цRAG鏌ヨ鏃跺彂鐢熼敊璇?, e);
            return RAGResponse.error("鎵цRAG鏌ヨ鏃跺彂鐢熼敊璇? " + e.getMessage());
        }
    }
    @Override
    public RAGResponse performQueryByName(String query, String ragName, Integer topK, Boolean includeGraphContext,
            Integer contextDepth) {
        try {
            if (query == null || query.isEmpty()) {
                return RAGResponse.error("鏌ヨ鏂囨湰涓嶈兘涓虹┖");
            }
            if (ragName == null || ragName.isEmpty()) {
                return RAGResponse.error("RAG鍚嶇О涓嶈兘涓虹┖");
            }
            Optional<RAG> ragOpt = getRAGByName(ragName);
            if (ragOpt.isEmpty()) {
                return RAGResponse.error("鎵句笉鍒板悕涓?'" + ragName + "' 鐨凴AG");
            }
            RAG rag = ragOpt.get();
            if (rag.getStatus() != RAG.RAGStatus.COMPLETED) {
                return RAGResponse.error("RAG鏁版嵁灏氭湭鐢熸垚瀹屾垚锛屽綋鍓嶇姸鎬? " + rag.getStatus() + ", " + rag.getStatusMessage());
            }
            return executeRagQuery(query, rag.getRagPath(), topK, includeGraphContext, contextDepth);
        } catch (Exception e) {
            logger.error("鎵цRAG鏌ヨ鏃跺彂鐢熼敊璇?, e);
            return RAGResponse.error("鎵цRAG鏌ヨ鏃跺彂鐢熼敊璇? " + e.getMessage());
        }
    }
    @Override
    public RAGResponse getKnowledgeGraph(Long ragId) {
        try {
            Optional<RAG> ragOpt = getRAGById(ragId);
            if (ragOpt.isEmpty()) {
                return RAGResponse.error("鎵句笉鍒版寚瀹欼D鐨凴AG");
            }
            RAG rag = ragOpt.get();
            if (rag.getStatus() != RAG.RAGStatus.COMPLETED) {
                return RAGResponse.error("RAG鏁版嵁灏氭湭鐢熸垚瀹屾垚锛屽綋鍓嶇姸鎬? " + rag.getStatus() + ", " + rag.getStatusMessage());
            }
            return readAndMergeKnowledgeGraphFiles(rag.getKnowledgeGraphPath());
        } catch (Exception e) {
            logger.error("鑾峰彇鐭ヨ瘑鍥捐氨鏁版嵁鏃跺彂鐢熼敊璇?, e);
            return RAGResponse.error("鑾峰彇鐭ヨ瘑鍥捐氨鏁版嵁鏃跺彂鐢熼敊璇? " + e.getMessage());
        }
    }
    @Override
    public RAGResponse getKnowledgeGraphByName(String ragName) {
        try {
            if (ragName == null || ragName.isEmpty()) {
                return RAGResponse.error("RAG鍚嶇О涓嶈兘涓虹┖");
            }
            Optional<RAG> ragOpt = getRAGByName(ragName);
            if (ragOpt.isEmpty()) {
                return RAGResponse.error("鎵句笉鍒板悕涓?'" + ragName + "' 鐨凴AG");
            }
            RAG rag = ragOpt.get();
            if (rag.getStatus() != RAG.RAGStatus.COMPLETED) {
                return RAGResponse.error("RAG鏁版嵁灏氭湭鐢熸垚瀹屾垚锛屽綋鍓嶇姸鎬? " + rag.getStatus() + ", " + rag.getStatusMessage());
            }
            return readAndMergeKnowledgeGraphFiles(rag.getKnowledgeGraphPath());
        } catch (Exception e) {
            logger.error("鑾峰彇鐭ヨ瘑鍥捐氨鏁版嵁鏃跺彂鐢熼敊璇?, e);
            return RAGResponse.error("鑾峰彇鐭ヨ瘑鍥捐氨鏁版嵁鏃跺彂鐢熼敊璇? " + e.getMessage());
        }
    }
    private RAGResponse readAndMergeKnowledgeGraphFiles(String knowledgeGraphPath) {
        try {
            logger.info("灏濊瘯璇诲彇鐭ヨ瘑鍥捐氨锛岃矾寰? {}", knowledgeGraphPath);
            Path dirPath = Paths.get(knowledgeGraphPath);
                logger.error("鐭ヨ瘑鍥捐氨璺緞涓嶅瓨鍦? {}", knowledgeGraphPath);
                return RAGResponse.error("鐭ヨ瘑鍥捐氨璺緞涓嶅瓨鍦? " + knowledgeGraphPath);
            }
            if (!Files.isDirectory(dirPath)) {
                logger.error("鐭ヨ瘑鍥捐氨璺緞涓嶆槸涓€涓湁鏁堢殑鐩綍: {}", knowledgeGraphPath);
                return RAGResponse.error("鐭ヨ瘑鍥捐氨璺緞涓嶆槸涓€涓湁鏁堢殑鐩綍: " + knowledgeGraphPath);
            }
            List<Path> jsonFiles;
            try (Stream<Path> paths = Files.list(dirPath)) {
                jsonFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                        .collect(Collectors.toList());
            }
            if (jsonFiles.isEmpty()) {
                logger.error("鐭ヨ瘑鍥捐氨鐩綍涓病鏈夋壘鍒癑SON鏂囦欢: {}", knowledgeGraphPath);
                return RAGResponse.error("鐭ヨ瘑鍥捐氨鐩綍涓病鏈夋壘鍒癑SON鏂囦欢: " + knowledgeGraphPath);
            }
            logger.info("鎵惧埌 {} 涓狫SON鏂囦欢鍦ㄨ矾寰? {}", jsonFiles.size(), knowledgeGraphPath);
            List<List<String>> mergedTriples = new ArrayList<>();
            for (Path jsonFile : jsonFiles) {
                try {
                    List<List<String>> fileTriples = objectMapper.readValue(
                            jsonFile.toFile(),
                            new TypeReference<List<List<String>>>() {
                            });
                    mergedTriples.addAll(fileTriples);
                } catch (IOException e) {
                    logger.warn("璇诲彇鏂囦欢鏃跺彂鐢熼敊璇? " + jsonFile, e);
                }
            }
            if (mergedTriples.isEmpty()) {
                return RAGResponse.error("浠庣煡璇嗗浘璋盝SON鏂囦欢涓病鏈夎鍙栧埌鏈夋晥鏁版嵁");
            }
            HashMap<String, Object> result = new HashMap<>();
            result.put("triples", mergedTriples);
            result.put("totalCount", mergedTriples.size());
            result.put("knowledgeGraphPath", knowledgeGraphPath);
            return RAGResponse.success("鑾峰彇鐭ヨ瘑鍥捐氨鏁版嵁鎴愬姛", result);
        } catch (IOException e) {
            logger.error("璇诲彇鐭ヨ瘑鍥捐氨鏂囦欢鏃跺彂鐢熼敊璇?, e);
            return RAGResponse.error("璇诲彇鐭ヨ瘑鍥捐氨鏂囦欢鏃跺彂鐢熼敊璇? " + e.getMessage());
        } catch (Exception e) {
            logger.error("澶勭悊鐭ヨ瘑鍥捐氨鏁版嵁鏃跺彂鐢熼敊璇?, e);
            return RAGResponse.error("澶勭悊鐭ヨ瘑鍥捐氨鏁版嵁鏃跺彂鐢熼敊璇? " + e.getMessage());
        }
    }
    private RAGResponse executeRagQuery(String query, String ragPath, Integer topK, Boolean includeGraphContext,
            Integer contextDepth) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(RAG_API_BASE_URL + "/rag")
                    .queryParam("query", query)
                    .queryParam("path", ragPath);
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
                return RAGResponse.success("鏌ヨ鎴愬姛", responseBody);
            } else {
                String errorMessage = responseBody != null ? (String) responseBody.get("message") : "鏈煡閿欒";
                String errorDetails = responseBody != null ? (String) responseBody.get("error_details") : null;
                return RAGResponse.error(errorMessage, errorDetails);
            }
        } catch (RestClientException e) {
            logger.error("璋冪敤RAG鏈嶅姟API鍑洪敊", e);
            return RAGResponse.error("璋冪敤RAG鏈嶅姟API鍑洪敊: " + e.getMessage());
        } catch (Exception e) {
            logger.error("鎵цRAG鏌ヨ鏃跺彂鐢熼敊璇?, e);
            return RAGResponse.error("鎵цRAG鏌ヨ鏃跺彂鐢熼敊璇? " + e.getMessage());
        }
    }
    @Override
    public RAGResponse fixKnowledgeGraphPath(Long ragId) {
        try {
            Optional<RAG> ragOpt = getRAGById(ragId);
            if (ragOpt.isEmpty()) {
                return RAGResponse.error("鎵句笉鍒版寚瀹欼D鐨凴AG");
            }
            RAG rag = ragOpt.get();
            String ragPath = rag.getRagPath();
            if (ragPath == null || ragPath.isEmpty()) {
                return RAGResponse.error("RAG璺緞涓虹┖锛屾棤娉曚慨澶?);
            }
            logger.info("淇RAG (ID: {}, 鍚嶇О: {}) 鐨勭煡璇嗗浘璋辫矾寰?, rag.getId(), rag.getName());
            logger.info("鍘熷RAG璺緞: {}", ragPath);
            logger.info("鍘熷鐭ヨ瘑鍥捐氨璺緞: {}", rag.getKnowledgeGraphPath());
            String parentDir1 = ragPathObj.getParent().toString();
            String kgPath1 = Paths.get(parentDir1, "knowledge_graph", "output").toString();
            logger.info("灏濊瘯璺緞1: {}", kgPath1);
            String kgPath2 = Paths.get(parentDir2, "knowledge_graph", "output").toString();
            logger.info("灏濊瘯璺緞2: {}", kgPath2);
            Path kgPathObj2 = Paths.get(kgPath2);
            String correctKnowledgeGraphPath;
            if (Files.exists(kgPathObj1) && Files.isDirectory(kgPathObj1)) {
                logger.info("璺緞1瀛樺湪涓旀槸鐩綍锛屼娇鐢ㄨ矾寰?");
                correctKnowledgeGraphPath = kgPath1;
            } else if (Files.exists(kgPathObj2) && Files.isDirectory(kgPathObj2)) {
                logger.info("璺緞2瀛樺湪涓旀槸鐩綍锛屼娇鐢ㄨ矾寰?");
                correctKnowledgeGraphPath = kgPath2;
            } else {
                logger.error("鏃犳硶鎵惧埌鏈夋晥鐨勭煡璇嗗浘璋辫矾寰?);
                logger.error("璺緞1: {} 瀛樺湪={}, 鏄洰褰?{}", kgPath1, Files.exists(kgPathObj1),
                        Files.exists(kgPathObj1) && Files.isDirectory(kgPathObj1));
                logger.error("璺緞2: {} 瀛樺湪={}, 鏄洰褰?{}", kgPath2, Files.exists(kgPathObj2),
                        Files.exists(kgPathObj2) && Files.isDirectory(kgPathObj2));
                return RAGResponse.error("鏃犳硶鎵惧埌鏈夋晥鐨勭煡璇嗗浘璋辫矾寰勩€傚皾璇曠殑璺緞: \n" + kgPath1 + "\n" + kgPath2);
            }
            logger.info("鏂扮煡璇嗗浘璋辫矾寰? {}", correctKnowledgeGraphPath);
            saveRAG(rag);
            HashMap<String, Object> result = new HashMap<>();
            result.put("id", rag.getId());
            result.put("name", rag.getName());
            result.put("ragPath", ragPath);
            result.put("oldKnowledgeGraphPath", rag.getKnowledgeGraphPath());
            result.put("newKnowledgeGraphPath", correctKnowledgeGraphPath);
            return RAGResponse.success("鐭ヨ瘑鍥捐氨璺緞宸叉洿鏂?, result);
        } catch (Exception e) {
            logger.error("淇鐭ヨ瘑鍥捐氨璺緞鏃跺彂鐢熼敊璇?, e);
            return RAGResponse.error("淇鐭ヨ瘑鍥捐氨璺緞鏃跺彂鐢熼敊璇? " + e.getMessage());
        }
    }
    @Override
    public RAGResponse fixAllKnowledgeGraphPaths() {
        try {
            List<RAG> allRags = getAllRAGs();
            List<HashMap<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;
            logger.info("寮€濮嬫壒閲忎慨澶?{} 涓猂AG鐨勭煡璇嗗浘璋辫矾寰?, allRags.size());
            for (RAG rag : allRags) {
                try {
                    if (rag.getRagPath() == null || rag.getRagPath().isEmpty()) {
                        logger.warn("RAG (ID: {}, 鍚嶇О: {}) 鐨勮矾寰勪负绌猴紝璺宠繃", rag.getId(), rag.getName());
                        continue;
                    }
                    String ragPath = rag.getRagPath();
                    String originalKgPath = rag.getKnowledgeGraphPath();
                    String parentDir1 = ragPathObj.getParent().toString();
                    String kgPath1 = Paths.get(parentDir1, "knowledge_graph", "output").toString();
                    String kgPath2;
                    try {
                        parentDir2 = ragPathObj.getParent().getParent().toString();
                        kgPath2 = Paths.get(parentDir2, "knowledge_graph", "output").toString();
                    } catch (Exception e) {
                        parentDir2 = parentDir1;
                        kgPath2 = kgPath1;
                    }
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
                        saveRAG(rag);
                        result.put("newKnowledgeGraphPath", correctKnowledgeGraphPath);
                        result.put("status", "success");
                        successCount++;
                    } else {
                        result.put("status", "failed");
                        result.put("message", "鏃犳硶鎵惧埌鏈夋晥鐨勭煡璇嗗浘璋辫矾寰?);
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
                    logger.error("淇RAG (ID: {}) 鐨勭煡璇嗗浘璋辫矾寰勬椂鍑洪敊", rag.getId(), e);
                }
            }
            HashMap<String, Object> summary = new HashMap<>();
            summary.put("totalCount", allRags.size());
            summary.put("successCount", successCount);
            summary.put("failCount", failCount);
            summary.put("results", results);
            return RAGResponse.success("鎵归噺淇鐭ヨ瘑鍥捐氨璺緞瀹屾垚", summary);
        } catch (Exception e) {
            logger.error("鎵归噺淇鐭ヨ瘑鍥捐氨璺緞鏃跺彂鐢熼敊璇?, e);
            return RAGResponse.error("鎵归噺淇鐭ヨ瘑鍥捐氨璺緞鏃跺彂鐢熼敊璇? " + e.getMessage());
        }
    }
    @PostConstruct
    public void initializeAndFixKnowledgeGraphPaths() {
        try {
            logger.info("绯荤粺鍚姩锛屽紑濮嬭嚜鍔ㄤ慨澶峈AG鐭ヨ瘑鍥捐氨璺緞...");
            List<RAG> allRags = getAllRAGs();
            if (allRags.isEmpty()) {
                logger.info("娌℃湁鎵惧埌闇€瑕佷慨澶嶇殑RAG");
                return;
            }
            logger.info("鎵惧埌 {} 涓猂AG闇€瑕佹鏌ョ煡璇嗗浘璋辫矾寰?, allRags.size());
            int fixedCount = 0;
            int errorCount = 0;
            for (RAG rag : allRags) {
                try {
                    if (rag.getRagPath() == null || rag.getRagPath().isEmpty()) {
                        logger.warn("RAG (ID: {}, 鍚嶇О: {}) 鐨勮矾寰勪负绌猴紝璺宠繃", rag.getId(), rag.getName());
                        continue;
                    }
                    String ragPath = rag.getRagPath();
                    String originalKgPath = rag.getKnowledgeGraphPath();
                    if (Files.exists(originalPath) && Files.isDirectory(originalPath)) {
                        logger.info("RAG (ID: {}, 鍚嶇О: {}) 鐨勭煡璇嗗浘璋辫矾寰勬甯革紝鏃犻渶淇", rag.getId(), rag.getName());
                        continue;
                    }
                    logger.info("淇RAG (ID: {}, 鍚嶇О: {}) 鐨勭煡璇嗗浘璋辫矾寰?, rag.getId(), rag.getName());
                    String parentDir1 = ragPathObj.getParent().toString();
                    String kgPath1 = Paths.get(parentDir1, "knowledge_graph", "output").toString();
                    String kgPath2;
                    try {
                        parentDir2 = ragPathObj.getParent().getParent().toString();
                        kgPath2 = Paths.get(parentDir2, "knowledge_graph", "output").toString();
                    } catch (Exception e) {
                        parentDir2 = parentDir1;
                        kgPath2 = kgPath1;
                    }
                    Path kgPathObj2 = Paths.get(kgPath2);
                    String correctKnowledgeGraphPath = null;
                    if (Files.exists(kgPathObj1) && Files.isDirectory(kgPathObj1)) {
                        correctKnowledgeGraphPath = kgPath1;
                        logger.info("浣跨敤璺緞1: {}", kgPath1);
                    } else if (Files.exists(kgPathObj2) && Files.isDirectory(kgPathObj2)) {
                        correctKnowledgeGraphPath = kgPath2;
                        logger.info("浣跨敤璺緞2: {}", kgPath2);
                    }
                    if (correctKnowledgeGraphPath != null) {
                        saveRAG(rag);
                        fixedCount++;
                        logger.info("鎴愬姛淇RAG (ID: {}, 鍚嶇О: {}) 鐨勭煡璇嗗浘璋辫矾寰?, rag.getId(), rag.getName());
                    } else {
                        logger.warn("鏃犳硶鎵惧埌鏈夋晥鐨勭煡璇嗗浘璋辫矾寰勶紝RAG (ID: {}, 鍚嶇О: {})", rag.getId(), rag.getName());
                        errorCount++;
                    }
                } catch (Exception e) {
                    logger.error("淇RAG (ID: {}) 鐨勭煡璇嗗浘璋辫矾寰勬椂鍑洪敊", rag.getId(), e);
                    errorCount++;
                }
            }
            logger.info("鑷姩淇RAG鐭ヨ瘑鍥捐氨璺緞瀹屾垚锛屾€绘暟: {}锛屾垚鍔? {}锛屽け璐? {}",
                    allRags.size(), fixedCount, errorCount);
        } catch (Exception e) {
            logger.error("鑷姩淇鐭ヨ瘑鍥捐氨璺緞鏃跺彂鐢熼敊璇?, e);
        }
    }
}
