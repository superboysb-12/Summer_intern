package com.XuebaoMaster.backend.KnowledgeDocument;

import com.XuebaoMaster.backend.LessonNode.LessonNode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "knowledge_documents")
@Data
public class KnowledgeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lesson_node_id", nullable = false)
    private LessonNode lessonNode;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum FileType {
        PDF, PPT, DOC, DOCX, MP4
    }

    public enum UploadStatus {
        UPLOADING, COMPLETED, FAILED
    }
} 