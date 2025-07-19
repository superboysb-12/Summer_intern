<script setup>
import { ref, computed, onMounted, reactive, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCounterStore } from '../../stores/counter'
import { Calendar, Plus, Check, Search, Document, Download, DocumentDelete, Timer, User, DataAnalysis, Link, ArrowLeft, ArrowRight, Reading, ChatLineSquare } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import * as echarts from 'echarts'

const store = useCounterStore()
const userInfo = store.getUserInfo()
const router = useRouter()
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

// ==================== 继续学习对话框相关 ====================
const continueDialogVisible = ref(false)
const continueActiveTab = ref('resources')

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
  // 根据当前视图模式选择数据源
  const dataSource = viewMode.value === 'enrolled' ? courses.value : availableCourses.value;
  if (!dataSource || dataSource.length === 0) return [];
  
  let filtered = [...dataSource];
  
  // 按状态过滤（只针对已选课程）
  if (viewMode.value === 'enrolled' && filterStatus.value) {
    filtered = filtered.filter(course => course.status === filterStatus.value);
  }
  
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
    const response = await axios.get(`http://localhost:8080/student-courses/student/${userInfo.id}`, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
    
    courses.value = response.data
    
  } catch (err) {
    console.error('获取课程失败:', err)
    error.value = '获取课程信息失败，请稍后重试'
    ElMessage.error(error.value)
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
    const response = await axios.get(`http://localhost:8080/course-files/course/${courseId}/visible`, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
    
    if (response.data) {
      courseFiles.value = response.data
      
      // 获取文件详情
      await fetchFileDetails()
    }
    
    // 加载课程关联的RAG知识库
    await getCourseRags(courseId)
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
    // 后端未提供批量获取接口，逐个请求文件信息
    const detailsMap = {}
    await Promise.all(fileIds.map(async (fileId) => {
      try {
        const res = await axios.get(`http://localhost:8080/files/${fileId}/info`, {
          headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        })
        if (res.data && res.data.data) {
          detailsMap[fileId] = res.data.data
        }
      } catch (err) {
        console.error(`获取文件ID ${fileId} 的详情失败:`, err)
      }
    }))
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
  enrollDialogVisible.value = true
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

const filterStatus = ref('')  // 添加缺失的状态过滤变量
const viewMode = ref('enrolled')  // 添加视图模式变量
const enrollDialogVisible = ref(false)  // 添加选课对话框可见性变量
const enrollSearchKeyword = ref('')  // 添加选课搜索关键字变量
const enrollFilterCategory = ref('')  // 添加选课分类过滤变量
const courseCategories = ref([  // 添加课程分类选项
  { id: 'programming', name: '编程语言' },
  { id: 'ai', name: '人工智能' },
  { id: 'data', name: '数据科学' },
  { id: 'software', name: '软件工程' }
])
const activeTabName = ref('all')  // 添加标签页活动名变量
const detailActiveTab = ref('details')  // 添加详情标签页活动名变量
const filteredAvailableCourses = computed(() => {  // 添加过滤后的可选课程计算属性
  if (!availableCourses.value) return []
  
  let filtered = [...availableCourses.value]
  
  // 按搜索关键字过滤
  if (enrollSearchKeyword.value.trim()) {
    const keyword = enrollSearchKeyword.value.trim().toLowerCase()
    filtered = filtered.filter(course => 
      (course.name || '').toLowerCase().includes(keyword) || 
      (course.description || '').toLowerCase().includes(keyword)
    )
  }
  
  // 按分类过滤
  if (enrollFilterCategory.value) {
    filtered = filtered.filter(course => course.categoryId === enrollFilterCategory.value)
  }
  
  // 根据标签页过滤
  switch (activeTabName.value) {
    case 'recommended':
      // 假设推荐的逻辑
      filtered = filtered.filter(course => course.courseId % 4 === 0)
      break
    case 'popular':
      // 假设热门的逻辑
      filtered = filtered.filter(course => course.courseId % 3 === 0)
      break
    case 'newest':
      // 按创建时间排序
      filtered.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      break
  }
  
  return filtered
})

// 添加缺失的方法
const filterCourses = () => {
  // 这里逻辑由计算属性实现，不需要额外操作
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString()
}

const formatTimeAgo = (timestamp) => {
  if (!timestamp) return '-'
  
  const now = new Date()
  const past = new Date(timestamp)
  const diff = Math.floor((now - past) / 1000)
  
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 2592000) return `${Math.floor(diff / 86400)}天前`
  
  return formatDate(timestamp)
}

// 进入课程学习
const enterCourse = async (course) => {
  selectedCourseForDetail.value = course
  // 载入课程资源和知识库
  await loadCourseResources(course.courseId)
  
  continueActiveTab.value = 'resources'
  continueDialogVisible.value = true
}

// ==================== RAG知识库相关 ====================
const ragList = ref([])
const ragLoading = ref(false)
const knowledgeGraphDialogVisible = ref(false)
const selectedRag = ref(null)
const queryDialogVisible = ref(false)
const queryForm = reactive({
  query: '',
  ragId: null,
  topK: 5,
  includeGraphContext: false,
  contextDepth: 1
})
const queryResults = ref(null)
const currentKnowledgeGraph = ref({
  triples: [],
  totalCount: 0,
  knowledgeGraphPath: ''
})
const graphChart = ref(null)
const graphInstance = ref(null)
const searchInputValue = ref('')
const filteredTriples = ref([])
const isFiltering = ref(false)
const queryFormRef = ref(null)

// 查询规则
const queryRules = {
  query: [
    { required: true, message: '请输入查询内容', trigger: 'blur' }
  ]
}

// 获取课程关联的RAG知识库
const getCourseRags = async (courseId) => {
  if (!courseId) return
  
  ragLoading.value = true
  try {
    const response = await axios.get(`http://localhost:8080/courses/${courseId}/rags`, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
    
    if (Array.isArray(response.data)) {
      ragList.value = response.data
    } else {
      ragList.value = []
    }
  } catch (err) {
    console.error('获取课程RAG列表失败:', err)
    ElMessage.error('获取课程RAG知识库列表失败')
  } finally {
    ragLoading.value = false
  }
}

// 打开查询RAG对话框
const openQueryDialog = (rag) => {
  queryResults.value = null
  queryForm.query = ''
  queryForm.ragId = rag.id
  queryForm.topK = 5
  queryForm.includeGraphContext = false
  queryForm.contextDepth = 1
  queryDialogVisible.value = true
}

// 提交查询表单
const submitQueryForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const params = {
          query: queryForm.query,
          topK: queryForm.topK,
          includeGraphContext: queryForm.includeGraphContext,
          contextDepth: queryForm.contextDepth
        }
        
        const response = await axios.get(`http://localhost:8080/api/rag/query/${queryForm.ragId}`, {
          params: params,
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        })
        
        if (response.data && response.data.success) {
          queryResults.value = response.data.data || {}
          ElMessage.success('查询成功')
        } else {
          ElMessage.error(response.data?.message || '查询RAG失败')
        }
      } catch (error) {
        console.error('查询RAG失败:', error)
        ElMessage.error('查询RAG失败')
      }
    }
  })
}

// 查看知识图谱
const viewKnowledgeGraph = async (rag) => {
  try {
    ragLoading.value = true
    selectedRag.value = rag
    const response = await axios.get(`http://localhost:8080/api/rag/knowledge-graph/${rag.id}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    
    if (response.data && response.data.success) {
      currentKnowledgeGraph.value = response.data.data
      knowledgeGraphDialogVisible.value = true
      
      // 延迟初始化图表，确保DOM已经渲染完成
      setTimeout(() => {
        nextTick(() => {
          initKnowledgeGraph()
        })
      }, 300)
      
      ElMessage.success('知识图谱数据获取成功')
    } else {
      ElMessage.error(response.data?.message || '获取知识图谱数据失败')
    }
  } catch (error) {
    console.error('获取知识图谱数据失败:', error)
    ElMessage.error('获取知识图谱数据失败')
  } finally {
    ragLoading.value = false
  }
}

// 初始化知识图谱
const initKnowledgeGraph = () => {
  if (!graphChart.value) return
  
  // 销毁之前的实例
  if (graphInstance.value) {
    graphInstance.value.dispose()
  }
  
  // 创建新实例
  graphInstance.value = echarts.init(graphChart.value)
  
  // 准备数据
  const nodes = []
  const edges = []
  const nodeMap = new Map()
  
  // 处理三元组数据，构建节点和边
  currentKnowledgeGraph.value.triples.forEach(triple => {
    const [source, relation, target] = triple
    
    // 添加源节点
    if (!nodeMap.has(source)) {
      nodeMap.set(source, {
        id: source,
        name: source,
        symbolSize: 40,
        value: 1
      })
      nodes.push(nodeMap.get(source))
    } else {
      // 如果节点已存在，增加其值和大小
      const node = nodeMap.get(source)
      node.value += 1
      node.symbolSize = Math.min(40 + node.value * 5, 80)
    }
    
    // 添加目标节点
    if (!nodeMap.has(target)) {
      nodeMap.set(target, {
        id: target,
        name: target,
        symbolSize: 40,
        value: 1
      })
      nodes.push(nodeMap.get(target))
    } else {
      // 如果节点已存在，增加其值和大小
      const node = nodeMap.get(target)
      node.value += 1
      node.symbolSize = Math.min(40 + node.value * 5, 80)
    }
    
    // 添加边
    edges.push({
      source: source,
      target: target,
      name: relation,
      value: relation
    })
  })
  
  // 配置选项
  const option = {
    title: {
      text: '知识图谱可视化',
      subtext: `共${currentKnowledgeGraph.value.totalCount}个三元组`,
      top: 'top',
      left: 'center'
    },
    tooltip: {
      formatter: function(params) {
        if (params.dataType === 'node') {
          return `<div>实体: ${params.name}</div><div>连接数: ${params.value}</div>`
        } else {
          return `<div>关系: ${params.value}</div><div>从: ${params.data.source}</div><div>到: ${params.data.target}</div>`
        }
      }
    },
    toolbox: {
      show: true,
      feature: {
        saveAsImage: {},
        restore: {},
        dataView: {},
        dataZoom: {},
        magicType: {
          type: ['force', 'circular']
        }
      }
    },
    legend: [
      {
        data: ['实体关系']
      }
    ],
    series: [
      {
        name: '知识图谱',
        type: 'graph',
        layout: 'force',
        data: nodes,
        links: edges,
        categories: [{ name: '实体关系' }],
        roam: true,
        draggable: true,
        label: {
          show: true,
          position: 'right',
          formatter: '{b}'
        },
        force: {
          repulsion: 200,
          gravity: 0.1,
          edgeLength: 150,
          layoutAnimation: true
        },
        edgeSymbol: ['circle', 'arrow'],
        edgeSymbolSize: [4, 10],
        edgeLabel: {
          show: true,
          position: 'middle',
          formatter: '{c}'
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: {
            width: 5
          }
        }
      }
    ]
  }
  
  // 应用选项
  graphInstance.value.setOption(option)
  
  // 添加点击事件
  graphInstance.value.on('click', params => {
    if (params.dataType === 'node') {
      // 点击节点显示相关信息
      const relatedTriples = currentKnowledgeGraph.value.triples.filter(
        triple => triple[0] === params.name || triple[2] === params.name
      )
      
      const content = `
        <div style="max-height: 400px; overflow-y: auto;">
          <h3>实体: ${params.name}</h3>
          <h4>相关三元组:</h4>
          <ul style="padding-left: 20px;">
            ${relatedTriples.map(t => `<li>${t[0]} - <b>${t[1]}</b> - ${t[2]}</li>`).join('')}
          </ul>
        </div>
      `
      
      ElMessageBox.alert(content, '实体详情', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭'
      })
    }
  })
  
  // 窗口大小变化时自动调整图表大小
  window.addEventListener('resize', () => {
    graphInstance.value && graphInstance.value.resize()
  })
}

// 重置图表
const resetKnowledgeGraph = () => {
  if (graphInstance.value) {
    graphInstance.value.dispose()
    graphInstance.value = null
  }
}

// 过滤三元组
const filterTriples = () => {
  if (!searchInputValue.value.trim()) {
    isFiltering.value = false
    return
  }
  
  const keyword = searchInputValue.value.toLowerCase()
  filteredTriples.value = currentKnowledgeGraph.value.triples.filter(triple => 
    triple.some(item => item.toLowerCase().includes(keyword))
  )
  
  isFiltering.value = true
}

// 导出三元组数据
const exportTriples = () => {
  if (!currentKnowledgeGraph.value || !currentKnowledgeGraph.value.triples.length) {
    ElMessage.warning('没有可导出的数据')
    return
  }
  
  // 生成CSV内容
  const csvContent = [
    '实体1,关系,实体2',
    ...currentKnowledgeGraph.value.triples.map(triple => triple.join(','))
  ].join('\n')
  
  // 创建Blob
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  
  // 创建下载链接
  const link = document.createElement('a')
  link.setAttribute('href', url)
  link.setAttribute('download', 'knowledge_graph_data.csv')
  document.body.appendChild(link)
  
  // 触发下载
  link.click()
  
  // 清理
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// 组件卸载前清理资源
onUnmounted(() => {
  if (graphInstance.value) {
    graphInstance.value.dispose()
    graphInstance.value = null
  }
})

// 添加知识点学习相关变量
const currentLearningRag = ref(null)
const currentKnowledgePoint = ref(null)
const knowledgePointIndex = ref(0)
const totalKnowledgePoints = ref(0)
const learningProgress = ref(0)
const learnedPoints = ref([])

// 从localStorage获取学习进度
const getLearningProgress = (ragId) => {
  try {
    const storedProgress = localStorage.getItem('ragLearningProgress')
    if (storedProgress) {
      const progress = JSON.parse(storedProgress)
      if (progress[ragId]) {
        return progress[ragId]
      }
    }
    return { currentIndex: 0, learnedPoints: [] }
  } catch (error) {
    console.error('获取学习进度失败:', error)
    return { currentIndex: 0, learnedPoints: [] }
  }
}

// 保存学习进度到localStorage
const saveLearningProgress = (ragId, currentIndex, learnedPointsArray) => {
  try {
    let progress = {}
    const storedProgress = localStorage.getItem('ragLearningProgress')
    if (storedProgress) {
      progress = JSON.parse(storedProgress)
    }
    
    progress[ragId] = {
      currentIndex,
      learnedPoints: Array.from(new Set(learnedPointsArray))
    }
    
    localStorage.setItem('ragLearningProgress', JSON.stringify(progress))
  } catch (error) {
    console.error('保存学习进度失败:', error)
  }
}

// 开始学习知识点
const startLearningKnowledgePoints = (rag) => {
  currentLearningRag.value = rag
  
  // 如果没有加载知识图谱数据，先加载
  if (!currentKnowledgeGraph.value?.triples || currentKnowledgeGraph.value.triples.length === 0) {
    loadKnowledgeGraphData(rag.id).then(() => {
      initializeKnowledgePoints(rag)
    })
  } else {
    initializeKnowledgePoints(rag)
  }
  
  // 切换到知识点学习标签页
  continueActiveTab.value = 'learning'
}

// 加载知识图谱数据
const loadKnowledgeGraphData = async (ragId) => {
  try {
    const response = await axios.get(`http://localhost:8080/api/rag/knowledge-graph/${ragId}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    
    if (response.data && response.data.success) {
      currentKnowledgeGraph.value = response.data.data
      return true
    } else {
      ElMessage.error(response.data?.message || '获取知识图谱数据失败')
      return false
    }
  } catch (error) {
    console.error('获取知识图谱数据失败:', error)
    ElMessage.error('获取知识图谱数据失败')
    return false
  }
}

// 初始化知识点学习
const initializeKnowledgePoints = (rag) => {
  if (!currentKnowledgeGraph.value?.triples || currentKnowledgeGraph.value.triples.length === 0) {
    ElMessage.warning('没有可用的知识点')
    return
  }
  
  // 获取保存的学习进度
  const progress = getLearningProgress(rag.id)
  knowledgePointIndex.value = progress.currentIndex || 0
  learnedPoints.value = progress.learnedPoints || []
  
  // 确保索引在有效范围内
  if (knowledgePointIndex.value >= currentKnowledgeGraph.value.triples.length) {
    knowledgePointIndex.value = 0
  }
  
  // 更新当前知识点和总数
  totalKnowledgePoints.value = currentKnowledgeGraph.value.triples.length
  currentKnowledgePoint.value = currentKnowledgeGraph.value.triples[knowledgePointIndex.value]
  
  // 计算进度百分比
  updateLearningProgress()
}

// 更新学习进度
const updateLearningProgress = () => {
  learningProgress.value = Math.round((learnedPoints.value.length / totalKnowledgePoints.value) * 100)
}

// 继续学习下一个知识点
const nextKnowledgePoint = () => {
  // 标记当前知识点为已学习
  if (!learnedPoints.value.includes(knowledgePointIndex.value)) {
    learnedPoints.value.push(knowledgePointIndex.value)
  }
  
  // 更新当前索引
  knowledgePointIndex.value = (knowledgePointIndex.value + 1) % totalKnowledgePoints.value
  
  // 更新当前知识点
  currentKnowledgePoint.value = currentKnowledgeGraph.value.triples[knowledgePointIndex.value]
  
  // 更新并保存学习进度
  updateLearningProgress()
  saveLearningProgress(currentLearningRag.value.id, knowledgePointIndex.value, learnedPoints.value)
}

// 查看上一个知识点
const prevKnowledgePoint = () => {
  // 更新当前索引
  knowledgePointIndex.value = (knowledgePointIndex.value - 1 + totalKnowledgePoints.value) % totalKnowledgePoints.value
  
  // 更新当前知识点
  currentKnowledgePoint.value = currentKnowledgeGraph.value.triples[knowledgePointIndex.value]
  
  // 保存学习进度
  saveLearningProgress(currentLearningRag.value.id, knowledgePointIndex.value, learnedPoints.value)
}

// 重置学习进度
const resetLearningProgress = () => {
  if (!currentLearningRag.value) return
  
  ElMessageBox.confirm('确定要重置学习进度吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    learnedPoints.value = []
    knowledgePointIndex.value = 0
    currentKnowledgePoint.value = currentKnowledgeGraph.value.triples[knowledgePointIndex.value]
    updateLearningProgress()
    saveLearningProgress(currentLearningRag.value.id, knowledgePointIndex.value, learnedPoints.value)
    ElMessage.success('学习进度已重置')
  }).catch(() => {})
}

// 格式化三元组为可读文本
const formatTripleText = (triple) => {
  if (!triple || triple.length < 3) return '无效知识点'
  
  const [subject, relation, object] = triple
  return `${subject} ${relation} ${object}`
}

// 判断知识点是否已学习
const isKnowledgePointLearned = (index) => {
  return learnedPoints.value.includes(index)
}

// 问问AI关于当前知识点
const askAIAboutKnowledgePoint = () => {
  if (!currentKnowledgePoint.value) {
    ElMessage.warning('当前没有选中的知识点')
    return
  }

  const knowledgeText = formatTripleText(currentKnowledgePoint.value)
  const prompt = `你好，请详细解释一下这个知识点：\n\n\`\`\`\n${knowledgeText}\n\`\`\`\n\n请说明它的定义、相关概念以及一个具体的例子来帮助我理解。`

  router.push({
    path: '/manage/deepseek',
    query: {
      prompt: prompt
    }
  })
}

// ==================== 问题生成相关 ====================
const generateQuestionDialogVisible = ref(false)
const generateQuestionForm = reactive({
  questionType: ''
})
const generatingQuestionLoading = ref(false)
const questionTypes = ref([])
const questionTypesLoading = ref(false)

// 获取题目类型
const getQuestionTypes = async () => {
  if (questionTypes.value.length > 0) return
  
  questionTypesLoading.value = true
  try {
    const response = await axios.get('http://localhost:8080/api/question-generator/types', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
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
    questionTypesLoading.value = false
  }
}

// 打开生成题目对话框
const handleGenerateQuestion = () => {
  // 重置表单
  generateQuestionForm.questionType = ''
  // 获取题目类型
  getQuestionTypes()
  // 打开对话框
  generateQuestionDialogVisible.value = true
}

// 提交生成题目任务
const submitGenerateQuestion = async () => {
  if (!currentLearningRag.value || !currentKnowledgePoint.value) {
    ElMessage.error('无法获取当前的知识库或知识点信息')
    return
  }
  
  generatingQuestionLoading.value = true
  try {
    const requestData = {
      query: formatTripleText(currentKnowledgePoint.value),
      ragName: currentLearningRag.value.name,
      questionType: generateQuestionForm.questionType || undefined
    }
    
    const response = await axios.post('http://localhost:8080/api/question-generator/generate-with-rag-name', requestData, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    
    if (response.data && response.data.success) {
      ElMessage.success('题目生成任务已提交，请稍后到问题生成页面查看')
      generateQuestionDialogVisible.value = false
    } else {
      ElMessage.error(response.data?.message || '提交题目生成任务失败')
    }
  } catch (error) {
    console.error('提交题目生成任务失败:', error)
    ElMessage.error(error.response?.data?.message || '提交题目生成任务失败')
  } finally {
    generatingQuestionLoading.value = false
  }
}
</script>

<template>
  <div class="container">
    <div class="page-header">
      <h1>我的课程</h1>
      <p>浏览和学习已选课程，查看学习进度</p>
    </div>
    
    <el-card class="search-card">
      <div class="flex flex-wrap gap-md items-center">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索课程名称"
          clearable
          @keyup.enter="filterCourses"
          style="width: 250px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select v-model="filterStatus" placeholder="学习状态" clearable @change="filterCourses" style="width: 150px">
          <el-option label="全部" value="" />
          <el-option label="进行中" value="IN_PROGRESS" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="未开始" value="NOT_STARTED" />
        </el-select>
        
        <el-radio-group v-model="viewMode" @change="filterCourses">
          <el-radio-button label="enrolled">已选课程</el-radio-button>
          <el-radio-button label="available">可选课程</el-radio-button>
        </el-radio-group>
      </div>
    </el-card>
    
    <div v-loading="loading" class="mt-lg">
      <!-- 课程列表为空时的提示 -->
      <el-empty
        v-if="filteredCourses.length === 0"
        description="暂无符合条件的课程"
      >
        <template #image>
          <el-icon :size="60"><DocumentDelete /></el-icon>
        </template>
        <el-button v-if="viewMode === 'available'" type="primary" @click="openEnrollDialog">查看选课</el-button>
      </el-empty>
      
      <!-- 课程卡片网格 -->
      <div v-else class="grid grid-auto">
        <el-card
          v-for="course in filteredCourses"
          :key="course.courseId"
          class="card-item"
          shadow="hover"
        >
          <div class="course-header">
            <h3>{{ course.name }}</h3>
            <el-tag 
              v-if="course.status" 
              :type="getCourseStatusType(course.status)"
              size="small"
            >
              {{ getCourseStatusDisplay(course.status) }}
            </el-tag>
          </div>
          
          <div class="course-body">
            <p class="course-desc">{{ course.description }}</p>
            
            <div v-if="course.status" class="mb-md">
              <span class="text-secondary text-sm">学习进度:</span>
              <el-progress :percentage="course.progress || 0" :status="course.progress === 100 ? 'success' : ''"></el-progress>
            </div>
            
            <div class="course-meta flex justify-between">
              <div class="meta-item">
                <el-icon><Calendar /></el-icon>
                <span>{{ formatDate(course.createdAt) }}</span>
              </div>
              <div v-if="course.lastAccessed" class="meta-item">
                <el-icon><Timer /></el-icon>
                <span>{{ formatTimeAgo(course.lastAccessed) }}</span>
              </div>
            </div>
          </div>
          
          <div class="course-actions">
            <el-button 
              v-if="!course.status" 
              type="primary" 
              size="small" 
              @click="enrollCourse(course.courseId)"
            >
              选择课程
            </el-button>
            
            <template v-else>
              <el-button type="success" size="small" @click="enterCourse(course)">
                继续学习
              </el-button>
              <el-button type="info" link @click="viewCourseDetail(course)">
                详情
              </el-button>
            </template>
          </div>
        </el-card>
      </div>
    </div>
    
    <!-- 选课对话框 -->
    <el-dialog v-model="enrollDialogVisible" title="课程选择" width="80%" fullscreen>
      <div class="enroll-dialog-content">
        <div class="enroll-filter">
          <el-input
            v-model="enrollSearchKeyword"
            placeholder="搜索课程名称或描述"
            clearable
            style="width: 300px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          
          <el-select v-model="enrollFilterCategory" placeholder="课程分类" clearable style="width: 200px">
            <el-option label="全部分类" value="" />
            <el-option
              v-for="category in courseCategories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </div>
        
        <el-tabs v-model="activeTabName" class="category-tabs">
          <el-tab-pane label="全部课程" name="all"></el-tab-pane>
          <el-tab-pane label="推荐课程" name="recommended"></el-tab-pane>
          <el-tab-pane label="热门课程" name="popular"></el-tab-pane>
          <el-tab-pane label="最新课程" name="newest"></el-tab-pane>
        </el-tabs>
        
        <div v-loading="enrollLoading" class="grid grid-auto">
          <el-card
            v-for="course in filteredAvailableCourses"
            :key="course.courseId"
            class="card-item"
            @click="viewCourseDetail(course)"
          >
            <div class="course-header">
              <h3>{{ course.name }}</h3>
            </div>
            
            <p class="course-desc">{{ course.description }}</p>
            
            <div class="course-meta flex justify-between">
              <div class="meta-item">
                <el-icon><User /></el-icon>
                <span>{{ course.studentCount || 0 }}人已选</span>
              </div>
              <div class="meta-item">
                <el-icon><Calendar /></el-icon>
                <span>{{ formatDate(course.createdAt) }}</span>
              </div>
            </div>
            
            <div class="course-actions">
              <el-button type="primary" @click.stop="enrollCourse(course.courseId)">
                选择课程
              </el-button>
              <el-button @click.stop="viewCourseDetail(course)">
                详情
              </el-button>
            </div>
          </el-card>
        </div>
        
        <div v-if="filteredAvailableCourses.length === 0" class="empty-state">
          <el-empty description="暂无可选课程"></el-empty>
        </div>
      </div>
    </el-dialog>
    
    <!-- 课程详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="selectedCourseForDetail ? selectedCourseForDetail.name : '课程详情'"
      width="80%"
    >
      <el-tabs v-model="activeTab">
        <el-tab-pane label="课程详情" name="info">
          <div v-loading="detailLoading" class="course-detail-content">
            <template v-if="selectedCourseForDetail">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="课程名称">
                  {{ selectedCourseForDetail.name }}
                </el-descriptions-item>
                <el-descriptions-item label="课程描述">
                  {{ selectedCourseForDetail.description }}
                </el-descriptions-item>
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
              
              <div v-if="!selectedCourseForDetail.status" class="flex justify-center mt-lg">
                <el-button type="primary" @click="enrollCourse(selectedCourseForDetail.courseId)">
                  选择此课程
                </el-button>
              </div>
              
              <div v-else class="flex justify-center mt-lg">
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
                  <div class="file-item flex items-center">
                    <el-icon class="mr-sm"><Document /></el-icon>
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
        
        <!-- 新增知识库标签页 -->
        <el-tab-pane label="知识库" name="knowledge">
          <div v-loading="ragLoading" class="knowledge-base-content">
            <el-empty v-if="ragList.length === 0" description="暂无关联的知识库">
              <template #image>
                <el-icon :size="60"><DataAnalysis /></el-icon>
              </template>
            </el-empty>
            
            <div v-else>
              <h3>课程关联知识库</h3>
              <p class="text-secondary">这些知识库包含课程相关的知识点和资料，可用于知识查询</p>
              
              <el-card v-for="rag in ragList" :key="rag.id" class="mb-md">
                <template #header>
                  <div class="flex justify-between items-center">
                    <span>{{ rag.name }}</span>
                    <el-tag type="success">{{ rag.status }}</el-tag>
                  </div>
                </template>
                
                <p v-if="rag.description" class="mb-md">{{ rag.description }}</p>
                <div class="rag-actions">
                  <el-button type="primary" @click="openQueryDialog(rag)">
                    <el-icon><Search /></el-icon> 知识查询
                  </el-button>
                  <el-button type="info" @click="viewKnowledgeGraph(rag)">
                    <el-icon><DataAnalysis /></el-icon> 查看知识图谱
                  </el-button>
                </div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
    
    <!-- 查询RAG对话框 -->
    <el-dialog 
      v-model="queryDialogVisible" 
      title="知识库查询"
      width="800px"
      append-to-body
    >
      <el-form 
        ref="queryFormRef"
        :model="queryForm"
        :rules="queryRules"
        label-width="120px"
      >
        <el-form-item label="查询内容" prop="query">
          <el-input 
            v-model="queryForm.query" 
            placeholder="请输入查询内容"
            type="textarea"
            :rows="3" 
          />
        </el-form-item>
        <el-form-item label="返回结果数" prop="topK">
          <el-input-number v-model="queryForm.topK" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="包含图谱上下文" prop="includeGraphContext">
          <el-switch v-model="queryForm.includeGraphContext" />
        </el-form-item>
        <el-form-item label="上下文深度" prop="contextDepth" v-if="queryForm.includeGraphContext">
          <el-input-number v-model="queryForm.contextDepth" :min="1" :max="5" />
        </el-form-item>
      </el-form>
      
      <el-button type="primary" @click="submitQueryForm(queryFormRef)">查询</el-button>
      
      <!-- 查询结果展示 -->
      <div v-if="queryResults && queryResults.results" class="query-results">
        <h3>查询结果</h3>
        <div class="query-info">
          <p><strong>查询内容:</strong> {{ queryResults.query }}</p>
          <p><strong>消息:</strong> {{ queryResults.message }}</p>
        </div>
        <el-card v-for="(result, index) in queryResults.results" :key="index" class="result-card">
          <div class="result-header">
            <span class="result-rank">排名: {{ result.rank }}</span>
            <span class="result-similarity">匹配度: {{ (result.similarity * 100).toFixed(2) }}%</span>
          </div>
          <div class="result-triple">
            <h4>三元组</h4>
            <p>{{ result.triple }}</p>
          </div>
          <div class="result-details">
            <div class="detail-item">
              <strong>主体:</strong> {{ result.subject }}
            </div>
            <div class="detail-item">
              <strong>关系:</strong> {{ result.relation }}
            </div>
            <div class="detail-item">
              <strong>客体:</strong> {{ result.object }}
            </div>
          </div>
          
          <!-- 显示入向关系 -->
          <div v-if="result.incoming_relations && result.incoming_relations.length > 0" class="relations-section">
            <h4>入向关系</h4>
            <div v-for="(rel, idx) in result.incoming_relations" :key="`in-${idx}`" class="relation-item">
              {{ rel.subject }} {{ rel.relation }} {{ rel.object }}
            </div>
          </div>
          
          <!-- 显示出向关系 -->
          <div v-if="result.outgoing_relations && result.outgoing_relations.length > 0" class="relations-section">
            <h4>出向关系</h4>
            <div v-for="(rel, idx) in result.outgoing_relations" :key="`out-${idx}`" class="relation-item">
              {{ rel.subject }} {{ rel.relation }} {{ rel.object }}
            </div>
          </div>
        </el-card>
        
        <!-- 知识图谱上下文展示 -->
        <div v-if="queryResults.graphContext" class="graph-context">
          <h3>知识图谱上下文</h3>
          <pre>{{ JSON.stringify(queryResults.graphContext, null, 2) }}</pre>
        </div>
      </div>
    </el-dialog>

    <!-- 知识图谱对话框 -->
    <el-dialog
      v-model="knowledgeGraphDialogVisible"
      title="知识图谱可视化"
      fullscreen
      append-to-body
      destroy-on-close
      @close="resetKnowledgeGraph"
    >
      <div class="knowledge-graph-container">
        <div class="knowledge-graph-header">
          <div class="kg-info">
            <h3>知识图谱信息</h3>
            <p>
              <strong>三元组数量:</strong> {{ currentKnowledgeGraph.totalCount }}
            </p>
            <p>
              <strong>知识图谱路径:</strong> {{ currentKnowledgeGraph.knowledgeGraphPath }}
            </p>
          </div>
          <div class="kg-actions">
            <el-input
              v-model="searchInputValue"
              placeholder="搜索三元组"
              clearable
              @keyup.enter="filterTriples"
            >
              <template #append>
                <el-button @click="filterTriples">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
            <el-button type="primary" @click="exportTriples">导出数据</el-button>
          </div>
        </div>
        
        <el-divider />
        
        <div class="knowledge-graph-tabs">
          <el-tabs type="border-card" @tab-change="(tab) => { if(tab === '0') nextTick(() => initKnowledgeGraph()) }">
            <el-tab-pane label="图形视图">
              <div ref="graphChart" class="knowledge-graph-chart"></div>
              <div class="graph-help-text">
                <p>操作提示: 鼠标滚轮可缩放, 拖拽可移动视图, 点击节点可查看详情</p>
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="表格视图">
              <el-table
                :data="isFiltering ? filteredTriples : currentKnowledgeGraph.triples"
                style="width: 100%"
                border
                stripe
                height="500px"
              >
                <el-table-column label="实体1" prop="0" min-width="33%" />
                <el-table-column label="关系" prop="1" min-width="33%" />
                <el-table-column label="实体2" prop="2" min-width="33%" />
              </el-table>
              
              <div v-if="isFiltering" class="filter-info">
                <p>已过滤: 显示 {{ filteredTriples.length }} 条匹配结果</p>
                <el-button size="small" type="info" @click="isFiltering = false; searchInputValue = ''">
                  清除过滤
                </el-button>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </el-dialog>
    
    <!-- 继续学习对话框 -->
    <el-dialog
      v-model="continueDialogVisible"
      :title="selectedCourseForDetail ? selectedCourseForDetail.name : '继续学习'"
      width="80%"
    >
      <el-tabs v-model="continueActiveTab">
        <el-tab-pane label="课程资源" name="resources">
          <div v-loading="courseFilesLoading" class="course-resources-content">
            <el-empty v-if="courseFiles.length === 0" description="暂无可用的课程资源" />
            
            <el-table v-else :data="courseFiles" style="width: 100%">
              <el-table-column label="文件名" min-width="200">
                <template #default="scope">
                  <div class="file-item flex items-center">
                    <el-icon class="mr-sm"><Document /></el-icon>
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
        
        <!-- 新增知识库标签页 -->
        <el-tab-pane label="知识库" name="knowledge">
          <div v-loading="ragLoading" class="knowledge-base-content">
            <el-empty v-if="ragList.length === 0" description="暂无关联的知识库">
              <template #image>
                <el-icon :size="60"><DataAnalysis /></el-icon>
              </template>
            </el-empty>
            
            <div v-else>
              <h3>课程关联知识库</h3>
              <p class="text-secondary">这些知识库包含课程相关的知识点和资料，可用于知识查询</p>
              
              <el-card v-for="rag in ragList" :key="rag.id" class="mb-md">
                <template #header>
                  <div class="flex justify-between items-center">
                    <span>{{ rag.name }}</span>
                    <el-tag type="success">{{ rag.status }}</el-tag>
                  </div>
                </template>
                
                <p v-if="rag.description" class="mb-md">{{ rag.description }}</p>
                <div class="rag-actions">
                  <el-button type="primary" @click="openQueryDialog(rag)">
                    <el-icon><Search /></el-icon> 知识查询
                  </el-button>
                  <el-button type="info" @click="viewKnowledgeGraph(rag)">
                    <el-icon><DataAnalysis /></el-icon> 查看知识图谱
                  </el-button>
                </div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>

        <!-- 新增知识点学习标签页 -->
        <el-tab-pane label="知识点学习" name="learning">
          <div class="learning-container">
            <div v-if="!currentLearningRag" class="select-rag-container">
              <h3 class="text-xl mb-md">选择知识库开始学习</h3>
              <p class="text-secondary mb-lg">选择一个知识库，开始系统学习其中的知识点</p>
              
              <el-empty v-if="ragList.length === 0" description="暂无关联的知识库">
                <template #image>
                  <el-icon :size="60"><Reading /></el-icon>
                </template>
              </el-empty>
              
              <div v-else class="rag-selection">
                <el-card v-for="rag in ragList" :key="rag.id" class="mb-md rag-card">
                  <template #header>
                    <div class="flex justify-between items-center">
                      <span>{{ rag.name }}</span>
                      <el-tag type="success">{{ rag.status }}</el-tag>
                    </div>
                  </template>
                  
                  <p v-if="rag.description" class="mb-md">{{ rag.description }}</p>
                  <el-button type="primary" @click="startLearningKnowledgePoints(rag)">
                    <el-icon><Reading /></el-icon> 开始学习
                  </el-button>
                </el-card>
              </div>
            </div>
            
            <div v-else class="knowledge-learning-container">
              <div class="learning-header">
                <div class="flex justify-between items-center mb-md">
                  <h3 class="text-xl">{{ currentLearningRag.name }}</h3>
                  <el-button type="text" @click="currentLearningRag = null">
                    选择其他知识库
                  </el-button>
                </div>
                
                <el-progress 
                  :percentage="learningProgress" 
                  :format="() => `${learnedPoints.length}/${totalKnowledgePoints}`" 
                  class="mb-md"
                ></el-progress>
                
                <div class="text-sm text-secondary mb-md">
                  当前知识点: {{ knowledgePointIndex + 1 }}/{{ totalKnowledgePoints }}
                  <el-tag v-if="isKnowledgePointLearned(knowledgePointIndex)" size="small" type="success" class="ml-sm">
                    已学习
                  </el-tag>
                </div>
              </div>
              
              <el-card class="knowledge-card">
                <template #header>
                  <div class="flex justify-between items-center">
                    <span class="text-lg">知识点</span>
                  </div>
                </template>
                
                <div v-if="currentKnowledgePoint" class="knowledge-content">
                  <div class="triple-container">
                    <div class="triple-entity">{{ currentKnowledgePoint[0] }}</div>
                    <div class="triple-relation">{{ currentKnowledgePoint[1] }}</div>
                    <div class="triple-entity">{{ currentKnowledgePoint[2] }}</div>
                  </div>
                  
                  <div class="knowledge-explanation mt-lg">
                    <p class="mb-md">知识解释:</p>
                    <div class="explanation-content">
                      {{ formatTripleText(currentKnowledgePoint) }}
                    </div>
                  </div>
                </div>
                
                <div v-else class="text-center py-xl">
                  <el-empty description="没有可用的知识点"></el-empty>
                </div>
              </el-card>
              
              <div class="navigation-controls mt-lg flex justify-between">
                <el-button type="primary" plain :icon="ArrowLeft" @click="prevKnowledgePoint">
                  上一个
                </el-button>
                
                <div class="action-controls">
                  <el-button type="danger" plain @click="resetLearningProgress">
                    重置进度
                  </el-button>
                  <el-button type="success" @click="handleGenerateQuestion">
                    <el-icon><Plus /></el-icon> 生成题目
                  </el-button>
                  <el-button type="primary" @click="askAIAboutKnowledgePoint">
                    <el-icon><ChatLineSquare /></el-icon> 问问AI
                  </el-button>
                </div>
                
                <el-button type="primary" @click="nextKnowledgePoint">
                  下一个 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      
      <template #footer>
        <el-button @click="continueDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 生成题目对话框 -->
    <el-dialog
      v-model="generateQuestionDialogVisible"
      title="生成题目"
      width="500px"
      append-to-body
    >
      <div v-if="currentKnowledgePoint">
        <p>将根据以下知识点生成题目：</p>
        <el-alert
          :title="formatTripleText(currentKnowledgePoint)"
          type="info"
          :closable="false"
          class="mb-md"
        />
        <el-form :model="generateQuestionForm" label-width="80px">
          <el-form-item label="题目类型" prop="questionType">
            <el-select 
              v-model="generateQuestionForm.questionType" 
              placeholder="请选择题目类型（可选）" 
              style="width: 100%"
              clearable
              :loading="questionTypesLoading"
            >
              <el-option 
                v-for="type in questionTypes" 
                :key="type" 
                :label="type" 
                :value="type" 
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="generateQuestionDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="submitGenerateQuestion" 
          :loading="generatingQuestionLoading"
        >
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.course-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.course-header h3 {
  font-size: var(--text-lg);
  margin: 0;
  font-weight: 500;
}

.course-body {
  flex-grow: 1;
}

.course-desc {
  color: var(--text-secondary);
  font-size: var(--text-sm);
  margin-bottom: var(--spacing-md);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-meta {
  margin-bottom: var(--spacing-md);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  color: var(--text-tertiary);
  font-size: var(--text-xs);
}

.course-actions {
  display: flex;
  justify-content: space-between;
  margin-top: var(--spacing-md);
}

.empty-state {
  padding: var(--spacing-xl);
  display: flex;
  justify-content: center;
}

.file-item {
  color: var(--text-primary);
}

.file-name {
  margin-left: var(--spacing-sm);
}

/* 选课对话框样式 */
.enroll-dialog-content {
  min-height: 50vh;
}

.enroll-filter {
  margin-bottom: var(--spacing-lg);
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-lg);
}

.category-tabs {
  margin-bottom: var(--spacing-md);
  overflow-x: auto;
}

/* 继续学习对话框样式 */
.continue-dialog-content {
  min-height: 50vh;
}

.continue-filter {
  margin-bottom: var(--spacing-lg);
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-lg);
}

.continue-category-tabs {
  margin-bottom: var(--spacing-md);
  overflow-x: auto;
}

.course-resources-content {
  padding: var(--spacing-md);
}

.knowledge-base-content {
  padding: var(--spacing-md);
}

.rag-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.query-results {
  margin-top: 20px;
}

.query-info {
  margin-bottom: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.result-card {
  margin-bottom: 15px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 5px;
  border-bottom: 1px solid #ebeef5;
}

.result-rank {
  font-weight: bold;
}

.result-similarity {
  color: #409EFF;
}

.result-triple {
  margin-bottom: 10px;
  padding: 10px;
  background-color: #f0f9eb;
  border-radius: 4px;
}

.result-triple h4 {
  margin-top: 0;
  margin-bottom: 5px;
  color: #67c23a;
}

.result-details {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 10px;
}

.detail-item {
  flex: 1;
  min-width: 150px;
}

.relations-section {
  margin-top: 10px;
  padding: 10px;
  background-color: #f4f4f5;
  border-radius: 4px;
}

.relations-section h4 {
  margin-top: 0;
  margin-bottom: 8px;
  color: #606266;
}

.relation-item {
  padding: 5px;
  margin-bottom: 5px;
  background-color: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
}

.graph-context {
  margin-top: 20px;
}

.knowledge-graph-container {
  width: 100%;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.knowledge-graph-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.kg-info {
  flex: 1;
  min-width: 300px;
}

.kg-info h3 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #409EFF;
}

.kg-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.knowledge-graph-tabs {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.knowledge-graph-chart {
  width: 100%;
  height: 600px;
  min-height: 600px;
}

.graph-help-text {
  text-align: center;
  margin-top: 15px;
  color: #909399;
  font-size: 14px;
}

.filter-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding: 5px 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

/* 知识点学习样式 */
.learning-container {
  padding: var(--spacing-md);
  min-height: 500px;
}

.select-rag-container {
  text-align: center;
}

.rag-selection {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.rag-card {
  transition: all 0.3s ease;
}

.rag-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px rgba(0,0,0,0.1);
}

.knowledge-learning-container {
  display: flex;
  flex-direction: column;
}

.triple-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.triple-entity {
  flex: 1;
  padding: 15px;
  background-color: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 4px;
  font-weight: bold;
  text-align: center;
}

.triple-relation {
  padding: 10px 15px;
  background-color: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 4px;
  min-width: 100px;
  text-align: center;
  font-style: italic;
}

.knowledge-card {
  flex: 1;
  margin-bottom: 20px;
}

.knowledge-content {
  padding: 10px 0;
}

.explanation-content {
  background-color: #f5f7fa;
  border-left: 4px solid #409EFF;
  padding: 15px;
  border-radius: 0 4px 4px 0;
}

.navigation-controls {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-controls {
  display: flex;
  gap: 10px;
}
</style> 