<script setup>
import { ref, defineProps, defineEmits, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const props = defineProps({
  // 类型：'class' 或 'course'
  type: {
    type: String,
    required: true,
    validator: (value) => ['class', 'course'].includes(value)
  },
  // 班级ID或课程ID
  id: {
    type: [Number, String],
    required: true
  },
  // 班级名称或课程名称
  name: {
    type: String,
    required: true
  },
  // 是否显示对话框
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible'])

const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

const enrollmentData = ref([])
const loading = ref(false)

// 获取选课情况
const fetchEnrollmentData = async () => {
  if (!props.id) return
  
  loading.value = true
  enrollmentData.value = []
  
  try {
    let response
    
    if (props.type === 'course') {
      // 获取课程的所有学生
      response = await axios.get(`${BaseUrl}student-courses/course/${props.id}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      
      if (response.data && Array.isArray(response.data)) {
        // 获取学生详情
        const studentDetails = []
        for (const enrollment of response.data) {
          try {
            const studentResponse = await axios.get(`${BaseUrl}users/${enrollment.studentId}`, {
              headers: {
                'Authorization': `Bearer ${getToken()}`
              }
            })
            
            if (studentResponse.data) {
              studentDetails.push({
                ...enrollment,
                student: studentResponse.data
              })
            }
          } catch (error) {
            console.error('获取学生详情失败:', error)
          }
        }
        
        enrollmentData.value = studentDetails
      }
    } else if (props.type === 'class') {
      // 获取班级信息
      response = await axios.get(`${BaseUrl}classes/${props.id}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      
      // 班级数据
      const classData = response.data
      
      if (!classData) {
        ElMessage.warning('获取班级信息失败')
        return
      }
      
      try {
        // 获取用户列表，过滤出属于该班级的学生
        const usersResponse = await axios.get(`${BaseUrl}users`, {
          headers: {
            'Authorization': `Bearer ${getToken()}`
          }
        })
        
        if (!usersResponse.data || !Array.isArray(usersResponse.data)) {
          ElMessage.warning('获取学生信息失败')
          return
        }
        
        // 过滤出班级学生
        const students = usersResponse.data.filter(user => 
          user.schoolClass && user.schoolClass.id === parseInt(props.id)
        )
        
        if (students.length === 0) {
          enrollmentData.value = []
          return
        }
        
        const studentsWithCourses = []
        
        for (const student of students) {
          try {
            // 获取学生选课情况
            const studentCoursesResponse = await axios.get(`${BaseUrl}student-courses/student/${student.id}`, {
              headers: {
                'Authorization': `Bearer ${getToken()}`
              }
            })
            
            const courses = studentCoursesResponse.data || []
            
            // 添加学生及其选课信息到列表
            studentsWithCourses.push({
              student: student,
              courses: courses,
              totalCourses: courses.length,
              completedCourses: courses.filter(c => c.status === 'completed').length,
              inProgressCourses: courses.filter(c => c.status === 'enrolled').length
            })
          } catch (error) {
            console.error(`获取学生${student.id}选课情况失败:`, error)
            // 添加学生但没有选课信息
            studentsWithCourses.push({
              student: student,
              courses: [],
              totalCourses: 0,
              completedCourses: 0,
              inProgressCourses: 0
            })
          }
        }
        
        enrollmentData.value = studentsWithCourses
      } catch (error) {
        console.error('获取班级学生信息失败:', error)
        ElMessage.error('获取班级学生信息失败')
      }
    }
  } catch (error) {
    console.error('获取选课情况失败:', error)
    ElMessage.error('获取选课情况失败')
  } finally {
    loading.value = false
  }
}

// 获取学生状态显示
const getStatusDisplay = (status) => {
  switch(status) {
    case 'enrolled': return '进行中'
    case 'completed': return '已完成'
    case 'withdrawn': return '已退课'
    default: return status || '未知状态'
  }
}

// 获取状态对应的标签类型
const getStatusType = (status) => {
  switch(status) {
    case 'enrolled': return 'warning'
    case 'completed': return 'success'
    case 'withdrawn': return 'info'
    default: return 'info'
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString()
}

// 关闭对话框
const closeDialog = () => {
  emit('update:visible', false)
}

// 监听visible变化，当打开对话框时获取数据
onMounted(() => {
  if (props.visible) {
    fetchEnrollmentData()
  }
})

// 监听props.visible变化
watch(() => props.visible, (newVal) => {
  if (newVal) {
    fetchEnrollmentData()
  }
})
</script>

<template>
  <el-dialog
    :modelValue="visible"
    :title="type === 'class' ? `${name}班级选课情况` : `课程《${name}》选课情况`"
    width="900px"
    append-to-body
    @update:modelValue="(val) => emit('update:visible', val)"
    @open="fetchEnrollmentData"
  >
    <div v-loading="loading">
      <!-- 班级选课情况 -->
      <template v-if="type === 'class'">
        <el-empty v-if="enrollmentData.length === 0" description="暂无学生信息" />
        
        <div v-else>
          <!-- 班级总体选课统计 -->
          <el-card class="statistics-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>选课统计概览</span>
              </div>
            </template>
            <div class="statistics-content">
              <div class="stat-item">
                <div class="stat-label">学生总数</div>
                <div class="stat-value">{{ enrollmentData.length }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">总选课数</div>
                <div class="stat-value">{{ enrollmentData.reduce((sum, item) => sum + item.totalCourses, 0) }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">已完成课程</div>
                <div class="stat-value">{{ enrollmentData.reduce((sum, item) => sum + item.completedCourses, 0) }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">进行中课程</div>
                <div class="stat-value">{{ enrollmentData.reduce((sum, item) => sum + item.inProgressCourses, 0) }}</div>
              </div>
            </div>
          </el-card>
          
          <!-- 学生选课详情 -->
          <el-table :data="enrollmentData" style="width: 100%; margin-top: 20px;" border>
            <el-table-column type="expand">
              <template #default="props">
                <el-table :data="props.row.courses" style="width: 100%">
                  <el-table-column prop="name" label="课程名称" />
                  <el-table-column label="学习进度" width="200">
                    <template #default="scope">
                      <el-progress :percentage="scope.row.progress || 0" />
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="100">
                    <template #default="scope">
                      <el-tag :type="getStatusType(scope.row.status)">
                        {{ getStatusDisplay(scope.row.status) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </template>
            </el-table-column>
            <el-table-column label="学生姓名" prop="student.username" />
            <el-table-column label="选课总数" prop="totalCourses" width="100" />
            <el-table-column label="已完成" prop="completedCourses" width="100" />
            <el-table-column label="进行中" prop="inProgressCourses" width="100" />
            <el-table-column label="选课完成率" width="180">
              <template #default="scope">
                <el-progress 
                  :percentage="scope.row.totalCourses ? Math.round((scope.row.completedCourses / scope.row.totalCourses) * 100) : 0"
                  :status="scope.row.totalCourses ? (scope.row.completedCourses === scope.row.totalCourses ? 'success' : 'warning') : 'exception'"
                />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
      
      <!-- 课程选课情况 -->
      <template v-else>
        <el-empty v-if="enrollmentData.length === 0" description="暂无学生选修此课程" />
        
        <el-table v-else :data="enrollmentData" style="width: 100%">
          <el-table-column label="学生信息" min-width="180">
            <template #default="scope">
              <div v-if="scope.row.student">
                <div><strong>{{ scope.row.student.username || '未知用户' }}</strong></div>
                <div>{{ scope.row.student.email || '无邮箱' }}</div>
              </div>
              <div v-else>学生ID: {{ scope.row.studentId }}</div>
            </template>
          </el-table-column>
          <el-table-column label="选课时间" min-width="150">
            <template #default="scope">
              {{ formatDate(scope.row.enrollDate) }}
            </template>
          </el-table-column>
          <el-table-column label="最近学习" min-width="150">
            <template #default="scope">
              {{ formatDate(scope.row.lastAccessDate) }}
            </template>
          </el-table-column>
          <el-table-column label="学习进度" width="120">
            <template #default="scope">
              <el-progress :percentage="scope.row.progress || 0" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusDisplay(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeDialog">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.statistics-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.statistics-content {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 20px;
}

.stat-item {
  text-align: center;
  flex: 1;
  min-width: 120px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

@media (max-width: 768px) {
  .statistics-content {
    flex-direction: column;
  }
}
</style> 