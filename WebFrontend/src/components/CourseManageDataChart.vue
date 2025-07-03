<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage, ElDatePicker, ElProgress } from 'element-plus'
import { ArrowLeft, Clock, Timer, Calendar } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const courseId = ref(route.params.courseId)

const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// Course information
const courseInfo = ref({
  courseId: '',
  name: '',
  description: '',
  teacherName: '',
  credit: 0
})

// Study duration data
const studyRecords = ref([])
const studyStatistics = ref({
  totalDuration: 0,
  averageDuration: 0,
  recordCount: 0
})

// 日期范围
const dateRange = ref([
  new Date(new Date().setMonth(new Date().getMonth() - 1)),
  new Date()
])

// Loading state
const chartLoading = ref(true)
const statsLoading = ref(true)

// Chart instances
let studyDurationChart = null
let userDistributionChart = null
let weeklyActivityChart = null

// Get course information
const getCourseInfo = async () => {
  try {
    const response = await axios.get(`${BaseUrl}courses/${courseId.value}`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data) {
      courseInfo.value = response.data
    }
  } catch (error) {
    console.error('获取课程信息失败:', error)
    ElMessage.error('获取课程信息失败')
  }
}

// 获取课程学习时长记录
const getStudyDurationRecords = async () => {
  try {
    chartLoading.value = true
    
    const response = await axios.get(`${BaseUrl}study-durations/course/${courseId.value}`, {
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

// 获取课程学习统计信息
const getStudyStatistics = async () => {
  try {
    statsLoading.value = true
    
    const response = await axios.get(`${BaseUrl}study-durations/statistics/course/${courseId.value}`, {
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

// 处理学习记录数据并初始化图表
const processStudyRecordsData = async () => {
  // 处理学习活动数据
  const studyActivityData = processStudyActivityData()
  await nextTick()
  initStudyDurationChart(studyActivityData.dates, studyActivityData.durations)
  
  // 处理用户分布数据
  const userData = processUserDistributionData()
  await nextTick()
  initUserDistributionChart(userData)
  
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

// 处理用户分布数据
const processUserDistributionData = () => {
  const userMap = new Map()
  
  // 按用户名称分组并求和学习时长
  studyRecords.value.forEach(record => {
    if (record.user && record.user.username) {
      const username = record.user.username
      const duration = record.length || 0
      
      if (userMap.has(username)) {
        userMap.set(username, userMap.get(username) + duration)
      } else {
        userMap.set(username, duration)
      }
    }
  })
  
  // 转换为图表所需格式
  return Array.from(userMap.entries())
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value) // 按学习时长降序排序
    .slice(0, 10) // 只取前10名用户
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

// 日期范围变更
const handleDateRangeChange = () => {
  if (studyRecords.value.length > 0) {
    // 重新处理并更新图表
    const studyActivityData = processStudyActivityData()
    initStudyDurationChart(studyActivityData.dates, studyActivityData.durations)
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
        name: '课程学习时长',
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
                color: 'rgba(65, 105, 225, 0.8)'
              },
              {
                offset: 1,
                color: 'rgba(65, 105, 225, 0.1)'
              }
            ]
          }
        },
        emphasis: {
          focus: 'series'
        },
        itemStyle: {
          color: 'royalblue'
        },
        smooth: true,
        data: durations
      }
    ]
  }
  
  // Apply configuration
  studyDurationChart.setOption(option)
}

// Initialize user distribution chart
const initUserDistributionChart = (data) => {
  const chartDom = document.getElementById('userDistributionChart')
  if (!chartDom) return
  
  // If chart instance already exists, dispose it
  if (userDistributionChart) {
    userDistributionChart.dispose()
  }
  
  // Create new chart instance
  userDistributionChart = echarts.init(chartDom)
  
  // 如果没有数据，显示无数据提示
  if (!data.length) {
    userDistributionChart.setOption({
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
        name: '用户学习时长',
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
  userDistributionChart.setOption(option)
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
            color: '#3498db'
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

// 生成课程学习总结
const generateCourseSummary = () => {
  if (!studyStatistics.value || studyStatistics.value.recordCount === 0) {
    return '该课程暂无学习记录。'
  }
  
  // 计算活跃用户数
  const activeUsers = new Set()
  studyRecords.value.forEach(record => {
    if (record.user && record.user.id) {
      activeUsers.add(record.user.id)
    }
  })
  
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
  
  return `该课程共有 ${studyStatistics.value.recordCount} 条学习记录，累计学习时长 ${totalText}，平均每条记录 ${avgText}。共有 ${activeUsers.size} 名用户学习过该课程。`
}

// Go back to previous page
const goBack = () => {
  router.push('/manage/courses')
}

// Handle resize events
const handleResize = () => {
  if (studyDurationChart) studyDurationChart.resize()
  if (userDistributionChart) userDistributionChart.resize()
  if (weeklyActivityChart) weeklyActivityChart.resize()
}

// Lifecycle hooks
onMounted(async () => {
  await getCourseInfo()
  await getStudyStatistics()
  await getStudyDurationRecords()
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  
  // Dispose all charts
  if (studyDurationChart) studyDurationChart.dispose()
  if (userDistributionChart) userDistributionChart.dispose()
  if (weeklyActivityChart) weeklyActivityChart.dispose()
})
</script>

<template>
  <div class="data-charts-container">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/manage' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/manage/courses' }">课程管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ courseInfo.name || '未知课程' }} - 数据分析</el-breadcrumb-item>
      </el-breadcrumb>
      <el-button @click="goBack" type="primary" plain :icon="ArrowLeft" size="small">
        返回课程列表
      </el-button>
    </div>
    
    <div class="page-header">
      <h2>{{ courseInfo.name || '课程' }} - 学习数据分析</h2>
      <p>详细的课程学习数据图表分析</p>
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

    <!-- 课程信息和统计卡片 -->
    <div class="card-row">
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>课程信息</span>
          </div>
        </template>
        <div class="course-info-container">
          <div class="course-avatar">
            <el-avatar :size="80" shape="square">
              {{ courseInfo.name?.[0] || 'C' }}
            </el-avatar>
            <div class="course-credit" v-if="courseInfo.credit">
              <el-tag type="success">{{ courseInfo.credit }} 学分</el-tag>
            </div>
          </div>
          <div class="course-details">
            <div class="info-row">
              <div class="info-item">
                <div class="label">
                  <i class="el-icon-notebook-1"></i>课程名称
                </div>
                <div class="value">{{ courseInfo.name || '无' }}</div>
              </div>
              <div class="info-item">
                <div class="label">
                  <i class="el-icon-user"></i>授课教师
                </div>
                <div class="value">{{ courseInfo.teacherName || '未指定' }}</div>
              </div>
            </div>
            <div class="info-row">
              <div class="info-item course-description">
                <div class="label">
                  <i class="el-icon-document"></i>课程描述
                </div>
                <div class="value description-text">{{ courseInfo.description || '暂无描述' }}</div>
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
              <div class="summary-text">{{ generateCourseSummary() }}</div>
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
            <span>课程学习时长趋势</span>
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
            <span>用户学习分布</span>
            <el-button text>导出</el-button>
          </div>
        </template>
        <div v-loading="chartLoading" class="chart-container">
          <div id="userDistributionChart" class="chart-inner"></div>
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

/* 课程信息卡片样式 */
.course-info-container {
  display: flex;
  height: 100%;
  padding: 10px 0;
}

.course-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 30px;
  min-width: 100px;
}

.course-credit {
  margin-top: 15px;
}

.course-details {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.course-description {
  flex: 1;
}

.description-text {
  line-height: 1.6;
  max-height: 80px;
  overflow-y: auto;
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

.course-description .value {
  white-space: normal;
}

/* 统计卡片样式 */
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

/* 图表样式 */
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
  height: 550px; /* 增加跨两列卡片的高度 */
  grid-column: span 2;
}

.weekly-chart-container {
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

/* 响应式设计 */
@media (max-width: 1200px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
  
  .full-width-card {
    grid-column: span 1;
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
  
  .course-info-container {
    flex-direction: column;
  }
  
  .course-avatar {
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
}
</style> 