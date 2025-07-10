package com.XuebaoMaster.backend.RAG;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface RAGRepository extends JpaRepository<RAG, Long> {
    Optional<RAG> findByName(String name);
}
