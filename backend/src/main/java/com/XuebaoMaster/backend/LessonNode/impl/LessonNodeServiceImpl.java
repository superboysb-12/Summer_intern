package com.XuebaoMaster.backend.LessonNode.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import com.XuebaoMaster.backend.LessonNode.LessonNode;
import com.XuebaoMaster.backend.LessonNode.LessonNodeRepository;
import com.XuebaoMaster.backend.LessonNode.LessonNodeService;
import com.XuebaoMaster.backend.LessonNode.RagResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class LessonNodeServiceImpl implements LessonNodeService {

    @Autowired
    private LessonNodeRepository lessonNodeRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 如果RestTemplate没有被定义为Bean，需要在构造函数中初始化
    public LessonNodeServiceImpl() {
        if (this.restTemplate == null) {
            this.restTemplate = new RestTemplate();
        }
    }

    @Override
    public LessonNode createLessonNode(LessonNode lessonNode) {
        return lessonNodeRepository.save(lessonNode);
    }

    @Override
    public LessonNode updateLessonNode(LessonNode lessonNode) {
        LessonNode existingNode = lessonNodeRepository.findById(lessonNode.getId())
                .orElseThrow(() -> new RuntimeException("Lesson node not found"));

        existingNode.setCourse(lessonNode.getCourse());
        existingNode.setNodeOrder(lessonNode.getNodeOrder());
        existingNode.setTitle(lessonNode.getTitle());
        existingNode.setPathToNodes(lessonNode.getPathToNodes());

        return lessonNodeRepository.save(existingNode);
    }

    @Override
    public void deleteLessonNode(Long id) {
        lessonNodeRepository.deleteById(id);
    }

    @Override
    public LessonNode getLessonNodeById(Long id) {
        return lessonNodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson node not found"));
    }

    @Override
    public List<LessonNode> getLessonNodesByCourseId(Long courseId) {
        return lessonNodeRepository.findByCourseCourseId(courseId);
    }
    
    @Override
    public List<LessonNode> getLessonNodesByCourseIdOrdered(Long courseId) {
        return lessonNodeRepository.findByCourseCourseIdOrderByNodeOrder(courseId);
    }
    
    @Override
    public LessonNode getLessonNodeByCourseIdAndOrder(Long courseId, Integer nodeOrder) {
        return lessonNodeRepository.findByCourseCourseIdAndNodeOrder(courseId, nodeOrder)
                .orElseThrow(() -> new RuntimeException("Lesson node not found"));
    }

    @Override
    public List<LessonNode> getAllLessonNodes() {
        return lessonNodeRepository.findAll();
    }

    @Override
    public List<LessonNode> searchLessonNodes(String keyword) {
        return lessonNodeRepository.findByTitleContaining(keyword);
    }
    
    @Override
    public String generateRag(String sourcePath) {
        try {
            // 将反斜杠替换为正斜杠
            sourcePath = sourcePath.replace('\\', '/');
            
            // 使用UriComponentsBuilder构建URL，它会自动处理编码
            String url = UriComponentsBuilder.fromHttpUrl("http://localhost:8000/generate_rag")
                .queryParam("path", sourcePath)
                .build()
                .toUriString();
            
            System.out.println("发送RAG生成请求: " + url);
            
            // 发送GET请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            // 解析响应，获取RAG目录路径
            String responseBody = response.getBody();
            System.out.println("RAG生成响应: " + responseBody);
            
            String ragDir = null;
            
            try {
                // 尝试解析JSON响应
                JsonNode rootNode = objectMapper.readTree(responseBody);
                
                // 首先尝试获取rag_dir字段
                if (rootNode.has("rag_dir")) {
                    ragDir = rootNode.get("rag_dir").asText();
                    System.out.println("从JSON中提取的RAG路径(rag_dir): " + ragDir);
                } 
                // 然后尝试从data.rag_dir获取
                else if (rootNode.has("data") && rootNode.get("data").has("rag_dir")) {
                    ragDir = rootNode.get("data").get("rag_dir").asText();
                    System.out.println("从JSON中提取的RAG路径(data.rag_dir): " + ragDir);
                }
                // 尝试从data.processed_dir获取
                else if (rootNode.has("data") && rootNode.get("data").has("processed_dir")) {
                    ragDir = rootNode.get("data").get("processed_dir").asText();
                    System.out.println("从JSON中提取的RAG路径(data.processed_dir): " + ragDir);
                }
                // 尝试从processed_dir获取
                else if (rootNode.has("processed_dir")) {
                    ragDir = rootNode.get("processed_dir").asText();
                    System.out.println("从JSON中提取的RAG路径(processed_dir): " + ragDir);
                }
                
                // 确保路径格式一致（使用反斜杠，因为这是Windows系统）
                if (ragDir != null) {
                    ragDir = ragDir.replace('/', '\\');
                    System.out.println("格式化后的RAG路径: " + ragDir);
                }
            } catch (Exception e) {
                System.out.println("JSON解析失败: " + e.getMessage());
            }
            
            // 如果无法从JSON中提取路径，则使用原始响应
            if (ragDir == null) {
                ragDir = responseBody;
                System.out.println("使用原始响应作为RAG路径: " + ragDir);
            }
            
            return ragDir;
        } catch (Exception e) {
            System.out.println("生成RAG失败: " + e.getMessage());
            throw new RuntimeException("生成RAG失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String queryRag(String query, String ragPath) {
        try {
            // 将反斜杠替换为正斜杠
            ragPath = ragPath.replace('\\', '/');
            
            // 使用UriComponentsBuilder构建URL，它会自动处理编码
            String url = UriComponentsBuilder.fromHttpUrl("http://localhost:8000/rag")
                .queryParam("query", query)
                .queryParam("path", ragPath)
                .build()
                .toUriString();
            
            // 发送GET请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            // 解析响应
            String responseBody = response.getBody();
            
            try {
                // 尝试解析JSON响应
                JsonNode rootNode = objectMapper.readTree(responseBody);
                
                // 如果响应中包含特定字段，可以提取该字段
                if (rootNode.has("answer")) {
                    return rootNode.get("answer").asText();
                } else if (rootNode.has("data") && rootNode.get("data").has("answer")) {
                    return rootNode.get("data").get("answer").asText();
                }
            } catch (Exception e) {
                // JSON解析失败，直接返回原始响应
                System.out.println("JSON解析失败，使用原始响应: " + e.getMessage());
            }
            
            // 返回原始响应
            return responseBody;
        } catch (Exception e) {
            throw new RuntimeException("查询RAG失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String sendChatMessage(String message, Map<String, Object> additionalParams) {
        try {
            System.out.println("发送聊天消息: " + message);
            
            // 创建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", message);
            
            // 添加附加参数（如果有）
            if (additionalParams != null) {
                requestBody.putAll(additionalParams);
            }
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 创建HTTP实体
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 使用正确的URL，确保聊天服务在运行
            String url = "http://localhost:8000/v1/chat-messages";
            System.out.println("发送请求到: " + url);
            
            try {
                // 发送POST请求
                ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    entity,
                    String.class
                );
                
                // 返回响应体
                return response.getBody();
            } catch (HttpClientErrorException e) {
                System.out.println("HTTP错误: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
                
                // 创建错误响应
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "聊天服务暂时不可用，请稍后再试。错误: " + e.getMessage());
                errorResponse.put("original_query", message);
                
                return objectMapper.writeValueAsString(errorResponse);
            }
        } catch (Exception e) {
            System.out.println("发送聊天消息出错: " + e.getMessage());
            try {
                // 创建错误响应
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "处理聊天消息时发生错误: " + e.getMessage());
                errorResponse.put("original_query", message);
                
                return objectMapper.writeValueAsString(errorResponse);
            } catch (Exception jsonEx) {
                return "{\"status\":\"error\",\"message\":\"处理聊天消息时发生错误\"}";
            }
        }
    }
} 