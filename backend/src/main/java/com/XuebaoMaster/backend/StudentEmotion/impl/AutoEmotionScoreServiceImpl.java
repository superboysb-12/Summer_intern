package com.XuebaoMaster.backend.StudentEmotion.impl;

import com.XuebaoMaster.backend.DeepSeekChat.ChatMessage;
import com.XuebaoMaster.backend.DeepSeekChat.ChatMessageEntity;
import com.XuebaoMaster.backend.DeepSeekChat.ChatConversation;
import com.XuebaoMaster.backend.DeepSeekChat.ChatConversationService;
import com.XuebaoMaster.backend.DeepSeekChat.ChatMessageService;
import com.XuebaoMaster.backend.DeepSeekChat.DeepSeekChatRequest;
import com.XuebaoMaster.backend.DeepSeekChat.DeepSeekChatResponse;
import com.XuebaoMaster.backend.DeepSeekChat.DeepSeekChatService;
import com.XuebaoMaster.backend.LoginRecord.LoginRecord;
import com.XuebaoMaster.backend.LoginRecord.LoginRecordService;
import com.XuebaoMaster.backend.StudentEmotion.AutoEmotionScoreService;
import com.XuebaoMaster.backend.StudentEmotion.StudentEmotionService;
import com.XuebaoMaster.backend.StudyDuration.StudyDuration;
import com.XuebaoMaster.backend.StudyDuration.StudyDurationService;
import com.XuebaoMaster.backend.User.User;
import com.XuebaoMaster.backend.User.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AutoEmotionScoreServiceImpl implements AutoEmotionScoreService {

    private static final Logger logger = LoggerFactory.getLogger(AutoEmotionScoreServiceImpl.class);

    @Autowired
    private LoginRecordService loginRecordService;

    @Autowired
    private StudyDurationService studyDurationService;

    @Autowired
    private DeepSeekChatService deepSeekChatService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatConversationService chatConversationService;

    @Autowired
    private StudentEmotionService studentEmotionService;

    @Autowired
    private UserService userService;

    @Override
    public void calculateDailyEmotionScores() {
        logger.info("开始计算所有用户的每日情绪评分");

        // 获取所有活跃用户
        List<User> activeUsers = getActiveUsers();

        for (User user : activeUsers) {
            try {
                // 计算总分
                int emotionScore = calculateUserEmotionScore(user);
                logger.info("用户 ID: {}, 情绪评分: {}", user.getId(), emotionScore);

                // 创建情绪记录
                studentEmotionService.createStudentEmotion(user.getId(), emotionScore);
            } catch (Exception e) {
                logger.error("计算用户 ID: {} 的情绪评分时出错", user.getId(), e);
            }
        }

        logger.info("每日情绪评分计算完成");
    }

    @Override
    public int calculateUserEmotionScore(User user) {
        // 基础分数
        int baseScore = 50;

        // 1. 计算登录行为得分
        int loginScore = calculateLoginScore(user);

        // 2. 计算学习时长得分
        int studyScore = calculateStudyScore(user);

        // 3. 计算聊天情绪得分（使用DeepSeek）
        int chatScore = calculateChatScore(user);

        logger.debug("用户 ID: {}, 基础分数: {}, 登录得分: {}, 学习得分: {}, 聊天得分: {}",
                user.getId(), baseScore, loginScore, studyScore, chatScore);

        // 综合计算最终得分（确保在0-100范围内）
        int totalScore = baseScore + loginScore + studyScore + chatScore;
        return Math.min(Math.max(totalScore, 0), 100);
    }

    private List<User> getActiveUsers() {
        // 获取今日有活动的用户
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrow = today.plusDays(1);

        // 获取今天有登录记录或学习记录的所有用户
        List<User> allUsers = userService.getAllUsers();

        return allUsers.stream()
                .filter(user -> {
                    // 检查是否有登录记录
                    boolean hasLogin = !loginRecordService.getLoginRecordsByUserIdAndTimeBetween(
                            user.getId(), today, tomorrow).isEmpty();

                    // 检查是否有学习记录
                    boolean hasStudy = !studyDurationService.getStudyDurationsByUserId(user.getId()).isEmpty();

                    return hasLogin || hasStudy;
                })
                .collect(Collectors.toList());
    }

    private int calculateLoginScore(User user) {
        int score = 0;
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrow = today.plusDays(1);

        // 获取今日登录记录
        List<LoginRecord> todayLogins = loginRecordService.getLoginRecordsByUserIdAndTimeBetween(
                user.getId(), today, tomorrow);

        // 登录频率评分
        int loginCount = todayLogins.size();
        if (loginCount >= 1 && loginCount <= 2) {
            score += 10;
        } else if (loginCount >= 3 && loginCount <= 5) {
            score += 15;
        } else if (loginCount > 5) {
            score += 5;
        }

        // 登录规律性分析
        if (isLoginTimeRegular(todayLogins)) {
            score += 10;
        } else if (!todayLogins.isEmpty()) {
            score += 5;
        }

        // 连续学习天数分析
        int consecutiveDays = calculateConsecutiveLoginDays(user.getId());
        if (consecutiveDays >= 7) {
            score += 15;
        } else if (consecutiveDays >= 3) {
            score += 10;
        }

        return score;
    }

    private boolean isLoginTimeRegular(List<LoginRecord> loginRecords) {
        // 如果登录记录少于2条，则无法判断规律性
        if (loginRecords.size() < 2) {
            return false;
        }

        // 提取所有登录时间点
        List<LocalTime> loginTimes = loginRecords.stream()
                .map(LoginRecord::getTime)
                .map(LocalDateTime::toLocalTime)
                .sorted()
                .collect(Collectors.toList());

        // 计算时间间隔的标准差，标准差越小表示规律性越高
        double meanMinutes = loginTimes.stream()
                .mapToInt(time -> time.getHour() * 60 + time.getMinute())
                .average()
                .orElse(0);

        double variance = loginTimes.stream()
                .mapToDouble(time -> {
                    int timeInMinutes = time.getHour() * 60 + time.getMinute();
                    return Math.pow(timeInMinutes - meanMinutes, 2);
                })
                .average()
                .orElse(0);

        // 计算标准差
        double standardDeviation = Math.sqrt(variance);

        // 如果标准差小于120分钟(2小时)，则认为登录时间规律
        return standardDeviation < 120;
    }

    private int calculateConsecutiveLoginDays(Long userId) {
        int consecutiveDays = 0;
        LocalDateTime currentDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        // 从今天开始向前检查每一天是否有登录
        for (int i = 0; i < 30; i++) { // 最多检查30天
            LocalDateTime dayStart = currentDay.minusDays(i);
            LocalDateTime dayEnd = dayStart.plusDays(1);

            List<LoginRecord> dayLogins = loginRecordService.getLoginRecordsByUserIdAndTimeBetween(
                    userId, dayStart, dayEnd);

            if (!dayLogins.isEmpty()) {
                consecutiveDays++;
            } else {
                // 一旦发现没有登录的一天，就中断计数
                break;
            }
        }

        return consecutiveDays;
    }

    private int calculateStudyScore(User user) {
        int score = 0;
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrow = today.plusDays(1);

        // 获取今日学习记录
        List<StudyDuration> todayStudy = studyDurationService.getStudyDurationsByUserId(user.getId()).stream()
                .filter(sd -> sd.getCurrentTimeStamp().isAfter(today) && sd.getCurrentTimeStamp().isBefore(tomorrow))
                .collect(Collectors.toList());

        // 计算总学习时长（分钟）
        int totalMinutes = todayStudy.stream()
                .mapToInt(StudyDuration::getLength)
                .sum();

        // 学习时长评分
        if (totalMinutes > 0 && totalMinutes < 30) {
            score += 5;
        } else if (totalMinutes >= 30 && totalMinutes <= 120) {
            score += 15;
        } else if (totalMinutes > 120) {
            score += 10;
        }

        // 学习专注度评分
        boolean hasLongSession = todayStudy.stream()
                .anyMatch(sd -> sd.getLength() >= 30);
        if (hasLongSession) {
            score += 10;
        } else if (!todayStudy.isEmpty()) {
            score += 5;
        }

        // 学习时间分布评分
        if (isStudyInPeakHours(todayStudy)) {
            score += 10;
        }
        if (isStudyInLateNight(todayStudy)) {
            score -= 5;
        }

        return score;
    }

    private boolean isStudyInPeakHours(List<StudyDuration> studies) {
        // 定义高效学习时间段: 9:00-11:00, 14:00-17:00
        final LocalTime morningStart = LocalTime.of(9, 0);
        final LocalTime morningEnd = LocalTime.of(11, 0);
        final LocalTime afternoonStart = LocalTime.of(14, 0);
        final LocalTime afternoonEnd = LocalTime.of(17, 0);

        return studies.stream().anyMatch(study -> {
            LocalTime studyTime = study.getCurrentTimeStamp().toLocalTime();
            return (studyTime.isAfter(morningStart) && studyTime.isBefore(morningEnd)) ||
                    (studyTime.isAfter(afternoonStart) && studyTime.isBefore(afternoonEnd));
        });
    }

    private boolean isStudyInLateNight(List<StudyDuration> studies) {
        // 定义深夜学习时间段: 23:00-5:00
        final LocalTime nightStart = LocalTime.of(23, 0);
        final LocalTime nightEnd = LocalTime.of(5, 0);

        return studies.stream().anyMatch(study -> {
            LocalTime studyTime = study.getCurrentTimeStamp().toLocalTime();
            return studyTime.isAfter(nightStart) || studyTime.isBefore(nightEnd);
        });
    }

    private int calculateChatScore(User user) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrow = today.plusDays(1);

        // 获取用户今日的所有会话
        List<ChatConversation> todayConversations = chatConversationService.getUserConversationsBetween(
                user.getId(), today, tomorrow);

        if (todayConversations == null || todayConversations.isEmpty()) {
            return 0; // 无聊天记录，返回0分
        }

        // 提取用户发送的所有消息
        List<String> userMessages = new ArrayList<>();
        for (ChatConversation conversation : todayConversations) {
            if (conversation.getMessages() != null) {
                for (ChatMessageEntity message : conversation.getMessages()) {
                    if ("user".equals(message.getRole())) {
                        userMessages.add(message.getContent());
                    }
                }
            }
        }

        if (userMessages.isEmpty()) {
            return 15; // 无用户消息，返回默认中等分数
        }

        // 准备发送给DeepSeek的消息
        List<Map<String, String>> messages = new ArrayList<>();

        // 添加系统指令
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "分析以下学生聊天内容的情绪状态，给出0-30分的积极性评分，其中0分表示非常消极，30分表示非常积极。只返回分数，不要解释。");
        messages.add(systemMessage);

        // 添加用户聊天记录
        StringBuilder chatContent = new StringBuilder();
        for (String content : userMessages) {
            chatContent.append(content).append("\n\n");
        }

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", chatContent.toString());
        messages.add(userMessage);

        try {
            // 调用DeepSeek API
            DeepSeekChatResponse response = deepSeekChatService.sendMessage(messages);

            // 解析回复，提取分数
            if (response != null && response.getChoices() != null && response.getChoices().length > 0) {
                String content = response.getChoices()[0].getMessage().getContent();
                return extractScoreFromContent(content);
            }
        } catch (Exception e) {
            logger.error("使用DeepSeek分析聊天情绪时出错", e);
        }

        return 15; // 默认中等情绪分数
    }

    private int extractScoreFromContent(String content) {
        // 尝试从DeepSeek响应中提取数字
        try {
            // 去除所有非数字字符
            String numberOnly = content.replaceAll("[^0-9]", "");
            if (!numberOnly.isEmpty()) {
                int score = Integer.parseInt(numberOnly);
                // 确保分数在0-30范围内
                return Math.min(Math.max(score, 0), 30);
            }
        } catch (NumberFormatException e) {
            logger.error("无法从DeepSeek响应中提取分数: {}", content, e);
        }

        // 默认返回中等分数
        return 15;
    }
}