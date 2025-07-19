<script setup>
import { ref, onMounted, reactive, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { ArrowLeft, Reading, ArrowRight } from '@element-plus/icons-vue'
import axios from 'axios'
import { useCounterStore } from '../../stores/counter'

const store = useCounterStore()
const route = useRoute()
const router = useRouter()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')
const userInfo = ref(store.getUserInfo())

// 作业信息
const homework = ref(null)
const loading = ref(false)

// 题目列表
const questions = ref([])
const loadingQuestions = ref(false)
const totalQuestions = ref(0)
const completedQuestions = ref(0)

// 当前题目
const currentQuestionIndex = ref(0)
const currentQuestion = computed(() => {
  if (questions.value.length === 0) return null
  return questions.value[currentQuestionIndex.value]
})

// 用户答案
const userAnswers = ref({}) // questionId -> answer
const userScores = ref({}) // questionId -> score

// 练习记录
const practiceResults = ref({}) // questionId -> result
const submitting = ref(false)

// 本地存储键
const getStorageKey = (homeworkId) => `homework_answers_${homeworkId}_${userInfo.value.id}`

// 评分结果统计
const scoreStats = computed(() => {
  if (questions.value.length === 0) return null
  
  const totalScore = Object.values(userScores.value).reduce((sum, score) => sum + score, 0)
  const avgScore = totalScore / Math.max(Object.keys(userScores.value).length, 1)
  
  return {
    total: totalScore,
    average: avgScore.toFixed(1),
    completed: Object.keys(userScores.value).length,
    totalQuestions: questions.value.length
  }
})

// 加载作业信息
const loadHomework = async () => {
  const homeworkId = route.params.id
  if (!homeworkId) {
    ElMessage.error('作业ID不能为空')
    return
  }
  
  loading.value = true
  try {
    const response = await axios.get(`${BaseUrl}homeworks/${homeworkId}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    homework.value = response.data
  } catch (error) {
    console.error('获取作业信息失败:', error)
    ElMessage.error('获取作业信息失败')
  } finally {
    loading.value = false
  }
}

// 加载作业题目
const loadQuestions = async () => {
  const homeworkId = route.params.id
  if (!homeworkId) return
  
  loadingQuestions.value = true
  questions.value = [] // 清空现有题目列表
  
  try {
    console.log('开始加载作业题目，作业ID:', homeworkId)
    
    // 获取作业关联的题目
    const homeworkQuestionsResponse = await axios.get(`${BaseUrl}homework-questions/homework/${homeworkId}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    const homeworkQuestions = homeworkQuestionsResponse.data || []
    console.log('获取到作业关联题目:', homeworkQuestions.length, '个')
    totalQuestions.value = homeworkQuestions.length
    
    if (homeworkQuestions.length === 0) {
      loadingQuestions.value = false
      return
    }
    
    // 获取每个题目的详情
    const questionDetails = []
    for (const hq of homeworkQuestions) {
      try {
        console.log('获取题目详情，题目ID:', hq.questionId)
        // 修正API路径，确保与QuestionGenerator.vue一致
        const questionResponse = await axios.get(`${BaseUrl}api/question-generator/${hq.questionId}`, {
          headers: { 'Authorization': `Bearer ${getToken()}` }
        })
        
        console.log('题目API响应:', questionResponse.data)
        
        if (questionResponse.data) {
          // 解析题目内容并添加权重信息
          const questionData = questionResponse.data
          if (questionData.questionJson) {
            try {
              questionData.parsedQuestion = JSON.parse(questionData.questionJson)
              questionData.weight = hq.weight || 1.0
              questionDetails.push(questionData)
              console.log('题目添加成功，ID:', questionData.id)
            } catch (parseError) {
              console.error('解析题目JSON失败:', parseError)
            }
          } else {
            console.warn('题目JSON为空, ID:', hq.questionId)
          }
        } else {
          console.warn('题目响应无数据, ID:', hq.questionId)
        }
      } catch (qError) {
        console.error(`获取题目 ${hq.questionId} 详情失败:`, qError)
      }
    }
    
    console.log('成功加载题目数量:', questionDetails.length)
    
    // 按照作业问题的顺序排序
    questions.value = questionDetails.sort((a, b) => {
      const aIndex = homeworkQuestions.findIndex(hq => hq.questionId === a.id)
      const bIndex = homeworkQuestions.findIndex(hq => hq.questionId === b.id)
      return aIndex - bIndex
    })
    
    // 从本地存储加载答案
    loadStoredAnswers()
  } catch (error) {
    console.error('获取作业题目失败:', error)
    ElMessage.error('获取作业题目失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loadingQuestions.value = false
    console.log('题目加载完成，状态设置为false')
  }
}

// 存储答案到本地
const saveAnswersToStorage = () => {
  const homeworkId = route.params.id
  if (!homeworkId) return
  
  const data = {
    answers: userAnswers.value,
    scores: userScores.value,
    lastUpdated: new Date().toISOString()
  }
  
  localStorage.setItem(getStorageKey(homeworkId), JSON.stringify(data))
}

// 从本地存储加载答案
const loadStoredAnswers = () => {
  const homeworkId = route.params.id
  if (!homeworkId) return
  
  const storedData = localStorage.getItem(getStorageKey(homeworkId))
  if (storedData) {
    try {
      const data = JSON.parse(storedData)
      userAnswers.value = data.answers || {}
      userScores.value = data.scores || {}
      
      // 计算已完成题目数量
      completedQuestions.value = Object.keys(userScores.value).length
    } catch (e) {
      console.error('解析存储的答案失败:', e)
    }
  }
}

// 更新用户答案
const updateUserAnswer = (questionId, answer) => {
  userAnswers.value[questionId] = answer
  saveAnswersToStorage()
}

// 计算选择题/判断题得分
const calculateObjectiveScore = (userAnswer, correctAnswer) => {
  if (userAnswer === correctAnswer) {
    return 100
  }
  return 0
}

// 评分函数
const gradeQuestion = async (questionId, questionType, userAnswer, correctAnswer) => {
  // 根据题目类型评分
  if (questionType === '选择题' || questionType === '判断题') {
    // 选择题和判断题直接比对答案
    const score = calculateObjectiveScore(userAnswer, correctAnswer)
    userScores.value[questionId] = score
    saveAnswersToStorage()
    return score
  } else {
    // 主观题调用大模型API评分
    try {
      // 构建评分请求
      const systemPrompt = `
你是一个公正的教育评分系统，需要为以下学生回答评分。评分标准是百分制（0-100分）。
请根据参考答案评估学生答案的正确性、完整性和表达质量。

请用JSON格式返回评分结果，格式如下：
{
  "score": 分数（0-100的整数，表示得分），
  "feedback": "详细反馈，包括优点和不足，以及改进建议"
}

评分标准：
- 90-100分：答案完全正确，表述清晰，内容全面
- 75-89分：答案基本正确，有一些小的不足
- 60-74分：答案部分正确，有明显不足
- 40-59分：答案有较大偏差，但有一些正确的要点
- 0-39分：答案完全错误或文不对题

请确保输出是有效的JSON格式，可以直接被JSON.parse()解析。`

      // 准备用户消息
      const userMessage = `
参考答案: ${correctAnswer}
学生答案: ${userAnswer}

请为这个${questionType}答案评分`

      // 准备API调用的消息
      const apiMessages = [
        { role: "system", content: systemPrompt },
        { role: "user", content: userMessage }
      ]

      // 调用DeepSeekChat API
      const aiResponse = await axios.post(`${BaseUrl}api/deepseek/chat`, 
        { messages: apiMessages },
        { headers: { 'Authorization': `Bearer ${getToken()}` } }
      )
                
      if (aiResponse.data && aiResponse.data.choices && aiResponse.data.choices.length > 0) {
        const aiMessage = aiResponse.data.choices[0].message
        try {
          // 获取内容
          let contentToProcess = aiMessage.content
          
          // 处理可能带有的Markdown代码块
          const jsonBlockRegex = /```json\s*([\s\S]*?)\s*```/
          const markdownMatch = jsonBlockRegex.exec(contentToProcess)
          
          if (markdownMatch && markdownMatch[1]) {
            // 如果找到了json代码块，则提取其中的内容
            contentToProcess = markdownMatch[1].trim()
          }
          
          // 解析JSON响应
          const gradingResult = JSON.parse(contentToProcess)
          
          // 设置分数
          const score = gradingResult.score || 0
          userScores.value[questionId] = score
          
          // 保存评分结果
          practiceResults.value[questionId] = {
            score: score,
            feedback: gradingResult.feedback || '无反馈',
            aiGenerated: true
          }
          
          saveAnswersToStorage()
          return score
        } catch (jsonError) {
          console.error('JSON解析失败:', jsonError)
          // 出错时默认给60分
          const score = 60
          userScores.value[questionId] = score
          saveAnswersToStorage()
          return score
        }
      }
    } catch (error) {
      console.error('AI评分失败:', error)
      // 出错时默认给60分
      const score = 60
      userScores.value[questionId] = score
      saveAnswersToStorage()
      return score
    }
  }
}

// 提交答案
const submitAnswer = async () => {
  if (!currentQuestion.value) return
  
  const questionId = currentQuestion.value.id
  const userAnswer = userAnswers.value[questionId]
  
  if (!userAnswer) {
    ElMessage.warning('请先作答再提交')
    return
  }
  
  // 获取正确答案
  const questionJson = currentQuestion.value.parsedQuestion
  const correctAnswer = questionJson.answer
  const questionType = currentQuestion.value.questionType
  
  // 评分
  const score = await gradeQuestion(questionId, questionType, userAnswer, correctAnswer)
  
  // 更新完成题目计数
  completedQuestions.value = Object.keys(userScores.value).length
  
  ElMessage.success(`评分完成: ${score}分`)
  
  // 检查是否为最后一题，提示是否提交整个作业
  if (currentQuestionIndex.value === questions.value.length - 1 && completedQuestions.value === questions.value.length) {
    setTimeout(() => {
      ElMessageBox.confirm('您已完成所有题目，是否提交练习记录？', '提交确认', {
        confirmButtonText: '提交',
        cancelButtonText: '稍后提交',
        type: 'info'
      }).then(() => {
        submitPracticeRecords()
      }).catch(() => {})
    }, 500)
  }
}

// 提交练习记录
const submitPracticeRecords = async () => {
  if (submitting.value) return
  
  submitting.value = true
  const homeworkId = route.params.id
  
  try {
    const loading = ElLoading.service({
      lock: true,
      text: '提交中...',
      background: 'rgba(0, 0, 0, 0.7)'
    })
    
    // 提交每个题目的练习记录
    for (const question of questions.value) {
      const questionId = question.id
      const userAnswer = userAnswers.value[questionId]
      const score = userScores.value[questionId]
      
      if (userAnswer && score !== undefined) {
        const questionJson = question.parsedQuestion
        
        // 准备提交数据
        const answerData = {
          userAnswer: userAnswer,
          correctAnswer: questionJson.answer,
          analysis: questionJson.analysis || '',
          questionType: question.questionType,
          feedback: practiceResults.value[questionId]?.feedback || ''
        }
        
        const currentTime = new Date().toISOString();
        
        // 提交练习记录，添加finish_time字段
        await axios.post(`${BaseUrl}practice-records/submit`, {
          studentId: userInfo.value.id,
          homeworkId: parseInt(homeworkId),
          questionId: questionId,
          score: score,
          answerData: JSON.stringify(answerData),
          timeSpent: 120, // 假设用时2分钟，实际应该记录时间
          finish_time: currentTime, // 将finishTime改为finish_time，匹配后端期望的字段名
          isCorrect: score >= 60  // 添加是否正确字段（分数≥60视为正确）
        }, {
          headers: { 'Authorization': `Bearer ${getToken()}` }
        })
      }
    }
    
    loading.close()
    ElMessage.success('练习记录提交成功')
    
    // 清除本地存储
    localStorage.removeItem(getStorageKey(homeworkId))
    
    // 返回作业列表页
    router.push('/manage/assignments')
  } catch (error) {
    console.error('提交练习记录失败:', error)
    ElMessage.error('提交练习记录失败: ' + (error.response?.data?.message || error.message))
  } finally {
    submitting.value = false
  }
}

// 导航到上一题/下一题
const goToPrevQuestion = () => {
  if (currentQuestionIndex.value > 0) {
    currentQuestionIndex.value--
  }
}

const goToNextQuestion = () => {
  if (currentQuestionIndex.value < questions.value.length - 1) {
    currentQuestionIndex.value++
  }
}

const goToQuestion = (index) => {
  if (index >= 0 && index < questions.value.length) {
    currentQuestionIndex.value = index
  }
}

// 检查是否过期
const isOverdue = (dueDate) => {
  if (!dueDate) return false
  
  const due = new Date(dueDate)
  const now = new Date()
  
  return due < now
}

// 获取题目状态
const getQuestionStatus = (questionId) => {
  if (userScores.value[questionId] !== undefined) {
    return 'completed'
  }
  if (userAnswers.value[questionId]) {
    return 'answered'
  }
  return 'unanswered'
}

// 获取分数颜色
const getScoreColor = (score) => {
  if (score >= 90) return '#67C23A'
  if (score >= 75) return '#409EFF'
  if (score >= 60) return '#E6A23C'
  return '#F56C6C'
}

// 初始化
onMounted(() => {
  loadHomework()
  loadQuestions()
})
</script>

<template>
  <div class="assignment-detail-container">
    <!-- 顶部标题和导航 -->
    <div class="page-header">
      <div class="back-nav">
        <el-button text @click="router.push('/manage/assignments')">
          <el-icon><ArrowLeft /></el-icon> 返回作业列表
        </el-button>
      </div>
      
      <div v-if="homework" class="title-section">
        <h2>{{ homework.title }}</h2>
        <p class="subtitle">{{ homework.courseName || '未知课程' }} | 
          <span v-if="homework.dueDate" :class="{ 'text-danger': isOverdue(homework.dueDate) }">
            截止日期: {{ new Date(homework.dueDate).toLocaleString() }}
          </span>
          <span v-else>无截止日期</span>
        </p>
        <p class="description">{{ homework.description || '无描述' }}</p>
      </div>
      
      <div class="progress-section">
        <el-progress 
          :percentage="Math.round((completedQuestions / Math.max(totalQuestions, 1)) * 100)" 
          :format="() => `${completedQuestions}/${totalQuestions}`"
          :status="completedQuestions === totalQuestions ? 'success' : ''"
          :stroke-width="15"
        />
      </div>
    </div>
    
    <el-card v-loading="loading || loadingQuestions" class="main-card">
      <div class="card-container">
        <!-- 题目导航 -->
        <div class="question-navigation">
          <div class="question-nav-header">
            <h3>题目导航</h3>
          </div>
          
          <div class="question-nav-list">
            <el-button
              v-for="(question, index) in questions"
              :key="question.id"
              :type="currentQuestionIndex === index ? 'primary' : ''"
              :class="{
                'nav-button': true,
                'completed': getQuestionStatus(question.id) === 'completed',
                'answered': getQuestionStatus(question.id) === 'answered'
              }"
              @click="goToQuestion(index)"
              size="small"
            >
              {{ index + 1 }}
            </el-button>
          </div>
        </div>
        
        <!-- 题目内容区域 -->
        <div class="question-content" v-if="currentQuestion">
          <!-- 题目类型和序号 -->
          <div class="question-header">
            <div class="question-type-badge">
              <el-tag size="medium" effect="dark" type="primary">{{ currentQuestion.questionType }}</el-tag>
              <span class="question-number">第 {{ currentQuestionIndex + 1 }} 题 / 共 {{ questions.length }} 题</span>
            </div>
            <div class="question-weight">
              <el-tag size="small" effect="plain" type="info">分值: {{ currentQuestion.weight * 100 }}%</el-tag>
            </div>
          </div>
          
          <!-- 题目内容 -->
          <div class="question-body">
            <div class="question-title">
              <h3>{{ currentQuestion.parsedQuestion.title }}</h3>
            </div>
            
            <div class="question-description" v-if="currentQuestion.parsedQuestion.description">
              <p>{{ currentQuestion.parsedQuestion.description }}</p>
            </div>
            
            <!-- 根据题目类型显示不同的答题界面 -->
            <div class="answer-section">
              <!-- 选择题 -->
              <template v-if="currentQuestion.questionType === '选择题'">
                <el-radio-group v-model="userAnswers[currentQuestion.id]" @change="updateUserAnswer(currentQuestion.id, userAnswers[currentQuestion.id])" class="option-group">
                  <div v-for="(option, key) in currentQuestion.parsedQuestion.options" :key="key" class="option-item">
                    <el-radio :label="key" border>{{ key }}. {{ option }}</el-radio>
                  </div>
                </el-radio-group>
              </template>
              
              <!-- 判断题 -->
              <template v-else-if="currentQuestion.questionType === '判断题'">
                <el-radio-group v-model="userAnswers[currentQuestion.id]" @change="updateUserAnswer(currentQuestion.id, userAnswers[currentQuestion.id])" class="option-group">
                  <el-radio label="true" border>正确</el-radio>
                  <el-radio label="false" border>错误</el-radio>
                </el-radio-group>
              </template>
              
              <!-- 问答题 -->
              <template v-else-if="currentQuestion.questionType === '问答题'">
                <el-input
                  v-model="userAnswers[currentQuestion.id]"
                  type="textarea"
                  :rows="8"
                  placeholder="请输入您的答案"
                  @blur="updateUserAnswer(currentQuestion.id, userAnswers[currentQuestion.id])"
                  class="text-answer"
                />
              </template>
              
              <!-- 填空题和其他类型 -->
              <template v-else>
                <el-input
                  v-model="userAnswers[currentQuestion.id]"
                  type="textarea"
                  :rows="8"
                  placeholder="请输入您的答案"
                  @blur="updateUserAnswer(currentQuestion.id, userAnswers[currentQuestion.id])"
                  class="text-answer"
                />
              </template>
            </div>
            
            <!-- 评分结果区域 -->
            <div v-if="userScores[currentQuestion.id] !== undefined" class="score-section">
              <div class="score-header">
                <div class="score-display">
                  <span class="score-label">得分:</span>
                  <span class="score-value" :style="{ color: getScoreColor(userScores[currentQuestion.id]) }">
                    {{ userScores[currentQuestion.id] }}
                  </span>
                </div>
              </div>
              
              <el-divider />
              
              <div class="answer-analysis">
                <h4>参考答案:</h4>
                <div class="answer-content">{{ currentQuestion.parsedQuestion.answer }}</div>
              </div>
              
              <div class="answer-analysis" v-if="currentQuestion.parsedQuestion.analysis">
                <h4>解析:</h4>
                <div class="answer-content">{{ currentQuestion.parsedQuestion.analysis }}</div>
              </div>
              
              <div class="feedback" v-if="practiceResults[currentQuestion.id]?.feedback">
                <h4>评价反馈:</h4>
                <div class="feedback-content">{{ practiceResults[currentQuestion.id].feedback }}</div>
              </div>
            </div>
            
            <!-- 操作按钮区 -->
            <div class="question-actions">
              <div>
                <el-button @click="goToPrevQuestion" :disabled="currentQuestionIndex === 0" type="info" plain>
                  <el-icon class="el-icon--left"><ArrowLeft /></el-icon>上一题
                </el-button>
              </div>
              
              <div>
                <el-button type="primary" @click="submitAnswer" 
                  :disabled="!userAnswers[currentQuestion.id] || userScores[currentQuestion.id] !== undefined">
                  提交答案
                </el-button>
              </div>
              
              <div>
                <el-button @click="goToNextQuestion" :disabled="currentQuestionIndex === questions.length - 1" type="info" plain>
                  下一题<el-icon class="el-icon--right"><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <el-empty v-else-if="!loadingQuestions && questions.length === 0" description="未找到题目">
          <p>该作业暂无题目或无法获取题目内容</p>
        </el-empty>
      </div>
      
      <!-- 底部统计区域 -->
      <div class="assignment-summary" v-if="completedQuestions > 0">
        <div class="summary-header">
          <h3>作答统计</h3>
          <el-button type="success" @click="submitPracticeRecords" :disabled="completedQuestions < totalQuestions || submitting">
            {{ completedQuestions < totalQuestions ? '完成所有题目后可提交' : '提交练习记录' }}
          </el-button>
        </div>
        
        <div class="summary-stats" v-if="scoreStats">
          <div class="stat-card">
            <div class="stat-label">完成题目</div>
            <div class="stat-value">{{ scoreStats.completed }}/{{ scoreStats.totalQuestions }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">平均得分</div>
            <div class="stat-value" :style="{ color: getScoreColor(scoreStats.average) }">{{ scoreStats.average }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">总分</div>
            <div class="stat-value">{{ scoreStats.total }}</div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.assignment-detail-container {
  padding: 20px;
  max-width: 1280px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.back-nav {
  margin-bottom: 16px;
}

.title-section h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.subtitle {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 14px;
}

.description {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.text-danger {
  color: #F56C6C;
}

.progress-section {
  margin-top: 16px;
}

.main-card {
  margin-bottom: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.card-container {
  display: flex;
  flex-direction: column;
  min-height: 500px;
}

.question-navigation {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #EBEEF5;
}

.question-nav-header {
  margin-bottom: 10px;
}

.question-nav-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.question-nav-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.nav-button {
  min-width: 36px;
  height: 36px;
}

.nav-button.completed {
  border-color: #67C23A;
  color: #67C23A;
}

.nav-button.answered {
  border-color: #E6A23C;
  color: #E6A23C;
}

.question-content {
  flex: 1;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #EBEEF5;
}

.question-type-badge {
  display: flex;
  align-items: center;
}

.question-number {
  color: #606266;
  font-size: 14px;
  margin-left: 12px;
}

.question-weight {
  color: #606266;
  font-size: 14px;
  font-weight: 500;
}

.question-body {
  padding: 0 10px;
}

.question-title h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  line-height: 1.5;
}

.question-description {
  margin-bottom: 20px;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.answer-section {
  margin-bottom: 20px;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  margin-bottom: 12px;
}

.text-answer {
  width: 100%;
}

.score-section {
  margin-top: 24px;
  padding: 16px;
  background-color: #F8F9FA;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.03);
}

.score-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score-display {
  display: flex;
  align-items: baseline;
}

.score-label {
  font-weight: 500;
  margin-right: 8px;
  font-size: 16px;
}

.score-value {
  font-size: 24px;
  font-weight: 700;
}

.answer-analysis {
  margin-bottom: 16px;
}

.answer-analysis h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.answer-content {
  background-color: #fff;
  padding: 12px;
  border-radius: 4px;
  white-space: pre-line;
  line-height: 1.6;
  border: 1px solid #EBEEF5;
}

.feedback h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.feedback-content {
  background-color: #fff;
  padding: 12px;
  border-radius: 4px;
  white-space: pre-line;
  line-height: 1.6;
  border: 1px solid #EBEEF5;
  color: #606266;
}

.question-actions {
  margin-top: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.assignment-summary {
  margin-top: 30px;
  padding-top: 16px;
  border-top: 1px solid #EBEEF5;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.summary-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.summary-stats {
  display: flex;
  gap: 16px;
}

.stat-card {
  background-color: #F8F9FA;
  border-radius: 4px;
  padding: 16px;
  flex: 1;
  text-align: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.03);
}

.stat-label {
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .question-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .question-actions {
    flex-direction: column;
    gap: 10px;
  }
  
  .summary-stats {
    flex-direction: column;
    gap: 10px;
  }
}
</style> 