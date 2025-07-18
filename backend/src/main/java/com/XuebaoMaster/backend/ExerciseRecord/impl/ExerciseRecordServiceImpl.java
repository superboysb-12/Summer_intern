package com.XuebaoMaster.backend.ExerciseRecord.impl;

import com.XuebaoMaster.backend.DeepSeekChat.DeepSeekChatResponse;
import com.XuebaoMaster.backend.DeepSeekChat.DeepSeekChatService;
import com.XuebaoMaster.backend.ExerciseRecord.ExerciseRecord;
import com.XuebaoMaster.backend.ExerciseRecord.ExerciseRecordRepository;
import com.XuebaoMaster.backend.ExerciseRecord.ExerciseRecordService;
import com.XuebaoMaster.backend.QuestionGenerator.QuestionGenerator;
import com.XuebaoMaster.backend.QuestionGenerator.QuestionGeneratorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExerciseRecordServiceImpl implements ExerciseRecordService {

    private static final Logger logger = LoggerFactory.getLogger(ExerciseRecordServiceImpl.class);

    @Autowired
    private ExerciseRecordRepository exerciseRecordRepository;

    @Autowired
    private QuestionGeneratorService questionGeneratorService;

    @Autowired
    private DeepSeekChatService deepSeekChatService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ExerciseRecord saveExerciseRecord(ExerciseRecord exerciseRecord) {
        return exerciseRecordRepository.save(exerciseRecord);
    }

    @Override
    public ExerciseRecord submitAnswer(Long studentId, Long questionId, Long homeworkId, String answerContent) {
        // 获取问题信息
        Optional<QuestionGenerator> questionOpt = questionGeneratorService.getQuestionGeneratorById(questionId);
        if (questionOpt.isEmpty()) {
            throw new IllegalArgumentException("问题不存在: " + questionId);
        }

        QuestionGenerator question = questionOpt.get();

        // 创建练习记录
        ExerciseRecord record = new ExerciseRecord();
        record.setStudentId(studentId);
        record.setQuestionId(questionId);
        record.setHomeworkId(homeworkId);
        record.setQuestionType(question.getQuestionType());
        record.setAnswerContent(answerContent);
        record.setStatus("SUBMITTED");

        // 保存记录
        record = exerciseRecordRepository.save(record);

        // 如果是选择题或填空题，直接进行评分
        if ("选择题".equals(question.getQuestionType()) || "填空题".equals(question.getQuestionType())) {
            record = gradeExercise(record.getId());
        }

        return record;
    }

    @Override
    public ExerciseRecord gradeExercise(Long recordId) {
        Optional<ExerciseRecord> recordOpt = exerciseRecordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            throw new IllegalArgumentException("练习记录不存在: " + recordId);
        }

        ExerciseRecord record = recordOpt.get();

        // 获取问题信息
        Optional<QuestionGenerator> questionOpt = questionGeneratorService
                .getQuestionGeneratorById(record.getQuestionId());
        if (questionOpt.isEmpty()) {
            throw new IllegalArgumentException("问题不存在: " + record.getQuestionId());
        }

        QuestionGenerator question = questionOpt.get();
        String questionJson = question.getQuestionJson();

        // 根据不同题目类型，使用不同的评分策略
        switch (record.getQuestionType()) {
            case "选择题":
                gradeMultipleChoiceQuestion(record, questionJson);
                break;

            case "填空题":
                gradeFillInBlankQuestion(record, questionJson);
                break;

            case "问答题":
                gradeEssayQuestion(record, questionJson);
                break;

            case "编程题":
                gradeProgrammingQuestion(record, questionJson);
                break;

            default:
                throw new IllegalArgumentException("不支持的题目类型: " + record.getQuestionType());
        }

        // 保存更新后的记录
        return exerciseRecordRepository.save(record);
    }

    /**
     * 评分选择题
     */
    private void gradeMultipleChoiceQuestion(ExerciseRecord record, String questionJson) {
        try {
            JsonNode questionNode = objectMapper.readTree(questionJson);
            JsonNode choicesNode = questionNode.path("choices");
            String answer = record.getAnswerContent().trim();

            // 寻找正确答案
            String correctAnswer = null;
            for (JsonNode choice : choicesNode) {
                if (choice.path("isCorrect").asBoolean()) {
                    correctAnswer = choice.path("label").asText();
                    break;
                }
            }

            // 评分逻辑：选择题直接比对答案
            if (correctAnswer != null && correctAnswer.equals(answer)) {
                record.setScore(100); // 满分
                record.setFeedback("回答正确");
            } else {
                record.setScore(0); // 不得分
                record.setFeedback("回答错误，正确答案是: " + correctAnswer);
            }

            record.setStatus("GRADED");

        } catch (Exception e) {
            logger.error("评分选择题时出错", e);
            record.setFeedback("评分过程出错: " + e.getMessage());
        }
    }

    /**
     * 评分填空题
     */
    private void gradeFillInBlankQuestion(ExerciseRecord record, String questionJson) {
        try {
            JsonNode questionNode = objectMapper.readTree(questionJson);
            JsonNode blanksNode = questionNode.path("blanks");
            String answer = record.getAnswerContent().trim();

            // 尝试将答案解析为JSON格式
            JsonNode answerNode = objectMapper.readTree(answer);

            // 计算得分
            int totalBlanks = blanksNode.size();
            int correctBlanks = 0;
            StringBuilder feedback = new StringBuilder();

            for (int i = 0; i < totalBlanks && i < answerNode.size(); i++) {
                String userAnswer = answerNode.path(i).asText().trim();
                JsonNode correctAnswersNode = blanksNode.path(i).path("answers");

                boolean isCorrect = false;
                for (JsonNode correctAnswer : correctAnswersNode) {
                    if (userAnswer.equalsIgnoreCase(correctAnswer.asText().trim())) {
                        isCorrect = true;
                        break;
                    }
                }

                if (isCorrect) {
                    correctBlanks++;
                }

                feedback.append("空白").append(i + 1).append(": ")
                        .append(isCorrect ? "正确" : "错误")
                        .append("，正确答案: ").append(correctAnswersNode.get(0).asText())
                        .append("\n");
            }

            // 计算分数（百分比）
            int score = (totalBlanks > 0) ? (correctBlanks * 100 / totalBlanks) : 0;
            record.setScore(score);
            record.setFeedback(feedback.toString());
            record.setStatus("GRADED");

        } catch (Exception e) {
            logger.error("评分填空题时出错", e);
            record.setFeedback("评分过程出错: " + e.getMessage());
        }
    }

    /**
     * 评分问答题（使用大模型进行评分）
     */
    private void gradeEssayQuestion(ExerciseRecord record, String questionJson) {
        try {
            JsonNode questionNode = objectMapper.readTree(questionJson);
            String questionText = questionNode.path("questionText").asText();
            String answerContent = record.getAnswerContent();

            // 构建评分提示词
            String systemPrompt = "你是一个公正、专业的教育评分系统。你需要根据问题和标准答案来评价学生的回答，给出0-100分的分数和详细的反馈意见。\n" +
                    "评分标准：\n" +
                    "1. 内容完整性和准确性（50分）：是否完整回答了问题，内容是否准确\n" +
                    "2. 逻辑性和条理性（30分）：回答的结构是否清晰，逻辑是否连贯\n" +
                    "3. 表达能力和专业性（20分）：用语是否专业，表达是否清晰\n\n" +
                    "请以JSON格式返回评分结果，格式如下：\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"score\": 85,\n" +
                    "  \"feedback\": \"详细的反馈意见\",\n" +
                    "  \"contentScore\": 40,\n" +
                    "  \"logicScore\": 25,\n" +
                    "  \"expressionScore\": 20\n" +
                    "}\n" +
                    "```";

            String userPrompt = "问题：" + questionText + "\n\n" +
                    "标准答案：" + questionNode.path("referenceAnswer").asText() + "\n\n" +
                    "学生回答：" + answerContent + "\n\n" +
                    "请根据上述信息进行评分，必须给出一个0-100的整数分数和详细的反馈。";

            // 调用大模型进行评分
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));

            DeepSeekChatResponse response = deepSeekChatService.sendMessage(messages);

            // 解析评分结果
            if (response != null && response.getChoices() != null && response.getChoices().length > 0) {
                String content = response.getChoices()[0].getMessage().getContent();

                // 提取JSON部分
                int jsonStart = content.indexOf("{");
                int jsonEnd = content.lastIndexOf("}");

                if (jsonStart != -1 && jsonEnd != -1) {
                    String jsonStr = content.substring(jsonStart, jsonEnd + 1);
                    JsonNode resultNode = objectMapper.readTree(jsonStr);

                    // 设置评分结果
                    record.setScore(resultNode.path("score").asInt());
                    record.setFeedback(resultNode.path("feedback").asText());
                    record.setGradingData(jsonStr);
                    record.setStatus("GRADED");
                } else {
                    // 无法解析JSON，使用默认评分
                    record.setScore(60);
                    record.setFeedback("无法自动评分，已给予默认分数。原始反馈：" + content);
                    record.setStatus("GRADED");
                }
            } else {
                record.setFeedback("评分过程出错: 无法获取AI评分结果");
            }

        } catch (Exception e) {
            logger.error("评分问答题时出错", e);
            record.setFeedback("评分过程出错: " + e.getMessage());
        }
    }

    /**
     * 评分编程题（使用大模型进行评分）
     */
    private void gradeProgrammingQuestion(ExerciseRecord record, String questionJson) {
        try {
            JsonNode questionNode = objectMapper.readTree(questionJson);
            String questionText = questionNode.path("questionText").asText();
            String codeTemplate = questionNode.path("codeTemplate").asText();
            String answerContent = record.getAnswerContent();

            // 构建评分提示词
            String systemPrompt = "你是一个专业的编程代码评估系统。你需要评价学生提交的代码，给出0-100分的分数和详细的反馈意见。\n" +
                    "评分标准：\n" +
                    "1. 功能正确性（50分）：代码是否实现了要求的功能\n" +
                    "2. 代码质量（25分）：代码是否简洁、高效、可维护\n" +
                    "3. 代码风格（15分）：命名、注释、格式是否规范\n" +
                    "4. 错误处理（10分）：是否有适当的错误处理机制\n\n" +
                    "请以JSON格式返回评分结果，格式如下：\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"score\": 85,\n" +
                    "  \"feedback\": \"详细的反馈意见\",\n" +
                    "  \"functionalityScore\": 45,\n" +
                    "  \"qualityScore\": 20,\n" +
                    "  \"styleScore\": 12,\n" +
                    "  \"errorHandlingScore\": 8\n" +
                    "}\n" +
                    "```";

            String userPrompt = "题目要求：" + questionText + "\n\n" +
                    "代码模板：\n```\n" + codeTemplate + "\n```\n\n" +
                    "学生提交的代码：\n```\n" + answerContent + "\n```\n\n" +
                    "请评估学生的代码实现，给出一个0-100的整数分数和详细的反馈。";

            // 调用大模型进行评分
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));

            DeepSeekChatResponse response = deepSeekChatService.sendMessage(messages);

            // 解析评分结果
            if (response != null && response.getChoices() != null && response.getChoices().length > 0) {
                String content = response.getChoices()[0].getMessage().getContent();

                // 提取JSON部分
                int jsonStart = content.indexOf("{");
                int jsonEnd = content.lastIndexOf("}");

                if (jsonStart != -1 && jsonEnd != -1) {
                    String jsonStr = content.substring(jsonStart, jsonEnd + 1);
                    JsonNode resultNode = objectMapper.readTree(jsonStr);

                    // 设置评分结果
                    record.setScore(resultNode.path("score").asInt());
                    record.setFeedback(resultNode.path("feedback").asText());
                    record.setGradingData(jsonStr);
                    record.setStatus("GRADED");
                } else {
                    // 无法解析JSON，使用默认评分
                    record.setScore(60);
                    record.setFeedback("无法自动评分，已给予默认分数。原始反馈：" + content);
                    record.setStatus("GRADED");
                }
            } else {
                record.setFeedback("评分过程出错: 无法获取AI评分结果");
            }

        } catch (Exception e) {
            logger.error("评分编程题时出错", e);
            record.setFeedback("评分过程出错: " + e.getMessage());
        }
    }

    @Override
    public Optional<ExerciseRecord> getExerciseRecordById(Long id) {
        return exerciseRecordRepository.findById(id);
    }

    @Override
    public List<ExerciseRecord> getExerciseRecordsByStudentId(Long studentId) {
        return exerciseRecordRepository.findByStudentId(studentId);
    }

    @Override
    public List<ExerciseRecord> getExerciseRecordsByHomeworkId(Long homeworkId) {
        return exerciseRecordRepository.findByHomeworkId(homeworkId);
    }

    @Override
    public List<ExerciseRecord> getExerciseRecordsByQuestionId(Long questionId) {
        return exerciseRecordRepository.findByQuestionId(questionId);
    }

    @Override
    public Map<String, Object> getStudentCourseStatistics(Long studentId, Long courseId) {
        Map<String, Object> statistics = new HashMap<>();

        // 获取该学生在该课程下的所有练习记录
        List<ExerciseRecord> records = exerciseRecordRepository.findByStudentIdAndCourseId(studentId, courseId);

        // 已完成的练习数量
        long completedCount = records.stream().filter(r -> "GRADED".equals(r.getStatus())).count();

        // 平均分
        Double averageScore = exerciseRecordRepository.getStudentCourseAverageScore(studentId, courseId);

        // 按题目类型统计
        Map<String, Long> typeCount = new HashMap<>();
        Map<String, Double> typeScores = new HashMap<>();

        for (ExerciseRecord record : records) {
            if ("GRADED".equals(record.getStatus()) && record.getScore() != null) {
                String type = record.getQuestionType();

                typeCount.put(type, typeCount.getOrDefault(type, 0L) + 1);

                Double currentSum = typeScores.getOrDefault(type, 0.0);
                typeScores.put(type, currentSum + record.getScore());
            }
        }

        // 计算每种题型的平均分
        Map<String, Double> typeAverages = new HashMap<>();
        for (String type : typeCount.keySet()) {
            double average = typeScores.get(type) / typeCount.get(type);
            typeAverages.put(type, average);
        }

        // 组装统计数据
        statistics.put("studentId", studentId);
        statistics.put("courseId", courseId);
        statistics.put("totalExercises", records.size());
        statistics.put("completedExercises", completedCount);
        statistics.put("averageScore", averageScore != null ? averageScore : 0);
        statistics.put("typeStatistics", typeAverages);

        return statistics;
    }

    @Override
    public Map<String, Object> getClassHomeworkStatistics(Long classId, Long homeworkId) {
        Map<String, Object> statistics = new HashMap<>();

        // 获取该班级在该作业下的所有练习记录
        List<ExerciseRecord> records = exerciseRecordRepository.findByHomeworkIdAndClassId(homeworkId, classId);

        // 已完成的学生人数
        Long completedStudents = exerciseRecordRepository.countStudentsCompletedHomework(homeworkId, classId);

        // 平均分
        Double averageScore = exerciseRecordRepository.getClassHomeworkAverageScore(homeworkId, classId);

        // 计算分数分布
        Map<String, Integer> scoreDistribution = new HashMap<>();
        scoreDistribution.put("90-100", 0);
        scoreDistribution.put("80-89", 0);
        scoreDistribution.put("70-79", 0);
        scoreDistribution.put("60-69", 0);
        scoreDistribution.put("0-59", 0);

        for (ExerciseRecord record : records) {
            if ("GRADED".equals(record.getStatus()) && record.getScore() != null) {
                int score = record.getScore();

                if (score >= 90) {
                    scoreDistribution.put("90-100", scoreDistribution.get("90-100") + 1);
                } else if (score >= 80) {
                    scoreDistribution.put("80-89", scoreDistribution.get("80-89") + 1);
                } else if (score >= 70) {
                    scoreDistribution.put("70-79", scoreDistribution.get("70-79") + 1);
                } else if (score >= 60) {
                    scoreDistribution.put("60-69", scoreDistribution.get("60-69") + 1);
                } else {
                    scoreDistribution.put("0-59", scoreDistribution.get("0-59") + 1);
                }
            }
        }

        // 组装统计数据
        statistics.put("classId", classId);
        statistics.put("homeworkId", homeworkId);
        statistics.put("totalStudents", completedStudents);
        statistics.put("averageScore", averageScore != null ? averageScore : 0);
        statistics.put("scoreDistribution", scoreDistribution);

        return statistics;
    }

    @Override
    public Map<String, Object> getCourseStatistics(Long courseId) {
        Map<String, Object> statistics = new HashMap<>();

        // 获取该课程下的所有练习记录
        List<ExerciseRecord> records = exerciseRecordRepository.findByCourseId(courseId);

        // 计算题型分布
        Map<String, Long> typeCount = new HashMap<>();
        Map<String, Double> typeScores = new HashMap<>();

        for (ExerciseRecord record : records) {
            String type = record.getQuestionType();

            typeCount.put(type, typeCount.getOrDefault(type, 0L) + 1);

            if ("GRADED".equals(record.getStatus()) && record.getScore() != null) {
                Double currentSum = typeScores.getOrDefault(type, 0.0);
                typeScores.put(type, currentSum + record.getScore());
            }
        }

        // 计算每种题型的平均分
        Map<String, Double> typeAverages = new HashMap<>();
        for (String type : typeCount.keySet()) {
            double average = typeScores.getOrDefault(type, 0.0) / typeCount.get(type);
            typeAverages.put(type, average);
        }

        // 计算总平均分
        Double courseAverageScore = exerciseRecordRepository.getCourseAverageScore(courseId);

        // 组装统计数据
        statistics.put("courseId", courseId);
        statistics.put("totalExercises", records.size());
        statistics.put("typeDistribution", typeCount);
        statistics.put("typeAverageScores", typeAverages);
        statistics.put("courseAverageScore", courseAverageScore != null ? courseAverageScore : 0);

        return statistics;
    }
}