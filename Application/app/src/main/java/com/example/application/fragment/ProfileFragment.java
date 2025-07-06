package com.example.application.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.application.LoginActivity;
import com.example.application.R;
import com.example.application.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {

    private PreferenceManager preferenceManager;
    private TextView welcomeText;
    private TextView userInfoText;
    private TextView tokenText;
    private MaterialButton logoutButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        preferenceManager = PreferenceManager.getInstance(requireContext());
        
        initViews(view);
        setupUserInfo();
        setupLogoutButton();
    }

    private void initViews(View view) {
        welcomeText = view.findViewById(R.id.welcome_text);
        userInfoText = view.findViewById(R.id.user_info);
        tokenText = view.findViewById(R.id.token_text);
        logoutButton = view.findViewById(R.id.logout_button);
    }

    private void setupUserInfo() {
        String userName = preferenceManager.getUserName();
        String userEmail = preferenceManager.getUserEmail();
        String username = preferenceManager.getUsername();
        String userRole = preferenceManager.getUserRole();
        String token = preferenceManager.getUserToken();

        // 设置欢迎文本
        if (userName != null) {
            welcomeText.setText("欢迎回来，" + userName + "!");
        } else {
            welcomeText.setText("欢迎回来！");
        }

        // 设置用户信息
        StringBuilder userInfo = new StringBuilder();
        if (userName != null) {
            userInfo.append("姓名：").append(userName).append("\n");
        }
        if (userEmail != null) {
            userInfo.append("邮箱：").append(userEmail).append("\n");
        }
        if (username != null) {
            userInfo.append("用户名：").append(username).append("\n");
        }
        if (userRole != null) {
            userInfo.append("角色：").append(userRole).append("\n");
        }
        String userId = preferenceManager.getUserId();
        if (userId != null) {
            userInfo.append("用户ID：").append(userId);
        }

        userInfoText.setText(userInfo.toString());

        // 设置Token信息（仅显示前20个字符用于调试）
        if (token != null) {
            String displayToken = token.length() > 20 ? token.substring(0, 20) + "..." : token;
            tokenText.setText("Token：" + displayToken);
        } else {
            tokenText.setText("Token：无");
        }
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> {
            // 清除用户数据
            preferenceManager.clearUserData();

            Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show();

            // 跳转到登录页面
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }
}
