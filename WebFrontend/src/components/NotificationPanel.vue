<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElDialog, ElButton } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

const notifications = ref([])
const privateMessages = ref([])
const loading = ref(false)
const activeTab = ref('system')
const refreshInterval = ref(null)
const REFRESH_INTERVAL = 60000 // Refresh every minute

const dialogVisible = ref(false)
const selectedMessage = ref(null)

// 获取系统通知
const fetchNotifications = async () => {
  try {
    loading.value = true
    const token = localStorage.getItem('token')
    const headers = token ? { Authorization: `Bearer ${token}` } : {}
    const response = await axios.get(`${BaseUrl}messages/my-messages`, { headers })
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
    const token = localStorage.getItem('token')
    const headers = token ? { Authorization: `Bearer ${token}` } : {}
    const response = await axios.get(`${BaseUrl}private-messages/unread`, { headers })
    privateMessages.value = Array.isArray(response.data) ? response.data : []
  } catch (apiError) {
    console.warn('Private messages API not available yet:', apiError)
    privateMessages.value = []
  } finally {
    loading.value = false
  }
}

// 计算未读通知总数
const unreadCount = computed(() => {
  const systemUnread = Array.isArray(notifications.value)
    ? notifications.value.filter(n => n && !n.read).length
    : 0
  const privateUnread = Array.isArray(privateMessages.value)
    ? privateMessages.value.filter(m => m && !m.read).length
    : 0
  return systemUnread + privateUnread
})

const unreadPrivateCount = computed(() => {
    return Array.isArray(privateMessages.value)
    ? privateMessages.value.filter(m => m && !m.read).length
    : 0
})

const handleContentClick = () => {
  if (activeTab.value === 'system') {
    fetchNotifications()
  } else {
    fetchPrivateMessages()
  }
}

const viewNotification = (notification) => {
  selectedMessage.value = { ...notification, type: 'system' }
  dialogVisible.value = true
}

const viewPrivateMessage = async (message) => {
  selectedMessage.value = { ...message, type: 'private' }
  dialogVisible.value = true
  
  if (!message.read) {
    try {
      const token = getToken()
      const headers = token ? { Authorization: `Bearer ${token}` } : {}
      await axios.put(`${BaseUrl}private-messages/${message.id}/read`, {}, { headers })
      const foundMessage = privateMessages.value.find(m => m.id === message.id)
      if (foundMessage) {
        foundMessage.read = true
      }
    } catch (error) {
      console.error('Failed to mark private message as read:', error)
      ElMessage.error('无法将私信标记为已读')
    }
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

const goToMessageCenter = () => {
  router.push('/manage/messages')
}

const goToPrivateMessageCenter = () => {
  router.push('/manage/private-messages')
}

// Start refresh interval
const startRefreshInterval = () => {
  if (refreshInterval.value) {
    clearInterval(refreshInterval.value)
  }
  
  refreshInterval.value = setInterval(() => {
    fetchNotifications()
    fetchPrivateMessages()
  }, REFRESH_INTERVAL)
}

// Component mounted
onMounted(() => {
  fetchNotifications()
  fetchPrivateMessages()
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
  <div>
    <el-popover
      placement="bottom"
      :width="300"
      trigger="click"
      class="notification-popover"
      @show="handleContentClick"
    >
      <template #reference>
        <div class="notification-badge">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" type="danger">
            <el-button type="primary" text>
              <el-icon :size="20"><bell /></el-icon>
            </el-button>
          </el-badge>
        </div>
      </template>
      
      <div class="notification-header d-flex flex-col mb-sm">
        <span class="text-bold text-md mb-sm">通知中心</span>
        <el-tabs v-model="activeTab" class="w-full" @tab-click="handleContentClick">
          <el-tab-pane label="系统通知" name="system"></el-tab-pane>
          <el-tab-pane name="private">
              <template #label>
                  <span>私信</span>
                  <el-badge :value="unreadPrivateCount" :hidden="unreadPrivateCount === 0" type="danger" class="private-message-badge"/>
              </template>
          </el-tab-pane>
        </el-tabs>
      </div>
      
      <div class="notification-list" v-loading="loading">
        <!-- 系统通知 -->
        <template v-if="activeTab === 'system'">
          <div v-if="!notifications || notifications.length === 0" class="empty-notification text-center p-md text-tertiary">
            暂无通知
          </div>
          <div 
            v-else
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
            v-else
            v-for="message in privateMessages" 
            :key="message.id"
            class="notification-item p-sm border-bottom cursor-pointer"
            :class="{'bg-primary-light': !message.read}"
            @click="viewPrivateMessage(message)"
          >
            <div class="notification-sender text-bold mb-xs">
              {{ message.sender?.username }}
            </div>
            <div class="notification-title text-sm mb-xs">
              {{ message.subject }}
            </div>
            <div class="notification-time text-xs text-tertiary">
              {{ formatTime(message.createdAt) }}
            </div>
          </div>
          
          <div class="notification-footer d-flex justify-center p-sm border-top">
            <el-button type="primary" link @click="goToPrivateMessageCenter">私信中心</el-button>
          </div>
        </template>
      </div>
    </el-popover>

    <el-dialog
      v-model="dialogVisible"
      :title="selectedMessage?.type === 'system' ? '系统通知详情' : '私信详情'"
      width="600px"
    >
      <div v-if="selectedMessage">
          <div class="message-detail-header">
              <h3>{{ selectedMessage.subject || selectedMessage.title }}</h3>
              <p v-if="selectedMessage.type === 'private'">
                  <strong>发件人:</strong> {{ selectedMessage.sender?.username }}
              </p>
          </div>
          <div class="message-detail-content">
              <p>{{ selectedMessage.content }}</p>
          </div>
          <div class="message-detail-footer">
              时间: {{ formatTime(selectedMessage.createdAt) }}
          </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.notification-badge {
  cursor: pointer;
  transition: transform 0.2s ease;
}

.notification-badge:hover {
  transform: scale(1.05);
}

.notification-header {
  border-bottom: 1px solid #e4e7ed;
}

.notification-list {
  max-height: 300px;
  overflow-y: auto;
}

.notification-item {
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-footer {
  border-top: 1px solid #e4e7ed;
}

.empty-notification {
  color: #909399;
}

.bg-primary-light {
    background-color: #ecf5ff;
}

.text-bold {
    font-weight: bold;
}

.text-sm {
    font-size: 14px;
}

.text-xs {
    font-size: 12px;
}
.text-md {
    font-size: 16px;
}

.text-tertiary {
    color: #909399;
}

.p-sm {
    padding: 8px;
}

.p-md {
    padding: 16px;
}

.mb-xs {
    margin-bottom: 4px;
}

.mb-sm {
    margin-bottom: 8px;
}
.border-bottom {
    border-bottom: 1px solid #e4e7ed;
}

.border-top {
    border-top: 1px solid #e4e7ed;
}

.cursor-pointer {
    cursor: pointer;
}

.d-flex {
    display: flex;
}

.flex-col {
    flex-direction: column;
}

.justify-center {
    justify-content: center;
}

.w-full {
    width: 100%;
}

.private-message-badge {
    margin-left: 5px;
    line-height: 1.2;
}

.message-detail-header {
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 10px;
  margin-bottom: 15px;
}

.message-detail-header h3 {
    margin: 0;
    font-size: 18px;
}

.message-detail-header p {
    margin: 5px 0 0;
    font-size: 14px;
    color: #606266;
}

.message-detail-content {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #303133;
  max-height: 40vh;
  overflow-y: auto;
  padding: 10px 0;
}

.message-detail-footer {
  margin-top: 15px;
  font-size: 12px;
  color: #909399;
  text-align: right;
}
</style> 