package com.example.application.api;

import com.example.application.model.AuthResponse;
import com.example.application.model.LoginResponse;
import com.example.application.model.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthAPI {
    
    /**
     * Login endpoint
     * 根据接口文档: POST /users/login
     */
    @POST("users/login")
    Call<LoginResponse> login(@Body User user);
    
    /**
     * Register endpoint
     * 根据接口文档: POST /users/register
     */
    @POST("users/register")
    Call<AuthResponse> register(@Body User user);
    
    /**
     * Forgot password endpoint
     * 后端接口示例: POST /api/auth/forgot-password
     */
    @POST("api/auth/forgot-password")
    Call<AuthResponse> forgotPassword(@Body String email);
    
    /**
     * Social login endpoints - 预留给社交登录
     * 后端接口示例: POST /api/auth/social/google
     */
    @POST("api/auth/social/google")
    Call<AuthResponse> loginWithGoogle(@Body String token);
    
    @POST("api/auth/social/linkedin")
    Call<AuthResponse> loginWithLinkedIn(@Body String token);
    
    @POST("api/auth/social/apple")
    Call<AuthResponse> loginWithApple(@Body String token);
}
