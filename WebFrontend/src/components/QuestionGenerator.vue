<script setup>
import { ref, onMounted, reactive, watch, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download, RefreshRight, Document, Edit, Histogram, Delete, Timer } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'
import { useRouter } from 'vue-router'
// 移除marked导入，改用CDN方式
import { copyToClipboard } from '../utils/clipboard'

const router = useRouter()
const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 题目列表数据
const questionList = ref([])
const loading = ref(false)
const total = ref(0)

// RAG列表数据
const ragList = ref([])
const loadingRag = ref(false)

// 题目类型数据
const questionTypes = ref([])
const loadingTypes = ref(false)

// 表单引用
const generateFormRef = ref(null)

// 分页参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

// 提交表单数据
const generateForm = reactive({
  query: '',
  useRag: false,
  ragId: null,
  ragName: '',
  questionType: ''
})

// 对话框控制
const generateDialogVisible = ref(false)
const detailDialogVisible = ref(false)

// 当前查看的题目
const currentQuestion = ref(null)

// 自动刷新状态相关
const refreshInterval = ref(null)
const autoRefreshEnabled = ref(true)
const refreshLoading = ref(false)

// 表单校验规则
const generateRules = {
  query: [
    { required: true, message: '请输入检索词', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  ragId: [
    { required: true, message: '请选择RAG', trigger: 'change', type: 'number' }
  ],
  ragName: [
    { required: true, message: '请输入RAG名称', trigger: 'blur' }
  ]
}

// 获取题目列表
const getQuestionList = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${BaseUrl}api/question-generator/list`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      questionList.value = response.data.questions || []
      total.value = response.data.total || 0
      
      // 如果有关键词，前端过滤
      if (queryParams.keyword) {
        const keyword = queryParams.keyword.toLowerCase()
        questionList.value = questionList.value.filter(question => 
          (question.query && question.query.toLowerCase().includes(keyword))
        )
        total.value = questionList.value.length
      }
    } else {
      ElMessage.error(response.data?.message || '获取题目列表失败')
    }
  } catch (error) {
    console.error('获取题目列表失败:', error)
    ElMessage.error('获取题目列表失败')
  } finally {
    loading.value = false
  }
}

// 获取RAG列表
const getRAGList = async () => {
  loadingRag.value = true
  try {
    const response = await axios.get(`${BaseUrl}api/rag`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      ragList.value = response.data.data || []
    } else {
      ElMessage.error(response.data?.message || '获取RAG列表失败')
    }
  } catch (error) {
    console.error('获取RAG列表失败:', error)
    ElMessage.error('获取RAG列表失败')
  } finally {
    loadingRag.value = false
  }
}

// 获取题目类型
const getQuestionTypes = async () => {
  loadingTypes.value = true
  try {
    const response = await axios.get(`${BaseUrl}api/question-generator/types`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      questionTypes.value = response.data.types || []
    } else {
      ElMessage.error(response.data?.message || '获取题目类型失败')
    }
  } catch (error) {
    console.error('获取题目类型失败:', error)
    ElMessage.error('获取题目类型失败')
  } finally {
    loadingTypes.value = false
  }
}

// 打开生成题目对话框
const handleGenerate = () => {
  resetGenerateForm()
  getRAGList() // 获取RAG列表
  getQuestionTypes() // 获取题目类型
  generateDialogVisible.value = true
}

// 重置生成表单
const resetGenerateForm = () => {
  generateForm.query = ''
  generateForm.useRag = false
  generateForm.ragId = null
  generateForm.ragName = ''
  generateForm.questionType = ''
}

// 提交生成题目表单
const submitGenerateForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        let url
        let requestData
        
        // 根据选择的RAG方式决定调用的API
        if (generateForm.useRag) {
          if (generateForm.ragId) {
            // 使用RAG ID生成
            url = `${BaseUrl}api/question-generator/generate-with-rag`
            requestData = {
              query: generateForm.query,
              ragId: generateForm.ragId,
              questionType: generateForm.questionType || undefined
            }
          } else if (generateForm.ragName) {
            // 使用RAG名称生成
            url = `${BaseUrl}api/question-generator/generate-with-rag-name`
            requestData = {
              query: generateForm.query,
              ragName: generateForm.ragName,
              questionType: generateForm.questionType || undefined
            }
          } else {
            ElMessage.warning('请选择RAG ID或输入RAG名称')
            return
          }
        } else {
          // 不使用RAG
          url = `${BaseUrl}api/question-generator/generate`
          requestData = {
            query: generateForm.query,
            questionType: generateForm.questionType || undefined
          }
        }
        
        const response = await axios.post(url, requestData, {
          headers: {
            'Authorization': `Bearer ${getToken()}`
          }
        })
        
        if (response.data && response.data.success) {
          ElMessage.success('题目生成任务已提交')
          generateDialogVisible.value = false
          getQuestionList() // 刷新列表
        } else {
          ElMessage.error(response.data?.message || '提交题目生成任务失败')
        }
      } catch (error) {
        console.error('提交题目生成任务失败:', error)
        ElMessage.error(error.response?.data?.message || '提交题目生成任务失败')
      }
    }
  })
}

// 查看题目详情
const handleViewDetail = async (row) => {
  try {
    const response = await axios.get(`${BaseUrl}api/question-generator/${row.id}`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      currentQuestion.value = response.data
      detailDialogVisible.value = true
    } else {
      ElMessage.error(response.data?.message || '获取题目详情失败')
    }
  } catch (error) {
    console.error('获取题目详情失败:', error)
    ElMessage.error('获取题目详情失败')
  }
}



// 刷新单个题目状态
const refreshQuestionStatus = async (id) => {
  try {
    const response = await axios.get(`${BaseUrl}api/question-generator/${id}`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      const index = questionList.value.findIndex(item => item.id === id)
      if (index !== -1) {
        questionList.value[index] = {
          ...questionList.value[index],
          ...response.data
        }
      }
    }
  } catch (error) {
    console.error(`刷新题目 ${id} 状态失败:`, error)
  }
}

// 刷新所有PENDING状态的题目
const refreshPendingQuestions = async () => {
  if (refreshLoading.value) return
  
  refreshLoading.value = true
  try {
    const pendingQuestions = questionList.value.filter(q => q.status === 'PENDING')
    for (const question of pendingQuestions) {
      await refreshQuestionStatus(question.id)
    }
  } finally {
    refreshLoading.value = false
  }
}

// 初始化自动刷新
const initAutoRefresh = () => {
  if (refreshInterval.value) {
    clearInterval(refreshInterval.value)
  }
  
  if (autoRefreshEnabled.value) {
    refreshInterval.value = setInterval(refreshPendingQuestions, 5000)
  }
}

// 切换自动刷新状态
const toggleAutoRefresh = () => {
  autoRefreshEnabled.value = !autoRefreshEnabled.value
  initAutoRefresh()
}

// 手动刷新列表
const handleRefresh = () => {
  getQuestionList()
}

// 监听页码变化
const handlePageChange = (page) => {
  queryParams.pageNum = page
  getQuestionList()
}

// 处理搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  getQuestionList()
}

// 重置搜索条件
const resetQuery = () => {
  queryParams.pageNum = 1
  queryParams.keyword = ''
  getQuestionList()
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

// 格式化题目类型
const formatQuestionType = (type) => {
  switch (type) {
    case '选择题': return '选择题'
    case '判断题': return '判断题'
    case '问答题': return '问答题'
    case '编程题': return '编程题'
    default: return type
  }
}

// 状态标签的类型
const statusTypeMap = {
  'PENDING': 'warning',
  'COMPLETED': 'success',
  'FAILED': 'danger'
}

// 格式化JSON显示
const formatJson = (json) => {
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch (e) {
    return json
  }
}

// 添加格式化题目函数
const renderQuestion = (questionJson) => {
  try {
    if (!questionJson) return '';
    
    const question = JSON.parse(questionJson);
    
    // 如果不是有效的题目JSON，直接返回格式化的原始内容
    if (!question.type) {
      return formatJson(questionJson);
    }
    
    return question; // 返回解析后的对象，而不是格式化的字符串
  } catch (e) {
    return formatJson(questionJson);
  }
};

// 格式化解析文本，处理换行符
const formatExplanation = (text) => {
  if (!text) return '';
  return text.replace(/\n/g, '<br>');
};

// 在detailDialogVisible变量后添加以下内容
const solutionDialogVisible = ref(false);
const solutionLoading = ref(false);
const currentSolution = ref('');

// 获取题目解答
const handleGetSolution = async (row) => {
  if (!row || row.status !== 'COMPLETED') {
    ElMessage.warning('只能为已完成生成的题目生成解答');
    return;
  }
  
  try {
    solutionLoading.value = true;
    currentQuestion.value = row;
    
    const response = await axios.get(`${BaseUrl}api/question-generator/${row.id}/solution`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    });
    
    if (response.data && response.data.success) {
      currentSolution.value = response.data.solution || '解答生成成功，但内容为空';
      solutionDialogVisible.value = true;
    } else {
      ElMessage.error(response.data?.message || '获取题目解答失败');
    }
  } catch (error) {
    console.error('获取题目解答失败:', error);
    ElMessage.error('获取题目解答失败: ' + (error.response?.data?.message || error.message));
  } finally {
    solutionLoading.value = false;
  }
};

// 批量删除题目
const selectedQuestions = ref([]);

const handleSelectionChange = (selection) => {
  selectedQuestions.value = selection;
};

const handleBatchDelete = async () => {
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请先选择要删除的题目');
    return;
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的${selectedQuestions.value.length}个题目吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    const ids = selectedQuestions.value.map(item => item.id);
    const response = await axios.delete(`${BaseUrl}api/question-generator/batch`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      },
      data: { ids }
    });
    
    if (response.data && response.data.success) {
      ElMessage.success(response.data.message || `成功删除${response.data.deletedCount}个题目`);
      getQuestionList(); // 刷新列表
    } else {
      ElMessage.error(response.data?.message || '批量删除题目失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除题目失败:', error);
      ElMessage.error('批量删除题目失败: ' + (error.response?.data?.message || error.message));
    }
  }
};

// 添加 markdownToHtml 和 handleCopySolution 方法
const markdownToHtml = (markdown) => {
  if (!markdown) return '';
  // 检查window.marked是否可用
  if (typeof window.marked === 'undefined') {
    // 如果marked不可用，简单返回原始文本并添加基本HTML格式
    return markdown
      .replace(/\n/g, '<br>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>');
  }
  return window.marked.parse(markdown);
};

const handleCopySolution = () => {
  if (currentSolution.value) {
    copyToClipboard(currentSolution.value)
      .then(() => {
        ElMessage.success('解答内容已复制到剪贴板');
      })
      .catch(err => {
        console.error('复制失败:', err);
        ElMessage.error('复制失败，请手动复制');
      });
  }
};

// 添加设计相关的状态
const designDialogVisible = ref(false)
const designEfficiencyDialogVisible = ref(false)
const optimizationDialogVisible = ref(false)
const designContent = ref('')
const designLoading = ref(false)
const saveDesignLoading = ref(false)
const finishDesignLoading = ref(false)
const currentEfficiencyData = ref(null)
const currentOptimizationData = ref(null)
const optimizationLoading = ref(false)

// 开始设计课后练习
const handleStartDesign = async (row) => {
  try {
    if (row.status !== 'COMPLETED') {
      ElMessage.warning('只能为已完成生成的题目进行设计')
      return
    }
    
    designLoading.value = true
    currentQuestion.value = row
    
    const response = await axios.get(`${BaseUrl}api/question-generator/${row.id}/design`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      designContent.value = response.data.content || ''
      designDialogVisible.value = true
    } else {
      ElMessage.error(response.data?.message || '开始设计失败')
    }
  } catch (error) {
    console.error('开始设计失败:', error)
    ElMessage.error('开始设计失败: ' + (error.response?.data?.message || error.message))
  } finally {
    designLoading.value = false
  }
}

// 保存设计内容
const handleSaveDesign = async () => {
  try {
    if (!currentQuestion.value || !currentQuestion.value.id) {
      ElMessage.warning('找不到当前题目')
      return
    }
    
    saveDesignLoading.value = true
    
    const response = await axios.post(`${BaseUrl}api/question-generator/${currentQuestion.value.id}/design/save`, {
      content: designContent.value
    }, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      ElMessage.success('设计内容已保存')
    } else {
      ElMessage.error(response.data?.message || '保存设计内容失败')
    }
  } catch (error) {
    console.error('保存设计内容失败:', error)
    ElMessage.error('保存设计内容失败: ' + (error.response?.data?.message || error.message))
  } finally {
    saveDesignLoading.value = false
  }
}

// 完成设计并计算效率指数
const handleFinishDesign = async () => {
  try {
    if (!currentQuestion.value || !currentQuestion.value.id) {
      ElMessage.warning('找不到当前题目')
      return
    }
    
    // 先保存最新内容
    await handleSaveDesign()
    
    finishDesignLoading.value = true
    
    const response = await axios.post(`${BaseUrl}api/question-generator/${currentQuestion.value.id}/design/finish`, {}, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      ElMessage.success('课后练习设计已完成')
      designDialogVisible.value = false
      
      // 显示效率数据
      currentEfficiencyData.value = response.data
      designEfficiencyDialogVisible.value = true
      
      // 刷新列表
      getQuestionList()
    } else {
      ElMessage.error(response.data?.message || '完成设计失败')
    }
  } catch (error) {
    console.error('完成设计失败:', error)
    ElMessage.error('完成设计失败: ' + (error.response?.data?.message || error.message))
  } finally {
    finishDesignLoading.value = false
  }
}

// 获取效率统计数据
const handleViewEfficiencyStatistics = async () => {
  try {
    const response = await axios.get(`${BaseUrl}api/question-generator/design/efficiency/statistics`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      if (!response.data.hasData) {
        ElMessage.info('暂无效率统计数据')
        return
      }
      
      currentEfficiencyData.value = response.data
      designEfficiencyDialogVisible.value = true
    } else {
      ElMessage.error(response.data?.message || '获取效率统计数据失败')
    }
  } catch (error) {
    console.error('获取效率统计数据失败:', error)
    ElMessage.error('获取效率统计数据失败: ' + (error.response?.data?.message || error.message))
  }
}

// 查看优化建议
const handleViewOptimization = async (row) => {
  try {
    if (!row || !row.id) {
      ElMessage.warning('找不到题目信息')
      return
    }
    
    // 设置加载状态
    optimizationLoading.value = true
    optimizationDialogVisible.value = true
    currentOptimizationData.value = { 
      query: row.query, 
      questionType: row.questionType,
      efficiencyIndex: row.efficiencyIndex,
      designDuration: row.designDuration
    }
    
    // 获取基本优化建议
    try {
    const response = await axios.get(`${BaseUrl}api/question-generator/${row.id}/optimization`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
        currentOptimizationData.value = {
          ...currentOptimizationData.value,
          ...response.data
        }
    } else {
        ElMessage.error(response.data?.message || '获取基础优化建议失败')
      }
    } catch (error) {
      console.error('获取基础优化建议失败:', error)
      ElMessage.error('获取基础优化建议失败: ' + (error.response?.data?.message || error.message))
    }
    
    // 使用大模型生成针对性建议
    if (currentOptimizationData.value.query && currentOptimizationData.value.questionType) {
      try {
        // 构建系统提示词和用户消息
        const systemPrompt = `你是一位专业的教育教学顾问，专长于提供针对性的课后练习优化建议。
请基于以下信息，提供具体、实用的优化建议：
- 题目关键词: ${currentOptimizationData.value.query}
- 题目类型: ${currentOptimizationData.value.questionType}
- 效率指数: ${currentOptimizationData.value.efficiencyIndex}
- 设计时长: ${formatDuration(currentOptimizationData.value.designDuration)}

请严格按照以下JSON格式输出你的建议，不要包含任何其他文本：

{
  "summary": "整体优化方向的简短总结，不超过50字",
  "categories": [
    {
      "name": "教学目标",
      "score": 分数（1-10的整数，表示当前表现，10为最佳）,
      "strengths": ["优势点1", "优势点2"],
      "weaknesses": ["不足点1", "不足点2"],
      "suggestions": ["具体建议1", "具体建议2"]
    },
    {
      "name": "内容设计",
      "score": 分数,
      "strengths": ["优势点1", "优势点2"],
      "weaknesses": ["不足点1", "不足点2"],
      "suggestions": ["具体建议1", "具体建议2"]
    },
    {
      "name": "难度梯度",
      "score": 分数,
      "strengths": ["优势点1", "优势点2"],
      "weaknesses": ["不足点1", "不足点2"],
      "suggestions": ["具体建议1", "具体建议2"]
    },
    {
      "name": "学生参与度",
      "score": 分数,
      "strengths": ["优势点1", "优势点2"],
      "weaknesses": ["不足点1", "不足点2"],
      "suggestions": ["具体建议1", "具体建议2"]
    },
    {
      "name": "创新性",
      "score": 分数,
      "strengths": ["优势点1", "优势点2"],
      "weaknesses": ["不足点1", "不足点2"],
      "suggestions": ["具体建议1", "具体建议2"]
    }
  ],
  "overall_suggestion": "综合性建议，100-200字"
}

必须确保输出是有效的JSON格式，可以直接被JSON.parse()解析。`;

        const userMessage = `请为"${currentOptimizationData.value.query}"这道${currentOptimizationData.value.questionType}生成结构化的优化建议JSON。`;

        // 准备要发送到API的消息
        const apiMessages = [
          { role: "system", content: systemPrompt },
          { role: "user", content: userMessage }
        ];

        // 直接调用DeepSeekChat API
        const aiResponse = await axios.post(`${BaseUrl}api/deepseek/chat`, 
          { messages: apiMessages },
          { headers: { 'Authorization': `Bearer ${getToken()}` } }
        );
                  
          if (aiResponse.data && aiResponse.data.choices && aiResponse.data.choices.length > 0) {
            const aiMessage = aiResponse.data.choices[0].message;
            try {
              // 获取内容
              let contentToProcess = aiMessage.content;
              
              // 处理可能带有的Markdown代码块
              // 首先检查是否包含```json标记的代码块
              const jsonBlockRegex = /```json\s*([\s\S]*?)\s*```/;
              const markdownMatch = jsonBlockRegex.exec(contentToProcess);
              
              if (markdownMatch && markdownMatch[1]) {
                // 如果找到了json代码块，则提取其中的内容
                contentToProcess = markdownMatch[1].trim();
                console.log('从Markdown代码块提取JSON:', contentToProcess);
              }
              
              // 尝试解析JSON响应
              currentOptimizationData.value.structuredSuggestions = JSON.parse(contentToProcess);
              // 保留原始文本以备后用
              currentOptimizationData.value.additionalSuggestions = aiMessage.content;
            } catch (jsonError) {
              console.error('JSON解析失败:', jsonError);
              currentOptimizationData.value.additionalSuggestions = aiMessage.content;
              currentOptimizationData.value.jsonParseError = true;
            }
          } else {
            currentOptimizationData.value.additionalSuggestions = '无法获取优化建议，请稍后再试';
          }
      } catch (aiError) {
        console.error('获取AI针对性建议失败:', aiError);
        currentOptimizationData.value.additionalSuggestions = '获取AI针对性建议失败，请稍后再试';
      }
    }
  } catch (error) {
    console.error('获取优化建议失败:', error)
    ElMessage.error('获取优化建议失败: ' + (error.response?.data?.message || error.message))
    // 如果在对话框已打开的情况下出错，则关闭对话框
    if (optimizationDialogVisible.value) {
      optimizationDialogVisible.value = false
    }
  } finally {
    // 无论成功失败，都关闭加载状态
    optimizationLoading.value = false
  }
}

// 格式化时长显示
const formatDuration = (seconds) => {
  if (!seconds && seconds !== 0) return '未知'
  
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainingSeconds = seconds % 60
  
  let result = ''
  if (hours > 0) {
    result += `${hours}小时`
  }
  if (minutes > 0) {
    result += `${minutes}分钟`
  }
  if (remainingSeconds > 0 || result === '') {
    result += `${remainingSeconds}秒`
  }
  
  return result
}

// 效率指数颜色
const getEfficiencyColor = (index) => {
  if (!index && index !== 0) return ''
  
  if (index >= 90) return 'success'
  if (index >= 70) return 'primary'
  if (index >= 50) return 'warning'
  return 'danger'
}

// 生命周期钩子
onMounted(() => {
  getQuestionList()
  initAutoRefresh()
})

onBeforeUnmount(() => {
  if (refreshInterval.value) {
    clearInterval(refreshInterval.value)
  }
})

// 监听查询参数变化
watch(
  () => [queryParams.pageNum, queryParams.pageSize],
  () => {
    getQuestionList()
  }
)

// 练习相关的状态变量
const practiceDialogVisible = ref(false)
const practiceLoading = ref(false)
const currentPractice = ref(null)
const userAnswer = ref('')
const practiceResult = ref(null)
const practiceGrading = ref(false)

// 处理练习题目
const handlePractice = (row) => {
  if (!row || row.status !== 'COMPLETED') {
    ElMessage.warning('只能练习已完成生成的题目')
    return
  }
  
  try {
    practiceLoading.value = true
    currentPractice.value = null
    userAnswer.value = ''
    practiceResult.value = null
    
    // 获取题目详情
    axios.get(`${BaseUrl}api/question-generator/${row.id}`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    }).then(response => {
      if (response.data && response.data.success) {
        currentPractice.value = response.data
        practiceDialogVisible.value = true
      } else {
        ElMessage.error(response.data?.message || '获取题目详情失败')
      }
    }).catch(error => {
      console.error('获取题目详情失败:', error)
      ElMessage.error('获取题目详情失败: ' + (error.response?.data?.message || error.message))
    }).finally(() => {
      practiceLoading.value = false
    })
  } catch (error) {
    practiceLoading.value = false
    console.error('处理练习失败:', error)
    ElMessage.error('处理练习失败: ' + error.message)
  }
}

// 提交练习答案
const submitPractice = async () => {
  if (!currentPractice.value) return
  
  const question = renderQuestion(currentPractice.value.questionJson)
  if (!question) return
  
  const questionType = question.type
  
  // 验证用户是否提供了答案
  if (!userAnswer.value || userAnswer.value.trim() === '') {
    ElMessage.warning('请先输入答案')
    return
  }
  
  practiceGrading.value = true
  practiceResult.value = null
  
  try {
    // 根据题型判断答案
    if (questionType === '选择题') {
      // 选择题：直接比较答案
      const isCorrect = userAnswer.value.trim().toUpperCase() === question.answer.trim().toUpperCase()
      practiceResult.value = {
        correct: isCorrect,
        score: isCorrect ? 100 : 0,
        feedback: isCorrect ? 
          '回答正确！' : 
          `回答错误。正确答案是: ${question.answer}${question.explanation ? '\n解析: ' + question.explanation : ''}`
      }
    } else if (questionType === '判断题') {
      // 判断题：直接比较答案
      const userChoice = userAnswer.value.trim()
      const correctAnswer = question.answer.trim()
      const isCorrect = (userChoice === '正确' || userChoice === '对' || userChoice === 'true' || userChoice === 'True') && correctAnswer === '正确' ||
                       (userChoice === '错误' || userChoice === '错' || userChoice === 'false' || userChoice === 'False') && correctAnswer === '错误'
      
      practiceResult.value = {
        correct: isCorrect,
        score: isCorrect ? 100 : 0,
        feedback: isCorrect ? 
          '回答正确！' : 
          `回答错误。正确答案是: ${question.answer}${question.explanation ? '\n解析: ' + question.explanation : ''}`
      }
    } else if (questionType === '问答题' || questionType === '编程题') {
      // 问答题或编程题：使用大模型API进行评估
      await gradeWithAI(questionType, question)
    } else {
      ElMessage.warning('不支持的题目类型')
      practiceGrading.value = false
      return
    }
  } catch (error) {
    console.error('提交答案出错:', error)
    ElMessage.error('提交答案失败: ' + (error.response?.data?.message || error.message))
  } finally {
    practiceGrading.value = false
  }
}

// 使用大模型API进行答案评分
const gradeWithAI = async (questionType, question) => {
  try {
    // 构建系统提示词
    let systemPrompt
    
    if (questionType === '问答题') {
      systemPrompt = `你是一位专业的教育评分助手，负责评判学生的问答题回答。
请基于以下信息，给出评分和详细反馈：
- 问题: ${question.question}
- 关键点: ${JSON.stringify(question.key_points || [])}
- 参考答案: ${question.answer || '无'}
- 学生回答: ${userAnswer.value}

请严格按照以下JSON格式输出你的评分，不要包含任何其他文本：
{
  "score": 分数（0-100的整数，表示得分），
  "feedback": "详细反馈，包括优点和不足，以及改进建议",
  "keyPointsCovered": ["学生回答中涵盖的关键点列表"],
  "keyPointsMissed": ["学生回答中缺失的关键点列表"]
}

评分标准：
- 90-100分：回答全面准确，涵盖了所有关键点，表述清晰逻辑严密
- 75-89分：回答较为准确，涵盖了大部分关键点，可能存在小的不足
- 60-74分：回答基本准确，涵盖了部分关键点，但有明显不足
- 40-59分：回答部分正确，但关键点覆盖不足，存在概念混淆或错误
- 0-39分：回答大部分不正确，几乎没有涵盖关键点，存在严重错误

请确保输出是有效的JSON格式，可以直接被JSON.parse()解析。`

    } else if (questionType === '编程题') {
      systemPrompt = `你是一位专业的编程教育评分助手，负责评判学生的编程题解答。
请基于以下信息，给出评分和详细反馈：
- 题目: ${question.title || ''}
- 题目描述: ${question.description || ''}
- 参考代码: ${question.reference_code || '无'}
- 学生代码: ${userAnswer.value}

请严格按照以下JSON格式输出你的评分，不要包含任何其他文本：
{
  "score": 分数（0-100的整数，表示得分），
  "feedback": "详细反馈，包括优点和不足，以及改进建议",
  "correctness": "代码正确性分析",
  "efficiency": "代码效率分析",
  "readability": "代码可读性分析"
}

评分标准：
- 90-100分：代码完全正确，高效，具有良好的可读性和适当的注释
- 75-89分：代码基本正确，比较高效，可读性好
- 60-74分：代码能够解决问题，但效率不高或可读性一般
- 40-59分：代码有部分错误，但整体思路基本正确
- 0-39分：代码存在严重错误，无法解决问题

请确保输出是有效的JSON格式，可以直接被JSON.parse()解析。`
    }

    // 用户消息
    const userMessage = `请评分这个${questionType === '问答题' ? '问答题回答' : '编程题解答'}`

    // 准备API调用的消息
    const apiMessages = [
      { role: "system", content: systemPrompt },
      { role: "user", content: userMessage }
    ]

    // 调用DeepSeekChat API
    const aiResponse = await axios.post(`${BaseUrl}api/deepseek/chat`, 
      { messages: apiMessages },
      { headers: { 'Authorization': `Bearer ${getToken()}` } }
    )
              
    if (aiResponse.data && aiResponse.data.choices && aiResponse.data.choices.length > 0) {
      const aiMessage = aiResponse.data.choices[0].message
      try {
        // 获取内容
        let contentToProcess = aiMessage.content
        
        // 处理可能带有的Markdown代码块
        const jsonBlockRegex = /```json\s*([\s\S]*?)\s*```/
        const markdownMatch = jsonBlockRegex.exec(contentToProcess)
        
        if (markdownMatch && markdownMatch[1]) {
          // 如果找到了json代码块，则提取其中的内容
          contentToProcess = markdownMatch[1].trim()
        }
        
        // 解析JSON响应
        const gradingResult = JSON.parse(contentToProcess)
        
        // 设置结果
        practiceResult.value = {
          ...gradingResult,
          aiGenerated: true
        }
        
      } catch (jsonError) {
        console.error('JSON解析失败:', jsonError)
        practiceResult.value = {
          score: 0,
          feedback: '评分系统出现错误，无法解析结果。原始响应: ' + aiMessage.content,
          error: true
        }
      }
    } else {
      practiceResult.value = {
        score: 0,
        feedback: '评分系统出现错误，无法获取评分结果。',
        error: true
      }
    }
  } catch (error) {
    console.error('AI评分失败:', error)
    practiceResult.value = {
      score: 0,
      feedback: '评分系统出现错误: ' + (error.message || '未知错误'),
      error: true
    }
  }
}

// 重置练习
const resetPractice = () => {
  userAnswer.value = ''
  practiceResult.value = null
}

// 根据分数返回颜色
const getScoreColor = (score) => {
  if (score >= 90) return '#67C23A' // 成功
  if (score >= 75) return '#409EFF' // 主色
  if (score >= 60) return '#E6A23C' // 警告
  return '#F56C6C' // 危险
}

// 根据分数返回等级
const getScoreLevel = (score) => {
  if (score >= 90) return '优秀'
  if (score >= 75) return '良好'
  if (score >= 60) return '及格'
  if (score >= 40) return '需要改进'
  return '不及格'
}
</script>

<template>
  <div class="question-generator-container">
    <!-- 现有工具栏 -->
    <div class="toolbar">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item>
          <el-input v-model="queryParams.keyword" placeholder="请输入检索词" clearable @keyup.enter="handleSearch">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      
      <div class="toolbar-right">
        <el-button type="success" plain @click="handleGenerate" :icon="Plus">生成题目</el-button>
        <el-button type="danger" plain @click="handleBatchDelete" :disabled="!selectedQuestions.length" :icon="Delete">批量删除</el-button>
        <el-button type="primary" plain @click="handleRefresh" :icon="RefreshRight" :loading="loading">刷新</el-button>
        <el-button type="info" plain @click="handleViewEfficiencyStatistics" :icon="Histogram">效率统计</el-button>
        <el-switch
          v-model="autoRefreshEnabled"
          active-text="自动刷新"
          @change="toggleAutoRefresh"
        />
      </div>
    </div>
    
    <!-- 现有表格 -->
    <el-table
      v-loading="loading"
      :data="questionList"
      style="width: 100%; margin-top: 10px; margin-bottom: 10px"
      border
      stripe
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="检索词" prop="query" min-width="150" show-overflow-tooltip />
      <el-table-column label="题目类型" prop="questionType" width="100">
        <template #default="scope">
          {{ formatQuestionType(scope.row.questionType) }}
        </template>
      </el-table-column>
      <el-table-column label="RAG名称" prop="ragName" width="150" show-overflow-tooltip />
      <el-table-column label="状态" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="statusTypeMap[scope.row.status]" effect="plain">
            {{ formatStatus(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="效率指数" prop="efficiencyIndex" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.efficiencyIndex" :type="getEfficiencyColor(scope.row.efficiencyIndex)" effect="light">
            {{ scope.row.efficiencyIndex?.toFixed(2) || '未计算' }}
          </el-tag>
          <span v-else>未计算</span>
        </template>
      </el-table-column>
      <el-table-column label="设计时长" prop="designDuration" width="120">
        <template #default="scope">
          <span v-if="scope.row.designDuration">{{ formatDuration(scope.row.designDuration) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createdAt" width="180">
        <template #default="scope">
          {{ scope.row.createdAt ? new Date(scope.row.createdAt).toLocaleString() : '未知' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="380" fixed="right">
        <template #default="scope">
          <el-button 
            type="primary" 
            link 
            @click="handleViewDetail(scope.row)" 
            :disabled="scope.row.status !== 'COMPLETED'"
          >
            查看
          </el-button>
          <el-button
            type="info"
            link
            @click="handleGetSolution(scope.row)"
            :disabled="scope.row.status !== 'COMPLETED'"
          >
            解答
          </el-button>
          <el-button
            type="warning"
            link
            @click="handleStartDesign(scope.row)"
            :disabled="scope.row.status !== 'COMPLETED'"
          >
            设计
          </el-button>
          <el-button
            type="success"
            link
            @click="handlePractice(scope.row)"
            :disabled="scope.row.status !== 'COMPLETED'"
          >
            练习
          </el-button>
          <el-button
            type="primary"
            link
            @click="handleViewOptimization(scope.row)"
            :disabled="!scope.row.efficiencyIndex"
          >
            优化建议
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 分页和其他现有对话框 -->
    <div class="pagination-container">
      <el-pagination
        :current-page="queryParams.pageNum"
        :page-size="queryParams.pageSize"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @current-change="handlePageChange"
      />
    </div>
    
    <!-- 生成题目对话框 -->
    <el-dialog
      v-model="generateDialogVisible"
      title="生成题目"
      width="50%"
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
        
        <el-form-item label="使用RAG">
          <el-switch v-model="generateForm.useRag" />
        </el-form-item>
        
        <template v-if="generateForm.useRag">
          <el-form-item label="RAG选择方式">
            <el-radio-group v-model="generateForm.useRagName" @change="generateForm.ragId = null; generateForm.ragName = ''">
              <el-radio :label="false">使用RAG ID</el-radio>
              <el-radio :label="true">使用RAG名称</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item v-if="!generateForm.useRagName" label="RAG ID" prop="ragId">
            <el-select
              v-model="generateForm.ragId"
              placeholder="请选择RAG"
              clearable
              style="width: 100%"
              :loading="loadingRag"
            >
              <el-option
                v-for="item in ragList.filter(rag => rag.status === 'COMPLETED')"
                :key="item.id"
                :label="`${item.name} (ID: ${item.id})`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item v-else label="RAG名称" prop="ragName">
            <el-input v-model="generateForm.ragName" placeholder="请输入RAG名称" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="generateDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitGenerateForm(generateFormRef)">提交</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 题目详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="题目详情"
      width="70%"
      append-to-body
    >
      <div v-if="currentQuestion">
        <div class="question-info">
          <div class="info-item">
            <span class="info-label">ID:</span>
            <span class="info-value">{{ currentQuestion.id }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">检索词:</span>
            <span class="info-value">{{ currentQuestion.query }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">题目类型:</span>
            <span class="info-value">{{ formatQuestionType(currentQuestion.questionType) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">RAG名称:</span>
            <span class="info-value">{{ currentQuestion.ragName || '无' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">状态:</span>
            <span class="info-value">
              <el-tag :type="statusTypeMap[currentQuestion.status]" effect="plain">
                {{ formatStatus(currentQuestion.status) }}
              </el-tag>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间:</span>
            <span class="info-value">{{ new Date(currentQuestion.createdAt).toLocaleString() }}</span>
          </div>
        </div>
        
        <el-divider content-position="center">题目内容</el-divider>
        
        <div class="question-content" v-if="currentQuestion.questionJson">
          <!-- 选择题专用显示 -->
          <div v-if="renderQuestion(currentQuestion.questionJson).type === '选择题'" class="choice-question">
            <div class="question-title">
              <span class="question-type-tag">【选择题】</span>
              {{ renderQuestion(currentQuestion.questionJson).question }}
            </div>
            <div class="options-container">
              <div 
                v-for="(option, key) in renderQuestion(currentQuestion.questionJson).options" 
                :key="key"
                class="option-item"
                :class="{'correct-option': key === renderQuestion(currentQuestion.questionJson).answer}"
              >
                <span class="option-key">{{ key }}.</span>
                <span class="option-content">{{ option }}</span>
              </div>
            </div>
            <div v-if="renderQuestion(currentQuestion.questionJson).answer" class="question-answer">
              <span class="answer-label">答案：</span>
              <span class="answer-content">{{ renderQuestion(currentQuestion.questionJson).answer }}</span>
            </div>
            <div v-if="renderQuestion(currentQuestion.questionJson).explanation" class="question-explanation">
              <div class="explanation-label">解析：</div>
              <div class="explanation-content" v-html="formatExplanation(renderQuestion(currentQuestion.questionJson).explanation)"></div>
            </div>
          </div>
          <!-- 判断题专用显示 -->
          <div v-else-if="renderQuestion(currentQuestion.questionJson).type === '判断题'" class="true-false-question">
            <div class="question-title">
              <span class="question-type-tag judge-tag">【判断题】</span>
              {{ renderQuestion(currentQuestion.questionJson).statement }}
            </div>
            <div class="judge-options">
              <div 
                class="judge-option" 
                :class="{ 'selected-option': renderQuestion(currentQuestion.questionJson).answer === '正确' }"
              >
                <span class="judge-icon">✓</span>正确
              </div>
              <div 
                class="judge-option"
                :class="{ 'selected-option': renderQuestion(currentQuestion.questionJson).answer === '错误' }"
              >
                <span class="judge-icon">✗</span>错误
              </div>
            </div>
            <div class="question-answer">
              <span class="answer-label">答案：</span>
              <span class="answer-content">{{ renderQuestion(currentQuestion.questionJson).answer }}</span>
            </div>
            <div v-if="renderQuestion(currentQuestion.questionJson).explanation" class="question-explanation">
              <div class="explanation-label">解析：</div>
              <div class="explanation-content" v-html="formatExplanation(renderQuestion(currentQuestion.questionJson).explanation)"></div>
            </div>
          </div>
          <!-- 问答题专用显示 -->
          <div v-else-if="renderQuestion(currentQuestion.questionJson).type === '问答题'" class="qa-question">
            <div class="question-title">
              <span class="question-type-tag qa-tag">【问答题】</span>
              {{ renderQuestion(currentQuestion.questionJson).question }}
            </div>
            <div v-if="renderQuestion(currentQuestion.questionJson).key_points && renderQuestion(currentQuestion.questionJson).key_points.length > 0" class="key-points">
              <div class="key-points-title">关键点：</div>
              <ul class="key-points-list">
                <li v-for="(point, index) in renderQuestion(currentQuestion.questionJson).key_points" :key="index">
                  {{ point }}
                </li>
              </ul>
            </div>
            <div v-if="renderQuestion(currentQuestion.questionJson).answer" class="question-answer qa-answer">
              <div class="answer-label">参考答案：</div>
              <div class="answer-content" v-html="formatExplanation(renderQuestion(currentQuestion.questionJson).answer)"></div>
            </div>
          </div>
          <!-- 编程题专用显示 -->
          <div v-else-if="renderQuestion(currentQuestion.questionJson).type === '编程题'" class="programming-question">
            <div class="question-title">
              <span class="question-type-tag programming-tag">【编程题】</span>
              {{ renderQuestion(currentQuestion.questionJson).title }}
            </div>
            
            <div class="section">
              <div class="section-title">题目描述：</div>
              <div class="section-content" v-html="formatExplanation(renderQuestion(currentQuestion.questionJson).description)"></div>
            </div>
            
            <div class="section" v-if="renderQuestion(currentQuestion.questionJson).input_format">
              <div class="section-title">输入格式：</div>
              <pre class="code-block">{{ renderQuestion(currentQuestion.questionJson).input_format }}</pre>
            </div>
            
            <div class="section" v-if="renderQuestion(currentQuestion.questionJson).output_format">
              <div class="section-title">输出格式：</div>
              <div class="section-content">{{ renderQuestion(currentQuestion.questionJson).output_format }}</div>
            </div>
            
            <div class="section" v-if="renderQuestion(currentQuestion.questionJson).examples && renderQuestion(currentQuestion.questionJson).examples.length > 0">
              <div class="section-title">示例：</div>
              <div v-for="(example, index) in renderQuestion(currentQuestion.questionJson).examples" :key="index" class="example-item">
                <div class="example-header">示例 {{ index + 1 }}：</div>
                <div class="example-content">
                  <div class="example-input">
                    <div class="example-label">输入：</div>
                    <pre class="code-block">{{ example.input }}</pre>
                  </div>
                  <div class="example-output">
                    <div class="example-label">输出：</div>
                    <pre class="code-block">{{ example.output }}</pre>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="section" v-if="renderQuestion(currentQuestion.questionJson).solution_approach">
              <div class="section-title">解题思路：</div>
              <div class="section-content" v-html="formatExplanation(renderQuestion(currentQuestion.questionJson).solution_approach)"></div>
            </div>
            
            <div class="section" v-if="renderQuestion(currentQuestion.questionJson).reference_code">
              <div class="section-title">参考代码：</div>
              <pre class="code-block">{{ renderQuestion(currentQuestion.questionJson).reference_code }}</pre>
            </div>
          </div>
          <!-- 其他题型仍使用JSON格式展示 -->
          <pre v-else>{{ formatJson(currentQuestion.questionJson) }}</pre>
        </div>
        <el-empty v-else description="暂无题目内容" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
    


    <!-- 题目解答对话框 -->
    <el-dialog
      v-model="solutionDialogVisible"
      title="题目解答"
      width="70%"
      append-to-body
    >
      <div v-loading="solutionLoading">
        <div class="question-info" v-if="currentQuestion">
          <div class="info-item">
            <span class="info-label">检索词:</span>
            <span class="info-value">{{ currentQuestion.query }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">题目类型:</span>
            <span class="info-value">{{ formatQuestionType(currentQuestion.questionType) }}</span>
          </div>
        </div>
        
        <el-divider content-position="center">题目内容</el-divider>
        
        <div class="question-content" v-if="currentQuestion && currentQuestion.questionJson">
          <!-- 选择题专用显示 -->
          <div v-if="renderQuestion(currentQuestion.questionJson).type === '选择题'" class="choice-question">
            <div class="question-title">
              <span class="question-type-tag">【选择题】</span>
              {{ renderQuestion(currentQuestion.questionJson).question }}
            </div>
            <div class="options-container">
              <div 
                v-for="(option, key) in renderQuestion(currentQuestion.questionJson).options" 
                :key="key"
                class="option-item"
                :class="{'correct-option': key === renderQuestion(currentQuestion.questionJson).answer}"
              >
                <span class="option-key">{{ key }}.</span>
                <span class="option-content">{{ option }}</span>
              </div>
            </div>
            <div v-if="renderQuestion(currentQuestion.questionJson).answer" class="question-answer">
              <span class="answer-label">答案：</span>
              <span class="answer-content">{{ renderQuestion(currentQuestion.questionJson).answer }}</span>
            </div>
            <div v-if="renderQuestion(currentQuestion.questionJson).explanation" class="question-explanation">
              <div class="explanation-label">解析：</div>
              <div class="explanation-content" v-html="formatExplanation(renderQuestion(currentQuestion.questionJson).explanation)"></div>
            </div>
          </div>
          <!-- 判断题专用显示 -->
          <div v-else-if="renderQuestion(currentQuestion.questionJson).type === '判断题'" class="true-false-question">
            <div class="question-title">
              <span class="question-type-tag judge-tag">【判断题】</span>
              {{ renderQuestion(currentQuestion.questionJson).statement }}
            </div>
            <div class="judge-options">
              <div 
                class="judge-option" 
                :class="{ 'selected-option': renderQuestion(currentQuestion.questionJson).answer === '正确' }"
              >
                <span class="judge-icon">✓</span>正确
              </div>
              <div 
                class="judge-option"
                :class="{ 'selected-option': renderQuestion(currentQuestion.questionJson).answer === '错误' }"
              >
                <span class="judge-icon">✗</span>错误
              </div>
            </div>
            <div class="question-answer">
              <span class="answer-label">答案：</span>
              <span class="answer-content">{{ renderQuestion(currentQuestion.questionJson).answer }}</span>
            </div>
            <div v-if="renderQuestion(currentQuestion.questionJson).explanation" class="question-explanation">
              <div class="explanation-label">解析：</div>
              <div class="explanation-content" v-html="formatExplanation(renderQuestion(currentQuestion.questionJson).explanation)"></div>
            </div>
          </div>
          <!-- 问答题专用显示 -->
          <div v-else-if="renderQuestion(currentQuestion.questionJson).type === '问答题'" class="qa-question">
            <div class="question-title">
              <span class="question-type-tag qa-tag">【问答题】</span>
              {{ renderQuestion(currentQuestion.questionJson).question }}
            </div>
            <div v-if="renderQuestion(currentQuestion.questionJson).key_points && renderQuestion(currentQuestion.questionJson).key_points.length > 0" class="key-points">
              <div class="key-points-title">关键点：</div>
              <ul class="key-points-list">
                <li v-for="(point, index) in renderQuestion(currentQuestion.questionJson).key_points" :key="index">
                  {{ point }}
                </li>
              </ul>
            </div>
            <div v-if="renderQuestion(currentQuestion.questionJson).answer" class="question-answer qa-answer">
              <div class="answer-label">参考答案：</div>
              <div class="answer-content" v-html="formatExplanation(renderQuestion(currentQuestion.questionJson).answer)"></div>
            </div>
          </div>
          <!-- 编程题专用显示 -->
          <div v-else-if="renderQuestion(currentQuestion.questionJson).type === '编程题'" class="programming-question">
            <div class="question-title">
              <span class="question-type-tag programming-tag">【编程题】</span>
              {{ renderQuestion(currentQuestion.questionJson).title }}
            </div>
            
            <div class="section">
              <div class="section-title">题目描述：</div>
              <div class="section-content" v-html="formatExplanation(renderQuestion(currentQuestion.questionJson).description)"></div>
            </div>
            
            <div class="section" v-if="renderQuestion(currentQuestion.questionJson).input_format">
              <div class="section-title">输入格式：</div>
              <pre class="code-block">{{ renderQuestion(currentQuestion.questionJson).input_format }}</pre>
            </div>
            
            <div class="section" v-if="renderQuestion(currentQuestion.questionJson).output_format">
              <div class="section-title">输出格式：</div>
              <div class="section-content">{{ renderQuestion(currentQuestion.questionJson).output_format }}</div>
            </div>
            
            <div class="section" v-if="renderQuestion(currentQuestion.questionJson).examples && renderQuestion(currentQuestion.questionJson).examples.length > 0">
              <div class="section-title">示例：</div>
              <div v-for="(example, index) in renderQuestion(currentQuestion.questionJson).examples" :key="index" class="example-item">
                <div class="example-header">示例 {{ index + 1 }}：</div>
                <div class="example-content">
                  <div class="example-input">
                    <div class="example-label">输入：</div>
                    <pre class="code-block">{{ example.input }}</pre>
                  </div>
                  <div class="example-output">
                    <div class="example-label">输出：</div>
                    <pre class="code-block">{{ example.output }}</pre>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="section" v-if="renderQuestion(currentQuestion.questionJson).solution_approach">
              <div class="section-title">解题思路：</div>
              <div class="section-content" v-html="formatExplanation(renderQuestion(currentQuestion.questionJson).solution_approach)"></div>
            </div>
            
            <div class="section" v-if="renderQuestion(currentQuestion.questionJson).reference_code">
              <div class="section-title">参考代码：</div>
              <pre class="code-block">{{ renderQuestion(currentQuestion.questionJson).reference_code }}</pre>
            </div>
          </div>
          <!-- 其他题型仍使用JSON格式展示 -->
          <pre v-else>{{ formatJson(currentQuestion.questionJson) }}</pre>
        </div>
        
        <el-divider content-position="center">题目解答</el-divider>
        
        <div class="solution-content">
          <div v-if="currentSolution" v-html="markdownToHtml(currentSolution)"></div>
          <el-empty v-else description="暂无解答内容" />
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="solutionDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleCopySolution" v-if="currentSolution">复制解答</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 课后练习设计对话框 -->
    <el-dialog
      v-model="designDialogVisible"
      title="课后练习设计与修正"
      width="70%"
      append-to-body
      :close-on-click-modal="false"
    >
      <div v-if="currentQuestion" class="design-header">
        <div class="info-item">
          <span class="info-label">检索词:</span>
          <span class="info-value">{{ currentQuestion.query }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">题目类型:</span>
          <span class="info-value">{{ formatQuestionType(currentQuestion.questionType) }}</span>
        </div>
      </div>
      
      <el-divider content-position="center">设计内容</el-divider>
      
      <div class="design-content" v-loading="designLoading">
        <el-input
          v-model="designContent"
          type="textarea"
          :rows="15"
          placeholder="请在此处设计和修正课后练习内容"
          resize="both"
        />
        
        <div class="design-tips">
          <h4>设计建议：</h4>
          <ul>
            <li>根据学生实际水平设置适当的难度梯度</li>
            <li>增加实践型题目，培养学生动手能力</li>
            <li>添加开放性问题，激发创造性思维</li>
            <li>明确考察重点，与教学目标对应</li>
          </ul>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="designDialogVisible = false">取消</el-button>
          <el-button type="info" @click="handleSaveDesign" :loading="saveDesignLoading">保存</el-button>
          <el-button type="primary" @click="handleFinishDesign" :loading="finishDesignLoading">完成设计</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 设计效率结果对话框 -->
    <el-dialog
      v-model="designEfficiencyDialogVisible"
      title="课后练习设计效率统计"
      width="50%"
      append-to-body
    >
      <div v-if="currentEfficiencyData" class="efficiency-container" v-loading="false">
        <!-- 统计总览 -->
        <div class="efficiency-overview">
          <div class="efficiency-card">
            <div class="card-title">平均效率指数</div>
            <div class="card-value" :class="'text-' + getEfficiencyColor(currentEfficiencyData.avgEfficiencyIndex)">
              {{ currentEfficiencyData.avgEfficiencyIndex?.toFixed(2) || '无数据' }}
            </div>
          </div>
          
          <div class="efficiency-card">
            <div class="card-title">平均设计时长</div>
            <div class="card-value">
              {{ formatDuration(currentEfficiencyData.avgDesignDuration) }}
            </div>
          </div>
          
          <div class="efficiency-card">
            <div class="card-title">已设计题目数</div>
            <div class="card-value">{{ currentEfficiencyData.totalDesignedQuestions || 0 }}</div>
          </div>
        </div>
        
        <!-- 效率详情 -->
        <div v-if="currentEfficiencyData.mostEfficient || currentEfficiencyData.leastEfficient" class="efficiency-details">
          <el-divider content-position="center">效率详情</el-divider>
          
          <div class="detail-section" v-if="currentEfficiencyData.mostEfficient">
            <h4>最高效率题目</h4>
            <div class="detail-item">
              <span>题目ID: {{ currentEfficiencyData.mostEfficient.id }}</span>
            </div>
            <div class="detail-item">
              <span>检索词: {{ currentEfficiencyData.mostEfficient.query }}</span>
            </div>
            <div class="detail-item">
              <span>题目类型: {{ formatQuestionType(currentEfficiencyData.mostEfficient.questionType) }}</span>
            </div>
            <div class="detail-item">
              <span>效率指数: </span>
              <el-tag :type="getEfficiencyColor(currentEfficiencyData.mostEfficient.efficiencyIndex)">
                {{ currentEfficiencyData.mostEfficient.efficiencyIndex?.toFixed(2) }}
              </el-tag>
            </div>
          </div>
          
          <div class="detail-section" v-if="currentEfficiencyData.leastEfficient">
            <h4>最低效率题目</h4>
            <div class="detail-item">
              <span>题目ID: {{ currentEfficiencyData.leastEfficient.id }}</span>
            </div>
            <div class="detail-item">
              <span>检索词: {{ currentEfficiencyData.leastEfficient.query }}</span>
            </div>
            <div class="detail-item">
              <span>题目类型: {{ formatQuestionType(currentEfficiencyData.leastEfficient.questionType) }}</span>
            </div>
            <div class="detail-item">
              <span>效率指数: </span>
              <el-tag :type="getEfficiencyColor(currentEfficiencyData.leastEfficient.efficiencyIndex)">
                {{ currentEfficiencyData.leastEfficient.efficiencyIndex?.toFixed(2) }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <!-- 月度效率趋势 -->
        <div v-if="currentEfficiencyData.monthlyEfficiency && Object.keys(currentEfficiencyData.monthlyEfficiency).length > 0" class="efficiency-trend">
          <el-divider content-position="center">月度效率趋势</el-divider>
          
          <div class="month-chart">
            <el-timeline>
              <el-timeline-item
                v-for="(value, month) in currentEfficiencyData.monthlyEfficiency"
                :key="month"
                :type="getEfficiencyColor(value)"
                :timestamp="month"
              >
                效率指数: {{ value.toFixed(2) }}
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
        
        <!-- 优化建议 -->
        <div v-if="currentEfficiencyData.optimizationSuggestions" class="optimization-section">
          <el-divider content-position="center">优化建议</el-divider>
          
          <el-alert
            type="info"
            :closable="false"
            show-icon
          >
            <pre>{{ currentEfficiencyData.optimizationSuggestions }}</pre>
          </el-alert>
        </div>
      </div>
      <el-empty v-else description="暂无效率数据" />
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="designEfficiencyDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 优化建议对话框 -->
    <el-dialog
      v-model="optimizationDialogVisible"
      title="课后练习优化建议"
      width="50%"
      append-to-body
      :close-on-click-modal="false"
    >
      <div v-loading="optimizationLoading" element-loading-text="正在生成优化建议..." class="optimization-container">
        <div v-if="currentOptimizationData" class="question-info">
          <div class="info-item">
            <span class="info-label">检索词:</span>
            <span class="info-value">{{ currentOptimizationData.query }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">题目类型:</span>
            <span class="info-value">{{ formatQuestionType(currentOptimizationData.questionType) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">设计时长:</span>
            <span class="info-value">{{ formatDuration(currentOptimizationData.designDuration) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">效率指数:</span>
            <span class="info-value">
              <el-tag :type="getEfficiencyColor(currentOptimizationData.efficiencyIndex)">
                {{ currentOptimizationData.efficiencyIndex?.toFixed(2) }}
              </el-tag>
            </span>
          </div>
        </div>
        
        <el-divider content-position="center">优化建议</el-divider>
        
        <div v-if="currentOptimizationData && currentOptimizationData.optimizationSuggestions" class="suggestion-content">
          <el-alert
            type="success"
            :closable="false"
            show-icon
            title="系统建议"
          >
            <pre>{{ currentOptimizationData.optimizationSuggestions }}</pre>
          </el-alert>
        </div>
        
        <div v-if="currentOptimizationData && currentOptimizationData.structuredSuggestions" class="structured-suggestion-content">
          <div class="suggestion-header">
            <h3 class="suggestion-summary">{{ currentOptimizationData.structuredSuggestions.summary }}</h3>
          </div>
          
          <!-- 分类评分卡片 -->
          <div class="categories-grid">
            <el-card v-for="(category, index) in currentOptimizationData.structuredSuggestions.categories" :key="index" class="category-card" shadow="hover">
              <template #header>
                <div class="category-header">
                  <span class="category-name">{{ category.name }}</span>
                  <el-rate
                    v-model="category.score"
                    :max="10"
                    disabled
                    show-score
                    text-color="#ff9900"
                  />
                </div>
              </template>
              
              <div class="category-content">
                <div class="strengths-section">
                  <h4 class="section-title">优势</h4>
                  <ul class="points-list">
                    <li v-for="(strength, i) in category.strengths" :key="i">{{ strength }}</li>
                  </ul>
                </div>
                
                <div class="weaknesses-section">
                  <h4 class="section-title">不足</h4>
                  <ul class="points-list">
                    <li v-for="(weakness, i) in category.weaknesses" :key="i">{{ weakness }}</li>
                  </ul>
                </div>
                
                <div class="suggestions-section">
                  <h4 class="section-title">建议</h4>
                  <ul class="points-list">
                    <li v-for="(suggestion, i) in category.suggestions" :key="i">{{ suggestion }}</li>
                  </ul>
                </div>
              </div>
            </el-card>
          </div>
          
          <!-- 综合建议 -->
          <div class="overall-suggestion">
            <el-alert
              type="success"
              :closable="false"
              show-icon
              title="综合建议"
            >
              <p>{{ currentOptimizationData.structuredSuggestions.overall_suggestion }}</p>
            </el-alert>
          </div>
        </div>

        <div v-else-if="currentOptimizationData && currentOptimizationData.additionalSuggestions" class="additional-suggestion-content">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            :title="currentOptimizationData.jsonParseError ? '针对性建议 (解析失败，显示原始内容)' : '针对性建议'"
            style="margin-top: 15px;"
          >
            <pre>{{ currentOptimizationData.additionalSuggestions }}</pre>
          </el-alert>
        </div>
        
        <el-empty v-if="currentOptimizationData && !optimizationLoading && !currentOptimizationData.optimizationSuggestions && !currentOptimizationData.additionalSuggestions" description="暂无优化建议" />
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="optimizationDialogVisible = false" :disabled="optimizationLoading">关闭</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 练习对话框 -->
    <el-dialog
      v-model="practiceDialogVisible"
      title="题目练习"
      width="70%"
      append-to-body
      :close-on-click-modal="false"
    >
      <div v-loading="practiceLoading" class="practice-container">
        <!-- 题目信息 -->
        <div v-if="currentPractice" class="question-info">
          <div class="info-item">
            <span class="info-label">检索词:</span>
            <span class="info-value">{{ currentPractice.query }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">题目类型:</span>
            <span class="info-value">{{ formatQuestionType(currentPractice.questionType) }}</span>
          </div>
        </div>
        
        <el-divider content-position="center">题目内容</el-divider>
        
        <!-- 题目内容 -->
        <div class="question-content" v-if="currentPractice && currentPractice.questionJson">
          <!-- 选择题练习 -->
          <div v-if="renderQuestion(currentPractice.questionJson).type === '选择题'" class="choice-question">
            <div class="question-title">
              <span class="question-type-tag">【选择题】</span>
              {{ renderQuestion(currentPractice.questionJson).question }}
            </div>
            <div class="options-container">
              <div 
                v-for="(option, key) in renderQuestion(currentPractice.questionJson).options" 
                :key="key"
                class="option-item"
                :class="{'selected-option': userAnswer === key && !practiceResult}"
                @click="userAnswer = key"
              >
                <span class="option-key">{{ key }}.</span>
                <span class="option-content">{{ option }}</span>
              </div>
            </div>
          </div>
          
          <!-- 判断题练习 -->
          <div v-else-if="renderQuestion(currentPractice.questionJson).type === '判断题'" class="true-false-question">
            <div class="question-title">
              <span class="question-type-tag judge-tag">【判断题】</span>
              {{ renderQuestion(currentPractice.questionJson).statement }}
            </div>
            <div class="judge-options">
              <div 
                class="judge-option"
                :class="{'selected-option': userAnswer === '正确' && !practiceResult}"
                @click="userAnswer = '正确'"
              >
                <span class="judge-icon">✓</span>正确
              </div>
              <div 
                class="judge-option"
                :class="{'selected-option': userAnswer === '错误' && !practiceResult}"
                @click="userAnswer = '错误'"
              >
                <span class="judge-icon">✗</span>错误
              </div>
            </div>
          </div>
          
          <!-- 问答题练习 -->
          <div v-else-if="renderQuestion(currentPractice.questionJson).type === '问答题'" class="qa-question">
            <div class="question-title">
              <span class="question-type-tag qa-tag">【问答题】</span>
              {{ renderQuestion(currentPractice.questionJson).question }}
            </div>
            <el-input
              v-model="userAnswer"
              type="textarea"
              :rows="8"
              placeholder="请在此处输入您的答案"
              :disabled="!!practiceResult"
            />
          </div>
          
          <!-- 编程题练习 -->
          <div v-else-if="renderQuestion(currentPractice.questionJson).type === '编程题'" class="programming-question">
            <div class="question-title">
              <span class="question-type-tag programming-tag">【编程题】</span>
              {{ renderQuestion(currentPractice.questionJson).title }}
            </div>
            
            <div class="section">
              <div class="section-title">题目描述：</div>
              <div class="section-content" v-html="formatExplanation(renderQuestion(currentPractice.questionJson).description)"></div>
            </div>
            
            <div class="section" v-if="renderQuestion(currentPractice.questionJson).input_format">
              <div class="section-title">输入格式：</div>
              <pre class="code-block">{{ renderQuestion(currentPractice.questionJson).input_format }}</pre>
            </div>
            
            <div class="section" v-if="renderQuestion(currentPractice.questionJson).output_format">
              <div class="section-title">输出格式：</div>
              <div class="section-content">{{ renderQuestion(currentPractice.questionJson).output_format }}</div>
            </div>
            
            <div class="section" v-if="renderQuestion(currentPractice.questionJson).examples && renderQuestion(currentPractice.questionJson).examples.length > 0">
              <div class="section-title">示例：</div>
              <div v-for="(example, index) in renderQuestion(currentPractice.questionJson).examples" :key="index" class="example-item">
                <div class="example-header">示例 {{ index + 1 }}：</div>
                <div class="example-content">
                  <div class="example-input">
                    <div class="example-label">输入：</div>
                    <pre class="code-block">{{ example.input }}</pre>
                  </div>
                  <div class="example-output">
                    <div class="example-label">输出：</div>
                    <pre class="code-block">{{ example.output }}</pre>
                  </div>
                </div>
              </div>
            </div>
            
            <el-input
              v-model="userAnswer"
              type="textarea"
              :rows="12"
              placeholder="请在此处编写您的代码"
              :disabled="!!practiceResult"
              class="code-editor"
              spellcheck="false"
            />
          </div>
          
          <!-- 不支持的题型 -->
          <div v-else class="unsupported-question">
            <el-alert
              title="不支持的题目类型"
              type="warning"
              description="当前题目类型不支持在线练习"
              :closable="false"
              show-icon
            />
          </div>
        </div>
        
        <!-- 评分结果 -->
        <div v-if="practiceResult" class="practice-result">
          <el-divider content-position="center">评分结果</el-divider>
          
          <!-- 选择题/判断题结果 -->
          <div v-if="renderQuestion(currentPractice?.questionJson)?.type === '选择题' || renderQuestion(currentPractice?.questionJson)?.type === '判断题'" class="basic-result">
            <el-result
              :icon="practiceResult.correct ? 'success' : 'error'"
              :title="practiceResult.correct ? '回答正确！' : '回答错误'"
              :sub-title="practiceResult.correct ? '太棒了！' : '请继续努力！'"
            >
              <template #extra>
                <div class="result-details">
                  <div class="result-item">
                    <span class="result-label">你的答案:</span>
                    <span class="result-value">{{ userAnswer }}</span>
                  </div>
                  <div v-if="!practiceResult.correct" class="result-item">
                    <span class="result-label">正确答案:</span>
                    <span class="result-value">{{ renderQuestion(currentPractice.questionJson).answer }}</span>
                  </div>
                  <div v-if="!practiceResult.correct && renderQuestion(currentPractice.questionJson).explanation" class="result-item">
                    <div class="result-label">解析:</div>
                    <div class="result-value" v-html="formatExplanation(renderQuestion(currentPractice.questionJson).explanation)"></div>
                  </div>
                </div>
              </template>
            </el-result>
          </div>
          
          <!-- AI评分结果 (问答题/编程题) -->
          <div v-else-if="practiceResult.aiGenerated" class="ai-result">
            <div class="score-section">
              <el-progress
                type="dashboard"
                :percentage="practiceResult.score"
                :color="getScoreColor(practiceResult.score)"
                :status="practiceResult.score >= 60 ? 'success' : 'exception'"
              />
              <div class="score-label">{{ getScoreLevel(practiceResult.score) }}</div>
            </div>
            
            <el-divider content-position="center">详细反馈</el-divider>
            
            <div class="feedback-section">
              <div class="feedback-content" v-html="formatExplanation(practiceResult.feedback)"></div>
              
              <!-- 问答题关键点分析 -->
              <div v-if="practiceResult.keyPointsCovered && practiceResult.keyPointsMissed" class="key-points-analysis">
                <div class="covered-points">
                  <h4>已涵盖的关键点</h4>
                  <ul>
                    <li v-for="(point, index) in practiceResult.keyPointsCovered" :key="index">{{ point }}</li>
                  </ul>
                </div>
                <div class="missed-points">
                  <h4>未涵盖的关键点</h4>
                  <ul>
                    <li v-for="(point, index) in practiceResult.keyPointsMissed" :key="index">{{ point }}</li>
                  </ul>
                </div>
              </div>
              
              <!-- 编程题详细分析 -->
              <div v-if="practiceResult.correctness || practiceResult.efficiency || practiceResult.readability" class="code-analysis">
                <el-collapse>
                  <el-collapse-item title="代码正确性" name="correctness">
                    <div v-html="formatExplanation(practiceResult.correctness)"></div>
                  </el-collapse-item>
                  <el-collapse-item title="代码效率" name="efficiency">
                    <div v-html="formatExplanation(practiceResult.efficiency)"></div>
                  </el-collapse-item>
                  <el-collapse-item title="代码可读性" name="readability">
                    <div v-html="formatExplanation(practiceResult.readability)"></div>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>
          </div>
          
          <!-- 错误结果 -->
          <div v-else-if="practiceResult.error" class="error-result">
            <el-alert
              title="评分出错"
              type="error"
              :description="practiceResult.feedback"
              :closable="false"
              show-icon
            />
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="practiceDialogVisible = false">关闭</el-button>
          <el-button type="info" @click="resetPractice" v-if="practiceResult">重新作答</el-button>
          <el-button 
            type="primary" 
            @click="submitPractice" 
            :loading="practiceGrading" 
            :disabled="!userAnswer || practiceGrading || practiceResult"
          >提交答案</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.question-generator-container {
  padding: 20px;
  background-color: var(--el-bg-color);
  min-height: calc(100vh - 60px);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-form {
  flex-grow: 1;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.question-info {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  align-items: center;
}

.info-label {
  font-weight: bold;
  margin-right: 10px;
  min-width: 80px;
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

.choice-question {
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
  cursor: pointer;
  transition: background-color 0.3s ease;
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

.programming-question .question-title {
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--el-color-warning-light-5);
}

.section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

/* 添加新的样式 */
.design-header {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.design-content {
  margin-bottom: 20px;
}

.design-tips {
  margin-top: 15px;
  padding: 15px;
  background-color: var(--el-color-info-light-9);
  border-radius: 4px;
}

.design-tips h4 {
  margin-top: 0;
  color: var(--el-color-info);
}

.design-tips ul {
  margin-bottom: 0;
}

.design-tips li {
  margin-bottom: 5px;
  color: var(--el-text-color-secondary);
}

.efficiency-container {
  padding: 10px;
}

.efficiency-overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
}

/* 结构化建议样式 */
.structured-suggestion-content {
  margin-top: 20px;
}

.suggestion-header {
  text-align: center;
  margin-bottom: 20px;
}

.suggestion-summary {
  font-size: 16px;
  color: #303133;
  font-weight: 600;
  margin: 0;
  padding: 10px;
  background-color: #f0f9eb;
  border-radius: 4px;
  border-left: 4px solid #67c23a;
}

.categories-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.category-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.category-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1) !important;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.category-name {
  font-weight: 600;
  font-size: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 10px 0 5px;
  color: #606266;
  border-bottom: 1px dashed #dcdfe6;
  padding-bottom: 5px;
}

.points-list {
  padding-left: 20px;
  margin: 5px 0;
}

.points-list li {
  margin-bottom: 5px;
  line-height: 1.5;
}

.overall-suggestion {
  margin-top: 20px;
}

.overall-suggestion p {
  line-height: 1.6;
  text-indent: 2em;
  margin: 0;
}

.efficiency-card {
  padding: 15px;
  text-align: center;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background-color: var(--el-fill-color-light);
}

.card-title {
  font-size: 16px;
  color: var(--el-text-color-secondary);
  margin-bottom: 10px;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-color-primary);
}

.text-success {
  color: var(--el-color-success);
}

.text-primary {
  color: var(--el-color-primary);
}

.text-warning {
  color: var(--el-color-warning);
}

.text-danger {
  color: var(--el-color-danger);
}

.efficiency-details {
  margin-top: 20px;
}

.detail-section {
  padding: 15px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  margin-bottom: 15px;
  background-color: var(--el-fill-color-lighter);
}

.detail-section h4 {
  margin-top: 0;
  color: var(--el-color-primary);
  border-bottom: 1px solid var(--el-border-color-light);
  padding-bottom: 10px;
}

.detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.month-chart {
  margin-top: 15px;
  padding: 10px;
}

.optimization-section {
  margin-top: 20px;
}

.optimization-section pre,
.suggestion-content pre,
.additional-suggestion-content pre {
  white-space: pre-wrap;
  margin: 0;
  font-family: inherit;
}

.optimization-container .question-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .efficiency-overview,
  .optimization-container .question-info {
    grid-template-columns: 1fr;
  }
}

/* 练习相关样式 */
.practice-container {
  padding: 20px;
}

.selected-option {
  border-color: var(--el-color-primary) !important;
  background-color: var(--el-color-primary-light-9) !important;
}

.practice-result {
  margin-top: 20px;
  padding: 20px;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
}

.basic-result .result-details {
  text-align: left;
  margin-top: 20px;
}

.result-item {
  margin-bottom: 10px;
}

.result-label {
  font-weight: bold;
  margin-right: 10px;
  color: var(--el-text-color-secondary);
}

.ai-result {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.score-section {
  text-align: center;
  margin-bottom: 20px;
}

.score-label {
  margin-top: 10px;
  font-size: 18px;
  font-weight: bold;
}

.feedback-section {
  width: 100%;
  text-align: left;
}

.feedback-content {
  line-height: 1.6;
  margin-bottom: 20px;
  padding: 10px;
  background-color: var(--el-fill-color-lighter);
  border-radius: 4px;
}

.key-points-analysis {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}

.covered-points, .missed-points {
  flex: 1;
  padding: 10px;
  border-radius: 4px;
}

.covered-points {
  background-color: var(--el-color-success-light-9);
}

.missed-points {
  background-color: var(--el-color-warning-light-9);
}

.covered-points h4, .missed-points h4 {
  margin-top: 0;
  margin-bottom: 10px;
}

.code-analysis {
  margin-top: 20px;
  width: 100%;
}

.code-editor {
  font-family: 'Consolas', 'Monaco', 'Andale Mono', 'Ubuntu Mono', 'Monaco', 'Menlo', 'Courier New', monospace;
}

@media (max-width: 768px) {
  .key-points-analysis {
    flex-direction: column;
  }
}
</style> 