package com.XuebaoMaster.backend.QuestionGenerator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionGeneratorRepository extends JpaRepository<QuestionGenerator, Long> {
}