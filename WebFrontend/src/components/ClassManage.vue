<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, RefreshRight, DataAnalysis, Reading } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'
import { useRouter } from 'vue-router'
import EnrollmentStats from './EnrollmentStats.vue'

const router = useRouter()
const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 班级列表数据
const classList = ref([])
const loading = ref(false)
const total = ref(0)

// 表单引用
const classFormRef = ref(null)

// 分页参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

// 表单数据
const classForm = reactive({
  id: undefined,
  className: ''
})

// 对话框控制
const dialogVisible = ref(false)
const dialogTitle = ref('添加班级')
const formMode = ref('add')

// 选课情况对话框
const enrollmentDialogVisible = ref(false)
const currentClassId = ref(null)
const currentClassName = ref('')

// 表单校验规则
const rules = {
  className: [
    { required: true, message: '请输入班级名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ]
}

// 获取班级列表
const getClassList = async () => {
  loading.value = true
  try {
    // 构建请求参数
    let url = BaseUrl + 'classes'
    let params = {}
    
    // 如果有搜索关键词，使用搜索接口
    if (queryParams.keyword) {
      url = BaseUrl + 'classes/search'
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
      classList.value = response.data
      total.value = response.data.length
    } else {
      classList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取班级列表失败:', error)
    ElMessage.error('获取班级列表失败')
  } finally {
    loading.value = false
  }
}

// 打开添加班级对话框
const handleAdd = () => {
  resetForm()
  dialogTitle.value = '添加班级'
  formMode.value = 'add'
  dialogVisible.value = true
}

// 打开编辑班级对话框
const handleEdit = (row) => {
  resetForm()
  dialogTitle.value = '编辑班级'
  formMode.value = 'edit'
  
  // 填充表单数据
  Object.keys(classForm).forEach(key => {
    if (key in row) {
      classForm[key] = row[key]
    }
  })
  
  dialogVisible.value = true
}

// 删除班级
const handleDelete = (id) => {
  ElMessageBox.confirm('确认要删除该班级吗？删除班级不会删除关联的学生，但会解除关联关系。', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`${BaseUrl}classes/${id}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      ElMessage.success('删除成功')
      getClassList()
    } catch (error) {
      console.error('删除班级失败:', error)
      ElMessage.error('删除班级失败')
    }
  }).catch(() => {})
}

// 查看班级数据
const viewClassData = (classId) => {
  router.push(`/class-data/${classId}`)
}

// 提交表单
const submitForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const data = { ...classForm }
        
        // 检查班级名是否已存在
        if (formMode.value === 'add') {
          const checkResponse = await axios.get(`${BaseUrl}classes/exists`, {
            params: { className: data.className },
            headers: {
              'Authorization': `Bearer ${getToken()}`
            }
          })
          
          if (checkResponse.data === true) {
            ElMessage.warning('班级名称已存在')
            return
          }
        }
        
        if (formMode.value === 'add') {
          await axios.post(BaseUrl + 'classes', data, {
            headers: {
              'Authorization': `Bearer ${getToken()}`
            }
          })
          ElMessage.success('添加成功')
        } else {
          await axios.put(`${BaseUrl}classes/${data.id}`, data, {
            headers: {
              'Authorization': `Bearer ${getToken()}`
            }
          })
          ElMessage.success('更新成功')
        }
        
        dialogVisible.value = false
        getClassList()
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error('操作失败: ' + (error.response?.data?.message || error.message))
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  // 重置表单数据
  Object.keys(classForm).forEach(key => {
    classForm[key] = key === 'id' ? undefined : ''
  })
  
  // 如果表单引用存在，重置校验状态
  if (classFormRef.value) {
    classFormRef.value.resetFields()
  }
}

// 刷新列表
const refreshList = () => {
  queryParams.pageNum = 1
  getClassList()
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  getClassList()
}

// 重置搜索
const resetSearch = () => {
  queryParams.keyword = ''
  refreshList()
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString()
}

// 查看班级选课情况
const viewClassEnrollments = (row) => {
  currentClassId.value = row.id
  currentClassName.value = row.className
  enrollmentDialogVisible.value = true
}

// 获取学生状态显示
const getStatusDisplay = (status) => {
  switch(status) {
    case 'enrolled': return '进行中'
    case 'completed': return '已完成'
    case 'withdrawn': return '已退课'
    default: return status || '未知状态'
  }
}

// 获取状态对应的标签类型
const getStatusType = (status) => {
  switch(status) {
    case 'enrolled': return 'warning'
    case 'completed': return 'success'
    case 'withdrawn': return 'info'
    default: return 'info'
  }
}

// 生命周期钩子
onMounted(() => {
  getClassList()
})
</script>

<template>
  <div class="container">
    <div class="page-header mb-lg">
      <h2 class="text-2xl mb-sm">班级管理</h2>
      <p class="text-secondary">管理学生班级信息，包括添加、编辑和删除班级</p>
    </div>
    
    <!-- 搜索工具栏 -->
    <el-card class="mb-md">
      <div class="card-body">
        <el-form :model="queryParams" ref="queryForm" :inline="true">
          <el-form-item label="关键词">
            <el-input
              v-model="queryParams.keyword"
              placeholder="班级名称"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <div class="d-flex gap-sm">
              <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
              <el-button :icon="RefreshRight" @click="resetSearch">重置</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    
    <!-- 操作工具栏 -->
    <div class="d-flex justify-between mb-md">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增班级</el-button>
      <el-button :icon="RefreshRight" @click="refreshList">刷新</el-button>
    </div>
    
    <!-- 班级表格 -->
    <el-card class="mb-lg">
      <div class="card-body">
        <el-table
          v-loading="loading"
          :data="classList"
          border
          stripe
          style="width: 100%"
        >
          <el-table-column type="index" width="50" />
          <el-table-column prop="className" label="班级名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="students.length" label="学生数量" min-width="100" />
          <el-table-column prop="createdAt" label="创建时间" min-width="180" show-overflow-tooltip>
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" min-width="180" show-overflow-tooltip>
            <template #default="scope">
              {{ formatDate(scope.row.updatedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="scope">
              <div class="d-flex gap-sm">
                <el-button type="primary" link :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
                <el-button type="danger" link :icon="Delete" @click="handleDelete(scope.row.id)">删除</el-button>
                <el-button type="success" link :icon="DataAnalysis" @click="viewClassData(scope.row.id)">数据查看</el-button>
                <el-button type="info" link :icon="Reading" @click="viewClassEnrollments(scope.row)">选课情况</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div class="pagination-container mt-md d-flex justify-end">
          <el-pagination
            v-model:current-page="queryParams.pageNum"
            v-model:page-size="queryParams.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="getClassList"
            @current-change="getClassList"
          />
        </div>
      </div>
    </el-card>
    
    <!-- 添加/编辑班级对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      append-to-body
    >
      <el-form
        ref="classFormRef"
        :model="classForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="classForm.className" placeholder="请输入班级名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="d-flex justify-end gap-sm">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm(classFormRef)">确定</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 使用新的选课情况组件 -->
    <EnrollmentStats
      :visible="enrollmentDialogVisible"
      @update:visible="val => enrollmentDialogVisible = val"
      type="class"
      :id="currentClassId"
      :name="currentClassName"
    />
  </div>
</template>

<style scoped>
/* 使用全局样式，仅保留特殊样式 */
.statistics-content {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: var(--spacing-lg);
}

.stat-item {
  text-align: center;
  min-width: 100px;
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

.stat-value {
  font-size: var(--text-xl);
  font-weight: bold;
  color: var(--text-primary);
  margin-top: 5px;
}
</style> 