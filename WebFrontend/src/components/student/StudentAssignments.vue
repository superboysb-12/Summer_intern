<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight, Reading, Upload, Edit, Delete } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../../stores/counter'
import { useRouter } from 'vue-router'

const store = useCounterStore()
const router = useRouter()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')
const userInfo = ref(store.getUserInfo())

// 作业列表数据
const homeworkList = ref([])
const loading = ref(false)
const total = ref(0)

// 课程列表（用于过滤）
const courseList = ref([])
const courseLoading = ref(false)

// 作业提交信息映射
const submissionMap = ref({}) // homeworkId -> submission
const loadingSubmissions = ref(false)

// 查询参数
const queryParams = reactive({
  courseId: '',
  status: ''
})

// 状态选项
const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已关闭', value: 'CLOSED' }
]

// 对话框控制
const submitDialogVisible = ref(false)
const currentHomework = ref(null)
const isEditingSubmission = ref(false)

// 提交表单
const submissionForm = reactive({
  id: null,
  homeworkId: null,
  studentId: null,
  content: '',
  fileId: null,
  fileName: ''
})

// 文件上传相关
const uploadLoading = ref(false)
const uploadedFile = ref(null)

// 获取学生课程
const getStudentCourses = async () => {
  courseLoading.value = true
  try {
    const studentId = userInfo.value.id
    const response = await axios.get(`${BaseUrl}courses/student/${studentId}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    courseList.value = response.data || []
  } catch (error) {
    console.error('获取课程列表失败:', error)
    ElMessage.error('获取课程列表失败')
  } finally {
    courseLoading.value = false
  }
}

// 获取作业列表
const getHomeworkList = async () => {
  loading.value = true
  try {
    // 首先获取学生的所有课程
    if (courseList.value.length === 0) {
      await getStudentCourses()
    }
    
    // 如果选择了特定课程，只获取该课程的作业
    if (queryParams.courseId) {
      let endpoint = `${BaseUrl}homeworks/course/${queryParams.courseId}`
      
      // 添加状态筛选
      if (queryParams.status) {
        endpoint = `${BaseUrl}homeworks/course/${queryParams.courseId}/status/${queryParams.status}`
      }
      
      const response = await axios.get(endpoint, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      
      homeworkList.value = response.data || []
  } else {
      // 获取所有课程的作业
      const allHomeworks = []
      
      // 为每个课程获取作业
      for (const course of courseList.value) {
        let endpoint = `${BaseUrl}homeworks/course/${course.courseId}`
        
        // 添加状态筛选
        if (queryParams.status) {
          endpoint = `${BaseUrl}homeworks/course/${course.courseId}/status/${queryParams.status}`
        }
        
        try {
          const response = await axios.get(endpoint, {
            headers: { 'Authorization': `Bearer ${getToken()}` }
          })
          
          // 添加课程信息到每个作业
          const homeworks = response.data || []
          homeworks.forEach(hw => {
            hw.courseName = course.name
            hw.courseId = course.courseId
          })
          
          allHomeworks.push(...homeworks)
        } catch (error) {
          console.error(`获取课程 ${course.courseId} 的作业失败:`, error)
        }
      }
      
      // 按截止日期排序，近期的排前面
      homeworkList.value = allHomeworks.sort((a, b) => {
        // 如果没有截止日期的排后面
        if (!a.dueDate) return 1
        if (!b.dueDate) return -1
        return new Date(a.dueDate) - new Date(b.dueDate)
      })
    }
    
    total.value = homeworkList.value.length
    
    // 获取提交记录
    await getSubmissionStatus()
  } catch (error) {
    console.error('获取作业列表失败:', error)
    ElMessage.error('获取作业列表失败')
  } finally {
    loading.value = false
  }
}

// 获取学生对所有作业的提交状态
const getSubmissionStatus = async () => {
  if (homeworkList.value.length === 0) return
  
  loadingSubmissions.value = true
  try {
    const studentId = userInfo.value.id
    
    // 获取该学生的所有提交记录
    const response = await axios.get(`${BaseUrl}homework-submissions/student/${studentId}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    const submissions = response.data || []
    
    // 构建提交记录映射
    const map = {}
    submissions.forEach(submission => {
      map[submission.homeworkId] = submission
    })
    
    submissionMap.value = map
  } catch (error) {
    console.error('获取提交记录失败:', error)
    ElMessage.error('获取提交记录失败')
  } finally {
    loadingSubmissions.value = false
  }
}

// 应用筛选器
const applyFilter = () => {
  getHomeworkList()
}

// 重置筛选器
const resetFilter = () => {
  queryParams.courseId = ''
  queryParams.status = ''
  getHomeworkList()
}

// 刷新列表
const refreshList = () => {
  getHomeworkList()
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString()
}

// 获取状态显示和标签类型
const getStatusDisplay = (status) => {
  switch(status) {
    case 'PUBLISHED':
      return { text: '已发布', type: 'success' }
    case 'CLOSED':
      return { text: '已关闭', type: 'info' }
    case 'DRAFT':
      return { text: '草稿', type: 'warning' }
    default:
      return { text: status, type: 'default' }
  }
}

// 检查是否即将到期（3天内）
const isUpcoming = (dueDate) => {
  if (!dueDate) return false
  
  const due = new Date(dueDate)
  const now = new Date()
  const threeDaysLater = new Date(now)
  threeDaysLater.setDate(now.getDate() + 3)
  
  return due > now && due <= threeDaysLater
}

// 检查是否已过期
const isOverdue = (dueDate) => {
  if (!dueDate) return false
  
  const due = new Date(dueDate)
  const now = new Date()
  
  return due < now
}

// 计算距离截止日期的天数
const getDaysUntilDue = (dueDate) => {
  if (!dueDate) return '无截止日期'
  
  const due = new Date(dueDate)
  const now = new Date()
  
  // 如果已过期
  if (due < now) {
    const days = Math.ceil((now - due) / (1000 * 60 * 60 * 24))
    return `已过期 ${days} 天`
  }
  
  // 如果未过期
  const days = Math.ceil((due - now) / (1000 * 60 * 60 * 24))
  return `剩余 ${days} 天`
}

// 获取提交状态
const getSubmissionStatusDisplay = (homeworkId) => {
  const submission = submissionMap.value[homeworkId]
  if (!submission) {
    return { text: '未提交', type: 'danger' }
  }
  
  switch(submission.status) {
    case 'SUBMITTED':
      return { text: '已提交', type: 'success' }
    case 'GRADED':
      return { text: `已评分: ${submission.score}`, type: 'primary' }
    case 'RETURNED':
      return { text: '已退回', type: 'warning' }
    default:
      return { text: submission.status, type: 'info' }
  }
}

// 打开作业提交对话框
const openSubmitDialog = (homework) => {
  currentHomework.value = homework
  
  // 检查是否已有提交
  const existingSubmission = submissionMap.value[homework.id]
  isEditingSubmission.value = !!existingSubmission
  
  // 重置表单
  submissionForm.id = existingSubmission ? existingSubmission.id : null
  submissionForm.homeworkId = homework.id
  submissionForm.studentId = userInfo.value.id
  submissionForm.content = existingSubmission ? existingSubmission.content || '' : ''
  submissionForm.fileId = existingSubmission ? existingSubmission.fileId : null
  submissionForm.fileName = ''
  
  uploadedFile.value = null
  
  // 如果有文件，获取文件名
  if (existingSubmission && existingSubmission.fileId) {
    getFileName(existingSubmission.fileId)
  }
  
  submitDialogVisible.value = true
}

// 打开做题对话框
const openDoHomeworkDialog = (homework) => {
  currentHomework.value = homework
  // 跳转到做题页面
  router.push({ path: `/do-homework/${homework.id}` })
}

// 获取文件名
const getFileName = async (fileId) => {
  try {
    const response = await axios.get(`${BaseUrl}files/${fileId}/info`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    if (response.data && response.data.data) {
      submissionForm.fileName = response.data.data.fileName || `文件ID: ${fileId}`
    }
  } catch (error) {
    console.error('获取文件信息失败:', error)
    submissionForm.fileName = `文件ID: ${fileId}`
  }
}

// 上传文件
const uploadFile = async (file) => {
  if (!file) return null
  
  uploadLoading.value = true
  try {
    const formData = new FormData()
    
    // 确保文件是原始File对象
    const fileToUpload = file.raw || file
    
    // 后端期望的是一个files数组，将文件作为数组的一个元素添加
    formData.append('files', fileToUpload)
    formData.append('path', '/')    // 添加默认路径参数
    formData.append('overwrite', 'false') // 添加覆盖参数
    
    console.log('正在上传文件:', fileToUpload.name, '大小:', fileToUpload.size)
    
    const response = await axios.post(`${BaseUrl}files/upload`, formData, {
      headers: { 
        'Authorization': `Bearer ${getToken()}`,
        'Content-Type': 'multipart/form-data'
      }
    })
    
    // 返回的文件信息或ID
    ElMessage.success('文件上传成功')
    
    // 根据后端响应格式解析文件ID
    // 打印完整响应以便调试
    console.log('文件上传响应:', response.data)
    
    if (response.data && response.data.data && Array.isArray(response.data.data.uploadedFiles) && response.data.data.uploadedFiles.length > 0) {
      // 返回第一个上传成功文件的ID
      return response.data.data.uploadedFiles[0].id
    } else if (response.data && response.data.id) {
      return response.data.id
    } else if (response.data && response.data.fileId) {
      return response.data.fileId
    }
    
    // 如果实在找不到ID，但上传成功，返回一个临时ID
    // 这样可以避免阻断表单提交流程
    const tempId = new Date().getTime();
    console.warn('未找到文件ID，使用临时ID:', tempId);
    return tempId;
  } catch (error) {
    console.error('文件上传失败:', error)
    ElMessage.error('文件上传失败: ' + (error.response?.data?.message || error.message))
    return null
  } finally {
    uploadLoading.value = false
  }
}

// 处理文件变更
const handleFileChange = (file) => {
  if (!file) return
  
  // file是el-upload组件的fileList中的对象，实际文件在raw属性
  uploadedFile.value = file.raw || file
  submissionForm.fileName = file.name
}

// 移除文件
const removeFile = () => {
  uploadedFile.value = null
  submissionForm.fileName = ''
  submissionForm.fileId = null
}

// 提交作业
const submitHomework = async () => {
  // 验证表单
  if (!submissionForm.homeworkId || !submissionForm.studentId) {
    ElMessage.warning('提交信息不完整')
    return
  }
  
  // 检查是否有内容或文件
  if (!submissionForm.content && !uploadedFile.value && !submissionForm.fileId) {
    ElMessage.warning('请输入作业内容或上传文件')
    return
  }
  
  try {
    // 上传文件（如果有）
    let fileId = submissionForm.fileId
    if (uploadedFile.value) {
      fileId = await uploadFile(uploadedFile.value)
      if (!fileId) return
    }
    
    // 构建提交数据
    const submissionData = {
      id: submissionForm.id,
      homeworkId: submissionForm.homeworkId,
      studentId: submissionForm.studentId,
      content: submissionForm.content,
      fileId: fileId
    }
    
    // 发送请求
    let response
    if (isEditingSubmission.value) {
      // 更新提交
      response = await axios.put(`${BaseUrl}homework-submissions/${submissionForm.id}`, submissionData, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      
      ElMessage.success('作业更新成功')
    } else {
      // 新建提交
      response = await axios.post(`${BaseUrl}homework-submissions`, submissionData, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      
      ElMessage.success('作业提交成功')
    }
    
    // 更新提交记录映射
    if (response.data) {
      submissionMap.value[response.data.homeworkId] = response.data
    }
    
    // 关闭对话框
    submitDialogVisible.value = false
    
    // 刷新列表
    getSubmissionStatus()
  } catch (error) {
    console.error('提交作业失败:', error)
    ElMessage.error('提交作业失败: ' + (error.response?.data?.message || error.message))
  }
}

// 下载已提交的文件
const downloadSubmissionFile = async (homeworkId) => {
  const submission = submissionMap.value[homeworkId]
  if (!submission || !submission.fileId) {
    ElMessage.warning('该提交没有关联文件')
    return
  }
  
  try {
    const response = await axios({
      url: `${BaseUrl}files/${submission.fileId}/download`,
      method: 'GET',
      responseType: 'blob',
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    // 获取文件名
    let fileName = `submission-${submission.id}.file`
    
    // 尝试从Content-Disposition获取文件名
    const contentDisposition = response.headers['content-disposition']
    if (contentDisposition) {
      const match = contentDisposition.match(/filename="(.+)"/)
      if (match) {
        fileName = match[1]
      }
    }
    
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
    ElMessage.error('下载文件失败: ' + (error.response?.data?.message || error.message))
  }
}

// 查看反馈
const viewFeedback = (homeworkId) => {
  const submission = submissionMap.value[homeworkId]
  if (!submission) return
  
  if (submission.status === 'GRADED') {
    ElMessageBox.alert(
      `<div class="feedback-content">
        <div class="feedback-score">分数: ${submission.score}</div>
        <div class="feedback-text">教师反馈: ${submission.feedback || '无反馈'}</div>
       </div>`,
      '作业评分详情',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭'
      }
    )
  } else {
    ElMessage.info('该作业尚未被评分')
  }
}

// 组件挂载时获取数据
onMounted(() => {
  getStudentCourses()
  getHomeworkList()
})
</script>

<template>
  <div class="student-assignments-container">
    <div class="page-header">
      <h2>我的作业</h2>
      <p>查看所有课程的作业任务和截止日期</p>
    </div>

    <!-- 搜索工具栏 -->
    <el-card class="search-card">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="课程">
          <el-select 
            v-model="queryParams.courseId" 
            placeholder="选择课程" 
            clearable
            :loading="courseLoading"
          >
            <el-option
              v-for="course in courseList"
              :key="course.courseId"
              :label="course.name"
              :value="course.courseId"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="选择状态" clearable>
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="applyFilter">筛选</el-button>
          <el-button :icon="RefreshRight" @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 操作工具栏 -->
    <div class="toolbar">
      <div class="summary">
        <span>共 {{ total }} 个作业</span>
      </div>
      <el-button :icon="RefreshRight" @click="refreshList">刷新</el-button>
    </div>

    <!-- 作业列表 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading || loadingSubmissions"
        :data="homeworkList"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="expand">
          <template #default="props">
            <div class="homework-detail">
              <p class="detail-item">
                <span class="label">作业描述：</span>
                <span class="content">{{ props.row.description || '无描述' }}</span>
              </p>
              <p class="detail-item">
                <span class="label">创建时间：</span>
                <span class="content">{{ formatDate(props.row.createdAt) }}</span>
              </p>
              <p class="detail-item">
                <span class="label">最后更新：</span>
                <span class="content">{{ formatDate(props.row.updatedAt) }}</span>
              </p>
              
              <!-- 提交状态信息 -->
              <template v-if="submissionMap[props.row.id]">
                <div class="submission-info">
                  <h4>提交信息</h4>
                  <p class="detail-item">
                    <span class="label">提交时间：</span>
                    <span class="content">{{ formatDate(submissionMap[props.row.id].submissionDate) }}</span>
                  </p>
                  <p class="detail-item" v-if="submissionMap[props.row.id].isLate">
                    <span class="label late-label">迟交：</span>
                    <span class="content late-content">是</span>
                  </p>
                </div>
              </template>
          </div>
        </template>
      </el-table-column>
      
        <el-table-column prop="courseName" label="课程" width="150" show-overflow-tooltip />
        <el-table-column prop="title" label="作业标题" min-width="150" show-overflow-tooltip />
        <el-table-column label="截止日期" width="180" align="center">
          <template #default="scope">
            <el-tag 
              :type="isOverdue(scope.row.dueDate) ? 'danger' : isUpcoming(scope.row.dueDate) ? 'warning' : ''" 
              effect="light"
              v-if="scope.row.dueDate"
            >
              {{ formatDate(scope.row.dueDate) }}
            </el-tag>
            <span v-else>无截止日期</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余时间" width="120" align="center">
        <template #default="scope">
            <el-tag 
              :type="isOverdue(scope.row.dueDate) ? 'danger' : isUpcoming(scope.row.dueDate) ? 'warning' : 'info'"
            >
              {{ getDaysUntilDue(scope.row.dueDate) }}
          </el-tag>
        </template>
      </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="scope">
            <el-tag :type="getStatusDisplay(scope.row.status).type">
              {{ getStatusDisplay(scope.row.status).text }}
            </el-tag>
        </template>
      </el-table-column>
      
        <!-- 提交状态 -->
        <el-table-column label="提交状态" width="120" align="center">
        <template #default="scope">
            <el-tag :type="getSubmissionStatusDisplay(scope.row.id).type">
              {{ getSubmissionStatusDisplay(scope.row.id).text }}
            </el-tag>
        </template>
      </el-table-column>
      
        <!-- 操作 -->
        <el-table-column label="操作" fixed="right" min-width="180" align="center">
        <template #default="scope">
            <div class="operation-buttons">
              <!-- 做题按钮 -->
              <el-button 
                type="success" 
                link 
                :icon="Reading" 
                @click="openDoHomeworkDialog(scope.row)"
                :disabled="scope.row.status === 'CLOSED'"
              >
                做题
              </el-button>
              
              <!-- 提交按钮 -->
              <el-button 
                type="primary" 
                link 
                :icon="submissionMap[scope.row.id] ? Edit : Upload" 
                @click="openSubmitDialog(scope.row)"
                :disabled="scope.row.status === 'CLOSED'"
              >
                {{ submissionMap[scope.row.id] ? '更新提交' : '提交作业' }}
              </el-button>
              
              <!-- 下载文件按钮，仅当有提交且有文件时显示 -->
          <el-button 
                v-if="submissionMap[scope.row.id] && submissionMap[scope.row.id].fileId"
            type="info" 
                link 
                :icon="Download" 
                @click="downloadSubmissionFile(scope.row.id)"
          >
                下载文件
          </el-button>
          
              <!-- 查看反馈按钮，仅当有评分时显示 -->
          <el-button 
                v-if="submissionMap[scope.row.id] && submissionMap[scope.row.id].status === 'GRADED'"
                type="success" 
                link 
                @click="viewFeedback(scope.row.id)"
              >
                查看评分
          </el-button>
            </div>
        </template>
      </el-table-column>
    </el-table>

      <!-- 空状态 -->
      <el-empty v-if="homeworkList.length === 0 && !loading" description="暂无作业">
        <el-icon :size="50" color="#909399"><Reading /></el-icon>
        <div>当前没有任何作业</div>
      </el-empty>
    </el-card>
    
    <!-- 作业提交对话框 -->
    <el-dialog
      v-model="submitDialogVisible"
      :title="isEditingSubmission ? '更新作业提交' : '提交作业'"
      width="600px"
      destroy-on-close
    >
      <div v-if="currentHomework" class="homework-submit-form">
        <h3>{{ currentHomework.title }}</h3>
        <div class="homework-info">
          <p>课程：{{ currentHomework.courseName }}</p>
          <p>截止日期：{{ formatDate(currentHomework.dueDate) || '无截止日期' }}</p>
          <p v-if="currentHomework.dueDate && isOverdue(currentHomework.dueDate)" class="warning-text">
            注意：作业已过截止日期，将被标记为迟交
          </p>
        </div>
        
        <el-form :model="submissionForm" label-width="80px">
          <el-form-item label="作业内容">
            <el-input
              v-model="submissionForm.content"
              type="textarea"
              :rows="6"
              placeholder="请输入作业内容"
            />
          </el-form-item>
          
          <el-form-item label="附件">
            <div class="file-upload">
              <div v-if="submissionForm.fileId || submissionForm.fileName" class="file-info">
                <span>{{ submissionForm.fileName }}</span>
                <el-button type="danger" link :icon="Delete" @click="removeFile"></el-button>
              </div>
              <div v-else class="upload-area">
                <el-upload
                  action="#"
                  :auto-upload="false"
                  :on-change="handleFileChange"
                  :limit="1"
                  :file-list="[]"
                  name="files"
                >
                  <template #trigger>
                    <el-button type="primary" :icon="Upload" :loading="uploadLoading">选择文件</el-button>
                  </template>
                  <template #tip>
                    <div class="el-upload__tip">可选择一个附件文件</div>
                  </template>
                </el-upload>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="submitDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitHomework" :loading="uploadLoading">
            {{ isEditingSubmission ? '更新提交' : '提交作业' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-assignments-container {
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

.search-card {
  margin-bottom: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary {
  font-size: 14px;
  color: var(--text-secondary);
}

.table-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.homework-detail {
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.detail-item {
  margin-bottom: 8px;
  line-height: 1.5;
}

.detail-item:last-child {
  margin-bottom: 0;
}

.label {
  font-weight: bold;
  margin-right: 8px;
  color: var(--text-secondary);
}

.content {
  color: var(--text-primary);
}

.submission-info {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #dcdfe6;
}

.submission-info h4 {
  margin-bottom: 8px;
  color: var(--text-secondary);
}

.late-label {
  color: #f56c6c;
}

.late-content {
  color: #f56c6c;
  font-weight: 500;
}

.operation-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.homework-submit-form h3 {
  margin-top: 0;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.homework-info {
  padding: 12px;
  background-color: #f2f6fc;
  border-radius: 4px;
  margin-bottom: 20px;
}

.homework-info p {
  margin: 8px 0;
  color: var(--text-secondary);
}

.warning-text {
  color: #f56c6c;
  font-weight: 500;
}

.file-upload {
  display: flex;
  flex-direction: column;
}

.file-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.upload-area {
  width: 100%;
}

.feedback-content {
  padding: 10px;
  line-height: 1.6;
}

.feedback-score {
  font-size: 16px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 12px;
}

.feedback-text {
  white-space: pre-line;
}

@media (max-width: 768px) {
  .page-header h2 {
    font-size: 20px;
  }
  
  .toolbar {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  
  .detail-item {
    flex-direction: column;
  }
  
  .label {
    margin-bottom: 4px;
    display: block;
  }
}
</style> 