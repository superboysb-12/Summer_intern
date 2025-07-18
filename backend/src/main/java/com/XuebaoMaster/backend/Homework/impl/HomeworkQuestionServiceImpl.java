package com.XuebaoMaster.backend.Homework.impl;

import com.XuebaoMaster.backend.Homework.HomeworkQuestion;
import com.XuebaoMaster.backend.Homework.HomeworkQuestionRepository;
import com.XuebaoMaster.backend.Homework.HomeworkQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HomeworkQuestionServiceImpl implements HomeworkQuestionService {

    @Autowired
    private HomeworkQuestionRepository homeworkQuestionRepository;

    @Override
    public HomeworkQuestion addQuestionToHomework(Long homeworkId, Long questionId, Integer questionOrder,
            Integer scoreWeight) {
        // 检查是否已存在同样的关联
        if (homeworkQuestionRepository.existsByHomeworkIdAndQuestionId(homeworkId, questionId)) {
            throw new IllegalArgumentException("该作业已包含此问题");
        }

        // 创建新关联
        HomeworkQuestion homeworkQuestion = new HomeworkQuestion();
        homeworkQuestion.setHomeworkId(homeworkId);
        homeworkQuestion.setQuestionId(questionId);
        homeworkQuestion.setQuestionOrder(questionOrder);

        if (scoreWeight != null) {
            homeworkQuestion.setScoreWeight(scoreWeight);
        }

        return homeworkQuestionRepository.save(homeworkQuestion);
    }

    @Override
    @Transactional
    public List<HomeworkQuestion> addQuestionsToHomework(Long homeworkId, List<Map<String, Object>> questions) {
        List<HomeworkQuestion> savedQuestions = new ArrayList<>();

        for (Map<String, Object> question : questions) {
            Long questionId = toLong(question.get("questionId"));
            Integer questionOrder = toInteger(question.get("questionOrder"));
            Integer scoreWeight = toInteger(question.get("scoreWeight"));

            if (questionId == null || questionOrder == null) {
                throw new IllegalArgumentException("问题ID和顺序号不能为空");
            }

            // 检查是否已存在同样的关联
            if (!homeworkQuestionRepository.existsByHomeworkIdAndQuestionId(homeworkId, questionId)) {
                HomeworkQuestion homeworkQuestion = new HomeworkQuestion();
                homeworkQuestion.setHomeworkId(homeworkId);
                homeworkQuestion.setQuestionId(questionId);
                homeworkQuestion.setQuestionOrder(questionOrder);

                if (scoreWeight != null) {
                    homeworkQuestion.setScoreWeight(scoreWeight);
                }

                savedQuestions.add(homeworkQuestionRepository.save(homeworkQuestion));
            }
        }

        return savedQuestions;
    }

    @Override
    @Transactional
    public boolean removeQuestionFromHomework(Long homeworkId, Long questionId) {
        try {
            // 查找对应的关联记录
            List<HomeworkQuestion> questions = homeworkQuestionRepository.findAll();

            boolean removed = false;
            for (HomeworkQuestion question : questions) {
                if (question.getHomeworkId().equals(homeworkId) && question.getQuestionId().equals(questionId)) {
                    homeworkQuestionRepository.delete(question);
                    removed = true;
                    break;
                }
            }

            return removed;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public HomeworkQuestion updateHomeworkQuestion(Long id, Integer questionOrder, Integer scoreWeight) {
        HomeworkQuestion homeworkQuestion = homeworkQuestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的作业问题关联: " + id));

        if (questionOrder != null) {
            homeworkQuestion.setQuestionOrder(questionOrder);
        }

        if (scoreWeight != null) {
            homeworkQuestion.setScoreWeight(scoreWeight);
        }

        return homeworkQuestionRepository.save(homeworkQuestion);
    }

    @Override
    public List<HomeworkQuestion> getHomeworkQuestions(Long homeworkId) {
        return homeworkQuestionRepository.findByHomeworkIdOrderByQuestionOrder(homeworkId);
    }

    @Override
    public List<HomeworkQuestion> getHomeworksByQuestionId(Long questionId) {
        return homeworkQuestionRepository.findByQuestionId(questionId);
    }

    @Override
    @Transactional
    public boolean clearHomeworkQuestions(Long homeworkId) {
        try {
            homeworkQuestionRepository.deleteByHomeworkId(homeworkId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 辅助方法：将Object转换为Long
    private Long toLong(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        } else if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // 辅助方法：将Object转换为Integer
    private Integer toInteger(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Long) {
            return ((Long) obj).intValue();
        } else if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}