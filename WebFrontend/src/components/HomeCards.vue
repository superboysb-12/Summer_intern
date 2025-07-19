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
  PieChart,
  Histogram,
  Bell,
  QuestionFilled,
  Tools
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
          title: '通知管理',
          description: '发布和管理系统通知',
          icon: Bell,
          color: 'rgb(230, 162, 60)',
          route: '/manage/messages'
        },
        {
          title: '私信',
          description: '查看和发送私信',
          icon: ChatDotRound,
          color: 'rgb(103, 194, 58)',
          route: '/manage/private-messages'
        },
        { 
          title: 'RAG知识库', 
          description: '管理和查询系统知识库', 
          icon: Collection, 
          color: 'rgb(83, 168, 255)', 
          route: '/manage/rag' 
        },
        {
          title: '题目生成',
          description: '使用AI生成各种类型的题目',
          icon: QuestionFilled,
          color: 'rgb(144, 147, 153)',
          route: '/manage/question-generator'
        },
        { 
          title: '教案生成', 
          description: '使用AI生成教学计划', 
          icon: Edit, 
          color: 'rgb(245, 108, 108)', 
          route: '/manage/teaching-plan' 
        },
        {
          title: 'PPT生成',
          description: '根据教案一键生成PPT',
          icon: Histogram,
          color: 'rgb(255, 159, 64)',
          route: '/manage/ppt-generator'
        },
        { 
          title: 'DeepSeek Chat', 
          description: '使用DeepSeek智能助手', 
          icon: ChatDotRound, 
          color: 'rgb(121, 187, 255)', 
          route: '/manage/deepseek' 
        },
        {
          title: '实用工具',
          description: '教学辅助工具集合',
          icon: Tools,
          color: 'rgb(245, 108, 108)',
          route: '/manage/tools'
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
          title: '题目生成',
          description: '使用AI生成各种类型的题目',
          icon: QuestionFilled,
          color: 'rgb(121, 187, 255)',
          route: '/manage/question-generator'
        },
        { 
          title: '教案生成', 
          description: '使用AI生成教学计划', 
          icon: Edit, 
          color: 'rgb(245, 108, 108)', 
          route: '/manage/teaching-plan' 
        },
        {
          title: 'PPT生成',
          description: '根据教案一键生成PPT',
          icon: Histogram,
          color: 'rgb(255, 159, 64)',
          route: '/manage/ppt-generator'
        },
        { 
          title: '文件管理', 
          description: '上传和管理教学资源', 
          icon: Document, 
          color: 'rgb(121, 187, 255)', 
          route: '/manage/files' 
        },
        {
          title: '私信',
          description: '查看和发送私信',
          icon: ChatDotRound,
          color: 'rgb(103, 194, 58)',
          route: '/manage/private-messages'
        },
        { 
          title: 'DeepSeek Chat', 
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
          title: '文件管理',
          description: '查看和下载课程文件',
          icon: Document,
          color: 'rgb(103, 194, 58)',
          route: '/manage/files'
        },
        {
          title: '题目生成',
          description: '使用AI进行练习和提问',
          icon: QuestionFilled,
          color: 'rgb(245, 108, 108)',
          route: '/manage/question-generator'
        },
        {
          title: '私信',
          description: '与老师和同学进行交流',
          icon: ChatDotRound,
          color: 'rgb(144, 147, 153)',
          route: '/manage/private-messages'
        },
        { 
          title: 'DeepSeek Chat', 
          description: '使用DeepSeek智能助手', 
          icon: ChatDotRound, 
          color: 'rgb(121, 187, 255)', 
          route: '/manage/deepseek' 
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
  <div class="container">
    <div class="text-center mb-lg">
      <h1 class="text-2xl mb-sm">欢迎使用学习系统</h1>
      <p class="text-md text-secondary">{{ userInfo.username }}，祝您使用愉快</p>
    </div>

    <div class="linear-cards">
      <el-card
        v-for="(card, index) in cards"
        :key="index"
        shadow="hover"
        class="card-item"
        @click="handleCardClick(card.route)"
      >
        <div class="d-flex align-center" :style="{ '--card-color': card.color }">
          <div class="card-icon mr-md">
            <el-icon :size="40">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div>
            <h3 class="text-lg mb-xs">{{ card.title }}</h3>
            <p class="text-sm text-secondary m-0">{{ card.description }}</p>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.linear-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-lg);
  max-width: 1200px;
  margin: 0 auto;
}

.card-item {
  cursor: pointer;
  transition: transform 0.2s;
  border-radius: 16px;
  overflow: hidden;
  padding: 8px;
}

.card-item :deep(.el-card__body) {
  padding: 20px;
}

.card-item:hover {
  transform: translateY(-5px);
}

.card-icon {
  background-color: var(--card-color);
  color: white;
  width: 70px;
  height: 70px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 20px;
}

.text-center {
  text-align: center;
}

.mb-lg {
  margin-bottom: 2rem;
}

.mb-sm {
  margin-bottom: 0.5rem;
}

.text-2xl {
  font-size: 1.5rem;
  font-weight: 600;
}

.text-md {
  font-size: 1rem;
}

.text-secondary {
  color: #666;
}

.d-flex {
  display: flex;
}

.align-center {
  align-items: center;
}

.mr-md {
  margin-right: 1rem;
}

.text-lg {
  font-size: 1.2rem;
  font-weight: 500;
}

.mb-xs {
  margin-bottom: 0.25rem;
}

.text-sm {
  font-size: 0.9rem;
}

.m-0 {
  margin: 0;
}

@media (max-width: 768px) {
  .linear-cards {
    grid-template-columns: 1fr;
  }
}
</style> 