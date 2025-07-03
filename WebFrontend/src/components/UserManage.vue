<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, RefreshRight } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'

const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 用户列表数据
const userList = ref([])
const loading = ref(false)
const total = ref(0)

// 表单引用
const userFormRef = ref(null)

// 分页参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

// 表单数据
const userForm = reactive({
  id: undefined,
  username: '',
  password: '',
  name: '',
  email: '',
  phone: '',
  studentNumber: '',
  userRole: 'STUDENT',
  schoolClass: null,
  schoolClassId: null
})

// 班级列表
const classList = ref([])

// 对话框控制
const dialogVisible = ref(false)
const dialogTitle = ref('添加用户')
const formMode = ref('add')

// 动态表单校验规则
const getPasswordRules = () => {
  return [
    { required: formMode.value === 'add', message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

// 表单校验规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度在 3 到 50 个字符', trigger: 'blur' }
  ],
  password: getPasswordRules(),
  name: [
    { max: 100, message: '长度不能超过 100 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  userRole: [
    { required: true, message: '请选择用户角色', trigger: 'change' }
  ]
}

// 获取用户列表
const getUserList = async () => {
  loading.value = true
  try {
    // 构建请求参数
    let url = BaseUrl + 'users'
    let params = {}
    
    // 如果有搜索关键词，添加到请求参数
    if (queryParams.keyword) {
      params.keyword = queryParams.keyword
    }
    
    // 添加分页参数
    params.pageNum = queryParams.pageNum
    params.pageSize = queryParams.pageSize
    
    const response = await axios.get(url, {
      params: params,
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    // 如果后端返回的是分页数据结构
    if (response.data.content && Array.isArray(response.data.content)) {
      userList.value = response.data.content
      total.value = response.data.totalElements || response.data.content.length
    } else if (Array.isArray(response.data)) {
      // 如果后端直接返回数组
      userList.value = response.data
      total.value = response.data.length
      
      // 如果有关键词，前端过滤
      if (queryParams.keyword) {
        const keyword = queryParams.keyword.toLowerCase()
        userList.value = userList.value.filter(user => 
          (user.username && user.username.toLowerCase().includes(keyword)) ||
          (user.name && user.name.toLowerCase().includes(keyword)) ||
          (user.studentNumber && user.studentNumber.toLowerCase().includes(keyword))
        )
        total.value = userList.value.length
      }
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 获取班级列表
const getClassList = async () => {
  try {
    const response = await axios.get(BaseUrl + 'classes', {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    classList.value = response.data
  } catch (error) {
    console.error('获取班级列表失败:', error)
  }
}

// 打开添加用户对话框
const handleAdd = () => {
  resetForm()
  dialogTitle.value = '添加用户'
  formMode.value = 'add'
  // 更新密码校验规则
  rules.password = getPasswordRules()
  dialogVisible.value = true
}

// 打开编辑用户对话框
const handleEdit = (row) => {
  resetForm()
  dialogTitle.value = '编辑用户'
  formMode.value = 'edit'
  
  // 填充表单数据
  Object.keys(userForm).forEach(key => {
    if (key in row && key !== 'schoolClass' && key !== 'schoolClassId') {
      userForm[key] = row[key]
    }
  })
  
  // 特殊处理班级ID
  if (row.schoolClass) {
    // 如果班级数据是完整对象，设置ID
    if (typeof row.schoolClass === 'object' && row.schoolClass !== null) {
      userForm.schoolClassId = row.schoolClass.id
    } 
    // 如果只有ID，直接使用
    else if (typeof row.schoolClass === 'number' || typeof row.schoolClass === 'string') {
      userForm.schoolClassId = Number(row.schoolClass)
    }
  }
  
  // 更新密码校验规则
  rules.password = getPasswordRules()
  
  dialogVisible.value = true
}

// 删除用户
const handleDelete = (id) => {
  ElMessageBox.confirm('确认要删除该用户吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`${BaseUrl}users/${id}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      ElMessage.success('删除成功')
      getUserList()
    } catch (error) {
      console.error('删除用户失败:', error)
      ElMessage.error('删除用户失败')
    }
  }).catch(() => {})
}

// 提交表单
const submitForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const data = { ...userForm }
        
        // 如果是编辑模式，不传密码和用户名字段
        if (formMode.value === 'edit') {
          delete data.password
          delete data.username
        }
        
        // 处理班级数据，确保传递正确的格式
        if (data.schoolClassId) {
          data.schoolClass = { id: data.schoolClassId }
        } else {
          data.schoolClass = null
        }
        
        // 删除辅助字段
        delete data.schoolClassId
        
        if (formMode.value === 'add') {
          await axios.post(BaseUrl + 'users/register', data, {
            headers: {
              'Authorization': `Bearer ${getToken()}`
            }
          })
          ElMessage.success('添加成功')
        } else {
          await axios.put(`${BaseUrl}users/${data.id}`, data, {
            headers: {
              'Authorization': `Bearer ${getToken()}`
            }
          })
          ElMessage.success('更新成功')
        }
        
        dialogVisible.value = false
        getUserList()
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
  Object.keys(userForm).forEach(key => {
    if (key === 'userRole') {
      userForm[key] = 'STUDENT'
    } else {
      userForm[key] = key === 'id' ? undefined : ''
    }
  })
  
  // 确保schoolClass和schoolClassId为null
  userForm.schoolClass = null
  userForm.schoolClassId = null
  
  // 如果表单引用存在，重置校验状态
  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }
}

// 刷新列表
const refreshList = () => {
  queryParams.pageNum = 1
  getUserList()
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  getUserList()
}

// 重置搜索
const resetSearch = () => {
  queryParams.keyword = ''
  refreshList()
}

// 格式化角色显示
const formatRole = (role) => {
  const roleMap = {
    'ADMIN': '管理员',
    'TEACHER': '教师',
    'STUDENT': '学生'
  }
  return roleMap[role] || role
}

// 生命周期钩子
onMounted(() => {
  getUserList()
  getClassList()
})
</script>

<template>
  <div class="user-manage-container">
    <div class="page-header">
      <h2>用户管理</h2>
      <p>管理系统用户信息，包括管理员、教师和学生</p>
    </div>
    
    <!-- 搜索工具栏 -->
    <el-card class="search-card">
      <el-form :model="queryParams" ref="queryForm" :inline="true">
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="用户名/姓名/学号"
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
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增用户</el-button>
    </div>
    
    <!-- 用户表格 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="userList"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" width="50" />
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="studentNumber" label="学号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="userRole" label="角色" min-width="80">
          <template #default="scope">
            <el-tag :type="scope.row.userRole === 'ADMIN' ? 'danger' : scope.row.userRole === 'TEACHER' ? 'warning' : 'success'">
              {{ formatRole(scope.row.userRole) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="150" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="schoolClass.className" label="班级" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(scope.row.id)">删除</el-button>
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
          @size-change="getUserList"
          @current-change="getUserList"
        />
      </div>
    </el-card>
    
    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="600px"
      append-to-body
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="userForm.username" 
            placeholder="请输入用户名" 
            :disabled="formMode === 'edit'"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="formMode === 'add'">
          <el-input
            v-model="userForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="学号" prop="studentNumber">
          <el-input v-model="userForm.studentNumber" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="角色" prop="userRole">
          <el-select v-model="userForm.userRole" placeholder="请选择角色">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="schoolClass">
          <el-select
            v-model="userForm.schoolClassId"
            placeholder="请选择班级"
            filterable
            clearable
          >
            <el-option
              v-for="item in classList"
              :key="item.id"
              :label="item.className"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm(userFormRef)">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-manage-container {
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
