package com.XuebaoMaster.backend.DeepSeekChat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 聊天会话服务接口
 */
public interface ChatConversationService {

    /**
     * 创建新会话
     * 
     * @param userId 用户ID
     * @return 创建的会话
     */
    ChatConversation createConversation(Long userId);

    /**
     * 创建新会话并添加初始消息
     * 
     * @param userId          用户ID
     * @param initialMessages 初始消息
     * @return 创建的会话
     */
    ChatConversation createConversation(Long userId, List<Map<String, String>> initialMessages);

    /**
     * 向会话添加消息
     * 
     * @param conversationId 会话ID
     * @param messages       消息列表
     * @return 更新后的会话
     */
    ChatConversation addMessagesToConversation(Long conversationId, List<Map<String, String>> messages);

    /**
     * 添加单条消息到会话
     * 
     * @param conversationId 会话ID
     * @param role           角色
     * @param content        内容
     * @return 更新后的会话
     */
    ChatConversation addMessageToConversation(Long conversationId, String role, String content);

    /**
     * 获取会话详情（包含消息）
     * 
     * @param conversationId 会话ID
     * @return 会话详情
     */
    ChatConversation getConversationWithMessages(Long conversationId);

    /**
     * 获取用户的所有会话
     * 
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatConversation> getUserConversations(Long userId);

    /**
     * 获取用户的最近会话
     * 
     * @param userId 用户ID
     * @param count  数量
     * @return 会话列表
     */
    List<ChatConversation> getRecentUserConversations(Long userId, int count);

    /**
     * 删除会话
     * 
     * @param conversationId 会话ID
     */
    void deleteConversation(Long conversationId);

    /**
     * 更新会话标题
     * 
     * @param conversationId 会话ID
     * @param title          新标题
     * @return 更新后的会话
     */
    ChatConversation updateConversationTitle(Long conversationId, String title);

    /**
     * 获取用户最近一段时间内的会话
     * 
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 会话列表
     */
    List<ChatConversation> getUserConversationsBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
}