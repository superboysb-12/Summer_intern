<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Download, RefreshRight } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'
import { useRouter } from 'vue-router'

const router = useRouter()
const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// PPT列表数据
const pptList = ref([])
const loading = ref(false)
const total = ref(0)

// 用于生成的教案列表
const teachingPlanListForSelection = ref([])
const loadingTeachingPlans = ref(false)

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
  teachingPlanId: null
})

// 对话框控制
const generateDialogVisible = ref(false)

// 表单校验规则
const generateRules = {
  teachingPlanId: [
    { required: true, message: '请选择一个教案用于生成PPT', trigger: 'change', type: 'number' }
  ]
}

// 获取PPT生成任务列表
const getPPTList = async () => {
  loading.value = true
  try {
    // 假设的API端点
    const response = await axios.get(`${BaseUrl}api/ppt-generator/list`, {
      headers: { 'Authorization': `Bearer ${getToken()}` },
      params: queryParams
    })
    
    if (response.data && response.data.success) {
      pptList.value = response.data.data || [] // 假设返回的数据结构
      total.value = response.data.total || 0
    } else {
      // 模拟数据，因为后端API可能还不存在
      console.warn('API /api/ppt-generator/list not found, using mock data.')
      pptList.value = [
        { id: 1, teachingPlan: { id: 101, prompt: 'TensorFlow.js入门' }, status: 'COMPLETED', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), fileName: 'TensorFlow.js入门.pptx' },
        { id: 2, teachingPlan: { id: 102, prompt: 'Vue 3 核心概念' }, status: 'PENDING', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), fileName: null },
        { id: 3, teachingPlan: { id: 103, prompt: '深入理解JavaScript闭包' }, status: 'FAILED', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), fileName: null }
      ]
      total.value = 3
      ElMessage.info('后端接口尚未就绪，当前为模拟数据。')
    }
  } catch (error) {
    console.error('获取PPT列表失败:', error)
    // ElMessage.error('获取PPT列表失败')
    // 模拟数据
    console.warn('API /api/ppt-generator/list failed, using mock data.')
    pptList.value = [
        { id: 1, teachingPlan: { id: 101, prompt: 'TensorFlow.js入门' }, status: 'COMPLETED', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), fileName: 'TensorFlow.js入门.pptx' },
        { id: 2, teachingPlan: { id: 102, prompt: 'Vue 3 核心概念' }, status: 'PENDING', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), fileName: null },
        { id: 3, teachingPlan: { id: 103, prompt: '深入理解JavaScript闭包' }, status: 'FAILED', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), fileName: null }
    ]
    total.value = 3
    ElMessage.info('后端接口尚未就绪，当前为模拟数据。')
  } finally {
    loading.value = false
  }
}

// 获取已完成的教案列表
const getCompletedTeachingPlans = async () => {
  loadingTeachingPlans.value = true
  try {
    const response = await axios.get(`${BaseUrl}api/teaching-plan-generator/list`, {
      headers: { 'Authorization': `Bearer ${getToken()}` },
      params: { status: 'COMPLETED' } // 假设API支持按状态筛选
    })
    
    if (response.data && response.data.success) {
      teachingPlanListForSelection.value = response.data.teachingPlans || []
    } else {
      ElMessage.error(response.data?.message || '获取已完成教案列表失败')
      teachingPlanListForSelection.value = [
        { id: 101, prompt: 'TensorFlow.js入门' },
        { id: 102, prompt: 'Vue 3 核心概念' },
        { id: 103, prompt: '深入理解JavaScript闭包' }
      ]
    }
  } catch (error) {
    console.error('获取已完成教案列表失败:', error)
    ElMessage.error('获取已完成教案列表失败, 使用模拟数据')
    teachingPlanListForSelection.value = [
      { id: 101, prompt: 'TensorFlow.js入门' },
      { id: 102, prompt: 'Vue 3 核心概念' },
      { id: 103, prompt: '深入理解JavaScript闭包' }
    ]
  } finally {
    loadingTeachingPlans.value = false
  }
}

// 打开生成PPT对话框
const handleGenerate = () => {
  resetGenerateForm()
  getCompletedTeachingPlans()
  generateDialogVisible.value = true
}

// 重置生成表单
const resetGenerateForm = () => {
  generateForm.teachingPlanId = null
}

// 提交生成PPT表单
const submitGenerateForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const url = `${BaseUrl}api/ppt-generator/generate`
        const requestData = {
          teachingPlanId: generateForm.teachingPlanId
        }
        
        // 由于是模拟，我们不实际发送请求
        // const response = await axios.post(url, requestData, {
        //   headers: { 'Authorization': `Bearer ${getToken()}` }
        // })
        
        // if (response.data && response.data.success) {
        //   ElMessage.success('PPT生成任务已提交')
        //   generateDialogVisible.value = false
        //   getPPTList() // 刷新列表
        // } else {
        //   ElMessage.error(response.data?.message || '提交PPT生成任务失败')
        // }
        
        ElMessage.success('PPT生成任务已提交（模拟）')
        generateDialogVisible.value = false
        getPPTList() // 刷新列表
      } catch (error) {
        console.error('提交PPT生成任务失败:', error)
        ElMessage.error('提交PPT生成任务失败')
      }
    }
  })
}

// 刷新单个PPT状态
const refreshPPTStatus = async (id) => {
  try {
    // const response = await axios.get(`${BaseUrl}api/ppt-generator/${id}`, {
    //   headers: { 'Authorization': `Bearer ${getToken()}` }
    // })
    
    // if (response.data && response.data.success) {
    //   const index = pptList.value.findIndex(item => item.id === id)
    //   if (index !== -1) {
    //     pptList.value[index] = { ...pptList.value[index], ...response.data }
    //     if (response.data.status === 'COMPLETED') {
    //       ElMessage.success('PPT已生成完成')
    //     } else if (response.data.status === 'FAILED') {
    //       ElMessage.error(`PPT生成失败: ${response.data.error || '未知错误'}`)
    //     }
    //   }
    // } else {
    //   ElMessage.error(response.data?.message || '获取PPT状态失败')
    // }
    ElMessage.success(`刷新了ID为 ${id} 的任务状态（模拟）`)
  } catch (error) {
    console.error('获取PPT状态失败:', error)
    ElMessage.error('获取PPT状态失败')
  }
}

// 下载PPT文件
const downloadPPT = async (id, fileName) => {
  ElMessage.success(`开始下载文件: ${fileName}（模拟）`)
  // 实际下载逻辑可以参考 TeachingPlanGenerator.vue
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  getPPTList()
}

// 重置搜索
const resetSearch = () => {
  queryParams.keyword = ''
  queryParams.pageNum = 1
  getPPTList()
}

// 格式化状态
const getStatusTagType = (status) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'danger'
    case 'PENDING': return 'warning'
    default: return 'info'
  }
}

const formatStatus = (status) => {
  switch (status) {
    case 'COMPLETED': return '已完成'
    case 'FAILED': return '失败'
    case 'PENDING': return '处理中'
    default: return status
  }
}

// 格式化日期
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    hour12: false
  }).format(date)
}

// 页面加载时获取列表
onMounted(() => {
  getPPTList()
})
</script>

<template>
  <div class="ppt-generator-container">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item>
          <el-input v-model="queryParams.keyword" placeholder="请输入源教案关键词" clearable @keyup.enter="handleSearch">
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
        <el-button type="success" plain @click="handleGenerate" :icon="Plus">生成PPT</el-button>
        <el-button type="info" plain @click="getPPTList" :icon="RefreshRight" :loading="loading">刷新</el-button>
      </div>
    </div>
    
    <!-- PPT任务列表表格 -->
    <el-table v-loading="loading" :data="pptList" style="width: 100%; margin-top: 10px; margin-bottom: 10px" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="源教案" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.teachingPlan?.prompt || `教案ID: ${row.teachingPlanId}` }}
        </template>
      </el-table-column>
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
      <el-table-column label="文件名" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.fileName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" type="info" link @click="refreshPPTStatus(row.id)">
            刷新状态
          </el-button>
          <el-button v-if="row.status === 'COMPLETED'" type="primary" link @click="downloadPPT(row.id, row.fileName)">
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
        @size-change="val => { queryParams.pageSize = val; getPPTList() }"
        @current-change="val => { queryParams.pageNum = val; getPPTList() }"
      />
    </div>
    
    <!-- 生成PPT对话框 -->
    <el-dialog v-model="generateDialogVisible" title="从教案生成PPT" width="50%" append-to-body>
      <el-form ref="generateFormRef" :model="generateForm" :rules="generateRules" label-width="100px">
        <el-form-item label="选择教案" prop="teachingPlanId">
          <el-select 
            v-model="generateForm.teachingPlanId" 
            placeholder="请选择已完成的教案"
            :loading="loadingTeachingPlans"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in teachingPlanListForSelection"
              :key="item.id"
              :label="item.prompt"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        
        <div class="form-tip">
          <p><i class="el-icon-info"></i> 提示：PPT生成是异步处理的，可能需要一些时间才能完成。</p>
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
.ppt-generator-container {
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
</style> 