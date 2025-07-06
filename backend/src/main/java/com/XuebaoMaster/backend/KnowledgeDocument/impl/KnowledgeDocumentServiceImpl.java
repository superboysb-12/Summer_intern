package com.XuebaoMaster.backend.KnowledgeDocument.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.KnowledgeDocument.KnowledgeDocument;
import com.XuebaoMaster.backend.KnowledgeDocument.KnowledgeDocumentRepository;
import com.XuebaoMaster.backend.KnowledgeDocument.KnowledgeDocumentService;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {
    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Override
    public KnowledgeDocument createKnowledgeDocument(KnowledgeDocument knowledgeDocument) {
        return knowledgeDocumentRepository.save(knowledgeDocument);
    }
    @Override
    public KnowledgeDocument updateKnowledgeDocument(KnowledgeDocument knowledgeDocument) {
        KnowledgeDocument existingDocument = knowledgeDocumentRepository.findById(knowledgeDocument.getId())
                .orElseThrow(() -> new RuntimeException("Knowledge document not found"));
        existingDocument.setLessonNode(knowledgeDocument.getLessonNode());
        existingDocument.setFileName(knowledgeDocument.getFileName());
        existingDocument.setFilePath(knowledgeDocument.getFilePath());
        existingDocument.setFileType(knowledgeDocument.getFileType());
        existingDocument.setFileSize(knowledgeDocument.getFileSize());
        existingDocument.setUploadStatus(knowledgeDocument.getUploadStatus());
        return knowledgeDocumentRepository.save(existingDocument);
    }
    @Override
    public void deleteKnowledgeDocument(Long id) {
        knowledgeDocumentRepository.deleteById(id);
    }
    @Override
    public KnowledgeDocument getKnowledgeDocumentById(Long id) {
        return knowledgeDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Knowledge document not found"));
    }
    @Override
    public List<KnowledgeDocument> getKnowledgeDocumentsByLessonNodeId(Long lessonNodeId) {
        return knowledgeDocumentRepository.findByLessonNodeId(lessonNodeId);
    }
    @Override
    public List<KnowledgeDocument> getKnowledgeDocumentsByFileType(KnowledgeDocument.FileType fileType) {
        return knowledgeDocumentRepository.findByFileType(fileType);
    }
    @Override
    public List<KnowledgeDocument> getKnowledgeDocumentsByUploadStatus(KnowledgeDocument.UploadStatus uploadStatus) {
        return knowledgeDocumentRepository.findByUploadStatus(uploadStatus);
    }
    @Override
    public List<KnowledgeDocument> getAllKnowledgeDocuments() {
        return knowledgeDocumentRepository.findAll();
    }
    @Override
    public List<KnowledgeDocument> searchKnowledgeDocuments(String keyword) {
        return knowledgeDocumentRepository.findByFileNameContaining(keyword);
    }
    @Override
    public KnowledgeDocument updateUploadStatus(Long id, KnowledgeDocument.UploadStatus status) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Knowledge document not found"));
        document.setUploadStatus(status);
        return knowledgeDocumentRepository.save(document);
    }
} 
