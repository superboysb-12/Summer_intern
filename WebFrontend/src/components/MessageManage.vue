<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, RefreshRight, Bell, School, Reading } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'

const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// Message list data
const messageList = ref([])
const loading = ref(false)
const total = ref(0)

// Class and Course data
const classesList = ref([])
const coursesList = ref([])
const classesLoading = ref(false)
const coursesLoading = ref(false)

// Form reference
const messageFormRef = ref(null)

// Query parameters
const queryParams = reactive({
  keyword: ''
})

// Form data
const messageForm = reactive({
  id: undefined,
  title: '',
  content: '',
  targetType: 'ALL',
  targetIds: '',
  expiresAt: null,
  active: true
})

// Dialog control
const dialogVisible = ref(false)
const dialogTitle = ref('添加通知')
const formMode = ref('add')

// Target type options
const targetTypeOptions = [
  { label: '所有用户', value: 'ALL' },
  { label: '管理员', value: 'ADMINS' },
  { label: '教师', value: 'TEACHERS' },
  { label: '学生', value: 'STUDENTS' },
  { label: '特定班级', value: 'CLASS' },
  { label: '特定课程', value: 'COURSE' },
  { label: '特定用户', value: 'SPECIFIC' }
]

// Get all classes
const getClasses = async () => {
  if (classesList.value.length > 0) return
  classesLoading.value = true
  try {
    const response = await axios.get(`${BaseUrl}classes`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    if (Array.isArray(response.data)) {
      classesList.value = response.data
    }
  } catch (error) {
    console.error('获取班级列表失败:', error)
  } finally {
    classesLoading.value = false
  }
}

// Get all courses
const getCourses = async () => {
  if (coursesList.value.length > 0) return
  coursesLoading.value = true
  try {
    const response = await axios.get(`${BaseUrl}courses`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    if (Array.isArray(response.data)) {
      coursesList.value = response.data
    }
  } catch (error) {
    console.error('获取课程列表失败:', error)
  } finally {
    coursesLoading.value = false
  }
}

// Watch target type changes
const handleTargetTypeChange = async (value) => {
  messageForm.targetIds = ''
  
  if (value === 'CLASS') {
    await getClasses()
  } else if (value === 'COURSE') {
    await getCourses()
  }
}

// Form validation rules
const rules = {
  title: [
    { required: true, message: '请输入通知标题', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入通知内容', trigger: 'blur' },
    { max: 1000, message: '长度不能超过 1000 个字符', trigger: 'blur' }
  ],
  targetType: [
    { required: true, message: '请选择接收对象', trigger: 'change' }
  ],
  targetIds: [
    { 
      validator: (rule, value, callback) => {
        const type = messageForm.targetType
        if (type === 'SPECIFIC') {
          // 对于特定用户，value是字符串
          if (!value || (typeof value === 'string' && value.trim() === '')) {
            callback(new Error('请输入接收者ID'))
          } else {
            callback()
          }
        } else if (type === 'CLASS' || type === 'COURSE') {
          // 对于班级和课程，value是数组
          if (!value || (Array.isArray(value) && value.length === 0)) {
            callback(new Error(type === 'CLASS' ? '请选择班级' : '请选择课程'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// Get message list
const getMessageList = async () => {
  loading.value = true
  try {
    // 构建请求参数
    let url = BaseUrl + 'messages'
    let params = {}
    
    // 如果有搜索关键词，使用搜索接口
    if (queryParams.keyword) {
      url = BaseUrl + 'messages/search'
      params.keyword = queryParams.keyword
    }
    
    const response = await axios.get(url, {
      params: params,
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    // 处理返回的数据
    if (Array.isArray(response.data)) {
      messageList.value = response.data
      total.value = response.data.length
    } else {
      messageList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取通知列表失败:', error)
    ElMessage.error('获取通知列表失败')
  } finally {
    loading.value = false
  }
}

// Open add message dialog
const handleAdd = () => {
  resetForm()
  dialogTitle.value = '添加通知'
  formMode.value = 'add'
  dialogVisible.value = true
}

// Open edit message dialog
const handleEdit = (row) => {
  resetForm()
  dialogTitle.value = '编辑通知'
  formMode.value = 'edit'
  
  // Fill form data
  Object.keys(messageForm).forEach(key => {
    if (key in row) {
      // 处理targetIds，如果是逗号分隔的字符串，转为数组
      if (key === 'targetIds' && row[key] && (row.targetType === 'CLASS' || row.targetType === 'COURSE')) {
        messageForm[key] = row[key].split(',')
      } else {
        messageForm[key] = row[key]
      }
    }
  })

  // 预加载相关数据
  handleTargetTypeChange(row.targetType)
  
  dialogVisible.value = true
}

// Delete message
const handleDelete = (id) => {
  ElMessageBox.confirm('确认要删除该通知吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`${BaseUrl}messages/${id}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      ElMessage.success('删除成功')
      getMessageList()
    } catch (error) {
      console.error('删除通知失败:', error)
      ElMessage.error('删除通知失败')
    }
  }).catch(() => {})
}

// Toggle message status
const toggleStatus = async (id) => {
  try {
    await axios.put(`${BaseUrl}messages/${id}/toggle-status`, {}, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    ElMessage.success('状态已更新')
    getMessageList()
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('更新状态失败')
  }
}

// Submit form
const submitForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const data = { ...messageForm }
        
        // 处理多选数据，转为逗号分隔字符串
        if (Array.isArray(data.targetIds)) {
          data.targetIds = data.targetIds.join(',')
        }
        
        if (formMode.value === 'add') {
          await axios.post(BaseUrl + 'messages', data, {
            headers: {
              'Authorization': `Bearer ${getToken()}`
            }
          })
          ElMessage.success('添加成功')
        } else {
          await axios.put(`${BaseUrl}messages/${data.id}`, data, {
            headers: {
              'Authorization': `Bearer ${getToken()}`
            }
          })
          ElMessage.success('更新成功')
        }
        
        dialogVisible.value = false
        getMessageList()
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error('操作失败: ' + (error.response?.data?.message || error.message))
      }
    }
  })
}

// Reset form
const resetForm = () => {
  // Reset form data
  messageForm.id = undefined
  messageForm.title = ''
  messageForm.content = ''
  messageForm.targetType = 'ALL'
  messageForm.targetIds = ''
  messageForm.expiresAt = null
  messageForm.active = true
  
  // Reset validation state if form reference exists
  if (messageFormRef.value) {
    messageFormRef.value.resetFields()
  }
}

// Refresh list
const refreshList = () => {
  getMessageList()
}

// Search
const handleSearch = () => {
  queryParams.pageNum = 1 // Reset page to 1 when searching
  getMessageList()
}

// Reset search
const resetSearch = () => {
  queryParams.keyword = ''
  queryParams.pageNum = 1 // Reset page to 1 when resetting search
  getMessageList()
}

// Format date
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString()
}

// Format target type
const formatTargetType = (type) => {
  const option = targetTypeOptions.find(option => option.value === type)
  return option ? option.label : type
}

// Component mounted, get data
onMounted(() => {
  getMessageList()
  getClasses()  // 预加载班级数据
  getCourses()  // 预加载课程数据
})
</script>

<template>
  <div class="message-manage-container">
    <div class="page-header">
      <h2>通知管理</h2>
      <p>管理系统通知信息，包括创建、编辑和删除通知</p>
    </div>
    
    <!-- Search toolbar -->
    <el-card class="search-card">
      <el-form :model="queryParams" ref="queryForm" :inline="true">
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="通知标题或内容"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 操作工具栏 -->
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增通知</el-button>
      <el-button :icon="RefreshRight" @click="refreshList">刷新</el-button>
    </div>
    
    <!-- Message list -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="messageList"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" width="50" align="center" />
        <el-table-column prop="id" label="ID" width="80" sortable align="center" />
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="接收对象" width="120" align="center">
          <template #default="scope">
            {{ formatTargetType(scope.row.targetType) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="expiresAt" label="过期时间" width="180" align="center">
          <template #default="scope">
            {{ formatDate(scope.row.expiresAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="active" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.active ? 'success' : 'info'">
              {{ scope.row.active ? '活跃' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="scope">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button :type="scope.row.active ? 'warning' : 'success'" link :icon="Bell" @click="toggleStatus(scope.row.id)">
              {{ scope.row.active ? '停用' : '启用' }}
            </el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          :page-size="10"
          @current-change="page => getMessageList()"
        />
      </div>
    </el-card>
    
    <!-- Message dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="messageFormRef"
        :model="messageForm"
        :rules="rules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="通知标题" prop="title">
          <el-input v-model="messageForm.title" placeholder="请输入通知标题" />
        </el-form-item>
        <el-form-item label="通知内容" prop="content">
          <el-input
            v-model="messageForm.content"
            type="textarea"
            rows="5"
            placeholder="请输入通知内容"
          />
        </el-form-item>
        <el-form-item label="接收对象" prop="targetType">
          <el-select v-model="messageForm.targetType" placeholder="请选择接收对象" @change="handleTargetTypeChange">
            <el-option
              v-for="item in targetTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="接收者ID"
          prop="targetIds"
          v-if="messageForm.targetType === 'SPECIFIC'"
        >
          <el-input
            v-model="messageForm.targetIds"
            placeholder="请输入接收者ID，多个ID用逗号分隔"
          />
        </el-form-item>
        <el-form-item
          label="接收班级"
          prop="targetIds"
          v-if="messageForm.targetType === 'CLASS'"
        >
          <el-select
            v-model="messageForm.targetIds"
            placeholder="请选择班级"
            multiple
            filterable
            :loading="classesLoading"
          >
            <el-option
              v-for="item in classesList"
              :key="item.id"
              :label="item.className"
              :value="item.id.toString()"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="接收课程"
          prop="targetIds"
          v-if="messageForm.targetType === 'COURSE'"
        >
          <el-select
            v-model="messageForm.targetIds"
            placeholder="请选择课程"
            multiple
            filterable
            :loading="coursesLoading"
          >
            <el-option
              v-for="item in coursesList"
              :key="item.courseId"
              :label="item.name"
              :value="item.courseId.toString()"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间" prop="expiresAt">
          <el-date-picker
            v-model="messageForm.expiresAt"
            type="datetime"
            placeholder="选择过期时间（可选）"
          />
        </el-form-item>
        <el-form-item label="状态" prop="active">
          <el-switch
            v-model="messageForm.active"
            active-text="活跃"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm(messageFormRef)">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.message-manage-container {
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
}

.table-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.dialog-footer {
  text-align: right;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .el-form-item {
    margin-bottom: 10px;
  }
  
  .toolbar {
    flex-direction: column;
  }
  
  .pagination {
    justify-content: center;
  }
}
</style> 