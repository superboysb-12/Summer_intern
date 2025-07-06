package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.application.api.ApiClient;
import com.example.application.api.AuthAPI;
import com.example.application.model.LoginResponse;
import com.example.application.model.User;
import com.example.application.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private MaterialButton signInButton;
    private TextView forgotPassword;
    private TextView signUpLink;
    private ImageButton btnGoogle, btnLinkedIn, btnApple;

    private AuthAPI authAPI;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        initAPI();
        setupClickListeners();
    }

    private void initViews() {
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        signInButton = findViewById(R.id.sign_in_button);
        forgotPassword = findViewById(R.id.forgot_password);
        signUpLink = findViewById(R.id.sign_up_link);
        btnGoogle = findViewById(R.id.btn_google);
        btnLinkedIn = findViewById(R.id.btn_linkedin);
        btnApple = findViewById(R.id.btn_apple);
    }

    private void initAPI() {
        authAPI = ApiClient.getAuthAPI();
        preferenceManager = PreferenceManager.getInstance(this);
    }

    private void setupClickListeners() {
        signInButton.setOnClickListener(v -> performLogin());
        
        signUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        forgotPassword.setOnClickListener(v -> {
            // TODO: 实现忘记密码功能
            Toast.makeText(this, "忘记密码功能待实现", Toast.LENGTH_SHORT).show();
        });

        // 社交登录按钮
        btnGoogle.setOnClickListener(v -> {
            // TODO: 实现Google登录
            Toast.makeText(this, "Google登录功能待实现", Toast.LENGTH_SHORT).show();
        });

        btnLinkedIn.setOnClickListener(v -> {
            // TODO: 实现LinkedIn登录
            Toast.makeText(this, "LinkedIn登录功能待实现", Toast.LENGTH_SHORT).show();
        });

        btnApple.setOnClickListener(v -> {
            // TODO: 实现Apple登录
            Toast.makeText(this, "Apple登录功能待实现", Toast.LENGTH_SHORT).show();
        });
    }

    private void performLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("请输入用户名");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("请输入密码");
            return;
        }

        // 检查测试账号
        if ("test".equals(username) && "test".equals(password)) {
            handleTestLogin();
            return;
        }

        // 显示加载状态
        setLoadingState(true);

        // 创建User对象用于登录
        User user = new User(username, password);

        // 调用API
        Call<LoginResponse> call = authAPI.login(user);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoadingState(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    handleSuccessfulLogin(loginResponse);
                } else {
                    // 网络错误或服务器错误
                    String errorMessage = "网络错误，请稍后重试";
                    if (response.code() == 401) {
                        errorMessage = "用户名或密码错误";
                    } else if (response.code() >= 500) {
                        errorMessage = "服务器错误，请稍后重试";
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoadingState(false);
                Toast.makeText(LoginActivity.this, "网络连接失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleTestLogin() {
        // 测试账号直接登录
        Toast.makeText(this, "测试账号登录成功", Toast.LENGTH_SHORT).show();
        
        // 保存测试用户信息
        preferenceManager.setLoggedIn(true);
        preferenceManager.setUserToken("test_token");
        preferenceManager.saveUserInfo("test_id", "测试用户", "test@example.com");
        
        // 跳转到主页面
        navigateToMainActivity();
    }

    private void handleSuccessfulLogin(LoginResponse loginResponse) {
        // 登录成功 - 保存用户信息
        preferenceManager.setLoggedIn(true);
        
        if (loginResponse.getToken() != null) {
            preferenceManager.setUserToken(loginResponse.getToken());
        }
        
        if (loginResponse.getUser() != null) {
            User user = loginResponse.getUser();
            preferenceManager.saveCompleteUserInfo(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUsername(),
                user.getUserRole()
            );
        }
        
        Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
        
        // 跳转到主页面
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoadingState(boolean isLoading) {
        signInButton.setEnabled(!isLoading);
        signInButton.setText(isLoading ? getString(R.string.loading) : getString(R.string.sign_in));
        
        // 禁用/启用输入框
        usernameInput.setEnabled(!isLoading);
        passwordInput.setEnabled(!isLoading);
    }
}
