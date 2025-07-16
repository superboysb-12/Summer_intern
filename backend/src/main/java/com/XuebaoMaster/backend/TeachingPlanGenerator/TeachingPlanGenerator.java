package com.XuebaoMaster.backend.TeachingPlanGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "teaching_plan_generator")
public class TeachingPlanGenerator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String prompt;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "status")
    private String status; // PENDING, COMPLETED, FAILED

    @Column(name = "edit_start_time")
    private LocalDateTime editStartTime;

    @Column(name = "edit_end_time")
    private LocalDateTime editEndTime;

    @Column(name = "edit_duration")
    private Long editDuration; // in seconds

    @Column(name = "edit_content", columnDefinition = "LONGTEXT")
    private String editContent;

    @Column(name = "efficiency_index")
    private Double efficiencyIndex;

    @Column(name = "optimization_suggestions", columnDefinition = "TEXT")
    private String optimizationSuggestions;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getEditStartTime() {
        return editStartTime;
    }

    public void setEditStartTime(LocalDateTime editStartTime) {
        this.editStartTime = editStartTime;
    }

    public LocalDateTime getEditEndTime() {
        return editEndTime;
    }

    public void setEditEndTime(LocalDateTime editEndTime) {
        this.editEndTime = editEndTime;
    }

    public Long getEditDuration() {
        return editDuration;
    }

    public void setEditDuration(Long editDuration) {
        this.editDuration = editDuration;
    }

    public String getEditContent() {
        return editContent;
    }

    public void setEditContent(String editContent) {
        this.editContent = editContent;
    }

    public Double getEfficiencyIndex() {
        return efficiencyIndex;
    }

    public void setEfficiencyIndex(Double efficiencyIndex) {
        this.efficiencyIndex = efficiencyIndex;
    }

    public String getOptimizationSuggestions() {
        return optimizationSuggestions;
    }

    public void setOptimizationSuggestions(String optimizationSuggestions) {
        this.optimizationSuggestions = optimizationSuggestions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}