<script setup>
import { ref, onMounted, reactive, watch, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download, RefreshRight, Document, Edit, Histogram } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'
import { useRouter } from 'vue-router'

const router = useRouter()
const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 教案列表数据
const teachingPlanList = ref([])
const loading = ref(false)
const total = ref(0)

// RAG列表数据
const ragList = ref([])
const loadingRag = ref(false)

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
  inputs: {
    depth: 2
  }
})

// 对话框控制
const generateDialogVisible = ref(false)

// 深度选项
const depthOptions = [
  { value: 1, label: '1 - 非常基础' },
  { value: 2, label: '2' },
  { value: 3, label: '3' },
  { value: 4, label: '4' },
  { value: 5, label: '5 - 中等' },
  { value: 6, label: '6' },
  { value: 7, label: '7' },
  { value: 8, label: '8' },
  { value: 9, label: '9' },
  { value: 10, label: '10 - 非常深入' }
]

// 表单校验规则
const generateRules = {
  query: [
    { required: true, message: '请输入教案提示词', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  ragId: [
    { required: true, message: '请选择RAG', trigger: 'change', type: 'number' }
  ]
}

// 获取教案列表
const getTeachingPlanList = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${BaseUrl}api/teaching-plan-generator/list`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      teachingPlanList.value = response.data.teachingPlans || []
      total.value = response.data.count || 0
      
      // 如果有关键词，前端过滤
      if (queryParams.keyword) {
        const keyword = queryParams.keyword.toLowerCase()
        teachingPlanList.value = teachingPlanList.value.filter(plan => 
          (plan.prompt && plan.prompt.toLowerCase().includes(keyword))
        )
        total.value = teachingPlanList.value.length
      }
    } else {
      ElMessage.error(response.data?.message || '获取教案列表失败')
    }
  } catch (error) {
    console.error('获取教案列表失败:', error)
    ElMessage.error('获取教案列表失败')
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

// 打开生成教案对话框
const handleGenerate = () => {
  resetGenerateForm()
  getRAGList() // 获取RAG列表
  generateDialogVisible.value = true
}

// 重置生成表单
const resetGenerateForm = () => {
  generateForm.query = ''
  generateForm.useRag = false
  generateForm.ragId = null
  generateForm.inputs.depth = 5
}

// 提交生成教案表单
const submitGenerateForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        let url = `${BaseUrl}api/teaching-plan-generator/generate`
        let requestData = {
          query: generateForm.query,
          inputs: {
            depth: generateForm.inputs.depth
          }
        }
        
        // 如果使用RAG，切换为使用RAG的API
        if (generateForm.useRag && generateForm.ragId) {
          url = `${BaseUrl}api/teaching-plan-generator/generate-with-rag`
          requestData.ragId = generateForm.ragId
        }
        
        const response = await axios.post(url, requestData, {
          headers: {
            'Authorization': `Bearer ${getToken()}`
          }
        })
        
        if (response.data && response.data.success) {
          ElMessage.success('教案生成任务已提交')
          generateDialogVisible.value = false
          getTeachingPlanList() // 刷新列表
        } else {
          ElMessage.error(response.data?.message || '提交教案生成任务失败')
        }
      } catch (error) {
        console.error('提交教案生成任务失败:', error)
        ElMessage.error('提交教案生成任务失败')
      }
    }
  })
}

// 刷新单个教案状态
const refreshTeachingPlanStatus = async (id) => {
  try {
    const response = await axios.get(`${BaseUrl}api/teaching-plan-generator/${id}`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      const index = teachingPlanList.value.findIndex(item => item.id === id)
      if (index !== -1) {
        teachingPlanList.value[index] = {
          ...teachingPlanList.value[index],
          ...response.data
        }
        
        if (response.data.status === 'COMPLETED') {
          ElMessage.success('教案已生成完成')
        } else if (response.data.status === 'FAILED') {
          ElMessage.error(`教案生成失败: ${response.data.error || '未知错误'}`)
        }
      }
    } else {
      ElMessage.error(response.data?.message || '获取教案状态失败')
    }
  } catch (error) {
    console.error('获取教案状态失败:', error)
    ElMessage.error('获取教案状态失败')
  }
}

// 下载教案文件
const downloadTeachingPlan = async (id, fileName) => {
  try {
    // 使用专门的下载接口
    const fileUrl = `${BaseUrl}api/teaching-plan-generator/${id}/download`
    
    // 使用axios进行文件下载
    const response = await axios({
      url: fileUrl,
      method: 'GET',
      responseType: 'blob', // 重要：指定响应类型为blob
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    // 创建Blob对象
    const blob = new Blob([response.data], { 
      type: response.headers['content-type'] || 'application/octet-stream' 
    })
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    
    // 从响应头获取文件名
    const contentDisposition = response.headers['content-disposition']
    let downloadFileName = '教案.docx'
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename="(.+)"/)
      if (fileNameMatch && fileNameMatch.length === 2) {
        downloadFileName = fileNameMatch[1]
      }
    } else if (fileName) {
      // 如果没有从响应头获取到文件名，则使用传入的文件名
      downloadFileName = fileName
    } else {
      // 如果都没有，使用默认文件名
      downloadFileName = `教案文件_${id}.docx`
    }
    
    link.href = url
    link.setAttribute('download', downloadFileName)
    document.body.appendChild(link)
    link.click()
    
    // 清理
    window.URL.revokeObjectURL(url)
    link.remove()
    
    ElMessage.success('文件下载成功')
  } catch (error) {
    console.error('下载文件失败:', error)
    ElMessage.error('下载文件失败: ' + (error.response?.status === 404 ? '文件不存在或任务未完成' : '请求失败'))
  }
}

// 搜索教案
const handleSearch = () => {
  queryParams.pageNum = 1
  getTeachingPlanList()
}

// 重置搜索条件
const resetSearch = () => {
  queryParams.keyword = ''
  queryParams.pageNum = 1
  getTeachingPlanList()
}

// 格式化状态标签类型
const getStatusTagType = (status) => {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'PENDING':
      return 'warning'
    default:
      return 'info'
  }
}

// 格式化状态显示文本
const formatStatus = (status) => {
  switch (status) {
    case 'COMPLETED':
      return '已完成'
    case 'FAILED':
      return '失败'
    case 'PENDING':
      return '处理中'
    default:
      return status
  }
}

// 格式化日期时间
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return ''
  
  const date = new Date(dateTimeStr)
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(date)
}

// 页面加载时获取教案列表
onMounted(() => {
  getTeachingPlanList()
})

// 教学效率统计
const efficiencyStatsDialogVisible = ref(false)
const singleEfficiencyDialogVisible = ref(false)
const loadingEfficiency = ref(false)
const efficiencyStats = ref({})
const currentEfficiency = ref(null)
const efficiencyChartRef = ref(null)
let efficiencyChart = null

// 添加优化建议相关的状态
const optimizationDialogVisible = ref(false)
const optimizationLoading = ref(false)
const currentOptimizationData = ref(null)

// 在线编辑教案
const editTeachingPlan = (id) => {
  router.push(`/teaching-plan-editor/${id}`)
}

// 查看单个教案效率
const viewEfficiency = async (id) => {
  loadingEfficiency.value = true
  currentEfficiency.value = null
  
  try {
    const response = await axios.get(`${BaseUrl}api/teaching-plan-generator/${id}/optimization`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      currentEfficiency.value = response.data
      singleEfficiencyDialogVisible.value = true
    } else {
      ElMessage.error(response.data?.message || '获取效率数据失败')
    }
  } catch (error) {
    console.error('获取效率数据失败:', error)
    ElMessage.error('获取效率数据失败')
  } finally {
    loadingEfficiency.value = false
  }
}

// 查看教学效率统计数据
const viewEfficiencyStatistics = async () => {
  loadingEfficiency.value = true
  efficiencyStats.value = {}
  
  try {
    const response = await axios.get(`${BaseUrl}api/teaching-plan-generator/efficiency/statistics`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      efficiencyStats.value = response.data
      efficiencyStatsDialogVisible.value = true
      
      // 如果有月度数据，创建图表
      if (response.data.hasData && response.data.monthlyEfficiency) {
        setTimeout(() => {
          createEfficiencyChart(response.data.monthlyEfficiency)
        }, 300)
      }
    } else {
      ElMessage.error(response.data?.message || '获取效率统计失败')
    }
  } catch (error) {
    console.error('获取效率统计失败:', error)
    ElMessage.error('获取效率统计失败')
  } finally {
    loadingEfficiency.value = false
  }
}

// 创建效率趋势图表
const createEfficiencyChart = (monthlyData) => {
  if (!efficiencyChartRef.value) return
  
  // 导入echarts
  import('echarts').then((echarts) => {
    if (efficiencyChart) {
      efficiencyChart.dispose()
    }
    
    efficiencyChart = echarts.init(efficiencyChartRef.value)
    
    const months = Object.keys(monthlyData).sort()
    const values = months.map(month => monthlyData[month])
    
    const option = {
      tooltip: {
        trigger: 'axis',
        formatter: '{b}: {c}'
      },
      xAxis: {
        type: 'category',
        data: months,
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value',
        name: '效率指数'
      },
      series: [{
        data: values,
        type: 'line',
        smooth: true,
        lineStyle: {
          width: 3,
          color: '#409EFF'
        },
        itemStyle: {
          color: '#409EFF'
        }
      }]
    }
    
    efficiencyChart.setOption(option)
    
    // 响应窗口大小变化
    window.addEventListener('resize', () => {
      if (efficiencyChart) {
        efficiencyChart.resize()
      }
    })
  })
}

// 格式化优化建议
const formatSuggestions = (suggestions) => {
  if (!suggestions) return ''
  return suggestions
}

// 获取效率指数对应的颜色
const getEfficiencyColor = (value) => {
  if (value >= 80) return 'success'
  if (value >= 60) return 'warning'
  return 'danger'
}

// 格式化时长
const formatDuration = (seconds) => {
  if (!seconds && seconds !== 0) return '--'
  
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainingSeconds = Math.floor(seconds % 60)
  
  return [
    hours > 0 ? `${hours}小时` : '',
    minutes > 0 ? `${minutes}分钟` : '',
    `${remainingSeconds}秒`
  ].filter(Boolean).join(' ')
}

// 监听对话框关闭
watch(efficiencyStatsDialogVisible, (val) => {
  if (!val && efficiencyChart) {
    efficiencyChart.dispose()
    efficiencyChart = null
  }
})

// 组件卸载前清理图表
onBeforeUnmount(() => {
  if (efficiencyChart) {
    efficiencyChart.dispose()
    efficiencyChart = null
  }
})

// 查看优化建议
const handleViewOptimization = async (row) => {
  try {
    if (!row || !row.id) {
      ElMessage.warning('找不到教案信息')
      return
    }
    
    // 设置加载状态
    optimizationLoading.value = true
    optimizationDialogVisible.value = true
    currentOptimizationData.value = { 
      prompt: row.prompt, 
      efficiencyIndex: row.efficiencyIndex,
      editDuration: row.editDuration
    }
    
    // 获取优化建议
    try {
      const response = await axios.get(`${BaseUrl}api/teaching-plan-generator/${row.id}/optimization`, {
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
        ElMessage.error(response.data?.message || '获取优化建议失败')
      }
    } catch (error) {
      console.error('获取优化建议失败:', error)
      ElMessage.error('获取优化建议失败: ' + (error.response?.data?.message || error.message))
    }
    
    // 使用大模型生成针对性建议
    if (currentOptimizationData.value.prompt) {
      try {
        // 构建系统提示词和用户消息
        const systemPrompt = `你是一位专业的教育教学顾问，专长于提供针对性的教案优化建议。
请基于以下信息，提供具体、实用的优化建议：
- 教案主题: ${currentOptimizationData.value.prompt}
- 效率指数: ${currentOptimizationData.value.efficiencyIndex}
- 编辑时长: ${formatDuration(currentOptimizationData.value.editDuration)}

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
      "name": "内容结构",
      "score": 分数,
      "strengths": ["优势点1", "优势点2"],
      "weaknesses": ["不足点1", "不足点2"],
      "suggestions": ["具体建议1", "具体建议2"]
    },
    {
      "name": "教学活动",
      "score": 分数,
      "strengths": ["优势点1", "优势点2"],
      "weaknesses": ["不足点1", "不足点2"],
      "suggestions": ["具体建议1", "具体建议2"]
    },
    {
      "name": "时间安排",
      "score": 分数,
      "strengths": ["优势点1", "优势点2"],
      "weaknesses": ["不足点1", "不足点2"],
      "suggestions": ["具体建议1", "具体建议2"]
    },
    {
      "name": "教学资源",
      "score": 分数,
      "strengths": ["优势点1", "优势点2"],
      "weaknesses": ["不足点1", "不足点2"],
      "suggestions": ["具体建议1", "具体建议2"]
    }
  ],
  "overall_suggestion": "综合性建议，100-200字"
}

必须确保输出是有效的JSON格式，可以直接被JSON.parse()解析。`;

        const userMessage = `请为"${currentOptimizationData.value.prompt}"这个主题的教案生成结构化的优化建议JSON。`;

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
</script>

<template>
  <div class="teaching-plan-generator-container">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item>
          <el-input v-model="queryParams.keyword" placeholder="请输入教案提示词" clearable @keyup.enter="handleSearch">
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
      
      <div class="toolbar-right">
        <el-button type="success" plain @click="handleGenerate" :icon="Plus">生成教案</el-button>
        <el-button type="primary" plain @click="viewEfficiencyStatistics" :icon="Histogram">效率统计</el-button>
        <el-button type="info" plain @click="getTeachingPlanList" :icon="RefreshRight" :loading="loading">刷新</el-button>
      </div>
    </div>
    
    <!-- 教案列表表格 -->
    <el-table
      v-loading="loading"
      :data="teachingPlanList"
      style="width: 100%; margin-top: 10px; margin-bottom: 10px"
      border
      stripe
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="prompt" label="提示词" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)">{{ formatStatus(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="文件名" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.fileName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button 
            v-if="row.status === 'PENDING'" 
            type="info" 
            link
            @click="refreshTeachingPlanStatus(row.id)"
          >
            刷新状态
          </el-button>
          <el-button 
            v-if="row.status === 'COMPLETED'" 
            type="primary" 
            link
            @click="downloadTeachingPlan(row.id, row.fileName)"
          >
            下载
          </el-button>
          <el-button 
            v-if="row.status === 'COMPLETED'" 
            type="warning" 
            link
            @click="editTeachingPlan(row.id)"
          >
            在线编辑
          </el-button>
          <el-button
            v-if="row.efficiencyIndex !== undefined"
            type="success"
            link
            @click="viewEfficiency(row.id)"
          >
            效率指数
          </el-button>
          <el-button
            v-if="row.efficiencyIndex !== undefined"
            type="info"
            link
            @click="handleViewOptimization(row)"
          >
            优化建议
          </el-button>
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
        @size-change="val => { queryParams.pageSize = val; getTeachingPlanList() }"
        @current-change="val => { queryParams.pageNum = val; getTeachingPlanList() }"
      />
    </div>
    
    <!-- 生成教案对话框 -->
    <el-dialog 
      v-model="generateDialogVisible" 
      title="生成教案"
      width="50%"
      append-to-body
    >
      <el-form 
        ref="generateFormRef"
        :model="generateForm"
        :rules="generateRules"
        label-width="100px"
      >
        <el-form-item label="教案提示词" prop="query">
          <el-input 
            v-model="generateForm.query" 
            type="textarea" 
            :rows="3"
            placeholder="请输入教案提示词，如: tensorflow.js" 
          />
        </el-form-item>
        
        <el-form-item label="深度设置">
          <el-select v-model="generateForm.inputs.depth" placeholder="请选择教案深度" style="width: 100%">
            <el-option
              v-for="item in depthOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="使用RAG">
          <el-switch v-model="generateForm.useRag" />
        </el-form-item>
        
        <el-form-item label="选择RAG" prop="ragId" v-if="generateForm.useRag">
          <el-select 
            v-model="generateForm.ragId" 
            placeholder="请选择RAG"
            :loading="loadingRag"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in ragList"
              :key="item.id"
              :label="`${item.name} (ID: ${item.id})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        
        <div class="form-tip">
          <p><i class="el-icon-info"></i> 提示：教案生成是异步处理的，可能需要较长时间（约15分钟）才能完成。</p>
          <p v-if="generateForm.useRag"><i class="el-icon-info"></i> 使用RAG可以基于知识图谱生成更加专业的教案。</p>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="generateDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitGenerateForm(generateFormRef)">提交</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 教学效率统计对话框 -->
    <el-dialog 
      v-model="efficiencyStatsDialogVisible" 
      title="教学效率统计"
      width="70%"
      append-to-body
    >
      <div v-loading="loadingEfficiency">
        <div v-if="efficiencyStats.hasData">
          <!-- 统计总览 -->
          <div class="efficiency-overview">
            <div class="efficiency-card">
              <div class="card-title">已编辑教案数</div>
              <div class="card-value">{{ efficiencyStats.totalEditedPlans || 0 }}</div>
            </div>
            
            <div class="efficiency-card">
              <div class="card-title">平均效率指数</div>
              <div class="card-value" :class="'text-' + getEfficiencyColor(efficiencyStats.avgEfficiencyIndex)">
                {{ efficiencyStats.avgEfficiencyIndex?.toFixed(2) || '无数据' }}
              </div>
            </div>
            
            <div class="efficiency-card">
              <div class="card-title">平均编辑时长</div>
              <div class="card-value">
                {{ formatDuration(efficiencyStats.avgEditDuration) }}
              </div>
            </div>
          </div>
          
          <!-- 效率详情 -->
          <div v-if="efficiencyStats.mostEfficient || efficiencyStats.leastEfficient" class="efficiency-details">
            <el-divider content-position="center">效率详情</el-divider>
            
            <div class="detail-section" v-if="efficiencyStats.mostEfficient">
              <h4>最高效率教案</h4>
              <div class="detail-item">
                <span>提示词: {{ efficiencyStats.mostEfficient.prompt }}</span>
              </div>
              <div class="detail-item">
                <span>效率指数: </span>
                <el-tag :type="getEfficiencyColor(efficiencyStats.mostEfficient.efficiencyIndex)">
                  {{ efficiencyStats.mostEfficient.efficiencyIndex?.toFixed(2) }}
                </el-tag>
                <el-button 
                  type="primary" 
                  link
                  @click="viewEfficiency(efficiencyStats.mostEfficient.id)"
                >
                  查看详情
                </el-button>
              </div>
            </div>
            
            <div class="detail-section" v-if="efficiencyStats.leastEfficient">
              <h4>最低效率教案</h4>
              <div class="detail-item">
                <span>提示词: {{ efficiencyStats.leastEfficient.prompt }}</span>
              </div>
              <div class="detail-item">
                <span>效率指数: </span>
                <el-tag :type="getEfficiencyColor(efficiencyStats.leastEfficient.efficiencyIndex)">
                  {{ efficiencyStats.leastEfficient.efficiencyIndex?.toFixed(2) }}
                </el-tag>
                <el-button 
                  type="primary" 
                  link
                  @click="viewEfficiency(efficiencyStats.leastEfficient.id)"
                >
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
          
          <!-- 月度效率趋势 -->
          <div v-if="efficiencyStats.monthlyEfficiency && Object.keys(efficiencyStats.monthlyEfficiency).length > 0" class="efficiency-trend">
            <el-divider content-position="center">月度效率趋势</el-divider>
            <div ref="efficiencyChartRef" style="height: 300px"></div>
          </div>
        </div>
        
        <el-empty v-else description="暂无效率数据" />
      </div>
    </el-dialog>

    <!-- 单个教案效率对话框 -->
    <el-dialog 
      v-model="singleEfficiencyDialogVisible" 
      title="教案效率详情"
      width="60%"
      append-to-body
    >
      <div v-loading="loadingEfficiency">
        <div v-if="currentEfficiency" class="efficiency-container">
          <div class="question-info">
            <div class="info-item">
              <span class="info-label">提示词:</span>
              <span class="info-value">{{ currentEfficiency.prompt }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">效率指数:</span>
              <span class="info-value">
                <el-tag :type="getEfficiencyColor(currentEfficiency.efficiencyIndex)">
                  {{ currentEfficiency.efficiencyIndex?.toFixed(2) }}
                </el-tag>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">编辑时长:</span>
              <span class="info-value">{{ formatDuration(currentEfficiency.editDuration) }}</span>
            </div>
          </div>
          
          <el-divider content-position="center">优化建议</el-divider>
          
          <div v-if="currentEfficiency.optimizationSuggestions" class="suggestion-content">
            <pre>{{ currentEfficiency.optimizationSuggestions }}</pre>
          </div>
          <el-empty v-else description="暂无优化建议" />
        </div>
        
        <el-empty v-else description="无法获取效率数据" />
      </div>
    </el-dialog>

    <!-- 优化建议对话框 -->
    <el-dialog 
      v-model="optimizationDialogVisible" 
      title="教案优化建议"
      width="70%"
      append-to-body
      :close-on-click-modal="false"
    >
      <div v-loading="optimizationLoading" element-loading-text="正在生成优化建议..." class="optimization-container">
        <div v-if="currentOptimizationData" class="question-info">
          <div class="info-item">
            <span class="info-label">教案主题:</span>
            <span class="info-value">{{ currentOptimizationData.prompt }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">效率指数:</span>
            <span class="info-value">
              <el-tag :type="getEfficiencyColor(currentOptimizationData.efficiencyIndex)">
                {{ currentOptimizationData.efficiencyIndex?.toFixed(2) }}
              </el-tag>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">编辑时长:</span>
            <span class="info-value">{{ formatDuration(currentOptimizationData.editDuration) }}</span>
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

        <div v-else-if="currentOptimizationData && currentOptimizationData.additionalSuggestions && !currentOptimizationData.structuredSuggestions" class="additional-suggestion-content">
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
  </div>
</template>

<style scoped>
.teaching-plan-generator-container {
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

.form-tip {
  margin: 15px 0;
  padding: 10px 15px;
  background-color: var(--el-color-info-light-9);
  border-radius: 4px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  line-height: 1.5;
}

.form-tip p {
  margin: 5px 0;
}

.efficiency-container {
  padding: 10px;
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

.efficiency-overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
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

.text-warning {
  color: var(--el-color-warning);
}

.text-danger {
  color: var(--el-color-danger);
}

.text-primary {
  color: var(--el-color-primary);
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
  gap: 10px;
}

.efficiency-trend {
  margin-top: 20px;
}

.suggestion-content {
  background-color: var(--el-fill-color-lighter);
  padding: 15px;
  border-radius: 4px;
  white-space: pre-wrap;
}

.optimization-container {
  padding: 10px;
}

.structured-suggestion-content {
  margin-top: 20px;
}

.suggestion-header {
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.suggestion-summary {
  margin: 0;
  color: var(--el-color-primary);
  font-size: 18px;
  font-weight: bold;
}

.categories-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
  margin-bottom: 15px;
}

.category-card {
  border-radius: 8px;
  overflow: hidden;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.category-name {
  font-size: 16px;
  font-weight: bold;
  color: var(--el-color-primary);
}

.category-content {
  padding: 15px;
}

.section-title {
  margin-top: 0;
  margin-bottom: 10px;
  color: var(--el-color-primary);
  border-bottom: 1px solid var(--el-border-color-light);
  padding-bottom: 5px;
}

.strengths-section .section-title {
  color: var(--el-color-success);
}

.weaknesses-section .section-title {
  color: var(--el-color-danger);
}

.suggestions-section .section-title {
  color: var(--el-color-primary);
}

.strengths-section .points-list li {
  color: var(--el-color-success-dark-2);
}

.weaknesses-section .points-list li {
  color: var(--el-color-danger-dark-2);
}

.suggestions-section .points-list li {
  color: var(--el-color-primary-dark-2);
}

.points-list {
  list-style: disc;
  padding-left: 20px;
  margin: 0 0 10px 0;
}

.points-list li {
  margin-bottom: 5px;
  color: var(--el-text-color-regular);
  font-size: 14px;
  position: relative;
  line-height: 1.5;
}

.overall-suggestion {
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px solid var(--el-border-color-light);
}

.additional-suggestion-content {
  margin-top: 15px;
}

@media (max-width: 768px) {
  .efficiency-overview,
  .question-info {
    grid-template-columns: 1fr;
  }
}
</style> 