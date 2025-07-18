<script setup>
import { ref, onMounted, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, RefreshRight, Calendar, Upload, Bell, Document, Download } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'

const props = defineProps({
  courseId: {
    type: [Number, String],
    required: true
  },
  courseName: {
    type: String,
    default: ''
  },
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'refresh'])

const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')
const userInfo = ref(store.getUserInfo())
const isTeacher = ref(userInfo.value.userRole === 'TEACHER')
const isAdmin = ref(userInfo.value.userRole === 'ADMIN')

// Homework list
const homeworkList = ref([])
const loading = ref(false)

// 作业提交管理
const currentHomework = ref(null)
const submissionList = ref([])
const submissionLoading = ref(false)
const submissionsDialogVisible = ref(false)

// 评分表单
const gradeDialogVisible = ref(false)
const currentSubmission = ref(null)
const gradeForm = reactive({
  score: null,
  feedback: ''
})

// 题目关联管理
const questionsDialogVisible = ref(false)
const currentHomeworkForQuestions = ref(null)
const homeworkQuestions = ref([])
const loadingQuestions = ref(false)

// 搜索题目相关
const searchQuestionQuery = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const selectedQuestions = ref([])
const addingQuestions = ref(false)

// 添加题目表单
const newQuestionForm = reactive({
  questionOrder: 1,
  scoreWeight: 1
})

// Query parameters
const queryParams = reactive({
  status: ''
})

// Status options
const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '草稿', value: 'DRAFT' }
]

// Homework form
const homeworkDialogVisible = ref(false)
const homeworkForm = reactive({
  id: undefined,
  title: '',
  description: '',
  courseId: null,
  dueDate: null,
  status: 'PUBLISHED'
})

// Form reference
const homeworkFormRef = ref(null)

// Form validation rules
const rules = {
  title: [
    { required: true, message: '请输入作业标题', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  description: [
    { max: 1000, message: '描述不能超过1000个字符', trigger: 'blur' }
  ]
}

// Dialog control
const dialogTitle = ref('添加作业')
const formMode = ref('add')

// Watch for visibility changes
watch(() => props.visible, (newVal) => {
  if (newVal && props.courseId) {
    loadHomeworks()
  }
})

// Load homeworks for the current course
const loadHomeworks = async () => {
  loading.value = true
  try {
    let endpoint = `${BaseUrl}homeworks/course/${props.courseId}`
    
    // Add status filter if selected
    if (queryParams.status) {
      endpoint = `${BaseUrl}homeworks/course/${props.courseId}/status/${queryParams.status}`
    }
    
    const response = await axios.get(endpoint, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    homeworkList.value = response.data || []
  } catch (error) {
    console.error('获取作业列表失败:', error)
    ElMessage.error('获取作业列表失败: ' + (error.response?.data?.error || error.message))
  } finally {
    loading.value = false
  }
}

// Open dialog to add homework
const openAddHomeworkDialog = () => {
  resetHomeworkForm()
  dialogTitle.value = '添加作业'
  formMode.value = 'add'
  homeworkForm.courseId = props.courseId
  homeworkDialogVisible.value = true
}

// Open dialog to edit homework
const openEditHomeworkDialog = (homework) => {
  resetHomeworkForm()
  dialogTitle.value = '编辑作业'
  formMode.value = 'edit'
  
  // Fill form with homework data
  Object.keys(homeworkForm).forEach(key => {
    if (key in homework) {
      homeworkForm[key] = homework[key]
    }
  })
  
  homeworkDialogVisible.value = true
}

// Reset homework form
const resetHomeworkForm = () => {
  homeworkForm.id = undefined
  homeworkForm.title = ''
  homeworkForm.description = ''
  homeworkForm.courseId = props.courseId
  homeworkForm.dueDate = null
  homeworkForm.status = 'PUBLISHED'
  
  if (homeworkFormRef.value) {
    homeworkFormRef.value.resetFields()
  }
}

// Submit homework form
const submitHomeworkForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const data = { ...homeworkForm }
        
        if (formMode.value === 'add') {
          await axios.post(`${BaseUrl}homeworks`, data, {
            headers: { 'Authorization': `Bearer ${getToken()}` }
          })
          ElMessage.success('作业添加成功，已通知相关学生')
        } else {
          await axios.put(`${BaseUrl}homeworks/${data.id}`, data, {
            headers: { 'Authorization': `Bearer ${getToken()}` }
          })
          ElMessage.success('作业更新成功，已通知相关学生')
        }
        
        homeworkDialogVisible.value = false
        loadHomeworks()
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error('操作失败: ' + (error.response?.data?.error || error.message))
      }
    }
  })
}

// Delete homework
const deleteHomework = (id) => {
  ElMessageBox.confirm('确认要删除该作业吗？这将会通知所有学生。', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`${BaseUrl}homeworks/${id}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      
      ElMessage.success('作业删除成功')
      loadHomeworks()
    } catch (error) {
      console.error('删除作业失败:', error)
      ElMessage.error('删除作业失败: ' + (error.response?.data?.error || error.message))
    }
  }).catch(() => {})
}

// Update homework status
const updateHomeworkStatus = (homework, status) => {
  const statusText = status === 'PUBLISHED' ? '发布' : status === 'CLOSED' ? '关闭' : '设为草稿'
  
  ElMessageBox.confirm(`确认要${statusText}该作业吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.put(`${BaseUrl}homeworks/${homework.id}/status`, { status }, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      
      ElMessage.success(`作业状态已更新为${statusText}`)
      loadHomeworks()
    } catch (error) {
      console.error('更新作业状态失败:', error)
      ElMessage.error('更新作业状态失败: ' + (error.response?.data?.error || error.message))
    }
  }).catch(() => {})
}

// 加载作业提交列表
const loadSubmissions = async (homework) => {
  submissionLoading.value = true
  submissionsDialogVisible.value = true
  currentHomework.value = homework
  
  try {
    const response = await axios.get(`${BaseUrl}homework-submissions/homework/${homework.id}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    // 按提交日期倒序排序（最新的在前）
    submissionList.value = response.data.sort((a, b) => {
      return new Date(b.submissionDate) - new Date(a.submissionDate)
    }) || []
    
    // 为每个提交加载学生信息
    await loadStudentInfoForSubmissions()
  } catch (error) {
    console.error('获取提交列表失败:', error)
    ElMessage.error('获取提交列表失败: ' + (error.response?.data?.error || error.message))
  } finally {
    submissionLoading.value = false
  }
}

// 为提交加载学生信息
const loadStudentInfoForSubmissions = async () => {
  if (submissionList.value.length === 0) return
  
  try {
    // 获取不同学生的ID列表
    const studentIds = [...new Set(submissionList.value.map(sub => sub.studentId))]
    
    // 获取每个学生的信息
    for (const studentId of studentIds) {
      try {
        const response = await axios.get(`${BaseUrl}users/${studentId}`, {
          headers: { 'Authorization': `Bearer ${getToken()}` }
        })
        
        if (response.data) {
          // 添加学生信息到对应的提交记录
          submissionList.value.forEach(sub => {
            if (sub.studentId === studentId) {
              sub.studentName = response.data.username
              sub.studentInfo = response.data
            }
          })
        }
      } catch (error) {
        console.error(`获取学生 ${studentId} 信息失败:`, error)
      }
    }
  } catch (error) {
    console.error('获取学生信息失败:', error)
  }
}

// 下载提交的文件
const downloadSubmissionFile = async (submission) => {
  if (!submission.fileId) {
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
    ElMessage.error('下载文件失败: ' + (error.response?.data?.error || error.message))
  }
}

// 打开评分对话框
const openGradeDialog = (submission) => {
  currentSubmission.value = submission
  gradeForm.score = submission.score || null
  gradeForm.feedback = submission.feedback || ''
  gradeDialogVisible.value = true
}

// 提交评分
const submitGrade = async () => {
  if (!currentSubmission.value) return
  
  if (gradeForm.score === null || gradeForm.score === undefined) {
    ElMessage.warning('请输入分数')
    return
  }
  
  try {
    await axios.put(`${BaseUrl}homework-submissions/${currentSubmission.value.id}/grade`, {
      score: gradeForm.score,
      feedback: gradeForm.feedback
    }, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    ElMessage.success('评分成功，已通知学生')
    gradeDialogVisible.value = false
    
    // 刷新提交列表
    await loadSubmissions(currentHomework.value)
  } catch (error) {
    console.error('评分失败:', error)
    ElMessage.error('评分失败: ' + (error.response?.data?.error || error.message))
  }
}

// 加载作业关联的题目列表
const loadHomeworkQuestions = async (homework) => {
  loadingQuestions.value = true
  currentHomeworkForQuestions.value = homework
  questionsDialogVisible.value = true
  
  try {
    // 获取作业关联的题目列表
    const response = await axios.get(`${BaseUrl}api/homework-questions/homework/${homework.id}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    if (response.data && response.data.success) {
      homeworkQuestions.value = response.data.homeworkQuestions || []
      
      // 加载每个题目的详细信息
      for (const item of homeworkQuestions.value) {
        await loadQuestionDetails(item)
      }
      
      // 按题目顺序排序
      homeworkQuestions.value.sort((a, b) => a.questionOrder - b.questionOrder)
    } else {
      homeworkQuestions.value = []
      ElMessage.warning('获取作业题目失败: ' + (response.data?.message || '未知错误'))
    }
  } catch (error) {
    console.error('获取作业题目失败:', error)
    ElMessage.error('获取作业题目失败: ' + (error.response?.data?.message || error.message))
    homeworkQuestions.value = []
  } finally {
    loadingQuestions.value = false
  }
}

// 加载题目详情
const loadQuestionDetails = async (homeworkQuestion) => {
  try {
    // 注意：确保API路径正确
    const response = await axios.get(`${BaseUrl}api/question-generator/${homeworkQuestion.questionId}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    if (response.data) {
      // 处理两种可能的响应格式
      const questionData = response.data.success ? response.data : response.data;
      homeworkQuestion.questionDetails = questionData;
      
      // 解析问题JSON获取题目类型和内容
      if (questionData.questionJson) {
        try {
          const parsedJson = JSON.parse(questionData.questionJson);
          homeworkQuestion.questionContent = parsedJson.questionText || '无题目内容';
          homeworkQuestion.questionType = questionData.questionType || '未知类型';
        } catch (e) {
          console.error('解析题目JSON失败:', e);
          homeworkQuestion.questionContent = '无法解析题目内容';
        }
      } else {
        // 如果没有questionJson字段，尝试使用其他字段
        homeworkQuestion.questionContent = questionData.query || questionData.content || '无题目内容';
        homeworkQuestion.questionType = questionData.questionType || '未知类型';
      }
    }
  } catch (error) {
    console.error(`获取题目 ${homeworkQuestion.questionId} 详情失败:`, error);
    homeworkQuestion.questionContent = '获取题目详情失败';
  }
}

// 搜索题目
const searchQuestions = async () => {
  // 允许空关键词搜索（加载所有题目）
  if (!searchQuestionQuery.value.trim()) {
    // 不再显示警告，允许空关键词搜索
    // ElMessage.warning('请输入搜索关键词')
    // return
  }
  
  searchLoading.value = true
  try {
    // 调用题目搜索API，使用query参数搜索题目
    const response = await axios.get(`${BaseUrl}api/question-generator/list`, {
      params: {
        query: searchQuestionQuery.value.trim() // 即使为空也发送请求
      },
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    if (response.data && Array.isArray(response.data)) {
      // 直接使用返回的数组
      searchResults.value = response.data || []
      
      // 过滤掉已经添加的题目
      const existingQuestionIds = homeworkQuestions.value.map(q => q.questionId)
      searchResults.value = searchResults.value.filter(q => !existingQuestionIds.includes(q.id))
    } else if (response.data && response.data.success && Array.isArray(response.data.questions)) {
      // 如果返回的是 { success: true, questions: [...] } 格式 - 后端实际返回的格式
      searchResults.value = response.data.questions || []
      
      // 过滤掉已经添加的题目
      const existingQuestionIds = homeworkQuestions.value.map(q => q.questionId)
      searchResults.value = searchResults.value.filter(q => !existingQuestionIds.includes(q.id))
    } else if (response.data && response.data.success && Array.isArray(response.data.data)) {
      // 如果返回的是 { success: true, data: [...] } 格式
      searchResults.value = response.data.data || []
      
      // 过滤掉已经添加的题目
      const existingQuestionIds = homeworkQuestions.value.map(q => q.questionId)
      searchResults.value = searchResults.value.filter(q => !existingQuestionIds.includes(q.id))
    } else {
      searchResults.value = []
      ElMessage.warning('获取题目列表失败: ' + (response.data?.message || '未知错误'))
    }
  } catch (error) {
    console.error('搜索题目失败:', error)
    ElMessage.error('搜索题目失败: ' + (error.response?.data?.message || error.message))
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

// 添加选中的题目到作业
const addSelectedQuestionsToHomework = async () => {
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请选择要添加的题目')
    return
  }
  
  addingQuestions.value = true
  try {
    // 准备批量添加的题目列表
    const questions = selectedQuestions.value.map((question, index) => ({
      questionId: question.id,
      questionOrder: homeworkQuestions.value.length + index + 1, // 从当前最大顺序号开始
      scoreWeight: newQuestionForm.scoreWeight
    }))
    
    // 调用批量添加API
    const response = await axios.post(
      `${BaseUrl}api/homework-questions/${currentHomeworkForQuestions.value.id}/batch-add`,
      { questions },
      { headers: { 'Authorization': `Bearer ${getToken()}` } }
    )
    
    if (response.data && response.data.success) {
      ElMessage.success('添加题目成功')
      // 重新加载题目列表
      await loadHomeworkQuestions(currentHomeworkForQuestions.value)
      
      // 清空选择
      selectedQuestions.value = []
      searchResults.value = []
      searchQuestionQuery.value = ''
    } else {
      ElMessage.error('添加题目失败: ' + (response.data?.message || '未知错误'))
    }
  } catch (error) {
    console.error('添加题目失败:', error)
    ElMessage.error('添加题目失败: ' + (error.response?.data?.message || error.message))
  } finally {
    addingQuestions.value = false
  }
}

// 从作业中移除题目
const removeQuestionFromHomework = async (homeworkQuestion) => {
  try {
    await ElMessageBox.confirm('确认从作业中移除此题目?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await axios.delete(
      `${BaseUrl}api/homework-questions/${currentHomeworkForQuestions.value.id}/questions/${homeworkQuestion.questionId}`,
      { headers: { 'Authorization': `Bearer ${getToken()}` } }
    )
    
    if (response.data && response.data.success) {
      ElMessage.success('移除题目成功')
      // 更新本地题目列表
      homeworkQuestions.value = homeworkQuestions.value.filter(q => q.id !== homeworkQuestion.id)
      // 重新加载题目列表以确保顺序等信息正确
      await loadHomeworkQuestions(currentHomeworkForQuestions.value)
    } else {
      ElMessage.error('移除题目失败: ' + (response.data?.message || '未知错误'))
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除题目失败:', error)
      ElMessage.error('移除题目失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

// 更新题目顺序和权重
const updateHomeworkQuestion = async (homeworkQuestion) => {
  try {
    const response = await axios.put(
      `${BaseUrl}api/homework-questions/${homeworkQuestion.id}`,
      {
        questionOrder: homeworkQuestion.questionOrder,
        scoreWeight: homeworkQuestion.scoreWeight
      },
      { headers: { 'Authorization': `Bearer ${getToken()}` } }
    )
    
    if (response.data && response.data.success) {
      ElMessage.success('更新题目成功')
      // 重新排序本地列表
      homeworkQuestions.value.sort((a, b) => a.questionOrder - b.questionOrder)
    } else {
      ElMessage.error('更新题目失败: ' + (response.data?.message || '未知错误'))
    }
  } catch (error) {
    console.error('更新题目失败:', error)
    ElMessage.error('更新题目失败: ' + (error.response?.data?.message || error.message))
  }
}

// Format date
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString()
}

// Get status display text and tag type
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

// 获取提交状态显示
const getSubmissionStatusDisplay = (status) => {
  switch(status) {
    case 'SUBMITTED':
      return { text: '已提交', type: 'warning' }
    case 'GRADED':
      return { text: '已评分', type: 'success' }
    case 'RETURNED':
      return { text: '已退回', type: 'danger' }
    default:
      return { text: status, type: 'info' }
  }
}

// 获取题目类型显示
const getQuestionTypeDisplay = (type) => {
  const types = {
    '选择题': { text: '选择题', type: 'success' },
    '填空题': { text: '填空题', type: 'warning' },
    '问答题': { text: '问答题', type: 'info' },
    '编程题': { text: '编程题', type: 'danger' }
  }
  
  return types[type] || { text: type || '未知类型', type: 'default' }
}

// Apply filter
const applyFilter = () => {
  loadHomeworks()
}

// Reset filter
const resetFilter = () => {
  queryParams.status = ''
  loadHomeworks()
}

// Close dialog
const handleClose = () => {
  emit('update:visible', false)
}

// Refresh user information
const refreshUserInfo = () => {
  const freshUserInfo = store.getUserInfo()
  userInfo.value = freshUserInfo
  isTeacher.value = freshUserInfo.userRole === 'TEACHER'
  isAdmin.value = freshUserInfo.userRole === 'ADMIN'
}

// Component mounted
onMounted(() => {
  if (props.visible && props.courseId) {
    loadHomeworks()
  }
  
  refreshUserInfo()
})
</script>

<template>
  <el-dialog
    :modelValue="props.visible"
    :title="`作业管理 - ${props.courseName || '课程'}`"
    width="70%"
    @close="handleClose"
    @update:modelValue="handleClose"
    destroy-on-close
  >
    <div class="container">
      <!-- Filter section -->
      <el-card class="mb-md">
        <div class="card-body">
          <el-form :inline="true" :model="queryParams">
            <el-form-item label="作业状态">
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
              <div class="d-flex gap-sm">
                <el-button type="primary" @click="applyFilter">筛选</el-button>
                <el-button @click="resetFilter">重置</el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>
      </el-card>
      
      <!-- Toolbar -->
      <div class="d-flex justify-between mb-md">
        <div>
          <el-button type="primary" :icon="Plus" @click="openAddHomeworkDialog" v-if="isTeacher || isAdmin">
            布置作业
          </el-button>
          <el-text type="info" v-if="!isTeacher && !isAdmin" class="role-debug">
            注意: 您当前角色 "{{ userInfo.userRole || '未知' }}" 没有作业管理权限
            <el-button link type="primary" @click="refreshUserInfo">刷新权限</el-button>
          </el-text>
        </div>
        <el-button :icon="RefreshRight" @click="loadHomeworks">刷新</el-button>
      </div>
      
      <!-- Homework list -->
      <el-card class="mb-lg">
        <div class="card-body">
          <el-table
            v-loading="loading"
            :data="homeworkList"
            border
            style="width: 100%"
          >
            <el-table-column type="index" width="50" align="center" />
            <el-table-column prop="title" label="作业标题" min-width="150" show-overflow-tooltip />
            <el-table-column prop="description" label="作业描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="scope">
                <el-tag :type="getStatusDisplay(scope.row.status).type">
                  {{ getStatusDisplay(scope.row.status).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="dueDate" label="截止日期" width="180" align="center">
              <template #default="scope">
                {{ formatDate(scope.row.dueDate) }}
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
              <template #default="scope">
                {{ formatDate(scope.row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="340" fixed="right" align="center">
              <template #default="scope">
                <div class="d-flex justify-center gap-sm flex-wrap">
                  <el-button type="primary" link :icon="Edit" @click="openEditHomeworkDialog(scope.row)" v-if="isTeacher || isAdmin">编辑</el-button>
                  
                  <el-button 
                    v-if="scope.row.status !== 'PUBLISHED' && (isTeacher || isAdmin)" 
                    type="success" 
                    link 
                    :icon="Bell" 
                    @click="updateHomeworkStatus(scope.row, 'PUBLISHED')"
                  >
                    发布
                  </el-button>
                  
                  <el-button 
                    v-if="scope.row.status !== 'CLOSED' && (isTeacher || isAdmin)" 
                    type="info" 
                    link 
                    :icon="Bell" 
                    @click="updateHomeworkStatus(scope.row, 'CLOSED')"
                  >
                    关闭
                  </el-button>
                  
                  <el-button 
                    type="primary" 
                    link 
                    :icon="Document" 
                    @click="loadSubmissions(scope.row)"
                    v-if="isTeacher || isAdmin"
                  >
                    查看提交
                  </el-button>

                  <el-button 
                    type="success" 
                    link 
                    :icon="Document" 
                    @click="loadHomeworkQuestions(scope.row)"
                    v-if="isTeacher || isAdmin"
                  >
                    题目管理
                  </el-button>
                  
                  <el-button type="danger" link :icon="Delete" @click="deleteHomework(scope.row.id)" v-if="isTeacher || isAdmin">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- Empty state -->
          <el-empty v-if="homeworkList.length === 0 && !loading" description="暂无作业">
            <div class="d-flex justify-center gap-sm" v-if="isTeacher || isAdmin">
              <el-button type="primary" @click="openAddHomeworkDialog">布置作业</el-button>
            </div>
          </el-empty>
          
          <!-- 分页 (如需添加) -->
          <!-- <div class="pagination-container mt-md">...</div> -->
        </div>
      </el-card>
    </div>
    
    <!-- Add/Edit homework dialog -->
    <el-dialog
      v-model="homeworkDialogVisible"
      :title="dialogTitle"
      width="500px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="homeworkFormRef"
        :model="homeworkForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="作业标题" prop="title">
          <el-input v-model="homeworkForm.title" placeholder="请输入作业标题" />
        </el-form-item>
        
        <el-form-item label="作业描述" prop="description">
          <el-input
            v-model="homeworkForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入作业描述"
          />
        </el-form-item>
        
        <el-form-item label="截止日期" prop="dueDate">
          <el-date-picker
            v-model="homeworkForm.dueDate"
            type="datetime"
            placeholder="选择截止日期（可选）"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-select v-model="homeworkForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option
              v-for="item in statusOptions.filter(item => item.value !== '')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="d-flex justify-end gap-sm">
          <el-button @click="homeworkDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitHomeworkForm(homeworkFormRef)">确定</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 提交列表对话框 -->
    <el-dialog
      v-model="submissionsDialogVisible"
      :title="`提交列表 - ${currentHomework?.title || '作业'}`"
      width="80%"
      append-to-body
      destroy-on-close
    >
      <div class="container" v-loading="submissionLoading">
        <div class="mb-lg" v-if="currentHomework">
          <h3 class="text-xl mb-sm">作业详情</h3>
          <div class="bg-light-secondary p-md radius-md">
            <p class="mb-sm"><strong>标题：</strong> {{ currentHomework.title }}</p>
            <p class="mb-sm"><strong>描述：</strong> {{ currentHomework.description || '无描述' }}</p>
            <p class="mb-sm"><strong>截止日期：</strong> {{ formatDate(currentHomework.dueDate) || '无截止日期' }}</p>
            <p class="mb-sm"><strong>状态：</strong> {{ getStatusDisplay(currentHomework.status).text }}</p>
          </div>
        </div>
        
        <div class="mb-lg">
          <h3 class="text-xl mb-md">提交列表 ({{ submissionList.length }})</h3>
          
          <div class="table-responsive">
            <el-table
              :data="submissionList"
              border
              style="width: 100%"
            >
              <el-table-column type="expand">
                <template #default="props">
                  <div class="bg-light-secondary p-md radius-md">
                    <div class="mb-lg" v-if="props.row.content">
                      <h4 class="text-md mb-sm text-secondary">提交内容</h4>
                      <div class="bg-light p-md radius-sm">{{ props.row.content }}</div>
                    </div>
                    
                    <div v-if="props.row.status === 'GRADED'">
                      <h4 class="text-md mb-sm text-secondary">教师评分与反馈</h4>
                      <p class="mb-sm"><strong>分数：</strong> {{ props.row.score }}</p>
                      <p class="mb-sm"><strong>反馈：</strong> {{ props.row.feedback || '无反馈' }}</p>
                    </div>
                  </div>
                </template>
              </el-table-column>
              
              <el-table-column prop="studentName" label="学生" width="120" show-overflow-tooltip>
                <template #default="scope">
                  {{ scope.row.studentName || `学生ID: ${scope.row.studentId}` }}
                </template>
              </el-table-column>
              
              <el-table-column label="提交时间" width="180" align="center">
                <template #default="scope">
                  {{ formatDate(scope.row.submissionDate) }}
                </template>
              </el-table-column>
              
              <el-table-column label="状态" width="120" align="center">
                <template #default="scope">
                  <el-tag :type="getSubmissionStatusDisplay(scope.row.status).type">
                    {{ getSubmissionStatusDisplay(scope.row.status).text }}
                  </el-tag>
                </template>
              </el-table-column>
              
              <el-table-column label="迟交" width="80" align="center">
                <template #default="scope">
                  <el-tag type="danger" v-if="scope.row.isLate">是</el-tag>
                  <el-tag type="success" v-else>否</el-tag>
                </template>
              </el-table-column>
              
              <el-table-column label="分数" width="80" align="center">
                <template #default="scope">
                  {{ scope.row.score !== null ? scope.row.score : '-' }}
                </template>
              </el-table-column>
              
              <el-table-column label="操作" min-width="180" align="center">
                <template #default="scope">
                  <div class="d-flex justify-center gap-sm">
                    <el-button 
                      type="primary" 
                      link 
                      :icon="Edit" 
                      @click="openGradeDialog(scope.row)"
                      v-if="isTeacher || isAdmin"
                    >
                      评分
                    </el-button>
                    
                    <el-button 
                      type="info" 
                      link 
                      :icon="Download" 
                      @click="downloadSubmissionFile(scope.row)"
                      v-if="scope.row.fileId"
                    >
                      下载文件
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
          
          <el-empty v-if="submissionList.length === 0 && !submissionLoading" description="暂无提交记录" />
        </div>
      </div>
      
      <template #footer>
        <div class="d-flex justify-end gap-sm">
          <el-button @click="submissionsDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="loadSubmissions(currentHomework)">刷新提交列表</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 评分对话框 -->
    <el-dialog
      v-model="gradeDialogVisible"
      title="评分"
      width="500px"
      append-to-body
      destroy-on-close
    >
      <div v-if="currentSubmission">
        <div class="bg-light-secondary p-md radius-md mb-md">
          <p class="mb-sm"><strong>学生：</strong> {{ currentSubmission.studentName || `学生ID: ${currentSubmission.studentId}` }}</p>
          <p class="mb-sm"><strong>提交时间：</strong> {{ formatDate(currentSubmission.submissionDate) }}</p>
          <p v-if="currentSubmission.isLate" class="mb-sm text-danger"><strong>注意：</strong> 该提交已逾期</p>
        </div>
        
        <el-form :model="gradeForm" label-width="80px">
          <el-form-item label="分数">
            <el-input-number v-model="gradeForm.score" :min="0" :max="100" />
          </el-form-item>
          
          <el-form-item label="反馈">
            <el-input
              v-model="gradeForm.feedback"
              type="textarea"
              :rows="4"
              placeholder="请输入反馈内容"
            />
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <div class="d-flex justify-end gap-sm">
          <el-button @click="gradeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitGrade">提交评分</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 题目管理对话框 -->
    <el-dialog
      v-model="questionsDialogVisible"
      :title="`题目管理 - ${currentHomeworkForQuestions?.title || '作业'}`"
      width="90%"
      append-to-body
      destroy-on-close
    >
      <div class="container" v-loading="loadingQuestions">
        <!-- 作业题目列表 -->
        <div class="mb-lg">
          <h3 class="text-xl mb-md d-flex justify-between">
            <span>已关联题目 ({{ homeworkQuestions.length }})</span>
            <div>
              <el-button type="primary" size="small" @click="searchQuestions()">
                添加题目
              </el-button>
            </div>
          </h3>
          
          <el-table
            :data="homeworkQuestions"
            border
            style="width: 100%"
            row-key="id"
            v-if="homeworkQuestions.length > 0"
          >
            <el-table-column label="序号" width="80" align="center">
              <template #default="scope">
                <el-input-number 
                  v-model="scope.row.questionOrder" 
                  :min="1" 
                  size="small" 
                  controls-position="right"
                  @change="updateHomeworkQuestion(scope.row)"
                />
              </template>
            </el-table-column>

            <el-table-column prop="questionId" label="题目ID" width="100" align="center" />
            
            <el-table-column prop="questionType" label="题型" width="120" align="center">
              <template #default="scope">
                <el-tag :type="getQuestionTypeDisplay(scope.row.questionType).type">
                  {{ getQuestionTypeDisplay(scope.row.questionType).text }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="questionContent" label="题目内容" min-width="300" show-overflow-tooltip>
              <template #default="scope">
                <div class="mb-sm">{{ scope.row.questionContent || '加载中...' }}</div>
              </template>
            </el-table-column>

            <el-table-column label="权重" width="120" align="center">
              <template #default="scope">
                <el-input-number 
                  v-model="scope.row.scoreWeight" 
                  :min="1" 
                  :max="10" 
                  size="small" 
                  controls-position="right"
                  @change="updateHomeworkQuestion(scope.row)"
                />
              </template>
            </el-table-column>

            <el-table-column label="操作" width="120" align="center" fixed="right">
              <template #default="scope">
                <el-button type="danger" link :icon="Delete" @click="removeQuestionFromHomework(scope.row)">
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-else description="暂无关联题目，请添加题目" />
        </div>

        <!-- 搜索题目 -->
        <div class="mb-lg" v-if="!loadingQuestions">
          <el-divider>添加题目</el-divider>
          
          <div class="d-flex gap-md mb-md align-items-center">
            <el-input
              v-model="searchQuestionQuery"
              placeholder="请输入题目关键词进行搜索"
              prefix-icon="search"
              class="flex-1"
              clearable
            />
            <el-button type="primary" :icon="Search" @click="searchQuestions" :loading="searchLoading">
              搜索题目
            </el-button>
          </div>

          <!-- 搜索结果 -->
          <div v-if="searchResults.length > 0" class="mb-md">
            <h4 class="mb-sm">搜索结果 ({{ searchResults.length }})</h4>

            <el-table
              :data="searchResults"
              border
              style="width: 100%"
              @selection-change="selectedQuestions = $event"
            >
              <el-table-column type="selection" width="55" />
              
              <el-table-column prop="id" label="题目ID" width="100" align="center" />
              
              <el-table-column prop="questionType" label="题型" width="120" align="center">
                <template #default="scope">
                  <el-tag :type="getQuestionTypeDisplay(scope.row.questionType).type">
                    {{ getQuestionTypeDisplay(scope.row.questionType).text }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column prop="query" label="题目关键词" width="150" show-overflow-tooltip />
              
              <el-table-column label="题目内容" min-width="300" show-overflow-tooltip>
                <template #default="scope">
                  <div v-if="scope.row.questionJson">
                    {{ JSON.parse(scope.row.questionJson).questionText || '无题目内容' }}
                  </div>
                  <div v-else>无题目内容</div>
                </template>
              </el-table-column>
            </el-table>

            <div class="d-flex gap-md mt-md justify-end">
              <div class="d-flex align-items-center">
                <span class="mr-sm">题目权重：</span>
                <el-input-number v-model="newQuestionForm.scoreWeight" :min="1" :max="10" size="small" />
              </div>
              
              <el-button type="primary" @click="addSelectedQuestionsToHomework" :disabled="selectedQuestions.length === 0" :loading="addingQuestions">
                添加选中题目 ({{ selectedQuestions.length }})
              </el-button>
            </div>
          </div>

          <el-empty v-else-if="searchQuestionQuery && !searchLoading" description="未找到匹配的题目" />
        </div>
      </div>
      
      <template #footer>
        <div class="d-flex justify-end gap-sm">
          <el-button @click="questionsDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="loadHomeworkQuestions(currentHomeworkForQuestions)">刷新题目列表</el-button>
        </div>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<style scoped>
/* 使用全局样式，仅保留特殊样式 */
.role-debug {
  margin-left: var(--spacing-sm);
  font-size: var(--text-sm);
  color: var(--warning-color);
}

.text-danger {
  color: var(--danger-color);
}

.table-responsive {
  overflow-x: auto;
}
</style> 