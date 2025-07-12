package com.XuebaoMaster.backend.DeepSeekChat.impl;

import com.XuebaoMaster.backend.DeepSeekChat.ChatMessageEntity;
import com.XuebaoMaster.backend.DeepSeekChat.ChatMessageRepository;
import com.XuebaoMaster.backend.DeepSeekChat.ChatMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Override
    public ChatMessageEntity saveChatMessage(Long userId, String content, String role) {
        try {
            ChatMessageEntity chatMessage = new ChatMessageEntity();
            chatMessage.setUserId(userId);
            chatMessage.setContent(content);
            chatMessage.setRole(role);
            return chatMessageRepository.save(chatMessage);
        } catch (Exception e) {
            logger.error("保存聊天消息时出错", e);
            throw e;
        }
    }

    @Override
    public List<ChatMessageEntity> saveChatMessages(Long userId, List<Map<String, String>> messages) {
        try {
            List<ChatMessageEntity> chatMessages = new ArrayList<>();

            for (Map<String, String> message : messages) {
                String role = message.getOrDefault("role", "user");
                String content = message.getOrDefault("content", "");

                ChatMessageEntity chatMessage = new ChatMessageEntity();
                chatMessage.setUserId(userId);
                chatMessage.setContent(content);
                chatMessage.setRole(role);

                chatMessages.add(chatMessage);
            }

            return chatMessageRepository.saveAll(chatMessages);
        } catch (Exception e) {
            logger.error("保存多条聊天消息时出错", e);
            throw e;
        }
    }

    @Override
    public ChatMessageEntity getChatMessageById(Long id) {
        Optional<ChatMessageEntity> chatMessage = chatMessageRepository.findById(id);
        return chatMessage.orElseThrow(() -> new RuntimeException("聊天消息不存在"));
    }

    @Override
    public List<ChatMessageEntity> getChatMessagesByUserId(Long userId) {
        return chatMessageRepository.findByUserId(userId);
    }

    @Override
    public List<ChatMessageEntity> getChatMessagesByUserIdAndTimeBetween(Long userId, LocalDateTime startTime,
            LocalDateTime endTime) {
        return chatMessageRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }

    @Override
    public List<ChatMessageEntity> getAllChatMessages() {
        return chatMessageRepository.findAll();
    }

    @Override
    public void deleteChatMessage(Long id) {
        chatMessageRepository.deleteById(id);
    }
}