package com.XuebaoMaster.backend.DeepSeekChat;
import java.util.List;
import java.util.Map;
public class ChatMessage {
    private List<Map<String, String>> messages;
    public ChatMessage() {
    }
    public ChatMessage(List<Map<String, String>> messages) {
        this.messages = messages;
    }
    public List<Map<String, String>> getMessages() {
        return messages;
    }
    public void setMessages(List<Map<String, String>> messages) {
        this.messages = messages;
    }
} 
