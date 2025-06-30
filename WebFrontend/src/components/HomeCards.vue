<script setup>
import { ref } from 'vue'
import { 
  User, 
  Document, 
  Bell, 
  Promotion 
} from '@element-plus/icons-vue'

const cards = [
  { 
    title: '用户总数', 
    value: '1,234', 
    icon: User, 
    color: 'linear-gradient(135deg, #a5b4fc 0%, #818cf8 100%)',
    growth: '+12%'
  },
  { 
    title: '今日访问', 
    value: '423', 
    icon: Promotion, 
    color: 'linear-gradient(135deg, #93c5fd 0%, #60a5fa 100%)',
    growth: '+5%'
  },
  { 
    title: '文档数量', 
    value: '89', 
    icon: Document, 
    color: 'linear-gradient(135deg, #6ee7b7 0%, #34d399 100%)',
    growth: '+8%'
  },
  { 
    title: '系统消息', 
    value: '12', 
    icon: Bell, 
    color: 'linear-gradient(135deg, #fdba74 0%, #fb923c 100%)',
    growth: '+2'
  }
]
</script>

<template>
  <div class="home-container">
    <div class="page-header">
      <h2>控制面板</h2>
      <p>欢迎回来，这里是您的系统概览</p>
    </div>
    
    <div class="card-grid">
      <div v-for="(card, index) in cards" :key="index" class="stat-card" :style="{ backgroundImage: card.color }">
        <div class="card-content">
          <div class="card-value">{{ card.value }}</div>
          <div class="card-title">{{ card.title }}</div>
          <div class="card-growth">{{ card.growth }}</div>
        </div>
        <div class="card-icon">
          <el-icon :size="40"><component :is="card.icon" /></el-icon>
        </div>
      </div>
    </div>
    
    <div class="card-row">
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>用户增长趋势</span>
            <el-button text>更多</el-button>
          </div>
        </template>
        <div class="chart-placeholder">
          <p>此处放置用户增长图表</p>
        </div>
      </el-card>
      
      <el-card class="table-card">
        <template #header>
          <div class="card-header">
            <span>最近活动</span>
            <el-button text>查看全部</el-button>
          </div>
        </template>
        <div class="activity-list">
          <div class="activity-item">
            <div class="activity-time">10:30</div>
            <div class="activity-content">用户 admin 登录了系统</div>
          </div>
          <div class="activity-item">
            <div class="activity-time">09:45</div>
            <div class="activity-content">新增文档 "系统使用手册.pdf"</div>
          </div>
          <div class="activity-item">
            <div class="activity-time">09:12</div>
            <div class="activity-content">用户 manager 更新了系统设置</div>
          </div>
          <div class="activity-item">
            <div class="activity-time">昨天</div>
            <div class="activity-content">系统完成了数据备份</div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
/* 首页卡片样式 */
.home-container {
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

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: hidden;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 130px;
  transition: transform 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.card-content {
  z-index: 2;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
}

.card-title {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.card-growth {
  font-size: 12px;
  background-color: rgba(255, 255, 255, 0.25);
  padding: 2px 8px;
  border-radius: 12px;
  display: inline-block;
}

.card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  width: 70px;
  height: 70px;
  z-index: 2;
}

.card-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card, .table-card {
  height: 350px;
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.chart-card:hover, .table-card:hover {
  transform: translateY(-5px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-placeholder {
  height: 280px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f9fafc;
  border-radius: 8px;
  color: #909399;
}

.activity-list {
  padding: 10px 0;
}

.activity-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-time {
  color: #909399;
  width: 60px;
  font-size: 13px;
}

.activity-content {
  flex: 1;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .card-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style> 