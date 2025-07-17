<script setup>
import { ref, onMounted, computed, watch } from 'vue'
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
  Edit,
  PieChart
} from '@element-plus/icons-vue'
import { useCounterStore } from '../stores/counter'
import { useRouter, useRoute } from 'vue-router'
import NotificationPanel from '../components/NotificationPanel.vue'

const router = useRouter()
const route = useRoute()

const isCollapse = ref(false)

// 根据当前路径设置激活的菜单项
const getActiveMenu = () => {
  const path = route.path
  if (path.includes('/manage/users')) return 'users'
  if (path.includes('/manage/classes')) return 'classes'
  if (path.includes('/manage/courses')) return 'courses'
  if (path.includes('/manage/student-courses')) return 'student-courses'
  if (path.includes('/manage/files')) return 'files'
  if (path.includes('/manage/rag')) return 'rag'
  if (path.includes('/manage/deepseek')) return 'deepseek'
  if (path.includes('/manage/teaching-plan')) return 'teaching-plan'
  if (path.includes('/manage/tools')) return 'tools'
  if (path.includes('/manage/data')) return 'data-overview'
  if (path.includes('/manage/settings')) return 'settings'
  if (path.includes('/manage/assignments')) return 'assignments'
  if (path.includes('/manage/study-records')) return 'study-records'
  if (path.includes('/manage/resources')) return 'resources'
  if (path.includes('/manage/enrollment-stats')) return 'enrollment-stats'
  if (path.includes('/manage/messages')) return 'messages'
  if (path.includes('/manage/usage-statistics')) return 'usage-statistics'
  if (path.includes('/manage/private-messages')) return 'private-messages'
  if (path.includes('/manage/question-generator')) return 'question-generator'
  return 'home'
}

const activeMenu = ref(getActiveMenu())

// 初始化数据
const store = useCounterStore()
const userInfo = ref(store.getUserInfo() || {})

// 角色权限常量
const ROLES = {
  ADMIN: 'ADMIN',
  TEACHER: 'TEACHER',
  STUDENT: 'STUDENT'
}

// 获取当前用户角色
const userRole = computed(() => userInfo.value.userRole || ROLES.STUDENT)

// 定义菜单项与权限
const menuItems = [
  { key: 'home', title: '首页', icon: HomeFilled, path: '/manage', roles: [ROLES.ADMIN, ROLES.TEACHER, ROLES.STUDENT] },
  { 
    key: 'data', 
    title: '数据分析', 
    icon: DataAnalysis, 
    roles: [ROLES.ADMIN, ROLES.TEACHER],
    children: [
      { key: 'data-overview', title: '数据概览', path: '/manage/data', roles: [ROLES.ADMIN, ROLES.TEACHER] },
      { key: 'enrollment-stats', title: '选课统计', path: '/manage/enrollment-stats', roles: [ROLES.ADMIN, ROLES.TEACHER] },
      { key: 'usage-statistics', title: '使用统计', path: '/manage/usage-statistics', roles: [ROLES.ADMIN, ROLES.TEACHER] }
    ]
  },
  { key: 'users', title: '用户管理', icon: User, path: '/manage/users', roles: [ROLES.ADMIN] },
  { key: 'classes', title: '班级管理', icon: School, path: '/manage/classes', roles: [ROLES.ADMIN, ROLES.TEACHER] },
  { key: 'courses', title: '课程管理', icon: Reading, path: '/manage/courses', roles: [ROLES.ADMIN, ROLES.TEACHER] },
  { key: 'student-courses', title: '我的课程', icon: Reading, path: '/manage/student-courses', roles: [ROLES.STUDENT] },
  { key: 'assignments', title: '作业管理', icon: Document, path: '/manage/assignments', roles: [ROLES.STUDENT] },
  { key: 'study-records', title: '学习记录', icon: DataAnalysis, path: '/manage/study-records', roles: [ROLES.STUDENT] },
  { key: 'resources', title: '学习资源', icon: Collection, path: '/manage/resources', roles: [ROLES.STUDENT] },
  { key: 'files', title: '文件管理', icon: Document, path: '/manage/files', roles: [ROLES.ADMIN, ROLES.TEACHER, ROLES.STUDENT] },
  { key: 'messages', title: '通知管理', icon: Bell, path: '/manage/messages', roles: [ROLES.ADMIN] },
  { key: 'private-messages', title: '私信', icon: ChatDotRound, path: '/manage/private-messages', roles: [ROLES.ADMIN, ROLES.TEACHER, ROLES.STUDENT] },
  { key: 'rag', title: 'RAG知识库', icon: Collection, path: '/manage/rag', roles: [ROLES.ADMIN] },
  { key: 'teaching-plan', title: '教案生成', icon: Edit, path: '/manage/teaching-plan', roles: [ROLES.ADMIN, ROLES.TEACHER] },
  { key: 'question-generator', title: '题目生成', icon: Document, path: '/manage/question-generator', roles: [ROLES.ADMIN, ROLES.TEACHER] },
  { key: 'deepseek', title: 'DeepSeek Chat', icon: ChatDotRound, path: '/manage/deepseek', roles: [ROLES.ADMIN, ROLES.TEACHER, ROLES.STUDENT] },
  { key: 'tools', title: '实用工具', icon: Tools, path: '/manage/tools', roles: [ROLES.ADMIN, ROLES.TEACHER, ROLES.STUDENT] },
  { key: 'settings', title: '系统设置', icon: Setting, path: '/manage/settings', roles: [ROLES.ADMIN] },
]

// 根据用户角色过滤菜单项
const filteredMenuItems = computed(() => {
  return menuItems.filter(item => item.roles.includes(userRole.value))
})

// 处理菜单点击
const handleMenuSelect = (key) => {
  activeMenu.value = key
  // 查找菜单项并跳转
  const menuItem = menuItems.find(item => item.key === key) || 
                   menuItems.flatMap(item => item.children || []).find(child => child.key === key)
  
  if (menuItem && menuItem.path) {
    router.push(menuItem.path)
  }
}

// 处理登出
const handleLogout = () => {
  store.logout()
  router.push('/login')
  ElMessage.success('退出登录成功')
}

// 动画相关
const menuItemHover = ref(null)
const headerHover = ref(false)

// 在路由变化时更新激活的菜单项
onMounted(() => {
  activeMenu.value = getActiveMenu()
})

// 监听路由变化
watch(() => route.path, () => {
  activeMenu.value = getActiveMenu()
}, { immediate: true })  // 添加 immediate: true，确保首次加载时立即执行
</script>

<template>
  <div class="manage-container">
    <!-- 顶部标题栏 -->
    <el-header class="header" :class="{ 'header-hover': headerHover }" @mouseenter="headerHover = true" @mouseleave="headerHover = false">
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
        <NotificationPanel />
        <el-dropdown>
          <div class="user-info">
            <el-avatar 
              class="avatar"
              size="small" 
              :src="userInfo.avatar" 
              v-if="userInfo.avatar"
            />
            <el-avatar 
              class="avatar"
              size="small" 
              v-else
            >{{ userInfo.username?.substring(0, 1) }}</el-avatar>
            <span class="username">{{ userInfo.username }}</span>
            <span class="role-tag">{{ 
              userInfo.userRole === 'ADMIN' ? '管理员' : 
              userInfo.userRole === 'TEACHER' ? '教师' : 
              '学生'
            }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu class="dropdown-menu">
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
          <!-- 动态生成菜单项 -->
          <template v-for="item in filteredMenuItems" :key="item.key">
            <!-- 带子菜单的菜单项 -->
            <el-sub-menu 
              v-if="item.children && item.children.length > 0" 
              :index="item.key"
              @mouseenter="menuItemHover = item.key"
              @mouseleave="menuItemHover = null"
              :popper-class="'linear-submenu'"
            >
              <template #title>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item 
                v-for="child in item.children.filter(child => child.roles.includes(userRole))" 
                :key="child.key" 
                :index="child.key"
                class="submenu-item"
              >
                {{ child.title }}
              </el-menu-item>
            </el-sub-menu>
            
            <!-- 普通菜单项 -->
            <el-menu-item 
              v-else 
              :index="item.key"
              :class="{'menu-item-hover': menuItemHover === item.key}"
              @mouseenter="menuItemHover = item.key"
              @mouseleave="menuItemHover = null"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>
      
      <!-- 右侧内容区 -->
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </div>
  </div>
</template>

<style scoped>
/* 保留其余样式，:root 变量已移至全局样式 */

/* 动画定义 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(94, 106, 210, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(94, 106, 210, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(94, 106, 210, 0);
  }
}

.manage-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--linear-background);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--linear-header-bg);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding: 0 var(--spacing-lg);
  height: 60px;
  position: sticky;
  top: 0;
  z-index: 100;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  box-shadow: var(--linear-shadow);
}

.header-hover {
  box-shadow: var(--linear-hover-shadow);
}

.logo-container {
  display: flex;
  align-items: center;
}

.collapse-btn {
  margin-right: var(--spacing-md);
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.collapse-btn:hover {
  transform: scale(1.05);
}

.logo {
  margin: 0;
  font-size: var(--text-lg);
  color: var(--text-light);
  font-weight: 500;
  letter-spacing: 0.5px;
  opacity: 0.95;
  transition: opacity 0.3s ease;
}

.logo:hover {
  opacity: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.notification-badge {
  margin-right: var(--spacing-lg);
  transition: transform 0.2s ease;
}

.notification-badge:hover {
  transform: scale(1.05);
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  background: rgba(255, 255, 255, 0.05);
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.1);
}

.avatar {
  border: 2px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.avatar:hover {
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.3);
}

.username {
  margin: 0 var(--spacing-sm);
  color: var(--text-light);
  font-weight: 500;
  transition: color 0.2s ease;
}

.role-tag {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-hover) 100%);
  color: var(--text-light);
  font-size: var(--text-xs);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.role-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.15);
}

.main-container {
  display: flex;
  height: calc(100vh - 60px);
  overflow: hidden;
}

.aside {
  background: var(--linear-sidebar-bg);
  transition: width 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  height: 100%;
  overflow: auto;
  box-shadow: 2px 0 5px rgba(0, 0, 0, 0.05);
  position: relative;
  z-index: 10;
}

.menu {
  border-right: none;
  height: 100%;
}

.menu :deep(.el-menu-item) {
  height: 50px;
  line-height: 50px;
  margin: 4px 0;
  border-radius: 6px;
  margin-right: 8px;
  margin-left: 8px;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.menu :deep(.el-menu-item.is-active) {
  background: var(--linear-active-bg);
  color: #60a5fa !important;
  font-weight: 500;
}

.menu :deep(.el-menu-item):hover {
  background: rgba(255, 255, 255, 0.05);
}

.menu-item-hover {
  transform: translateX(2px);
}

.menu :deep(.el-sub-menu__title) {
  height: 50px;
  line-height: 50px;
  border-radius: 6px;
  margin: 4px 8px;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.05) !important;
}

.menu :deep(.el-sub-menu__icon-arrow) {
  transition: transform 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.menu :deep(.el-sub-menu.is-opened .el-sub-menu__icon-arrow) {
  transform: rotateZ(180deg);
}

.submenu-item {
  animation: fadeIn 0.3s ease;
}

:deep(.linear-submenu) {
  border-radius: var(--radius-md) !important;
  overflow: hidden;
  box-shadow: var(--linear-shadow) !important;
  border: 1px solid rgba(0, 0, 0, 0.05) !important;
  animation: fadeIn 0.25s ease;
}

.dropdown-menu {
  border-radius: var(--radius-md) !important;
  overflow: hidden;
  box-shadow: var(--linear-shadow) !important;
  animation: fadeIn 0.25s ease;
}

.main {
  flex: 1;
  overflow-y: auto;
  background-color: var(--bg-secondary);
  padding: var(--spacing-md);
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

/* 页面过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .header {
    padding: 0 var(--spacing-md);
  }
  
  .logo {
    font-size: var(--text-md);
  }
  
  .username {
    display: none;
  }
  
  .menu :deep(.el-menu-item), 
  .menu :deep(.el-sub-menu__title) {
    margin: 2px 4px;
    height: 44px;
    line-height: 44px;
  }
}
</style>

<style>
:root {
  --linear-background: linear-gradient(135deg, #f8fafc 0%, #eef2ff 100%);
  --linear-card-bg: rgba(255, 255, 255, 0.8);
  --linear-sidebar-bg: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  --linear-active-bg: linear-gradient(90deg, rgba(96, 165, 250, 0.1), rgba(96, 165, 250, 0.05));
  --linear-header-bg: linear-gradient(90deg, #1e293b, #1a2234);
  --linear-shadow: 0 4px 12px rgba(0, 0, 0, 0.03), 0 1px 3px rgba(0, 0, 0, 0.02);
  --linear-hover-shadow: 0 8px 20px rgba(0, 0, 0, 0.06), 0 2px 6px rgba(0, 0, 0, 0.04);
  --linear-button-bg: linear-gradient(135deg, #5E6AD2 0%, #4F5ABF 100%);
}
</style>
