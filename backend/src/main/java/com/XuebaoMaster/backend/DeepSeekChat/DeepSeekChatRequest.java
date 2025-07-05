package com.XuebaoMaster.backend.DeepSeekChat;

import java.util.List;
import java.util.Map;

public class DeepSeekChatRequest {
    private String model;
    private List<Map<String, String>> messages;
    private Double temperature;
    private Integer max_tokens;
    
    public DeepSeekChatRequest() {
    }
    
    public DeepSeekChatRequest(String model, List<Map<String, String>> messages, Double temperature, Integer max_tokens) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<Map<String, String>> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Map<String, String>> messages) {
        this.messages = messages;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Integer getMax_tokens() {
        return max_tokens;
    }
    
    public void setMax_tokens(Integer max_tokens) {
        this.max_tokens = max_tokens;
    }
} 