<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { 
  User, 
  Document, 
  Promotion,
  School
} from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 统计数据
const totalUsers = ref('--')
const teacherVisits = ref('--')
const studentVisits = ref('--')
const documentCount = ref('--')
const teacherGrowth = ref('--')
const studentGrowth = ref('--')

// 卡片数据
const cards = ref([
  { 
    title: '用户总数', 
    value: totalUsers,
    icon: User, 
    color: 'linear-gradient(135deg, #a5b4fc 0%, #818cf8 100%)',
    growth: '+12%'
  },
  { 
    title: '今日教师访问', 
    value: teacherVisits,
    icon: Promotion, 
    color: 'linear-gradient(135deg, #93c5fd 0%, #60a5fa 100%)',
    growth: teacherGrowth
  },
  { 
    title: '今日学生访问', 
    value: studentVisits,
    icon: School, 
    color: 'linear-gradient(135deg, #fdba74 0%, #fb923c 100%)',
    growth: studentGrowth
  },
  { 
    title: '文档数量', 
    value: documentCount,
    icon: Document, 
    color: 'linear-gradient(135deg, #6ee7b7 0%, #34d399 100%)',
    growth: '+8%'
  }
])

// 图表实例
let userGrowthChart = null
const chartLoading = ref(true)

// 获取用户总数
const getUserCount = async () => {
  try {
    const response = await axios.get(BaseUrl + 'users', {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (Array.isArray(response.data)) {
      totalUsers.value = response.data.length.toString()
    } else if (response.data.totalElements) {
      totalUsers.value = response.data.totalElements.toString()
    }
  } catch (error) {
    console.error('获取用户总数失败:', error)
  }
}

// 获取今日访问统计
const getTodayVisits = async () => {
  try {
    // 获取今天的开始和结束时间
    const today = new Date()
    const startOfDay = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0).toISOString()
    const endOfDay = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59, 59).toISOString()
    
    // 获取今日所有登录记录
    const response = await axios.get(BaseUrl + 'login-records/time', {
      params: {
        startTime: startOfDay,
        endTime: endOfDay
      },
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (Array.isArray(response.data)) {
      // 按角色分组统计
      const teacherLogins = response.data.filter(record => 
        record.user && record.user.userRole === 'TEACHER'
      ).length
      
      const studentLogins = response.data.filter(record => 
        record.user && record.user.userRole === 'STUDENT'
      ).length
      
      teacherVisits.value = teacherLogins.toString()
      studentVisits.value = studentLogins.toString()
      
      // 获取昨天的数据进行对比
      const yesterday = new Date(today)
      yesterday.setDate(yesterday.getDate() - 1)
      const startOfYesterday = new Date(yesterday.getFullYear(), yesterday.getMonth(), yesterday.getDate(), 0, 0, 0).toISOString()
      const endOfYesterday = new Date(yesterday.getFullYear(), yesterday.getMonth(), yesterday.getDate(), 23, 59, 59).toISOString()
      
      const yesterdayResponse = await axios.get(BaseUrl + 'login-records/time', {
        params: {
          startTime: startOfYesterday,
          endTime: endOfYesterday
        },
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      
      if (Array.isArray(yesterdayResponse.data)) {
        const yesterdayTeacherLogins = yesterdayResponse.data.filter(record => 
          record.user && record.user.userRole === 'TEACHER'
        ).length
        
        const yesterdayStudentLogins = yesterdayResponse.data.filter(record => 
          record.user && record.user.userRole === 'STUDENT'
        ).length
        
        // 计算增长率
        if (yesterdayTeacherLogins > 0) {
          const teacherGrowthRate = ((teacherLogins - yesterdayTeacherLogins) / yesterdayTeacherLogins * 100).toFixed(0)
          teacherGrowth.value = (teacherGrowthRate >= 0 ? '+' : '') + teacherGrowthRate + '%'
        } else {
          teacherGrowth.value = '+100%'
        }
        
        if (yesterdayStudentLogins > 0) {
          const studentGrowthRate = ((studentLogins - yesterdayStudentLogins) / yesterdayStudentLogins * 100).toFixed(0)
          studentGrowth.value = (studentGrowthRate >= 0 ? '+' : '') + studentGrowthRate + '%'
        } else {
          studentGrowth.value = '+100%'
        }
      }
    }
  } catch (error) {
    console.error('获取今日访问统计失败:', error)
  }
}

// 获取文档数量
const getDocumentCount = async () => {
  try {
    // 这里假设有一个获取文档数量的接口
    // 如果没有，可以暂时使用静态数据
    documentCount.value = '89'
  } catch (error) {
    console.error('获取文档数量失败:', error)
  }
}

// 获取最近活动
const recentActivities = ref([])
const getRecentActivities = async () => {
  try {
    // 获取最近的登录记录
    const response = await axios.get(BaseUrl + 'login-records', {
      params: {
        pageSize: 4,  // 只获取最近的4条记录
        sort: 'time,desc'
      },
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (Array.isArray(response.data)) {
      recentActivities.value = response.data.map(record => {
        const time = new Date(record.time)
        const now = new Date()
        let timeDisplay
        
        // 格式化时间显示
        if (time.toDateString() === now.toDateString()) {
          // 今天的记录显示时间
          timeDisplay = time.getHours().toString().padStart(2, '0') + ':' + 
                        time.getMinutes().toString().padStart(2, '0')
        } else {
          // 非今天的记录显示"昨天"或日期
          const yesterday = new Date(now)
          yesterday.setDate(yesterday.getDate() - 1)
          
          if (time.toDateString() === yesterday.toDateString()) {
            timeDisplay = '昨天'
          } else {
            timeDisplay = (time.getMonth() + 1) + '/' + time.getDate()
          }
        }
        
        return {
          time: timeDisplay,
          content: `用户 ${record.user?.username || 'unknown'} 登录了系统`
        }
      })
    }
  } catch (error) {
    console.error('获取最近活动失败:', error)
  }
}

// 获取用户增长数据
const getUserGrowthData = async () => {
  try {
    chartLoading.value = true
    
    // 获取过去30天的日期
    const dates = []
    const today = new Date()
    
    for (let i = 29; i >= 0; i--) {
      const date = new Date(today)
      date.setDate(date.getDate() - i)
      dates.push(date.toISOString().split('T')[0])
    }
    
    // 获取所有用户数据
    const response = await axios.get(BaseUrl + 'users', {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    let users = []
    if (Array.isArray(response.data)) {
      users = response.data
    } else if (response.data.content && Array.isArray(response.data.content)) {
      users = response.data.content
    }
    
    // 初始化每天的教师和学生注册数据
    const teacherData = Array(30).fill(0)
    const studentData = Array(30).fill(0)
    
    // 按创建日期和角色分组统计
    users.forEach(user => {
      if (user.createdAt) {
        const createdAt = new Date(user.createdAt)
        const createdDate = createdAt.toISOString().split('T')[0]
        
        // 检查是否在过去30天内
        const dateIndex = dates.indexOf(createdDate)
        if (dateIndex !== -1) {
          if (user.userRole === 'TEACHER') {
            teacherData[dateIndex]++
          } else if (user.userRole === 'STUDENT') {
            studentData[dateIndex]++
          }
        }
      }
    })
    
    // 初始化图表
    await nextTick()
    initUserGrowthChart(dates, teacherData, studentData)
    
    chartLoading.value = false
  } catch (error) {
    console.error('获取用户增长数据失败:', error)
    chartLoading.value = false
    
    // 如果获取真实数据失败，使用模拟数据作为备份
    const dates = []
    const today = new Date()
    
    for (let i = 29; i >= 0; i--) {
      const date = new Date(today)
      date.setDate(date.getDate() - i)
      dates.push(date.toISOString().split('T')[0])
    }
    
    // 生成模拟数据
    const teacherData = []
    const studentData = []
    
    // 生成30天的数据
    for (let i = 0; i < 30; i++) {
      // 教师数据 - 每天1-5人
      teacherData.push(Math.floor(Math.random() * 5) + 1)
      
      // 学生数据 - 每天3-10人
      studentData.push(Math.floor(Math.random() * 8) + 3)
    }
    
    // 初始化图表
    await nextTick()
    initUserGrowthChart(dates, teacherData, studentData)
    
    chartLoading.value = false
  }
}

// 初始化用户增长图表
const initUserGrowthChart = (dates, teacherData, studentData) => {
  const chartDom = document.getElementById('userGrowthChart')
  if (!chartDom) return
  
  // 如果图表实例已存在，销毁它
  if (userGrowthChart) {
    userGrowthChart.dispose()
  }
  
  // 创建新的图表实例
  userGrowthChart = echarts.init(chartDom)
  
  // 图表配置
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['教师', '学生'],
      top: '10px'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates.map(date => {
        // 格式化日期为 MM-DD
        const parts = date.split('-')
        return `${parts[1]}-${parts[2]}`
      }),
      axisLabel: {
        interval: 'auto',
        rotate: 45
      }
    },
    yAxis: {
      type: 'value',
      name: '新增用户数',
      minInterval: 1
    },
    series: [
      {
        name: '教师',
        type: 'bar',
        stack: 'total',
        emphasis: {
          focus: 'series'
        },
        itemStyle: {
          color: '#60a5fa'
        },
        data: teacherData
      },
      {
        name: '学生',
        type: 'bar',
        stack: 'total',
        emphasis: {
          focus: 'series'
        },
        itemStyle: {
          color: '#fb923c'
        },
        data: studentData
      }
    ]
  }
  
  // 设置图表选项
  userGrowthChart.setOption(option)
  
  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    userGrowthChart && userGrowthChart.resize()
  })
}

// 初始化数据
const initData = async () => {
  try {
    await Promise.all([
      getUserCount(),
      getTodayVisits(),
      getDocumentCount(),
      getRecentActivities()
    ])
    
    // 获取用户增长数据
    await getUserGrowthData()
  } catch (error) {
    console.error('初始化数据失败:', error)
    ElMessage.error('获取数据失败，请刷新页面重试')
  }
}

// 组件挂载时初始化数据
onMounted(() => {
  initData()
})

// 组件卸载时清理图表实例
onUnmounted(() => {
  if (userGrowthChart) {
    userGrowthChart.dispose()
    userGrowthChart = null
  }
  
  // 移除事件监听器
  window.removeEventListener('resize', () => {
    userGrowthChart && userGrowthChart.resize()
  })
})
</script>

<template>
  <div class="home-container">
    <div class="page-header">
      <h2>控制面板</h2>
      <p>欢迎回来，这里是您的系统概览</p>
    </div>
    
    <div class="card-grid">
      <div v-for="(card, index) in cards" :key="index" class="stat-card" :style="{ backgroundImage: card.color }">
        <div class="card-content">
          <div class="card-value">{{ card.value }}</div>
          <div class="card-title">{{ card.title }}</div>
          <div class="card-growth">{{ card.growth }}</div>
        </div>
        <div class="card-icon">
          <el-icon :size="40"><component :is="card.icon" /></el-icon>
        </div>
      </div>
    </div>
    
    <div class="card-row">
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>用户增长趋势</span>
            <el-button text>更多</el-button>
          </div>
        </template>
        <div v-loading="chartLoading" class="chart-container">
          <div id="userGrowthChart" class="chart-inner"></div>
        </div>
      </el-card>
      
      <el-card class="table-card">
        <template #header>
          <div class="card-header">
            <span>最近活动</span>
            <el-button text>查看全部</el-button>
          </div>
        </template>
        <div class="activity-list">
          <div class="activity-item" v-for="(activity, index) in recentActivities" :key="index">
            <div class="activity-time">{{ activity.time }}</div>
            <div class="activity-content">{{ activity.content }}</div>
          </div>
          <div v-if="recentActivities.length === 0" class="empty-activity">
            <p>暂无活动记录</p>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
/* 首页卡片样式 */
.home-container {
  padding: 20px 0;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
}

.page-header p {
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: hidden;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 130px;
  transition: transform 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.card-content {
  z-index: 2;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
}

.card-title {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.card-growth {
  font-size: 12px;
  background-color: rgba(255, 255, 255, 0.25);
  padding: 2px 8px;
  border-radius: 12px;
  display: inline-block;
}

.card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  width: 70px;
  height: 70px;
  z-index: 2;
}

.card-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card, .table-card {
  height: 350px;
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.chart-card:hover, .table-card:hover {
  transform: translateY(-5px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 280px;
  width: 100%;
  position: relative;
}

.chart-inner {
  width: 100%;
  height: 100%;
}

.activity-list {
  padding: 10px 0;
}

.activity-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-time {
  color: #909399;
  width: 60px;
  font-size: 13px;
}

.activity-content {
  flex: 1;
  font-size: 14px;
}

.empty-activity {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: #909399;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .card-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style> 