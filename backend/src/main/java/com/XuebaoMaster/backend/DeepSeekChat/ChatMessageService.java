package com.XuebaoMaster.backend.DeepSeekChat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息服务接口
 */
public interface ChatMessageService {

    /**
     * 保存聊天消息
     * 
     * @param userId  用户ID
     * @param content 消息内容
     * @param role    角色（user/assistant）
     * @return 保存的聊天消息
     */
    ChatMessageEntity saveChatMessage(Long userId, String content, String role);

    /**
     * 保存一组聊天消息
     * 
     * @param userId   用户ID
     * @param messages 聊天消息列表
     * @return 保存的聊天消息列表
     */
    List<ChatMessageEntity> saveChatMessages(Long userId, List<Map<String, String>> messages);

    /**
     * 根据ID获取聊天消息
     * 
     * @param id 消息ID
     * @return 聊天消息
     */
    ChatMessageEntity getChatMessageById(Long id);

    /**
     * 根据用户ID获取聊天消息
     * 
     * @param userId 用户ID
     * @return 聊天消息列表
     */
    List<ChatMessageEntity> getChatMessagesByUserId(Long userId);

    /**
     * 根据用户ID和时间范围获取聊天消息
     * 
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 聊天消息列表
     */
    List<ChatMessageEntity> getChatMessagesByUserIdAndTimeBetween(Long userId, LocalDateTime startTime,
            LocalDateTime endTime);

    /**
     * 获取所有聊天消息
     * 
     * @return 所有聊天消息
     */
    List<ChatMessageEntity> getAllChatMessages();

    /**
     * 删除聊天消息
     * 
     * @param id 消息ID
     */
    void deleteChatMessage(Long id);
}