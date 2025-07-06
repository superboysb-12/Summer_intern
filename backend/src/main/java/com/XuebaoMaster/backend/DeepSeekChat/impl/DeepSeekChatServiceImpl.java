package com.XuebaoMaster.backend.DeepSeekChat.impl;
import com.XuebaoMaster.backend.DeepSeekChat.DeepSeekChatRequest;
import com.XuebaoMaster.backend.DeepSeekChat.DeepSeekChatResponse;
import com.XuebaoMaster.backend.DeepSeekChat.DeepSeekChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;
@Service
public class DeepSeekChatServiceImpl implements DeepSeekChatService {
    @Autowired
    private RestTemplate restTemplate;
    @Value("${deepseek.api.url}")
    private String apiUrl;
    @Value("${deepseek.api.key}")
    private String apiKey;
    @Value("${deepseek.api.model}")
    private String model;
    @Value("${deepseek.api.temperature:0.7}")
    private Double temperature;
    @Value("${deepseek.api.max-tokens:2048}")
    private Integer maxTokens;
    @Override
    public DeepSeekChatResponse sendMessage(List<Map<String, String>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        DeepSeekChatRequest request = new DeepSeekChatRequest();
        request.setModel(model);
        request.setMessages(messages);
        request.setTemperature(temperature);
        request.setMax_tokens(maxTokens);
        HttpEntity<DeepSeekChatRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<DeepSeekChatResponse> response = restTemplate.postForEntity(
                apiUrl,
                entity,
                DeepSeekChatResponse.class
        );
        return response.getBody();
    }
} 
