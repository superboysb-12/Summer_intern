﻿package com.XuebaoMaster.backend.TeachingPlanGenerator.model;

import java.util.HashMap;
import java.util.Map;

public class ChatMessageRequest {
    private Map<String, Object> inputs = new HashMap<>();
    private String query;
    private String response_mode;
    private String conversation_id;
    private String user;

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getResponse_mode() {
        return response_mode;
    }

    public void setResponse_mode(String response_mode) {
        this.response_mode = response_mode;
    }

    public String getConversation_id() {
        return conversation_id;
    }

    public void setConversation_id(String conversation_id) {
        this.conversation_id = conversation_id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}