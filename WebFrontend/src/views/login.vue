<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useCounterStore } from '../stores/counter'

const loginForm = ref({
  username: '',
  password: ''
})

const rules = ref({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
})

const store = useCounterStore()

const loading = ref(false)

const handleLogin = async () => {
  loading.value = true
  const result = await store.login(loginForm.value.username, loginForm.value.password)
  if (result) {
    ElMessage.success('登录成功')
    loading.value = false
  } else {
    ElMessage.error('登录失败')
    loading.value = false
  }
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
        
        <div class="register-link">
          <span>还没有账号？</span>
          <el-button 
            type="text" 
            class="register-button"
            @click="$router.push('/register')"
          >
            立即注册
          </el-button>
        </div>
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
  background-image: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  border: none;
  transition: all 0.3s ease;
}

.login-button:hover {
  background-image: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.register-link {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: var(--text-secondary);
  display: flex;
  justify-content: center;
  align-items: center;
}

.register-button {
  padding: 0 4px;
  font-weight: 500;
  color: var(--primary-color);
  margin-left: 4px;
}

.register-button:hover {
  color: var(--primary-hover);
  text-decoration: underline;
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
