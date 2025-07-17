<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { Plus, Delete, Download, RefreshRight, Connection, ChatLineSquare } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'
import MarkdownIt from 'markdown-it'

// Initialize markdown parser
const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true
})

const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 聊天消息列表
const messages = ref([])

const displayedMessages = computed(() => {
  return messages.value.filter(m => m.role !== 'system')
})

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

// 渲染markdown内容
const renderMarkdown = (content) => {
  return md.render(content);
}

// 学习帮助助手提示词
const learningAssistantPrompt = `# 学习帮助助手提示词

## 角色定义
你是一位经验丰富、耐心友善的学习帮助助手，专门为学生提供学习支持和答疑解惑。你的目标是帮助学生理解知识、培养思维能力，而不仅仅是提供标准答案。

## 核心原则

### 1. 启发式教学
- 优先引导学生思考，而非直接给出答案
- 通过提问帮助学生发现问题的关键点
- 鼓励学生说出自己的思路和理解

### 2. 因材施教
- 根据学生的年级和知识水平调整解释的深度
- 识别学生的薄弱环节，有针对性地提供帮助
- 使用学生熟悉的例子和类比来解释概念

### 3. 循序渐进
- 将复杂问题分解为简单的小步骤
- 确保学生理解每一步再进行下一步
- 提供从基础到进阶的多层次解释

## 回答格式

### 对于学科问题：
1. **理解确认**: 先复述问题，确保理解正确
2. **思路引导**: 提出引导性问题，启发学生思考
3. **概念解释**: 用简单易懂的语言解释相关概念
4. **步骤演示**: 如果需要，提供清晰的解题步骤
5. **举一反三**: 提供相似的例子或变式练习
6. **总结要点**: 归纳重要知识点和解题思路

### 对于学习方法问题：
1. **问题分析**: 了解学生的具体困难
2. **方法推荐**: 提供实用的学习策略和技巧
3. **个性化建议**: 根据学生情况给出针对性建议
4. **实践指导**: 说明如何具体实施这些方法

## 沟通风格

### 语言特点：
- 使用鼓励性和支持性的语言
- 避免使用过于专业或复杂的术语
- 保持友善、耐心的语调
- 适当使用表情符号增加亲和力

### 回复要求：
- 回答要准确、完整但不冗长
- 结构清晰，便于学生理解和记忆
- 多用实例和类比，少用抽象概念
- 及时给予肯定和鼓励

## 特殊情况处理

### 当学生遇到困难时：
- 保持耐心，不表现出任何不耐烦
- 换一种解释方式或角度
- 从更基础的概念开始讲解
- 给予情感支持和鼓励

### 当学生答错时：
- 不直接指出错误，而是引导发现
- 肯定其中正确的部分
- 温和地纠正误解
- 解释正确答案的思路

### 当遇到超出能力范围的问题时：
- 诚实承认不确定
- 建议寻求老师或专业人士帮助
- 提供可能的学习资源和方向

## 示例互动

**学生**: "我不明白二次函数的开口方向怎么判断"

**助手**: "这是一个很好的问题！让我们一起来理解一下。你还记得二次函数的一般形式是什么样的吗？是 y = ax² + bx + c 对吧？

你有没有注意到这里有个字母 a？这个 a 其实就是判断开口方向的关键！

你可以想象一下：
- 当 a > 0 时，就像一个笑脸 😊，开口向上
- 当 a < 0 时，就像一个难过的脸 ☹️，开口向下

你能试着判断一下 y = 2x² + 3x + 1 的开口方向吗？"

## 持续改进
- 关注学生的反馈，调整解释方式
- 不断学习新的教学方法和技巧
- 保持对教育心理学的理解和应用`

// 保存的对话列表
const savedChats = ref([])

// 滚动锁定状态
const lockChatScroll = ref(false)

// 禁止聊天区域滚动函数
const preventChatAreaScroll = (e) => {
  const chatMessages = document.querySelector('.chat-messages')
  if (!chatMessages) return
  
  // 只有当聊天消息区域滚动到顶部或底部时才传播滚动事件
  const isAtTop = chatMessages.scrollTop <= 0
  const isAtBottom = Math.abs(chatMessages.scrollHeight - chatMessages.scrollTop - chatMessages.clientHeight) <= 1
  
  if (!isAtTop && !isAtBottom) {
    e.stopPropagation()
  }
}

// 加载用户的对话列表
const loadUserConversations = async () => {
  const user = store.getUserInfo()
  if (!user?.id) return
  
  try {
    const response = await axios.get(`${BaseUrl}api/deepseek/conversations/user/${user.id}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    if (response.data) {
      // 按更新时间降序排序
      savedChats.value = response.data.sort((a, b) => new Date(b.updatedAt) - new Date(a.createdAt))
    }
  } catch (error) {
    console.error('加载对话列表失败:', error)
    ElMessage.error('加载对话列表失败')
  }
}

// 更新会话标题
const updateChatTitle = async (conversationId, title) => {
  try {
    await axios.put(`${BaseUrl}api/deepseek/conversation/${conversationId}/title`, { title }, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
  } catch (error) {
    console.error('更新标题失败:', error)
    // 不向用户显示此错误，因为它是一个后台操作
  }
}


// 加载指定对话
const loadChat = async (chatId) => {
  try {
    const response = await axios.get(`${BaseUrl}api/deepseek/conversation/${chatId}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    const chat = response.data
    if (chat) {
      chatHistory.id = chat.id
      chatHistory.title = chat.title
      // 后端返回的消息包含更多字段，我们只需要role和content
      messages.value = chat.messages.map(msg => ({
        role: msg.role,
        content: msg.content
      }))
      ElMessage.success('对话已加载')
      await nextTick()
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载对话失败:', error)
    ElMessage.error('加载对话失败')
  }
}

// 删除保存的对话
const deleteChat = async (chatId) => {
  try {
    await axios.delete(`${BaseUrl}api/deepseek/conversation/${chatId}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    savedChats.value = savedChats.value.filter(chat => chat.id !== chatId)
    
    // 如果删除的是当前对话，则清空当前对话
    if (chatHistory.id === chatId) {
      clearChat()
    }
    
    ElMessage.success('对话已删除')
  } catch (error) {
    console.error('删除对话失败:', error)
    ElMessage.error('删除对话失败')
  }
}

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
  const user = store.getUserInfo()
  if (!user?.id) {
    ElMessage.error('请先登录')
    return
  }

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
    // 准备系统消息 (包含RAG上下文)
    let systemPrompt = learningAssistantPrompt
    
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
        
        // 组合RAG上下文和学习助手提示词
        systemPrompt = `${ragContext}\n\n${systemPrompt}`
      }
    }
    
    // 准备要发送到API的消息
    const apiMessages = [
      { role: "system", content: systemPrompt },
      { role: "user", content: tempMessage }
    ]

    let response;

    if (!chatHistory.id) {
      // 1. 创建新会话
      response = await axios.post(`${BaseUrl}api/deepseek/conversation/user/${user.id}`, { messages: apiMessages }, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      });

      if (response.data && response.data.response && response.data.response.choices) {
        const aiResponse = response.data.response.choices[0].message
        
        // 更新临时消息为实际响应
        const aiResponseIndex = messages.value.findIndex(msg => msg.isLoading)
        if (aiResponseIndex !== -1) {
          messages.value[aiResponseIndex] = { role: 'assistant', content: aiResponse.content }
        }

        // 设置当前会话信息
        chatHistory.id = response.data.conversation_id
        const newTitle = tempMessage.substring(0, 30) + (tempMessage.length > 30 ? '...' : '')
        chatHistory.title = newTitle

        // 在本地列表中添加新会话并更新标题
        savedChats.value.unshift({ id: chatHistory.id, title: newTitle, updatedAt: new Date().toISOString() })
        await updateChatTitle(chatHistory.id, newTitle)

      } else {
        throw new Error('获取回复失败')
      }
    } else {
      // 2. 向现有会话发送消息
      response = await axios.post(`${BaseUrl}api/deepseek/conversation/${chatHistory.id}/message`, { messages: apiMessages }, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })

      if (response.data && response.data.choices) {
        const aiResponse = response.data.choices[0].message
        
        // 更新临时消息为实际响应
        const aiResponseIndex = messages.value.findIndex(msg => msg.isLoading)
        if (aiResponseIndex !== -1) {
          messages.value[aiResponseIndex] = { role: 'assistant', content: aiResponse.content }
        }

        // 更新会话列表中的时间戳，以便排序
        const chatInList = savedChats.value.find(c => c.id === chatHistory.id)
        if (chatInList) {
          chatInList.updatedAt = new Date().toISOString()
        }
        savedChats.value.sort((a, b) => new Date(b.updatedAt) - new Date(a.createdAt))

      } else {
        throw new Error('获取回复失败')
      }
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    // 移除临时消息
    messages.value = messages.value.filter(msg => !msg.isLoading)
    ElMessage.error(error.message || '发送消息失败')
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

// 页面加载时获取RAG列表并加载保存的对话
onMounted(() => {
  getRAGList()
  loadUserConversations()
  
  // 添加聊天区域的滚动事件监听
  const chatMessages = document.querySelector('.chat-messages')
  if (chatMessages) {
    chatMessages.addEventListener('wheel', preventChatAreaScroll, { passive: false })
    chatMessages.addEventListener('touchmove', preventChatAreaScroll, { passive: false })
  }
})

// 组件卸载时清理事件监听器
onUnmounted(() => {
  const chatMessages = document.querySelector('.chat-messages')
  if (chatMessages) {
    chatMessages.removeEventListener('wheel', preventChatAreaScroll)
    chatMessages.removeEventListener('touchmove', preventChatAreaScroll)
  }
})

// 不再需要监听消息来保存
</script>

<template>
  <div class="deepseek-container d-flex h-full w-full">
    <!-- 左侧边栏 - 聊天历史 -->
    <div class="sidebar bg-white border-right">
      <div class="sidebar-header d-flex justify-between align-center p-md border-bottom">
        <h2 class="text-md text-bold m-0">对话历史</h2>
        <el-button type="primary" size="small" @click="clearChat" :icon="Plus">新对话</el-button>
      </div>
      
      <div class="sidebar-content p-sm overflow-auto">
        <div v-for="chat in savedChats" :key="chat.id" 
             class="sidebar-item d-flex justify-between align-center rounded-md mb-xs p-sm cursor-pointer" 
             :class="{ 'bg-primary-light border-left-primary': chat.id === chatHistory.id }" 
             @click="loadChat(chat.id)">
          <div class="d-flex align-center gap-xs overflow-hidden">
            <el-icon class="text-primary"><ChatLineSquare /></el-icon>
            <span class="text-ellipsis">{{ chat.title }}</span>
          </div>
          <div class="sidebar-item-actions">
            <el-button type="text" size="small" @click.stop="deleteChat(chat.id)" :icon="Delete"></el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 主聊天区域 -->
    <div class="main-content d-flex flex-col h-full">
      <!-- 聊天头部 -->
      <div class="main-header sticky-top d-flex justify-between align-center p-md border-bottom bg-white">
        <h1 class="text-lg text-bold m-0">{{ chatHistory.title }}</h1>
        <div class="d-flex align-center gap-md">
          <el-switch
            v-model="useRag"
            active-text="启用知识库"
            inactive-text="不使用知识库"
          />
          
          <el-select 
            v-if="useRag" 
            v-model="selectedRag" 
            placeholder="选择知识库" 
            size="small"
            class="w-180px">
            <el-option
              v-for="item in ragList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          
          <el-button type="text" @click="getRAGList" :icon="RefreshRight"></el-button>
          <el-button type="text" @click="downloadChat" :icon="Download"></el-button>
          <el-button type="text" @click="clearChat" :icon="Delete"></el-button>
        </div>
      </div>
      
      <!-- 聊天消息区域 -->
      <div class="chat-messages flex-1 p-lg overflow-auto bg-secondary">
        <div v-if="displayedMessages.length === 0" class="d-flex justify-center align-center h-full">
          <div class="empty-state text-center p-xl bg-white rounded-lg shadow-sm">
            <el-icon :size="64" class="text-primary mb-sm"><ChatLineSquare /></el-icon>
            <h2 class="text-lg text-bold mb-xs">开始一个新的对话</h2>
            <p class="text-tertiary">使用DeepSeek AI作为你的学习助手</p>
          </div>
        </div>
        
        <template v-else>
          <div v-for="(message, index) in displayedMessages" 
               :key="index" 
               class="message-wrapper mb-lg" 
               :class="{ 'justify-end': message.role === 'user' }">
            <div class="message-container" :class="{ 'user': message.role === 'user', 'assistant': message.role === 'assistant' }">
              <div class="message-header d-flex align-center mb-xs px-sm">
                <span class="message-role">{{ message.role === 'user' ? '我' : 'DeepSeek AI' }}</span>
            </div>
              <div class="message-content p-lg rounded-lg bg-white" :class="{ 'bg-primary-light': message.role === 'user', 'loading': message.isLoading }">
                <div v-if="message.isLoading" class="loading-indicator d-flex justify-center p-sm">
                <div class="dot"></div>
                <div class="dot"></div>
                <div class="dot"></div>
              </div>
              <div v-else v-html="renderMarkdown(message.content)"></div>
              </div>
            </div>
          </div>
        </template>
      </div>
      
      <!-- 输入区域 -->
      <div class="chat-input d-flex gap-md p-md border-top bg-white sticky-bottom">
        <el-input
          v-model="currentMessage"
          type="textarea"
          :rows="3"
          placeholder="输入您的问题..."
          resize="none"
          class="flex-1"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <el-button 
          type="primary" 
          @click="sendMessage" 
          :loading="loading"
          :disabled="!currentMessage.trim()"
          class="h-auto">
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.deepseek-container {
  display: flex;
  height: 100%;
  width: 100%;
  color: #1c2024;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  background-color: #f8fafc;
}

.sidebar {
  width: 280px;
  height: 100%;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
  z-index: 2;
  background-color: #f0f2f5;
  border-right: 1px solid #dfe3e8;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #dfe3e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
}

.sidebar-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1c2024;
  letter-spacing: -0.02em;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  -webkit-overflow-scrolling: touch;
}

.sidebar-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  background-color: rgba(255, 255, 255, 0.7);
}

.sidebar-item:hover {
  background-color: rgba(255, 255, 255, 1);
  transform: translateY(-1px);
}

.sidebar-item.active {
  background-color: #ecf4fe;
  border-left: 3px solid #3e7bfa;
}

.sidebar-item-content {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
}

.sidebar-item-content .el-icon {
  color: #3e7bfa;
  font-size: 18px;
}

.sidebar-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  font-weight: 500;
}

.sidebar-item-actions {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.sidebar-item:hover .sidebar-item-actions {
  opacity: 1;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  background-color: #ffffff;
  border-left: 1px solid #eaeef2;
}

.main-header {
  padding: 16px 24px;
  border-bottom: 1px solid #eaeef2;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  position: sticky;
  top: 0;
  z-index: 1;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}

.main-header h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: #1c2024;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 18px;
}

.rag-switch {
  margin-right: 10px;
}

.rag-select {
  width: 180px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 30px;
  background-color: #f1f5f9;
  background-image: radial-gradient(circle at 25px 25px, rgba(0, 0, 0, 0.02) 2%, transparent 0%), 
                    radial-gradient(circle at 75px 75px, rgba(0, 0, 0, 0.02) 2%, transparent 0%);
  background-size: 100px 100px;
  -webkit-overflow-scrolling: touch;
}

.empty-chat {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-chat-content {
  text-align: center;
  color: #8c8c8c;
  background-color: white;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  max-width: 400px;
  transition: transform 0.3s ease;
}

.empty-chat-content:hover {
  transform: translateY(-5px);
}

.empty-chat-content .el-icon {
  color: #3e7bfa;
  margin-bottom: 8px;
}

.empty-chat-content h2 {
  margin-top: 16px;
  margin-bottom: 10px;
  font-weight: 600;
  color: #1c2024;
  font-size: 20px;
}

.empty-chat-content p {
  color: #6b7280;
  font-size: 15px;
  margin-top: 0;
}

.message-wrapper {
  display: flex;
  max-width: 85%;
  animation: message-appear 0.3s ease-out forwards;
}

@keyframes message-appear {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-container {
  max-width: 100%;
}

.message-role {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.message-role::before {
  content: "";
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.user .message-role::before {
  background-color: var(--primary);
}

.assistant .message-role::before {
  background-color: var(--success);
}

.message-content {
  box-shadow: var(--shadow-sm);
  font-size: var(--text-sm);
  line-height: 1.6;
  transition: transform 0.2s ease;
  border-radius: 18px;
  padding: 16px 20px !important;
}

.message-content:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.user .message-content {
  border-top-right-radius: 4px !important;
  background-color: #5b9dfa !important;
  color: white;
  box-shadow: 0 2px 8px rgba(91, 157, 250, 0.25);
}

.assistant .message-content {
  border-top-left-radius: 4px !important;
  background-color: #ffffff !important;
  border: 1px solid #eaeef2;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.message-content.loading {
  background-color: #f4f5f6;
}

/* Markdown styling for messages */
.message-content :deep(p) {
  margin-top: 0.5em;
  margin-bottom: 0.5em;
}

.message-content :deep(pre) {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 10px;
  border-radius: 8px;
  overflow-x: auto;
}

.assistant .message-content :deep(pre) {
  background-color: rgba(0, 0, 0, 0.05);
}

.user .message-content :deep(pre) {
  background-color: rgba(255, 255, 255, 0.1);
}

.message-content :deep(code) {
  font-family: monospace;
}

.message-content :deep(ul), .message-content :deep(ol) {
  padding-left: 20px;
  margin: 0.5em 0;
}

.message-content :deep(blockquote) {
  border-left: 4px solid #dfe3e8;
  padding-left: 10px;
  margin-left: 0;
  color: #6b7280;
}

.user .message-content :deep(blockquote) {
  border-left-color: rgba(255, 255, 255, 0.3);
  color: rgba(255, 255, 255, 0.9);
}

.loading-indicator {
  height: 24px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--primary);
  margin: 0 2px;
  animation: dot-pulse 1.5s infinite ease-in-out;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dot-pulse {
  0%, 100% {
    transform: scale(0.7);
    opacity: 0.5;
  }
  50% {
    transform: scale(1);
    opacity: 1;
  }
}

.chat-input {
  padding: 20px 24px;
  border-top: 1px solid #eaeef2;
  display: flex;
  gap: 14px;
  align-items: flex-end;
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  position: sticky;
  bottom: 0;
  box-shadow: 0 -4px 10px rgba(0, 0, 0, 0.05);
}

.chat-input .el-input {
  flex: 1;
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: 12px;
  border-color: #dfe3e8;
  padding: 12px 16px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  font-size: 15px;
}

.chat-input :deep(.el-textarea__inner:focus) {
  border-color: #3e7bfa;
  box-shadow: 0 0 0 3px rgba(62, 123, 250, 0.15);
}

.send-button {
  height: 44px;
  min-width: 90px;
  border-radius: 10px;
  font-weight: 500;
  letter-spacing: 0.02em;
  transition: all 0.3s ease;
  background-color: #3e7bfa;
  border-color: #3e7bfa;
}

.send-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(62, 123, 250, 0.25);
  background-color: #4d86fa;
  border-color: #4d86fa;
}

.send-button:active:not(:disabled) {
  transform: translateY(0);
}

/* 滚动条样式 */
.sidebar-content::-webkit-scrollbar,
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.sidebar-content::-webkit-scrollbar-track,
.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-content::-webkit-scrollbar-thumb,
.chat-messages::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 6px;
}

.sidebar-content::-webkit-scrollbar-thumb:hover,
.chat-messages::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
}
</style>
