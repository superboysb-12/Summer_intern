<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCounterStore } from '../stores/counter'
import {
  User,
  School,
  Reading,
  Document,
  Collection,
  ChatDotRound,
  Tickets,
  Edit,
  Operation,
  PieChart
} from '@element-plus/icons-vue'

// 获取用户信息和路由
const store = useCounterStore()
const router = useRouter()
const userInfo = computed(() => store.getUserInfo())

// 根据用户角色显示不同的功能卡片
const cards = computed(() => {
  // 默认为空数组
  let userCards = []

  // 根据角色生成不同的卡片
  switch (userInfo.value.userRole) {
    case 'ADMIN': // 管理员角色
      userCards = [
        { 
          title: '用户管理', 
          description: '添加、编辑和删除系统用户账户', 
          icon: User, 
          color: 'rgb(64, 158, 255)', 
          route: '/manage/users' 
        },
        { 
          title: '班级管理', 
          description: '管理班级信息和学生分配', 
          icon: School, 
          color: 'rgb(103, 194, 58)', 
          route: '/manage/classes' 
        },
        { 
          title: '课程管理', 
          description: '创建和管理课程内容', 
          icon: Reading, 
          color: 'rgb(230, 162, 60)', 
          route: '/manage/courses' 
        },
        { 
          title: '选课统计', 
          description: '查看系统选课情况和数据分析', 
          icon: PieChart, 
          color: 'rgb(144, 147, 153)', 
          route: '/manage/enrollment-stats' 
        },
        { 
          title: '文件管理', 
          description: '上传和管理教学资源文件', 
          icon: Document, 
          color: 'rgb(245, 108, 108)', 
          route: '/manage/files' 
        },
        { 
          title: 'RAG知识库', 
          description: '管理和查询系统知识库', 
          icon: Collection, 
          color: 'rgb(83, 168, 255)', 
          route: '/manage/rag' 
        },
        { 
          title: 'AI助手', 
          description: '使用DeepSeek智能助手', 
          icon: ChatDotRound, 
          color: 'rgb(121, 187, 255)', 
          route: '/manage/deepseek' 
        }
      ]
      break
    case 'TEACHER': // 教师角色
      userCards = [
        { 
          title: '班级管理', 
          description: '管理您的班级学生', 
          icon: School, 
          color: 'rgb(103, 194, 58)', 
          route: '/manage/classes' 
        },
        { 
          title: '课程管理', 
          description: '创建和编辑课程', 
          icon: Reading, 
          color: 'rgb(230, 162, 60)', 
          route: '/manage/courses' 
        },
        { 
          title: '选课统计', 
          description: '查看课程选课情况和统计分析', 
          icon: PieChart, 
          color: 'rgb(144, 147, 153)', 
          route: '/manage/enrollment-stats' 
        },
        { 
          title: '教案生成', 
          description: '使用AI生成教学计划', 
          icon: Edit, 
          color: 'rgb(245, 108, 108)', 
          route: '/manage/teaching-plan' 
        },
        { 
          title: '文件管理', 
          description: '上传和管理教学资源', 
          icon: Document, 
          color: 'rgb(121, 187, 255)', 
          route: '/manage/files' 
        },
        { 
          title: 'AI助手', 
          description: '使用DeepSeek智能助手', 
          icon: ChatDotRound, 
          color: 'rgb(144, 147, 255)', 
          route: '/manage/deepseek' 
        },
        { 
          title: '实用工具', 
          description: '教学辅助工具集合', 
          icon: Operation, 
          color: 'rgb(121, 187, 255)', 
          route: '/manage/tools' 
        }
      ]
      break
    case 'STUDENT': // 学生角色
      userCards = [
        { 
          title: '我的课程', 
          description: '查看已选课程和学习进度', 
          icon: Reading, 
          color: 'rgb(64, 158, 255)', 
          route: '/manage/student-courses' 
        },
        { 
          title: '作业管理', 
          description: '查看和提交课程作业', 
          icon: Document, 
          color: 'rgb(230, 162, 60)', 
          route: '/manage/assignments' 
        },
        { 
          title: '学习记录', 
          description: '查看个人学习数据和成绩', 
          icon: Tickets, 
          color: 'rgb(103, 194, 58)', 
          route: '/manage/study-records' 
        },
        { 
          title: '学习资源', 
          description: '浏览和下载学习资料', 
          icon: Collection, 
          color: 'rgb(144, 147, 153)', 
          route: '/manage/resources' 
        },
        { 
          title: 'AI助手', 
          description: '使用DeepSeek智能助手', 
          icon: ChatDotRound, 
          color: 'rgb(121, 187, 255)', 
          route: '/manage/deepseek' 
        },
        { 
          title: '实用工具', 
          description: '学习辅助工具集合', 
          icon: Operation, 
          color: 'rgb(245, 108, 108)', 
          route: '/manage/tools' 
        }
      ]
      break
    default:
      // 默认卡片，适用于未定义角色或访客
      userCards = [
        { 
          title: '首页', 
          description: '返回系统首页', 
          icon: Document, 
          color: 'rgb(64, 158, 255)', 
          route: '/manage' 
        }
      ]
  }

  return userCards
})

// 卡片点击处理函数
const handleCardClick = (route) => {
  router.push(route)
}

// 组件挂载
onMounted(() => {
  // 可以添加一些初始化逻辑
})
</script>

<template>
  <div class="home-container">
    <div class="welcome-section">
      <h1>欢迎使用学习系统</h1>
      <p>{{ userInfo.username }}，祝您使用愉快</p>
        </div>

    <div class="cards-grid">
      <el-card
        v-for="(card, index) in cards"
        :key="index"
        shadow="hover"
        class="card-item"
        @click="handleCardClick(card.route)"
      >
        <div class="card-content" :style="{ '--card-color': card.color }">
          <div class="card-icon">
            <el-icon :size="36">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div class="card-text">
            <h3>{{ card.title }}</h3>
            <p>{{ card.description }}</p>
          </div>
          </div>
        </el-card>
          </div>
  </div>
</template>

<style scoped>
.home-container {
  padding: 20px;
}

.welcome-section {
  text-align: center;
  margin-bottom: 40px;
}

.welcome-section h1 {
  font-size: 28px;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.welcome-section p {
  font-size: 16px;
  color: var(--text-secondary);
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.card-item {
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  height: 100%;
}

.card-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.card-content {
  display: flex;
  align-items: center;
  color: var(--text-primary);
}

.card-icon {
  background-color: var(--card-color);
  color: white;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.card-text {
  flex-grow: 1;
}

.card-text h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.card-text p {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}

/* 响应式布局 */
@media (max-width: 768px) {
  .cards-grid {
    grid-template-columns: 1fr;
  }
}
</style> 