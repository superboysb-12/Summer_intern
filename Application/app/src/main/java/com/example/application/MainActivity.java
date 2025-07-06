package com.example.application;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.application.utils.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private PreferenceManager preferenceManager;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化偏好设置管理器
        preferenceManager = PreferenceManager.getInstance(this);
        
        // 检查用户是否已登录
        if (!preferenceManager.isLoggedIn()) {
            // 如果未登录，跳转到登录页面
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main);
        
        setupBottomNavigation();
    }
    
    private void setupBottomNavigation() {
        // 获取NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            
            // 获取BottomNavigationView
            BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
            
            // 将BottomNavigationView与NavController关联
            NavigationUI.setupWithNavController(bottomNavigation, navController);
        }
    }
}