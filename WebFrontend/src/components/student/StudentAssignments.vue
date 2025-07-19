<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight, Reading, Upload, Edit, Delete } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../../stores/counter'
import { useRouter } from 'vue-router'  // 导入路由器

const router = useRouter()  // 使用路由器
const store = useCounterStore()
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
const questionsDialogVisible = ref(false) // 新增：题目详情对话框
const questionsLoading = ref(false) // 新增：题目加载状态

// 作业提交表单
const submissionForm = reactive({
  id: null,
  homeworkId: null,
  studentId: null,
  content: '',
  fileId: null,
  fileName: ''
})

// 新增：题目详情相关
const currentHomeworkQuestions = ref([])
const currentViewingHomework = ref(null)
const currentQuestionIndex = ref(0)
const userAnswers = ref({}) // { questionId: answer }
const practiceResults = ref({}) // { questionId: result }
const gradingState = ref({}) // { questionId: boolean }
const finishingHomework = ref(false) // 新增：提交作业中的状态

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

// 查看作业题目
const viewHomeworkQuestions = async (homework) => {
  currentViewingHomework.value = homework
  questionsDialogVisible.value = true
  questionsLoading.value = true
  
  // 重置状态
  currentHomeworkQuestions.value = []
  currentQuestionIndex.value = 0
  userAnswers.value = {}
  practiceResults.value = {}
  gradingState.value = {}

  try {
    // 1. 获取作业下的问题ID列表和问题的详细信息 (并行)
    const [questionLinksResponse, practiceRecordsResponse] = await Promise.all([
      axios.get(`${BaseUrl}homework-questions/homework/${homework.id}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      }),
      axios.get(`${BaseUrl}practice-records/student/${userInfo.value.id}/homework/${homework.id}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
    ])
    
    // 处理题目
    const questionLinks = questionLinksResponse.data || []
    if (questionLinks.length === 0) {
      currentHomeworkQuestions.value = []
      questionsLoading.value = false
      return
    }

    const questionPromises = questionLinks.map(link =>
      axios.get(`${BaseUrl}api/question-generator/${link.questionId}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
    )
    const questionResponses = await Promise.all(questionPromises)
    const questions = questionResponses.map(res => res.data).filter(Boolean)
    
    const questionOrderMap = new Map(questionLinks.map(link => [link.questionId, link.orderIndex]))
    questions.sort((a, b) => (questionOrderMap.get(a.id) ?? 999) - (questionOrderMap.get(b.id) ?? 999))
    currentHomeworkQuestions.value = questions

    // 处理已有练习记录
    const records = practiceRecordsResponse.data || []
    records.forEach(record => {
      // 存储答案，需要解析 answerData
      try {
        if(record.answerData) {
          const answer = JSON.parse(record.answerData)
          userAnswers.value[record.questionId] = answer.userAnswer
        }
      } catch (e) {
        console.error("解析答案失败", e)
      }
      
      // 存储评分结果
      practiceResults.value[record.questionId] = {
        score: record.score,
        feedback: record.feedback || (record.score >= 60 ? '回答正确' : '回答错误'),
        isCorrect: record.isCorrect,
        aiGenerated: true // 假设都是AI生成的
      }
    })

  } catch (error) {
    console.error('获取作业题目失败:', error)
    ElMessage.error('获取作业题目失败: ' + (error.response?.data?.message || error.message))
  } finally {
    questionsLoading.value = false
  }
}

// --- 做题相关函数 ---

const currentQuestion = computed(() => {
  if (currentHomeworkQuestions.value.length === 0) return null
  return currentHomeworkQuestions.value[currentQuestionIndex.value]
})

const goToNextQuestion = () => {
  if (currentQuestionIndex.value < currentHomeworkQuestions.value.length - 1) {
    currentQuestionIndex.value++
  }
}

const goToPrevQuestion = () => {
  if (currentQuestionIndex.value > 0) {
    currentQuestionIndex.value--
  }
}

const goToQuestion = (index) => {
  currentQuestionIndex.value = index
}

// 提交当前题目答案
const submitCurrentAnswer = async () => {
  const question = currentQuestion.value
  if (!question) return
  
  const userAnswer = userAnswers.value[question.id]
  if (!userAnswer || userAnswer.trim() === '') {
    ElMessage.warning('请先输入答案')
    return
  }

  gradingState.value[question.id] = true
  
  const questionParsed = renderQuestion(question.questionJson)
  const questionType = questionParsed.type
  let result = null

  try {
    if (questionType === '选择题') {
      const isCorrect = userAnswer.trim().toUpperCase() === questionParsed.answer.trim().toUpperCase()
      result = { score: isCorrect ? 100 : 0, isCorrect, feedback: isCorrect ? '回答正确！' : `回答错误。正确答案是: ${questionParsed.answer}` }
    } else if (questionType === '判断题') {
      const isCorrect = (userAnswer === questionParsed.answer)
      result = { score: isCorrect ? 100 : 0, isCorrect, feedback: isCorrect ? '回答正确！' : `回答错误。正确答案是: ${questionParsed.answer}` }
    } else {
      // 问答题和编程题使用AI评分
      const aiResult = await gradeWithAI(questionType, questionParsed, userAnswer)
      result = { ...aiResult, isCorrect: aiResult.score >= 60 }
    }

    practiceResults.value[question.id] = result
    
    // 保存练习记录到后端
    await savePracticeRecord(question.id, result, userAnswer)
    ElMessage.success('答案已提交并评分！')

  } catch (error) {
    console.error('评分失败:', error)
    ElMessage.error('评分失败: ' + (error.response?.data?.message || error.message))
  } finally {
    gradingState.value[question.id] = false
  }
}

// 使用大模型API评分
const gradeWithAI = async (questionType, question, userAnswer) => {
  let systemPrompt
  if (questionType === '问答题') {
    systemPrompt = `你是一位专业的教育评分助手...（此处省略与QuestionGenerator类似的Prompt）...请严格按照以下JSON格式输出你的评分：\n{ "score": 分数（0-100）, "feedback": "详细反馈" }`
  } else if (questionType === '编程题') {
    systemPrompt = `你是一位专业的编程教育评分助手...（此处省略与QuestionGenerator类似的Prompt）...请严格按照以下JSON格式输出你的评分：\n{ "score": 分数（0-100）, "feedback": "详细反馈" }`
  } else {
    return { score: 0, feedback: '不支持的题目类型' }
  }

  const apiMessages = [{ role: "system", content: systemPrompt }, { role: "user", content: userAnswer }]
  
  const aiResponse = await axios.post(`${BaseUrl}api/deepseek/chat`, 
    { messages: apiMessages },
    { headers: { 'Authorization': `Bearer ${getToken()}` } }
  )

  if (aiResponse.data && aiResponse.data.choices && aiResponse.data.choices.length > 0) {
    const content = aiResponse.data.choices[0].message.content
    try {
      // 简单处理json代码块
      const jsonMatch = content.match(/\{[\s\S]*\}/)
      if (jsonMatch) {
        return JSON.parse(jsonMatch[0])
      }
      throw new Error("AI返回的不是有效的JSON")
    } catch (e) {
      console.error("解析AI返回的JSON失败", e)
      return { score: 0, feedback: 'AI评分返回格式错误，原始信息：\n' + content }
    }
  }
  throw new Error("AI服务调用失败")
}

// 保存练习记录
const savePracticeRecord = async (questionId, result, userAnswer) => {
  const payload = {
    studentId: userInfo.value.id,
    homeworkId: currentViewingHomework.value.id,
    questionId: questionId,
    score: result.score,
    answerData: JSON.stringify({ userAnswer }), // 将答案包装成JSON字符串
    feedback: result.feedback,
    isCorrect: result.isCorrect
  }
  
  try {
    await axios.post(`${BaseUrl}practice-records/submit`, payload, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
  } catch (error) {
    console.error('保存练习记录失败:', error)
    // 即使保存失败，也不阻塞前端显示
    ElMessage.error('保存练习记录失败，但评分结果已在本地显示。')
  }
}

// 完成并提交整个作业
const handleFinishHomework = async () => {
  if (!currentViewingHomework.value) return;

  // 二次确认
  try {
    await ElMessageBox.confirm(
      '提交后，您的本次作答将被记录为最终提交，但仍可查看题目。确定要提交整个作业吗？',
      '确认提交',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
  } catch (e) {
    return; // 用户取消
  }

  finishingHomework.value = true;
  const payload = {
    homeworkId: currentViewingHomework.value.id,
    studentId: userInfo.value.id,
    content: '学生通过在线答题系统完成并提交作业。',
    fileId: null,
  };

  try {
    const response = await axios.post(`${BaseUrl}homework-submissions`, payload, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    });
    
    if (response.data) {
      ElMessage.success('作业提交成功！');
      questionsDialogVisible.value = false;
      // 刷新提交状态以更新表格
      await getSubmissionStatus(); 
    } else {
      throw new Error('提交失败，未收到确认信息。')
    }
  } catch (error) {
    console.error('提交作业失败:', error);
    ElMessage.error('提交作业失败: ' + (error.response?.data?.message || error.message));
  } finally {
    finishingHomework.value = false;
  }
}

const getScoreColor = (score) => {
  if (score >= 90) return '#67C23A'
  if (score >= 60) return '#409EFF'
  return '#F56C6C'
}

// --- 从 QuestionGenerator.vue 复制过来的辅助函数 ---

// 格式化JSON显示
const formatJson = (json) => {
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch (e) {
    return json
  }
}

// 渲染题目函数
const renderQuestion = (questionJson) => {
  try {
    if (!questionJson) return ''
    const question = JSON.parse(questionJson)
    if (!question.type) {
      return formatJson(questionJson)
    }
    return question
  } catch (e) {
    return formatJson(questionJson)
  }
}

// 格式化解析文本，处理换行符
const formatExplanation = (text) => {
  if (!text) return ''
  return text.replace(/\n/g, '<br>')
}

// 状态标签的类型
const statusTypeMap = {
  'PENDING': 'warning',
  'COMPLETED': 'success',
  'FAILED': 'danger'
}

// 格式化状态
const formatStatus = (status) => {
  switch (status) {
    case 'PENDING': return '处理中'
    case 'COMPLETED': return '已完成'
    case 'FAILED': return '失败'
    default: return status
  }
}

// --- 辅助函数结束 ---

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
              <!-- 查看题目按钮 -->
              <el-button 
                type="primary" 
                link
                :icon="Reading"
                @click="viewHomeworkQuestions(scope.row)"
              >
                查看题目
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

    <!-- 作业题目详情对话框 (新增) -->
    <el-dialog
      v-model="questionsDialogVisible"
      :title="`正在做作业 - ${currentViewingHomework?.title || ''}`"
      width="80%"
      top="5vh"
      destroy-on-close
      :close-on-click-modal="false"
      class="homework-do-dialog"
    >
      <div v-loading="questionsLoading" class="homework-do-container">

        <!-- 题目导航 -->
        <div class="question-nav">
          <div class="nav-header">题目导航</div>
          <div class="nav-dots">
            <span
              v-for="(q, index) in currentHomeworkQuestions"
              :key="q.id"
              class="nav-dot"
              :class="{
                'active': index === currentQuestionIndex,
                'answered': practiceResults[q.id]
              }"
              @click="goToQuestion(index)"
            >
              {{ index + 1 }}
            </span>
          </div>
        </div>

        <!-- 题目内容区域 -->
        <div v-if="currentQuestion" class="question-display-area">
          <el-card class="question-card">
            <template #header>
              <div class="question-card-header">
                <span>第 {{ currentQuestionIndex + 1 }} 题 / 共 {{ currentHomeworkQuestions.length }} 题</span>
                <el-tag effect="plain">{{ currentQuestion.questionType }}</el-tag>
              </div>
            </template>
            
            <div class="question-content" v-if="currentQuestion.questionJson">
              <!-- 各类题目的展示和作答区域 -->
              <!-- 选择题 -->
              <div v-if="renderQuestion(currentQuestion.questionJson).type === '选择题'">
                <div class="question-title" v-html="renderQuestion(currentQuestion.questionJson).question"></div>
                <el-radio-group v-model="userAnswers[currentQuestion.id]" class="options-container" :disabled="!!practiceResults[currentQuestion.id]">
                  <el-radio 
                    v-for="(option, key) in renderQuestion(currentQuestion.questionJson).options" 
                    :key="key" 
                    :label="key" 
                    border
                    class="option-item"
                  >
                    {{ key }}. {{ option }}
                  </el-radio>
                </el-radio-group>
              </div>

              <!-- 判断题 -->
              <div v-else-if="renderQuestion(currentQuestion.questionJson).type === '判断题'">
                <div class="question-title" v-html="renderQuestion(currentQuestion.questionJson).statement"></div>
                 <el-radio-group v-model="userAnswers[currentQuestion.id]" class="judge-options" :disabled="!!practiceResults[currentQuestion.id]">
                    <el-radio label="正确" border>
                      <span class="judge-icon">✓</span> 正确
                    </el-radio>
                    <el-radio label="错误" border>
                      <span class="judge-icon">✗</span> 错误
                    </el-radio>
                  </el-radio-group>
              </div>

              <!-- 问答题/编程题 -->
              <div v-else>
                <div class="question-title" v-html="renderQuestion(currentQuestion.questionJson).question || renderQuestion(currentQuestion.questionJson).title"></div>
                <div v-if="renderQuestion(currentQuestion.questionJson).description" class="section-content" v-html="formatExplanation(renderQuestion(currentQuestion.questionJson).description)"></div>
                <el-input
                  v-model="userAnswers[currentQuestion.id]"
                  type="textarea"
                  :rows="8"
                  placeholder="请在此处输入您的答案"
                  :disabled="!!practiceResults[currentQuestion.id]"
                  class="answer-textarea"
                />
              </div>

              <!-- 评分结果显示区域 -->
              <div v-if="practiceResults[currentQuestion.id]" class="practice-result">
                <el-divider>评分结果</el-divider>
                <div class="result-feedback">
                  <div class="result-score">
                    得分：<span :style="{ color: getScoreColor(practiceResults[currentQuestion.id].score) }">{{ practiceResults[currentQuestion.id].score }}</span>
                  </div>
                  <div class="result-text" v-html="formatExplanation(practiceResults[currentQuestion.id].feedback)"></div>
                </div>
                <!-- 正确答案显示 -->
                <div class="correct-answer-section" v-if="!practiceResults[currentQuestion.id].isCorrect">
                   <div v-if="renderQuestion(currentQuestion.questionJson).answer" class="question-answer">
                      <span class="answer-label">正确答案：</span>
                      <span class="answer-content">{{ renderQuestion(currentQuestion.questionJson).answer }}</span>
                    </div>
                    <div v-if="renderQuestion(currentQuestion.questionJson).explanation" class="question-explanation">
                      <div class="explanation-label">解析：</div>
                      <div class="explanation-content" v-html="formatExplanation(renderQuestion(currentQuestion.questionJson).explanation)"></div>
                    </div>
                </div>
              </div>

            </div>
          </el-card>
        </div>
        <el-empty v-else-if="!questionsLoading" description="该作业下没有题目" />
      </div>

      <template #footer>
        <div class="dialog-footer homework-do-footer">
          <el-button @click="goToPrevQuestion" :disabled="currentQuestionIndex === 0">上一题</el-button>
          
          <el-button 
            type="primary" 
            @click="submitCurrentAnswer"
            :loading="gradingState[currentQuestion?.id]"
            :disabled="!userAnswers[currentQuestion?.id] || !!practiceResults[currentQuestion?.id]"
          >
            提交答案
          </el-button>

          <el-button 
            type="success" 
            @click="handleFinishHomework"
            :loading="finishingHomework"
            :disabled="!!submissionMap[currentViewingHomework?.id] || currentViewingHomework?.status === 'CLOSED'"
          >
            完成并提交作业
          </el-button>

          <el-button @click="goToNextQuestion" :disabled="currentQuestionIndex === currentHomeworkQuestions.length - 1">下一题</el-button>
          
          <el-button @click="questionsDialogVisible = false" style="margin-left: auto;">关闭</el-button>
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

.homework-questions {
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.homework-questions h3 {
  margin-top: 0;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.homework-questions p {
  margin: 8px 0;
  color: var(--text-secondary);
}

.homework-questions .warning-text {
  color: #f56c6c;
  font-weight: 500;
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

/* --- 从 QuestionGenerator.vue 复制过来的样式 --- */

.homework-questions-container {
  max-height: 75vh;
  overflow-y: auto;
  padding: 0 10px;
}

.question-card {
  margin-bottom: 20px;
}

.question-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-content {
  background-color: var(--el-fill-color-lighter);
  padding: 15px;
  border-radius: 4px;
  overflow-x: auto;
}

.question-content pre {
  white-space: pre-wrap;
  margin: 0;
}

.choice-question,
.true-false-question,
.qa-question,
.programming-question {
  padding: 15px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background-color: var(--el-fill-color-light);
}

.question-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 15px;
  color: var(--el-color-primary);
}

.question-type-tag {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
  margin-right: 8px;
  font-weight: bold;
}

.options-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 15px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background-color: var(--el-fill-color-light);
}

.option-item:hover {
  background-color: var(--el-color-primary-light-8);
}

.option-item.correct-option {
  border-color: var(--el-color-success);
  background-color: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.option-key {
  font-weight: bold;
  margin-right: 10px;
  color: var(--el-color-primary);
}

.option-content {
  flex-grow: 1;
}

.question-answer {
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-light);
  font-weight: bold;
  color: var(--el-color-danger);
}

.question-explanation {
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-light);
}

.explanation-label {
  font-weight: bold;
  margin-bottom: 5px;
  color: var(--el-color-info);
}

.explanation-content {
  color: var(--el-text-color-secondary);
}

.true-false-question {
  padding: 15px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background-color: var(--el-fill-color-light);
}

.judge-tag {
  background-color: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.judge-options {
  display: flex;
  justify-content: space-around;
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-light);
}

.judge-option {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background-color: var(--el-fill-color-light);
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.judge-option:hover {
  background-color: var(--el-color-primary-light-8);
}

.judge-option.selected-option {
  border-color: var(--el-color-success);
  background-color: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.judge-icon {
  font-size: 18px;
  margin-right: 5px;
}

.qa-question {
  padding: 15px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background-color: var(--el-fill-color-light);
}

.qa-tag {
  background-color: var(--el-color-info-light-9);
  color: var(--el-color-info);
}

.key-points {
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-light);
}

.key-points-title {
  font-weight: bold;
  margin-bottom: 5px;
  color: var(--el-color-primary);
}

.key-points-list {
  list-style-type: disc;
  padding-left: 20px;
  margin: 10px 0;
}

.key-points-list li {
  margin-bottom: 8px;
  padding-left: 5px;
  position: relative;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.key-points-list li::before {
  content: "•";
  color: var(--el-color-primary);
  font-weight: bold;
  display: inline-block;
  width: 1em;
  margin-left: -1em;
  position: absolute;
  left: 0;
}

.qa-answer {
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-light);
}

.qa-answer .answer-content {
  line-height: 1.6;
  text-align: justify;
  color: var(--el-text-color-primary);
  white-space: pre-wrap;
  padding: 10px;
  background-color: var(--el-fill-color-lighter);
  border-radius: 4px;
}

.programming-question {
  padding: 15px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background-color: var(--el-fill-color-light);
}

.programming-tag {
  background-color: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
}

.section {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px dashed var(--el-border-color-light);
}

.section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
  color: var(--el-color-primary);
}

.section-content {
  color: var(--el-text-color-secondary);
  line-height: 1.6;
  text-align: justify;
}

.code-block {
  background-color: var(--el-color-info-light-9);
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 0.9em;
  font-family: 'Consolas', 'Monaco', 'Andale Mono', 'Ubuntu Mono', 'Monaco', 'Menlo', 'Courier New', monospace;
  color: var(--el-text-color-primary);
  white-space: pre-wrap;
  word-break: break-all;
  word-wrap: break-word;
  border-left: 4px solid var(--el-color-primary);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.example-item {
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px dashed var(--el-border-color-light);
}

.example-header {
  font-size: 15px;
  font-weight: bold;
  margin-bottom: 10px;
  color: var(--el-color-primary);
}

.example-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.example-input, .example-output {
  flex: 1;
}

.example-label {
  font-weight: bold;
  margin-bottom: 5px;
  color: var(--el-color-info);
}

@media (min-width: 768px) {
  .example-content {
    flex-direction: row;
  }
}

/* --- 新增的做题界面样式 --- */
.homework-do-dialog .el-dialog__body {
  padding: 0;
}

.homework-do-container {
  display: flex;
  height: 75vh;
}

.question-nav {
  width: 180px;
  border-right: 1px solid var(--el-border-color-light);
  padding: 20px;
  display: flex;
  flex-direction: column;
}
.nav-header {
  font-weight: bold;
  margin-bottom: 15px;
}
.nav-dots {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.nav-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--el-border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}
.nav-dot:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}
.nav-dot.answered {
  background-color: var(--el-color-success-light-8);
  border-color: var(--el-color-success-light-5);
}
.nav-dot.active {
  background-color: var(--el-color-primary);
  color: #fff;
  border-color: var(--el-color-primary);
}


.question-display-area {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: var(--el-bg-color-page);
}

.question-card {
  border: none;
  box-shadow: none;
}

.options-container, .judge-options {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-top: 20px;
}
.option-item {
  width: 100%;
  margin-left: 0 !important;
  height: auto;
  padding: 15px;
  white-space: normal;
}
.judge-options .el-radio {
  width: 120px;
  justify-content: center;
}

.answer-textarea {
  margin-top: 20px;
}

.practice-result {
  margin-top: 20px;
  padding: 15px;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
}

.result-feedback {
  display: flex;
  align-items: center;
  gap: 20px;
}
.result-score {
  font-size: 18px;
  font-weight: bold;
}
.result-text {
  flex: 1;
}

.correct-answer-section {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed var(--el-border-color);
}

.homework-do-footer {
  display: flex;
  justify-content: center;
  width: 100%;
  padding: 10px 20px;
  border-top: 1px solid var(--el-border-color-light);
}
</style> 