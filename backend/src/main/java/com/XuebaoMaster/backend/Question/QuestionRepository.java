package com.XuebaoMaster.backend.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("SELECT q FROM Question q WHERE q.lessonNode.id = :lessonNodeId")
    List<Question> findByLessonNodeId(@Param("lessonNodeId") Long lessonNodeId);
    @Query("SELECT q FROM Question q WHERE q.lessonNode.id = :lessonNodeId")
    Optional<Question> findOneByLessonNodeId(@Param("lessonNodeId") Long lessonNodeId);
    List<Question> findByGenerationStatus(Question.GenerationStatus status);
} 
