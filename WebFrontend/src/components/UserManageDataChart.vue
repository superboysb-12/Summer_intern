<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage, ElDatePicker, ElProgress } from 'element-plus'
import { ArrowLeft, Clock, Timer, Calendar } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const userId = ref(route.params.userId)

const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// User information
const userInfo = ref({
  username: '',
  name: '',
  studentNumber: '',
  email: '',
  userRole: ''
})

// Study duration data
const studyRecords = ref([])
const studyStatistics = ref({
  totalDuration: 0,
  averageDuration: 0,
  recordCount: 0
})

// Emotion data
const emotionRecords = ref([])
const emotionLoading = ref(true)

// 日期范围
const dateRange = ref([
  new Date(new Date().setMonth(new Date().getMonth() - 1)),
  new Date()
])

// Loading state
const chartLoading = ref(true)
const statsLoading = ref(true)

// Chart instances
let loginActivityChart = null
let studyDurationChart = null
let courseDistributionChart = null
let weeklyActivityChart = null
let emotionChart = null

// Get user information
const getUserInfo = async () => {
  try {
    const response = await axios.get(`${BaseUrl}users/${userId.value}`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data) {
      userInfo.value = response.data
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  }
}

// 获取用户学习时长记录
const getStudyDurationRecords = async () => {
  try {
    chartLoading.value = true
    
    const response = await axios.get(`${BaseUrl}study-durations/user/${userId.value}`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data) {
      studyRecords.value = response.data
      // 处理数据后初始化图表
      processStudyRecordsData()
    }
    
    chartLoading.value = false
  } catch (error) {
    console.error('获取学习时长记录失败:', error)
    ElMessage.error('获取学习时长记录失败')
    chartLoading.value = false
  }
}

// 获取用户学习统计信息
const getStudyStatistics = async () => {
  try {
    statsLoading.value = true
    
    const response = await axios.get(`${BaseUrl}study-durations/statistics/user/${userId.value}`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data) {
      studyStatistics.value = response.data
    }
    
    statsLoading.value = false
  } catch (error) {
    console.error('获取学习统计信息失败:', error)
    ElMessage.error('获取学习统计信息失败')
    statsLoading.value = false
  }
}

// 获取用户情绪记录
const getStudentEmotions = async () => {
  try {
    emotionLoading.value = true
    
    // 使用API文档中定义的正确端点
    const response = await axios.get(`${BaseUrl}student-emotions/user/${userId.value}`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data) {
      emotionRecords.value = response.data
      // 处理数据后初始化图表
      processEmotionData()
    } else {
      emotionRecords.value = []
    }
    
    emotionLoading.value = false
  } catch (error) {
    console.error('获取情绪记录失败:', error)
    ElMessage.error('获取情绪记录失败')
    emotionLoading.value = false
    emotionRecords.value = []
  }
}

// 处理学习记录数据并初始化图表
const processStudyRecordsData = async () => {
  // 处理学习活动数据
  const studyActivityData = processStudyActivityData()
  await nextTick()
  initStudyDurationChart(studyActivityData.dates, studyActivityData.durations)
  
  // 处理课程分布数据
  const courseData = processCourseDistributionData()
  await nextTick()
  initCourseDistributionChart(courseData)
  
  // 处理每周活动数据
  const weeklyData = processWeeklyStudyData()
  await nextTick()
  initWeeklyActivityChart(weeklyData.weekdays, weeklyData.durations)
}

// 处理学习活动数据
const processStudyActivityData = () => {
  const dateMap = new Map()
  
  // 过滤日期范围内的记录
  const filteredRecords = dateRange.value && dateRange.value.length === 2 
    ? studyRecords.value.filter(record => {
        const recordDate = new Date(record.lessonStartTimeStamp)
        return recordDate >= dateRange.value[0] && recordDate <= dateRange.value[1]
      })
    : studyRecords.value

  // 按日期分组并求和学习时长
  filteredRecords.forEach(record => {
    const date = record.lessonStartTimeStamp.split('T')[0]
    const duration = record.length || 0
    
    if (dateMap.has(date)) {
      dateMap.set(date, dateMap.get(date) + duration)
    } else {
      dateMap.set(date, duration)
    }
  })
  
  // 确保日期是有序的
  const sortedDates = Array.from(dateMap.keys()).sort()
  const durations = sortedDates.map(date => dateMap.get(date))
  
  return { dates: sortedDates, durations }
}

// 处理课程分布数据
const processCourseDistributionData = () => {
  const courseMap = new Map()
  
  // 按课程名称分组并求和学习时长
  studyRecords.value.forEach(record => {
    if (record.course && record.course.name) {
      const courseName = record.course.name
      const duration = record.length || 0
      
      if (courseMap.has(courseName)) {
        courseMap.set(courseName, courseMap.get(courseName) + duration)
      } else {
        courseMap.set(courseName, duration)
      }
    }
  })
  
  // 转换为图表所需格式
  return Array.from(courseMap.entries()).map(([name, value]) => ({ name, value }))
}

// 处理每周活动数据
const processWeeklyStudyData = () => {
  const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const durationsPerDay = [0, 0, 0, 0, 0, 0, 0]
  
  // 根据星期几分组并求和学习时长
  studyRecords.value.forEach(record => {
    if (record.lessonStartTimeStamp) {
      const date = new Date(record.lessonStartTimeStamp)
      // 获取星期几 (0是周日，1是周一，所以需要调整)
      let dayIndex = date.getDay() - 1
      if (dayIndex === -1) dayIndex = 6 // 调整周日
      
      durationsPerDay[dayIndex] += record.length || 0
    }
  })
  
  return { weekdays, durations: durationsPerDay }
}

// 处理情绪数据并初始化图表
const processEmotionData = async () => {
  try {
    const emotionData = processEmotionChartData()
    await nextTick()
    initEmotionChart(emotionData.dates, emotionData.marks)
  } catch (error) {
    console.error('处理情绪数据失败:', error)
    initEmotionChart([], []) // 初始化一个空图表
  }
}

// 处理情绪数据
const processEmotionChartData = () => {
  const dateMap = new Map()
  
  // 确保记录存在
  if (!emotionRecords.value || emotionRecords.value.length === 0) {
    return { dates: [], marks: [] }
  }
  
  // 过滤日期范围内的记录
  const filteredRecords = dateRange.value && dateRange.value.length === 2 
    ? emotionRecords.value.filter(record => {
        if (!record.createdAt) return false
        const recordDate = new Date(record.createdAt)
        return recordDate >= dateRange.value[0] && recordDate <= dateRange.value[1]
      })
    : emotionRecords.value

  // 按日期分组并计算平均情绪评分
  filteredRecords.forEach(record => {
    if (!record.createdAt) return
    
    const date = record.createdAt.split('T')[0]
    const mark = record.mark || 0
    
    if (dateMap.has(date)) {
      const current = dateMap.get(date)
      current.sum += mark
      current.count += 1
      dateMap.set(date, current)
    } else {
      dateMap.set(date, { sum: mark, count: 1 })
    }
  })
  
  // 计算每日平均情绪评分
  const dateEntries = Array.from(dateMap.entries())
  dateEntries.sort((a, b) => new Date(a[0]) - new Date(b[0]))
  
  const dates = dateEntries.map(entry => entry[0])
  const marks = dateEntries.map(entry => Math.round(entry[1].sum / entry[1].count))
  
  return { dates, marks }
}

// 初始化情绪曲线图表
const initEmotionChart = (dates, marks) => {
  const chartDom = document.getElementById('emotionChart')
  if (!chartDom) return
  
  // 如果图表实例已存在，销毁它
  if (emotionChart) {
    emotionChart.dispose()
  }
  
  // 创建新的图表实例
  emotionChart = echarts.init(chartDom)
  
  // 如果没有数据，显示无数据提示
  if (!dates.length) {
    emotionChart.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center'
      }
    })
    return
  }
  
  // 图表配置
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      },
      formatter: function(params) {
        return `${params[0].axisValue}<br/>
                ${params[0].seriesName}: ${params[0].value} 分`
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
      name: '情绪评分(0-100)',
      min: 0,
      max: 100
    },
    visualMap: {
      show: false,
      dimension: 1,
      pieces: [
        { min: 0, max: 30, color: '#F56C6C' }, // 低情绪
        { min: 30, max: 70, color: '#E6A23C' }, // 中等情绪
        { min: 70, max: 100, color: '#67C23A' }  // 高情绪
      ]
    },
    series: [
      {
        name: '情绪评分',
        type: 'line',
        smooth: true,
        lineStyle: {
          width: 3,
        },
        symbol: 'circle',
        symbolSize: 8,
        markArea: {
          itemStyle: {
            opacity: 0.1
          },
          data: [
            [
              { yAxis: 70, itemStyle: { color: '#67C23A' } },
              { yAxis: 100 }
            ],
            [
              { yAxis: 30, itemStyle: { color: '#E6A23C' } },
              { yAxis: 70 }
            ],
            [
              { yAxis: 0, itemStyle: { color: '#F56C6C' } },
              { yAxis: 30 }
            ]
          ]
        },
        data: marks
      }
    ]
  }
  
  // 应用配置
  emotionChart.setOption(option)
}

// 日期范围变更
const handleDateRangeChange = () => {
  if (studyRecords.value.length > 0) {
    // 重新处理并更新图表
    const studyActivityData = processStudyActivityData()
    initStudyDurationChart(studyActivityData.dates, studyActivityData.durations)
  }
  
  if (emotionRecords.value.length > 0) {
    // 重新处理并更新情绪图表
    const emotionData = processEmotionChartData()
    initEmotionChart(emotionData.dates, emotionData.marks)
  }
}

// Initialize study duration chart
const initStudyDurationChart = (dates, durations) => {
  const chartDom = document.getElementById('studyDurationChart')
  if (!chartDom) return
  
  // If chart instance already exists, dispose it
  if (studyDurationChart) {
    studyDurationChart.dispose()
  }
  
  // Create new chart instance
  studyDurationChart = echarts.init(chartDom)
  
  // 如果没有数据，显示无数据提示
  if (!dates.length) {
    studyDurationChart.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center'
      }
    })
    return
  }
  
  // Chart configuration
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      },
      formatter: function(params) {
        return `${params[0].axisValue}<br/>
                ${params[0].seriesName}: ${params[0].value} 分钟`
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
      data: dates.map(date => {
        // Format date to MM-DD
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
      name: '学习时长(分钟)'
    },
    series: [
      {
        name: '学习时长',
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
        data: durations
      }
    ]
  }
  
  // Apply configuration
  studyDurationChart.setOption(option)
}

// Initialize course distribution chart
const initCourseDistributionChart = (data) => {
  const chartDom = document.getElementById('courseDistributionChart')
  if (!chartDom) return
  
  // If chart instance already exists, dispose it
  if (courseDistributionChart) {
    courseDistributionChart.dispose()
  }
  
  // Create new chart instance
  courseDistributionChart = echarts.init(chartDom)
  
  // 如果没有数据，显示无数据提示
  if (!data.length) {
    courseDistributionChart.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center'
      }
    })
    return
  }
  
  // Chart configuration
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} 分钟 ({d}%)'
    },
    legend: {
      orient: 'horizontal',
      bottom: 'bottom',
      type: 'scroll',
      pageIconSize: 10
    },
    series: [
      {
        name: '课程学习时长',
        type: 'pie',
        radius: ['50%', '70%'],
        avoidLabelOverlap: false,
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '18',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: data
      }
    ]
  }
  
  // Apply configuration
  courseDistributionChart.setOption(option)
}

// Initialize weekly activity chart
const initWeeklyActivityChart = (weekdays, durations) => {
  const chartDom = document.getElementById('weeklyActivityChart')
  if (!chartDom) return
  
  // If chart instance already exists, dispose it
  if (weeklyActivityChart) {
    weeklyActivityChart.dispose()
  }
  
  // Create new chart instance
  weeklyActivityChart = echarts.init(chartDom)
  
  // 如果没有数据，显示无数据提示
  if (durations.every(d => d === 0)) {
    weeklyActivityChart.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center'
      }
    })
    return
  }
  
  // Chart configuration
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: function(params) {
        return `${params[0].name}<br/>
                ${params[0].seriesName}: ${params[0].value} 分钟`
      }
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
        name: '学习时长(分钟)'
      }
    ],
    series: [
      {
        name: '学习时长',
        type: 'bar',
        emphasis: {
          focus: 'series'
        },
        data: durations.map(value => ({
          value,
          itemStyle: {
            color: '#67C23A'
          }
        }))
      }
    ]
  }
  
  // Apply configuration
  weeklyActivityChart.setOption(option)
}

// 格式化时长显示为小时和分钟
const formatDuration = (minutes) => {
  if (!minutes) return '0分钟'
  
  const hours = Math.floor(minutes / 60)
  const mins = Math.round(minutes % 60)
  
  if (hours > 0) {
    return `${hours}小时${mins > 0 ? mins + '分钟' : ''}`
  }
  return `${mins}分钟`
}

// 计算最近一次学习的日期
const calculateLastStudyDay = () => {
  if (!studyRecords.value || studyRecords.value.length === 0) {
    return '无记录'
  }
  
  // 找出最新的学习记录
  let latestDate = null
  studyRecords.value.forEach(record => {
    const recordDate = new Date(record.lessonStartTimeStamp)
    if (!latestDate || recordDate > latestDate) {
      latestDate = recordDate
    }
  })
  
  if (!latestDate) return '无记录'
  
  // 计算与当前日期的差异
  const now = new Date()
  const diffTime = Math.abs(now - latestDate)
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) {
    return '今天'
  } else if (diffDays === 1) {
    return '昨天'
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else if (diffDays < 30) {
    return `${Math.floor(diffDays / 7)}周前`
  } else {
    // 格式化日期为 MM-DD 格式
    const month = latestDate.getMonth() + 1
    const day = latestDate.getDate()
    return `${month}-${day}`
  }
}

// 生成用户学习总结
const generateUserSummary = () => {
  if (!studyStatistics.value || studyStatistics.value.recordCount === 0) {
    return '该用户暂无学习记录。'
  }
  
  // 将分钟转换为小时和分钟
  const totalHours = Math.floor(studyStatistics.value.totalDuration / 60)
  const totalMinutes = studyStatistics.value.totalDuration % 60
  
  const avgHours = Math.floor(studyStatistics.value.averageDuration / 60)
  const avgMinutes = Math.round(studyStatistics.value.averageDuration % 60)
  
  const totalText = totalHours > 0 
    ? `${totalHours}小时${totalMinutes > 0 ? totalMinutes + '分钟' : ''}` 
    : `${totalMinutes}分钟`
    
  const avgText = avgHours > 0 
    ? `${avgHours}小时${avgMinutes > 0 ? avgMinutes + '分钟' : ''}` 
    : `${avgMinutes}分钟`
  
  return `该用户共有 ${studyStatistics.value.recordCount} 条学习记录，累计学习时长 ${totalText}，平均每次学习 ${avgText}。`
}

// Go back to previous page
const goBack = () => {
  router.push('/manage/users')
}

// Handle resize events
const handleResize = () => {
  if (studyDurationChart) studyDurationChart.resize()
  if (courseDistributionChart) courseDistributionChart.resize()
  if (weeklyActivityChart) weeklyActivityChart.resize()
  if (emotionChart) emotionChart.resize()
}

// Lifecycle hooks
onMounted(async () => {
  await getUserInfo()
  await getStudyStatistics()
  await getStudyDurationRecords()
  await getStudentEmotions()
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  
  // Dispose all charts
  if (studyDurationChart) studyDurationChart.dispose()
  if (courseDistributionChart) courseDistributionChart.dispose()
  if (weeklyActivityChart) weeklyActivityChart.dispose()
  if (emotionChart) emotionChart.dispose()
})
</script>

<template>
  <div class="data-charts-container">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/manage' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/manage/users' }">用户管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ userInfo.name || userInfo.username || '未知用户' }} - 数据分析</el-breadcrumb-item>
      </el-breadcrumb>
      <el-button @click="goBack" type="primary" plain :icon="ArrowLeft" size="small">
        返回用户列表
      </el-button>
    </div>
    
    <div class="page-header">
      <h2>{{ userInfo.name || userInfo.username || '用户' }} - 学习数据分析</h2>
      <p>详细的用户学习数据图表分析</p>
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

    <!-- 用户信息和统计卡片 -->
    <div class="card-row">
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>用户信息</span>
          </div>
        </template>
        <div class="user-info-container">
          <div class="user-avatar">
            <el-avatar :size="80" :src="userInfo.avatar">
              {{ userInfo.name?.[0] || userInfo.username?.[0] || 'U' }}
            </el-avatar>
            <div class="user-role">
              <el-tag :type="userInfo.userRole === 'ADMIN' ? 'danger' : userInfo.userRole === 'TEACHER' ? 'warning' : 'success'">
                {{ userInfo.userRole === 'ADMIN' ? '管理员' : userInfo.userRole === 'TEACHER' ? '教师' : '学生' }}
              </el-tag>
            </div>
          </div>
          <div class="user-details">
            <div class="info-row">
              <div class="info-item">
                <div class="label">
                  <i class="el-icon-user"></i>用户名
                </div>
                <div class="value">{{ userInfo.username || '无' }}</div>
              </div>
              <div class="info-item">
                <div class="label">
                  <i class="el-icon-s-custom"></i>姓名
                </div>
                <div class="value">{{ userInfo.name || '无' }}</div>
              </div>
            </div>
            <div class="info-row">
              <div class="info-item">
                <div class="label">
                  <i class="el-icon-notebook-1"></i>学号
                </div>
                <div class="value">{{ userInfo.studentNumber || '无' }}</div>
              </div>
              <div class="info-item">
                <div class="label">
                  <i class="el-icon-message"></i>邮箱
                </div>
                <div class="value">{{ userInfo.email || '无' }}</div>
              </div>
            </div>
            <div class="info-row">
              <div class="info-item" v-if="userInfo.phone">
                <div class="label">
                  <i class="el-icon-phone"></i>手机
                </div>
                <div class="value">{{ userInfo.phone }}</div>
              </div>
              <div class="info-item" v-if="userInfo.schoolClass && userInfo.schoolClass.className">
                <div class="label">
                  <i class="el-icon-school"></i>班级
                </div>
                <div class="value">{{ userInfo.schoolClass.className }}</div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
      
      <el-card class="stats-card" v-loading="statsLoading">
        <template #header>
          <div class="card-header">
            <span>学习统计摘要</span>
          </div>
        </template>
        
        <div class="stats-content">
          <div class="stats-overview">
            <div class="stat-visual">
              <el-progress
                type="dashboard"
                :percentage="studyStatistics.recordCount ? 100 : 0"
                :stroke-width="8"
                :width="120"
                status="success"
              >
                <template #default>
                  <div class="progress-content">
                    <div class="progress-value">{{ studyStatistics.recordCount || 0 }}</div>
                    <div class="progress-label">学习记录数</div>
                  </div>
                </template>
              </el-progress>
            </div>
            <div class="stats-summary">
              <div class="summary-title">学习概况</div>
              <div class="summary-text">{{ generateUserSummary() }}</div>
            </div>
          </div>
          
          <div class="stats-details">
            <div class="stats-item">
              <div class="stats-icon">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-value">{{ formatDuration(studyStatistics.totalDuration || 0) }}</div>
                <div class="stats-label">总学习时长</div>
              </div>
            </div>
            
            <div class="stats-item">
              <div class="stats-icon">
                <el-icon><Timer /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-value">{{ formatDuration(studyStatistics.averageDuration || 0) }}</div>
                <div class="stats-label">平均学习时长</div>
              </div>
            </div>
            
            <div class="stats-item" v-if="studyRecords.length > 0">
              <div class="stats-icon">
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-value">{{ calculateLastStudyDay() }}</div>
                <div class="stats-label">最近学习</div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
    
    <div class="chart-grid">
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>学习时长趋势</span>
            <el-button text>导出</el-button>
          </div>
        </template>
        <div v-loading="chartLoading" class="chart-container">
          <div id="studyDurationChart" class="chart-inner"></div>
        </div>
      </el-card>
      
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>课程学习分布</span>
            <el-button text>导出</el-button>
          </div>
        </template>
        <div v-loading="chartLoading" class="chart-container">
          <div id="courseDistributionChart" class="chart-inner"></div>
        </div>
      </el-card>
      
      <el-card class="chart-card full-width-card">
        <template #header>
          <div class="card-header">
            <span>每周学习活动</span>
            <el-button text>导出</el-button>
          </div>
        </template>
        <div v-loading="chartLoading" class="chart-container weekly-chart-container">
          <div id="weeklyActivityChart" class="chart-inner"></div>
        </div>
      </el-card>
      
      <el-card class="chart-card full-width-card">
        <template #header>
          <div class="card-header">
            <span>学习情绪曲线</span>
            <el-button text>导出</el-button>
          </div>
        </template>
        <div v-loading="emotionLoading" class="chart-container emotion-chart-container">
          <div id="emotionChart" class="chart-inner"></div>
        </div>
        <div class="emotion-legend">
          <div class="emotion-legend-item">
            <div class="emotion-legend-color" style="background-color: #67C23A"></div>
            <div class="emotion-legend-text">高情绪 (70-100)</div>
          </div>
          <div class="emotion-legend-item">
            <div class="emotion-legend-color" style="background-color: #E6A23C"></div>
            <div class="emotion-legend-text">中等情绪 (30-70)</div>
          </div>
          <div class="emotion-legend-item">
            <div class="emotion-legend-color" style="background-color: #F56C6C"></div>
            <div class="emotion-legend-text">低情绪 (0-30)</div>
          </div>
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
  color: var(--el-text-color-primary);
}

.page-header p {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
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

.card-row {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}

.info-card,
.stats-card {
  flex: 1;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  min-height: 280px;
}

.stats-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.stats-overview {
  display: flex;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.stat-visual {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 20px;
  min-width: 120px;
}

.progress-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.progress-value {
  font-size: 24px;
  font-weight: 600;
  color: #67C23A;
}

.progress-label {
  font-size: 12px;
  color: #909399;
}

.stats-summary {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.summary-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.summary-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.stats-details {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}

.stats-item {
  flex: 1;
  min-width: 33%;
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  padding: 0 15px;
}

.stats-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background-color: #ecf5ff;
  color: #409EFF;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 12px;
  font-size: 20px;
}

.stats-item:nth-child(2) .stats-icon {
  background-color: #f0f9eb;
  color: #67C23A;
}

.stats-item:nth-child(3) .stats-icon {
  background-color: #fdf6ec;
  color: #E6A23C;
}

.stats-info {
  flex: 1;
}

.stats-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.stats-label {
  font-size: 12px;
  color: #909399;
}

.user-info-container {
  display: flex;
  height: 100%;
  padding: 10px 0;
}

.user-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 30px;
  min-width: 100px;
}

.user-role {
  margin-top: 15px;
}

.user-details {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.info-row {
  display: flex;
  margin-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 15px;
}

.info-row:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.info-item {
  flex: 1;
  min-width: 0;
  padding-right: 15px;
}

.info-item .label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.info-item .label i {
  margin-right: 5px;
}

.info-item .value {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
  height: 500px;
}

.full-width-card {
  grid-column: span 2;
  height: 550px; /* 增加跨两列卡片的高度 */
}

.weekly-chart-container,
.emotion-chart-container {
  height: 480px; /* 增加内部图表容器的高度 */
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
  height: 430px;
  width: 100%;
  position: relative;
}

.chart-inner {
  width: 100%;
  height: 100%;
}

.emotion-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 10px;
}

.emotion-legend-item {
  display: flex;
  align-items: center;
}

.emotion-legend-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  margin-right: 6px;
}

.emotion-legend-text {
  font-size: 12px;
  color: #606266;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
  
  .full-width-card {
    grid-column: 1;
  }
}

@media (max-width: 992px) {
  .card-row {
    flex-direction: column;
  }
  
  .info-card,
  .stats-card {
    width: 100%;
  }
  
  .user-info-container {
    flex-direction: column;
  }
  
  .user-avatar {
    margin-right: 0;
    margin-bottom: 20px;
    width: 100%;
  }
  
  .info-row {
    flex-direction: column;
  }
  
  .info-item {
    margin-bottom: 15px;
  }
  
  .stats-overview {
    flex-direction: column;
    align-items: center;
  }
  
  .stat-visual {
    margin-right: 0;
    margin-bottom: 20px;
  }
  
  .stats-summary {
    text-align: center;
  }
  
  .stats-details {
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .chart-container {
    height: 350px;
  }
  
  .stats-item {
    min-width: 100%;
    justify-content: center;
  }
  
  .emotion-legend {
    flex-direction: column;
    align-items: center;
    gap: 10px;
  }
}

/* 确保面包屑项在悬停时显示指针和颜色变化 */
.clickable-breadcrumb :deep(.el-breadcrumb__inner) {
  cursor: pointer;
}

.clickable-breadcrumb :deep(.el-breadcrumb__inner):hover {
  color: var(--el-color-primary);
}
</style> 