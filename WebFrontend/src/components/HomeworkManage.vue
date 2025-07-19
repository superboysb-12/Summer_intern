<script setup>
import { ref, onMounted, reactive, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { Search, Plus, Edit, Delete, RefreshRight, Calendar, Upload, Bell, Document, Download, List, Finished, View, Select } from '@element-plus/icons-vue'
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

// 练习记录相关
const practiceRecordsVisible = ref(false)
const practiceRecordsList = ref([])
const practiceRecordsLoading = ref(false)
const currentPracticeHomework = ref(null)
const practiceStats = ref(null)
const practiceStatsLoading = ref(false)

// 作业题目管理
const homeworkQuestionsVisible = ref(false)
const homeworkQuestions = ref([])
const questionsLoading = ref(false)
const currentQuestionsHomework = ref(null)

// 题目选择对话框
const questionSelectDialogVisible = ref(false)
const availableQuestions = ref([])
const selectedQuestionIds = ref([])
const questionsSelectLoading = ref(false)
const questionSearchKeyword = ref('')
const tableRef = ref(null)

// 题目详情对话框
const questionDetailVisible = ref(false)
const currentQuestion = ref(null)
const questionTypes = {
  MULTIPLE_CHOICE: '选择题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题'
}

// 批量添加题目表单
const addQuestionsForm = reactive({
  questionIds: []
})

// 评分表单
const gradeDialogVisible = ref(false)
const currentSubmission = ref(null)
const gradeForm = reactive({
  score: null,
  feedback: ''
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

// 打开练习记录对话框
const openPracticeRecords = async (homework) => {
  currentPracticeHomework.value = homework
  practiceRecordsVisible.value = true
  await loadPracticeRecords(homework.id)
}

// 加载练习记录
const loadPracticeRecords = async (homeworkId) => {
  practiceRecordsLoading.value = true
  
  try {
    // 获取班级信息
    const classId = props.courseId // 假设课程ID就是班级ID，实际情况可能需要调整
    
    // 获取班级统计数据
    try {
      const statsResponse = await axios.get(`${BaseUrl}practice-records/stats/class/${classId}/homework/${homeworkId}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      
      if (statsResponse.data) {
        practiceStats.value = statsResponse.data
      }
    } catch (error) {
      console.error('获取统计数据失败:', error)
      // 不阻止加载记录，继续执行
    }
    
    // 获取所有学生的练习记录
    try {
      // 修复API路径: 使用student/homework双重筛选而不是直接homework
      const response = await axios.get(`${BaseUrl}practice-records/student/0/homework/${homeworkId}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      
      if (response.data) {
        practiceRecordsList.value = response.data
        
        // 为每个记录加载学生信息
        const studentIds = [...new Set(practiceRecordsList.value.map(record => record.studentId))]
        
        for (const studentId of studentIds) {
          try {
            const userResponse = await axios.get(`${BaseUrl}users/${studentId}`, {
              headers: { 'Authorization': `Bearer ${getToken()}` }
            })
            
            if (userResponse.data) {
              practiceRecordsList.value.forEach(record => {
                if (record.studentId === studentId) {
                  record.studentName = userResponse.data.username
                }
              })
            }
          } catch (error) {
            console.error(`获取学生 ${studentId} 信息失败:`, error)
          }
        }
      } else {
        practiceRecordsList.value = []
      }
    } catch (error) {
      console.error('获取练习记录失败:', error)
      practiceRecordsList.value = []
      ElMessage.error('获取练习记录失败: ' + (error.response?.data?.error || error.message))
    }
  } catch (error) {
    console.error('加载练习记录失败:', error)
    ElMessage.error('加载练习记录失败: ' + (error.response?.data?.error || error.message))
  } finally {
    practiceRecordsLoading.value = false
  }
}

// 打开题目管理对话框
const openHomeworkQuestions = async (homework) => {
  currentQuestionsHomework.value = homework
  homeworkQuestionsVisible.value = true
  await loadHomeworkQuestions(homework.id)
}

// 加载作业题目
const loadHomeworkQuestions = async (homeworkId) => {
  questionsLoading.value = true
  
  try {
    const response = await axios.get(`${BaseUrl}homework-questions/homework/${homeworkId}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    if (response.data) {
      homeworkQuestions.value = response.data

      // 加载每个题目的详细信息
      for (const question of homeworkQuestions.value) {
        try {
          if (question.questionId) {
            const questionResponse = await axios.get(`${BaseUrl}api/question-generator/${question.questionId}`, {
              headers: { 'Authorization': `Bearer ${getToken()}` }
            })
            
            if (questionResponse.data && questionResponse.data.success) {
              // 合并题目详情到当前记录
              const questionData = questionResponse.data
              question.title = questionData.query || 'No Title'
              question.content = questionData.questionJson || ''
              question.type = questionData.questionType || 'MULTIPLE_CHOICE'
              question.answer = questionData.answer || ''
            }
          }
        } catch (questionError) {
          console.error(`获取题目 ${question.questionId} 详情失败:`, questionError)
        }
      }
    } else {
      homeworkQuestions.value = []
    }
  } catch (error) {
    console.error('获取作业题目失败:', error)
    ElMessage.error('获取作业题目失败: ' + (error.response?.data?.error || error.message))
  } finally {
    questionsLoading.value = false
  }
}

// 打开题目详情对话框
const openQuestionDetail = (question) => {
  currentQuestion.value = {
    id: question.id,
    title: question.query || question.title || '',
    type: question.questionType || question.type || '',
    content: question.questionJson || question.content || '',
    answer: question.answer || ''
  }
  questionDetailVisible.value = true
}

// 从作业中删除题目
const removeQuestionFromHomework = async (homeworkId, questionId) => {
  ElMessageBox.confirm('确认从作业中移除此题目吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`${BaseUrl}homework-questions/homework/${homeworkId}/question/${questionId}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      
      ElMessage.success('题目已从作业中移除')
      await loadHomeworkQuestions(homeworkId)
    } catch (error) {
      console.error('移除题目失败:', error)
      ElMessage.error('移除题目失败: ' + (error.response?.data?.error || error.message))
    }
  }).catch(() => {})
}

// 调整题目顺序
const updateQuestionOrder = async (homeworkQuestion, newOrder) => {
  try {
    await axios.put(`${BaseUrl}homework-questions/${homeworkQuestion.id}/order`, 
      { orderIndex: newOrder },
      {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      }
    )
    
    ElMessage.success('题目顺序已更新')
    await loadHomeworkQuestions(homeworkQuestion.homeworkId)
  } catch (error) {
    console.error('更新题目顺序失败:', error)
    ElMessage.error('更新题目顺序失败: ' + (error.response?.data?.error || error.message))
  }
}

// 批量添加题目到作业
const batchAddQuestionsToHomework = async (homeworkId) => {
  if (!addQuestionsForm.questionIds || addQuestionsForm.questionIds.length === 0) {
    ElMessage.warning('请选择要添加的题目')
    return
  }
  
  try {
    await axios.post(`${BaseUrl}homework-questions/homework/${homeworkId}/batch`, 
      { questionIds: addQuestionsForm.questionIds },
      {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      }
    )
    
    ElMessage.success('题目已添加到作业')
    addQuestionsForm.questionIds = [] // 清空表单
    await loadHomeworkQuestions(homeworkId)
  } catch (error) {
    console.error('添加题目失败:', error)
    ElMessage.error('添加题目失败: ' + (error.response?.data?.error || error.message))
  }
}

// 打开添加题目对话框
const openAddQuestionDialog = async () => {
  if (!currentQuestionsHomework.value) {
    ElMessage.error('请先选择作业')
    return
  }
  
  questionSelectDialogVisible.value = true
  selectedQuestionIds.value = []
  await loadAvailableQuestions()
}

// 加载可用题目列表
const loadAvailableQuestions = async () => {
  questionsSelectLoading.value = true
  
  try {
    // 构建请求URL，支持搜索
    let url = `${BaseUrl}api/question-generator/list`
    if (questionSearchKeyword.value) {
      url = `${BaseUrl}api/question-generator/search?keyword=${encodeURIComponent(questionSearchKeyword.value)}`
    }
    
    const response = await axios.get(url, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    if (response.data && response.data.success) {
      availableQuestions.value = response.data.questions || []
      
      // 获取已添加的题目ID列表，用于标记已添加的题目
      const existingQuestionIds = new Set(homeworkQuestions.value.map(q => q.questionId))
      
      // 标记哪些题目已经被添加到作业中
      availableQuestions.value.forEach(question => {
        question.isAdded = existingQuestionIds.has(question.id)
      })

      // 设置表格引用，用于预选
      nextTick(() => {
        if (tableRef.value) {
          availableQuestions.value.forEach(question => {
            if (!existingQuestionIds.has(question.id)) {
              tableRef.value.toggleRowSelection(question, true)
            }
          })
        }
      })
    } else {
      availableQuestions.value = []
      ElMessage.warning(response.data?.message || '获取题目列表失败')
    }
  } catch (error) {
    console.error('获取题目列表失败:', error)
    ElMessage.error('获取题目列表失败: ' + (error.response?.data?.error || error.message))
  } finally {
    questionsSelectLoading.value = false
  }
}

// 搜索题目
const searchQuestions = () => {
  loadAvailableQuestions()
}

// 重置题目搜索
const resetQuestionSearch = () => {
  questionSearchKeyword.value = ''
  loadAvailableQuestions()
}

// 选择题目
const handleSelectionChange = (selection) => {
  selectedQuestionIds.value = selection.map(item => item.id)
}

// 提交选择的题目
const submitSelectedQuestions = async () => {
  if (selectedQuestionIds.value.length === 0) {
    ElMessage.warning('请至少选择一个题目')
    return
  }
  
  try {
    await axios.post(`${BaseUrl}homework-questions/homework/${currentQuestionsHomework.value.id}/batch`, 
      { questionIds: selectedQuestionIds.value },
      {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      }
    )
    
    ElMessage.success('题目已添加到作业')
    questionSelectDialogVisible.value = false
    await loadHomeworkQuestions(currentQuestionsHomework.value.id)
  } catch (error) {
    console.error('添加题目失败:', error)
    ElMessage.error('添加题目失败: ' + (error.response?.data?.error || error.message))
  }
}

// 为主观题调用AI评分
const scoreWithAI = async (practiceRecord) => {
  try {
    // 解析答案数据
    const answerData = JSON.parse(practiceRecord.answerData || '{}')
    const studentAnswer = answerData.answer || ''
    const questionContent = practiceRecord.content || practiceRecord.question?.content || '未知题目'
    const expectedAnswer = practiceRecord.answer || practiceRecord.question?.answer || '未知答案'
    
    // 准备评分提示词
    const scoringPrompt = `
你是一位公正、专业的评分助手。请根据以下信息，为学生的回答评分（0-100分）：

题目：${questionContent}

标准答案：${expectedAnswer}

学生回答：${studentAnswer}

请提供：
1. 分数（0-100）
2. 简短的评分理由
3. 改进建议

回复格式要求：
{
  "score": 分数,
  "feedback": "评分理由和改进建议"
}
`
    
    // 显示加载状态
    const loadingInstance = ElLoading.service({
      lock: true,
      text: 'AI评分中...',
      background: 'rgba(0, 0, 0, 0.7)'
    })
    
    try {
      // 调用DeepSeek API进行评分 - 使用与DeepSeekChat.vue相同的API模式
      const user = store.getUserInfo()
      
      if (!user?.id) {
        throw new Error('用户未登录或无法获取用户信息')
      }
      
      const response = await axios.post(`${BaseUrl}api/deepseek/conversation/user/${user.id}`, {
        messages: [
          { role: "system", content: "你是一位公正的评分助手，请严格按照格式要求回复。" },
          { role: "user", content: scoringPrompt }
        ]
      }, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      
      if (response.data && response.data.response && response.data.response.choices) {
        // 解析AI返回的评分结果
        try {
          const aiResponse = response.data.response.choices[0].message.content
          
          // 尝试从回复中提取JSON
          const jsonMatch = aiResponse.match(/\{[\s\S]*"score"[\s\S]*\}/m)
          let scoringResult
          
          if (jsonMatch) {
            scoringResult = JSON.parse(jsonMatch[0])
          } else {
            // 尝试解析整个回复
            try {
              scoringResult = JSON.parse(aiResponse)
            } catch (e) {
              throw new Error('无法从AI回复中提取JSON格式的评分结果')
            }
          }
          
          // 确保得到的分数在0-100范围内
          const score = Math.max(0, Math.min(100, Number(scoringResult.score) || 0))
          const feedback = scoringResult.feedback || '无评分反馈'
          
          // 更新练习记录
          await axios.put(`${BaseUrl}practice-records/${practiceRecord.id}`, 
            {
              score: score,
              feedback: feedback
            },
            {
              headers: { 'Authorization': `Bearer ${getToken()}` }
            }
          )
          
          ElMessage.success('AI评分完成')
          await loadPracticeRecords(currentPracticeHomework.value.id)
        } catch (e) {
          console.error('解析AI评分结果失败:', e)
          ElMessage.error('解析AI评分结果失败: ' + e.message)
        }
      } else {
        throw new Error('未收到有效的AI评分结果')
      }
    } finally {
      // 关闭加载状态
      loadingInstance.close()
    }
  } catch (error) {
    console.error('AI评分失败:', error)
    ElMessage.error('AI评分失败: ' + (error.response?.data?.error || error.message))
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
                  
                  <el-button type="danger" link :icon="Delete" @click="deleteHomework(scope.row.id)" v-if="isTeacher || isAdmin">删除</el-button>

                  <el-button 
                    type="success" 
                    link 
                    :icon="List" 
                    @click="openHomeworkQuestions(scope.row)"
                    v-if="isTeacher || isAdmin"
                  >
                    题目管理
                  </el-button>
                  
                  <el-button 
                    type="warning" 
                    link 
                    :icon="Finished" 
                    @click="openPracticeRecords(scope.row)"
                    v-if="isTeacher || isAdmin"
                  >
                    练习记录
                  </el-button>
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
    
    <!-- 练习记录对话框 -->
    <el-dialog
      v-model="practiceRecordsVisible"
      :title="`练习记录 - ${currentPracticeHomework?.title || '作业'}`"
      width="80%"
      append-to-body
      destroy-on-close
    >
      <div class="container" v-loading="practiceRecordsLoading">
        <div class="mb-lg" v-if="currentPracticeHomework">
          <h3 class="text-xl mb-sm">作业详情</h3>
          <div class="bg-light-secondary p-md radius-md">
            <p class="mb-sm"><strong>标题：</strong> {{ currentPracticeHomework.title }}</p>
            <p class="mb-sm"><strong>描述：</strong> {{ currentPracticeHomework.description || '无描述' }}</p>
            <p class="mb-sm"><strong>截止日期：</strong> {{ formatDate(currentPracticeHomework.dueDate) || '无截止日期' }}</p>
            <p class="mb-sm"><strong>状态：</strong> {{ getStatusDisplay(currentPracticeHomework.status).text }}</p>
          </div>
        </div>
        
        <div class="mb-lg">
          <h3 class="text-xl mb-md">练习记录 ({{ practiceRecordsList.length }})</h3>
          
          <div class="table-responsive">
            <el-table
              :data="practiceRecordsList"
              border
              style="width: 100%"
            >
              <el-table-column type="expand">
                <template #default="props">
                  <div class="bg-light-secondary p-md radius-md">
                    <div class="mb-lg" v-if="props.row.question">
                      <h4 class="text-md mb-sm text-secondary">题目详情</h4>
                      <div class="bg-light p-md radius-sm">
                        <p><strong>题目类型：</strong> {{ questionTypes[props.row.question.type] || props.row.question.type }}</p>
                        <p><strong>题目内容：</strong> {{ props.row.question.content }}</p>
                        <p v-if="props.row.question.type === 'FILL_BLANK' || props.row.question.type === 'SHORT_ANSWER'"><strong>标准答案：</strong> {{ props.row.question.answer }}</p>
                      </div>
                    </div>
                    
                    <div v-if="props.row.answerData">
                      <h4 class="text-md mb-sm text-secondary">学生答案</h4>
                      <div class="bg-light p-md radius-sm">
                        <p><strong>学生答案：</strong> {{ JSON.parse(props.row.answerData).answer || '无答案' }}</p>
                        <p v-if="props.row.answerData && JSON.parse(props.row.answerData).isLate"><strong>迟交：</strong> 是</p>
                      </div>
                    </div>
                    
                    <div v-if="props.row.score !== null">
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

                    <el-button 
                      type="success" 
                      link 
                      :icon="Finished" 
                      @click="scoreWithAI(scope.row)"
                      v-if="isTeacher || isAdmin"
                    >
                      调用AI评分
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
          
          <el-empty v-if="practiceRecordsList.length === 0 && !practiceRecordsLoading" description="暂无练习记录" />
        </div>
      </div>
      
      <template #footer>
        <div class="d-flex justify-end gap-sm">
          <el-button @click="practiceRecordsVisible = false">关闭</el-button>
          <el-button type="primary" @click="loadPracticeRecords(currentPracticeHomework.id)">刷新练习记录</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 题目管理对话框 -->
    <el-dialog
      v-model="homeworkQuestionsVisible"
      :title="`题目管理 - ${currentQuestionsHomework?.title || '作业'}`"
      width="80%"
      append-to-body
      destroy-on-close
    >
      <div class="container" v-loading="questionsLoading">
        <div class="mb-lg" v-if="currentQuestionsHomework">
          <h3 class="text-xl mb-sm">作业详情</h3>
          <div class="bg-light-secondary p-md radius-md">
            <p class="mb-sm"><strong>标题：</strong> {{ currentQuestionsHomework.title }}</p>
            <p class="mb-sm"><strong>描述：</strong> {{ currentQuestionsHomework.description || '无描述' }}</p>
            <p class="mb-sm"><strong>截止日期：</strong> {{ formatDate(currentQuestionsHomework.dueDate) || '无截止日期' }}</p>
            <p class="mb-sm"><strong>状态：</strong> {{ getStatusDisplay(currentQuestionsHomework.status).text }}</p>
          </div>
        </div>
        
        <div class="mb-lg">
          <h3 class="text-xl mb-md">题目列表 ({{ homeworkQuestions.length }})</h3>
          
          <div class="d-flex justify-end mb-md">
            <el-button type="primary" :icon="List" @click="openAddQuestionDialog">添加题目</el-button>
          </div>

          <div class="table-responsive">
            <el-table
              :data="homeworkQuestions"
              border
              style="width: 100%"
            >
              <el-table-column type="index" width="50" align="center" />
              <el-table-column prop="title" label="题目标题" min-width="150" show-overflow-tooltip />
              <el-table-column prop="type" label="题目类型" width="100" align="center">
                <template #default="scope">
                  {{ questionTypes[scope.row.type] || scope.row.type }}
                </template>
              </el-table-column>
              <el-table-column prop="content" label="题目内容" min-width="200" show-overflow-tooltip>
                <template #default="scope">
                  <div v-if="typeof scope.row.content === 'string'">{{ scope.row.content }}</div>
                  <div v-else>{{ JSON.stringify(scope.row.content) }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="answer" label="标准答案" min-width="150" show-overflow-tooltip />
              <el-table-column label="操作" width="200" align="center">
                <template #default="scope">
                  <div class="d-flex justify-center gap-sm">
                    <el-button 
                      type="primary" 
                      link 
                      :icon="Edit" 
                      @click="openQuestionDetail(scope.row)"
                      v-if="isTeacher || isAdmin"
                    >
                      编辑
                    </el-button>
                    <el-button 
                      type="danger" 
                      link 
                      :icon="Delete" 
                      @click="removeQuestionFromHomework(currentQuestionsHomework.id, scope.row.id)"
                      v-if="isTeacher || isAdmin"
                    >
                      删除
                    </el-button>
                    <el-button 
                      type="info" 
                      link 
                      :icon="View" 
                      @click="openQuestionDetail(scope.row)"
                    >
                      详情
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
          
          <el-empty v-if="homeworkQuestions.length === 0 && !questionsLoading" description="暂无题目" />
        </div>
      </div>
      
      <template #footer>
        <div class="d-flex justify-end gap-sm">
          <el-button @click="homeworkQuestionsVisible = false">关闭</el-button>
          <el-button type="primary" @click="openAddQuestionDialog">批量添加题目</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 题目详情对话框 -->
    <el-dialog
      v-model="questionDetailVisible"
      :title="`题目详情 - ${currentQuestion?.type || '未知题目'}`"
      width="600px"
      append-to-body
      destroy-on-close
    >
      <div v-if="currentQuestion">
        <div class="bg-light-secondary p-md radius-md mb-md">
          <p class="mb-sm"><strong>题目标题：</strong> {{ currentQuestion.title }}</p>
          <p class="mb-sm"><strong>题目类型：</strong> {{ questionTypes[currentQuestion.type] || currentQuestion.type }}</p>
          <p class="mb-sm"><strong>题目内容：</strong> 
            <span v-if="typeof currentQuestion.content === 'string'">{{ currentQuestion.content }}</span>
            <span v-else>{{ JSON.stringify(currentQuestion.content) }}</span>
          </p>
          <p v-if="currentQuestion.type === 'FILL_BLANK' || currentQuestion.type === 'SHORT_ANSWER'"><strong>标准答案：</strong> {{ currentQuestion.answer }}</p>
        </div>
        
        <el-form :model="currentQuestion" label-width="100px">
          <el-form-item label="题目类型">
            <el-select v-model="currentQuestion.type" placeholder="请选择类型" style="width: 100%">
              <el-option
                v-for="(label, key) in questionTypes"
                :key="key"
                :label="label"
                :value="key"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="题目内容" prop="content">
            <el-input
              v-model="currentQuestion.content"
              type="textarea"
              :rows="4"
              placeholder="请输入题目内容"
            />
          </el-form-item>

          <el-form-item label="标准答案" prop="answer">
            <el-input
              v-model="currentQuestion.answer"
              type="textarea"
              :rows="4"
              placeholder="请输入标准答案"
            />
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <div class="d-flex justify-end gap-sm">
          <el-button @click="questionDetailVisible = false">取消</el-button>
          <el-button type="primary" @click="updateQuestionOrder(currentQuestion, homeworkQuestions.indexOf(currentQuestion))">保存顺序</el-button>
          <el-button type="primary" @click="updateQuestionOrder(currentQuestion, homeworkQuestions.indexOf(currentQuestion) - 1)">上移</el-button>
          <el-button type="primary" @click="updateQuestionOrder(currentQuestion, homeworkQuestions.indexOf(currentQuestion) + 1)">下移</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 题目选择对话框 -->
    <el-dialog
      v-model="questionSelectDialogVisible"
      :title="`选择题目 - ${currentQuestionsHomework?.title || '作业'}`"
      width="80%"
      append-to-body
      destroy-on-close
    >
      <div class="d-flex justify-between mb-md">
        <el-input
          v-model="questionSearchKeyword"
          placeholder="搜索题目"
          :prefix-icon="Search"
          style="width: 300px"
          @keyup.enter="searchQuestions"
        />
        <div class="d-flex gap-sm">
          <el-button @click="resetQuestionSearch">重置</el-button>
          <el-button type="primary" @click="searchQuestions">搜索</el-button>
        </div>
      </div>

      <el-table
        ref="tableRef"
        v-loading="questionsSelectLoading"
        :data="availableQuestions"
        border
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" width="50" align="center" />
        <el-table-column prop="query" label="题目标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="questionType" label="题目类型" width="100" align="center">
          <template #default="scope">
            {{ questionTypes[scope.row.questionType] || scope.row.questionType }}
          </template>
        </el-table-column>
        <el-table-column label="题目内容" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <div v-if="typeof scope.row.questionJson === 'string'">{{ scope.row.questionJson }}</div>
            <div v-else>{{ JSON.stringify(scope.row.questionJson) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag type="success" v-if="scope.row.isAdded">已添加</el-tag>
            <el-tag type="info" v-else>未添加</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="scope">
            <el-button 
              type="primary" 
              link 
              :icon="View" 
              @click="openQuestionDetail(scope.row)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <div class="d-flex justify-end gap-sm">
          <el-button @click="questionSelectDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitSelectedQuestions">确定添加</el-button>
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