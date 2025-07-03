package com.XuebaoMaster.backend.LessonNode;

import lombok.Data;

/**
 * RAG服务响应类
 */
@Data
public class RagResponse {
    private String status;
    private String message;
    private String data;
    
    // 默认构造函数
    public RagResponse() {
    }
    
    // 带参数的构造函数
    public RagResponse(String status, String message, String data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }
} 