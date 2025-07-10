<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download, RefreshRight, Document } from '@element-plus/icons-vue'
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
</script>

<template>
  <div class="app-container">
    <!-- 搜索工具栏 -->
    <div class="search-bar">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item>
          <el-input
            v-model="queryParams.keyword"
            placeholder="输入教案提示词搜索"
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
        <el-button type="primary" :icon="Plus" @click="handleGenerate">生成教案</el-button>
        <el-button type="default" :icon="RefreshRight" @click="getTeachingPlanList">刷新</el-button>
      </div>
    </div>
    
    <!-- 教案列表表格 -->
    <el-table
      v-loading="loading"
      :data="teachingPlanList"
      border
      stripe
      style="width: 100%; margin-top: 15px;"
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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button 
            v-if="row.status === 'PENDING'" 
            type="info" 
            size="small" 
            @click="refreshTeachingPlanStatus(row.id)"
          >
            刷新状态
          </el-button>
          <el-button 
            v-if="row.status === 'COMPLETED'" 
            type="primary" 
            size="small" 
            :icon="Download"
            @click="downloadTeachingPlan(row.id, row.fileName)"
          >
            下载
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
      width="500px"
      append-to-body
    >
      <el-form 
        ref="generateFormRef"
        :model="generateForm"
        :rules="generateRules"
        label-width="120px"
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
          <el-select v-model="generateForm.inputs.depth" placeholder="请选择教案深度">
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
          >
            <el-option
              v-for="item in ragList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        
        <div class="form-tips">
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

.pagination-container {
  margin-top: 15px;
  display: flex;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.form-tips {
  margin: 10px 0;
  padding: 8px 12px;
  background-color: #f4f4f5;
  border-radius: 4px;
  color: #666;
  font-size: 0.9em;
}
</style> 