<script setup>
import { ref, onMounted, computed } from 'vue'
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
      { key: 'enrollment-stats', title: '选课统计', path: '/manage/enrollment-stats', roles: [ROLES.ADMIN, ROLES.TEACHER] }
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
  { key: 'rag', title: 'RAG知识库', icon: Collection, path: '/manage/rag', roles: [ROLES.ADMIN] },
  { key: 'teaching-plan', title: '教案生成', icon: Edit, path: '/manage/teaching-plan', roles: [ROLES.ADMIN, ROLES.TEACHER] },
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
        <NotificationPanel />
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
            <span class="role-tag">{{ 
              userInfo.userRole === 'ADMIN' ? '管理员' : 
              userInfo.userRole === 'TEACHER' ? '教师' : 
              '学生'
            }}</span>
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
          <!-- 动态生成菜单项 -->
          <template v-for="item in filteredMenuItems" :key="item.key">
            <!-- 带子菜单的菜单项 -->
            <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.key">
              <template #title>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item 
                v-for="child in item.children.filter(child => child.roles.includes(userRole))" 
                :key="child.key" 
                :index="child.key"
              >
                {{ child.title }}
              </el-menu-item>
            </el-sub-menu>
            
            <!-- 普通菜单项 -->
            <el-menu-item v-else :index="item.key">
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </template>
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
}

.logo {
  margin: 0;
  font-size: 18px;
  color: white;
}

.header-right {
  display: flex;
  align-items: center;
}

.notification-badge {
  margin-right: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  margin: 0 8px;
  color: white;
}

.role-tag {
  background-color: #60a5fa;
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}

.main-container {
  display: flex;
  height: calc(100vh - 60px);
  overflow: hidden;
}

.aside {
  background-color: #1e293b;
  transition: width 0.3s;
  height: 100%;
  overflow: auto;
}

.menu {
  border-right: none;
  height: 100%;
}

.menu:not(.el-menu--collapse) {
  width: 220px;
}

.main {
  padding: 20px;
  overflow-y: auto;
  background-color: #f8fafc;
}

:deep(.el-menu-item.is-active) {
  background-color: #334155;
}

:deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) {
  background-color: #334155;
}
</style>
