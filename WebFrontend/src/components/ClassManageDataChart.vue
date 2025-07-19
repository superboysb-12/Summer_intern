<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, watch, reactive } from 'vue'
import { ElMessage, ElDatePicker, ElMessageBox } from 'element-plus'
import { ArrowLeft, Clock, Timer, Calendar, Search, DataAnalysis, Document } from '@element-plus/icons-vue'
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

// 新增：知识点掌握情况数据
const knowledgePointStats = ref([])
const knowledgePointLoading = ref(true)

// 添加题目生成相关变量
const generateQuestionDialogVisible = ref(false)
const generateForm = reactive({
  query: '',
  ragId: null,
  questionType: ''
})
const questionTypes = ref(['选择题', '判断题', '问答题', '编程题'])
const generatingQuestion = ref(false)
const generateFormRef = ref(null)

// 题目生成表单验证规则
const generateRules = {
  query: [
    { required: true, message: '请输入检索词', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ]
}

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

// 获取班级知识点掌握情况（假设后端接口为 /practice-records/stats/class/{classId}/knowledge-points）
const getKnowledgePointStats = async () => {
  try {
    knowledgePointLoading.value = true
    const response = await axios.get(`${BaseUrl}practice-records/stats/class/${classId.value}/knowledge-points`, { 
      headers: { 'Authorization': `Bearer ${getToken()}` } 
    })
    if (response.data) {
      knowledgePointStats.value = response.data
    } else {
      knowledgePointStats.value = []
    }
    await nextTick()
    initKnowledgePointChart()
    knowledgePointLoading.value = false
  } catch (error) {
    knowledgePointLoading.value = false
    knowledgePointStats.value = []
    ElMessage.error('获取知识点掌握情况失败')
    await nextTick()
    initKnowledgePointChart()
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

// 初始化知识点掌握情况图表
let knowledgePointChart = null
const initKnowledgePointChart = () => {
  const chartDom = document.getElementById('knowledgePointChart')
  if (!chartDom) return
  if (knowledgePointChart) knowledgePointChart.dispose()
  knowledgePointChart = echarts.init(chartDom)
  if (!knowledgePointStats.value.length) {
    knowledgePointChart.setOption({ title: { text: '暂无数据', left: 'center', top: 'center' } })
    return
  }
  const xData = knowledgePointStats.value.map(item => item.knowledge_point.length > 12 ? item.knowledge_point.slice(0,12)+'...' : item.knowledge_point)
  const avgScores = knowledgePointStats.value.map(item => Number(item.average_score?.toFixed(1) || 0))
  const attemptCounts = knowledgePointStats.value.map(item => item.attempt_count)
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: params => {
        const idx = params[0].dataIndex
        return `知识点: ${knowledgePointStats.value[idx].knowledge_point}<br/>平均分: ${avgScores[idx]}<br/>答题次数: ${attemptCounts[idx]}`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    xAxis: { type: 'category', data: xData, axisLabel: { interval: 0, rotate: 30 } },
    yAxis: [
      { type: 'value', name: '平均分', min: 0, max: 100 },
      { type: 'value', name: '答题次数', min: 0, max: Math.max(...attemptCounts, 10) }
    ],
    legend: { data: ['平均分', '答题次数'] },
    series: [
      {
        name: '平均分',
        type: 'bar',
        data: avgScores,
        yAxisIndex: 0,
        itemStyle: {
          color: params => {
            const score = avgScores[params.dataIndex]
            if (score < 60) return '#F56C6C'
            if (score < 80) return '#E6A23C'
            return '#67C23A'
          }
        },
        label: { show: true, position: 'top', formatter: '{c}' }
      },
      {
        name: '答题次数',
        type: 'line',
        data: attemptCounts,
        yAxisIndex: 1,
        itemStyle: { color: '#409EFF' },
        label: { show: false }
      }
    ]
  }
  knowledgePointChart.setOption(option)
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

// 打开生成题目对话框
const openGenerateQuestionDialog = (rag) => {
  generateForm.query = ''
  generateForm.ragId = rag.id
  generateForm.questionType = ''
  generateQuestionDialogVisible.value = true
}

// 提交生成题目
const submitGenerateForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        generatingQuestion.value = true
        
        // 使用RAG ID生成题目
        const url = `http://localhost:8080/api/question-generator/generate-with-rag`
        const requestData = {
          query: generateForm.query,
          ragId: generateForm.ragId,
          questionType: generateForm.questionType || undefined
        }
        
        const response = await axios.post(url, requestData, {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        })
        
        if (response.data && response.data.success) {
          ElMessage.success('题目生成任务已提交')
          generateQuestionDialogVisible.value = false
          
          // 询问用户是否跳转到问题生成页面
          ElMessageBox.confirm(
            '题目生成任务已提交，是否跳转到问题生成页面查看进度？',
            '提示',
            {
              confirmButtonText: '是',
              cancelButtonText: '否',
              type: 'info'
            }
          ).then(() => {
            router.push('/question-generator')
          }).catch(() => {})
        } else {
          ElMessage.error(response.data?.message || '提交题目生成任务失败')
        }
      } catch (error) {
        console.error('提交题目生成任务失败:', error)
        ElMessage.error(error.response?.data?.message || '提交题目生成任务失败')
      } finally {
        generatingQuestion.value = false
      }
    }
  })
}

// 处理窗口调整事件
const handleResize = () => {
  if (studyDurationChart) studyDurationChart.resize()
  if (courseDistributionChart) courseDistributionChart.resize()
  if (studentPerformanceChart) studentPerformanceChart.resize()
  if (knowledgePointChart) knowledgePointChart.resize()
}

// 生命周期钩子
onMounted(async () => {
  await getClassInfo()
  await getStudyStatistics()
  await getStudyDurationRecords()
  await getKnowledgePointStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  
  // 销毁所有图表实例
  if (studyDurationChart) studyDurationChart.dispose()
  if (courseDistributionChart) courseDistributionChart.dispose()
  if (studentPerformanceChart) studentPerformanceChart.dispose()
  if (knowledgePointChart) knowledgePointChart.dispose()
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

      <!-- 知识点掌握情况图 -->
      <el-card class="chart-card full-width-card">
        <template #header>
          <div class="card-header">
            <span>知识点掌握情况</span>
            <el-button text>导出</el-button>
          </div>
        </template>
        <div v-loading="knowledgePointLoading" class="chart-container">
          <div id="knowledgePointChart" class="chart-inner">
            <!-- 在rag-actions中添加生成题目按钮 -->
            <div class="rag-actions">
              <el-button type="primary" @click="openGenerateQuestionDialog(rag)">
                <el-icon><Search /></el-icon> 知识查询
              </el-button>
              <el-button type="info" @click="viewKnowledgeGraph(rag)">
                <el-icon><DataAnalysis /></el-icon> 查看知识图谱
              </el-button>
              <el-button type="success" @click="openGenerateQuestionDialog(rag)">
                <el-icon><Document /></el-icon> 生成题目
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 添加生成题目对话框 -->
    <el-dialog
      v-model="generateQuestionDialogVisible"
      title="生成题目"
      width="500px"
      append-to-body
    >
      <el-form
        ref="generateFormRef"
        :model="generateForm"
        :rules="generateRules"
        label-width="100px"
      >
        <el-form-item label="检索词" prop="query">
          <el-input v-model="generateForm.query" placeholder="请输入检索词" />
        </el-form-item>
        
        <el-form-item label="题目类型">
          <el-select v-model="generateForm.questionType" placeholder="请选择题目类型（可选）" clearable style="width: 100%">
            <el-option
              v-for="item in questionTypes"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="generateQuestionDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="submitGenerateForm(generateFormRef)" 
            :loading="generatingQuestion"
          >
            生成题目
          </el-button>
        </div>
      </template>
    </el-dialog>
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

.full-width-card {
  grid-column: span 2;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 400px; /* Adjust height as needed */
  width: 100%;
}

.chart-inner {
  height: 100%;
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