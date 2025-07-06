package com.XuebaoMaster.backend.RAG;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
public interface RAGService {
    RAG saveRAG(RAG rag);
    List<RAG> getAllRAGs();
    Optional<RAG> getRAGById(Long id);
    Optional<RAG> getRAGByName(String name);
    void deleteRAG(Long id);
    RAGResponse generateRAG(String sourceDir, String ragName, boolean forceRebuild);
    RAGResponse performQuery(String query, Long ragId, Integer topK, Boolean includeGraphContext, Integer contextDepth);
            Integer contextDepth);
    RAGResponse getKnowledgeGraph(Long ragId);
    RAGResponse getKnowledgeGraphByName(String ragName);
    RAGResponse fixKnowledgeGraphPath(Long ragId);
}
