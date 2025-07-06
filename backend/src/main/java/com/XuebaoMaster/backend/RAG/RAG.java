package com.XuebaoMaster.backend.RAG;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "rag")
public class RAG {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, length = 1024)
    private String knowledgeGraphPath;
    @Column(nullable = false, length = 1024)
    private String ragPath;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RAGStatus status = RAGStatus.PENDING;
    @Column(nullable = true, length = 1024)
    private String statusMessage;
    @Column(nullable = true)
    private LocalDateTime createdAt;
    @Column(nullable = true)
    private LocalDateTime updatedAt;
    public enum RAGStatus {
        PENDING, 
        GENERATING, 
        COMPLETED, 
        FAILED 
    }
    public RAG() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    public RAG(String name, String knowledgeGraphPath, String ragPath) {
        this.name = name;
        this.knowledgeGraphPath = knowledgeGraphPath;
        this.ragPath = ragPath;
        this.status = RAGStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getKnowledgeGraphPath() {
        return knowledgeGraphPath;
    }
    public void setKnowledgeGraphPath(String knowledgeGraphPath) {
        this.knowledgeGraphPath = knowledgeGraphPath;
    }
    public String getRagPath() {
        return ragPath;
    }
    public void setRagPath(String ragPath) {
        this.ragPath = ragPath;
    }
    public RAGStatus getStatus() {
        return status;
    }
    public void setStatus(RAGStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    public String getStatusMessage() {
        return statusMessage;
    }
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        this.updatedAt = LocalDateTime.now();
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
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
