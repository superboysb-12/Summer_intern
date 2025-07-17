package com.XuebaoMaster.backend.QuestionGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "question_generator")
public class QuestionGenerator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query", nullable = false)
    private String query;

    @Column(name = "rag_id")
    private Long ragId;

    @Column(name = "rag_name")
    private String ragName;

    @Column(name = "question_type")
    private String questionType;

    @Column(name = "question_json", columnDefinition = "LONGTEXT")
    private String questionJson;

    @Column(name = "status")
    private String status; // PENDING, COMPLETED, FAILED

    @Column(name = "status_message")
    private String statusMessage;

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

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getRagId() {
        return ragId;
    }

    public void setRagId(Long ragId) {
        this.ragId = ragId;
    }

    public String getRagName() {
        return ragName;
    }

    public void setRagName(String ragName) {
        this.ragName = ragName;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getQuestionJson() {
        return questionJson;
    }

    public void setQuestionJson(String questionJson) {
        this.questionJson = questionJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
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