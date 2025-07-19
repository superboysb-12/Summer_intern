<!-- UsageStatistics.vue - 模块使用统计组件 -->
<template>
  <div class="usage-statistics-container">
    <div class="header-section">
      <h2>模块使用统计</h2>
      <el-select v-model="selectedTimeRange" class="time-range-selector">
        <el-option label="今日" value="today" />
        <el-option label="本周" value="week" />
      </el-select>
    </div>

    <!-- 加载状态显示 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 数据为空时的显示 -->
    <el-empty v-else-if="noDataAvailable" description="暂无使用数据" />

    <!-- 数据显示区域 -->
    <div v-else class="statistics-content">
      <!-- 图表概览区 -->
      <div class="charts-section">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <h3>按角色统计</h3>
              <el-tag type="primary" effect="plain">{{ selectedTimeRange === 'today' ? '今日' : '本周' }}</el-tag>
            </div>
          </template>
          <div ref="roleChartRef" class="chart-container"></div>
        </el-card>

        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <h3>按模块统计</h3>
              <el-tag type="success" effect="plain">{{ selectedTimeRange === 'today' ? '今日' : '本周' }}</el-tag>
            </div>
          </template>
          <div ref="moduleChartRef" class="chart-container"></div>
        </el-card>
      </div>

      <!-- 详细数据表格 -->
      <el-tabs type="border-card" class="data-tabs">
        <el-tab-pane label="教师使用统计">
          <el-table :data="teacherData" stripe style="width: 100%">
            <el-table-column prop="module" label="模块名称" />
            <el-table-column prop="count" label="使用次数" sortable />
            <el-table-column prop="percentage" label="占比">
              <template #default="scope">
                <el-progress 
                  :percentage="scope.row.percentage" 
                  :stroke-width="15"
                  :color="getProgressColor(scope.row.percentage)"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="学生使用统计">
          <el-table :data="studentData" stripe style="width: 100%">
            <el-table-column prop="module" label="模块名称" />
            <el-table-column prop="count" label="使用次数" sortable />
            <el-table-column prop="percentage" label="占比">
              <template #default="scope">
                <el-progress 
                  :percentage="scope.row.percentage" 
                  :stroke-width="15"
                  :color="getProgressColor(scope.row.percentage)"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import axios from 'axios';
import * as echarts from 'echarts';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { useCounterStore } from '../stores/counter';

// 状态变量
const loading = ref(true);
const selectedTimeRange = ref('today');
const statsData = ref({
  teacherToday: {},
  teacherWeek: {},
  studentToday: {},
  studentWeek: {}
});

// 图表引用
const roleChartRef = ref(null);
const moduleChartRef = ref(null);
let roleChart = null;
let moduleChart = null;
const router = useRouter();
const counterStore = useCounterStore();

// 模块名称映射
const moduleNameMap = {
  'COURSE': '课程管理',
  'SCHOOL_CLASS': '班级管理',
  'TEACHING_PLAN_GENERATOR': '教案生成器',
  'DEEP_SEEK_CHAT': '智能助手'
};

// 获取模块显示名称
const getModuleDisplayName = (key) => moduleNameMap[key] || key;

// 计算是否没有可用数据
const noDataAvailable = computed(() => {
  const teacherData = selectedTimeRange.value === 'today' 
    ? statsData.value.teacherToday
    : statsData.value.teacherWeek;
  
  const studentData = selectedTimeRange.value === 'today'
    ? statsData.value.studentToday
    : statsData.value.studentWeek;
  
  const hasTeacherData = teacherData && Object.values(teacherData).some(count => count > 0);
  const hasStudentData = studentData && Object.values(studentData).some(count => count > 0);
  
  return !hasTeacherData && !hasStudentData;
});

// 计算教师数据
const teacherData = computed(() => {
  const data = selectedTimeRange.value === 'today'
    ? statsData.value.teacherToday || {}
    : statsData.value.teacherWeek || {};
  
  const totalCount = Object.values(data).reduce((sum, count) => sum + count, 0) || 1;
  
  return Object.entries(data).map(([key, count]) => ({
    module: getModuleDisplayName(key),
    count: count,
    percentage: Math.round((count / totalCount) * 100) || 0
  })).sort((a, b) => b.count - a.count); // 按使用次数降序排序
});

// 计算学生数据
const studentData = computed(() => {
  const data = selectedTimeRange.value === 'today'
    ? statsData.value.studentToday || {}
    : statsData.value.studentWeek || {};
  
  const totalCount = Object.values(data).reduce((sum, count) => sum + count, 0) || 1;
  
  return Object.entries(data).map(([key, count]) => ({
    module: getModuleDisplayName(key),
    count: count,
    percentage: Math.round((count / totalCount) * 100) || 0
  })).sort((a, b) => b.count - a.count); // 按使用次数降序排序
});

// 获取进度条颜色
const getProgressColor = (percentage) => {
  if (percentage < 20) return '#909399';
  if (percentage < 40) return '#67C23A';
  if (percentage < 60) return '#E6A23C';
  if (percentage < 80) return '#F56C6C';
  return '#409EFF';
};

// 初始化角色对比图表
const initRoleChart = () => {
  if (!roleChartRef.value) return;
  
  roleChart = echarts.init(roleChartRef.value);
  updateRoleChart();
};

// 初始化模块对比图表
const initModuleChart = () => {
  if (!moduleChartRef.value) return;
  
  moduleChart = echarts.init(moduleChartRef.value);
  updateModuleChart();
};

// 更新角色对比图表
const updateRoleChart = () => {
  if (!roleChart) return;
  
  const teacherData = selectedTimeRange.value === 'today'
    ? statsData.value.teacherToday
    : statsData.value.teacherWeek;
  
  const studentData = selectedTimeRange.value === 'today'
    ? statsData.value.studentToday
    : statsData.value.studentWeek;
  
  const teacherCount = Object.values(teacherData || {}).reduce((sum, count) => sum + count, 0);
  const studentCount = Object.values(studentData || {}).reduce((sum, count) => sum + count, 0);
  
  roleChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'horizontal',
      bottom: 'bottom',
      data: ['教师', '学生']
    },
    series: [
      {
        name: '使用次数',
        type: 'pie',
        radius: ['50%', '70%'],
        avoidLabelOverlap: false,
        emphasis: {
          label: {
            show: true,
            fontSize: '16',
            fontWeight: 'bold'
          }
        },
        data: [
          { value: teacherCount, name: '教师' },
          { value: studentCount, name: '学生' }
        ],
        color: ['#409EFF', '#67C23A']
      }
    ]
  });
};

// 更新模块对比图表
const updateModuleChart = () => {
  if (!moduleChart) return;
  
  const teacherData = selectedTimeRange.value === 'today'
    ? statsData.value.teacherToday || {}
    : statsData.value.teacherWeek || {};
    
  const studentData = selectedTimeRange.value === 'today'
    ? statsData.value.studentToday || {}
    : statsData.value.studentWeek || {};
  
  // 合并并汇总所有模块的数据
  const moduleData = {};
  
  Object.entries(teacherData).forEach(([key, count]) => {
    moduleData[key] = (moduleData[key] || 0) + count;
  });
  
  Object.entries(studentData).forEach(([key, count]) => {
    moduleData[key] = (moduleData[key] || 0) + count;
  });
  
  const chartData = Object.entries(moduleData).map(([key, value]) => ({
    name: getModuleDisplayName(key),
    value
  }));
  
  moduleChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      type: 'scroll',
      orient: 'horizontal',
      bottom: 'bottom'
    },
    series: [
      {
        name: '模块使用',
        type: 'pie',
        radius: '70%',
        center: ['50%', '50%'],
        data: chartData.length ? chartData : [{ name: '暂无数据', value: 1 }],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  });
};

// 获取统计数据
const fetchData = async () => {
  loading.value = true;
  try {
    // 验证权限
    const userInfo = counterStore.getUserInfo();
    const userRole = userInfo.userRole;
    
    if (userRole !== 'ADMIN' && userRole !== 'TEACHER') {
      ElMessage.error('无权限访问此页面');
      router.push('/manage');
      return;
    }
    
    // 获取token
    const token = localStorage.getItem('token');
    if (!token) {
      ElMessage.error('认证失败，请重新登录');
      router.push('/login');
      return;
    }
    
    // 发起API请求
    const BaseUrl = 'http://localhost:8080';
    const response = await axios.get(`${BaseUrl}/api/module-usage/summary`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.data || typeof response.data !== 'object') {
      throw new Error('服务器返回的数据格式不正确');
    }
    
    statsData.value = {
      teacherToday: response.data.teacherToday || {},
      teacherWeek: response.data.teacherWeek || {},
      studentToday: response.data.studentToday || {},
      studentWeek: response.data.studentWeek || {}
    };
    
    // 初始化图表
    setTimeout(() => {
      initRoleChart();
      initModuleChart();
    }, 50);
  } catch (error) {
    console.error('获取模块使用统计数据失败:', error);
    
    if (error.response) {
      if (error.response.status === 403) {
        ElMessage.error('无权访问此资源，请确认您有管理员或教师权限');
      } else if (error.response.status === 401) {
        ElMessage.error('认证失败，请重新登录');
        router.push('/login');
      } else {
        ElMessage.error(`服务器错误 (${error.response.status})`);
      }
    } else if (error.request) {
      ElMessage.error('服务器未响应，请检查网络连接');
    } else {
      ElMessage.error('请求错误: ' + error.message);
    }
    
    // 设置空数据
    statsData.value = {
      teacherToday: {},
      teacherWeek: {},
      studentToday: {},
      studentWeek: {}
    };
  } finally {
    loading.value = false;
  }
};

// 处理窗口大小变化
const handleResize = () => {
  if (roleChart) roleChart.resize();
  if (moduleChart) moduleChart.resize();
};

// 监听时间范围变化
watch(selectedTimeRange, () => {
  if (roleChart && moduleChart) {
    updateRoleChart();
    updateModuleChart();
  }
});

// 组件挂载时
onMounted(() => {
  fetchData();
  window.addEventListener('resize', handleResize);
});

// 组件卸载前
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  
  if (roleChart) {
    roleChart.dispose();
    roleChart = null;
  }
  
  if (moduleChart) {
    moduleChart.dispose();
    moduleChart = null;
  }
});
</script>

<style scoped>
.usage-statistics-container {
  padding: 24px;
  width: 100%;
  background-color: #f9fafb;
  min-height: 100vh;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 10px;
}

.time-range-selector {
  width: 120px;
}

.loading-container, .el-empty {
  padding: 40px;
  border-radius: 12px;
  background-color: #ffffff;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
}

.statistics-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.charts-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.chart-card{
  height: 600px;
}

.chart-card, .data-tabs {
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
  background-color: #ffffff;
  transition: box-shadow 0.3s ease-in-out;

}

.chart-card:hover, .data-tabs:hover {
  box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -2px rgb(0 0 0 / 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px; /* Add padding for spacing */
}

.chart-container {
  height: 400px; /* Increased height */
  width: 100%;
}

.data-tabs {
  margin-top: 0;
}

h2 {
  margin: 0;
  color: #1f2937;
  font-size: 22px;
  font-weight: 600;
}

h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #374151;
}

@media screen and (max-width: 992px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
}
</style> 