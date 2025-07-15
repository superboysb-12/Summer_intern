<script setup>
import { ref, computed, onMounted } from 'vue'
import { useCounterStore } from '../../stores/counter'
import { Calendar, Document, Upload, Warning, SuccessFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const store = useCounterStore()
const userInfo = store.getUserInfo()

// 作业数据
const assignmentList = ref([
  {
    id: 1,
    title: 'JavaScript基础练习',
    courseName: '前端开发基础',
    deadline: '2023-07-15',
    status: 'completed',
    score: 90
  },
  {
    id: 2,
    title: 'Vue组件开发作业',
    courseName: '前端框架开发',
    deadline: '2023-07-20',
    status: 'pending',
    score: null
  },
  {
    id: 3,
    title: 'React项目实战',
    courseName: '前端高级开发',
    deadline: '2023-07-30',
    status: 'overdue',
    score: null
  },
  {
    id: 4,
    title: 'Python数据分析基础',
    courseName: 'Python编程',
    deadline: '2023-08-05',
    status: 'pending',
    score: null
  }
])

// 活跃的标签
const activeTab = ref('all')

// 过滤后的作业列表
const filteredAssignments = computed(() => {
  if (activeTab.value === 'all') {
    return assignmentList.value
  } else {
    return assignmentList.value.filter(item => item.status === activeTab.value)
  }
})

// 提交作业
const submitAssignment = (assignment) => {
  ElMessage.success(`提交作业: ${assignment.title}`)
}

// 查看作业
const viewAssignment = (assignment) => {
  ElMessage.info(`查看作业: ${assignment.title}`)
}

// 获取状态对应的图标
const getStatusIcon = (status) => {
  switch(status) {
    case 'completed': return SuccessFilled
    case 'pending': return Calendar
    case 'overdue': return Warning
    default: return Document
  }
}

// 获取状态对应的文本
const getStatusText = (status) => {
  switch(status) {
    case 'completed': return '已完成'
    case 'pending': return '待提交'
    case 'overdue': return '已逾期'
    default: return '未知状态'
  }
}

// 获取状态对应的类型
const getStatusType = (status) => {
  switch(status) {
    case 'completed': return 'success'
    case 'pending': return 'warning'
    case 'overdue': return 'danger'
    default: return 'info'
  }
}
</script>

<template>
  <div class="student-assignments-container">
    <div class="page-header">
      <h1>作业管理</h1>
      <p>查看和提交您的课程作业</p>
    </div>

    <!-- 筛选标签 -->
    <el-tabs v-model="activeTab" class="filter-tabs">
      <el-tab-pane label="全部作业" name="all"></el-tab-pane>
      <el-tab-pane label="待提交" name="pending"></el-tab-pane>
      <el-tab-pane label="已完成" name="completed"></el-tab-pane>
      <el-tab-pane label="已逾期" name="overdue"></el-tab-pane>
    </el-tabs>

    <!-- 作业列表 -->
    <el-table :data="filteredAssignments" style="width: 100%" v-loading="false">
      <el-table-column label="作业名称" min-width="250">
        <template #default="scope">
          <div class="assignment-title">
            <el-icon :size="18" class="title-icon">
              <component :is="getStatusIcon(scope.row.status)"></component>
            </el-icon>
            <span>{{ scope.row.title }}</span>
          </div>
          <div class="assignment-course">{{ scope.row.courseName }}</div>
        </template>
      </el-table-column>
      
      <el-table-column label="状态" width="120" align="center">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="截止日期" width="120" align="center">
        <template #default="scope">
          <span>{{ scope.row.deadline }}</span>
        </template>
      </el-table-column>
      
      <el-table-column label="成绩" width="100" align="center">
        <template #default="scope">
          <span v-if="scope.row.score !== null">{{ scope.row.score }}</span>
          <span v-else class="no-score">暂无</span>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="180" align="center">
        <template #default="scope">
          <el-button 
            size="small" 
            @click="viewAssignment(scope.row)"
            type="info" 
            text
          >
            查看详情
          </el-button>
          
          <el-button 
            size="small"
            type="primary"
            @click="submitAssignment(scope.row)"
            :disabled="scope.row.status === 'completed'"
            text
          >
            {{ scope.row.status === 'completed' ? '已提交' : '提交作业' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空数据状态 -->
    <el-empty v-if="filteredAssignments.length === 0" description="暂无作业" />
  </div>
</template>

<style scoped>
.student-assignments-container {
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

.filter-tabs {
  margin-bottom: 20px;
}

.assignment-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.assignment-course {
  color: #666;
  font-size: 12px;
  margin-top: 4px;
  margin-left: 26px;
}

.title-icon {
  color: var(--el-color-primary);
}

.no-score {
  color: #999;
}

:deep(.el-tabs__nav) {
  margin-bottom: 10px;
}
</style> 