<template>
  <div class="container">
    <div class="page-header">
      <h2>私信中心</h2>
      <p>管理您的私人消息</p>
    </div>

    <div class="card bg-white">
      <div class="card-header d-flex justify-between align-center">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="收件箱" name="inbox"></el-tab-pane>
          <el-tab-pane label="发件箱" name="outbox"></el-tab-pane>
        </el-tabs>
        <el-button type="primary" @click="showComposeDialog">写消息</el-button>
      </div>
      <div class="card-body">
        <!-- 收件箱内容 -->
        <div v-if="activeTab === 'inbox'" class="table-container">
          <el-table
            v-loading="loading"
            :data="inboxMessages"
            class="table-responsive"
            empty-text="暂无收到的消息"
          >
            <el-table-column label="状态" width="70">
              <template #default="scope">
                <el-tag 
                  size="small"
                  :type="scope.row.read ? 'info' : 'warning'"
                >
                  {{ scope.row.read ? '已读' : '未读' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sender.username" label="发件人" width="120"/>
            <el-table-column prop="subject" label="主题" min-width="200" show-overflow-tooltip/>
            <el-table-column prop="createdAt" label="时间" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <div class="d-flex gap-sm">
                  <el-button type="primary" link @click="viewMessage(scope.row)">查看</el-button>
                  <el-button type="danger" link @click="deleteMessage(scope.row.id)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <!-- 发件箱内容 -->
        <div v-if="activeTab === 'outbox'" class="table-container">
          <el-table
            v-loading="loading"
            :data="outboxMessages"
            class="table-responsive"
            empty-text="暂无发送的消息"
          >
            <el-table-column label="状态" width="70">
              <template #default="scope">
                <el-tag 
                  size="small"
                  :type="scope.row.read ? 'success' : 'info'"
                >
                  {{ scope.row.read ? '已读' : '未读' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="recipient.username" label="收件人" width="120"/>
            <el-table-column prop="subject" label="主题" min-width="200" show-overflow-tooltip/>
            <el-table-column prop="createdAt" label="时间" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <div class="d-flex gap-sm">
                  <el-button type="primary" link @click="viewMessage(scope.row)">查看</el-button>
                  <el-button type="danger" link @click="deleteMessage(scope.row.id)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 查看消息对话框 -->
    <el-dialog
      title="查看消息"
      v-model="viewDialogVisible"
      width="600px"
      class="custom-dialog"
    >
      <div v-if="selectedMessage" class="message-detail">
        <div class="message-header">
          <h3>{{ selectedMessage.subject }}</h3>
          <p>
            <strong>发送者:</strong> {{ selectedMessage.sender.username }}
          </p>
          <p>
            <strong>接收者:</strong> {{ selectedMessage.recipient.username }}
          </p>
          <p>
            <strong>时间:</strong> {{ formatDate(selectedMessage.createdAt) }}
          </p>
          <p v-if="selectedMessage.read">
            <strong>已读时间:</strong> {{ formatDate(selectedMessage.readAt) }}
          </p>
        </div>
        <div class="message-content" v-html="formatContent(selectedMessage.content)"></div>
        <div class="d-flex justify-end gap-sm mt-lg">
          <el-button @click="viewDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="replyToMessage" v-if="selectedMessage.recipient.id === currentUser.id">回复</el-button>
        </div>
      </div>
    </el-dialog>
    
    <!-- 写消息对话框 -->
    <el-dialog
      :title="isReply ? '回复消息' : '写新消息'"
      v-model="composeDialogVisible"
      width="600px"
      class="custom-dialog"
    >
      <el-form :model="newMessage" label-width="80px" class="form-container">
        <el-form-item label="收件人">
          <el-select v-model="newMessage.recipientId" placeholder="请选择收件人" class="w-full">
            <el-option
              v-for="user in availableRecipients"
              :key="user.id"
              :label="user.username"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="主题">
          <el-input v-model="newMessage.subject" placeholder="请输入主题"/>
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="newMessage.content"
            type="textarea"
            :rows="8"
            placeholder="请输入消息内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="d-flex justify-end gap-sm">
          <el-button @click="composeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="sendMessage">发送</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useCounterStore } from '../stores/counter'
import MessageList from './MessageList.vue'

export default {
  components: {
    MessageList
  },
  setup() {
    const store = useCounterStore()
    const activeTab = ref('inbox')
    const loading = ref(false)
    const inboxMessages = ref([])
    const outboxMessages = ref([])
    const viewDialogVisible = ref(false)
    const composeDialogVisible = ref(false)
    const selectedMessage = ref(null)
    const isReply = ref(false)
    const availableRecipients = ref([])
    
    const currentUser = computed(() => store.getUserInfo())
    
    const newMessage = ref({
      recipientId: '',
      subject: '',
      content: ''
    })
    
    // 加载收件箱消息
    const loadInboxMessages = async () => {
      loading.value = true
      try {
        // 获取认证令牌
        const token = localStorage.getItem('token')
        const headers = token ? { Authorization: `Bearer ${token}` } : {}
        
        const response = await axios.get('http://localhost:8080/private-messages/inbox', { headers })
        inboxMessages.value = Array.isArray(response.data) ? response.data : []
      } catch (error) {
        console.error('Error loading inbox messages:', error)
        ElMessage.error('加载收件箱失败')
        inboxMessages.value = []
      } finally {
        loading.value = false
      }
    }
    
    // 加载发件箱消息
    const loadOutboxMessages = async () => {
      loading.value = true
      try {
        // 获取认证令牌
        const token = localStorage.getItem('token')
        const headers = token ? { Authorization: `Bearer ${token}` } : {}
        
        const response = await axios.get('http://localhost:8080/private-messages/outbox', { headers })
        outboxMessages.value = Array.isArray(response.data) ? response.data : []
      } catch (error) {
        console.error('Error loading outbox messages:', error)
        ElMessage.error('加载发件箱失败')
        outboxMessages.value = []
      } finally {
        loading.value = false
      }
    }
    
    // 加载可选的收件人列表
    const loadAvailableRecipients = async () => {
      try {
        const userRole = currentUser.value?.userRole
        loading.value = true;
        
        // 获取认证令牌
        const token = localStorage.getItem('token')
        const headers = token ? { Authorization: `Bearer ${token}` } : {}
        
        // 模拟一些用户数据，用于开发测试
        const mockUsers = [
          { id: 1, name: '张老师', username: 'teacher1', userRole: 'TEACHER' },
          { id: 2, name: '李老师', username: 'teacher2', userRole: 'TEACHER' },
          { id: 3, name: '王同学', username: 'student1', userRole: 'STUDENT' },
          { id: 4, name: '赵同学', username: 'student2', userRole: 'STUDENT' },
          { id: 5, name: '管理员', username: 'admin', userRole: 'ADMIN' }
        ];
        
        try {
          // 尝试从API获取用户
          const response = await axios.get('http://localhost:8080/users', { headers });
          
          if (Array.isArray(response.data) && response.data.length > 0) {
            // 如果API正常返回用户列表，使用API数据
            const responseData = response.data;
            
            // 过滤掉自己
            availableRecipients.value = responseData.filter(user => 
              user && user.id !== currentUser.value?.id &&
              (userRole === 'ADMIN' || 
               userRole === 'TEACHER' && ['TEACHER', 'STUDENT'].includes(user.userRole) ||
               userRole === 'STUDENT' && user.userRole === 'TEACHER')
            );
          } else {
            console.warn('API返回的用户列表为空，使用模拟数据');
            // 使用模拟数据
            availableRecipients.value = mockUsers.filter(user => 
              user.id !== currentUser.value?.id &&
              (userRole === 'ADMIN' || 
               userRole === 'TEACHER' && ['TEACHER', 'STUDENT'].includes(user.userRole) ||
               userRole === 'STUDENT' && user.userRole === 'TEACHER')
            );
          }
        } catch (apiError) {
          console.warn('用户API不可用，使用模拟数据:', apiError);
          
          // 使用模拟数据
          availableRecipients.value = mockUsers.filter(user => 
            user.id !== currentUser.value?.id &&
            (userRole === 'ADMIN' || 
             userRole === 'TEACHER' && ['TEACHER', 'STUDENT'].includes(user.userRole) ||
             userRole === 'STUDENT' && user.userRole === 'TEACHER')
          );
        }
      } catch (error) {
        console.error('Error loading recipients:', error);
        ElMessage.error('加载收件人列表失败');
        availableRecipients.value = [];
      } finally {
        loading.value = false;
      }
    };
    
    // 查看消息详情
    const viewMessage = async (message) => {
      selectedMessage.value = message
      viewDialogVisible.value = true
      
      // 如果是收到的未读消息，标记为已读
      if (message.recipient.id === currentUser.value.id && !message.read) {
        await markAsRead(message.id)
      }
    }
    
    // 标记消息为已读
    const markAsRead = async (messageId) => {
      try {
        // 获取认证令牌
        const token = localStorage.getItem('token')
        const headers = token ? { Authorization: `Bearer ${token}` } : {}
        
        await axios.put(`http://localhost:8080/private-messages/${messageId}/read`, {}, { headers })
        
        // 更新消息状态
        if (selectedMessage.value && selectedMessage.value.id === messageId) {
          selectedMessage.value.read = true
          selectedMessage.value.readAt = new Date().toISOString()
        }
        
        // 更新收件箱中对应的消息
        const index = inboxMessages.value.findIndex(msg => msg.id === messageId)
        if (index !== -1) {
          inboxMessages.value[index].read = true
          inboxMessages.value[index].readAt = new Date().toISOString()
        }
        
        ElMessage.success('消息已标记为已读')
      } catch (error) {
        console.error('Error marking message as read:', error)
        ElMessage.error('标记消息为已读失败')
      }
    }
    
    // 删除消息
    const deleteMessage = async (messageId) => {
      try {
        // 获取认证令牌
        const token = localStorage.getItem('token')
        const headers = token ? { Authorization: `Bearer ${token}` } : {}
        
        await axios.delete(`http://localhost:8080/private-messages/${messageId}`, { headers })
        
        // 从列表中移除消息
        inboxMessages.value = inboxMessages.value.filter(msg => msg.id !== messageId)
        outboxMessages.value = outboxMessages.value.filter(msg => msg.id !== messageId)
        
        ElMessage.success('消息已删除')
        
        // 如果当前正在查看该消息，关闭对话框
        if (selectedMessage.value && selectedMessage.value.id === messageId) {
          viewDialogVisible.value = false
        }
      } catch (error) {
        console.error('Error deleting message:', error)
        ElMessage.error('删除消息失败')
      }
    }
    
    // 显示写消息对话框
    const showComposeDialog = () => {
      isReply.value = false
      newMessage.value = {
        recipientId: '',
        subject: '',
        content: ''
      }
      composeDialogVisible.value = true
    }
    
    // 回复消息
    const replyToMessage = () => {
      isReply.value = true
      newMessage.value = {
        recipientId: selectedMessage.value.sender.id,
        subject: `回复: ${selectedMessage.value.subject}`,
        content: `\n\n\n----- 原始消息 -----\n${selectedMessage.value.content}`
      }
      viewDialogVisible.value = false
      composeDialogVisible.value = true
    }
    
    // 发送消息
    const sendMessage = async () => {
      try {
        if (!newMessage.value.recipientId) {
          ElMessage.error('请选择收件人')
          return
        }
        
        if (!newMessage.value.subject) {
          ElMessage.error('请输入主题')
          return
        }
        
        if (!newMessage.value.content) {
          ElMessage.error('请输入内容')
          return
        }
        
        // 获取认证令牌
        const token = localStorage.getItem('token')
        const headers = token ? { Authorization: `Bearer ${token}` } : {}
        
        await axios.post('http://localhost:8080/private-messages', newMessage.value, { headers })
        
        ElMessage.success('消息发送成功')
        composeDialogVisible.value = false
        
        // 重新加载发件箱
        loadOutboxMessages()
      } catch (error) {
        console.error('Error sending message:', error)
        ElMessage.error('发送消息失败')
      }
    }
    
    // 格式化日期
    const formatDate = (dateString) => {
      const date = new Date(dateString)
      return date.toLocaleString()
    }
    
    // 格式化消息内容（简单的文本到HTML转换）
    const formatContent = (content) => {
      if (!content) return ''
      return content.replace(/\n/g, '<br>')
    }
    
    onMounted(() => {
      loadInboxMessages()
      loadOutboxMessages()
      loadAvailableRecipients()
    })
    
    return {
      activeTab,
      loading,
      inboxMessages,
      outboxMessages,
      viewDialogVisible,
      composeDialogVisible,
      selectedMessage,
      newMessage,
      isReply,
      currentUser,
      availableRecipients,
      viewMessage,
      markAsRead,
      deleteMessage,
      showComposeDialog,
      replyToMessage,
      sendMessage,
      formatDate,
      formatContent
    }
  }
}
</script>

<style scoped>
.message-detail {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.message-header {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border-color);
}

.message-header h3 {
  margin: 0;
  font-size: var(--text-lg);
  color: var(--text-primary);
}

.message-header p {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.message-content {
  padding: var(--spacing-md) 0;
  min-height: 100px;
  white-space: pre-wrap;
}

.form-container {
  width: 100%;
}

.table-container {
  width: 100%;
  overflow-x: auto;
}
</style> 