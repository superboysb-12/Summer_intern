<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { Search, RefreshRight, Download } from '@element-plus/icons-vue'

const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

const loading = ref(false)
const courseStats = ref([])
const studentStats = ref([])
const totalEnrollments = ref(0)
const activeTab = ref('courses')

// 查询参数
const queryParams = ref({
  courseKeyword: '',
  studentKeyword: '',
  status: 'all'
})

// 状态选项
const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '进行中', value: 'enrolled' },
  { label: '已完成', value: 'completed' },
  { label: '已退课', value: 'withdrawn' }
]

// 获取选课统计数据
const fetchEnrollmentStats = async () => {
  loading.value = true
  
  try {
    // 获取所有课程
    const coursesResponse = await axios.get(`${BaseUrl}courses`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    // 获取所有学生
    const studentsResponse = await axios.get(`${BaseUrl}users`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    // 获取所有选课记录
    const enrollmentsResponse = await axios.get(`${BaseUrl}student-courses`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    const courses = coursesResponse.data || []
    const students = studentsResponse.data || []
    const enrollments = enrollmentsResponse.data || []
    
    totalEnrollments.value = enrollments.length
    
    // 处理课程统计
    const courseMap = new Map()
    courses.forEach(course => {
      courseMap.set(course.courseId, {
        ...course,
        totalStudents: 0,
        enrolledStudents: 0,
        completedStudents: 0,
        withdrawnStudents: 0,
        averageProgress: 0
      })
    })
    
    // 处理学生统计
    const studentMap = new Map()
    students.forEach(student => {
      studentMap.set(student.id, {
        ...student,
        totalCourses: 0,
        enrolledCourses: 0,
        completedCourses: 0,
        withdrawnCourses: 0,
        averageProgress: 0
      })
    })
    
    // 计算统计数据
    enrollments.forEach(enrollment => {
      // 更新课程统计
      if (courseMap.has(enrollment.courseId)) {
        const courseStats = courseMap.get(enrollment.courseId)
        courseStats.totalStudents++
        
        if (enrollment.status === 'enrolled') {
          courseStats.enrolledStudents++
        } else if (enrollment.status === 'completed') {
          courseStats.completedStudents++
        } else if (enrollment.status === 'withdrawn') {
          courseStats.withdrawnStudents++
        }
        
        // 更新平均进度
        courseStats.averageProgress = 
          (courseStats.averageProgress * (courseStats.totalStudents - 1) + (enrollment.progress || 0)) / 
          courseStats.totalStudents
      }
      
      // 更新学生统计
      if (studentMap.has(enrollment.studentId)) {
        const studentStat = studentMap.get(enrollment.studentId)
        studentStat.totalCourses++
        
        if (enrollment.status === 'enrolled') {
          studentStat.enrolledCourses++
        } else if (enrollment.status === 'completed') {
          studentStat.completedCourses++
        } else if (enrollment.status === 'withdrawn') {
          studentStat.withdrawnCourses++
        }
        
        // 更新平均进度
        studentStat.averageProgress = 
          (studentStat.averageProgress * (studentStat.totalCourses - 1) + (enrollment.progress || 0)) / 
          studentStat.totalCourses
      }
    })
    
    courseStats.value = Array.from(courseMap.values())
    studentStats.value = Array.from(studentMap.values())
    
  } catch (error) {
    console.error('获取选课统计数据失败:', error)
    ElMessage.error('获取选课统计数据失败')
  } finally {
    loading.value = false
  }
}

// 过滤课程数据
const filteredCourseStats = computed(() => {
  let result = [...courseStats.value]
  
  if (queryParams.value.courseKeyword) {
    const keyword = queryParams.value.courseKeyword.toLowerCase()
    result = result.filter(course => 
      course.name.toLowerCase().includes(keyword) ||
      (course.description && course.description.toLowerCase().includes(keyword))
    )
  }
  
  return result
})

// 过滤学生数据
const filteredStudentStats = computed(() => {
  let result = [...studentStats.value]
  
  if (queryParams.value.studentKeyword) {
    const keyword = queryParams.value.studentKeyword.toLowerCase()
    result = result.filter(student => 
      student.username.toLowerCase().includes(keyword) ||
      (student.email && student.email.toLowerCase().includes(keyword))
    )
  }
  
  return result
})

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString()
}

// 格式化百分比
const formatPercent = (value) => {
  return Math.round(value) + '%'
}

// 获取状态类型
const getStatusType = (status) => {
  switch(status) {
    case 'enrolled': return 'warning'
    case 'completed': return 'success'
    case 'withdrawn': return 'info'
    default: return 'info'
  }
}

// 导出选课数据
const exportData = () => {
  let data = []
  let headers = []
  
  if (activeTab.value === 'courses') {
    headers = ['课程ID', '课程名称', '学生总数', '进行中', '已完成', '已退课', '平均进度']
    data = filteredCourseStats.value.map(course => [
      course.courseId,
      course.name,
      course.totalStudents,
      course.enrolledStudents,
      course.completedStudents,
      course.withdrawnStudents,
      formatPercent(course.averageProgress)
    ])
  } else {
    headers = ['学生ID', '用户名', '邮箱', '课程总数', '进行中', '已完成', '已退课', '平均进度']
    data = filteredStudentStats.value.map(student => [
      student.id,
      student.username,
      student.email || '-',
      student.totalCourses,
      student.enrolledCourses,
      student.completedCourses,
      student.withdrawnCourses,
      formatPercent(student.averageProgress)
    ])
  }
  
  // 生成CSV内容
  const csvContent = [
    headers.join(','),
    ...data.map(row => row.join(','))
  ].join('\n')
  
  // 创建下载链接
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.setAttribute('href', url)
  link.setAttribute('download', `选课统计_${activeTab.value}_${new Date().toISOString().split('T')[0]}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 重置搜索
const resetSearch = () => {
  queryParams.value.courseKeyword = ''
  queryParams.value.studentKeyword = ''
  queryParams.value.status = 'all'
}

// 刷新数据
const refreshData = () => {
  fetchEnrollmentStats()
}

// 组件挂载时获取数据
onMounted(() => {
  fetchEnrollmentStats()
})
</script>

<template>
  <div class="enrollment-stats-container">
    <div class="page-header">
      <h2>选课统计</h2>
      <p>查看系统中所有课程的选课情况和学生学习情况</p>
    </div>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ totalEnrollments }}</div>
          <div class="stat-label">选课总数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ courseStats.length }}</div>
          <div class="stat-label">课程总数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ studentStats.length }}</div>
          <div class="stat-label">学生总数</div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 操作工具栏 -->
    <div class="toolbar">
      <el-button :icon="RefreshRight" @click="refreshData">刷新数据</el-button>
      <el-button type="primary" :icon="Download" @click="exportData">导出数据</el-button>
    </div>
    
    <!-- 选项卡 -->
    <el-tabs v-model="activeTab" class="enrollment-tabs">
      <!-- 课程视角 -->
      <el-tab-pane label="课程视角" name="courses">
        <!-- 搜索栏 -->
        <el-card class="search-card">
          <el-form :model="queryParams" inline>
            <el-form-item label="课程名称">
              <el-input
                v-model="queryParams.courseKeyword"
                placeholder="搜索课程名称或描述"
                clearable
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search">搜索</el-button>
              <el-button :icon="RefreshRight" @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        
        <!-- 课程数据表格 -->
        <el-card class="data-card">
          <el-table
            v-loading="loading"
            :data="filteredCourseStats"
            border
            stripe
            style="width: 100%"
          >
            <el-table-column type="index" width="50" />
            <el-table-column prop="courseId" label="课程ID" width="80" />
            <el-table-column prop="name" label="课程名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="totalStudents" label="学生总数" width="100" sortable />
            <el-table-column label="学生状态分布" width="300">
              <template #default="scope">
                <div class="status-distribution">
                  <el-tooltip content="进行中">
                    <div class="status-bar enrolled" :style="{width: `${scope.row.totalStudents ? (scope.row.enrolledStudents / scope.row.totalStudents) * 100 : 0}%`}">
                      {{ scope.row.enrolledStudents }}
                    </div>
                  </el-tooltip>
                  <el-tooltip content="已完成">
                    <div class="status-bar completed" :style="{width: `${scope.row.totalStudents ? (scope.row.completedStudents / scope.row.totalStudents) * 100 : 0}%`}">
                      {{ scope.row.completedStudents }}
                    </div>
                  </el-tooltip>
                  <el-tooltip content="已退课">
                    <div class="status-bar withdrawn" :style="{width: `${scope.row.totalStudents ? (scope.row.withdrawnStudents / scope.row.totalStudents) * 100 : 0}%`}">
                      {{ scope.row.withdrawnStudents }}
                    </div>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="平均进度" width="150" sortable>
              <template #default="scope">
                <el-progress :percentage="Math.round(scope.row.averageProgress)" />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="160">
              <template #default="scope">
                {{ formatDate(scope.row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
      
      <!-- 学生视角 -->
      <el-tab-pane label="学生视角" name="students">
        <!-- 搜索栏 -->
        <el-card class="search-card">
          <el-form :model="queryParams" inline>
            <el-form-item label="学生信息">
              <el-input
                v-model="queryParams.studentKeyword"
                placeholder="搜索学生姓名或邮箱"
                clearable
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search">搜索</el-button>
              <el-button :icon="RefreshRight" @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        
        <!-- 学生数据表格 -->
        <el-card class="data-card">
          <el-table
            v-loading="loading"
            :data="filteredStudentStats"
            border
            stripe
            style="width: 100%"
          >
            <el-table-column type="index" width="50" />
            <el-table-column prop="id" label="学生ID" width="80" />
            <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
            <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
            <el-table-column prop="totalCourses" label="课程总数" width="100" sortable />
            <el-table-column label="课程状态分布" width="300">
              <template #default="scope">
                <div class="status-distribution">
                  <el-tooltip content="进行中">
                    <div class="status-bar enrolled" :style="{width: `${scope.row.totalCourses ? (scope.row.enrolledCourses / scope.row.totalCourses) * 100 : 0}%`}">
                      {{ scope.row.enrolledCourses }}
                    </div>
                  </el-tooltip>
                  <el-tooltip content="已完成">
                    <div class="status-bar completed" :style="{width: `${scope.row.totalCourses ? (scope.row.completedCourses / scope.row.totalCourses) * 100 : 0}%`}">
                      {{ scope.row.completedCourses }}
                    </div>
                  </el-tooltip>
                  <el-tooltip content="已退课">
                    <div class="status-bar withdrawn" :style="{width: `${scope.row.totalCourses ? (scope.row.withdrawnCourses / scope.row.totalCourses) * 100 : 0}%`}">
                      {{ scope.row.withdrawnCourses }}
                    </div>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="平均进度" width="150" sortable>
              <template #default="scope">
                <el-progress :percentage="Math.round(scope.row.averageProgress)" />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="注册时间" min-width="160">
              <template #default="scope">
                {{ formatDate(scope.row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.enrollment-stats-container {
  padding: 20px 0;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  margin-bottom: 8px;
}

.page-header p {
  color: #606266;
  margin: 0;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 10px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}

.search-card {
  margin-bottom: 20px;
}

.data-card {
  margin-bottom: 20px;
}

.status-distribution {
  display: flex;
  height: 24px;
  width: 100%;
  border-radius: 4px;
  overflow: hidden;
}

.status-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  min-width: 24px;
}

.status-bar.enrolled {
  background-color: #E6A23C;
}

.status-bar.completed {
  background-color: #67C23A;
}

.status-bar.withdrawn {
  background-color: #909399;
}

.enrollment-tabs {
  margin-top: 20px;
}
</style> 