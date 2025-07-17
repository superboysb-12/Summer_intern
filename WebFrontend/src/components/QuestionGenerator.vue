<script setup>
import { ref, onMounted, reactive, watch, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download, RefreshRight, Document, Edit, Histogram, Delete } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'
import { useRouter } from 'vue-router'

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
const editFormRef = ref(null)

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
const editDialogVisible = ref(false)

// 当前查看的题目
const currentQuestion = ref(null)
// 编辑题目表单
const editForm = reactive({
  id: null,
  questionJson: ''
})

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

// 打开编辑题目对话框
const handleEdit = (row) => {
  try {
    // 确保已有题目数据
    if (!row.questionJson) {
      ElMessage.warning('该题目尚未生成完成或没有JSON数据')
      return
    }
    
    editForm.id = row.id
    editForm.questionJson = row.questionJson
    editDialogVisible.value = true
  } catch (error) {
    console.error('打开编辑对话框失败:', error)
    ElMessage.error('打开编辑对话框失败')
  }
}

// 提交编辑表单
const submitEditForm = async () => {
  try {
    // 校验JSON格式
    try {
      JSON.parse(editForm.questionJson)
    } catch (e) {
      ElMessage.error('JSON格式不正确，请检查')
      return
    }
    
    const response = await axios.put(`${BaseUrl}api/question-generator/${editForm.id}`, {
      questionJson: editForm.questionJson
    }, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.success) {
      ElMessage.success('更新题目成功')
      editDialogVisible.value = false
      getQuestionList() // 刷新列表
    } else {
      ElMessage.error(response.data?.message || '更新题目失败')
    }
  } catch (error) {
    console.error('更新题目失败:', error)
    ElMessage.error('更新题目失败')
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
</script>

<template>
  <div class="question-generator-container">
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
        <el-switch
          v-model="autoRefreshEnabled"
          active-text="自动刷新"
          @change="toggleAutoRefresh"
        />
      </div>
    </div>
    
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
      <el-table-column label="创建时间" prop="createdAt" width="180">
        <template #default="scope">
          {{ scope.row.createdAt ? new Date(scope.row.createdAt).toLocaleString() : '未知' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
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
            type="success"
            link
            @click="handleEdit(scope.row)"
            :disabled="scope.row.status !== 'COMPLETED'"
          >
            编辑
          </el-button>
          <el-button
            type="info"
            link
            @click="handleGetSolution(scope.row)"
            :disabled="scope.row.status !== 'COMPLETED'"
          >
            解答
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
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
          <pre>{{ formatJson(currentQuestion.questionJson) }}</pre>
        </div>
        <el-empty v-else description="暂无题目内容" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button 
            type="primary" 
            @click="handleEdit(currentQuestion); detailDialogVisible = false"
            v-if="currentQuestion && currentQuestion.status === 'COMPLETED'"
          >
            编辑
          </el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 编辑题目对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑题目"
      width="70%"
      append-to-body
    >
      <el-form ref="editFormRef" :model="editForm">
        <el-form-item label="题目JSON" prop="questionJson">
          <el-input
            v-model="editForm.questionJson"
            type="textarea"
            :rows="15"
            placeholder="请输入题目JSON"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitEditForm">保存</el-button>
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
          <pre>{{ formatJson(currentQuestion.questionJson) }}</pre>
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
</style> 