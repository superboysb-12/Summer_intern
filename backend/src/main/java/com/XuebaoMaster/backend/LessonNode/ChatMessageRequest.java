package com.XuebaoMaster.backend.LessonNode;

import lombok.Data;
import java.util.Map;

/**
 * 聊天消息请求类
 */
@Data
public class ChatMessageRequest {
    private String message;
    private Map<String, Object> additionalParams;
    
    // 默认构造函数
    public ChatMessageRequest() {
    }
    
    // 带参数的构造函数
    public ChatMessageRequest(String message, Map<String, Object> additionalParams) {
        this.message = message;
        this.additionalParams = additionalParams;
    }
} 