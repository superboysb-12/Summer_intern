package com.example.application.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    
    // TODO: 替换为您的后端服务器URL
    // 认证服务器地址
    private static final String AUTH_BASE_URL = "http://10.0.2.2:8080/";
    // Chat API服务器地址 (可以是不同的服务器)
    private static final String CHAT_BASE_URL = "http://10.0.2.2:8080/";
    
    private static Retrofit retrofit;
    private static Retrofit chatRetrofit;
    private static AuthAPI authAPI;
    
    /**
     * 获取Retrofit实例
     */
    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // 创建HTTP日志拦截器用于调试
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // 创建OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();
            
            // 创建Retrofit实例
            retrofit = new Retrofit.Builder()
                    .baseUrl(AUTH_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
    
    /**
     * 获取认证API实例
     */
    public static AuthAPI getAuthAPI() {
        if (authAPI == null) {
            authAPI = getRetrofitInstance().create(AuthAPI.class);
        }
        return authAPI;
    }
    
    /**
     * 设置基础URL（用于不同环境配置）
     */
    public static void setBaseUrl(String baseUrl) {
        retrofit = null;
        authAPI = null;
        // 重新创建实例时会使用新的URL
    }

    /**
     * 获取Chat专用的Retrofit实例
     */
    public static Retrofit getChatRetrofitInstance() {
        if (chatRetrofit == null) {
            // 创建HTTP日志拦截器用于调试
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // 创建OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();
            
            // 创建Chat专用的Retrofit实例
            chatRetrofit = new Retrofit.Builder()
                    .baseUrl(CHAT_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return chatRetrofit;
    }

    /**
     * 获取聊天API实例
     */
    public static ChatAPI getChatAPI() {
        return getChatRetrofitInstance().create(ChatAPI.class);
    }
}
