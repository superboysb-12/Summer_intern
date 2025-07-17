<template>
  <div class="message-list-container">
    <el-table
      :data="safeMessages"
      style="width: 100%"
      v-loading="loading"
      @row-click="handleRowClick"
    >
      <el-table-column width="50">
        <template #default="scope">
          <el-icon v-if="!isOutbox && !scope.row.read"><el-icon-message /></el-icon>
          <el-icon v-else-if="!isOutbox && scope.row.read"><el-icon-chat-dot-round /></el-icon>
          <el-icon v-else><el-icon-position /></el-icon>
        </template>
      </el-table-column>
      
      <el-table-column label="发件人" v-if="!isOutbox" width="150">
        <template #default="scope">
          {{ scope.row.sender?.name || scope.row.sender?.username }}
        </template>
      </el-table-column>
      
      <el-table-column label="收件人" v-if="isOutbox" width="150">
        <template #default="scope">
          {{ scope.row.recipient?.name || scope.row.recipient?.username }}
        </template>
      </el-table-column>
      
      <el-table-column label="主题" min-width="200">
        <template #default="scope">
          <span :class="{'unread': !isOutbox && !scope.row.read}">
            {{ scope.row.subject }}
          </span>
        </template>
      </el-table-column>
      
      <el-table-column label="时间" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.sentAt) }}
        </template>
      </el-table-column>
      
      <el-table-column width="120">
        <template #default="scope">
          <el-button 
            @click.stop="handleView(scope.row)" 
            type="primary" 
            size="small"
            text
          >查看</el-button>
          <el-button 
            @click.stop="handleDelete(scope.row)" 
            type="danger" 
            size="small"
            text
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div v-if="safeMessages.length === 0 && !loading" class="empty-state">
      <el-empty description="暂无消息" />
    </div>
  </div>
</template>

<script>
import { ElMessageBox } from 'element-plus'
import { 
  Message as ElIconMessage, 
  ChatDotRound as ElIconChatDotRound,
  Position as ElIconPosition
} from '@element-plus/icons-vue'
import { computed } from 'vue'

export default {
  props: {
    messages: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    isOutbox: {
      type: Boolean,
      default: false
    }
  },
  
  components: {
    ElIconMessage,
    ElIconChatDotRound,
    ElIconPosition
  },
  
  emits: ['view', 'delete'],
  
  setup(props, { emit }) {
    // 确保messages是安全的数组
    const safeMessages = computed(() => {
      return Array.isArray(props.messages) ? props.messages : [];
    });
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      
      const date = new Date(dateString);
      
      try {
        // 如果是今天，只显示时间
        const today = new Date();
        if (date.toDateString() === today.toDateString()) {
          return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        }
        
        // 如果是今年，显示月日和时间
        if (date.getFullYear() === today.getFullYear()) {
          return date.toLocaleDateString([], { month: '2-digit', day: '2-digit' }) + ' ' + 
                 date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        }
        
        // 否则显示完整日期
        return date.toLocaleDateString() + ' ' + 
               date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      } catch (error) {
        console.error('Error formatting date:', error);
        return dateString;
      }
    }
    
    const handleRowClick = (row) => {
      if (row && row.id) {
        emit('view', row);
      }
    }
    
    const handleView = (row) => {
      if (row && row.id) {
        emit('view', row);
      }
    }
    
    const handleDelete = (row) => {
      if (!row || !row.id) return;
      
      ElMessageBox.confirm(
        '确定要删除这条消息吗？',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(() => {
        emit('delete', row.id);
      }).catch(() => {});
    }
    
    return {
      safeMessages,
      formatDate,
      handleRowClick,
      handleView,
      handleDelete
    }
  }
}
</script>

<style scoped>
.message-list-container {
  margin-top: 20px;
}

.unread {
  font-weight: bold;
}

.empty-state {
  margin-top: 40px;
  text-align: center;
}
</style> 