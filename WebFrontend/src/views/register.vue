<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Phone } from '@element-plus/icons-vue'
import { useCounterStore } from '../stores/counter'

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: ''
})

const rules = ref({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validatePass, trigger: 'blur' }
  ]
})

function validatePass(rule, value, callback) {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.value.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const store = useCounterStore()

const loading = ref(false)

const handleRegister = async () => {
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.error('两次输入密码不一致')
    return
  }
  
  loading.value = true
  try {
    const result = await store.register(
      registerForm.value.username, 
      registerForm.value.password,
      registerForm.value.email,
      registerForm.value.phone
    )
    if (result) {
      ElMessage.success('注册成功')
      loading.value = false
      // 可以在这里添加跳转到登录页面的逻辑
    } else {
      ElMessage.error('注册失败')
      loading.value = false
    }
  } catch (error) {
    ElMessage.error('注册出错: ' + error.message)
    loading.value = false
  }
}
</script>

<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h1>创建账号</h1>
        <p>请填写以下信息进行注册</p>
      </div>
      
      <div class="register-form">
        <div class="form-item">
          <label>用户名 <span class="required">*</span></label>
          <el-input 
            v-model="registerForm.username"
            placeholder="请输入用户名" 
            :prefix-icon="User"
          />
        </div>
        
        <div class="form-item">
          <label>密码 <span class="required">*</span></label>
          <el-input 
            v-model="registerForm.password"
            type="password" 
            placeholder="请输入密码" 
            :prefix-icon="Lock"
            show-password
          />
        </div>
        
        <div class="form-item">
          <label>确认密码 <span class="required">*</span></label>
          <el-input 
            v-model="registerForm.confirmPassword"
            type="password" 
            placeholder="请再次输入密码" 
            :prefix-icon="Lock"
            show-password
          />
        </div>
        
        <div class="form-item">
          <label>邮箱 <span class="optional">(选填)</span></label>
          <el-input 
            v-model="registerForm.email"
            placeholder="请输入邮箱" 
            :prefix-icon="Message"
          />
        </div>
        
        <div class="form-item">
          <label>手机号 <span class="optional">(选填)</span></label>
          <el-input 
            v-model="registerForm.phone"
            placeholder="请输入手机号" 
            :prefix-icon="Phone"
          />
        </div>
        
        <el-button 
          type="primary" 
          class="register-button" 
          @click="handleRegister"
          :loading="loading"
          round
        >
          注册
        </el-button>
        
        <div class="login-link">
          <span>已有账号？</span>
          <el-button 
            type="text" 
            class="login-button"
            @click="$router.push('/login')"
          >
            立即登录
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.register-box {
  width: 400px;
  padding: 40px;
  background-color: white;
  border-radius: 12px;
  box-shadow: var(--shadow-md);
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.register-header p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.register-form .form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.required {
  color: #f56c6c;
  margin-left: 2px;
}

.optional {
  color: #909399;
  font-size: 12px;
  font-weight: normal;
}

.register-button {
  width: 100%;
  padding: 10px;
  font-size: 14px;
  font-weight: 500;
  background-image: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  border: none;
  transition: all 0.3s ease;
}

.register-button:hover {
  background-image: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.login-link {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: var(--text-secondary);
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-button {
  padding: 0 4px;
  font-weight: 500;
  color: var(--primary-color);
  margin-left: 4px;
}

.login-button:hover {
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
