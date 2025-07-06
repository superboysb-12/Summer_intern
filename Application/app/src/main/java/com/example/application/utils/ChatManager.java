package com.example.application.utils;

import android.content.Context;
import android.util.Log;

import com.example.application.api.ApiClient;
import com.example.application.api.ChatAPI;
import com.example.application.model.ChatMessage;
import com.example.application.model.ChatRequest;
import com.example.application.model.ChatResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatManager {
    private static final String TAG = "ChatManager";
    private ChatAPI chatAPI;
    private PreferenceManager preferenceManager;
    private List<ChatRequest.Message> conversationHistory;
    
    public ChatManager(Context context) {
        this.chatAPI = ApiClient.getChatAPI();
        this.preferenceManager = PreferenceManager.getInstance(context);
        this.conversationHistory = new ArrayList<>();
        
        // 添加系统提示词
        conversationHistory.add(new ChatRequest.Message("system", "你是一个教育软件对话助手"));
    }
    
    public interface ChatCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public void sendMessage(String userMessage, ChatCallback callback) {
        // 添加用户消息到对话历史
        conversationHistory.add(new ChatRequest.Message("user", userMessage));
        
        // 创建请求
        ChatRequest.Message[] messages = conversationHistory.toArray(new ChatRequest.Message[0]);
        ChatRequest request = new ChatRequest(messages);
        
        // 获取token
        String token = preferenceManager.getUserToken();
        if (token == null || token.isEmpty()) {
            callback.onError("未登录，请先登录");
            return;
        }
        
        String authHeader = "Bearer " + token;
        
        // 发送请求
        Call<ChatResponse> call = chatAPI.sendChatMessage(authHeader, request);
        call.enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ChatResponse chatResponse = response.body();
                    if (chatResponse.getChoices() != null && chatResponse.getChoices().length > 0) {
                        String assistantMessage = chatResponse.getChoices()[0].getMessage().getContent();
                        
                        // 添加助手回复到对话历史
                        conversationHistory.add(new ChatRequest.Message("assistant", assistantMessage));
                        
                        callback.onSuccess(assistantMessage);
                    } else {
                        callback.onError("服务器响应格式错误");
                    }
                } else {
                    String errorMsg = "请求失败: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += " - " + response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                Log.e(TAG, "Chat request failed", t);
                callback.onError("网络请求失败: " + t.getMessage());
            }
        });
    }
    
    public void clearHistory() {
        conversationHistory.clear();
        // 重新添加系统提示词
        conversationHistory.add(new ChatRequest.Message("system", "你是一个教育软件对话助手"));
    }
    
    public List<ChatRequest.Message> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }
}
