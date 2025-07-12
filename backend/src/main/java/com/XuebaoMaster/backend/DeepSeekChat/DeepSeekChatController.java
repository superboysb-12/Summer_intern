package com.XuebaoMaster.backend.DeepSeekChat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deepseek")
@CrossOrigin
public class DeepSeekChatController {

    @Autowired
    private DeepSeekChatService deepSeekChatService;

    @Autowired
    private ChatConversationService chatConversationService;

    /**
     * 发送聊天消息并获取AI回复（不保存记录）
     * 
     * @param chatMessage 聊天消息
     * @return AI回复
     */
    @PostMapping("/chat")
    public ResponseEntity<DeepSeekChatResponse> chat(@RequestBody ChatMessage chatMessage) {
        DeepSeekChatResponse response = deepSeekChatService.sendMessage(chatMessage.getMessages());
        return ResponseEntity.ok(response);
    }

    /**
     * 创建新会话并发送首条消息
     * 
     * @param userId      用户ID
     * @param chatMessage 聊天消息
     * @return 包含会话ID和AI回复的响应
     */
    @PostMapping("/conversation/user/{userId}")
    public ResponseEntity<Map<String, Object>> createConversation(
            @PathVariable Long userId,
            @RequestBody ChatMessage chatMessage) {

        // 1. 创建新会话并保存用户消息
        ChatConversation conversation = chatConversationService.createConversation(userId, chatMessage.getMessages());

        // 2. 发送到DeepSeek并获取回复
        DeepSeekChatResponse response = deepSeekChatService.sendMessage(chatMessage.getMessages());

        // 3. 保存AI回复到会话
        if (response.getChoices() != null && response.getChoices().length > 0) {
            DeepSeekChatResponse.Message aiMessage = response.getChoices()[0].getMessage();
            chatConversationService.addMessageToConversation(
                    conversation.getId(), aiMessage.getRole(), aiMessage.getContent());
        }

        // 4. 准备返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("conversation_id", conversation.getId());
        result.put("response", response);

        return ResponseEntity.ok(result);
    }

    /**
     * 向现有会话发送消息
     * 
     * @param conversationId 会话ID
     * @param chatMessage    聊天消息
     * @return AI回复
     */
    @PostMapping("/conversation/{conversationId}/message")
    public ResponseEntity<DeepSeekChatResponse> addMessageToConversation(
            @PathVariable Long conversationId,
            @RequestBody ChatMessage chatMessage) {

        // 1. 获取会话
        ChatConversation conversation = chatConversationService.getConversationWithMessages(conversationId);

        // 2. 添加用户消息
        chatConversationService.addMessagesToConversation(conversationId, chatMessage.getMessages());

        // 3. 准备完整的会话历史消息用于API调用
        List<Map<String, String>> fullHistory = convertConversationToMessages(conversation);
        fullHistory.addAll(chatMessage.getMessages());

        // 4. 发送到DeepSeek并获取回复
        DeepSeekChatResponse response = deepSeekChatService.sendMessage(fullHistory);

        // 5. 保存AI回复
        if (response.getChoices() != null && response.getChoices().length > 0) {
            DeepSeekChatResponse.Message aiMessage = response.getChoices()[0].getMessage();
            chatConversationService.addMessageToConversation(
                    conversation.getId(), aiMessage.getRole(), aiMessage.getContent());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取会话详情（包含所有消息）
     * 
     * @param conversationId 会话ID
     * @return 会话详情
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ChatConversation> getConversation(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatConversationService.getConversationWithMessages(conversationId));
    }

    /**
     * 获取用户的所有会话
     * 
     * @param userId 用户ID
     * @return 会话列表
     */
    @GetMapping("/conversations/user/{userId}")
    public ResponseEntity<List<ChatConversation>> getUserConversations(@PathVariable Long userId) {
        return ResponseEntity.ok(chatConversationService.getUserConversations(userId));
    }

    /**
     * 获取用户的最近会话
     * 
     * @param userId 用户ID
     * @param count  数量
     * @return 会话列表
     */
    @GetMapping("/conversations/user/{userId}/recent")
    public ResponseEntity<List<ChatConversation>> getRecentUserConversations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int count) {
        return ResponseEntity.ok(chatConversationService.getRecentUserConversations(userId, count));
    }

    /**
     * 更新会话标题
     * 
     * @param conversationId 会话ID
     * @param titleRequest   标题请求
     * @return 更新后的会话
     */
    @PutMapping("/conversation/{conversationId}/title")
    public ResponseEntity<ChatConversation> updateConversationTitle(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> titleRequest) {
        String title = titleRequest.get("title");
        return ResponseEntity.ok(chatConversationService.updateConversationTitle(conversationId, title));
    }

    /**
     * 删除会话
     * 
     * @param conversationId 会话ID
     * @return 无内容响应
     */
    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long conversationId) {
        chatConversationService.deleteConversation(conversationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 将会话转换为消息格式
     * 
     * @param conversation 会话
     * @return 消息列表
     */
    private List<Map<String, String>> convertConversationToMessages(ChatConversation conversation) {
        if (conversation.getMessages() == null) {
            return List.of();
        }

        return conversation.getMessages().stream()
                .map(message -> {
                    Map<String, String> messageMap = new HashMap<>();
                    messageMap.put("role", message.getRole());
                    messageMap.put("content", message.getContent());
                    return messageMap;
                })
                .collect(Collectors.toList());
    }
}