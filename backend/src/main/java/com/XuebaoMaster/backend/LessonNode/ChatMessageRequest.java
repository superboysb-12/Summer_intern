package com.XuebaoMaster.backend.LessonNode;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息请求类
 */
@Data
public class ChatMessageRequest {
    private Map<String, Object> inputs;
    private String query;
    private String response_mode;
    private String conversation_id;
    private String user;
    private List<FileInfo> files;
    
    @Data
    public static class FileInfo {
        private String type;
        private String transfer_method;
        private String url;
    }
    
    // 默认构造函数
    public ChatMessageRequest() {
    }
} 