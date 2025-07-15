<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Close } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'

const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

const notifications = ref([])
const notificationCount = ref(0)
const loading = ref(false)
const drawerVisible = ref(false)

// Refresh interval
const refreshInterval = ref(null)
const REFRESH_INTERVAL = 60000 // Refresh every minute

// Get notification count
const getNotificationCount = async () => {
  try {
    const response = await axios.get(`${BaseUrl}messages/count`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.count !== undefined) {
      notificationCount.value = response.data.count
    }
  } catch (error) {
    console.error('获取通知数量失败:', error)
  }
}

// Get user notifications
const getNotifications = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${BaseUrl}messages/my-messages`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (Array.isArray(response.data)) {
      notifications.value = response.data
      notificationCount.value = response.data.length
    } else {
      notifications.value = []
      notificationCount.value = 0
    }
  } catch (error) {
    console.error('获取通知失败:', error)
    ElMessage.error('获取通知失败')
  } finally {
    loading.value = false
  }
}

// Toggle notification drawer
const toggleNotificationDrawer = () => {
  drawerVisible.value = !drawerVisible.value
  if (drawerVisible.value) {
    getNotifications()
  }
}

// Format date
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString()
}

// Start refresh interval
const startRefreshInterval = () => {
  if (refreshInterval.value) {
    clearInterval(refreshInterval.value)
  }
  
  refreshInterval.value = setInterval(() => {
    getNotificationCount()
  }, REFRESH_INTERVAL)
}

// Component mounted
onMounted(() => {
  getNotificationCount()
  startRefreshInterval()
})

// Clean up on unmount
onUnmounted(() => {
  if (refreshInterval.value) {
    clearInterval(refreshInterval.value)
  }
})
</script>

<template>
  <div class="notification-panel">
    <el-badge :value="notificationCount" :max="99" class="notification-badge">
      <el-button
        :icon="Bell"
        circle
        type="primary"
        text
        @click="toggleNotificationDrawer"
        class="notification-button"
      />
    </el-badge>
    
    <!-- Notification drawer -->
    <el-drawer
      v-model="drawerVisible"
      title="系统通知"
      direction="rtl"
      size="350px"
      :before-close="() => drawerVisible = false"
    >
      <div v-loading="loading" class="notification-list">
        <div v-if="notifications.length === 0" class="notification-empty">
          <el-icon :size="32" class="empty-icon"><Bell /></el-icon>
          <p>暂无通知</p>
        </div>
        
        <el-card
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-item"
          shadow="hover"
        >
          <div class="notification-header">
            <h4>{{ notification.title }}</h4>
            <span class="notification-time">{{ formatDate(notification.createdAt) }}</span>
          </div>
          <div class="notification-content">
            {{ notification.content }}
          </div>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.notification-panel {
  display: inline-block;
  margin-right: 20px;
}

.notification-badge {
  margin: 0;
}

.notification-button {
  font-size: 20px;
  color: white;
  height: 32px;
  width: 32px;
}

.notification-list {
  padding: 10px;
  height: 100%;
  overflow-y: auto;
}

.notification-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;
}

.empty-icon {
  margin-bottom: 10px;
}

.notification-item {
  margin-bottom: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notification-header h4 {
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  font-weight: 600;
  color: var(--text-primary, #303133);
}

.notification-time {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
  margin-left: 10px;
}

.notification-content {
  color: #606266;
  font-size: 14px;
  white-space: pre-line;
}
</style> 