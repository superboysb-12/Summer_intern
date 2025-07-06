package com.XuebaoMaster.backend.KnowledgeDocument;
import java.util.List;
public interface KnowledgeDocumentService {
    KnowledgeDocument createKnowledgeDocument(KnowledgeDocument knowledgeDocument);
    KnowledgeDocument updateKnowledgeDocument(KnowledgeDocument knowledgeDocument);
    void deleteKnowledgeDocument(Long id);
    KnowledgeDocument getKnowledgeDocumentById(Long id);
    List<KnowledgeDocument> getKnowledgeDocumentsByLessonNodeId(Long lessonNodeId);
    List<KnowledgeDocument> getKnowledgeDocumentsByFileType(KnowledgeDocument.FileType fileType);
    List<KnowledgeDocument> getKnowledgeDocumentsByUploadStatus(KnowledgeDocument.UploadStatus uploadStatus);
    List<KnowledgeDocument> getAllKnowledgeDocuments();
    List<KnowledgeDocument> searchKnowledgeDocuments(String keyword);
    KnowledgeDocument updateUploadStatus(Long id, KnowledgeDocument.UploadStatus status);
} 
