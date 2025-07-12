package com.XuebaoMaster.backend.DeepSeekChat.impl;

import com.XuebaoMaster.backend.DeepSeekChat.ChatConversation;
import com.XuebaoMaster.backend.DeepSeekChat.ChatConversationRepository;
import com.XuebaoMaster.backend.DeepSeekChat.ChatConversationService;
import com.XuebaoMaster.backend.DeepSeekChat.ChatMessageEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatConversationServiceImpl implements ChatConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ChatConversationServiceImpl.class);

    @Autowired
    private ChatConversationRepository chatConversationRepository;

    @Override
    @Transactional
    public ChatConversation createConversation(Long userId) {
        ChatConversation conversation = new ChatConversation();
        conversation.setUserId(userId);
        return chatConversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public ChatConversation createConversation(Long userId, List<Map<String, String>> initialMessages) {
        ChatConversation conversation = new ChatConversation();
        conversation.setUserId(userId);

        // 添加初始消息
        if (initialMessages != null && !initialMessages.isEmpty()) {
            for (Map<String, String> messageMap : initialMessages) {
                String role = messageMap.getOrDefault("role", "user");
                String content = messageMap.getOrDefault("content", "");

                ChatMessageEntity message = new ChatMessageEntity();
                message.setUserId(userId);
                message.setRole(role);
                message.setContent(content);

                conversation.addMessage(message);
            }
        }

        conversation.generateTitle();
        return chatConversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public ChatConversation addMessagesToConversation(Long conversationId, List<Map<String, String>> messages) {
        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + conversationId));

        for (Map<String, String> messageMap : messages) {
            String role = messageMap.getOrDefault("role", "user");
            String content = messageMap.getOrDefault("content", "");

            ChatMessageEntity message = new ChatMessageEntity();
            message.setUserId(conversation.getUserId());
            message.setRole(role);
            message.setContent(content);

            conversation.addMessage(message);
        }

        return chatConversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public ChatConversation addMessageToConversation(Long conversationId, String role, String content) {
        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + conversationId));

        ChatMessageEntity message = new ChatMessageEntity();
        message.setUserId(conversation.getUserId());
        message.setRole(role);
        message.setContent(content);

        conversation.addMessage(message);

        return chatConversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatConversation getConversationWithMessages(Long conversationId) {
        return chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + conversationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatConversation> getUserConversations(Long userId) {
        return chatConversationRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatConversation> getRecentUserConversations(Long userId, int count) {
        return chatConversationRepository.findRecentByUserId(userId, PageRequest.of(0, count));
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId) {
        chatConversationRepository.deleteById(conversationId);
    }

    @Override
    @Transactional
    public ChatConversation updateConversationTitle(Long conversationId, String title) {
        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + conversationId));

        conversation.setTitle(title);
        return chatConversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatConversation> getUserConversationsBetween(Long userId, LocalDateTime startTime,
            LocalDateTime endTime) {
        return chatConversationRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }
}