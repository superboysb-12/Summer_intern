package com.example.application.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 用户偏好设置管理器
 * 用于保存和获取用户登录状态、token等信息
 */
public class PreferenceManager {
    
    private static final String PREF_NAME = "app_preferences";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_TOKEN = "user_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ROLE = "user_role";
    
    private static PreferenceManager instance;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    
    private PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }
    
    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 保存登录状态
     */
    public void setLoggedIn(boolean isLoggedIn) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.apply();
    }
    
    /**
     * 获取登录状态
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    /**
     * 保存用户token
     */
    public void setUserToken(String token) {
        editor.putString(KEY_USER_TOKEN, token);
        editor.apply();
    }
    
    /**
     * 获取用户token
     */
    public String getUserToken() {
        return sharedPreferences.getString(KEY_USER_TOKEN, null);
    }
    
    /**
     * 保存用户信息
     */
    public void saveUserInfo(String userId, String userName, String userEmail) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_EMAIL, userEmail);
        editor.apply();
    }

    /**
     * 保存完整的用户信息
     */
    public void saveCompleteUserInfo(String userId, String userName, String userEmail, String username, String userRole) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_EMAIL, userEmail);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_USER_ROLE, userRole);
        editor.apply();
    }
    
    /**
     * 获取用户ID
     */
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }
    
    /**
     * 获取用户姓名
     */
    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, null);
    }
    
    /**
     * 获取用户邮箱
     */
    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }

    /**
     * 获取用户名（登录用户名）
     */
    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    /**
     * 获取用户角色
     */
    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, null);
    }
    
    /**
     * 清除所有用户数据（退出登录时使用）
     */
    public void clearUserData() {
        editor.clear();
        editor.apply();
    }
}
