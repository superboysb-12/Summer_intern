<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { ArrowLeft, Check, Close, RefreshRight, QuestionFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'
import { useCounterStore } from '../../stores/counter'

const route = useRoute()
const router = useRouter()
const store = useCounterStore()
const BaseUrl = 'http://localhost:8080/'
const getToken = () => localStorage.getItem('token')
const userInfo = ref(store.getUserInfo())

// 作业ID
const homeworkId = computed(() => route.params.homeworkId)

// 作业信息
const homework = ref(null)
const loading = ref(false)
const homeworkQuestions = ref([])
const currentQuestionIndex = ref(0)

// 学生答案
const studentAnswers = reactive({})

// 评分结果
const gradingResults = ref({})
const submittingAnswer = ref(false)

// 获取作业信息
const getHomeworkInfo = async () => {
  if (!homeworkId.value) return
  
  loading.value = true
  try {
    const response = await axios.get(`${BaseUrl}homeworks/${homeworkId.value}`, {
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

// 获取作业题目
const getHomeworkQuestions = async () => {
  if (!homeworkId.value) return
  
  loading.value = true
  try {
    const response = await axios.get(`${BaseUrl}api/homework-questions/homework/${homeworkId.value}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    if (response.data && response.data.success) {
      homeworkQuestions.value = response.data.homeworkQuestions || []
      
      // 加载每个题目的详细信息
      for (const item of homeworkQuestions.value) {
        await loadQuestionDetails(item)
      }
      
      // 按题目顺序排序
      homeworkQuestions.value.sort((a, b) => a.questionOrder - b.questionOrder)
      
      // 初始化学生答案对象
      homeworkQuestions.value.forEach(q => {
        if (!studentAnswers[q.questionId]) {
          if (q.questionDetails?.questionType === '选择题') {
            studentAnswers[q.questionId] = ''
          } else if (q.questionDetails?.questionType === '判断题') {
            studentAnswers[q.questionId] = null
          } else {
            studentAnswers[q.questionId] = ''
          }
        }
      })
    } else {
      homeworkQuestions.value = []
      ElMessage.warning('获取作业题目失败: ' + (response.data?.message || '未知错误'))
    }
  } catch (error) {
    console.error('获取作业题目失败:', error)
    ElMessage.error('获取作业题目失败: ' + (error.response?.data?.message || error.message))
    homeworkQuestions.value = []
  } finally {
    loading.value = false
  }
}

// 加载题目详情
const loadQuestionDetails = async (homeworkQuestion) => {
  try {
    const response = await axios.get(`${BaseUrl}api/question-generator/${homeworkQuestion.questionId}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    })
    
    if (response.data) {
      // 处理两种可能的响应格式
      const questionData = response.data.success ? response.data : response.data
      homeworkQuestion.questionDetails = questionData
      
      // 解析问题JSON获取题目类型和内容
      if (questionData.questionJson) {
        try {
          // 确保我们处理的是字符串
          const jsonStr = typeof questionData.questionJson === 'string' 
            ? questionData.questionJson 
            : JSON.stringify(questionData.questionJson)
            
          homeworkQuestion.parsedQuestion = JSON.parse(jsonStr)
          console.log('解析后的题目数据:', homeworkQuestion.parsedQuestion)
          
          // 先从questionType或type确定题目类型
          const questionType = questionData.questionType || homeworkQuestion.parsedQuestion.type || '未知类型'
          
          // 根据题型获取题目内容
          switch (questionType) {
            case '选择题':
              homeworkQuestion.questionContent = homeworkQuestion.parsedQuestion.question || '无题目内容'
              homeworkQuestion.questionType = '选择题'
              
              // 确保options是对象形式，如果是数组则转换
              if (Array.isArray(homeworkQuestion.parsedQuestion.options)) {
                const optionsObj = {}
                homeworkQuestion.parsedQuestion.options.forEach((option, index) => {
                  // 使用A, B, C...作为键
                  const key = String.fromCharCode(65 + index) 
                  optionsObj[key] = option
                })
                homeworkQuestion.parsedQuestion.options = optionsObj
              }
              break
              
            case '判断题':
              homeworkQuestion.questionContent = homeworkQuestion.parsedQuestion.statement || '无题目内容'
              homeworkQuestion.questionType = '判断题'
              break
              
            case '问答题':
              homeworkQuestion.questionContent = homeworkQuestion.parsedQuestion.question || '无题目内容'
              homeworkQuestion.questionType = '问答题'
              break
              
            case '编程题':
              homeworkQuestion.questionContent = homeworkQuestion.parsedQuestion.description || 
                                              homeworkQuestion.parsedQuestion.title || 
                                              '无题目内容'
              homeworkQuestion.questionType = '编程题'
              break
              
            default:
              // 尝试从内容推断题目类型
              if (homeworkQuestion.parsedQuestion.options) {
                homeworkQuestion.questionType = '选择题'
                homeworkQuestion.questionContent = homeworkQuestion.parsedQuestion.question || '无题目内容'
                
                // 确保options是对象形式
                if (Array.isArray(homeworkQuestion.parsedQuestion.options)) {
                  const optionsObj = {}
                  homeworkQuestion.parsedQuestion.options.forEach((option, index) => {
                    const key = String.fromCharCode(65 + index)
                    optionsObj[key] = option
                  })
                  homeworkQuestion.parsedQuestion.options = optionsObj
                }
              } else if (homeworkQuestion.parsedQuestion.statement !== undefined) {
                homeworkQuestion.questionType = '判断题'
                homeworkQuestion.questionContent = homeworkQuestion.parsedQuestion.statement || '无题目内容'
              } else if (homeworkQuestion.parsedQuestion.examples || 
                       homeworkQuestion.parsedQuestion.input_format || 
                       homeworkQuestion.parsedQuestion.output_format) {
                homeworkQuestion.questionType = '编程题'
                homeworkQuestion.questionContent = homeworkQuestion.parsedQuestion.description || 
                                                homeworkQuestion.parsedQuestion.title || 
                                                '无题目内容'
              } else {
                homeworkQuestion.questionType = homeworkQuestion.parsedQuestion.type || questionData.questionType || '问答题'
                homeworkQuestion.questionContent = homeworkQuestion.parsedQuestion.question || 
                                                homeworkQuestion.parsedQuestion.content || 
                                                '无题目内容'
              }
          }
          
          // 确保每种题型都有答案字段，即使为空
          if (!homeworkQuestion.parsedQuestion.answer && homeworkQuestion.parsedQuestion.answer !== false) {
            switch (homeworkQuestion.questionType) {
              case '选择题':
                homeworkQuestion.parsedQuestion.answer = ''
                break
              case '判断题':
                homeworkQuestion.parsedQuestion.answer = null
                break
              case '问答题':
                homeworkQuestion.parsedQuestion.answer = ''
                break
              case '编程题':
                homeworkQuestion.parsedQuestion.answer = ''
                homeworkQuestion.parsedQuestion.reference_code = homeworkQuestion.parsedQuestion.reference_code || ''
                break
              default:
                homeworkQuestion.parsedQuestion.answer = ''
            }
          }
          
        } catch (e) {
          console.error('解析题目JSON失败:', e)
          console.error('原始JSON字符串:', questionData.questionJson)
          homeworkQuestion.questionContent = '无法解析题目内容'
          homeworkQuestion.questionType = questionData.questionType || '未知类型'
          
          // 创建一个基本的解析对象以防止进一步的错误
          homeworkQuestion.parsedQuestion = {
            type: questionData.questionType || '未知类型',
            question: '无法解析题目内容',
            answer: null
          }
        }
      } else {
        // 如果没有questionJson字段，尝试使用其他字段
        homeworkQuestion.questionContent = questionData.query || questionData.content || '无题目内容'
        homeworkQuestion.questionType = questionData.questionType || '未知类型'
        
        // 创建一个基本的解析对象
        homeworkQuestion.parsedQuestion = {
          type: questionData.questionType || '未知类型',
          question: questionData.query || questionData.content || '无题目内容',
          answer: null
        }
      }
    }
  } catch (error) {
    console.error(`获取题目 ${homeworkQuestion.questionId} 详情失败:`, error)
    homeworkQuestion.questionContent = '获取题目详情失败'
    homeworkQuestion.questionType = '未知类型'
    homeworkQuestion.parsedQuestion = {
      type: '未知类型',
      question: '获取题目详情失败',
      answer: null
    }
  }
}

// 获取当前题目
const currentQuestion = computed(() => {
  if (homeworkQuestions.value.length === 0) return null
  return homeworkQuestions.value[currentQuestionIndex.value]
})

// 下一题
const nextQuestion = () => {
  if (currentQuestionIndex.value < homeworkQuestions.value.length - 1) {
    currentQuestionIndex.value++
  }
}

// 上一题
const prevQuestion = () => {
  if (currentQuestionIndex.value > 0) {
    currentQuestionIndex.value--
  }
}

// 跳转到指定题目
const goToQuestion = (index) => {
  if (index >= 0 && index < homeworkQuestions.value.length) {
    currentQuestionIndex.value = index
  }
}

// 判断题目是否已回答
const isQuestionAnswered = (questionId) => {
  const answer = studentAnswers[questionId]
  if (answer === null || answer === undefined) return false
  if (typeof answer === 'string' && answer.trim() === '') return false
  return true
}

// 提交当前题目的答案
const submitCurrentAnswer = async () => {
  if (!currentQuestion.value) {
    ElMessage.warning('无法获取当前题目信息')
    return
  }
  
  const questionId = currentQuestion.value.questionId
  const answer = studentAnswers[questionId]
  const questionType = currentQuestion.value.questionType
  
  // 检查答案是否填写
  if ((answer === null || answer === undefined || (typeof answer === 'string' && answer.trim() === '')) && 
      answer !== false) { // 判断题的false值是有效答案
    ElMessage.warning('请先回答题目')
    return Promise.reject(new Error('未填写答案'))
  }
  
  submittingAnswer.value = true
  
  try {
    // 根据题型格式化答案数据
    let formattedAnswer = answer
    
    if (questionType === '选择题') {
      // 选择题只需提交选项标识
      formattedAnswer = String(answer).trim()
    } else if (questionType === '判断题') {
      // 判断题转为字符串
      formattedAnswer = String(answer)
    } else if (questionType === '编程题') {
      // 编程题内容可能较长
      if (!formattedAnswer || formattedAnswer.trim() === '') {
        ElMessage.warning('请输入代码答案')
        submittingAnswer.value = false
        return Promise.reject(new Error('未填写代码答案'))
      }
    }
    
    // 构建提交数据
    const submissionData = {
      studentId: userInfo.value.id,
      questionId: questionId,
      homeworkId: homeworkId.value,
      answerContent: formattedAnswer,
      questionType: questionType
    }
    
    // 发送答案到后端
    try {
      const response = await axios.post(`${BaseUrl}api/exercise-records/submit`, submissionData, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      
      if (response.data && response.data.success) {
        ElMessage.success('答案提交成功')
        
        // 如果已经评分，显示结果
        if (response.data.status === 'GRADED') {
          gradingResults.value[questionId] = {
            score: response.data.score,
            feedback: response.data.feedback
          }
          
          // 显示评分结果
          showGradingResult(questionId)
        } else {
          // 自动评分
          await gradeAnswer(response.data.recordId)
        }
        
        return Promise.resolve() // 返回成功的Promise
      } else {
        const errorMsg = response.data?.message || '未知错误'
        ElMessage.error('答案提交失败: ' + errorMsg)
        console.error('提交答案失败:', errorMsg)
        return Promise.reject(new Error(errorMsg))
      }
    } catch (error) {
      console.error('提交答案失败:', error)
      ElMessage.error('提交答案失败: ' + (error.response?.data?.message || error.message))
      return Promise.reject(error)
    }
  } catch (error) {
    console.error('提交答案过程中发生错误:', error)
    ElMessage.error('提交答案过程中发生错误: ' + error.message)
    return Promise.reject(error)
  } finally {
    submittingAnswer.value = false
  }
}

// 评分答案
const gradeAnswer = async (recordId) => {
  if (!recordId) return
  
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '正在评分中...',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  
  try {
    // 对于选择题和判断题，直接在前端评分
    const question = currentQuestion.value
    const questionId = question.questionId
    const questionType = question.questionType
    const answer = studentAnswers[questionId]
    
    if (questionType === '选择题' || questionType === '判断题') {
      // 获取正确答案
      let correctAnswer = null
      let isCorrect = false
      
      if (question.parsedQuestion && question.parsedQuestion.answer) {
        correctAnswer = question.parsedQuestion.answer
        
        // 确保比较时考虑大小写和空格
        if (questionType === '选择题') {
          isCorrect = String(correctAnswer).trim() === String(answer).trim()
        } else if (questionType === '判断题') {
          // 判断题可能有不同的表示方式，需要统一处理
          const normalizedCorrect = normalizeJudgementAnswer(correctAnswer)
          const normalizedAnswer = normalizeJudgementAnswer(answer)
          isCorrect = normalizedCorrect === normalizedAnswer
        }
        
        // 构建评分结果和反馈信息
        let feedbackMessage = ''
        if (isCorrect) {
          feedbackMessage = '回答正确！'
        } else {
          // 确保正确答案不为null
          const displayAnswer = correctAnswer ? 
            (questionType === '选择题' ? 
              `${correctAnswer}: ${question.parsedQuestion.options[correctAnswer] || ''}` : 
              correctAnswer) : 
            '未提供答案'
          feedbackMessage = `回答错误。正确答案是: ${displayAnswer}`
        }
        
        gradingResults.value[questionId] = {
          score: isCorrect ? 100 : 0,
          feedback: feedbackMessage
        }
      } else {
        // 如果没有正确答案，给予适当的反馈
        gradingResults.value[questionId] = {
          score: 0,
          feedback: '无法评分：题目未提供正确答案'
        }
      }
      
      // 更新后端记录
      await axios.put(`${BaseUrl}api/exercise-records/${recordId}`, {
        score: gradingResults.value[questionId].score,
        feedback: gradingResults.value[questionId].feedback,
        status: 'GRADED'
      }, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
    } else {
      // 对于问答题和编程题，使用DeepSeek API评分
      await gradeWithAI(recordId, questionId, questionType, answer)
    }
    
    // 显示评分结果
    showGradingResult(questionId)
  } catch (error) {
    console.error('评分失败:', error)
    ElMessage.error('评分失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loadingInstance.close()
  }
}

// 判断题答案标准化处理
const normalizeJudgementAnswer = (answer) => {
  if (!answer) return ''
  
  const answerStr = String(answer).trim().toLowerCase()
  
  // 处理各种可能的"正确"表示
  if (['true', 't', '正确', '对', '是', '1'].includes(answerStr)) {
    return '正确'
  }
  
  // 处理各种可能的"错误"表示
  if (['false', 'f', '错误', '错', '不对', '否', '不是', '0'].includes(answerStr)) {
    return '错误'
  }
  
  return answerStr
}

// 使用AI评分
const gradeWithAI = async (recordId, questionId, questionType, answer) => {
  const question = currentQuestion.value
  
  // 构建评分提示词
  const systemPrompt = `你是一个专业的教育评分助手。请根据以下信息对学生的答案进行评分：
  
题目类型：${questionType}
题目内容：${question.questionContent}
${question.parsedQuestion && questionType === '问答题' && question.parsedQuestion.key_points ? 
  `评分关键点：${JSON.stringify(question.parsedQuestion.key_points)}` : ''}
${question.parsedQuestion && questionType === '编程题' && question.parsedQuestion.examples ? 
  `测试用例：${JSON.stringify(question.parsedQuestion.examples)}` : ''}
${question.parsedQuestion && question.parsedQuestion.answer ? 
  `参考答案：${question.parsedQuestion.answer}` : ''}

你的任务是：
1. 对学生的答案进行评分（满分100分）
2. 提供详细的反馈，包括优点和改进建议
3. 以JSON格式返回评分结果

只返回以下JSON格式，不要有任何其他文本：
{
  "score": 分数（0-100的整数）,
  "feedback": "详细的反馈，指出优点和不足"
}`;

  const userMessage = `学生答案：${answer || '(学生未提供答案)'}
  
请对这个${questionType}答案进行评分，并提供详细反馈。`;

  try {
    // 调用DeepSeek API进行评分
    const apiResponse = await axios.post(`${BaseUrl}api/deepseek/chat`, 
      { 
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: userMessage }
        ]
      },
      { headers: { 'Authorization': `Bearer ${getToken()}` } }
    );
    
    if (apiResponse.data && apiResponse.data.choices && apiResponse.data.choices.length > 0) {
      const aiMessage = apiResponse.data.choices[0].message;
      
      // 解析AI返回的JSON
      try {
        // 提取JSON部分
        const jsonMatch = aiMessage.content.match(/\{[\s\S]*\}/);
        const jsonString = jsonMatch ? jsonMatch[0] : aiMessage.content;
        
        const gradingResult = JSON.parse(jsonString);
        
        // 验证结果格式
        if (!gradingResult.score || typeof gradingResult.score !== 'number') {
          gradingResult.score = 0;
        }
        
        // 确保分数在0-100范围内
        gradingResult.score = Math.max(0, Math.min(100, gradingResult.score));
        
        // 确保有反馈
        if (!gradingResult.feedback) {
          gradingResult.feedback = '未能生成有效反馈';
        }
        
        // 保存评分结果
        gradingResults.value[questionId] = gradingResult;
        
        // 更新后端记录
        await axios.put(`${BaseUrl}api/exercise-records/${recordId}`, {
          score: gradingResult.score,
          feedback: gradingResult.feedback,
          status: 'GRADED',
          gradingData: JSON.stringify({
            aiResponse: aiMessage.content,
            questionType: questionType,
            parsedResult: gradingResult
          })
        }, {
          headers: { 'Authorization': `Bearer ${getToken()}` }
        });
      } catch (jsonError) {
        console.error('解析AI评分结果失败:', jsonError);
        
        // 创建一个默认评分结果
        const defaultGradingResult = {
          score: 0,
          feedback: '无法解析AI评分结果，请联系教师手动评分。'
        };
        
        gradingResults.value[questionId] = defaultGradingResult;
        
        // 更新后端记录
        await axios.put(`${BaseUrl}api/exercise-records/${recordId}`, {
          score: defaultGradingResult.score,
          feedback: defaultGradingResult.feedback,
          status: 'GRADED',
          gradingData: JSON.stringify({
            error: 'JSON解析失败',
            rawResponse: aiMessage.content
          })
        }, {
          headers: { 'Authorization': `Bearer ${getToken()}` }
        });
        
        ElMessage.error('解析评分结果失败');
      }
    } else {
      // 处理API响应为空的情况
      const defaultGradingResult = {
        score: 0,
        feedback: 'AI评分服务无响应，请联系教师手动评分。'
      };
      
      gradingResults.value[questionId] = defaultGradingResult;
      
      // 更新后端记录
      await axios.put(`${BaseUrl}api/exercise-records/${recordId}`, {
        score: defaultGradingResult.score,
        feedback: defaultGradingResult.feedback,
        status: 'GRADED'
      }, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      });
    }
  } catch (error) {
    console.error('AI评分失败:', error);
    
    // 创建一个默认评分结果
    const defaultGradingResult = {
      score: 0,
      feedback: `AI评分失败: ${error.message || '未知错误'}，请联系教师手动评分。`
    };
    
    gradingResults.value[questionId] = defaultGradingResult;
    
    // 更新后端记录
    await axios.put(`${BaseUrl}api/exercise-records/${recordId}`, {
      score: defaultGradingResult.score,
      feedback: defaultGradingResult.feedback,
      status: 'GRADED',
      gradingData: JSON.stringify({
        error: error.message || '未知错误'
      })
    }, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    });
    
    ElMessage.error('AI评分失败');
  }
}

// 显示评分结果
const showGradingResult = (questionId) => {
  const result = gradingResults.value[questionId]
  if (!result) return
  
  // 根据分数确定显示样式
  let scoreClass = 'score-normal'
  if (result.score >= 90) {
    scoreClass = 'score-excellent'
  } else if (result.score >= 60) {
    scoreClass = 'score-pass'
  } else {
    scoreClass = 'score-fail'
  }
  
  ElMessageBox.alert(
    `<div class="grading-result">
      <div class="score ${scoreClass}">得分: ${result.score}</div>
      <div class="feedback">${result.feedback}</div>
     </div>`,
    '评分结果',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '确定',
      callback: () => {
        // 如果得分低于60分，可以提供重新作答的选项
        if (result.score < 60) {
          ElMessageBox.confirm(
            '您的得分低于60分，是否要重新作答？',
            '重新作答确认',
            {
              confirmButtonText: '重新作答',
              cancelButtonText: '继续下一题',
              type: 'warning'
            }
          ).then(() => {
            // 用户选择重新作答，不清空答案
          }).catch(() => {
            // 用户选择继续，如果不是最后一题则前进到下一题
            if (currentQuestionIndex.value < homeworkQuestions.value.length - 1) {
              nextQuestion()
            }
          })
        } else {
          // 如果得分合格，且不是最后一题，询问是否继续下一题
          if (currentQuestionIndex.value < homeworkQuestions.value.length - 1) {
            ElMessageBox.confirm(
              '您已完成当前题目，是否继续下一题？',
              '继续确认',
              {
                confirmButtonText: '下一题',
                cancelButtonText: '留在当前题',
                type: 'info'
              }
            ).then(() => {
              nextQuestion()
            }).catch(() => {
              // 用户选择留在当前题，不做操作
            })
          }
        }
      }
    }
  )
}

// 提交全部答案
const submitAllAnswers = async () => {
  // 检查是否所有题目都已回答
  const unansweredQuestions = homeworkQuestions.value.filter(q => !isQuestionAnswered(q.questionId));
  
  if (unansweredQuestions.length > 0) {
    ElMessageBox.confirm(
      `还有 ${unansweredQuestions.length} 道题目未回答，确定要提交吗？`,
      '提示',
      {
        confirmButtonText: '确定提交',
        cancelButtonText: '继续作答',
        type: 'warning'
      }
    ).then(async () => {
      // 用户确认提交
      await submitCurrentAnswer();
      ElMessage.success('所有答案已提交');
      // 可以选择跳转回作业列表
      // router.push('/student-assignments');
    }).catch(() => {
      // 用户取消提交，继续作答
    });
  } else {
    // 所有题目都已回答，直接提交
    await submitCurrentAnswer();
    ElMessage.success('所有答案已提交');
    // 可以选择跳转回作业列表
    // router.push('/student-assignments');
  }
}

// 返回作业列表
const goBack = () => {
  router.push('/student-assignments');
}

// 组件挂载时获取数据
onMounted(() => {
  getHomeworkInfo();
  getHomeworkQuestions();
})
</script>

<template>
  <div class="do-homework-container">
    <!-- 顶部导航 -->
    <div class="top-nav">
      <el-button type="default" :icon="ArrowLeft" @click="goBack">返回作业列表</el-button>
      <h2 v-if="homework">{{ homework.title }}</h2>
      <div class="spacer"></div>
      <el-button type="primary" @click="submitAllAnswers" :loading="submittingAnswer">提交答案</el-button>
    </div>
    
    <!-- 主体内容 -->
    <div class="main-content" v-loading="loading">
      <template v-if="homeworkQuestions.length > 0">
        <!-- 左侧题目导航 -->
        <div class="question-nav">
          <h3>题目列表</h3>
          <div class="question-list">
            <div 
              v-for="(question, index) in homeworkQuestions" 
              :key="question.id"
              class="question-item"
              :class="{
                'active': index === currentQuestionIndex,
                'answered': isQuestionAnswered(question.questionId)
              }"
              @click="goToQuestion(index)"
            >
              <span class="question-number">{{ index + 1 }}</span>
              <span class="question-type">{{ question.questionType }}</span>
              <el-icon v-if="isQuestionAnswered(question.questionId)" class="answered-icon"><Check /></el-icon>
            </div>
          </div>
        </div>
        
        <!-- 右侧题目内容和答题区域 -->
        <div class="question-content">
          <template v-if="currentQuestion">
            <!-- 题目头部 -->
            <div class="question-header">
              <div class="question-info">
                <span class="question-index">第 {{ currentQuestionIndex + 1 }}/{{ homeworkQuestions.length }} 题</span>
                <el-tag>{{ currentQuestion.questionType }}</el-tag>
              </div>
              <div class="question-nav-buttons">
                <el-button :disabled="currentQuestionIndex === 0" @click="prevQuestion">上一题</el-button>
                <el-button :disabled="currentQuestionIndex === homeworkQuestions.length - 1" @click="nextQuestion">下一题</el-button>
              </div>
            </div>
            
            <!-- 题目内容 -->
            <div class="question-body">
              <h3 class="question-title">{{ currentQuestion.questionContent }}</h3>
              
              <!-- 选择题 -->
              <div v-if="currentQuestion.questionType === '选择题' && currentQuestion.parsedQuestion" class="options-container">
                <el-radio-group v-model="studentAnswers[currentQuestion.questionId]">
                  <el-radio 
                    v-for="(option, key) in currentQuestion.parsedQuestion.options" 
                    :key="key"
                    :label="key"
                    class="option-item"
                  >
                    {{ key }}: {{ option }}
                  </el-radio>
                </el-radio-group>
              </div>
              
              <!-- 判断题 -->
              <div v-else-if="currentQuestion.questionType === '判断题'" class="options-container">
                <el-radio-group v-model="studentAnswers[currentQuestion.questionId]">
                  <el-radio :label="'正确'" class="option-item judge-option">
                    <el-icon class="true-icon"><Check /></el-icon> 正确
                  </el-radio>
                  <el-radio :label="'错误'" class="option-item judge-option">
                    <el-icon class="false-icon"><Close /></el-icon> 错误
                  </el-radio>
                </el-radio-group>
              </div>
              
              <!-- 问答题 -->
              <div v-else-if="currentQuestion.questionType === '问答题'" class="answer-container">
                <!-- 显示关键点，如果有的话 -->
                <div v-if="currentQuestion.parsedQuestion && currentQuestion.parsedQuestion.key_points && currentQuestion.parsedQuestion.key_points.length > 0" class="key-points-container">
                  <h4>作答要点：</h4>
                  <ul class="key-points-list">
                    <li v-for="(point, index) in currentQuestion.parsedQuestion.key_points" :key="index">
                      {{ point }}
                    </li>
                  </ul>
                </div>
                
                <el-input
                  v-model="studentAnswers[currentQuestion.questionId]"
                  type="textarea"
                  :rows="8"
                  placeholder="请输入您的答案"
                  resize="both"
                />
              </div>
              
              <!-- 编程题 -->
              <div v-else-if="currentQuestion.questionType === '编程题'" class="answer-container">
                <div v-if="currentQuestion.parsedQuestion" class="programming-question-info">
                  <div v-if="currentQuestion.parsedQuestion.input_format" class="info-section">
                    <h4>输入格式</h4>
                    <pre class="format-block">{{ currentQuestion.parsedQuestion.input_format }}</pre>
                  </div>
                  <div v-if="currentQuestion.parsedQuestion.output_format" class="info-section">
                    <h4>输出格式</h4>
                    <pre class="format-block">{{ currentQuestion.parsedQuestion.output_format }}</pre>
                  </div>
                  <div v-if="currentQuestion.parsedQuestion.examples && currentQuestion.parsedQuestion.examples.length > 0" class="info-section">
                    <h4>示例</h4>
                    <div v-for="(example, index) in currentQuestion.parsedQuestion.examples" :key="index" class="example-item">
                      <div class="example-header">示例 {{ index + 1 }}</div>
                      <div class="example-content">
                        <div class="example-input">
                          <div class="example-label">输入:</div>
                          <pre class="code-block">{{ example.input }}</pre>
                        </div>
                        <div class="example-output">
                          <div class="example-label">输出:</div>
                          <pre class="code-block">{{ example.output }}</pre>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div v-if="currentQuestion.parsedQuestion.solution_approach" class="info-section">
                    <h4>提示</h4>
                    <div class="hint-content">{{ currentQuestion.parsedQuestion.solution_approach }}</div>
                  </div>
                </div>
                
                <el-input
                  v-model="studentAnswers[currentQuestion.questionId]"
                  type="textarea"
                  :rows="12"
                  placeholder="请输入您的代码答案"
                  class="code-editor"
                  :spellcheck="false"
                  resize="both"
                />
              </div>
              
              <!-- 未知题型 -->
              <div v-else class="answer-container">
                <el-alert
                  type="info"
                  show-icon
                  title="未识别的题目类型"
                  description="请按要求作答，系统将保存您的回答"
                  :closable="false"
                  style="margin-bottom: 15px;"
                />
                <el-input
                  v-model="studentAnswers[currentQuestion.questionId]"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入您的答案"
                />
              </div>
            </div>
            
            <!-- 提交按钮 -->
            <div class="question-footer">
              <el-button type="primary" @click="submitCurrentAnswer" :loading="submittingAnswer">
                提交答案
              </el-button>
              <el-button v-if="currentQuestionIndex < homeworkQuestions.length - 1" type="success" @click="submitCurrentAnswer().then(() => nextQuestion())" :loading="submittingAnswer">
                提交并继续
              </el-button>
            </div>
          </template>
          
          <el-empty v-else description="未找到题目" />
        </div>
      </template>
      
      <el-empty v-else-if="!loading" description="该作业暂无题目">
        <el-button type="primary" @click="goBack">返回作业列表</el-button>
      </el-empty>
    </div>
  </div>
</template>

<style scoped>
.do-homework-container {
  padding: 20px;
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
}

.top-nav {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.top-nav h2 {
  margin: 0 20px;
  flex-grow: 1;
}

.spacer {
  flex-grow: 1;
}

.main-content {
  display: flex;
  flex-grow: 1;
  gap: 20px;
  height: 100%;
}

.question-nav {
  width: 200px;
  border-right: 1px solid var(--el-border-color-light);
  padding-right: 15px;
}

.question-nav h3 {
  margin-top: 0;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.question-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 4px;
  cursor: pointer;
  position: relative;
}

.question-item:hover {
  background-color: var(--el-fill-color-light);
}

.question-item.active {
  background-color: var(--el-color-primary-light-9);
  border-left: 3px solid var(--el-color-primary);
}

.question-item.answered {
  background-color: var(--el-color-success-light-9);
}

.question-item.active.answered {
  background-color: var(--el-color-primary-light-8);
}

.question-number {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  background-color: var(--el-color-info-light-8);
  margin-right: 8px;
  font-weight: bold;
}

.question-item.active .question-number {
  background-color: var(--el-color-primary);
  color: white;
}

.question-type {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.answered-icon {
  position: absolute;
  right: 10px;
  color: var(--el-color-success);
}

.question-content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.question-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-index {
  font-weight: bold;
}

.question-nav-buttons {
  display: flex;
  gap: 10px;
}

.question-body {
  flex-grow: 1;
  margin-bottom: 20px;
}

.question-title {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  line-height: 1.5;
}

.options-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.option-item {
  margin-right: 0;
  padding: 10px;
  border-radius: 4px;
  width: 100%;
  box-sizing: border-box;
}

.option-item:hover {
  background-color: var(--el-fill-color-light);
}

.answer-container {
  margin-top: 20px;
}

.programming-question-info {
  margin-bottom: 20px;
  padding: 15px;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
}

.info-section {
  margin-bottom: 15px;
}

.info-section h4 {
  margin-top: 0;
  margin-bottom: 8px;
  color: var(--el-color-primary);
}

.example-item {
  padding: 10px;
  background-color: var(--el-fill-color);
  border-radius: 4px;
  margin-bottom: 10px;
}

.example-input, .example-output {
  margin-bottom: 8px;
}

pre {
  background-color: var(--el-fill-color-darker);
  padding: 8px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 5px 0;
}

.code-editor {
  font-family: monospace;
}

.question-footer {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--el-border-color-light);
  gap: 10px;
}

.grading-result {
  padding: 15px;
}

.score {
  font-size: 18px;
  font-weight: bold;
  color: var(--el-color-primary);
  margin-bottom: 10px;
}

.score-excellent {
  color: var(--el-color-success);
}

.score-pass {
  color: var(--el-color-primary);
}

.score-fail {
  color: var(--el-color-danger);
}

.feedback {
  white-space: pre-line;
}

/* 新增样式 */
.judge-option {
  display: flex;
  align-items: center;
  gap: 5px;
}

.true-icon {
  color: var(--el-color-success);
}

.false-icon {
  color: var(--el-color-danger);
}

.key-points-container {
  margin-bottom: 15px;
  padding: 10px 15px;
  background-color: var(--el-color-info-light-9);
  border-radius: 4px;
}

.key-points-container h4 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 16px;
  color: var(--el-color-info-dark-2);
}

.key-points-list {
  margin: 0;
  padding-left: 20px;
}

.key-points-list li {
  margin-bottom: 5px;
}

.format-block {
  background-color: var(--el-color-info-light-8);
  border-left: 3px solid var(--el-color-primary);
  margin: 10px 0;
}

.example-header {
  font-weight: bold;
  padding: 5px 0;
  color: var(--el-color-primary);
  border-bottom: 1px dashed var(--el-border-color);
  margin-bottom: 10px;
}

.example-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.example-label {
  font-weight: bold;
  margin-bottom: 5px;
}

.code-block {
  background-color: var(--el-color-info-light-9);
  border-left: 3px solid var(--el-color-primary);
}

.hint-content {
  padding: 10px;
  background-color: var(--el-color-success-light-9);
  border-left: 3px solid var(--el-color-success);
  border-radius: 4px;
  font-style: italic;
}

/* 响应式样式调整 */
@media (min-width: 768px) {
  .example-content {
    flex-direction: row;
  }
  
  .example-input, .example-output {
    flex: 1;
  }
}

@media (max-width: 768px) {
  .main-content {
    flex-direction: column;
  }
  
  .question-nav {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-light);
    padding-right: 0;
    padding-bottom: 15px;
    margin-bottom: 15px;
  }
  
  .question-list {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .question-item {
    width: 60px;
  }
  
  .question-type {
    display: none;
  }
}
</style> 