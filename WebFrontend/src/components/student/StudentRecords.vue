<script setup>
import { ref, onMounted } from 'vue'
import { useCounterStore } from '../../stores/counter'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 注册必需的组件
echarts.use([
  BarChart,
  LineChart,
  PieChart,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  CanvasRenderer
])

const store = useCounterStore()
const userInfo = store.getUserInfo()
const loading = ref(false)

// 学习记录数据
const studyRecords = ref({
  totalCourses: 5,
  completedCourses: 2,
  totalStudyHours: 128,
  averageScore: 87,
  courseScores: [
    { courseName: '前端开发基础', score: 92 },
    { courseName: '前端框架开发', score: 85 },
    { courseName: '前端高级开发', score: 78 },
    { courseName: 'Python编程', score: 90 },
    { courseName: '数据结构与算法', score: 88 }
  ],
  weeklyStudyHours: [
    { week: '周一', hours: 2.5 },
    { week: '周二', hours: 3.0 },
    { week: '周三', hours: 2.0 },
    { week: '周四', hours: 4.5 },
    { week: '周五', hours: 3.5 },
    { week: '周六', hours: 6.0 },
    { week: '周日', hours: 5.0 }
  ]
})

// 渲染成绩分布图表
const renderScoreChart = () => {
  const chartDom = document.getElementById('scoreChart')
  if (!chartDom) return
  
  const chart = echarts.init(chartDom)
  
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
      data: studyRecords.value.courseScores.map(item => item.courseName),
      axisLabel: {
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      max: 100,
      name: '分数'
    },
    series: [
      {
        name: '课程成绩',
        type: 'bar',
        data: studyRecords.value.courseScores.map(item => item.score),
        itemStyle: {
          color: function(params) {
            const score = params.value
            if (score >= 90) return '#67C23A'
            if (score >= 80) return '#409EFF'
            if (score >= 70) return '#E6A23C'
            return '#F56C6C'
          }
        },
        label: {
          show: true,
          position: 'top'
        }
      }
    ]
  }
  
  chart.setOption(option)
  
  window.addEventListener('resize', () => {
    chart.resize()
  })
}

// 渲染周学习时长图表
const renderStudyHoursChart = () => {
  const chartDom = document.getElementById('studyHoursChart')
  if (!chartDom) return
  
  const chart = echarts.init(chartDom)
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: studyRecords.value.weeklyStudyHours.map(item => item.week)
    },
    yAxis: {
      type: 'value',
      name: '小时'
    },
    series: [
      {
        name: '学习时长',
        type: 'line',
        data: studyRecords.value.weeklyStudyHours.map(item => item.hours),
        smooth: true,
        lineStyle: {
          width: 3,
          color: '#409EFF'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0, color: 'rgba(64, 158, 255, 0.5)'
            }, {
              offset: 1, color: 'rgba(64, 158, 255, 0.05)'
            }]
          }
        },
        symbol: 'circle',
        symbolSize: 8
      }
    ]
  }
  
  chart.setOption(option)
  
  window.addEventListener('resize', () => {
    chart.resize()
  })
}

// 组件挂载后渲染图表
onMounted(() => {
  setTimeout(() => {
    renderScoreChart()
    renderStudyHoursChart()
  }, 100)
})
</script>

<template>
  <div class="student-records-container">
    <div class="page-header">
      <h1>学习记录</h1>
      <p>查看您的学习进度和成绩情况</p>
    </div>

    <!-- 总体数据卡片 -->
    <el-row :gutter="20" class="data-overview">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="data-card">
          <div class="data-value">{{ studyRecords.totalCourses }}</div>
          <div class="data-label">总课程数</div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="data-card">
          <div class="data-value">{{ studyRecords.completedCourses }}</div>
          <div class="data-label">已完成课程</div>
          <el-progress 
            :percentage="Math.round(studyRecords.completedCourses / studyRecords.totalCourses * 100)" 
            :stroke-width="8" 
            class="progress"
          />
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="data-card">
          <div class="data-value">{{ studyRecords.totalStudyHours }}</div>
          <div class="data-label">总学习时长(小时)</div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="data-card">
          <div class="data-value">{{ studyRecords.averageScore }}</div>
          <div class="data-label">平均成绩</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <h3>课程成绩</h3>
            </div>
          </template>
          <div id="scoreChart" class="chart"></div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <h3>本周学习时长</h3>
            </div>
          </template>
          <div id="studyHoursChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 课程成绩表格 -->
    <el-card shadow="hover" class="scores-card">
      <template #header>
        <div class="card-header">
          <h3>课程成绩详情</h3>
        </div>
      </template>
      
      <el-table :data="studyRecords.courseScores" style="width: 100%" :border="true" stripe>
        <el-table-column prop="courseName" label="课程名称" min-width="200"></el-table-column>
        <el-table-column prop="score" label="成绩" width="120" align="center">
          <template #default="scope">
            <span :class="[
              'score',
              scope.row.score >= 90 ? 'excellent' : 
              scope.row.score >= 80 ? 'good' : 
              scope.row.score >= 70 ? 'pass' : 'fail'
            ]">{{ scope.row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="120" align="center">
          <template #default="scope">
            <el-tag :type="
              scope.row.score >= 90 ? 'success' : 
              scope.row.score >= 80 ? 'primary' : 
              scope.row.score >= 70 ? 'warning' : 'danger'
            ">
              {{ 
                scope.row.score >= 90 ? '优秀' : 
                scope.row.score >= 80 ? '良好' : 
                scope.row.score >= 70 ? '及格' : '不及格'
              }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.student-records-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  margin-bottom: 8px;
  font-weight: 500;
}

.page-header p {
  color: #666;
  margin: 0;
}

.data-overview {
  margin-bottom: 20px;
}

.data-card {
  text-align: center;
  padding: 10px;
  margin-bottom: 20px;
  height: 100%;
}

.data-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
  color: #409EFF;
}

.data-label {
  font-size: 14px;
  color: #666;
}

.progress {
  margin-top: 10px;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;
  height: 100%;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.chart {
  height: 300px;
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.scores-card {
  margin-bottom: 20px;
}

.score {
  font-weight: bold;
}

.excellent {
  color: #67C23A;
}

.good {
  color: #409EFF;
}

.pass {
  color: #E6A23C;
}

.fail {
  color: #F56C6C;
}
</style> 