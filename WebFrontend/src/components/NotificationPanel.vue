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
  <el-popover
    placement="bottom"
    :width="300"
    trigger="click"
    class="notification-popover"
  >
    <template #reference>
      <div class="notification-badge">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0">
          <el-button type="primary" text>
            <el-icon :size="20"><bell /></el-icon>
          </el-button>
        </el-badge>
      </div>
    </template>
    
    <div class="notification-header d-flex flex-col mb-sm">
      <span class="text-bold text-md mb-sm">通知</span>
      <el-tabs v-model="activeTab" class="w-full">
        <el-tab-pane label="系统通知" name="system"></el-tab-pane>
        <el-tab-pane label="私信" name="private" :badge="unreadPrivateCount || ''"></el-tab-pane>
      </el-tabs>
    </div>
    
    <div class="notification-list" v-loading="loading">
      <!-- 系统通知 -->
      <template v-if="activeTab === 'system'">
        <div v-if="!notifications || notifications.length === 0" class="empty-notification text-center p-md text-tertiary">
          暂无通知
        </div>
        <div 
          v-for="notification in notifications" 
          :key="notification.id"
          class="notification-item p-sm border-bottom cursor-pointer"
          :class="{'bg-primary-light': !notification.read}"
          @click="viewNotification(notification)"
        >
          <div class="notification-title text-sm mb-xs">
            {{ notification.title }}
          </div>
          <div class="notification-time text-xs text-tertiary">
            {{ formatTime(notification.createdAt) }}
          </div>
        </div>
        
        <div class="notification-footer d-flex justify-center p-sm border-top">
          <el-button type="primary" link @click="goToMessageCenter">查看全部</el-button>
        </div>
      </template>
      
      <!-- 私信通知 -->
      <template v-else>
        <div v-if="!privateMessages || privateMessages.length === 0" class="empty-notification text-center p-md text-tertiary">
          暂无私信
        </div>
        <div 
          v-for="message in privateMessages || []" 
          :key="message.id"
          class="notification-item p-sm border-bottom cursor-pointer"
          :class="{'bg-primary-light': !message.read}"
          @click="viewPrivateMessage(message)"
        >
          <div class="notification-sender text-bold mb-xs">
            {{ message.sender?.name || message.sender?.username }}
          </div>
          <div class="notification-title text-sm mb-xs">
            {{ message.subject }}
          </div>
          <div class="notification-time text-xs text-tertiary">
            {{ formatTime(message.sentAt) }}
          </div>
        </div>
        
        <div class="notification-footer d-flex justify-center p-sm border-top">
          <el-button type="primary" link @click="goToPrivateMessageCenter">私信中心</el-button>
        </div>
      </template>
    </div>
  </el-popover>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
  components: {
    Bell
  },
  setup() {
    const router = useRouter()
    const notifications = ref([])
    const privateMessages = ref([])
    const loading = ref(false)
    const activeTab = ref('system')
    
    // 获取系统通知
    const fetchNotifications = async () => {
      try {
        loading.value = true
        // 获取认证令牌
        const token = localStorage.getItem('token')
        const headers = token ? { Authorization: `Bearer ${token}` } : {}
        
        const response = await axios.get('http://localhost:8080/messages/user', { headers })
        notifications.value = Array.isArray(response.data) ? response.data : []
      } catch (error) {
        console.error('Error fetching notifications:', error)
        notifications.value = []
      } finally {
        loading.value = false
      }
    }
    
    // 获取私信通知
    const fetchPrivateMessages = async () => {
      try {
        loading.value = true
        // 获取认证令牌
        const token = localStorage.getItem('token')
        const headers = token ? { Authorization: `Bearer ${token}` } : {}
        
        // 确保API端点存在，如果API还未实现，添加错误处理
        try {
          const response = await axios.get('http://localhost:8080/private-messages/unread', { headers })
          privateMessages.value = Array.isArray(response.data) ? response.data : []
        } catch (apiError) {
          console.warn('Private messages API not available yet:', apiError)
          privateMessages.value = [] // 确保至少是空数组
        }
      } catch (error) {
        console.error('Error fetching private messages:', error)
        privateMessages.value = []
      } finally {
        loading.value = false
      }
    }
    
    // 计算未读通知总数
    const unreadCount = computed(() => {
      // 确保notifications.value是数组
      const systemUnread = Array.isArray(notifications.value) ? 
        notifications.value.filter(n => n && !n.read).length : 0;
        
      // 未读私信直接使用数组长度，确保privateMessages.value存在
      const privateUnread = privateMessages.value && Array.isArray(privateMessages.value) ? 
        privateMessages.value.length : 0;
      
      return systemUnread + privateUnread;
    })
    
    // 计算未读私信数量
    const unreadPrivateCount = computed(() => {
      return privateMessages.value && Array.isArray(privateMessages.value) ? 
        privateMessages.value.length : 0;
    })
    
    // 查看通知
    const viewNotification = (notification) => {
      if (!notification) return;
      ElMessage.success('查看通知: ' + notification.title)
      // 实际项目中这里应该调用标记已读API
    }
    
    // 查看私信
    const viewPrivateMessage = (message) => {
      if (!message) return;
      router.push('/manage/private-messages')
    }
    
    // 前往消息中心
    const goToMessageCenter = () => {
      router.push('/manage')
    }
    
    // 前往私信中心
    const goToPrivateMessageCenter = () => {
      router.push('/manage/private-messages')
    }
    
    // 格式化时间
    const formatTime = (timeStr) => {
      if (!timeStr) return ''
      
      const date = new Date(timeStr)
      const now = new Date()
      
      try {
        // 今天的消息只显示时间
        if (date.toDateString() === now.toDateString()) {
          return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
        
        // 今年的消息显示月日
        if (date.getFullYear() === now.getFullYear()) {
          return `${date.getMonth() + 1}月${date.getDate()}日`
        }
        
        // 往年的消息显示年月日
        return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
      } catch (error) {
        console.error('Error formatting date:', error)
        return ''
      }
    }
    
    // 轮询获取最新通知
    const setupPolling = () => {
      // 初始化阶段立即执行一次
      fetchNotifications().catch(err => {
        console.error('Initial notification fetch failed:', err)
        notifications.value = []
      })
      
      fetchPrivateMessages().catch(err => {
        console.error('Initial private messages fetch failed:', err)
        privateMessages.value = []
      })
      
      // 每分钟轮询一次
      const intervalId = setInterval(() => {
        fetchNotifications().catch(() => notifications.value = [])
        fetchPrivateMessages().catch(() => privateMessages.value = [])
      }, 60000)
      
      // 组件卸载时清除定时器
      return () => {
        clearInterval(intervalId)
      }
    }
    
    onMounted(() => {
      setupPolling()
    })
    
    return {
      notifications,
      privateMessages,
      loading,
      activeTab,
      unreadCount,
      unreadPrivateCount,
      viewNotification,
      viewPrivateMessage,
      goToMessageCenter,
      goToPrivateMessageCenter,
      formatTime
    }
  }
}
</script>

<style scoped>
.notification-badge {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.notification-list {
  max-height: 300px;
  overflow-y: auto;
}

.notification-item:hover {
  background-color: var(--bg-tertiary);
}

.notification-popover {
  box-shadow: var(--shadow-md);
  border-radius: var(--radius-md);
}
</style> 