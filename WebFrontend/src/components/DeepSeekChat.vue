<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { Plus, Delete, Download, RefreshRight, Connection, ChatLineSquare } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'

const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 聊天消息列表
const messages = ref([])

// 当前输入的消息
const currentMessage = ref('')

// 是否正在加载响应
const loading = ref(false)

// RAG相关
const ragList = ref([])
const selectedRag = ref(null)
const useRag = ref(false)

// 聊天历史记录
const chatHistory = reactive({
  id: null,
  title: '新的对话',
  messages: []
})

// 获取RAG列表
const getRAGList = async () => {
  try {
    const response = await axios.get(`${BaseUrl}api/rag`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      ragList.value = response.data.data || []
    } else {
      ElMessage.error(response.data?.message || '获取RAG列表失败')
    }
  } catch (error) {
    console.error('获取RAG列表失败:', error)
    ElMessage.error('获取RAG列表失败')
  }
}

// 发送消息
const sendMessage = async () => {
  if (!currentMessage.value.trim()) {
    return
  }

  // 添加用户消息到列表
  const userMessage = {
    role: 'user',
    content: currentMessage.value
  }
  
  messages.value.push(userMessage)
  
  // 清空输入框
  const tempMessage = currentMessage.value
  currentMessage.value = ''
  
  // 添加一个临时的AI消息，显示加载中
  const tempAiMessage = {
    role: 'assistant',
    content: '思考中...',
    isLoading: true
  }
  
  messages.value.push(tempAiMessage)
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()
  
  // 设置加载状态
  loading.value = true
  
  try {
    // 准备请求体
    const requestBody = {
      messages: messages.value.filter(msg => !msg.isLoading).map(msg => ({
        role: msg.role,
        content: msg.content
      }))
    }
    
    // 如果启用了RAG，并且选择了RAG，则添加RAG查询
    if (useRag.value && selectedRag.value) {
      // 先进行RAG查询
      const ragResponse = await axios.get(`${BaseUrl}api/rag/query/${selectedRag.value}`, {
        params: {
          query: tempMessage,
          topK: 5,
          includeGraphContext: true,
          contextDepth: 1
        },
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      
      if (ragResponse.data && ragResponse.data.success && ragResponse.data.data.results) {
        // 将RAG结果添加到系统消息中
        const ragResults = ragResponse.data.data.results
        let ragContext = "以下是与查询相关的知识库信息，请在回答时参考这些信息：\n\n"
        
        ragResults.forEach((result, index) => {
          ragContext += `${index + 1}. ${result.triple}\n`
          
          // 添加相关关系
          if (result.incoming_relations && result.incoming_relations.length > 0) {
            ragContext += "   相关入向关系:\n"
            result.incoming_relations.forEach(rel => {
              ragContext += `   - ${rel.subject} ${rel.relation} ${rel.object}\n`
            })
          }
          
          if (result.outgoing_relations && result.outgoing_relations.length > 0) {
            ragContext += "   相关出向关系:\n"
            result.outgoing_relations.forEach(rel => {
              ragContext += `   - ${rel.subject} ${rel.relation} ${rel.object}\n`
            })
          }
          
          ragContext += "\n"
        })
        
        // 在消息列表的最前面添加系统消息
        requestBody.messages.unshift({
          role: "system",
          content: ragContext
        })
      }
    }
    
    // 发送请求到DeepSeek API
    const response = await axios.post(`${BaseUrl}api/deepseek/chat`, requestBody, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    // 处理响应
    if (response.data && response.data.choices && response.data.choices.length > 0) {
      // 更新临时消息为实际响应
      const aiResponseIndex = messages.value.findIndex(msg => msg.isLoading)
      if (aiResponseIndex !== -1) {
        messages.value[aiResponseIndex] = {
          role: 'assistant',
          content: response.data.choices[0].message.content
        }
      }
      
      // 保存到聊天历史
      chatHistory.messages = [...messages.value]
      
      // 更新聊天标题（如果是第一次回复）
      if (!chatHistory.id && messages.value.length === 2) {
        chatHistory.title = tempMessage.substring(0, 30) + (tempMessage.length > 30 ? '...' : '')
      }
    } else {
      // 移除临时消息
      messages.value = messages.value.filter(msg => !msg.isLoading)
      ElMessage.error('获取回复失败')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    // 移除临时消息
    messages.value = messages.value.filter(msg => !msg.isLoading)
    ElMessage.error('发送消息失败')
  } finally {
    loading.value = false
  }
}

// 清空聊天记录
const clearChat = () => {
  ElMessage.warning('聊天记录已清空')
  messages.value = []
  chatHistory.messages = []
  chatHistory.title = '新的对话'
  chatHistory.id = null
}

// 下载聊天记录
const downloadChat = () => {
  if (messages.value.length === 0) {
    ElMessage.warning('没有聊天记录可下载')
    return
  }
  
  // 格式化聊天记录
  let chatContent = `# ${chatHistory.title}\n\n`
  messages.value.forEach(msg => {
    const role = msg.role === 'user' ? '用户' : 'DeepSeek AI'
    chatContent += `## ${role}:\n${msg.content}\n\n`
  })
  
  // 创建下载链接
  const blob = new Blob([chatContent], { type: 'text/markdown;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.setAttribute('href', url)
  link.setAttribute('download', `${chatHistory.title}.md`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// 滚动到底部
const scrollToBottom = () => {
  const chatContainer = document.querySelector('.chat-messages')
  if (chatContainer) {
    chatContainer.scrollTop = chatContainer.scrollHeight
  }
}

// 页面加载时获取RAG列表
onMounted(() => {
  getRAGList()
})
</script>

<template>
  <div class="deepseek-chat-container">
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <h3>DeepSeek Chat</h3>
        <el-button type="primary" :icon="Plus" circle @click="clearChat" />
      </div>
      
      <div class="rag-controls">
        <div class="rag-toggle">
          <span>启用RAG知识增强</span>
          <el-switch v-model="useRag" />
        </div>
        
        <el-select 
          v-model="selectedRag" 
          placeholder="选择知识库" 
          :disabled="!useRag"
          class="rag-select"
        >
          <el-option
            v-for="rag in ragList"
            :key="rag.id"
            :label="rag.name"
            :value="rag.id"
          >
            <div class="rag-option">
              <span>{{ rag.name }}</span>
              <small>{{ rag.description }}</small>
            </div>
          </el-option>
        </el-select>
      </div>
      
      <div class="chat-actions">
        <el-button :icon="RefreshRight" @click="getRAGList">刷新知识库</el-button>
        <el-button :icon="Download" @click="downloadChat">下载对话</el-button>
        <el-button type="danger" :icon="Delete" @click="clearChat">清空对话</el-button>
      </div>
    </div>
    
    <div class="chat-main">
      <div class="chat-messages" ref="chatContainer">
        <template v-if="messages.length === 0">
          <div class="welcome-message">
            <h2>欢迎使用 DeepSeek Chat</h2>
            <p>这是一个强大的AI聊天助手，可以回答您的问题、提供信息和帮助您解决问题。</p>
            <p v-if="ragList.length > 0">您可以启用RAG知识增强功能，让AI基于您的知识库回答问题。</p>
          </div>
        </template>
        
        <template v-else>
          <div 
            v-for="(message, index) in messages" 
            :key="index"
            :class="['message', message.role === 'user' ? 'user-message' : 'ai-message']"
          >
            <div class="message-header">
              <span class="message-role">{{ message.role === 'user' ? '用户' : 'DeepSeek AI' }}</span>
            </div>
            <div class="message-content" v-if="!message.isLoading">
              <!-- 使用简单的Markdown渲染 -->
              <div v-html="message.content.replace(/\n/g, '<br>')"></div>
            </div>
            <div class="message-content loading" v-else>
              <el-skeleton :rows="3" animated />
            </div>
          </div>
        </template>
      </div>
      
      <div class="chat-input">
        <el-input
          v-model="currentMessage"
          type="textarea"
          :rows="3"
          placeholder="输入消息，按Enter发送..."
          resize="none"
          @keyup.enter.exact="sendMessage"
        />
        <el-button 
          type="primary" 
          :icon="ChatLineSquare" 
          :loading="loading"
          @click="sendMessage"
          :disabled="!currentMessage.trim()"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.deepseek-chat-container {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.chat-sidebar {
  width: 280px;
  background-color: #f5f7fa;
  border-right: 1px solid #e6e9f0;
  display: flex;
  flex-direction: column;
  padding: 15px;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.rag-controls {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.rag-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.rag-select {
  width: 100%;
}

.rag-option {
  display: flex;
  flex-direction: column;
}

.rag-option small {
  color: #909399;
  font-size: 12px;
}

.chat-actions {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #ffffff;
}

.welcome-message {
  text-align: center;
  padding: 40px 20px;
  color: #606266;
}

.welcome-message h2 {
  color: #303133;
  margin-bottom: 20px;
}

.message {
  margin-bottom: 20px;
  padding: 15px;
  border-radius: 8px;
  max-width: 85%;
}

.user-message {
  background-color: #ecf5ff;
  margin-left: auto;
}

.ai-message {
  background-color: #f5f7fa;
  margin-right: auto;
}

.message-header {
  margin-bottom: 8px;
  font-weight: bold;
}

.message-role {
  font-size: 14px;
}

.user-message .message-role {
  color: #409eff;
}

.ai-message .message-role {
  color: #67c23a;
}

.message-content {
  font-size: 15px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-content.loading {
  min-width: 300px;
}

.chat-input {
  padding: 15px;
  background-color: #ffffff;
  border-top: 1px solid #e6e9f0;
  display: flex;
  gap: 10px;
}

.chat-input .el-input {
  flex: 1;
}

.chat-input .el-button {
  align-self: flex-end;
}
</style> 