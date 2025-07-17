<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, RefreshRight, DataAnalysis, User, Files, Notebook, Link } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'
import { useRouter } from 'vue-router'
import CourseFileManage from './CourseFileManage.vue'
import EnrollmentStats from './EnrollmentStats.vue'
import HomeworkManage from './HomeworkManage.vue'

const router = useRouter()
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

// 选课情况对话框和课程资源管理对话框共用的变量
const enrollmentDialogVisible = ref(false)
const courseFileDialogVisible = ref(false)
const homeworkDialogVisible = ref(false)
const currentCourseId = ref(null)
const currentCourseName = ref('')

// RAG管理相关变量
const ragDialogVisible = ref(false)
const ragList = ref([])
const availableRags = ref([])
const ragLoading = ref(false)
const selectedRag = ref('')

// 打开课程资源管理对话框
const openCourseFileDialog = (course) => {
  currentCourseId.value = course.courseId
  currentCourseName.value = course.name
  courseFileDialogVisible.value = true
}

// 打开课程作业管理对话框
const openHomeworkDialog = (course) => {
  currentCourseId.value = course.courseId
  currentCourseName.value = course.name
  homeworkDialogVisible.value = true
}

// 打开RAG管理对话框
const openRagDialog = async (course) => {
  currentCourseId.value = course.courseId
  currentCourseName.value = course.name
  ragDialogVisible.value = true
  await getCourseRags()
  await getAvailableRags()
}

// 获取课程关联的RAG
const getCourseRags = async () => {
  if (!currentCourseId.value) return
  
  ragLoading.value = true
  try {
    const response = await axios.get(`${BaseUrl}courses/${currentCourseId.value}/rags`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (Array.isArray(response.data)) {
      ragList.value = response.data
    } else {
      ragList.value = []
    }
  } catch (error) {
    console.error('获取课程RAG列表失败:', error)
    ElMessage.error('获取课程RAG列表失败')
  } finally {
    ragLoading.value = false
  }
}

// 获取所有可用的RAG
const getAvailableRags = async () => {
  try {
    const response = await axios.get(`${BaseUrl}api/rag`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && response.data.data && Array.isArray(response.data.data)) {
      // 过滤掉已经关联的RAG
      const currentRagIds = ragList.value.map(rag => rag.id)
      availableRags.value = response.data.data.filter(rag => !currentRagIds.includes(rag.id))
    } else {
      availableRags.value = []
    }
  } catch (error) {
    console.error('获取可用RAG列表失败:', error)
    ElMessage.error('获取可用RAG列表失败')
  }
}

// 添加RAG到课程
const addRagToCourse = async () => {
  if (!selectedRag.value) {
    ElMessage.warning('请选择要添加的RAG')
    return
  }
  
  try {
    await axios.post(`${BaseUrl}courses/${currentCourseId.value}/rags/${selectedRag.value}`, null, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    ElMessage.success('RAG关联成功')
    selectedRag.value = ''
    await getCourseRags()
    await getAvailableRags()
  } catch (error) {
    console.error('添加RAG到课程失败:', error)
    ElMessage.error('添加RAG到课程失败: ' + (error.response?.data?.message || error.message))
  }
}

// 从课程移除RAG
const removeRagFromCourse = async (ragId) => {
  ElMessageBox.confirm('确认要移除该RAG关联吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`${BaseUrl}courses/${currentCourseId.value}/rags/${ragId}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      })
      
      ElMessage.success('RAG关联已移除')
      await getCourseRags()
      await getAvailableRags()
    } catch (error) {
      console.error('移除RAG关联失败:', error)
      ElMessage.error('移除RAG关联失败: ' + (error.response?.data?.message || error.message))
    }
  }).catch(() => {})
}

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

// 查看课程数据
const viewCourseData = (courseId) => {
  router.push(`/course-manage-data/${courseId}`)
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

// 查看选课情况
const viewEnrollments = (row) => {
  currentCourseId.value = row.courseId
  currentCourseName.value = row.name
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
  getCourseList()
})
</script>

<template>
  <div class="container">
    <div class="page-header mb-lg">
      <h2 class="text-2xl mb-sm">课程管理</h2>
      <p class="text-secondary">管理课程信息，包括创建、编辑和删除课程</p>
    </div>
    
    <!-- 搜索工具栏 -->
    <el-card class="mb-md">
      <div class="card-body">
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
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增课程</el-button>
      <el-button :icon="RefreshRight" @click="refreshList">刷新</el-button>
    </div>
    
    <!-- 课程列表 -->
    <el-card class="mb-lg">
      <div class="card-body">
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
          <el-table-column label="操作" width="520" fixed="right">
            <template #default="scope">
              <div class="d-flex gap-sm flex-wrap">
                <el-button type="primary" link :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
                <el-button type="danger" link :icon="Delete" @click="handleDelete(scope.row.courseId)">删除</el-button>
                <el-button type="success" link :icon="DataAnalysis" @click="viewCourseData(scope.row.courseId)">数据查看</el-button>
                <el-button type="info" link :icon="User" @click="viewEnrollments(scope.row)">选课情况</el-button>
                <el-button type="warning" link :icon="Files" @click="openCourseFileDialog(scope.row)">课程资源</el-button>
                <el-button type="primary" link :icon="Notebook" @click="openHomeworkDialog(scope.row)">作业管理</el-button>
                <el-button type="success" link :icon="Link" @click="openRagDialog(scope.row)">RAG管理</el-button>
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
            @size-change="getCourseList"
            @current-change="getCourseList"
          />
        </div>
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
        <div class="d-flex justify-end gap-sm">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm(courseFormRef)">确定</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 使用新的选课情况组件 -->
    <EnrollmentStats
      :visible="enrollmentDialogVisible"
      @update:visible="val => enrollmentDialogVisible = val"
      type="course"
      :id="currentCourseId"
      :name="currentCourseName"
    />
    
    <!-- 课程资源管理对话框 -->
    <CourseFileManage
      :visible="courseFileDialogVisible"
      @update:visible="val => courseFileDialogVisible = val"
      :courseId="currentCourseId"
      :courseName="currentCourseName"
      @refresh="getCourseList"
    />
    
    <!-- 课程作业管理对话框 -->
    <HomeworkManage
      :visible="homeworkDialogVisible"
      @update:visible="val => homeworkDialogVisible = val"
      :courseId="currentCourseId"
      :courseName="currentCourseName"
      @refresh="getCourseList"
    />
    
    <!-- RAG管理对话框 -->
    <el-dialog
      title="RAG知识库管理"
      v-model="ragDialogVisible"
      width="700px"
      append-to-body
    >
      <div v-if="currentCourseId">
        <h3 class="mb-md">{{ currentCourseName }} - RAG知识库关联</h3>
        
        <!-- 添加RAG关联部分 -->
        <div class="mb-lg">
          <el-card>
            <template #header>
              <div class="d-flex justify-between align-center">
                <h4>添加RAG关联</h4>
              </div>
            </template>
            
            <div class="d-flex gap-md mb-md">
              <el-select v-model="selectedRag" placeholder="选择要关联的RAG" style="width: 70%">
                <el-option
                  v-for="rag in availableRags"
                  :key="rag.id"
                  :label="rag.name"
                  :value="rag.id"
                />
              </el-select>
              <el-button type="primary" @click="addRagToCourse" :disabled="!selectedRag">添加关联</el-button>
            </div>
          </el-card>
        </div>
        
        <!-- 已关联的RAG列表 -->
        <el-card>
          <template #header>
            <div class="d-flex justify-between align-center">
              <h4>已关联的RAG知识库</h4>
              <el-button type="primary" :icon="RefreshRight" circle @click="getCourseRags" />
            </div>
          </template>
          
          <el-table
            v-loading="ragLoading"
            :data="ragList"
            border
            stripe
            style="width: 100%"
          >
            <el-table-column type="index" width="50" align="center" />
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="name" label="RAG名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="120" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'COMPLETED' ? 'success' : 'warning'">
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="160" align="center">
              <template #default="scope">
                {{ formatDate(scope.row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="scope">
                <el-button type="danger" link :icon="Delete" @click="removeRagFromCourse(scope.row.id)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <div class="empty-block" v-if="ragList.length === 0 && !ragLoading">
            <span class="empty-text">暂无关联的RAG知识库</span>
          </div>
        </el-card>
      </div>
      
      <template #footer>
        <div class="d-flex justify-end gap-sm">
          <el-button @click="ragDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 由于使用了全局样式，此处保留特殊样式覆盖或组件特有样式 */
@media (max-width: 768px) {
  .page-header h2 {
    font-size: var(--text-lg);
  }
}

.empty-block {
  min-height: 60px;
  text-align: center;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.empty-text {
  color: #909399;
}
</style> 