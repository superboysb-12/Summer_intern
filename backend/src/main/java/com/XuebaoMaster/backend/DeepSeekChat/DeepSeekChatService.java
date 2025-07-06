package com.XuebaoMaster.backend.DeepSeekChat;
import java.util.List;
import java.util.Map;
public interface DeepSeekChatService {
    DeepSeekChatResponse sendMessage(List<Map<String, String>> messages);
} 
