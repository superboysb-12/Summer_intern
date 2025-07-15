<script setup>
import { ref, computed, onMounted } from 'vue'
import { useCounterStore } from '../../stores/counter'
import { Calendar, Plus, Check, Search, Document, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const store = useCounterStore()
const userInfo = store.getUserInfo()
const loading = ref(false)
const courses = ref([])
const error = ref(null)
const availableCourses = ref([])
const showEnrollDialog = ref(false)
const enrollLoading = ref(false)
const searchKeyword = ref('')
const selectedCourseForDetail = ref(null)
const showDetailDialog = ref(false)
const detailLoading = ref(false)

// 分类标签
const categoryOptions = ref([
  { label: '全部课程', value: 'all' },
  { label: '热门课程', value: 'popular' },
  { label: '编程语言', value: 'programming' },
  { label: '人工智能', value: 'ai' },
  { label: '数据科学', value: 'data' },
  { label: '软件工程', value: 'software' }
])
const selectedCategory = ref('all')

// 过滤和搜索后的可选课程
const filteredCourses = computed(() => {
  if (!availableCourses.value.length) return []
  
  let filtered = [...availableCourses.value]
  
  // 按分类过滤
  if (selectedCategory.value !== 'all') {
    // 模拟分类过滤（实际应用中应该从后端获取分类数据）
    // 这里假设课程对象有一个category属性，或者通过名称或描述来模拟分类
    filtered = filtered.filter(course => {
      const name = (course.name || '').toLowerCase()
      const desc = (course.description || '').toLowerCase()
      
      switch (selectedCategory.value) {
        case 'programming':
          return name.includes('编程') || name.includes('语言') || 
                 desc.includes('编程') || desc.includes('语言') ||
                 ['java', 'python', 'c++', 'javascript'].some(lang => 
                   name.includes(lang) || desc.includes(lang))
        case 'ai':
          return name.includes('ai') || name.includes('人工智能') || 
                 desc.includes('ai') || desc.includes('人工智能') ||
                 name.includes('机器学习') || desc.includes('机器学习')
        case 'data':
          return name.includes('数据') || desc.includes('数据')
        case 'software':
          return name.includes('软件') || desc.includes('软件') ||
                 name.includes('工程') || desc.includes('工程')
        case 'popular':
          // 这里可以根据一些规则判断热门课程，这里简单按ID靠前的视为热门
          return (course.courseId % 3 === 0)
        default:
          return true
      }
    })
  }
  
  // 按关键字搜索
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.trim().toLowerCase()
    filtered = filtered.filter(
      course => (course.name || '').toLowerCase().includes(keyword) || 
                (course.description || '').toLowerCase().includes(keyword)
    )
  }
  
  return filtered
})

// 加载学生的已选课程
const loadCourses = async () => {
  loading.value = true
  error.value = null
  
  try {
    // 获取学生已选课程
    const response = await axios.get(`http://localhost:8080/courses/student/${userInfo.id}`, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
    
    courses.value = response.data
    
  } catch (err) {
    console.error('获取课程失败:', err)
    error.value = '获取课程信息失败，请稍后重试'
    ElMessage.error(error.value)
    
    // 回退策略：如果获取失败，尝试获取所有课程并模拟进度
    try {
      const allCoursesResponse = await axios.get('http://localhost:8080/courses', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      })
      
      // 添加模拟进度和状态
      courses.value = allCoursesResponse.data.map(course => ({
        ...course,
        progress: Math.floor(Math.random() * 100),
        status: ['enrolled', 'completed', 'enrolled'][Math.floor(Math.random() * 3)]
      }))
    } catch (fallbackErr) {
      console.error('回退获取课程失败:', fallbackErr)
    }
  } finally {
    loading.value = false
  }
}

// 加载可选课程
const loadAvailableCourses = async () => {
  enrollLoading.value = true
  searchKeyword.value = ''
  selectedCategory.value = 'all'
  
  try {
    // 获取所有课程
    const allCoursesResponse = await axios.get('http://localhost:8080/courses', {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
    
    // 已选课程ID集合
    const enrolledCourseIds = new Set(courses.value.map(course => course.courseId))
    
    // 过滤出未选的课程
    availableCourses.value = allCoursesResponse.data.filter(
      course => !enrolledCourseIds.has(course.courseId)
    )
  } catch (err) {
    console.error('获取可选课程失败:', err)
    ElMessage.error('获取可选课程失败，请稍后重试')
  } finally {
    enrollLoading.value = false
  }
}

// 选课
const enrollCourse = async (courseId) => {
  try {
    await axios.post('http://localhost:8080/student-courses/enroll', 
      { studentId: userInfo.id, courseId }, 
      { headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` } }
    )
    
    ElMessage.success('选课成功')
    
    // 刷新课程列表
    await loadCourses()
    
    // 刷新可选课程列表，但保持对话框打开状态
    loadAvailableCourses()
  } catch (err) {
    console.error('选课失败:', err)
    ElMessage.error(err.response?.data?.message || '选课失败，请稍后重试')
  }
}

// 课程资源相关
const courseFiles = ref([])
const courseFilesLoading = ref(false)
const activeTab = ref('info')

// 加载课程资源
const loadCourseResources = async (courseId) => {
  if (!courseId) return
  
  courseFilesLoading.value = true
  courseFiles.value = []
  
  try {
    const response = await axios.get(`http://localhost:8080/course-files/course/${courseId}`, {
      params: { isVisible: true }, // 只获取对学生可见的资源
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
    
    if (response.data) {
      courseFiles.value = response.data
      
      // 获取文件详情
      await fetchFileDetails()
    }
  } catch (error) {
    console.error('获取课程资源失败:', error)
    ElMessage.error('获取课程资源失败')
  } finally {
    courseFilesLoading.value = false
  }
}

// 文件详情映射
const fileDetailsMap = ref({})

// 获取文件详情
const fetchFileDetails = async () => {
  const fileIds = courseFiles.value.map(cf => cf.fileId)
  if (fileIds.length === 0) return
  
  try {
    const response = await axios.get(`http://localhost:8080/files/batch`, {
      params: { ids: fileIds.join(',') },
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
    
    const files = response.data || []
    const detailsMap = {}
    
    files.forEach(file => {
      detailsMap[file.id] = file
    })
    
    fileDetailsMap.value = detailsMap
  } catch (error) {
    console.error('获取文件详情失败:', error)
  }
}

// 获取文件名
const getFileName = (fileId) => {
  const file = fileDetailsMap.value[fileId]
  return file ? file.fileName : `文件ID: ${fileId}`
}

// 获取文件大小格式化
const formatFileSize = (fileId) => {
  const file = fileDetailsMap.value[fileId]
  if (!file) return '未知'
  
  const bytes = file.fileSize || 0
  if (bytes === 0) return '0 B'
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = bytes
  
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  
  return `${size.toFixed(2)} ${units[i]}`
}

// 下载文件
const downloadFile = async (fileId) => {
  try {
    const response = await axios({
      url: `http://localhost:8080/files/${fileId}/download`,
      method: 'GET',
      responseType: 'blob',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    
    const file = fileDetailsMap.value[fileId]
    const fileName = file ? file.fileName : `file-${fileId}`
    
    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载文件失败:', error)
    ElMessage.error('下载文件失败')
  }
}

// 修改查看课程详情方法，添加加载资源功能
const viewCourseDetail = (course) => {
  selectedCourseForDetail.value = course
  showDetailDialog.value = true
  activeTab.value = 'info' // 默认显示信息标签页
  
  // 加载课程资源
  loadCourseResources(course.courseId)
}

// 查看可选课程详情也需要加载资源
const viewAvailableCourseDetail = (course) => {
  selectedCourseForDetail.value = course
  showDetailDialog.value = true
  detailLoading.value = true
  activeTab.value = 'info' // 默认显示信息标签页
  
  // 加载课程资源
  loadCourseResources(course.courseId)
  
  // 模拟加载课程详情（实际应用中应该从后端获取）
  setTimeout(() => {
    detailLoading.value = false
  }, 500)
}

// 获取课程状态显示
const getCourseStatusDisplay = (status) => {
  switch(status) {
    case 'enrolled': return '进行中'
    case 'completed': return '已完成'
    case 'withdrawn': return '已退课'
    default: return status || '未知状态'
  }
}

// 获取课程状态对应的标签类型
const getCourseStatusType = (status) => {
  switch(status) {
    case 'enrolled': return 'warning'
    case 'completed': return 'success'
    case 'withdrawn': return 'info'
    default: return 'info'
  }
}

// 打开选课对话框
const openEnrollDialog = () => {
  showEnrollDialog.value = true
  loadAvailableCourses()
}

// 搜索课程
const searchCourses = () => {
  // 已通过计算属性实现
}

// 组件挂载时加载课程
onMounted(() => {
  loadCourses()
})
</script>

<template>
  <div class="student-courses-container">
    <div class="page-header">
      <h1>我的课程</h1>
      <p>查看所有已选课程及学习进度</p>
      <el-button type="primary" @click="openEnrollDialog">
        <el-icon><Plus /></el-icon> 选择新课程
      </el-button>
    </div>

    <el-card class="filter-card" shadow="hover">
      <div class="filter-row">
        <el-input
          placeholder="搜索课程名称"
          prefix-icon="el-icon-search"
          style="width: 300px"
        />
        <el-button type="primary">搜索</el-button>
      </div>
    </el-card>

    <!-- 加载中状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 错误状态 -->
    <el-alert v-if="error" type="error" :title="error" :closable="false" show-icon />

    <!-- 课程列表 -->
    <el-row v-else :gutter="20" class="courses-row">
      <el-col v-for="course in courses" :key="course.courseId" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="course-card">
          <div class="course-header">
            <h2>{{ course.name }}</h2>
            <el-progress type="circle" :percentage="course.progress || 0" :width="40"></el-progress>
          </div>
          
          <div class="course-body">
            <p class="course-desc">{{ course.description || '暂无课程描述' }}</p>
            <div class="course-meta">
              <el-tag size="small" :type="getCourseStatusType(course.status)">
                {{ getCourseStatusDisplay(course.status) }}
              </el-tag>
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                {{ new Date(course.createdAt).toLocaleDateString() }}
              </span>
            </div>
          </div>
          
          <div class="course-actions">
            <el-button type="primary" @click="viewCourseDetail(course)">进入学习</el-button>
          </div>
        </el-card>
      </el-col>
      
      <!-- 没有课程时显示空状态 -->
      <el-empty v-if="courses.length === 0" description="暂无课程，请点击选择新课程按钮选课">
        <el-button type="primary" @click="openEnrollDialog">选择课程</el-button>
      </el-empty>
    </el-row>
    
    <!-- 选课对话框 -->
    <el-dialog
      v-model="showEnrollDialog"
      title="选择课程"
      width="80%"
      :close-on-click-modal="false"
      top="5vh"
    >
      <div class="enroll-dialog-content" v-loading="enrollLoading">
        <!-- 分类与搜索 -->
        <div class="enroll-filter">
          <div class="category-tabs">
            <el-radio-group v-model="selectedCategory" size="large">
              <el-radio-button v-for="option in categoryOptions" :key="option.value" :label="option.value">
                {{ option.label }}
              </el-radio-button>
            </el-radio-group>
          </div>
          
          <div class="search-box">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索课程名称或描述"
              clearable
              @keyup.enter="searchCourses"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>
        
        <!-- 课程列表 -->
        <el-empty v-if="filteredCourses.length === 0" description="暂无符合条件的课程" />
        
        <el-row v-else :gutter="20" class="available-courses">
          <el-col v-for="course in filteredCourses" :key="course.courseId" 
                  :xs="24" :sm="12" :md="8" :lg="6">
            <el-card shadow="hover" class="available-course-card">
              <div class="course-header">
                <h3>{{ course.name }}</h3>
              </div>
              
              <div class="course-body">
                <p class="course-desc">{{ course.description || '暂无课程描述' }}</p>
              </div>
              
              <div class="course-actions">
                <el-button
                  type="primary"
                  size="small"
                  @click="enrollCourse(course.courseId)"
                >
                  <el-icon><Check /></el-icon> 选择此课程
                </el-button>
                <el-button
                  plain
                  size="small"
                  @click="viewAvailableCourseDetail(course)"
                >
                  查看详情
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
      
      <template #footer>
        <el-button @click="showEnrollDialog = false">关闭</el-button>
        <el-button type="primary" @click="showEnrollDialog = false">完成选课</el-button>
      </template>
    </el-dialog>
    
    <!-- 课程详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="selectedCourseForDetail ? selectedCourseForDetail.name : '课程详情'"
      width="70%"
    >
      <el-tabs v-model="activeTab" class="course-detail-tabs">
        <el-tab-pane label="课程信息" name="info">
          <div v-loading="detailLoading" class="course-detail-content">
            <template v-if="selectedCourseForDetail">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="课程名称">{{ selectedCourseForDetail.name }}</el-descriptions-item>
                <el-descriptions-item label="课程描述">{{ selectedCourseForDetail.description || '暂无描述' }}</el-descriptions-item>
                <el-descriptions-item label="开课时间">
                  {{ new Date(selectedCourseForDetail.createdAt).toLocaleString() }}
                </el-descriptions-item>
                <el-descriptions-item v-if="selectedCourseForDetail.status" label="学习状态">
                  <el-tag :type="getCourseStatusType(selectedCourseForDetail.status)">
                    {{ getCourseStatusDisplay(selectedCourseForDetail.status) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item v-if="selectedCourseForDetail.progress !== undefined" label="学习进度">
                  <el-progress :percentage="selectedCourseForDetail.progress || 0"></el-progress>
                </el-descriptions-item>
              </el-descriptions>
              
              <div v-if="!selectedCourseForDetail.status" class="detail-actions">
                <el-button type="primary" @click="enrollCourse(selectedCourseForDetail.courseId)">
                  选择此课程
                </el-button>
              </div>
              
              <div v-else class="detail-actions">
                <el-button type="primary">进入学习</el-button>
              </div>
            </template>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="课程资源" name="resources">
          <div v-loading="courseFilesLoading" class="course-resources-content">
            <el-empty v-if="courseFiles.length === 0" description="暂无可用的课程资源" />
            
            <el-table v-else :data="courseFiles" style="width: 100%">
              <el-table-column label="文件名" min-width="200">
                <template #default="scope">
                  <div class="file-item">
                    <el-icon><Document /></el-icon>
                    <span class="file-name">{{ getFileName(scope.row.fileId) }}</span>
                  </div>
                </template>
              </el-table-column>
              
              <el-table-column prop="resourceType" label="资源类型" width="120">
                <template #default="scope">
                  {{ scope.row.resourceType === 'SLIDES' ? '教学课件' : 
                     scope.row.resourceType === 'EXERCISE' ? '练习资料' :
                     scope.row.resourceType === 'REFERENCE' ? '参考资料' :
                     scope.row.resourceType === 'VIDEO' ? '视频资源' : '其他资源' }}
                </template>
              </el-table-column>
              
              <el-table-column label="大小" width="100">
                <template #default="scope">
                  {{ formatFileSize(scope.row.fileId) }}
                </template>
              </el-table-column>
              
              <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
              
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="scope">
                  <el-button type="primary" link :icon="Download" @click="downloadFile(scope.row.fileId)">
                    下载
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
      
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-courses-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
}

.page-header h1 {
  font-size: 24px;
  margin-bottom: 8px;
  font-weight: 500;
}

.page-header p {
  color: #666;
  margin: 0;
  flex-basis: 100%;
  margin-bottom: 10px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.courses-row {
  margin-top: 20px;
}

.course-card {
  height: 100%;
  margin-bottom: 20px;
  transition: transform 0.3s;
}

.course-card:hover {
  transform: translateY(-5px);
}

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.course-header h2 {
  font-size: 18px;
  margin: 0;
  flex-grow: 1;
}

.course-header h3 {
  font-size: 16px;
  margin: 0;
  flex-grow: 1;
}

.course-body {
  flex-grow: 1;
}

.course-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 15px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 12px;
}

.course-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 15px;
}

.loading-container {
  padding: 20px;
}

/* 选课对话框样式 */
.enroll-dialog-content {
  min-height: 50vh;
}

.enroll-filter {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.category-tabs {
  margin-bottom: 16px;
  overflow-x: auto;
}

.search-box {
  max-width: 400px;
  margin-left: auto;
}

.available-courses {
  margin-top: 20px;
}

.available-course-card {
  height: 100%;
  margin-bottom: 15px;
  transition: transform 0.2s;
}

.available-course-card:hover {
  transform: translateY(-3px);
}

.detail-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .enroll-filter {
    flex-direction: column;
  }
  
  .search-box {
    width: 100%;
    max-width: none;
  }
}

.course-detail-tabs {
  margin-top: -20px;
}

.course-resources-content {
  min-height: 300px;
  padding: 10px 0;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style> 