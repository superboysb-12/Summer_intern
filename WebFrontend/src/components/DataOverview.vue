<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { 
  Histogram, 
  PieChart, 
  DataAnalysis,
  Reading
} from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 统计数据
const totalCourses = ref('--')
const totalClasses = ref('--')
const completionRate = ref('--')
const averageScore = ref('--')

// 卡片数据
const cards = ref([
  { 
    title: '课程总数', 
    value: totalCourses,
    icon: Reading, 
    color: 'linear-gradient(135deg, #c084fc 0%, #a855f7 100%)'
  },
  { 
    title: '班级总数', 
    value: totalClasses,
    icon: DataAnalysis, 
    color: 'linear-gradient(135deg, #f472b6 0%, #ec4899 100%)'
  },
  { 
    title: '课程完成率', 
    value: completionRate,
    icon: PieChart, 
    color: 'linear-gradient(135deg, #38bdf8 0%, #0ea5e9 100%)'
  },
  { 
    title: '平均成绩', 
    value: averageScore,
    icon: Histogram, 
    color: 'linear-gradient(135deg, #4ade80 0%, #22c55e 100%)'
  }
])

// 图表实例
let courseDistributionChart = null
let scoreDistributionChart = null
const chartLoading = ref(true)

// 获取课程总数
const getCourseCount = async () => {
  try {
    const response = await axios.get(`${BaseUrl}courses`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (Array.isArray(response.data)) {
      totalCourses.value = response.data.length.toString()
    } else {
      totalCourses.value = '0'
    }
  } catch (error) {
    console.error('获取课程总数失败:', error)
    totalCourses.value = '0'
  }
}

// 获取班级总数
const getClassCount = async () => {
  try {
    const response = await axios.get(`${BaseUrl}classes`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (Array.isArray(response.data)) {
      totalClasses.value = response.data.length.toString()
    } else {
      totalClasses.value = '0'
    }
  } catch (error) {
    console.error('获取班级总数失败:', error)
    totalClasses.value = '0'
  }
}

// 获取课程完成率
const getCompletionRate = async () => {
  try {
    // 模拟数据，实际应从API获取
    completionRate.value = '78%'
  } catch (error) {
    console.error('获取课程完成率失败:', error)
  }
}

// 获取平均成绩
const getAverageScore = async () => {
  try {
    // 模拟数据，实际应从API获取
    averageScore.value = '85.6'
  } catch (error) {
    console.error('获取平均成绩失败:', error)
  }
}

// 获取最近提交
const recentSubmissions = ref([])
const getRecentSubmissions = async () => {
  try {
    // 模拟数据，实际应从API获取
    recentSubmissions.value = [
      { time: '14:25', content: '学生 张三 提交了《高等数学》作业' },
      { time: '13:10', content: '学生 李四 提交了《数据结构》实验报告' },
      { time: '昨天', content: '学生 王五 提交了《计算机网络》期末项目' },
      { time: '昨天', content: '学生 赵六 提交了《操作系统》课程论文' }
    ]
  } catch (error) {
    console.error('获取最近提交失败:', error)
  }
}

// 获取课程分布数据
const getCourseDistributionData = async () => {
  try {
    chartLoading.value = true
    
    // 模拟数据，实际应从API获取
    const courseCategories = ['计算机科学', '数学', '物理', '化学', '生物']
    const courseData = [12, 8, 6, 5, 11]
    
    // 初始化图表
    await nextTick()
    initCourseDistributionChart(courseCategories, courseData)
    
    chartLoading.value = false
  } catch (error) {
    console.error('获取课程分布数据失败:', error)
    chartLoading.value = false
  }
}

// 获取成绩分布数据
const getScoreDistributionData = async () => {
  try {
    // 模拟数据，实际应从API获取
    const scoreRanges = ['90-100', '80-89', '70-79', '60-69', '0-59']
    const scoreData = [25, 38, 22, 10, 5]
    
    // 初始化图表
    await nextTick()
    initScoreDistributionChart(scoreRanges, scoreData)
  } catch (error) {
    console.error('获取成绩分布数据失败:', error)
  }
}

// 初始化课程分布图表
const initCourseDistributionChart = (categories, data) => {
  const chartDom = document.getElementById('courseDistributionChart')
  if (!chartDom) return
  
  // 如果图表实例已存在，销毁它
  if (courseDistributionChart) {
    courseDistributionChart.dispose()
  }
  
  // 创建新的图表实例
  courseDistributionChart = echarts.init(chartDom)
  
  // 图表配置
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'horizontal',
      bottom: 10,
      data: categories
    },
    series: [
      {
        name: '课程分布',
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
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: categories.map((category, index) => ({
          value: data[index],
          name: category
        }))
      }
    ]
  }
  
  // 设置图表选项
  courseDistributionChart.setOption(option)
  
  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    courseDistributionChart && courseDistributionChart.resize()
  })
}

// 初始化成绩分布图表
const initScoreDistributionChart = (ranges, data) => {
  const chartDom = document.getElementById('scoreDistributionChart')
  if (!chartDom) return
  
  // 如果图表实例已存在，销毁它
  if (scoreDistributionChart) {
    scoreDistributionChart.dispose()
  }
  
  // 创建新的图表实例
  scoreDistributionChart = echarts.init(chartDom)
  
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
      data: ranges,
      axisTick: {
        alignWithLabel: true
      }
    },
    yAxis: {
      type: 'value',
      name: '学生人数'
    },
    series: [
      {
        name: '学生人数',
        type: 'bar',
        barWidth: '60%',
        data: data.map((value, index) => {
          const colors = ['#22c55e', '#0ea5e9', '#a855f7', '#f59e0b', '#ef4444']
          return {
            value: value,
            itemStyle: {
              color: colors[index]
            }
          }
        })
      }
    ]
  }
  
  // 设置图表选项
  scoreDistributionChart.setOption(option)
  
  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    scoreDistributionChart && scoreDistributionChart.resize()
  })
}

// 初始化数据
const initData = async () => {
  try {
    await Promise.all([
      getCourseCount(),
      getClassCount(),
      getCompletionRate(),
      getAverageScore(),
      getRecentSubmissions()
    ])
    
    // 获取图表数据
    await Promise.all([
      getCourseDistributionData(),
      getScoreDistributionData()
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
  if (courseDistributionChart) {
    courseDistributionChart.dispose()
    courseDistributionChart = null
  }
  
  if (scoreDistributionChart) {
    scoreDistributionChart.dispose()
    scoreDistributionChart = null
  }
  
  // 移除事件监听器
  window.removeEventListener('resize', () => {
    courseDistributionChart && courseDistributionChart.resize()
    scoreDistributionChart && scoreDistributionChart.resize()
  })
})
</script>

<template>
  <div class="data-overview-container">
    <div class="page-header">
      <h2>数据概览</h2>
      <p>课程与学习数据分析</p>
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
            <span>课程分类分布</span>
            <el-button text>详情</el-button>
          </div>
        </template>
        <div v-loading="chartLoading" class="chart-container">
          <div id="courseDistributionChart" class="chart-inner"></div>
        </div>
      </el-card>
      
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>学生成绩分布</span>
            <el-button text>详情</el-button>
          </div>
        </template>
        <div v-loading="chartLoading" class="chart-container">
          <div id="scoreDistributionChart" class="chart-inner"></div>
        </div>
      </el-card>
    </div>
    
    <div class="card-row">
      <el-card class="table-card">
        <template #header>
          <div class="card-header">
            <span>最近提交</span>
            <el-button text>查看全部</el-button>
          </div>
        </template>
        <div class="activity-list">
          <div class="activity-item" v-for="(submission, index) in recentSubmissions" :key="index">
            <div class="activity-time">{{ submission.time }}</div>
            <div class="activity-content">{{ submission.content }}</div>
          </div>
          <div v-if="recentSubmissions.length === 0" class="empty-activity">
            <p>暂无提交记录</p>
          </div>
        </div>
      </el-card>
      
      <el-card class="table-card">
        <template #header>
          <div class="card-header">
            <span>热门课程</span>
            <el-button text>查看全部</el-button>
          </div>
        </template>
        <div class="popular-courses">
          <el-table :data="[
            { name: '数据结构与算法', teacher: '张教授', students: 156, rating: 4.8 },
            { name: '高等数学', teacher: '李教授', students: 142, rating: 4.7 },
            { name: '计算机网络', teacher: '王教授', students: 128, rating: 4.6 },
            { name: '操作系统', teacher: '赵教授', students: 115, rating: 4.5 }
          ]" style="width: 100%">
            <el-table-column prop="name" label="课程名称" />
            <el-table-column prop="teacher" label="授课教师" width="100" />
            <el-table-column prop="students" label="学生数" width="80" />
            <el-table-column prop="rating" label="评分" width="80" />
          </el-table>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
/* 数据概览页面样式 */
.data-overview-container {
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
  margin-bottom: 24px;
}

.card-row:last-child {
  margin-bottom: 0;
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

.popular-courses {
  padding: 10px 0;
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