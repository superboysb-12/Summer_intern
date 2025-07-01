<script setup>
import { ref, onMounted, reactive } from 'vue'
import { 
  User, 
  Document, 
  Promotion,
  School
} from '@element-plus/icons-vue'
import axios from 'axios'
import * as echarts from 'echarts'

const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 图表实例引用
const userChartRef = ref(null)
let userChart = null

const cards = ref([
  { 
    title: '用户总数', 
    value: '加载中...', 
    icon: User, 
    color: 'linear-gradient(135deg, #a5b4fc 0%, #818cf8 100%)'
  },
  { 
    title: '今日教师访问', 
    value: '加载中...', 
    icon: Promotion, 
    color: 'linear-gradient(135deg, #93c5fd 0%, #60a5fa 100%)'
  },
  { 
    title: '今日学生访问', 
    value: '加载中...', 
    icon: School, 
    color: 'linear-gradient(135deg, #fdba74 0%, #fb923c 100%)'
  },
  { 
    title: '文档数量', 
    value: '89', 
    icon: Document, 
    color: 'linear-gradient(135deg, #6ee7b7 0%, #34d399 100%)'
  }
])

// 用户增长数据
const userGrowthData = reactive({
  dates: [],
  counts: []
})

// 获取用户总数
const getUserCount = async () => {
  try {
    const response = await axios.get(`${BaseUrl}users`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    // 根据API返回的数据结构处理
    let totalUsers = 0
    if (response.data.content && Array.isArray(response.data.content)) {
      totalUsers = response.data.totalElements || response.data.content.length
    } else if (Array.isArray(response.data)) {
      totalUsers = response.data.length
    }
    
    // 更新用户总数卡片
    cards.value[0].value = totalUsers.toLocaleString()
  } catch (error) {
    console.error('获取用户总数失败:', error)
    cards.value[0].value = '获取失败'
  }
}

// 获取今日登录记录
const getLoginRecords = async () => {
  try {
    // 获取今天的开始时间和结束时间
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const tomorrow = new Date(today)
    tomorrow.setDate(tomorrow.getDate() + 1)
    
    // 格式化为ISO字符串
    const startTime = today.toISOString()
    const endTime = tomorrow.toISOString()
    
    // 获取今日登录记录
    const response = await axios.get(`${BaseUrl}login-records/time`, {
      params: {
        startTime: startTime,
        endTime: endTime
      },
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (Array.isArray(response.data)) {
      // 统计教师和学生的登录次数
      let teacherLogins = 0
      let studentLogins = 0
      
      // 遍历登录记录，根据用户角色统计
      for (const record of response.data) {
        if (record.user && record.user.userRole) {
          if (record.user.userRole === 'TEACHER') {
            teacherLogins++
          } else if (record.user.userRole === 'STUDENT') {
            studentLogins++
          }
        }
      }
      
      // 更新教师访问卡片
      cards.value[1].value = teacherLogins.toString()
      
      // 更新学生访问卡片
      cards.value[2].value = studentLogins.toString()
    }
  } catch (error) {
    console.error('获取登录记录失败:', error)
    cards.value[1].value = '获取失败'
    cards.value[2].value = '获取失败'
  }
}

// 获取最近一个月的用户注册数据
const getUserGrowthData = async () => {
  try {
    // 计算一个月前的日期
    const endDate = new Date()
    const startDate = new Date()
    startDate.setMonth(startDate.getMonth() - 1)
    
    // 格式化为ISO字符串
    const startTime = startDate.toISOString()
    const endTime = endDate.toISOString()
    
    // 获取所有用户数据
    const response = await axios.get(`${BaseUrl}users`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    let users = []
    // 处理不同的响应格式
    if (response.data.content && Array.isArray(response.data.content)) {
      users = response.data.content
    } else if (Array.isArray(response.data)) {
      users = response.data
    }
    
    // 过滤出最近一个月注册的用户
    const recentUsers = users.filter(user => {
      if (!user.createdAt) return false
      const createdDate = new Date(user.createdAt)
      return createdDate >= startDate && createdDate <= endDate
    })
    
    // 按日期分组统计
    const dailyCounts = {}
    
    // 初始化日期范围内的每一天
    let currentDate = new Date(startDate)
    while (currentDate <= endDate) {
      const dateStr = currentDate.toISOString().split('T')[0]
      dailyCounts[dateStr] = 0
      currentDate.setDate(currentDate.getDate() + 1)
    }
    
    // 统计每天的注册用户数
    recentUsers.forEach(user => {
      if (user.createdAt) {
        const dateStr = new Date(user.createdAt).toISOString().split('T')[0]
        if (dailyCounts[dateStr] !== undefined) {
          dailyCounts[dateStr]++
        }
      }
    })
    
    // 转换为图表所需的数据格式
    userGrowthData.dates = Object.keys(dailyCounts).map(date => {
      // 转换为更友好的日期格式 (MM-DD)
      const [year, month, day] = date.split('-')
      return `${month}-${day}`
    })
    userGrowthData.counts = Object.values(dailyCounts)
    
    // 渲染图表
    renderUserGrowthChart()
  } catch (error) {
    console.error('获取用户增长数据失败:', error)
  }
}

// 渲染用户增长趋势图表
const renderUserGrowthChart = () => {
  if (!userChartRef.value) return
  
  // 初始化图表
  if (!userChart) {
    userChart = echarts.init(userChartRef.value)
  }
  
  // 图表配置
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: userGrowthData.dates,
      axisLabel: {
        rotate: 45,
        interval: Math.floor(userGrowthData.dates.length / 10)
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '新注册用户',
        type: 'line',
        smooth: true,
        data: userGrowthData.counts,
        itemStyle: {
          color: '#60a5fa'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(96, 165, 250, 0.5)' },
              { offset: 1, color: 'rgba(96, 165, 250, 0.05)' }
            ]
          }
        }
      }
    ]
  }
  
  // 设置图表选项
  userChart.setOption(option)
  
  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    userChart && userChart.resize()
  })
}

// 获取最近的活动记录
const getRecentActivities = async () => {
  try {
    // 获取最近的5条登录记录
    const response = await axios.get(`${BaseUrl}login-records/recent/5`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (Array.isArray(response.data)) {
      recentActivities.value = response.data.map(record => {
        // 格式化时间
        const time = new Date(record.time)
        const now = new Date()
        let timeStr = ''
        
        // 如果是今天的记录，显示时间
        if (time.toDateString() === now.toDateString()) {
          timeStr = time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
        } else {
          // 否则显示"昨天"或日期
          const yesterday = new Date(now)
          yesterday.setDate(yesterday.getDate() - 1)
          if (time.toDateString() === yesterday.toDateString()) {
            timeStr = '昨天'
          } else {
            timeStr = time.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
          }
        }
        
        return {
          time: timeStr,
          content: `用户 ${record.user?.username || '未知'} 登录了系统`
        }
      })
    }
  } catch (error) {
    console.error('获取最近活动失败:', error)
  }
}

// 最近活动列表
const recentActivities = ref([
  { time: '10:30', content: '用户 admin 登录了系统' },
  { time: '09:45', content: '新增文档 "系统使用手册.pdf"' },
  { time: '09:12', content: '用户 manager 更新了系统设置' },
  { time: '昨天', content: '系统完成了数据备份' }
])

// 页面加载时获取数据
onMounted(() => {
  getUserCount()
  getLoginRecords()
  getRecentActivities()
  getUserGrowthData()
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
        <div class="chart-container" ref="userChartRef"></div>
      </el-card>
      
      <el-card class="table-card">
        <template #header>
          <div class="card-header">
            <span>最近活动</span>
            <el-button text>查看全部</el-button>
          </div>
        </template>
        <div class="activity-list">
          <div v-for="(activity, index) in recentActivities" :key="index" class="activity-item">
            <div class="activity-time">{{ activity.time }}</div>
            <div class="activity-content">{{ activity.content }}</div>
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