package com.example.application.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.application.R;
import com.example.application.adapter.ChatAdapter;
import com.example.application.model.ChatMessage;
import com.example.application.utils.ChatManager;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private EditText messageInput;
    private ImageView sendButton;
    private ImageView microphoneButton;
    private ImageView backButton;
    private ImageView volumeButton;
    private ImageView exportButton;
    
    private List<ChatMessage> messageList;
    private ChatManager chatManager;
    private boolean isWaitingForResponse = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        
        // 初始化ChatManager
        chatManager = new ChatManager(getContext());
        
        // 添加欢迎消息
        addWelcomeMessage();
        
        return view;
    }
    
    private void initViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        microphoneButton = view.findViewById(R.id.microphoneButton);
        backButton = view.findViewById(R.id.backButton);
        volumeButton = view.findViewById(R.id.volumeButton);
        exportButton = view.findViewById(R.id.exportButton);
        
        messageList = new ArrayList<>();
    }
    
    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecyclerView.setAdapter(chatAdapter);
    }
    
    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
        
        microphoneButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "语音功能开发中...", Toast.LENGTH_SHORT).show();
        });
        
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        
        volumeButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "语音播放功能开发中...", Toast.LENGTH_SHORT).show();
        });
        
        exportButton.setOnClickListener(v -> {
            exportChatHistory();
        });
    }
    
    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (!message.isEmpty() && !isWaitingForResponse) {
            // 添加用户消息到UI
            ChatMessage userMessage = new ChatMessage(message, true);
            chatAdapter.addMessage(userMessage);
            
            // 清空输入框
            messageInput.setText("");
            
            // 滚动到最新消息
            chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
            
            // 设置等待状态
            isWaitingForResponse = true;
            sendButton.setEnabled(false);
            
            // 显示"正在输入"提示
            showTypingIndicator();
            
            // 发送到API
            chatManager.sendMessage(message, new ChatManager.ChatCallback() {
                @Override
                public void onSuccess(String response) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // 移除"正在输入"提示
                            hideTypingIndicator();
                            
                            // 添加机器人回复
                            ChatMessage botMessage = new ChatMessage(response, false);
                            chatAdapter.addMessage(botMessage);
                            
                            // 滚动到最新消息
                            chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                            
                            // 恢复发送状态
                            isWaitingForResponse = false;
                            sendButton.setEnabled(true);
                        });
                    }
                }
                
                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // 移除"正在输入"提示
                            hideTypingIndicator();
                            
                            // 显示错误消息
                            ChatMessage errorMessage = new ChatMessage("抱歉，发生了错误：" + error, false);
                            chatAdapter.addMessage(errorMessage);
                            
                            // 滚动到最新消息
                            chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                            
                            // 恢复发送状态
                            isWaitingForResponse = false;
                            sendButton.setEnabled(true);
                            
                            Toast.makeText(getContext(), "发送失败：" + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }
    }
    
    private void showTypingIndicator() {
        ChatMessage typingMessage = new ChatMessage("正在输入...", false);
        chatAdapter.addMessage(typingMessage);
        chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
    }
    
    private void hideTypingIndicator() {
        // 移除最后一条"正在输入"消息
        if (!messageList.isEmpty()) {
            ChatMessage lastMessage = messageList.get(messageList.size() - 1);
            if (!lastMessage.isUser() && "正在输入...".equals(lastMessage.getMessage())) {
                messageList.remove(messageList.size() - 1);
                chatAdapter.notifyItemRemoved(messageList.size());
            }
        }
    }
    
    private void addWelcomeMessage() {
        ChatMessage welcomeMessage = new ChatMessage(
            "你好！我是你的AI学习助手，我可以帮助你解答各种学习问题。有什么我可以帮助你的吗？", 
            false
        );
        chatAdapter.addMessage(welcomeMessage);
    }
    
    private void exportChatHistory() {
        if (messageList.isEmpty()) {
            Toast.makeText(getContext(), "没有聊天记录可以导出", Toast.LENGTH_SHORT).show();
            return;
        }
        
        StringBuilder chatText = new StringBuilder();
        chatText.append("聊天记录导出\n");
        chatText.append("================\n\n");
        
        for (ChatMessage message : messageList) {
            if (message.isUser()) {
                chatText.append("我: ").append(message.getMessage()).append("\n\n");
            } else {
                chatText.append("AI: ").append(message.getMessage()).append("\n\n");
            }
        }
        
        // 这里可以实现保存到文件或分享功能
        Toast.makeText(getContext(), "聊天记录已准备好，导出功能待完善", Toast.LENGTH_SHORT).show();
    }
    
    public void clearChatHistory() {
        messageList.clear();
        chatAdapter.notifyDataSetChanged();
        chatManager.clearHistory();
        addWelcomeMessage();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 初始化对话相关的UI组件
    }
}
