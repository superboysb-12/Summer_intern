<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, RefreshRight } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'

const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 课程列表数据
const courseList = ref([])
const loading = ref(false)
const total = ref(0)

// 表单引用
const courseFormRef = ref(null)

// 分页参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

// 表单数据
const courseForm = reactive({
  courseId: undefined,
  name: '',
  description: ''
})

// 对话框控制
const dialogVisible = ref(false)
const dialogTitle = ref('添加课程')
const formMode = ref('add')

// 表单校验规则
const rules = {
  name: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  description: [
    { max: 500, message: '长度不能超过 500 个字符', trigger: 'blur' }
  ]
}

// 获取课程列表
const getCourseList = async () => {
  loading.value = true
  try {
    // 构建请求参数
    let url = BaseUrl + 'courses'
    let params = {}
    
    // 如果有搜索关键词，使用搜索接口
    if (queryParams.keyword) {
      url = BaseUrl + 'courses/search'
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
      courseList.value = response.data
      total.value = response.data.length
    } else {
      courseList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取课程列表失败:', error)
    ElMessage.error('获取课程列表失败')
  } finally {
    loading.value = false
  }
}

// 打开添加课程对话框
const handleAdd = () => {
  resetForm()
  dialogTitle.value = '添加课程'
  formMode.value = 'add'
  dialogVisible.value = true
}

// 打开编辑课程对话框
const handleEdit = (row) => {
  resetForm()
  dialogTitle.value = '编辑课程'
  formMode.value = 'edit'
  
  // 填充表单数据
  Object.keys(courseForm).forEach(key => {
    if (key in row) {
      courseForm[key] = row[key]
    }
  })
  
  dialogVisible.value = true
}

// 删除课程
const handleDelete = (id) => {
  ElMessageBox.confirm('确认要删除该课程吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`${BaseUrl}courses/${id}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      ElMessage.success('删除成功')
      getCourseList()
    } catch (error) {
      console.error('删除课程失败:', error)
      ElMessage.error('删除课程失败')
    }
  }).catch(() => {})
}

// 提交表单
const submitForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const data = { ...courseForm }
        
        // 检查课程名是否已存在
        if (formMode.value === 'add') {
          const checkResponse = await axios.get(`${BaseUrl}courses/exists`, {
            params: { name: data.name },
            headers: {
              'Authorization': `Bearer ${getToken()}`
            }
          })
          
          if (checkResponse.data === true) {
            ElMessage.warning('课程名称已存在')
            return
          }
        }
        
        if (formMode.value === 'add') {
          await axios.post(BaseUrl + 'courses', data, {
            headers: {
              'Authorization': `Bearer ${getToken()}`
            }
          })
          ElMessage.success('添加成功')
        } else {
          await axios.put(`${BaseUrl}courses/${data.courseId}`, data, {
            headers: {
              'Authorization': `Bearer ${getToken()}`
            }
          })
          ElMessage.success('更新成功')
        }
        
        dialogVisible.value = false
        getCourseList()
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
  Object.keys(courseForm).forEach(key => {
    courseForm[key] = key === 'courseId' ? undefined : ''
  })
  
  // 如果表单引用存在，重置校验状态
  if (courseFormRef.value) {
    courseFormRef.value.resetFields()
  }
}

// 刷新列表
const refreshList = () => {
  queryParams.pageNum = 1
  getCourseList()
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  getCourseList()
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

// 生命周期钩子
onMounted(() => {
  getCourseList()
})
</script>

<template>
  <div class="course-manage-container">
    <div class="page-header">
      <h2>课程管理</h2>
      <p>管理系统课程信息，包括创建、编辑和删除课程</p>
    </div>
    
    <!-- 搜索工具栏 -->
    <el-card class="search-card">
      <el-form :model="queryParams" ref="queryForm" :inline="true">
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="课程名称或描述"
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
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增课程</el-button>
      <el-button :icon="RefreshRight" @click="refreshList">刷新</el-button>
    </div>
    
    <!-- 课程列表 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="courseList"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" width="50" align="center" />
        <el-table-column prop="courseId" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="课程名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="description" label="课程描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" min-width="160" align="center">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="160" align="center">
          <template #default="scope">
            {{ formatDate(scope.row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(scope.row.courseId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="getCourseList"
          @current-change="getCourseList"
        />
      </div>
    </el-card>
    
    <!-- 添加/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      append-to-body
    >
      <el-form
        ref="courseFormRef"
        :model="courseForm"
        :rules="rules"
        label-width="80px"
        label-position="right"
      >
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="courseForm.name" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程描述" prop="description">
          <el-input
            v-model="courseForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入课程描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm(courseFormRef)">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
  .course-manage-container {
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