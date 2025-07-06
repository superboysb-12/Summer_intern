package com.example.application.model;

public class ChatRequest {
    private Message[] messages;
    
    public ChatRequest(Message[] messages) {
        this.messages = messages;
    }
    
    public Message[] getMessages() {
        return messages;
    }
    
    public void setMessages(Message[] messages) {
        this.messages = messages;
    }
    
    public static class Message {
        private String role; // "system", "user", "assistant"
        private String content;
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
}
