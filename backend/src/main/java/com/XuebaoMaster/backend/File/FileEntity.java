package com.XuebaoMaster.backend.File;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
@Entity
@Table(name = "files")
@Data
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String fileName;
    @Column(nullable = false, length = 500)
    private String filePath;
    @Column(nullable = false, length = 100)
    private String mimeType;
    @Column(nullable = true)
    private Long fileSize;
    @Column(nullable = false)
    private boolean isDirectory;
    @Column(nullable = true, length = 500)
    private String parentPath;
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
}
