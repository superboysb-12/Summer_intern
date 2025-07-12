<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 按钮状态
const loading = ref(false)

// 生成今日所有学习情绪
const generateAllEmotionScores = async () => {
  loading.value = true
  try {
    await axios.post(BaseUrl + 'student-emotions/calculate-scores/all', {}, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    ElMessage.success('情绪数据生成成功')
  } catch (error) {
    console.error('生成情绪数据失败:', error)
    ElMessage.error('生成情绪数据失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="utility-tools-container">
    <div class="page-header">
      <h2>实用工具</h2>
      <p>系统相关实用工具集合</p>
    </div>
    
    <!-- 学习情绪分析工具 -->
    <el-card class="tool-card">
      <template #header>
        <div class="card-header">
          <h3>学习情绪分析</h3>
        </div>
      </template>
      
      <div class="tool-content">
        <el-button 
          type="primary" 
          @click="generateAllEmotionScores" 
          :loading="loading"
          size="large"
        >
          生成今日情绪分析
        </el-button>
        <p class="tool-description">点击按钮生成今日所有学习情绪数据</p>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.utility-tools-container {
  padding: 20px 0;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
}

.page-header p {
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.tool-card {
  margin-bottom: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.tool-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.tool-description {
  margin-top: 12px;
  color: var(--text-secondary);
  font-size: 14px;
}
</style> 