package com.XuebaoMaster.backend.Question.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.Question.Question;
import com.XuebaoMaster.backend.Question.QuestionRepository;
import com.XuebaoMaster.backend.Question.QuestionService;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Override
    public Question createQuestion(Question question) {
        return questionRepository.save(question);
    }

    @Override
    public Question updateQuestion(Question question) {
        Question existingQuestion = questionRepository.findById(question.getId())
                .orElseThrow(() -> new RuntimeException("Question not found"));

        existingQuestion.setLessonNode(question.getLessonNode());
        existingQuestion.setContent(question.getContent());
        existingQuestion.setGenerationStatus(question.getGenerationStatus());

        return questionRepository.save(existingQuestion);
    }

    @Override
    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    @Override
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
    }

    @Override
    public List<Question> getQuestionsByLessonNodeId(Long lessonNodeId) {
        return questionRepository.findByLessonNodeId(lessonNodeId);
    }
    
    @Override
    public Optional<Question> getOneQuestionByLessonNodeId(Long lessonNodeId) {
        return questionRepository.findOneByLessonNodeId(lessonNodeId);
    }
    
    @Override
    public List<Question> getQuestionsByStatus(Question.GenerationStatus status) {
        return questionRepository.findByGenerationStatus(status);
    }

    @Override
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }
    
    @Override
    public Question updateGenerationStatus(Long id, Question.GenerationStatus status) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        question.setGenerationStatus(status);
        return questionRepository.save(question);
    }
} 