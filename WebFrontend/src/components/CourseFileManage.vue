<script setup>
import { ref, onMounted, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, RefreshRight, Document, Picture, Download, Upload, Link, View, Hide, Folder } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'

const props = defineProps({
  courseId: {
    type: [Number, String],
    required: true
  },
  courseName: {
    type: String,
    default: ''
  },
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'refresh'])

const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')
const userInfo = ref(store.getUserInfo())
const isTeacher = ref(userInfo.value.userRole === 'TEACHER')
const isAdmin = ref(userInfo.value.userRole === 'ADMIN')

// 课程文件列表
const courseFiles = ref([])
const allFiles = ref([])
const loading = ref(false)
const fileDetailsMap = ref({}) // 存储文件详情信息

// 查询参数
const queryParams = reactive({
  resourceType: '',
  isVisible: true
})

// 资源类型选项
const resourceTypeOptions = [
  { label: '全部类型', value: '' },
  { label: '教学课件', value: 'SLIDES' },
  { label: '练习资料', value: 'EXERCISE' },
  { label: '参考资料', value: 'REFERENCE' },
  { label: '视频资源', value: 'VIDEO' },
  { label: '其他资源', value: 'OTHER' }
]

// 关联文件表单
const linkFileDialogVisible = ref(false)
const linkFileForm = reactive({
  courseId: null,
  fileId: null,
  resourceType: 'OTHER',
  description: '',
  isVisible: true
})

// 表单规则
const linkFileRules = {
  fileId: [
    { required: true, message: '请选择文件', trigger: 'change' }
  ],
  resourceType: [
    { required: true, message: '请选择资源类型', trigger: 'change' }
  ],
  description: [
    { max: 200, message: '描述不能超过200个字符', trigger: 'blur' }
  ]
}

const linkFileFormRef = ref(null)

// 批量关联文件对话框
const batchLinkDialogVisible = ref(false)
const selectedFiles = ref([])
const batchLinkForm = reactive({
  courseId: null,
  fileIds: [],
  resourceType: 'OTHER',
  isVisible: true
})

// 打开文件夹关联对话框
const folderLinkDialogVisible = ref(false)
const folderLinkForm = reactive({
  courseId: null,
  folderId: null,
  resourceType: 'OTHER',
  isVisible: true
})

// 加载文件夹列表
const folders = ref([])
const loadFolders = async () => {
  try {
    // 使用通用的文件列表API
    const response = await axios.get(`${BaseUrl}files`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    // 筛选只包含文件夹的数据
    const allItems = response.data && response.data.data ? response.data.data : response.data || []
    folders.value = allItems.filter(item => item.directory === true || item.isDirectory === true)
    
    console.log('加载文件夹:', folders.value)
  } catch (error) {
    console.error('获取文件夹列表失败:', error)
    ElMessage.error('获取文件夹列表失败')
  }
}

// 打开关联文件夹对话框
const openFolderLinkDialog = () => {
  folderLinkForm.courseId = props.courseId
  folderLinkForm.folderId = null
  folderLinkForm.resourceType = 'OTHER'
  folderLinkForm.isVisible = true
  folderLinkDialogVisible.value = true
  loadFolders()
}

// 提交文件夹关联
const submitFolderLink = async () => {
  if (!folderLinkForm.folderId) {
    ElMessage.warning('请选择文件夹')
    return
  }
  
  try {
    const response = await axios.post(`${BaseUrl}course-files/associate-folder`, folderLinkForm, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    if (response.data && response.data.count) {
      ElMessage.success(`成功关联 ${response.data.count} 个文件`)
    } else {
      ElMessage.info('文件夹为空或所有文件已关联')
    }
    
    folderLinkDialogVisible.value = false
    loadCourseFiles()
  } catch (error) {
    console.error('关联文件夹失败:', error)
    ElMessage.error('关联文件夹失败: ' + (error.response?.data?.error || error.message))
  }
}

// 监听对话框可见性变化
watch(() => props.visible, (newVal) => {
  if (newVal && props.courseId) {
    loadCourseFiles()
  }
})

// 加载课程关联的文件
const loadCourseFiles = async () => {
  loading.value = true
  try {
    let endpoint = `${BaseUrl}course-files/course/${props.courseId}`
    
    // 根据筛选条件选择不同的API端点
    if (queryParams.resourceType && queryParams.isVisible === true) {
      endpoint = `${BaseUrl}course-files/course/${props.courseId}/type/${queryParams.resourceType}/visible`
    } else if (queryParams.resourceType) {
      endpoint = `${BaseUrl}course-files/course/${props.courseId}/type/${queryParams.resourceType}`
    } else if (queryParams.isVisible === true) {
      endpoint = `${BaseUrl}course-files/course/${props.courseId}/visible`
    }
    
    const response = await axios.get(endpoint, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    courseFiles.value = response.data || []
    
    // 获取每个文件的详细信息
    await fetchFileDetails()
  } catch (error) {
    console.error('获取课程文件失败:', error)
    ElMessage.error('获取课程文件失败: ' + (error.response?.data?.error || error.message))
  } finally {
    loading.value = false
  }
}

// 获取文件批量信息
const fetchFileDetails = async () => {
  const fileIds = courseFiles.value.map(cf => cf.fileId)
  if (fileIds.length === 0) return
  
  try {
    // 逐个获取文件信息，因为后端没有提供批量获取API
    const detailsMap = {}
    
    await Promise.all(fileIds.map(async (fileId) => {
      try {
        const response = await axios.get(`${BaseUrl}files/${fileId}/info`, {
          headers: { 'Authorization': `Bearer ${getToken()}` }
        })
        
        if (response.data && response.data.data) {
          detailsMap[fileId] = response.data.data
        }
      } catch (err) {
        console.error(`获取文件ID ${fileId} 的详情失败:`, err)
      }
    }))
    
    fileDetailsMap.value = detailsMap
  } catch (error) {
    console.error('获取文件详情失败:', error)
  }
}

// 加载所有文件（用于关联）
const loadAllFiles = async () => {
  try {
    // 在FileController中，此API返回的数据应该包含在data字段中
    const response = await axios.get(`${BaseUrl}files`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    // 检查响应格式并正确获取数据
    allFiles.value = response.data && response.data.data ? response.data.data : response.data || []
  } catch (error) {
    console.error('获取文件列表失败:', error)
    ElMessage.error('获取文件列表失败')
  }
}

// 打开关联文件对话框
const openLinkFileDialog = () => {
  resetLinkFileForm()
  linkFileForm.courseId = props.courseId
  linkFileDialogVisible.value = true
  loadAllFiles()
}

// 重置关联文件表单
const resetLinkFileForm = () => {
  linkFileForm.fileId = null
  linkFileForm.resourceType = 'OTHER'
  linkFileForm.description = ''
  linkFileForm.isVisible = true
  
  if (linkFileFormRef.value) {
    linkFileFormRef.value.resetFields()
  }
}

// 提交关联文件
const submitLinkFile = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        // 修改为正确的后端API endpoint
        await axios.post(`${BaseUrl}course-files/associate`, {
          courseId: linkFileForm.courseId,
          fileId: linkFileForm.fileId,
          resourceType: linkFileForm.resourceType,
          description: linkFileForm.description,
          isVisible: linkFileForm.isVisible
        }, {
          headers: { 'Authorization': `Bearer ${getToken()}` }
        })
        
        ElMessage.success('关联文件成功')
        linkFileDialogVisible.value = false
        loadCourseFiles()
      } catch (error) {
        console.error('关联文件失败:', error)
        ElMessage.error('关联文件失败: ' + (error.response?.data?.error || error.message))
      }
    }
  })
}

// 取消关联文件
const unlinkFile = (courseFile) => {
  ElMessageBox.confirm('确认要取消该文件与课程的关联吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`${BaseUrl}course-files/${courseFile.id}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      
      ElMessage.success('取消关联成功')
      loadCourseFiles()
    } catch (error) {
      console.error('取消关联失败:', error)
      ElMessage.error('取消关联失败')
    }
  }).catch(() => {})
}

// 更新文件关联信息
const updateCourseFile = (courseFile) => {
  ElMessageBox.confirm('确认要更新该文件的关联信息吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // 切换可见性
      await axios.put(`${BaseUrl}course-files/${courseFile.id}/visibility`, {
        isVisible: !courseFile.isVisible
      }, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      
      ElMessage.success('更新关联信息成功')
      loadCourseFiles()
    } catch (error) {
      console.error('更新关联信息失败:', error)
      ElMessage.error('更新关联信息失败: ' + (error.response?.data?.error || error.message))
    }
  }).catch(() => {})
}

// 下载文件
const downloadFile = async (fileId) => {
  try {
    const response = await axios({
      url: `${BaseUrl}files/${fileId}/download`,
      method: 'GET',
      responseType: 'blob',
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    const file = fileDetailsMap.value[fileId]
    const fileName = file ? file.fileName : `file-${fileId}`
    
    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载文件失败:', error)
    ElMessage.error('下载文件失败')
  }
}

// 打开批量关联文件对话框
const openBatchLinkDialog = () => {
  batchLinkForm.courseId = props.courseId
  batchLinkForm.fileIds = []
  batchLinkForm.resourceType = 'OTHER'
  batchLinkForm.isVisible = true
  selectedFiles.value = []
  batchLinkDialogVisible.value = true
  loadAllFiles()
}

// 提交批量关联文件
const submitBatchLinkFiles = async () => {
  if (batchLinkForm.fileIds.length === 0) {
    ElMessage.warning('请至少选择一个文件')
    return
  }

  try {
    await axios.post(`${BaseUrl}course-files/associate-batch`, {
      courseId: batchLinkForm.courseId,
      fileIds: batchLinkForm.fileIds,
      resourceType: batchLinkForm.resourceType,
      isVisible: batchLinkForm.isVisible
    }, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    ElMessage.success('批量关联文件成功')
    batchLinkDialogVisible.value = false
    loadCourseFiles()
  } catch (error) {
    console.error('批量关联文件失败:', error)
    ElMessage.error('批量关联文件失败: ' + (error.response?.data?.error || error.message))
  }
}

// 选择文件变化处理
const handleFileSelectionChange = (selection) => {
  selectedFiles.value = selection
  batchLinkForm.fileIds = selection.map(file => file.id)
}

// 获取资源类型显示名称
const getResourceTypeDisplay = (type) => {
  const option = resourceTypeOptions.find(opt => opt.value === type)
  return option ? option.label : type
}

// 获取文件类型图标
const getFileTypeIcon = (fileId) => {
  const file = fileDetailsMap.value[fileId]
  if (!file) return Document
  
  if (file.directory || file.isDirectory) {
    return Folder
  }
  
  const mimeType = file.mimeType || ''
  
  if (mimeType.startsWith('image/')) {
    return Picture
  } else if (mimeType.includes('pdf')) {
    return Document
  } else if (mimeType.includes('word')) {
    return Document
  } else if (mimeType.includes('excel') || mimeType.includes('sheet')) {
    return Document
  } else {
    return Document
  }
}

// 打开空状态时关联文件夹
const handleEmptyStateAction = () => {
  if (isTeacher.value || isAdmin.value) {
    // 随机决定是打开单文件关联还是文件夹关联，提供多种选择
    if (Math.random() > 0.5) {
      openLinkFileDialog()
    } else {
      openFolderLinkDialog()
    }
  }
}

// 文件夹相关样式
const folderStyle = {
  background: '#f2f6fc',
  borderRadius: '4px',
  padding: '2px 8px',
  border: '1px dashed #a0cfff'
}

// 获取文件大小格式化
const formatFileSize = (fileId) => {
  const file = fileDetailsMap.value[fileId]
  if (!file) return '未知'
  
  const bytes = file.fileSize || 0
  if (bytes === 0) return '0 B'
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = bytes
  
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  
  return `${size.toFixed(2)} ${units[i]}`
}

// 获取文件名
const getFileName = (fileId) => {
  const file = fileDetailsMap.value[fileId]
  return file ? file.fileName : `文件ID: ${fileId}`
}

// 关闭对话框
const handleClose = () => {
  emit('update:visible', false)
}

// 刷新列表
const refreshList = () => {
  loadCourseFiles()
}

// 应用过滤器
const applyFilter = () => {
  loadCourseFiles()
}

// 重置过滤器
const resetFilter = () => {
  queryParams.resourceType = ''
  queryParams.isVisible = true
  loadCourseFiles()
}

// 刷新用户信息
const refreshUserInfo = () => {
  const freshUserInfo = store.getUserInfo()
  console.log('刷新用户信息:', freshUserInfo)
  // 更新本地变量
  userInfo.value = freshUserInfo
  // 重新计算权限
  isTeacher.value = freshUserInfo.userRole === 'TEACHER'
  isAdmin.value = freshUserInfo.userRole === 'ADMIN'
  ElMessage.info(`用户角色已刷新: ${freshUserInfo.userRole || '未知'}`)
  
  // 如果刷新后拥有权限，重新加载数据
  if (isTeacher.value || isAdmin.value) {
    loadCourseFiles()
  }
}

// 组件挂载时的操作
onMounted(() => {
  if (props.visible && props.courseId) {
    loadCourseFiles()
  }
  
  // 调试用户角色信息
  console.log('当前用户信息:', userInfo.value)
  console.log('是否教师:', isTeacher.value)
  console.log('是否管理员:', isAdmin.value)
  console.log('用户角色:', userInfo.value.userRole)
})
</script>

<template>
  <el-dialog
    :modelValue="props.visible"
    :title="`课程资源管理 - ${props.courseName || '课程'}`"
    width="80%"
    @close="handleClose"
    @update:modelValue="handleClose"
    destroy-on-close
  >
    <div class="container">
      <!-- 过滤器 -->
      <el-card class="search-card">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="资源类型">
            <el-select v-model="queryParams.resourceType" placeholder="选择资源类型" clearable>
              <el-option
                v-for="item in resourceTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="可见性">
            <el-select v-model="queryParams.isVisible" placeholder="选择可见性">
              <el-option label="仅显示可见" :value="true" />
              <el-option label="显示所有" :value="null" />
              <el-option label="仅显示隐藏" :value="false" />
            </el-select>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="applyFilter">筛选</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
      
      <!-- 操作工具栏 -->
      <div class="toolbar">
        <div class="flex gap-sm flex-wrap">
          <el-button type="primary" :icon="Plus" @click="openLinkFileDialog" v-if="isTeacher || isAdmin">
            关联文件
          </el-button>
          <el-button type="success" :icon="Upload" @click="openBatchLinkDialog" v-if="isTeacher || isAdmin">
            批量关联
          </el-button>
          <el-button type="info" :icon="Link" @click="openFolderLinkDialog" v-if="isTeacher || isAdmin">
            关联文件夹
          </el-button>
          <el-text type="info" v-if="!isTeacher && !isAdmin" class="text-warning text-sm ml-sm">
            注意: 您当前角色 "{{ userInfo.value.userRole || '未知' }}" 没有文件管理权限
            <el-button link type="primary" @click="refreshUserInfo">刷新权限</el-button>
          </el-text>
        </div>
        <el-button :icon="RefreshRight" @click="refreshList">刷新</el-button>
      </div>
      
      <!-- 文件列表 -->
      <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="courseFiles"
        border
        style="width: 100%"
      >
        <el-table-column label="文件名" min-width="200">
          <template #default="scope">
              <div class="flex items-center gap-sm">
              <el-icon :size="20">
                <component :is="getFileTypeIcon(scope.row.fileId)" />
              </el-icon>
                <span class="text-overflow">{{ getFileName(scope.row.fileId) }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="resourceType" label="资源类型" width="120">
          <template #default="scope">
            {{ getResourceTypeDisplay(scope.row.resourceType) }}
          </template>
        </el-table-column>
        
        <el-table-column label="大小" width="100">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileId) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        
        <el-table-column prop="isVisible" label="学生可见" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.isVisible ? 'success' : 'info'">
              {{ scope.row.isVisible ? '可见' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="关联时间" width="180">
          <template #default="scope">
            {{ new Date(scope.row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
              <div class="flex justify-center gap-sm flex-wrap">
            <el-button type="primary" link :icon="Download" @click="downloadFile(scope.row.fileId)">
              下载
            </el-button>
            
            <template v-if="isTeacher || isAdmin">
              <el-button 
                type="warning" 
                link 
                :icon="scope.row.isVisible ? Hide : View" 
                @click="updateCourseFile(scope.row)"
              >
                {{ scope.row.isVisible ? '隐藏' : '显示' }}
              </el-button>
              
              <el-button type="danger" link :icon="Delete" @click="unlinkFile(scope.row)">
                取消关联
              </el-button>
            </template>
              </div>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 空状态 -->
      <el-empty v-if="courseFiles.length === 0 && !loading" description="暂无关联文件">
          <div class="flex justify-center gap-sm flex-wrap" v-if="isTeacher || isAdmin">
          <el-button type="primary" @click="openLinkFileDialog">关联文件</el-button>
          <el-button type="success" @click="openBatchLinkDialog">批量关联</el-button>
          <el-button type="info" @click="openFolderLinkDialog">关联文件夹</el-button>
        </div>
      </el-empty>
      </el-card>
    </div>
    
    <!-- 关联文件对话框 -->
    <el-dialog
      v-model="linkFileDialogVisible"
      title="关联文件"
      width="500px"
      append-to-body
    >
      <el-form
        ref="linkFileFormRef"
        :model="linkFileForm"
        :rules="linkFileRules"
        label-width="100px"
      >
        <el-form-item label="选择文件" prop="fileId">
          <el-select
            v-model="linkFileForm.fileId"
            placeholder="请选择文件"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="file in allFiles"
              :key="file.id"
              :label="file.fileName"
              :value="file.id"
            >
              <div class="flex items-center gap-sm">
                <el-icon><Document /></el-icon>
                <span>{{ file.fileName }}</span>
                <span class="text-tertiary text-xs">({{ formatFileSize(file.id) }})</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="资源类型" prop="resourceType">
          <el-select v-model="linkFileForm.resourceType" placeholder="请选择资源类型" style="width: 100%">
            <el-option
              v-for="item in resourceTypeOptions.filter(item => item.value !== '')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="linkFileForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入资源描述"
          />
        </el-form-item>
        
        <el-form-item label="学生可见">
          <el-switch v-model="linkFileForm.isVisible" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="linkFileDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitLinkFile(linkFileFormRef)">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 批量关联文件对话框 -->
    <el-dialog
      v-model="batchLinkDialogVisible"
      title="批量关联文件"
      width="700px"
      append-to-body
    >
      <el-form :model="batchLinkForm" label-width="100px">
        <el-form-item label="资源类型">
          <el-select v-model="batchLinkForm.resourceType" placeholder="请选择资源类型" style="width: 100%">
            <el-option
              v-for="item in resourceTypeOptions.filter(item => item.value !== '')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="学生可见">
          <el-switch v-model="batchLinkForm.isVisible" />
        </el-form-item>
        
        <el-form-item label="选择文件">
          <div class="border radius-sm overflow-auto" style="max-height: 300px;">
            <el-table
              :data="allFiles"
              border
              style="width: 100%"
              @selection-change="handleFileSelectionChange"
              max-height="300px"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column prop="fileName" label="文件名" min-width="200" />
              <el-table-column label="大小" width="120">
                <template #default="scope">
                  {{ formatFileSize(scope.row.id) }}
                </template>
              </el-table-column>
              <el-table-column prop="mimeType" label="文件类型" width="150" />
            </el-table>
          </div>
        </el-form-item>
        
        <el-form-item v-if="selectedFiles.length > 0">
          <p>已选择 {{ selectedFiles.length }} 个文件</p>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="batchLinkDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitBatchLinkFiles">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 关联文件夹对话框 -->
    <el-dialog
      v-model="folderLinkDialogVisible"
      title="关联文件夹"
      width="500px"
      append-to-body
    >
      <el-form :model="folderLinkForm" label-width="100px">
        <el-form-item label="选择文件夹" prop="folderId">
          <el-select
            v-model="folderLinkForm.folderId"
            placeholder="请选择文件夹"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="folder in folders"
              :key="folder.id"
              :label="folder.fileName"
              :value="folder.id"
            >
              <div class="flex items-center gap-sm">
                <el-icon><Folder /></el-icon>
                <span>{{ folder.fileName }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="资源类型" prop="resourceType">
          <el-select v-model="folderLinkForm.resourceType" placeholder="请选择资源类型" style="width: 100%">
            <el-option
              v-for="item in resourceTypeOptions.filter(item => item.value !== '')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="学生可见">
          <el-switch v-model="folderLinkForm.isVisible" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="folderLinkDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitFolderLink">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<style scoped>
/* 使用全局样式，仅保留特殊样式 */
.text-overflow {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-xs {
  font-size: var(--text-xs);
}

.text-warning {
  color: var(--warning-color);
}
</style> 