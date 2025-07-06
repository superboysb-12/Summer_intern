<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  HomeFilled, 
  Setting, 
  Document, 
  User, 
  DataAnalysis, 
  Promotion,
  Tools,
  Bell,
  School,
  Reading,
  Collection,
  ChatDotRound,
  Edit
} from '@element-plus/icons-vue'
import { useCounterStore } from '../stores/counter'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const isCollapse = ref(false)

// 根据当前路径设置激活的菜单项
const getActiveMenu = () => {
  const path = route.path
  if (path.includes('/manage/users')) return 'users'
  if (path.includes('/manage/classes')) return 'classes'
  if (path.includes('/manage/courses')) return 'courses'
  if (path.includes('/manage/files')) return 'files'
  if (path.includes('/manage/rag')) return 'rag'
  if (path.includes('/manage/deepseek')) return 'deepseek'
  if (path.includes('/manage/teaching-plan')) return 'teaching-plan'
  if (path.includes('/manage/data')) return 'data-overview'
  if (path.includes('/manage/settings')) return 'settings'
  return 'home'
}

const activeMenu = ref(getActiveMenu())

// 初始化数据
const store = useCounterStore()
const userInfo = ref(store.getUserInfo() || {})

// 处理菜单点击
const handleMenuSelect = (key) => {
  activeMenu.value = key
  switch(key) {
    case 'home':
      router.push('/manage')
      break
    case 'users':
      router.push('/manage/users')
      break
    case 'classes':
      router.push('/manage/classes')
      break
    case 'courses':
      router.push('/manage/courses')
      break
    case 'files':
      router.push('/manage/files')
      break
    case 'rag':
      router.push('/manage/rag')
      break
    case 'deepseek':
      router.push('/manage/deepseek')
      break
    case 'teaching-plan':
      router.push('/manage/teaching-plan')
      break
    case 'data-overview':
      router.push('/manage/data')
      break
    case 'settings':
      router.push('/manage/settings')
      break
    default:
      router.push('/manage')
  }
}

// 处理登出
const handleLogout = () => {
  store.logout()
  router.push('/login')
  ElMessage.success('退出登录成功')
}

// 在路由变化时更新激活的菜单项
onMounted(() => {
  activeMenu.value = getActiveMenu()
})
</script>

<template>
  <div class="manage-container">
    <!-- 顶部标题栏 -->
    <el-header class="header">
      <div class="logo-container">
        <el-button 
          class="collapse-btn" 
          :icon="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"
          @click="isCollapse = !isCollapse"
          type="primary"
          text
        />
        <h1 class="logo">管理系统</h1>
      </div>
      <div class="header-right">
        <el-badge :value="3" class="notification-badge">
          <el-button type="primary" :icon="Bell" circle text />
        </el-badge>
        <el-dropdown>
          <div class="user-info">
            <el-avatar 
              size="small" 
              :src="userInfo.avatar" 
              v-if="userInfo.avatar"
            />
            <el-avatar 
              size="small" 
              v-else
            >{{ userInfo.username?.substring(0, 1) }}</el-avatar>
            <span class="username">{{ userInfo.username }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    
    <div class="main-container">
      <!-- 左侧菜单 -->
      <el-aside width="auto" class="aside">
        <el-menu
          :default-active="activeMenu"
          class="menu"
          :collapse="isCollapse"
          @select="handleMenuSelect"
          background-color="#1e293b"
          text-color="rgba(255, 255, 255, 0.85)"
          active-text-color="#60a5fa"
        >
          <el-menu-item index="home">
            <el-icon><HomeFilled /></el-icon>
            <template #title>首页</template>
          </el-menu-item>
          
          <el-sub-menu index="data">
            <template #title>
              <el-icon><DataAnalysis /></el-icon>
              <span>数据分析</span>
            </template>
            <el-menu-item index="data-overview">数据概览</el-menu-item>
          </el-sub-menu>
          
          <el-menu-item index="users">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          
          <el-menu-item index="classes">
            <el-icon><School /></el-icon>
            <template #title>班级管理</template>
          </el-menu-item>
          
          <el-menu-item index="courses">
            <el-icon><Reading /></el-icon>
            <template #title>课程管理</template>
          </el-menu-item>
          
          <el-menu-item index="files">
            <el-icon><Document /></el-icon>
            <template #title>文件管理</template>
          </el-menu-item>
          
          <el-menu-item index="rag">
            <el-icon><Collection /></el-icon>
            <template #title>RAG知识库</template>
          </el-menu-item>
          
          <el-menu-item index="teaching-plan">
            <el-icon><Edit /></el-icon>
            <template #title>教案生成</template>
          </el-menu-item>
          
          <el-menu-item index="deepseek">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>DeepSeek Chat</template>
          </el-menu-item>
          
          <el-menu-item index="tools">
            <el-icon><Tools /></el-icon>
            <template #title>实用工具</template>
          </el-menu-item>
          
          <el-menu-item index="settings">
            <el-icon><Setting /></el-icon>
            <template #title>系统设置</template>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <!-- 右侧内容区 -->
      <el-main class="main">
        <router-view></router-view>
      </el-main>
    </div>
  </div>
</template>

<style scoped>
.manage-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #1e293b;
  border-bottom: 1px solid #293548;
  padding: 0 20px;
  height: 60px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.logo-container {
  display: flex;
  align-items: center;
}

.collapse-btn {
  margin-right: 15px;
  border: none;
  background: transparent;
  color: #ffffff;
}

.logo {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.notification-badge {
  margin-right: 10px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  margin-left: 8px;
  font-size: 14px;
  color: #ffffff;
}

.main-container {
  display: flex;
  height: calc(100vh - 60px);
  overflow: hidden;
}

.aside {
  background-color: #1e293b;
  transition: width 0.3s;
  overflow-x: hidden;
}

.menu {
  height: 100%;
  border-right: none;
}

.main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f8fafc;
  border-left: 1px solid #e2e8f0;
}

.el-menu {
  transition: width 0.3s;
}

:deep(.el-button.is-circle) {
  color: #ffffff;
}

:deep(.el-badge__content) {
  background-color: #60a5fa;
}

:deep(.el-avatar) {
  background-color: #60a5fa;
  color: #ffffff;
}
</style>
