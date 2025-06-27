<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const loginForm = reactive({
  username: '',
  password: ''
})

const loading = ref(false)

const handleLogin = () => {
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>欢迎回来</h1>
        <p>请输入您的账号信息</p>
      </div>
      
      <div class="login-form">
        <div class="form-item">
          <label>用户名</label>
          <el-input 
            v-model="loginForm.username"
            placeholder="请输入用户名" 
            :prefix-icon="User"
          />
        </div>
        
        <div class="form-item">
          <label>密码</label>
          <el-input 
            v-model="loginForm.password"
            type="password" 
            placeholder="请输入密码" 
            :prefix-icon="Lock"
            show-password
          />
        </div>
        
        <div class="form-item remember-forgot">
          <el-checkbox>记住我</el-checkbox>
        </div>
        
        <el-button 
          type="primary" 
          class="login-button" 
          @click="handleLogin"
          :loading="loading"
          round
        >
          登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background-color: white;
  border-radius: 12px;
  box-shadow: var(--shadow-md);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.login-header p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.login-form .form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.remember-forgot {
  display: flex;
  margin-bottom: 24px;
}

.remember-forgot a {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 13px;
}

.remember-forgot a:hover {
  text-decoration: underline;
}

.login-button {
  width: 100%;
  padding: 10px;
  font-size: 14px;
  font-weight: 500;
  background-color: var(--primary-color);
  border-color: var(--primary-color);
}

.login-button:hover {
  background-color: var(--primary-hover);
  border-color: var(--primary-hover);
}

:deep(.el-input__wrapper) {
  border-radius: 6px;
  box-shadow: var(--shadow-sm);
  padding: 4px 12px;
  border: 1px solid var(--border-color);
}

:deep(.el-input__wrapper:hover) {
  border-color: var(--primary-color);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(94, 106, 210, 0.2);
}

:deep(.el-button) {
  height: 42px;
  border-radius: 6px;
}
</style>
