<script setup>
import { ref, onMounted, reactive, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, RefreshRight, DataAnalysis, Document, Folder } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'

const router = useRouter()
const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// RAG列表数据
const ragList = ref([])
const loading = ref(false)
const total = ref(0)

// 表单引用
const ragFormRef = ref(null)
const generateFormRef = ref(null)
const generateFromFileFormRef = ref(null)
const queryFormRef = ref(null)

// 分页参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

// 表单数据
const ragForm = reactive({
  id: undefined,
  name: '',
  description: ''
})

// 对话框控制
const dialogVisible = ref(false)
const dialogTitle = ref('创建RAG')
const formMode = ref('add')

// 生成RAG对话框控制
const generateDialogVisible = ref(false)
const generateForm = reactive({
  sourceDir: '',
  ragName: '',
  forceRebuild: false
})

// 从文件生成RAG对话框控制
const generateFromFileDialogVisible = ref(false)
const generateFromFileForm = reactive({
  folderId: null,
  ragName: '',
  forceRebuild: false
})

// 文件夹列表
const folderList = ref([])
const currentPath = ref('')

// 生成任务列表
const generationTasks = ref([])
const taskRefreshInterval = ref(null)

// 查询对话框控制
const queryDialogVisible = ref(false)
const queryForm = reactive({
  query: '',
  ragId: null,
  topK: 5,
  includeGraphContext: false,
  contextDepth: 1
})
const queryResults = ref(null)

// 新增知识图谱数据和控制变量
const knowledgeGraphData = ref(null)
const knowledgeGraphDialogVisible = ref(false)
const currentKnowledgeGraph = ref({
  triples: [],
  totalCount: 0,
  knowledgeGraphPath: ''
})
const graphChart = ref(null)
const graphInstance = ref(null)

// 表单校验规则
const rules = {
  name: [
    { required: true, message: '请输入RAG名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  description: [
    { max: 200, message: '长度不能超过 200 个字符', trigger: 'blur' }
  ]
}

const generateRules = {
  sourceDir: [
    { required: true, message: '请输入源目录路径', trigger: 'blur' }
  ],
  ragName: [
    { required: true, message: '请输入RAG名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}

const generateFromFileRules = {
  folderId: [
    { required: true, message: '请选择文件夹', trigger: 'change' }
  ],
  ragName: [
    { required: true, message: '请输入RAG名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}

const queryRules = {
  query: [
    { required: true, message: '请输入查询内容', trigger: 'blur' }
  ]
}

// 获取RAG列表
const getRAGList = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${BaseUrl}api/rag`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      ragList.value = response.data.data || []
      total.value = ragList.value.length
      
      // 如果有关键词，前端过滤
      if (queryParams.keyword) {
        const keyword = queryParams.keyword.toLowerCase()
        ragList.value = ragList.value.filter(rag => 
          (rag.name && rag.name.toLowerCase().includes(keyword)) ||
          (rag.description && rag.description.toLowerCase().includes(keyword))
        )
        total.value = ragList.value.length
      }
    } else {
      ElMessage.error(response.data?.message || '获取RAG列表失败')
    }
  } catch (error) {
    console.error('获取RAG列表失败:', error)
    ElMessage.error('获取RAG列表失败')
  } finally {
    loading.value = false
  }
}

// 获取根目录文件夹列表
const getRootFolders = async () => {
  try {
    const response = await axios.get(`${BaseUrl}api/rag/folders`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      folderList.value = response.data.data || []
      currentPath.value = ''
      console.log('获取的文件夹列表:', folderList.value)
    } else {
      ElMessage.error(response.data?.message || '获取文件夹列表失败')
    }
  } catch (error) {
    console.error('获取文件夹列表失败:', error)
    ElMessage.error('获取文件夹列表失败')
  }
}

// 获取指定路径的文件夹列表
const getFoldersByPath = async (path) => {
  try {
    const response = await axios.get(`${BaseUrl}api/rag/folders/${path}`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      folderList.value = response.data.data || []
      currentPath.value = path
    } else {
      ElMessage.error(response.data?.message || '获取文件夹列表失败')
    }
  } catch (error) {
    console.error('获取文件夹列表失败:', error)
    ElMessage.error('获取文件夹列表失败')
  }
}

// 文件夹选择函数
const selectFolder = (row) => {
  console.log('选择的行数据:', row)
  
  if (!row.directory) {
    ElMessage.warning('只能选择文件夹')
    return
  }
  
  // 如果已经选择了这个文件夹，则取消选择
  if (generateFromFileForm.folderId === row.id) {
    generateFromFileForm.folderId = null
    ElMessage.info('已取消选择')
    return
  }
  
  generateFromFileForm.folderId = row.id
  // 可以将文件夹名称自动填入RAG名称
  if (!generateFromFileForm.ragName) {
    generateFromFileForm.ragName = row.fileName
  }
  ElMessage.success(`已选择文件夹: ${row.fileName}`)
  
  // 打印选择的文件夹信息，便于调试
  console.log('选择的文件夹:', row)
}

// 为表格行添加自定义样式
const tableRowClassName = ({ row }) => {
  if (generateFromFileForm.folderId === row.id) {
    return 'selected-row'
  }
  return row.directory ? 'directory-row' : ''
}

// 打开添加RAG对话框
const handleAdd = () => {
  resetForm()
  dialogTitle.value = '创建RAG'
  formMode.value = 'add'
  dialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  ragForm.id = undefined
  ragForm.name = ''
  ragForm.description = ''
}

// 重置生成RAG表单
const resetGenerateForm = () => {
  generateForm.sourceDir = ''
  generateForm.ragName = ''
  generateForm.forceRebuild = false
}

// 重置从文件生成RAG表单
const resetGenerateFromFileForm = () => {
  generateFromFileForm.folderId = null
  generateFromFileForm.ragName = ''
  generateFromFileForm.forceRebuild = false
  
  // 调试信息，确认当前状态
  console.log('重置表单后的状态:', {
    folderId: generateFromFileForm.folderId,
    ragName: generateFromFileForm.ragName,
    folderList: folderList.value
  })
}

// 重置查询表单
const resetQueryForm = () => {
  queryForm.query = ''
  queryForm.ragId = null
  queryForm.topK = 5
  queryForm.includeGraphContext = false
  queryForm.contextDepth = 1
  queryResults.value = null
}

// 打开生成RAG对话框
const handleGenerate = () => {
  resetGenerateForm()
  generateDialogVisible.value = true
}

// 打开从文件生成RAG对话框
const handleGenerateFromFile = () => {
  resetGenerateFromFileForm()
  getRootFolders()
  generateFromFileDialogVisible.value = true
}

// 打开查询RAG对话框
const handleQuery = (row) => {
  resetQueryForm()
  queryForm.ragId = row.id
  queryDialogVisible.value = true
}

// 删除RAG
const handleDelete = (id) => {
  ElMessageBox.confirm('确认要删除该RAG吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await axios.delete(`${BaseUrl}api/rag/${id}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      
      if (response.data && response.data.success) {
        ElMessage.success('删除成功')
        getRAGList()
      } else {
        ElMessage.error(response.data?.message || '删除RAG失败')
      }
    } catch (error) {
      console.error('删除RAG失败:', error)
      ElMessage.error('删除RAG失败')
    }
  }).catch(() => {})
}

// 查看知识图谱
const viewKnowledgeGraph = async (id) => {
  try {
    loading.value = true
    const response = await axios.get(`${BaseUrl}api/rag/knowledge-graph/${id}`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    console.log('知识图谱响应数据:', response.data)
    
    if (response.data && response.data.success) {
      // 保存知识图谱数据
      currentKnowledgeGraph.value = response.data.data
      knowledgeGraphDialogVisible.value = true
      
      // 延迟初始化图表，确保DOM已经渲染
      nextTick(() => {
        initKnowledgeGraph()
      })
      
      ElMessage.success('知识图谱数据获取成功')
    } else {
      ElMessage.error(response.data?.message || '获取知识图谱数据失败')
    }
  } catch (error) {
    console.error('获取知识图谱数据失败:', error)
    ElMessage.error('获取知识图谱数据失败')
  } finally {
    loading.value = false
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

// 过滤三元组
const searchInputValue = ref('')
const filteredTriples = ref([])
const isFiltering = ref(false)

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

// 提交创建/编辑RAG表单
const submitForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const data = { ...ragForm }
        
        if (formMode.value === 'add') {
          const response = await axios.post(`${BaseUrl}api/rag`, data, {
            headers: {
              'Authorization': `Bearer ${getToken()}`
            }
          })
          
          if (response.data && response.data.success) {
            ElMessage.success('创建成功')
            dialogVisible.value = false
            getRAGList()
          } else {
            ElMessage.error(response.data?.message || '创建RAG失败')
          }
        }
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error('操作失败')
      }
    }
  })
}

// 提交生成RAG表单（异步）
const submitGenerateForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const params = {
          sourceDir: generateForm.sourceDir,
          ragName: generateForm.ragName,
          forceRebuild: generateForm.forceRebuild
        }
        
        const response = await axios.post(`${BaseUrl}api/rag/generate/async`, null, {
          params: params,
          headers: {
            'Authorization': `Bearer ${getToken()}`
          }
        })
        
        if (response.data && response.data.success) {
          ElMessage.success('RAG生成任务已启动')
          generateDialogVisible.value = false
          // 添加到任务列表
          if (response.data.data && response.data.data.taskId) {
            generationTasks.value.push({
              id: response.data.data.taskId,
              name: generateForm.ragName,
              status: response.data.data.status || 'IN_PROGRESS',
              progress: 0
            })
            // 启动任务状态检查
            startTaskStatusCheck()
          }
        } else {
          ElMessage.error(response.data?.message || '生成RAG失败')
        }
      } catch (error) {
        console.error('生成RAG失败:', error)
        ElMessage.error('生成RAG失败')
      }
    }
  })
}

// 提交从文件生成RAG表单（异步）
const submitGenerateFromFileForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const params = {
          folderId: generateFromFileForm.folderId,
          ragName: generateFromFileForm.ragName,
          forceRebuild: generateFromFileForm.forceRebuild
        }
        
        const response = await axios.post(`${BaseUrl}api/rag/generate/from-file/async`, null, {
          params: params,
          headers: {
            'Authorization': `Bearer ${getToken()}`
          }
        })
        
        if (response.data && response.data.success) {
          ElMessage.success('RAG生成任务已启动')
          generateFromFileDialogVisible.value = false
          // 添加到任务列表
          if (response.data.data && response.data.data.taskId) {
            generationTasks.value.push({
              id: response.data.data.taskId,
              name: generateFromFileForm.ragName,
              status: response.data.data.status || 'IN_PROGRESS',
              progress: 0
            })
            // 启动任务状态检查
            startTaskStatusCheck()
          }
        } else {
          ElMessage.error(response.data?.message || '生成RAG失败')
        }
      } catch (error) {
        console.error('生成RAG失败:', error)
        ElMessage.error('生成RAG失败')
      }
    }
  })
}

// 提交查询表单
const submitQueryForm = async (formEl) => {
  if (!formEl) {
    console.log('formEl is null')
    return
  }
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const params = {
          query: queryForm.query,
          topK: queryForm.topK,
          includeGraphContext: queryForm.includeGraphContext,
          contextDepth: queryForm.contextDepth
        }
        
        const response = await axios.get(`${BaseUrl}api/rag/query/${queryForm.ragId}`, {
          params: params,
          headers: {
            'Authorization': `Bearer ${getToken()}`
          }
        })

        console.log(response.data)
        
        if (response.data && response.data.success) {
          // 将返回的data直接赋值给queryResults
          queryResults.value = response.data.data || {}
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

// 启动任务状态检查
const startTaskStatusCheck = () => {
  // 清除现有的定时器
  if (taskRefreshInterval.value) {
    clearInterval(taskRefreshInterval.value)
  }
  
  // 创建新的定时器，每3秒检查一次
  taskRefreshInterval.value = setInterval(checkGenerationTasksStatus, 3000)
}

// 检查生成任务状态
const checkGenerationTasksStatus = async () => {
  // 过滤出进行中的任务
  const inProgressTasks = generationTasks.value.filter(task => task.status === 'IN_PROGRESS')
  
  // 如果没有进行中的任务，停止定时器
  if (inProgressTasks.length === 0) {
    if (taskRefreshInterval.value) {
      clearInterval(taskRefreshInterval.value)
      taskRefreshInterval.value = null
    }
    return
  }
  
  // 检查每个进行中任务的状态
  for (const task of inProgressTasks) {
    try {
      const response = await axios.get(`${BaseUrl}api/rag/generation-status/${task.id}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      
      if (response.data && response.data.success) {
        const taskData = response.data.data
        // 更新任务状态
        const taskIndex = generationTasks.value.findIndex(t => t.id === task.id)
        if (taskIndex !== -1) {
          generationTasks.value[taskIndex].status = taskData.status
          generationTasks.value[taskIndex].progress = taskData.progress || 0
          
          // 如果任务完成，刷新RAG列表
          if (taskData.status === 'COMPLETED') {
            getRAGList()
            ElMessage.success(`RAG "${task.name}" 生成完成`)
          } else if (taskData.status === 'FAILED') {
            ElMessage.error(`RAG "${task.name}" 生成失败`)
          }
        }
      }
    } catch (error) {
      console.error('检查任务状态失败:', error)
    }
  }
}

// 清除已完成的任务
const clearCompletedTasks = () => {
  generationTasks.value = generationTasks.value.filter(
    task => task.status === 'IN_PROGRESS'
  )
}

// 搜索RAG
const handleSearch = () => {
  queryParams.pageNum = 1
  getRAGList()
}

// 重置搜索条件
const resetSearch = () => {
  queryParams.keyword = ''
  queryParams.pageNum = 1
  getRAGList()
}

// 页面加载时获取RAG列表
onMounted(() => {
  getRAGList()
  
  // 启动任务状态检查（如果有未完成的任务）
  if (generationTasks.value.some(task => task.status === 'IN_PROGRESS')) {
    startTaskStatusCheck()
  }
})

// 组件销毁时清除定时器和图表实例
onUnmounted(() => {
  if (taskRefreshInterval.value) {
    clearInterval(taskRefreshInterval.value)
    taskRefreshInterval.value = null
  }
  
  if (graphInstance.value) {
    graphInstance.value.dispose()
    graphInstance.value = null
  }
})
</script>

<template>
  <div class="app-container">
    <!-- 搜索工具栏 -->
    <div class="search-bar">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item>
          <el-input
            v-model="queryParams.keyword"
            placeholder="输入RAG名称或描述搜索"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="action-buttons">
        <el-button type="primary" :icon="Plus" @click="handleAdd">创建RAG</el-button>
        <el-button type="success" :icon="Plus" @click="handleGenerate">生成RAG</el-button>
        <el-button type="info" :icon="Plus" @click="handleGenerateFromFile">从文件生成RAG</el-button>
        <el-button type="default" :icon="RefreshRight" @click="getRAGList">刷新</el-button>
      </div>
    </div>
    
    <!-- 生成任务状态列表 -->
    <div v-if="generationTasks.length > 0" class="task-status">
      <div class="task-status-header">
        <h3>生成任务状态</h3>
        <el-button type="text" @click="clearCompletedTasks">清除已完成任务</el-button>
      </div>
      <el-card v-for="task in generationTasks" :key="task.id" class="task-card">
        <div class="task-info">
          <div class="task-name">{{ task.name }}</div>
          <el-tag :type="task.status === 'COMPLETED' ? 'success' : (task.status === 'FAILED' ? 'danger' : 'info')">
            {{ task.status === 'COMPLETED' ? '已完成' : (task.status === 'FAILED' ? '失败' : '进行中') }}
          </el-tag>
        </div>
        <el-progress 
          :percentage="task.progress" 
          :status="task.status === 'COMPLETED' ? 'success' : (task.status === 'FAILED' ? 'exception' : '')"
        ></el-progress>
      </el-card>
    </div>
    
    <!-- RAG列表表格 -->
    <el-table
      v-loading="loading"
      :data="ragList"
      border
      stripe
      style="width: 100%; margin-top: 15px;"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleQuery(row)">查询</el-button>
          <el-button type="info" size="small" @click="viewKnowledgeGraph(row.id)">查看知识图谱</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 分页控件 -->
    <div class="pagination-container">
      <el-pagination
        v-if="total > 0"
        :current-page="queryParams.pageNum"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="queryParams.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="val => { queryParams.pageSize = val; getRAGList() }"
        @current-change="val => { queryParams.pageNum = val; getRAGList() }"
      />
    </div>
    
    <!-- 创建RAG对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle"
      width="500px"
      append-to-body
    >
      <el-form 
        ref="ragFormRef"
        :model="ragForm"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="ragForm.name" placeholder="请输入RAG名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="ragForm.description" 
            type="textarea" 
            placeholder="请输入RAG描述" 
            :rows="4"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm(ragFormRef)">确定</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 生成RAG对话框 -->
    <el-dialog 
      v-model="generateDialogVisible" 
      title="生成RAG"
      width="500px"
      append-to-body
    >
      <el-form 
        ref="generateFormRef"
        :model="generateForm"
        :rules="generateRules"
        label-width="120px"
      >
        <el-form-item label="源目录路径" prop="sourceDir">
          <el-input v-model="generateForm.sourceDir" placeholder="请输入源目录路径" />
        </el-form-item>
        <el-form-item label="RAG名称" prop="ragName">
          <el-input v-model="generateForm.ragName" placeholder="请输入RAG名称" />
        </el-form-item>
        <el-form-item label="强制重新构建" prop="forceRebuild">
          <el-switch v-model="generateForm.forceRebuild" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="generateDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitGenerateForm(generateFormRef)">确定</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 从文件生成RAG对话框 -->
    <el-dialog 
      v-model="generateFromFileDialogVisible" 
      title="从文件生成RAG"
      width="600px"
      append-to-body
    >
      <el-form 
        ref="generateFromFileFormRef"
        :model="generateFromFileForm"
        :rules="generateFromFileRules"
        label-width="120px"
      >
        <el-form-item label="选择文件夹" prop="folderId">
          <div>
            <div class="folder-navigation">
              <div v-if="currentPath" class="folder-breadcrumb">
                <el-breadcrumb separator="/">
                  <el-breadcrumb-item @click="getRootFolders">根目录</el-breadcrumb-item>
                  <el-breadcrumb-item v-for="(part, index) in currentPath.split('/')" 
                    :key="index" 
                    v-if="part"
                    @click="getFoldersByPath(currentPath.split('/').slice(0, index + 1).join('/'))">
                    {{ part }}
                  </el-breadcrumb-item>
                </el-breadcrumb>
              </div>
              <el-table 
                :data="folderList"
                style="width: 100%"
                @row-click="row => row.directory ? getFoldersByPath(row.filePath) : null"
                height="250"
                :row-class-name="tableRowClassName">
                <el-table-column label="名称" min-width="180">
                  <template #default="scope">
                    <div class="file-item">
                      <el-icon v-if="scope.row.directory"><Folder /></el-icon>
                      <el-icon v-else><Document /></el-icon>
                      <span class="file-name">{{ scope.row.fileName }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="类型" width="80">
                  <template #default="{ row }">
                    <span>{{ row.directory ? '文件夹' : '文件' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="大小" width="100" align="right">
                  <template #default="{ row }">
                    <span>{{ row.fileSize ? `${(row.fileSize / 1024).toFixed(2)} KB` : '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template #default="{ row }">
                    <el-button 
                      size="small"
                      :type="generateFromFileForm.folderId === row.id ? 'success' : 'primary'"
                      @click.stop="selectFolder(row)"
                      :disabled="!row.directory"
                      >
                      {{ generateFromFileForm.folderId === row.id ? '已选择' : '选择' }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div v-if="generateFromFileForm.folderId" class="selected-folder">
              已选择文件夹: {{ folderList.find(f => f.id === generateFromFileForm.folderId)?.fileName }}
              <el-button size="small" type="danger" @click="generateFromFileForm.folderId = null">取消选择</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="RAG名称" prop="ragName">
          <el-input v-model="generateFromFileForm.ragName" placeholder="请输入RAG名称" />
        </el-form-item>
        <el-form-item label="强制重新构建" prop="forceRebuild">
          <el-switch v-model="generateFromFileForm.forceRebuild" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="generateFromFileDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitGenerateFromFileForm(generateFromFileFormRef)">确定</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 查询RAG对话框 -->
    <el-dialog 
      v-model="queryDialogVisible" 
      title="查询RAG"
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
          <el-tabs type="border-card">
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
  </div>
</template>

<style scoped>
.app-container {
  padding: 20px;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  margin-bottom: 15px;
}

.search-form {
  display: flex;
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.task-status {
  margin-bottom: 15px;
}

.task-status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.task-status-header h3 {
  margin: 0;
}

.task-card {
  margin-bottom: 10px;
}

.task-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.pagination-container {
  margin-top: 15px;
  display: flex;
  justify-content: center;
}

.folder-navigation {
  margin-bottom: 15px;
}

.folder-breadcrumb {
  margin-bottom: 10px;
  cursor: pointer;
}

.selected-folder {
  margin-top: 10px;
  padding: 10px;
  background-color: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
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

:deep(.selected-row) {
  background-color: #e6f7ff;
}

:deep(.directory-row) {
  cursor: pointer;
}

:deep(.directory-row:hover) {
  background-color: #f5f7fa;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  font-size: 14px;
}

:deep(.el-dialog__body) {
  padding: 15px;
  overflow: hidden;
}

:deep(.el-tabs__content) {
  padding: 15px;
  height: calc(100% - 40px);
  overflow: auto;
}

:deep(.el-tab-pane) {
  height: 100%;
}

:deep(.el-tabs--border-card) {
  height: 100%;
}

/* 添加选中文件夹行的样式 */
:deep(.selected-row) {
  background-color: rgba(64, 158, 255, 0.1);
}
</style> 