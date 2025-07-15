<script setup>
import { ref, computed, onMounted } from 'vue'
import { useCounterStore } from '../../stores/counter'
import { Document, Folder, Download, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const store = useCounterStore()
const userInfo = store.getUserInfo()
const loading = ref(false)

// 资源分类
const categories = ref([
  { id: 'all', name: '全部' },
  { id: 'textbook', name: '课程教材' },
  { id: 'practice', name: '实践资料' },
  { id: 'video', name: '视频资源' },
  { id: 'code', name: '代码示例' }
])

// 当前选择的分类
const activeCategory = ref('all')

// 学习资源数据
const resources = ref([
  {
    id: 1,
    title: '前端开发基础教材',
    category: 'textbook',
    courseName: '前端开发基础',
    fileType: 'pdf',
    fileSize: '5.2MB',
    uploadTime: '2023-06-15'
  },
  {
    id: 2,
    title: 'JavaScript核心概念',
    category: 'textbook',
    courseName: '前端开发基础',
    fileType: 'pdf',
    fileSize: '3.8MB',
    uploadTime: '2023-06-16'
  },
  {
    id: 3,
    title: 'HTML与CSS实战项目',
    category: 'practice',
    courseName: '前端开发基础',
    fileType: 'zip',
    fileSize: '12.4MB',
    uploadTime: '2023-06-20'
  },
  {
    id: 4,
    title: 'Vue项目实践指南',
    category: 'practice',
    courseName: '前端框架开发',
    fileType: 'pdf',
    fileSize: '6.7MB',
    uploadTime: '2023-06-25'
  },
  {
    id: 5,
    title: 'React框架入门视频',
    category: 'video',
    courseName: '前端框架开发',
    fileType: 'mp4',
    fileSize: '256MB',
    uploadTime: '2023-06-28'
  },
  {
    id: 6,
    title: 'Node.js后端开发示例代码',
    category: 'code',
    courseName: '前端高级开发',
    fileType: 'zip',
    fileSize: '8.3MB',
    uploadTime: '2023-07-02'
  },
  {
    id: 7,
    title: 'Python数据分析基础教程',
    category: 'textbook',
    courseName: 'Python编程',
    fileType: 'pdf',
    fileSize: '7.5MB',
    uploadTime: '2023-07-05'
  }
])

// 筛选后的资源列表
const filteredResources = computed(() => {
  if (activeCategory.value === 'all') {
    return resources.value
  } else {
    return resources.value.filter(item => item.category === activeCategory.value)
  }
})

// 下载资源
const downloadResource = (resource) => {
  ElMessage.success(`开始下载: ${resource.title}`)
}

// 查看资源
const viewResource = (resource) => {
  ElMessage.info(`查看资源: ${resource.title}`)
}

// 获取文件类型图标
const getFileTypeIcon = (fileType) => {
  switch(fileType.toLowerCase()) {
    case 'pdf': return Document
    case 'zip': return Folder
    case 'mp4': return View
    default: return Document
  }
}

// 获取文件类型样式
const getFileTypeClass = (fileType) => {
  switch(fileType.toLowerCase()) {
    case 'pdf': return 'pdf-file'
    case 'zip': return 'zip-file'
    case 'mp4': return 'video-file'
    default: return 'other-file'
  }
}
</script>

<template>
  <div class="student-resources-container">
    <div class="page-header">
      <h1>学习资源</h1>
      <p>浏览课程学习资料和资源</p>
    </div>

    <!-- 资源分类标签 -->
    <el-tabs v-model="activeCategory" class="resource-tabs">
      <el-tab-pane 
        v-for="category in categories" 
        :key="category.id"
        :label="category.name" 
        :name="category.id"
      ></el-tab-pane>
    </el-tabs>

    <!-- 资源搜索 -->
    <el-card shadow="hover" class="filter-card">
      <div class="filter-row">
        <el-input
          placeholder="搜索资源名称"
          prefix-icon="el-icon-search"
          style="width: 300px"
        />
        <el-button type="primary">搜索</el-button>
      </div>
    </el-card>

    <!-- 资源列表 -->
    <div class="resources-list" v-loading="loading">
      <el-row :gutter="20">
        <el-col v-for="resource in filteredResources" :key="resource.id" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card shadow="hover" class="resource-card">
            <div :class="['file-icon', getFileTypeClass(resource.fileType)]">
              <el-icon :size="32">
                <component :is="getFileTypeIcon(resource.fileType)"></component>
              </el-icon>
              <div class="file-type">{{ resource.fileType.toUpperCase() }}</div>
            </div>
            
            <div class="resource-content">
              <h3 class="resource-title">{{ resource.title }}</h3>
              <div class="resource-course">{{ resource.courseName }}</div>
              <div class="resource-meta">
                <span class="meta-item">{{ resource.fileSize }}</span>
                <span class="meta-item">{{ resource.uploadTime }}</span>
              </div>
            </div>
            
            <div class="resource-actions">
              <el-button 
                type="primary" 
                :icon="Download" 
                circle
                @click="downloadResource(resource)"
              ></el-button>
              
              <el-button 
                type="info" 
                :icon="View" 
                circle
                @click="viewResource(resource)"
              ></el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 没有资源时显示空状态 -->
      <el-empty v-if="filteredResources.length === 0" description="暂无学习资源" />
    </div>
  </div>
</template>

<style scoped>
.student-resources-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  margin-bottom: 8px;
  font-weight: 500;
}

.page-header p {
  color: #666;
  margin: 0;
}

.resource-tabs {
  margin-bottom: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.resources-list {
  margin-top: 20px;
}

.resource-card {
  height: 100%;
  margin-bottom: 20px;
  transition: transform 0.3s;
  display: flex;
  flex-direction: column;
}

.resource-card:hover {
  transform: translateY(-5px);
}

.file-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 15px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: white;
}

.pdf-file {
  background-image: linear-gradient(135deg, #FF6B6B 10%, #cc0000 100%);
}

.zip-file {
  background-image: linear-gradient(135deg, #4facfe 10%, #00a2ff 100%);
}

.video-file {
  background-image: linear-gradient(135deg, #7F7FD5 10%, #5e5eaa 100%);
}

.other-file {
  background-image: linear-gradient(135deg, #86A8E7 10%, #627fb8 100%);
}

.file-type {
  font-size: 12px;
  margin-top: 5px;
  font-weight: bold;
}

.resource-content {
  flex-grow: 1;
  text-align: center;
}

.resource-title {
  font-size: 16px;
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.resource-course {
  color: #666;
  font-size: 13px;
  margin-bottom: 10px;
}

.resource-meta {
  display: flex;
  justify-content: space-around;
  color: #999;
  font-size: 12px;
  margin-bottom: 15px;
}

.resource-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: auto;
}

:deep(.el-tabs__nav) {
  margin-bottom: 10px;
}
</style> 