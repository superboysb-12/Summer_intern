<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { ElMessage, ElDatePicker } from 'element-plus'
import { ArrowLeft, Clock, Timer, Calendar } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const classId = ref(route.params.classId)

const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 班级信息
const classInfo = ref({
  id: '',
  className: '',
  students: []
})

// 学习时长数据
const studyRecords = ref([])
const studyStatistics = ref({
  totalDuration: 0,
  averageDuration: 0,
  recordCount: 0,
  studentCount: 0,
  averageDurationPerStudent: 0
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
let courseDistributionChart = null
let studentPerformanceChart = null

// 获取班级信息
const getClassInfo = async () => {
  try {
    const response = await axios.get(`${BaseUrl}classes/${classId.value}`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data) {
      classInfo.value = response.data
    }
  } catch (error) {
    console.error('获取班级信息失败:', error)
    ElMessage.error('获取班级信息失败')
  }
}

// 获取班级学习时长记录
const getStudyDurationRecords = async () => {
  try {
    chartLoading.value = true
    
    const response = await axios.get(`${BaseUrl}study-durations/class/${classId.value}`, {
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

// 获取班级学习统计信息
const getStudyStatistics = async () => {
  try {
    statsLoading.value = true
    
    const response = await axios.get(`${BaseUrl}study-durations/statistics/class/${classId.value}`, {
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
  
  // 处理课程分布数据
  const courseData = processCourseDistributionData()
  await nextTick()
  initCourseDistributionChart(courseData)
  
  // 处理学生表现数据
  const studentData = processStudentPerformanceData()
  await nextTick()
  initStudentPerformanceChart(studentData)
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

// 处理学生表现数据
const processStudentPerformanceData = () => {
  const studentMap = new Map()
  
  // 按学生分组并求和学习时长
  studyRecords.value.forEach(record => {
    if (record.user && record.user.username) {
      const username = record.user.username
      const duration = record.length || 0
      
      if (studentMap.has(username)) {
        studentMap.set(username, studentMap.get(username) + duration)
      } else {
        studentMap.set(username, duration)
      }
    }
  })
  
  // 转换为图表所需格式，并按学习时长降序排序
  return Array.from(studentMap.entries())
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
}

// 日期范围变化处理
const handleDateRangeChange = () => {
  processStudyRecordsData()
}

// 初始化学习时长图表
const initStudyDurationChart = (dates, durations) => {
  const chartDom = document.getElementById('studyDurationChart')
  if (!chartDom) return
  
  // 如果图表实例已存在，销毁它
  if (studyDurationChart) {
    studyDurationChart.dispose()
  }
  
  // 创建新的图表实例
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
  
  // 图表配置
  const option = {
    title: {
      text: '班级学习时长趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      formatter: function(params) {
        const date = params[0].axisValue
        const duration = params[0].data
        return `${date}<br/>${params[0].seriesName}: ${duration} 分钟`
      }
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      name: '学习时长 (分钟)'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    series: [
      {
        name: '班级学习时长',
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
                color: 'rgba(64, 158, 255, 0.8)'
              },
              {
                offset: 1,
                color: 'rgba(64, 158, 255, 0.1)'
              }
            ]
          }
        },
        emphasis: {
          focus: 'series'
        },
        itemStyle: {
          color: '#409EFF'
        },
        smooth: true,
        data: durations
      }
    ]
  }
  
  // 应用配置
  studyDurationChart.setOption(option)
}

// 初始化课程分布图表
const initCourseDistributionChart = (data) => {
  const chartDom = document.getElementById('courseDistributionChart')
  if (!chartDom) return
  
  // 如果图表实例已存在，销毁它
  if (courseDistributionChart) {
    courseDistributionChart.dispose()
  }
  
  // 创建新的图表实例
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
  
  // 图表配置
  const option = {
    title: {
      text: '课程学习时长分布',
      left: 'center'
    },
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
  
  // 应用配置
  courseDistributionChart.setOption(option)
}

// 初始化学生表现图表
const initStudentPerformanceChart = (data) => {
  const chartDom = document.getElementById('studentPerformanceChart')
  if (!chartDom) return
  
  // 如果图表实例已存在，销毁它
  if (studentPerformanceChart) {
    studentPerformanceChart.dispose()
  }
  
  // 创建新的图表实例
  studentPerformanceChart = echarts.init(chartDom)
  
  // 如果没有数据，显示无数据提示
  if (!data.length) {
    studentPerformanceChart.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center'
      }
    })
    return
  }
  
  // 限制数据数量，只展示前15名
  const displayData = data.slice(0, 15)
  
  // 图表配置
  const option = {
    title: {
      text: '学生学习时长排名',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: '{b}: {c} 分钟'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '学习时长 (分钟)'
    },
    yAxis: {
      type: 'category',
      data: displayData.map(item => item.name),
      axisLabel: {
        interval: 0,
        rotate: 0
      }
    },
    series: [
      {
        name: '学习时长',
        type: 'bar',
        data: displayData.map(item => item.value),
        itemStyle: {
          color: function(params) {
            // 渐变色，根据排名变化
            const colorList = [
              '#f56c6c', '#f89898', '#fab6b6', '#fcd3d3', '#fed1ae', 
              '#fed99c', '#fedf8a', '#ffe178', '#f7e8aa', '#eff0b0', 
              '#e8f0c0', '#d9edd0', '#c9e9e0', '#b9e6ef', '#a9e3ff'
            ];
            return colorList[params.dataIndex] || '#409EFF';
          }
        },
        label: {
          show: true,
          position: 'right',
          formatter: '{c} 分钟'
        }
      }
    ]
  }
  
  // 应用配置
  studentPerformanceChart.setOption(option)
}

// 生成班级学习概要
const generateClassSummary = () => {
  if (!studyStatistics.value || studyStatistics.value.recordCount === 0) {
    return '该班级暂无学习记录。'
  }
  
  // 将分钟转换为小时和分钟
  const totalHours = Math.floor(studyStatistics.value.totalDuration / 60)
  const totalMinutes = studyStatistics.value.totalDuration % 60
  
  const avgHours = Math.floor(studyStatistics.value.averageDuration / 60)
  const avgMinutes = Math.round(studyStatistics.value.averageDuration % 60)
  
  const avgPerStudentHours = Math.floor(studyStatistics.value.averageDurationPerStudent / 60)
  const avgPerStudentMinutes = Math.round(studyStatistics.value.averageDurationPerStudent % 60)
  
  const totalText = totalHours > 0 
    ? `${totalHours}小时${totalMinutes > 0 ? totalMinutes + '分钟' : ''}` 
    : `${totalMinutes}分钟`
    
  const avgText = avgHours > 0 
    ? `${avgHours}小时${avgMinutes > 0 ? avgMinutes + '分钟' : ''}` 
    : `${avgMinutes}分钟`
    
  const avgPerStudentText = avgPerStudentHours > 0 
    ? `${avgPerStudentHours}小时${avgPerStudentMinutes > 0 ? avgPerStudentMinutes + '分钟' : ''}` 
    : `${avgPerStudentMinutes}分钟`
  
  return `该班级共有 ${studyStatistics.value.recordCount} 条学习记录，累计学习时长 ${totalText}，平均每条记录 ${avgText}。共有 ${studyStatistics.value.studentCount} 名学生参与学习，每名学生平均学习时长 ${avgPerStudentText}。`
}

// 返回班级列表
const goBack = () => {
  router.push('/manage/classes')
}

// 处理窗口调整事件
const handleResize = () => {
  if (studyDurationChart) studyDurationChart.resize()
  if (courseDistributionChart) courseDistributionChart.resize()
  if (studentPerformanceChart) studentPerformanceChart.resize()
}

// 生命周期钩子
onMounted(async () => {
  await getClassInfo()
  await getStudyStatistics()
  await getStudyDurationRecords()
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  
  // 销毁所有图表实例
  if (studyDurationChart) studyDurationChart.dispose()
  if (courseDistributionChart) courseDistributionChart.dispose()
  if (studentPerformanceChart) studentPerformanceChart.dispose()
})

// 监听日期范围变化
watch(dateRange, () => {
  handleDateRangeChange()
})
</script>

<template>
  <div class="data-charts-container">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/manage' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/manage/classes' }">班级管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ classInfo.className || '未知班级' }} - 数据分析</el-breadcrumb-item>
      </el-breadcrumb>
      <el-button @click="goBack" type="primary" plain :icon="ArrowLeft" size="small">
        返回班级列表
      </el-button>
    </div>
    
    <div class="page-header">
      <h2>{{ classInfo.className || '班级' }} - 学习数据分析</h2>
      <p>详细的班级学习数据图表分析</p>
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
    
    <!-- 学习数据摘要卡片 -->
    <el-card class="stats-card" v-loading="statsLoading">
      <div class="stats-summary">
        <div class="summary-text">
          {{ generateClassSummary() }}
        </div>
      </div>
      
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-icon"><el-icon><Timer /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ studyStatistics.totalDuration || 0 }}</div>
            <div class="stat-label">总学习时长（分钟）</div>
          </div>
        </div>
        
        <div class="stat-item">
          <div class="stat-icon"><el-icon><Clock /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ studyStatistics.averageDuration?.toFixed(1) || 0 }}</div>
            <div class="stat-label">平均学习时长（分钟/记录）</div>
          </div>
        </div>
        
        <div class="stat-item">
          <div class="stat-icon"><el-icon><Calendar /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ studyStatistics.recordCount || 0 }}</div>
            <div class="stat-label">学习记录数</div>
          </div>
        </div>
        
        <div class="stat-item">
          <div class="stat-icon"><el-icon><Calendar /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ studyStatistics.studentCount || 0 }}</div>
            <div class="stat-label">学生数量</div>
          </div>
        </div>
        
        <div class="stat-item">
          <div class="stat-icon"><el-icon><Timer /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ studyStatistics.averageDurationPerStudent?.toFixed(1) || 0 }}</div>
            <div class="stat-label">学生平均学习时长（分钟/人）</div>
          </div>
        </div>
      </div>
    </el-card>
    
    <!-- 图表容器 -->
    <div class="charts-container" v-loading="chartLoading">
      <!-- 学习时长趋势图 -->
      <el-card class="chart-card">
        <div class="chart-title">班级学习时长趋势</div>
        <div id="studyDurationChart" class="chart"></div>
      </el-card>
      
      <!-- 课程分布图 -->
      <el-card class="chart-card">
        <div class="chart-title">课程学习时长分布</div>
        <div id="courseDistributionChart" class="chart"></div>
      </el-card>
      
      <!-- 学生表现排名图 -->
      <el-card class="chart-card horizontal-chart">
        <div class="chart-title">学生学习时长排名</div>
        <div id="studentPerformanceChart" class="chart"></div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.data-charts-container {
  padding: 20px;
}

.breadcrumb-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
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

.stats-card {
  margin-bottom: 24px;
  border-radius: 8px;
}

.stats-summary {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.summary-text {
  line-height: 1.6;
  color: var(--text-primary);
  font-size: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
}

.stat-icon {
  font-size: 24px;
  color: #409EFF;
  margin-right: 12px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.charts-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.chart-card {
  border-radius: 8px;
  overflow: hidden;
}

.horizontal-chart {
  grid-column: span 2;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.chart {
  height: 400px;
  width: 100%;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .charts-container {
    grid-template-columns: 1fr;
  }
  
  .horizontal-chart {
    grid-column: span 1;
  }
}

@media (max-width: 576px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .filter-bar {
    flex-direction: column;
  }
  
  .date-range-picker {
    width: 100%;
  }
}
</style> 