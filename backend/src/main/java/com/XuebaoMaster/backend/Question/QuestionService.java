package com.XuebaoMaster.backend.Question;

import java.util.List;
import java.util.Optional;

public interface QuestionService {
    Question createQuestion(Question question);

    Question updateQuestion(Question question);

    void deleteQuestion(Long id);

    Question getQuestionById(Long id);

    List<Question> getQuestionsByLessonNodeId(Long lessonNodeId);
    
    Optional<Question> getOneQuestionByLessonNodeId(Long lessonNodeId);
    
    List<Question> getQuestionsByStatus(Question.GenerationStatus status);

    List<Question> getAllQuestions();
    
    Question updateGenerationStatus(Long id, Question.GenerationStatus status);
} 