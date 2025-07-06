package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.application.api.ApiClient;
import com.example.application.api.AuthAPI;
import com.example.application.model.AuthResponse;
import com.example.application.model.User;
import com.example.application.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private TextInputEditText nameInput;
    private TextInputEditText passwordInput;
    private MaterialButton signUpButton;
    private TextView signInLink;
    private TextView passwordStrengthText;
    private View strengthBar1, strengthBar2, strengthBar3;
    private ImageButton btnGoogle, btnLinkedIn, btnApple;

    private AuthAPI authAPI;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        initAPI();
        setupClickListeners();
        setupPasswordStrengthMonitor();
    }

    private void initViews() {
        emailInput = findViewById(R.id.email_input);
        nameInput = findViewById(R.id.name_input);
        passwordInput = findViewById(R.id.password_input);
        signUpButton = findViewById(R.id.sign_up_button);
        signInLink = findViewById(R.id.sign_in_link);
        passwordStrengthText = findViewById(R.id.password_strength_text);
        strengthBar1 = findViewById(R.id.strength_bar_1);
        strengthBar2 = findViewById(R.id.strength_bar_2);
        strengthBar3 = findViewById(R.id.strength_bar_3);
        btnGoogle = findViewById(R.id.btn_google);
        btnLinkedIn = findViewById(R.id.btn_linkedin);
        btnApple = findViewById(R.id.btn_apple);
    }

    private void initAPI() {
        authAPI = ApiClient.getAuthAPI();
        preferenceManager = PreferenceManager.getInstance(this);
    }

    private void setupClickListeners() {
        signUpButton.setOnClickListener(v -> performRegister());
        
        signInLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // 社交登录按钮
        btnGoogle.setOnClickListener(v -> {
            // TODO: 实现Google注册
            Toast.makeText(this, "Google注册功能待实现", Toast.LENGTH_SHORT).show();
        });

        btnLinkedIn.setOnClickListener(v -> {
            // TODO: 实现LinkedIn注册
            Toast.makeText(this, "LinkedIn注册功能待实现", Toast.LENGTH_SHORT).show();
        });

        btnApple.setOnClickListener(v -> {
            // TODO: 实现Apple注册
            Toast.makeText(this, "Apple注册功能待实现", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupPasswordStrengthMonitor() {
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updatePasswordStrength(String password) {
        int strength = calculatePasswordStrength(password);
        
        // 重置所有强度条
        strengthBar1.setAlpha(0.3f);
        strengthBar2.setAlpha(0.3f);
        strengthBar3.setAlpha(0.3f);
        
        switch (strength) {
            case 1: // 弱
                strengthBar1.setAlpha(1.0f);
                passwordStrengthText.setText("弱");
                passwordStrengthText.setTextColor(getResources().getColor(R.color.password_weak));
                break;
            case 2: // 中
                strengthBar1.setAlpha(1.0f);
                strengthBar2.setAlpha(1.0f);
                passwordStrengthText.setText("中等");
                passwordStrengthText.setTextColor(getResources().getColor(R.color.password_medium));
                break;
            case 3: // 强
                strengthBar1.setAlpha(1.0f);
                strengthBar2.setAlpha(1.0f);
                strengthBar3.setAlpha(1.0f);
                passwordStrengthText.setText(getString(R.string.password_strength_strong));
                passwordStrengthText.setTextColor(getResources().getColor(R.color.password_strong_text));
                break;
            default:
                passwordStrengthText.setText("");
                break;
        }
    }

    private int calculatePasswordStrength(String password) {
        if (TextUtils.isEmpty(password)) return 0;
        
        int score = 0;
        
        // 长度检查
        if (password.length() >= 8) score++;
        
        // 包含数字
        if (password.matches(".*\\d.*")) score++;
        
        // 包含大小写字母
        if (password.matches(".*[a-z].*") && password.matches(".*[A-Z].*")) score++;
        
        // 包含特殊字符
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score++;
        
        if (score <= 1) return 1; // 弱
        if (score <= 2) return 2; // 中
        return 3; // 强
    }

    private void performRegister() {
        String email = emailInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("请输入邮箱");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("请输入有效的邮箱地址");
            return;
        }

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("请输入姓名");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("请输入密码");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("密码长度至少6位");
            return;
        }

        // 显示加载状态
        setLoadingState(true);

        // 创建User对象用于注册
        User user = new User(name, email, name, password); // 使用name作为username

        // 调用API
        Call<AuthResponse> call = authAPI.register(user);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoadingState(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    
                    if (authResponse.isSuccess()) {
                        handleSuccessfulRegister(authResponse);
                    } else {
                        // 注册失败
                        Toast.makeText(RegisterActivity.this, 
                                authResponse.getMessage() != null ? authResponse.getMessage() : "注册失败", 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 网络错误或服务器错误
                    String errorMessage = "网络错误，请稍后重试";
                    if (response.code() == 409) {
                        errorMessage = "用户名或邮箱已存在";
                    } else if (response.code() >= 500) {
                        errorMessage = "服务器错误，请稍后重试";
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoadingState(false);
                Toast.makeText(RegisterActivity.this, "网络连接失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSuccessfulRegister(AuthResponse authResponse) {
        // 注册成功 - 保存用户信息
        preferenceManager.setLoggedIn(true);
        
        if (authResponse.getToken() != null) {
            preferenceManager.setUserToken(authResponse.getToken());
        }
        
        if (authResponse.getUser() != null) {
            preferenceManager.saveUserInfo(
                authResponse.getUser().getId(),
                authResponse.getUser().getName(),
                authResponse.getUser().getEmail()
            );
        }
        
        Toast.makeText(RegisterActivity.this, 
                authResponse.getMessage() != null ? authResponse.getMessage() : "注册成功", 
                Toast.LENGTH_SHORT).show();
        
        // 跳转到主页面
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoadingState(boolean isLoading) {
        signUpButton.setEnabled(!isLoading);
        signUpButton.setText(isLoading ? getString(R.string.loading) : getString(R.string.sign_up));
        
        // 禁用/启用输入框
        emailInput.setEnabled(!isLoading);
        nameInput.setEnabled(!isLoading);
        passwordInput.setEnabled(!isLoading);
    }
}
