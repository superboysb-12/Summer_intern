package com.example.application.api;

import com.example.application.model.ChatRequest;
import com.example.application.model.ChatResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ChatAPI {
    
    @POST("api/deepseek/chat")
    Call<ChatResponse> sendChatMessage(
        @Header("Authorization") String authorization,
        @Body ChatRequest request
    );
}
