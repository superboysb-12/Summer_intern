<template>
  <div class="container">
    <!-- 编辑器顶部工具栏 -->
    <div class="card-header mb-lg">
      <el-row :gutter="20" align="middle">
        <el-col :span="12">
          <div class="flex items-center">
            <h2 class="mr-md">教案编辑器</h2>
            <div>
              <el-tag v-if="isEditing" type="warning">正在编辑</el-tag>
              <el-tag v-else-if="isSaved" type="success">已保存</el-tag>
            </div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="flex justify-end gap-sm">
            <el-button type="primary" @click="saveTeachingPlan" :loading="saving" :disabled="!hasChanges">
              保存
            </el-button>
            <el-button type="success" @click="confirmFinish" :loading="finishing">
              完成编辑
            </el-button>
            <el-button type="info" @click="backToList">
              返回列表
            </el-button>
          </div>
        </el-col>
      </el-row>
    </div>
    
    <!-- 编辑时长显示 -->
    <div class="card bg-secondary radius-md mb-lg">
      <div class="card-body">
      <el-row>
        <el-col :span="8">
            <div class="text-center">
              <div class="text-tertiary mb-sm">开始时间</div>
              <div class="text-md font-bold">{{ formatDateTime(editStartTime) }}</div>
          </div>
        </el-col>
        <el-col :span="8">
            <div class="text-center">
              <div class="text-tertiary mb-sm">已编辑时长</div>
              <div class="text-md font-bold">{{ formatDuration(editDuration) }}</div>
          </div>
        </el-col>
        <el-col :span="8">
            <div class="text-center" v-if="showEfficiency">
              <div class="text-tertiary mb-sm">效率指数</div>
              <div class="text-md font-bold">
              <el-progress 
                :percentage="efficiencyIndex" 
                :color="getEfficiencyColor"
                :format="percentFormat"
                :stroke-width="18"
              />
            </div>
          </div>
        </el-col>
      </el-row>
      </div>
    </div>
    
    <!-- 编辑器主体 -->
    <div class="card-body">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="编辑内容" name="edit">
          <div class="min-h-500">
            <el-input
              v-model="content"
              type="textarea"
              :rows="20"
              placeholder="在此编辑教案内容..."
              @input="onContentChange"
            />
          </div>
        </el-tab-pane>
        <el-tab-pane label="预览" name="preview">
          <div class="min-h-500 border radius-sm p-md overflow-auto">
            <div v-html="formattedContent" class="line-height-md"></div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    
    <!-- 效率统计与优化建议对话框 -->
    <el-dialog
      v-model="efficiencyDialogVisible"
      title="教学效率统计"
      width="700px"
      append-to-body
    >
      <div class="mb-xl">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-statistic title="效率指数" :value="efficiencyIndex" :precision="2">
              <template #suffix>
                <span>/100</span>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="12">
            <el-statistic title="编辑时长" :value="formatDuration(editDuration)" />
          </el-col>
        </el-row>
        
        <div class="mt-lg" v-if="optimizationSuggestions">
          <h3 class="mb-md">优化建议</h3>
          <div class="card-body bg-secondary radius-md">
            <div v-html="formattedSuggestions" class="line-height-md"></div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="efficiencyDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="backToList">返回列表</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { useCounterStore } from '../stores/counter'

const router = useRouter()
const route = useRoute()
const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')

// 编辑器状态
const content = ref('')
const originalContent = ref('')
const activeTab = ref('edit')
const isEditing = ref(false)
const isSaved = ref(false)
const hasChanges = ref(false)
const editDuration = ref(0)
const editStartTime = ref(null)
const efficiencyIndex = ref(0)
const optimizationSuggestions = ref('')
const showEfficiency = ref(false)
const loading = ref(false)
const saving = ref(false)
const finishing = ref(false)

// 对话框控制
const efficiencyDialogVisible = ref(false)

// 计时器
let durationTimer = null

// 获取ID参数
const planId = route.params.id

// 格式化预览内容
const formattedContent = computed(() => {
  if (!content.value) return ''
  return content.value
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
})

// 格式化优化建议
const formattedSuggestions = computed(() => {
  if (!optimizationSuggestions.value) return ''
  return optimizationSuggestions.value
    .replace(/\n/g, '<br>')
    .replace(/- (.*)/g, '<li>$1</li>')
})

// 获取效率指数对应的颜色
const getEfficiencyColor = computed(() => {
  if (efficiencyIndex.value >= 80) return '#67C23A'
  if (efficiencyIndex.value >= 60) return '#E6A23C'
  return '#F56C6C'
})

// 格式化百分比
const percentFormat = (percentage) => {
  return `${percentage.toFixed(1)}`
}

// 监听内容变化
const onContentChange = () => {
  if (content.value !== originalContent.value) {
    hasChanges.value = true
    isSaved.value = false
  } else {
    hasChanges.value = false
  }
}

// 开始编辑
const startEdit = async (retryCount = 0) => {
  loading.value = true;
  try {
    console.log(`尝试获取教案内容, ID: ${planId}`);
    const response = await axios.get(`${BaseUrl}api/teaching-plan-generator/${planId}/edit`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    });
    
    console.log("API响应数据:", response.data);
    
    if (response.data && response.data.success) {
      // 确保content字段存在，如果不存在则使用空字符串
      if (response.data.content !== undefined) {
        content.value = response.data.content || '';
        console.log(`成功获取内容，长度: ${content.value.length}`);
      } else {
        content.value = '';
        console.warn('API响应中缺少content字段');
      }
      
      originalContent.value = content.value;
      
      // 检查并设置编辑开始时间
      if (response.data.editStartTime) {
        editStartTime.value = response.data.editStartTime;
        console.log(`编辑开始时间: ${editStartTime.value}`);
      } else {
        console.warn('API响应中缺少editStartTime字段');
        editStartTime.value = new Date().toISOString();
      }
      
      // 开始计时
      startTimer();
      
      isEditing.value = true;
      ElMessage.success('开始编辑教案');
      
      // 如果有警告信息，显示给用户
      if (response.data.warning) {
        ElMessage({
          message: response.data.warning,
          type: 'warning',
          duration: 5000
        });
      }
    } else {
      const errorMsg = response.data?.message || '无法开始编辑，请稍后重试';
      console.error('开始编辑失败，服务器返回:', response.data);
      ElMessage.error(errorMsg);
      
      // 显示更详细的错误信息
      if (errorMsg.includes('文件不存在') || errorMsg.includes('文件路径为空')) {
        ElMessage({
          message: '教案文件可能已被移动或删除，请联系管理员',
          type: 'warning',
          duration: 5000
        });
      }
    }
  } catch (error) {
    console.error('开始编辑失败:', error);
    
    // 尝试检查响应内容以进行更好的诊断
    if (error.response && error.response.data) {
      console.log('错误响应详情:', JSON.stringify(error.response.data));
    }
    
    // 显示更详细的错误信息
    let errorMessage = '开始编辑失败';
    if (error.response) {
      // 服务器返回了错误响应
      console.error('错误响应状态:', error.response.status);
      console.error('错误响应数据:', error.response.data);
      
      // 检查是否是可能的字符编码或JSON解析问题
      if (error.response.status === 400 && typeof error.response.data === 'string') {
        try {
          // 尝试手动解析响应
          const parsedData = JSON.parse(error.response.data);
          errorMessage += `: ${parsedData.message || '数据解析错误'}`;
        } catch (e) {
          errorMessage += ': 响应数据格式错误';
        }
      } else {
        errorMessage += `: ${error.response.data?.message || error.response.statusText}`;
      }
      
      // 如果是400错误且重试次数少于3次，则尝试重试
      if (error.response.status === 400 && retryCount < 3) {
        ElMessage({
          message: `编辑初始化失败，正在尝试重试 (${retryCount + 1}/3)...`,
          type: 'warning',
          duration: 2000
        });
        
        // 延迟2秒后重试
        setTimeout(() => {
          startEdit(retryCount + 1);
        }, 2000);
        return;
      }
    } else if (error.request) {
      // 请求发送了但没有收到响应
      errorMessage += ': 服务器未响应，请检查网络连接';
    } else {
      // 请求设置时发生错误
      errorMessage += `: ${error.message}`;
    }
    
    ElMessage.error(errorMessage);
    
    // 添加额外的调试信息和后备方案
    if (retryCount >= 3) {
      ElMessage({
        message: '多次尝试失败，请尝试查看详情或刷新页面重试',
        type: 'error',
        duration: 0 // 不自动关闭
      });
      
      // 提供一个检查教案详情的选项
      ElMessageBox.confirm(
        '编辑初始化持续失败，是否查看教案详情进行诊断？',
        '错误诊断',
        {
          confirmButtonText: '查看详情',
          cancelButtonText: '返回列表',
          type: 'warning'
        }
      ).then(() => {
        // 打开新窗口查看教案详情
        window.open(`${BaseUrl}api/teaching-plan-generator/${planId}/details`, '_blank');
      }).catch(() => {
        // 返回列表
        router.push('/manage/teaching-plan');
      });
    }
  } finally {
    loading.value = false;
  }
}

// 保存教案内容
const saveTeachingPlan = async () => {
  if (!hasChanges.value) return;
  
  // 安全检查 - 防止内容过大
  if (content.value && content.value.length > 1000000) {
    ElMessageBox.confirm(
      '当前内容非常大，可能导致保存问题。建议减少内容量或分段编辑。是否仍要尝试保存？',
      '内容过大警告',
      {
        confirmButtonText: '继续保存',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      // 用户确认，继续保存
      doSaveTeachingPlan();
    }).catch(() => {
      // 用户取消，不做任何操作
    });
  } else {
    // 内容大小正常，直接保存
    doSaveTeachingPlan();
  }
};

// 实际执行保存的方法
const doSaveTeachingPlan = async () => {
  saving.value = true;
  try {
    console.log(`保存教案内容, ID: ${planId}, 内容长度: ${content.value.length}`);
    const response = await axios.post(
      `${BaseUrl}api/teaching-plan-generator/${planId}/edit/save`, 
      { content: content.value },
      {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      }
    );
    
    console.log("保存响应:", response.data);
    
    if (response.data && response.data.success) {
      originalContent.value = content.value;
      hasChanges.value = false;
      isSaved.value = true;
      ElMessage.success('保存成功');
    } else {
      console.error('保存失败，服务器返回:', response.data);
      ElMessage.error(response.data?.message || '保存失败，请重试');
    }
  } catch (error) {
    console.error('保存失败:', error);
    
    // 更详细的错误信息
    let errorMessage = '保存失败';
    if (error.response) {
      console.error('错误响应状态:', error.response.status);
      console.error('错误响应数据:', error.response.data);
      errorMessage += `: ${error.response.data?.message || error.response.statusText}`;
      
      // 检查是否是内容过大导致的问题
      if (error.response.status === 413 || 
          (error.response.status === 400 && content.value.length > 1000000)) {
        errorMessage = '内容过大，无法保存。请尝试减少内容量或分段保存。';
      }
    } else if (error.request) {
      errorMessage += ': 服务器未响应，请检查网络连接';
    } else {
      errorMessage += `: ${error.message}`;
    }
    
    ElMessage.error(errorMessage);
  } finally {
    saving.value = false;
  }
};

// 完成编辑
const finishEdit = async () => {
  finishing.value = true;
  try {
    console.log(`完成教案编辑, ID: ${planId}`);
    const response = await axios.post(
      `${BaseUrl}api/teaching-plan-generator/${planId}/edit/finish`, 
      {},
      {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      }
    );
    
    console.log("完成编辑响应:", response.data);
    
    if (response.data && response.data.success) {
      // 停止计时
      stopTimer();
      
      isEditing.value = false;
      isSaved.value = true;
      
      // 获取效率数据
      if (response.data.editDuration !== undefined) {
        editDuration.value = response.data.editDuration;
      }
      if (response.data.efficiencyIndex !== undefined) {
        efficiencyIndex.value = response.data.efficiencyIndex;
      }
      if (response.data.optimizationSuggestions) {
        optimizationSuggestions.value = response.data.optimizationSuggestions;
      }
      
      showEfficiency.value = true;
      efficiencyDialogVisible.value = true;
      
      ElMessage.success('教案编辑完成');
    } else {
      console.error('完成编辑失败，服务器返回:', response.data);
      ElMessage.error(response.data?.message || '完成编辑失败，请重试');
    }
  } catch (error) {
    console.error('完成编辑失败:', error);
    
    // 更详细的错误信息
    let errorMessage = '完成编辑失败';
    if (error.response) {
      console.error('错误响应状态:', error.response.status);
      console.error('错误响应数据:', error.response.data);
      errorMessage += `: ${error.response.data?.message || error.response.statusText}`;
    } else if (error.request) {
      errorMessage += ': 服务器未响应，请检查网络连接';
    } else {
      errorMessage += `: ${error.message}`;
    }
    
    ElMessage.error(errorMessage);
    
    // 如果完成失败，提供重试选项
    ElMessageBox.confirm(
      '完成编辑过程失败，是否重试？',
      '操作失败',
      {
        confirmButtonText: '重试',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      finishEdit(); // 重试完成编辑
    }).catch(() => {
      // 用户取消，不做任何操作
    });
  } finally {
    finishing.value = false;
  }
};

// 确认完成编辑
const confirmFinish = () => {
  if (hasChanges.value) {
    ElMessageBox.confirm(
      '您有未保存的更改，是否在完成前保存？',
      '未保存的更改',
      {
        confirmButtonText: '保存并完成',
        cancelButtonText: '直接完成',
        type: 'warning'
      }
    ).then(() => {
      // 先保存，然后在保存成功后完成编辑
      saving.value = true;
      doSaveTeachingPlan().then(() => {
        // 只有在保存成功且没有更改的情况下才完成
        if (!hasChanges.value) {
          finishEdit();
        }
      }).catch(() => {
        ElMessage.warning('保存失败，请先解决保存问题再完成编辑');
      }).finally(() => {
        saving.value = false;
      });
    }).catch((action) => {
      if (action === 'cancel') {
        finishEdit();
      }
    });
  } else {
    finishEdit();
  }
};

// 返回列表
const backToList = () => {
  if (hasChanges.value) {
    ElMessageBox.confirm(
      '您有未保存的更改，确定要离开吗？',
      '未保存的更改',
      {
        confirmButtonText: '确认离开',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      router.push('/manage/teaching-plan')
    }).catch(() => {})
  } else {
    router.push('/manage/teaching-plan')
  }
}

// 开始计时
const startTimer = () => {
  stopTimer()
  durationTimer = setInterval(() => {
    editDuration.value += 1 // 每秒增加1
  }, 1000)
}

// 停止计时
const stopTimer = () => {
  if (durationTimer) {
    clearInterval(durationTimer)
    durationTimer = null
  }
}

// 格式化日期时间
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '--'
  
  const date = new Date(dateTimeStr)
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(date)
}

// 格式化时长
const formatDuration = (seconds) => {
  if (seconds === null || seconds === undefined) return '--'
  
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainingSeconds = seconds % 60
  
  return [
    hours > 0 ? `${hours}小时` : '',
    minutes > 0 ? `${minutes}分钟` : '',
    `${remainingSeconds}秒`
  ].filter(Boolean).join(' ')
}

// 组件挂载时获取教案内容
onMounted(() => {
  if (!planId) {
    ElMessage.error('缺少教案ID参数')
    router.push('/manage/teaching-plan')
    return
  }
  
  startEdit()
})

// 组件卸载前清理计时器
onBeforeUnmount(() => {
  stopTimer()
})
</script>

<style scoped>
/* 添加自定义样式类 */
.min-h-500 {
  min-height: 500px;
}

.line-height-md {
  line-height: 1.8;
}

.font-bold {
  font-weight: bold;
}
</style> 