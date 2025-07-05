package com.XuebaoMaster.backend.DeepSeekChat;

import java.util.List;
import java.util.Map;

public interface DeepSeekChatService {
    /**
     * 发送消息到DeepSeek API并获取响应
     * 
     * @param messages 消息列表
     * @return DeepSeek API的响应
     */
    DeepSeekChatResponse sendMessage(List<Map<String, String>> messages);
} 