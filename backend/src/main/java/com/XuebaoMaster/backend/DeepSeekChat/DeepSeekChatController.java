package com.XuebaoMaster.backend.DeepSeekChat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deepseek")
@CrossOrigin
public class DeepSeekChatController {

    @Autowired
    private DeepSeekChatService deepSeekChatService;

    @PostMapping("/chat")
    public ResponseEntity<DeepSeekChatResponse> chat(@RequestBody ChatMessage chatMessage) {
        DeepSeekChatResponse response = deepSeekChatService.sendMessage(chatMessage.getMessages());
        return ResponseEntity.ok(response);
    }
} 