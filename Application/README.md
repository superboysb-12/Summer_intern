# Android登录注册应用

这是一个基于Figma设计稿构建的Android登录注册应用，实现了现代化的用户界面和完整的认证功能。

## 功能特性

### 🎨 UI设计
- 基于Figma设计稿的现代化界面
- 深色主题配色方案
- 渐变背景和按钮效果
- 响应式布局设计

### 🔐 认证功能
- 用户登录
- 用户注册  
- 密码强度指示器
- 忘记密码（待实现）
- 社交登录预留接口（Google、LinkedIn、Apple）

### 🏗️ 技术架构
- Material Design组件
- Retrofit网络库
- SharedPreferences用户状态管理
- MVP架构模式

## 项目结构

```
app/src/main/
├── java/com/example/application/
│   ├── MainActivity.java          # 主Activity
│   ├── LoginActivity.java         # 登录页面
│   ├── RegisterActivity.java      # 注册页面
│   ├── api/
│   │   ├── AuthAPI.java          # 认证API接口
│   │   └── ApiClient.java        # API客户端管理
│   ├── model/
│   │   ├── User.java             # 用户模型
│   │   ├── LoginRequest.java     # 登录请求模型
│   │   ├── RegisterRequest.java  # 注册请求模型
│   │   └── AuthResponse.java     # 认证响应模型
│   └── utils/
│       └── PreferenceManager.java # 用户偏好管理
└── res/
    ├── layout/
    │   ├── activity_login.xml     # 登录页面布局
    │   ├── activity_register.xml  # 注册页面布局
    │   └── activity_main.xml      # 主页面布局
    ├── drawable/                  # 图标和背景资源
    ├── values/
    │   ├── colors.xml            # 颜色定义
    │   └── strings.xml           # 字符串资源
    └── ...
```

## 后端API集成

### 1. 配置服务器URL

在 `ApiClient.java` 中更新 `BASE_URL`：

```java
private static final String BASE_URL = "https://your-backend-server.com/";
```

### 2. API端点

应用预期的后端API端点：

#### 登录接口
```
POST /api/auth/login
Content-Type: application/json

请求体:
{
    "username": "user@example.com",
    "password": "password123"
}

响应:
{
    "success": true,
    "message": "登录成功",
    "token": "jwt_token_here",
    "user": {
        "id": "user_id",
        "name": "用户姓名",
        "email": "user@example.com",
        "username": "username"
    }
}
```

#### 注册接口
```
POST /api/auth/register
Content-Type: application/json

请求体:
{
    "name": "用户姓名",
    "email": "user@example.com", 
    "password": "password123"
}

响应:
{
    "success": true,
    "message": "注册成功",
    "token": "jwt_token_here",
    "user": {
        "id": "user_id",
        "name": "用户姓名",
        "email": "user@example.com"
    }
}
```

#### 忘记密码接口（待实现）
```
POST /api/auth/forgot-password
Content-Type: application/json

请求体:
{
    "email": "user@example.com"
}
```

#### 社交登录接口（待实现）
```
POST /api/auth/social/google
POST /api/auth/social/linkedin  
POST /api/auth/social/apple

请求体:
{
    "token": "social_platform_token"
}
```

### 3. 错误处理

API应返回适当的HTTP状态码：
- `200`: 成功
- `400`: 请求参数错误
- `401`: 认证失败
- `409`: 用户已存在（注册时）
- `500`: 服务器内部错误

错误响应格式：
```json
{
    "success": false,
    "message": "错误描述信息"
}
```

## 开发配置

### 1. 环境要求
- Android Studio Arctic Fox或更高版本
- Android SDK 24+
- Java 11

### 2. 依赖库
```gradle
// 网络请求
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

// UI组件
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.cardview:cardview:1.0.0'
```

### 3. 权限配置
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 待实现功能

- [ ] 忘记密码功能
- [ ] Google社交登录
- [ ] LinkedIn社交登录  
- [ ] Apple社交登录
- [ ] 邮箱验证
- [ ] 用户资料编辑
- [ ] 退出登录功能
- [ ] 记住登录状态
- [ ] 生物识别登录

## 设计稿来源

本应用UI基于以下Figma设计稿：
- 登录页面: [Figma登录设计](https://www.figma.com/design/1etkQIc7gjMH0TDSz01ZaZ/Login-Mobile-App-Desgin-Free--Community-?node-id=1-5&t=5zz4Vr98v8sqgsbb-4)
- 注册页面: [Figma注册设计](https://www.figma.com/design/1etkQIc7gjMH0TDSz01ZaZ/Login-Mobile-App-Desgin-Free--Community-?node-id=1-125&t=5zz4Vr98v8sqgsbb-4)

## 许可证

MIT License
