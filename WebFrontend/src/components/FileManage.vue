<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, RefreshRight, Download, Upload, FolderAdd, Document, Folder, Picture } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'
import { useRouter } from 'vue-router'

const router = useRouter()
const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 文件列表数据
const fileList = ref([])
const loading = ref(false)
const total = ref(0)

// 当前路径
const currentPath = ref('/')
const pathHistory = ref(['/'])

// 表单引用
const fileFormRef = ref(null)
const folderFormRef = ref(null)

// 查询参数
const queryParams = reactive({
  path: '/',
  includeFiles: true,
  keyword: ''
})

// 文件上传表单
const uploadForm = reactive({
  files: [],
  path: '/',
  overwrite: false
})

// 文件夹表单
const folderForm = reactive({
  folderName: '',
  parentPath: '/'
})

// 对话框控制
const uploadDialogVisible = ref(false)
const folderDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const previewUrl = ref('')
const previewFileName = ref('')

// 预览内容类型和预览数据
const previewContentType = ref('')
const previewData = ref(null)
const isPreviewLoading = ref(false)
const previewBlob = ref(null)
const currentPreviewFile = ref(null)

// 文件夹表单校验规则
const folderRules = {
  folderName: [
    { required: true, message: '请输入文件夹名称', trigger: 'blur' },
    { pattern: /^[^\\/:*?"<>|]+$/, message: '文件夹名称不能包含特殊字符', trigger: 'blur' }
  ]
}

// 获取文件列表
const getFileList = async () => {
  loading.value = true
  try {
    // 构建请求参数
    let url = BaseUrl + 'files'
    let params = {}
    
    params.path = queryParams.path
    params.includeFiles = queryParams.includeFiles
    
    // 如果有搜索关键词，切换到搜索API
    if (queryParams.keyword) {
      url = BaseUrl + 'files/search'
      params.q = queryParams.keyword
    }
    
    const response = await axios.get(url, {
      params: params,
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data.code === 200 && response.data.data) {
      fileList.value = response.data.data
      total.value = response.data.data.length
    } else if (Array.isArray(response.data)) {
      fileList.value = response.data
      total.value = response.data.length
    }
  } catch (error) {
    console.error('获取文件列表失败:', error)
    ElMessage.error('获取文件列表失败')
  } finally {
    loading.value = false
  }
}

// 刷新文件列表
const refreshFileList = () => {
  getFileList()
}

// 搜索文件
const handleSearch = () => {
  queryParams.path = currentPath.value
  getFileList()
}

// 重置搜索
const resetSearch = () => {
  queryParams.keyword = ''
  getFileList()
}

// 打开文件夹
const openFolder = (folder) => {
  // 确保folder对象包含filePath属性，否则使用提供的path
  const targetPath = folder.filePath || folder.path || '/'
  currentPath.value = targetPath
  queryParams.path = targetPath
  
  // 如果是通过点击面包屑导航到根目录，重置路径历史
  if (targetPath === '/') {
    pathHistory.value = ['/']
  } else {
    // 避免重复添加相同路径
    if (pathHistory.value[pathHistory.value.length - 1] !== targetPath) {
      pathHistory.value.push(targetPath)
    }
  }
  
  getFileList()
}

// 返回上级目录
const goBack = () => {
  if (pathHistory.value.length > 1) {
    pathHistory.value.pop()
    currentPath.value = pathHistory.value[pathHistory.value.length - 1]
    queryParams.path = currentPath.value
    getFileList()
  }
}

// 上传文件对话框
const handleUpload = () => {
  // 每次打开对话框时重置文件列表
  uploadForm.files = []
  uploadForm.path = currentPath.value
  uploadForm.overwrite = false
  uploadDialogVisible.value = true
}

// 创建文件夹对话框
const handleCreateFolder = () => {
  folderForm.parentPath = currentPath.value
  folderForm.folderName = ''
  folderDialogVisible.value = true
}

// 提交创建文件夹
const submitFolderForm = async (formEl) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        await axios.post(BaseUrl + 'files/folders', folderForm, {
          headers: {
            'Authorization': `Bearer ${getToken()}`
          }
        })
        ElMessage.success('文件夹创建成功')
        folderDialogVisible.value = false
        getFileList()
      } catch (error) {
        console.error('创建文件夹失败:', error)
        ElMessage.error('创建文件夹失败')
      }
    }
  })
}

// 提交文件上传
const submitUpload = async (formEl) => {
  if (!formEl) return
  
  if (uploadForm.files.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }
  
  try {
    const formData = new FormData()
    
    // 添加文件
    for (const file of uploadForm.files) {
      formData.append('files', file)
    }
    
    // 确保使用当前路径
    const uploadPath = currentPath.value
    formData.append('path', uploadPath)
    formData.append('overwrite', uploadForm.overwrite)
    
    await axios.post(BaseUrl + 'files/upload', formData, {
      headers: {
        'Authorization': `Bearer ${getToken()}`,
        'Content-Type': 'multipart/form-data'
      }
    })
    
    ElMessage.success('文件上传成功')
    // 重置文件列表
    uploadForm.files = []
    uploadDialogVisible.value = false
    
    // 强制刷新当前目录的文件列表
    queryParams.path = uploadPath
    getFileList()
  } catch (error) {
    console.error('文件上传失败:', error)
    ElMessage.error('文件上传失败')
  }
}

// 文件预览
const handlePreview = async (file) => {
  try {
    isPreviewLoading.value = true
    previewFileName.value = file.fileName
    previewDialogVisible.value = true
    currentPreviewFile.value = file
    
    // 判断文件类型决定如何预览
    const fileType = file.mimeType || ''
    
    // 对所有文件类型使用相同的安全获取方式
    const response = await axios({
      url: `${BaseUrl}files/${file.id}/preview`,
      method: 'GET',
      responseType: 'blob',
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    const blob = new Blob([response.data], { type: fileType })
    previewBlob.value = blob
    
    if (fileType.startsWith('image/')) {
      // 图片预览
      previewUrl.value = URL.createObjectURL(blob)
      previewContentType.value = 'image'
    } else if (fileType === 'application/pdf') {
      // PDF预览 - 使用blob URL
      previewUrl.value = URL.createObjectURL(blob)
      previewContentType.value = 'pdf'
    } else if (fileType.startsWith('text/') || 
               fileType === 'application/json' ||
               fileType === 'application/xml') {
      // 文本预览 - 将blob转换为文本
      const text = await blob.text()
      previewData.value = text
      previewContentType.value = 'text'
    } else {
      // 其他类型 - 尝试使用blob URL
      previewUrl.value = URL.createObjectURL(blob)
      previewContentType.value = 'blob'
    }
  } catch (error) {
    console.error('预览文件失败:', error)
    ElMessage.error('预览文件失败')
  } finally {
    isPreviewLoading.value = false
  }
}

// 清理预览资源
const cleanupPreview = () => {
  if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
  previewData.value = null
  previewBlob.value = null
}

// 文件下载
const handleDownload = async (file) => {
  try {
    // 使用axios发送请求，自动带上token
    const response = await axios({
      url: `${BaseUrl}files/${file.id}/download`,
      method: 'GET',
      responseType: 'blob', // 重要：指定响应类型为blob
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', file.fileName) // 指定下载文件名
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

// 重命名文件
const handleRename = (file) => {
  ElMessageBox.prompt('请输入新名称', '重命名', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: file.fileName,
    inputPattern: /^[^\\/:*?"<>|]+$/,
    inputErrorMessage: '名称不能包含特殊字符'
  }).then(({ value }) => {
    axios.put(`${BaseUrl}files/${file.id}/rename`, {
      newName: value
    }, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    }).then(() => {
      ElMessage.success('重命名成功')
      getFileList()
    }).catch(error => {
      console.error('重命名失败:', error)
      ElMessage.error('重命名失败')
    })
  }).catch(() => {})
}

// 删除文件
const handleDelete = (file) => {
  const isFolder = file.directory
  const confirmMsg = isFolder ? '确认要删除该文件夹及其内容吗？' : '确认要删除该文件吗？'
  
  ElMessageBox.confirm(confirmMsg, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    let url = isFolder 
      ? `${BaseUrl}files/folders/${file.id}?force=true`
      : `${BaseUrl}files/${file.id}`
    
    axios.delete(url, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    }).then(() => {
      ElMessage.success('删除成功')
      getFileList()
    }).catch(error => {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    })
  }).catch(() => {})
}

// 处理文件选择变化
const handleFileChange = (e) => {
  // 添加到现有文件列表，而不是替换
  uploadForm.files = [...uploadForm.files, ...Array.from(e.target.files)]
}

// 触发文件选择
const triggerFileInput = () => {
  fileInput.value.click()
}

// 处理文件拖放
const handleFileDrop = (e) => {
  const droppedFiles = Array.from(e.dataTransfer.files)
  uploadForm.files = [...uploadForm.files, ...droppedFiles]
}

// 清空已选文件
const clearSelectedFiles = () => {
  uploadForm.files = []
}

// 移除单个文件
const removeFile = (index) => {
  uploadForm.files.splice(index, 1)
}

// 文件输入引用
const fileInput = ref(null)

// 批量删除
const handleBatchDelete = () => {
  // 实现批量删除逻辑
}

// 初始化
onMounted(() => {
  getFileList()
})
</script>

<template>
  <div class="file-manage-container">
    <!-- 头部区域 -->
    <div class="manage-header">
      <div class="manage-title">
        <h2>文件管理</h2>
      </div>
      <div class="manage-actions">
        <el-input
          v-model="queryParams.keyword"
          placeholder="搜索文件或文件夹"
          class="search-input"
          clearable
          @clear="resetSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
        
        <el-button type="primary" @click="handleUpload" :icon="Upload">上传文件</el-button>
        <el-button type="success" @click="handleCreateFolder" :icon="FolderAdd">新建文件夹</el-button>
        <el-button type="info" @click="refreshFileList" :icon="RefreshRight">刷新</el-button>
      </div>
    </div>
    
    <!-- 路径导航 -->
    <div class="path-navigation">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>
          <a @click.prevent="openFolder({path: '/'})">根目录</a>
        </el-breadcrumb-item>
        <template v-if="currentPath !== '/'">
          <el-breadcrumb-item v-for="(path, index) in currentPath.split('/').filter(Boolean)" :key="index">
            {{ path }}
          </el-breadcrumb-item>
        </template>
      </el-breadcrumb>
      <el-button 
        type="text" 
        @click="goBack" 
        :disabled="pathHistory.length <= 1"
      >
        返回上级
      </el-button>
    </div>

    <!-- 文件列表 -->
    <el-table
      v-loading="loading"
      :data="fileList"
      border
      style="width: 100%"
    >
      <el-table-column label="名称" min-width="180">
        <template #default="scope">
          <div class="file-item">
            <el-icon v-if="scope.row.directory"><Folder /></el-icon>
            <el-icon v-else><Document /></el-icon>
            <span 
              class="file-name" 
              @click="scope.row.directory ? openFolder(scope.row) : handlePreview(scope.row)"
            >
              {{ scope.row.fileName }}
            </span>
          </div>
        </template>
      </el-table-column>
      
      <el-table-column prop="fileSize" label="大小" width="120">
        <template #default="scope">
          <span v-if="!scope.row.directory">
            {{ formatFileSize(scope.row.fileSize || 0) }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      
      <el-table-column prop="updatedAt" label="修改时间" width="180">
        <template #default="scope">
          {{ new Date(scope.row.updatedAt).toLocaleString() }}
        </template>
      </el-table-column>
      
      <el-table-column prop="mimeType" label="类型" width="120">
        <template #default="scope">
          <span v-if="scope.row.directory">文件夹</span>
          <span v-else>{{ scope.row.mimeType || '文件' }}</span>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" min-width="140" fixed="right">
        <template #default="scope">
          <div class="action-buttons">
            <div class="action-row" v-if="!scope.row.directory">
              <el-button 
                type="primary" 
                link 
                @click="handlePreview(scope.row)" 
                :icon="Search"
              >
                预览
              </el-button>
              
              <el-button 
                type="success" 
                link 
                @click="handleDownload(scope.row)" 
                :icon="Download"
              >
                下载
              </el-button>
            </div>
            
            <div class="action-row">
              <el-button 
                type="primary" 
                link 
                @click="handleRename(scope.row)" 
                :icon="Edit"
              >
                重命名
              </el-button>
              
              <el-button 
                type="danger" 
                link 
                @click="handleDelete(scope.row)" 
                :icon="Delete"
              >
                删除
              </el-button>
            </div>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 上传文件对话框 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传文件"
      width="600px"
      destroy-on-close
      class="upload-dialog"
    >
      <el-form ref="fileFormRef" :model="uploadForm" label-width="100px">
        <el-form-item label="上传位置">
          <el-tag type="info" effect="plain">{{ uploadForm.path }}</el-tag>
        </el-form-item>
        
        <el-form-item label="文件选择">
          <div class="upload-drop-zone" @drop.prevent="handleFileDrop" @dragover.prevent>
            <div class="upload-icon-container">
              <el-icon class="upload-icon"><Upload /></el-icon>
              <p>拖放文件到此处或 <el-button type="primary" link @click="triggerFileInput">点击选择文件</el-button></p>
              <p class="upload-tip">支持多个文件上传</p>
            </div>
            <input 
              type="file" 
              ref="fileInput"
              @change="handleFileChange" 
              multiple
              class="hidden-file-input"
            />
          </div>
          
          <div v-if="uploadForm.files.length > 0" class="selected-files-panel">
            <div class="selected-files-header">
              <h4>已选择 {{ uploadForm.files.length }} 个文件</h4>
              <el-button type="danger" link @click="clearSelectedFiles">清空</el-button>
            </div>
            <el-scrollbar height="200px">
              <div class="file-list">
                <div v-for="(file, index) in uploadForm.files" :key="index" class="file-item-preview">
                  <div class="file-preview-icon">
                    <el-icon v-if="file.type.startsWith('image/')" class="preview-type-icon"><Picture /></el-icon>
                    <el-icon v-else-if="file.type.includes('pdf')" class="preview-type-icon"><Document /></el-icon>
                    <el-icon v-else-if="file.type.includes('word')" class="preview-type-icon"><Document /></el-icon>
                    <el-icon v-else-if="file.type.includes('excel') || file.type.includes('sheet')" class="preview-type-icon"><Document /></el-icon>
                    <el-icon v-else class="preview-type-icon"><Document /></el-icon>
                  </div>
                  <div class="file-info">
                    <div class="file-name-preview">{{ file.name }}</div>
                    <div class="file-size-preview">{{ formatFileSize(file.size) }}</div>
                  </div>
                  <el-button 
                    type="danger" 
                    circle 
                    size="small" 
                    @click="removeFile(index)"
                    class="remove-file-btn"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </el-scrollbar>
          </div>
        </el-form-item>
        
        <el-form-item label="覆盖同名文件">
          <el-switch 
            v-model="uploadForm.overwrite" 
            active-color="#13ce66"
            inactive-color="#ff4949"
            inline-prompt
            active-text="是"
            inactive-text="否"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="uploadDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitUpload(fileFormRef)" :disabled="uploadForm.files.length === 0">
            <el-icon><Upload /></el-icon> 上传
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 创建文件夹对话框 -->
    <el-dialog
      v-model="folderDialogVisible"
      title="创建文件夹"
      width="500px"
    >
      <el-form ref="folderFormRef" :model="folderForm" :rules="folderRules" label-width="100px">
        <el-form-item label="父目录">
          <span>{{ folderForm.parentPath }}</span>
        </el-form-item>
        
        <el-form-item label="文件夹名称" prop="folderName">
          <el-input v-model="folderForm.folderName" placeholder="请输入文件夹名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="folderDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitFolderForm(folderFormRef)">创建</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 文件预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      :title="previewFileName"
      width="80%"
      top="5vh"
      destroy-on-close
      @closed="cleanupPreview"
    >
      <div class="preview-container" v-loading="isPreviewLoading">
        <!-- 图片预览 -->
        <img 
          v-if="previewContentType === 'image'" 
          :src="previewUrl" 
          class="preview-image"
          alt="预览图片"
        />
        
        <!-- PDF预览 -->
        <iframe 
          v-else-if="previewContentType === 'pdf'" 
          :src="previewUrl" 
          class="preview-iframe"
        ></iframe>
        
        <!-- 文本预览 -->
        <pre v-else-if="previewContentType === 'text'" class="preview-text">{{ previewData }}</pre>
        
        <!-- Blob URL预览 -->
        <div v-else-if="previewContentType === 'blob'" class="preview-fallback">
          <p>尝试预览 (如果浏览器支持此格式):</p>
          <iframe :src="previewUrl" class="preview-iframe"></iframe>
          <el-button type="primary" @click="handleDownload(currentPreviewFile)" style="margin-top: 10px">
            下载文件
          </el-button>
        </div>
        
        <!-- 不支持预览 -->
        <div v-else class="preview-unsupported">
          <p>无法预览此类型的文件，请下载后查看。</p>
          <el-button type="primary" @click="handleDownload(currentPreviewFile)">
            下载文件
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
// 格式化文件大小的工具函数
function formatFileSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  while (bytes >= 1024 && i < units.length - 1) {
    bytes /= 1024
    i++
  }
  
  return `${bytes.toFixed(2)} ${units[i]}`
}
</script>

<style scoped>
.file-manage-container {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.manage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.manage-title h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.manage-actions {
  display: flex;
  gap: 10px;
}

.search-input {
  width: 300px;
}

.path-navigation {
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  cursor: pointer;
}

.file-name:hover {
  color: #409eff;
  text-decoration: underline;
}

.file-input {
  width: 100%;
}

.selected-files {
  margin-top: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  max-height: 150px;
  overflow-y: auto;
}

.selected-files p {
  margin-top: 0;
  font-weight: bold;
}

.selected-files ul {
  margin: 0;
  padding-left: 20px;
}

.preview-container {
  width: 100%;
  height: 70vh;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: auto;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-text {
  width: 100%;
  height: 100%;
  overflow: auto;
  white-space: pre-wrap;
  font-family: monospace;
  padding: 10px;
  margin: 0;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.preview-fallback {
  text-align: center;
  padding: 20px;
}

.preview-unsupported {
  text-align: center;
  padding: 20px;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 120px;
}

.action-row {
  display: flex;
  justify-content: flex-start;
  gap: 8px;
  width: 100%;
}

.action-row .el-button {
  margin-left: 0;
  padding: 2px 4px;
  font-size: 12px;
  width: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.action-row .el-button + .el-button {
  margin-left: 0;
}

.action-row .el-button [class*="el-icon"] + span {
  margin-left: 2px;
}

.upload-dialog :deep(.el-dialog__body) {
  padding: 20px 30px;
}

.upload-drop-zone {
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  background-color: #f9fafc;
  padding: 30px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.upload-drop-zone:hover {
  border-color: #409eff;
  background-color: rgba(64, 158, 255, 0.05);
}

.upload-icon-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-icon {
  font-size: 48px;
  color: #409eff;
  margin-bottom: 15px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.hidden-file-input {
  display: none;
}

.selected-files-panel {
  margin-top: 15px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.selected-files-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.selected-files-header h4 {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.file-list {
  padding: 10px;
}

.file-item-preview {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  border-radius: 4px;
  transition: background-color 0.2s;
  position: relative;
}

.file-item-preview:hover {
  background-color: #f5f7fa;
}

.file-preview-icon {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 40px;
  height: 40px;
  border-radius: 4px;
  background-color: #f0f2f5;
  margin-right: 12px;
}

.preview-type-icon {
  font-size: 24px;
  color: #909399;
}

.file-info {
  flex: 1;
  overflow: hidden;
}

.file-name-preview {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size-preview {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.remove-file-btn {
  opacity: 0;
  transition: opacity 0.2s;
}

.file-item-preview:hover .remove-file-btn {
  opacity: 1;
}
</style> 