package com.XuebaoMaster.backend.KnowledgeDocument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    @Query("SELECT k FROM KnowledgeDocument k WHERE k.lessonNode.id = :lessonNodeId")
    List<KnowledgeDocument> findByLessonNodeId(@Param("lessonNodeId") Long lessonNodeId);
    
    List<KnowledgeDocument> findByFileType(KnowledgeDocument.FileType fileType);
    
    List<KnowledgeDocument> findByUploadStatus(KnowledgeDocument.UploadStatus uploadStatus);
    
    List<KnowledgeDocument> findByFileNameContaining(String keyword);
} 