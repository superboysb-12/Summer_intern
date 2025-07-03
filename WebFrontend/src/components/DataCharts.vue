<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage, ElDatePicker } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const courseId = ref(route.params.courseId)
const classId = ref(route.params.classId)
const dataType = computed(() => courseId.value ? 'course' : 'class')
const itemName = ref('')

const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 加载状态
const chartLoading = ref(true)

// 图表实例
let userActivityChart = null
let scoreComparisonChart = null
let courseProgressChart = null
let weeklyActivityChart = null

// 日期范围
const dateRange = ref([
  new Date(new Date().setMonth(new Date().getMonth() - 1)),
  new Date()
])

// 获取数据项信息
const getItemInfo = async () => {
  try {
    if (dataType.value === 'course') {
      const response = await axios.get(`${BaseUrl}courses/${courseId.value}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      
      if (response.data) {
        itemName.value = response.data.name || '未知课程'
      }
    } else {
      const response = await axios.get(`${BaseUrl}classes/${classId.value}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      
      if (response.data) {
        itemName.value = response.data.className || '未知班级'
      }
    }
  } catch (error) {
    console.error('获取信息失败:', error)
    ElMessage.error(`获取${dataType.value === 'course' ? '课程' : '班级'}信息失败`)
  }
}

// 获取用户活跃度数据
const getUserActivityData = async () => {
  try {
    chartLoading.value = true
    
    // 模拟数据，实际应从API获取
    const days = []
    const activityData = []
    
    // 生成过去30天的日期和数据
    const today = new Date()
    for (let i = 29; i >= 0; i--) {
      const date = new Date(today)
      date.setDate(date.getDate() - i)
      days.push(date.toISOString().split('T')[0])
      
      // 生成随机活跃度数据
      activityData.push(Math.floor(Math.random() * 100) + 20)
    }
    
    // 初始化图表
    await nextTick()
    initUserActivityChart(days, activityData)
    
    chartLoading.value = false
  } catch (error) {
    console.error('获取用户活跃度数据失败:', error)
    chartLoading.value = false
  }
}

// 获取成绩对比数据
const getScoreComparisonData = async () => {
  try {
    // 模拟数据，实际应从API获取
    const courses = ['数据结构', '高等数学', '计算机网络', '操作系统', '软件工程']
    const classAvgScores = [78, 82, 75, 80, 85]
    const schoolAvgScores = [75, 80, 73, 78, 82]
    
    // 初始化图表
    await nextTick()
    initScoreComparisonChart(courses, classAvgScores, schoolAvgScores)
  } catch (error) {
    console.error('获取成绩对比数据失败:', error)
  }
}

// 获取课程进度数据
const getCourseProgressData = async () => {
  try {
    // 模拟数据，实际应从API获取
    const data = [
      { value: 85, name: '已完成' },
      { value: 15, name: '未完成' }
    ]
    
    // 初始化图表
    await nextTick()
    initCourseProgressChart(data)
  } catch (error) {
    console.error('获取课程进度数据失败:', error)
  }
}

// 获取每周活动数据
const getWeeklyActivityData = async () => {
  try {
    // 模拟数据，实际应从API获取
    const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    const loginData = [120, 132, 101, 134, 90, 50, 40]
    const assignmentData = [80, 100, 56, 120, 70, 30, 20]
    const discussionData = [20, 30, 40, 50, 60, 10, 5]
    
    // 初始化图表
    await nextTick()
    initWeeklyActivityChart(weekdays, loginData, assignmentData, discussionData)
  } catch (error) {
    console.error('获取每周活动数据失败:', error)
  }
}

// 初始化用户活跃度图表
const initUserActivityChart = (days, data) => {
  const chartDom = document.getElementById('userActivityChart')
  if (!chartDom) return
  
  // 如果图表实例已存在，销毁它
  if (userActivityChart) {
    userActivityChart.dispose()
  }
  
  // 创建新的图表实例
  userActivityChart = echarts.init(chartDom)
  
  // 图表配置
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
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
      boundaryGap: false,
      data: days.map(date => {
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
      name: '活跃用户数'
    },
    series: [
      {
        name: '活跃用户',
        type: 'line',
        stack: 'Total',
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: 'rgba(128, 108, 245, 0.8)'
              },
              {
                offset: 1,
                color: 'rgba(128, 108, 245, 0.1)'
              }
            ]
          }
        },
        emphasis: {
          focus: 'series'
        },
        itemStyle: {
          color: '#806cf5'
        },
        smooth: true,
        data: data
      }
    ]
  }
  
  // 设置图表选项
  userActivityChart.setOption(option)
  
  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    userActivityChart && userActivityChart.resize()
  })
}

// 初始化成绩对比图表
const initScoreComparisonChart = (courses, classAvgScores, schoolAvgScores) => {
  const chartDom = document.getElementById('scoreComparisonChart')
  if (!chartDom) return
  
  // 如果图表实例已存在，销毁它
  if (scoreComparisonChart) {
    scoreComparisonChart.dispose()
  }
  
  // 创建新的图表实例
  scoreComparisonChart = echarts.init(chartDom)
  
  // 图表配置
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['班级平均分', '学校平均分']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '分数',
      min: 0,
      max: 100
    },
    yAxis: {
      type: 'category',
      data: courses
    },
    series: [
      {
        name: '班级平均分',
        type: 'bar',
        data: classAvgScores.map(score => ({
          value: score,
          itemStyle: {
            color: '#60a5fa'
          }
        }))
      },
      {
        name: '学校平均分',
        type: 'bar',
        data: schoolAvgScores.map(score => ({
          value: score,
          itemStyle: {
            color: '#93c5fd'
          }
        }))
      }
    ]
  }
  
  // 设置图表选项
  scoreComparisonChart.setOption(option)
  
  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    scoreComparisonChart && scoreComparisonChart.resize()
  })
}

// 初始化课程进度图表
const initCourseProgressChart = (data) => {
  const chartDom = document.getElementById('courseProgressChart')
  if (!chartDom) return
  
  // 如果图表实例已存在，销毁它
  if (courseProgressChart) {
    courseProgressChart.dispose()
  }
  
  // 创建新的图表实例
  courseProgressChart = echarts.init(chartDom)
  
  // 图表配置
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      top: '5%',
      left: 'center'
    },
    series: [
      {
        name: '课程进度',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          {
            value: data[0].value,
            name: data[0].name,
            itemStyle: { color: '#22c55e' }
          },
          {
            value: data[1].value,
            name: data[1].name,
            itemStyle: { color: '#e5e7eb' }
          }
        ]
      }
    ]
  }
  
  // 设置图表选项
  courseProgressChart.setOption(option)
  
  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    courseProgressChart && courseProgressChart.resize()
  })
}

// 初始化每周活动图表
const initWeeklyActivityChart = (weekdays, loginData, assignmentData, discussionData) => {
  const chartDom = document.getElementById('weeklyActivityChart')
  if (!chartDom) return
  
  // 如果图表实例已存在，销毁它
  if (weeklyActivityChart) {
    weeklyActivityChart.dispose()
  }
  
  // 创建新的图表实例
  weeklyActivityChart = echarts.init(chartDom)
  
  // 图表配置
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['登录次数', '作业提交', '讨论参与']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: [
      {
        type: 'category',
        data: weekdays
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '活动次数'
      }
    ],
    series: [
      {
        name: '登录次数',
        type: 'bar',
        itemStyle: {
          color: '#f59e0b'
        },
        data: loginData
      },
      {
        name: '作业提交',
        type: 'bar',
        itemStyle: {
          color: '#60a5fa'
        },
        data: assignmentData
      },
      {
        name: '讨论参与',
        type: 'bar',
        itemStyle: {
          color: '#a855f7'
        },
        data: discussionData
      }
    ]
  }
  
  // 设置图表选项
  weeklyActivityChart.setOption(option)
  
  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    weeklyActivityChart && weeklyActivityChart.resize()
  })
}

// 日期范围变化处理
const handleDateRangeChange = () => {
  // 重新加载数据
  getUserActivityData()
}

// 返回管理页面
const goBack = () => {
  router.push('/manage')
}

// 初始化数据
const initData = async () => {
  try {
    await getItemInfo()
    await Promise.all([
      getUserActivityData(),
      getScoreComparisonData(),
      getCourseProgressData(),
      getWeeklyActivityData()
    ])
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
  if (userActivityChart) {
    userActivityChart.dispose()
    userActivityChart = null
  }
  
  if (scoreComparisonChart) {
    scoreComparisonChart.dispose()
    scoreComparisonChart = null
  }
  
  if (courseProgressChart) {
    courseProgressChart.dispose()
    courseProgressChart = null
  }
  
  if (weeklyActivityChart) {
    weeklyActivityChart.dispose()
    weeklyActivityChart = null
  }
  
  // 移除事件监听器
  window.removeEventListener('resize', () => {
    userActivityChart && userActivityChart.resize()
    scoreComparisonChart && scoreComparisonChart.resize()
    courseProgressChart && courseProgressChart.resize()
    weeklyActivityChart && weeklyActivityChart.resize()
  })
})
</script>

<template>
  <div class="data-charts-container">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/manage' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/manage' }">{{ dataType === 'course' ? '课程管理' : '班级管理' }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ itemName }} - 数据分析</el-breadcrumb-item>
      </el-breadcrumb>
      <el-button @click="goBack" type="primary" plain :icon="ArrowLeft" size="small">返回{{ dataType === 'course' ? '课程' : '班级' }}列表</el-button>
    </div>
    
    <div class="page-header">
      <h2>{{ itemName }} - 图表分析</h2>
      <p>详细的{{ dataType === 'course' ? '课程' : '班级' }}数据图表分析</p>
    </div>
    
    <div class="filter-bar">
      <div class="date-range-picker">
        <span class="filter-label">日期范围：</span>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          @change="handleDateRangeChange"
        />
      </div>
    </div>
    
    <div class="chart-grid">
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>用户活跃度趋势</span>
            <el-button text>导出</el-button>
          </div>
        </template>
        <div v-loading="chartLoading" class="chart-container">
          <div id="userActivityChart" class="chart-inner"></div>
        </div>
      </el-card>
      
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>成绩对比分析</span>
            <el-button text>导出</el-button>
          </div>
        </template>
        <div v-loading="chartLoading" class="chart-container">
          <div id="scoreComparisonChart" class="chart-inner"></div>
        </div>
      </el-card>
      
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>{{ dataType === 'course' ? '课程' : '学习' }}完成进度</span>
            <el-button text>导出</el-button>
          </div>
        </template>
        <div v-loading="chartLoading" class="chart-container">
          <div id="courseProgressChart" class="chart-inner"></div>
        </div>
      </el-card>
      
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>每周活动分析</span>
            <el-button text>导出</el-button>
          </div>
        </template>
        <div v-loading="chartLoading" class="chart-container">
          <div id="weeklyActivityChart" class="chart-inner"></div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
/* 图表分析页面样式 */
.data-charts-container {
  padding: 20px;
}

.breadcrumb-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background-color: white;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
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

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background-color: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.filter-label {
  color: #4b5563;
  margin-right: 8px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.chart-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
  height: 400px;
}

.chart-card:hover {
  transform: translateY(-5px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 330px;
  width: 100%;
  position: relative;
}

.chart-inner {
  width: 100%;
  height: 100%;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style> 