package com.XuebaoMaster.backend.KnowledgeDocument;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/knowledge-documents")
public class KnowledgeDocumentController {
    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    /**
     * 创建知识文档
     * 
     * @param knowledgeDocument 知识文档信息
     * @return 创建的知识文档
     */
    @PostMapping
    public ResponseEntity<KnowledgeDocument> createKnowledgeDocument(@RequestBody KnowledgeDocument knowledgeDocument) {
        return ResponseEntity.ok(knowledgeDocumentService.createKnowledgeDocument(knowledgeDocument));
    }

    /**
     * 获取所有知识文档
     * 
     * @return 知识文档列表
     */
    @GetMapping
    public ResponseEntity<List<KnowledgeDocument>> getAllKnowledgeDocuments() {
        return ResponseEntity.ok(knowledgeDocumentService.getAllKnowledgeDocuments());
    }

    /**
     * 根据ID获取知识文档
     * 
     * @param id 知识文档ID
     * @return 知识文档信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeDocument> getKnowledgeDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeDocumentService.getKnowledgeDocumentById(id));
    }

    /**
     * 根据课时节点ID获取知识文档
     * 
     * @param lessonNodeId 课时节点ID
     * @return 知识文档列表
     */
    @GetMapping("/lesson-node/{lessonNodeId}")
    public ResponseEntity<List<KnowledgeDocument>> getKnowledgeDocumentsByLessonNodeId(@PathVariable Long lessonNodeId) {
        return ResponseEntity.ok(knowledgeDocumentService.getKnowledgeDocumentsByLessonNodeId(lessonNodeId));
    }
    
    /**
     * 根据文件类型获取知识文档
     * 
     * @param fileType 文件类型
     * @return 知识文档列表
     */
    @GetMapping("/file-type/{fileType}")
    public ResponseEntity<List<KnowledgeDocument>> getKnowledgeDocumentsByFileType(@PathVariable String fileType) {
        KnowledgeDocument.FileType type = KnowledgeDocument.FileType.valueOf(fileType.toUpperCase());
        return ResponseEntity.ok(knowledgeDocumentService.getKnowledgeDocumentsByFileType(type));
    }
    
    /**
     * 根据上传状态获取知识文档
     * 
     * @param uploadStatus 上传状态
     * @return 知识文档列表
     */
    @GetMapping("/upload-status/{uploadStatus}")
    public ResponseEntity<List<KnowledgeDocument>> getKnowledgeDocumentsByUploadStatus(@PathVariable String uploadStatus) {
        KnowledgeDocument.UploadStatus status = KnowledgeDocument.UploadStatus.valueOf(uploadStatus.toUpperCase());
        return ResponseEntity.ok(knowledgeDocumentService.getKnowledgeDocumentsByUploadStatus(status));
    }

    /**
     * 更新知识文档信息
     * 
     * @param id 知识文档ID
     * @param knowledgeDocument 知识文档信息
     * @return 更新后的知识文档
     */
    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeDocument> updateKnowledgeDocument(@PathVariable Long id, @RequestBody KnowledgeDocument knowledgeDocument) {
        knowledgeDocument.setId(id);
        return ResponseEntity.ok(knowledgeDocumentService.updateKnowledgeDocument(knowledgeDocument));
    }
    
    /**
     * 更新知识文档上传状态
     * 
     * @param id 知识文档ID
     * @param status 上传状态
     * @return 更新后的知识文档
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<KnowledgeDocument> updateUploadStatus(@PathVariable Long id, @RequestParam String status) {
        KnowledgeDocument.UploadStatus uploadStatus = KnowledgeDocument.UploadStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(knowledgeDocumentService.updateUploadStatus(id, uploadStatus));
    }

    /**
     * 删除知识文档
     * 
     * @param id 知识文档ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledgeDocument(@PathVariable Long id) {
        knowledgeDocumentService.deleteKnowledgeDocument(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 搜索知识文档
     * 
     * @param keyword 关键词
     * @return 知识文档列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<KnowledgeDocument>> searchKnowledgeDocuments(@RequestParam String keyword) {
        return ResponseEntity.ok(knowledgeDocumentService.searchKnowledgeDocuments(keyword));
    }
} 