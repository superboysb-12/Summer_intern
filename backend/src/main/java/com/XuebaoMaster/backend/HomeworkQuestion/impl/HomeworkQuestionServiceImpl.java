package com.XuebaoMaster.backend.HomeworkQuestion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.XuebaoMaster.backend.HomeworkQuestion.HomeworkQuestion;
import com.XuebaoMaster.backend.HomeworkQuestion.HomeworkQuestionRepository;
import com.XuebaoMaster.backend.HomeworkQuestion.HomeworkQuestionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class HomeworkQuestionServiceImpl implements HomeworkQuestionService {

    @Autowired
    private HomeworkQuestionRepository homeworkQuestionRepository;

    @Override
    public HomeworkQuestion createHomeworkQuestion(HomeworkQuestion homeworkQuestion) {
        // 如果没有指定顺序，则将其添加到末尾
        if (homeworkQuestion.getOrderIndex() == null) {
            Optional<HomeworkQuestion> lastQuestion = homeworkQuestionRepository
                    .findTopByHomeworkIdOrderByOrderIndexDesc(homeworkQuestion.getHomeworkId());

            int orderIndex = lastQuestion.map(q -> q.getOrderIndex() + 1).orElse(0);
            homeworkQuestion.setOrderIndex(orderIndex);
        }

        return homeworkQuestionRepository.save(homeworkQuestion);
    }

    @Override
    @Transactional
    public List<HomeworkQuestion> addQuestionsToHomework(Long homeworkId, List<Long> questionIds) {
        List<HomeworkQuestion> result = new ArrayList<>();

        // 获取当前最大顺序值
        Optional<HomeworkQuestion> lastQuestion = homeworkQuestionRepository
                .findTopByHomeworkIdOrderByOrderIndexDesc(homeworkId);
        int nextOrderIndex = lastQuestion.map(q -> q.getOrderIndex() + 1).orElse(0);

        for (Long questionId : questionIds) {
            // 检查题目是否已存在于作业中
            if (homeworkQuestionRepository.existsByHomeworkIdAndQuestionId(homeworkId, questionId)) {
                continue;
            }

            HomeworkQuestion homeworkQuestion = new HomeworkQuestion();
            homeworkQuestion.setHomeworkId(homeworkId);
            homeworkQuestion.setQuestionId(questionId);
            homeworkQuestion.setOrderIndex(nextOrderIndex++);
            homeworkQuestion.setWeight(1.0); // 默认权重

            result.add(homeworkQuestionRepository.save(homeworkQuestion));
        }

        return result;
    }

    @Override
    @Transactional
    public HomeworkQuestion updateQuestionOrder(Long id, Integer newOrderIndex) {
        HomeworkQuestion homeworkQuestion = homeworkQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("HomeworkQuestion not found with id: " + id));

        int oldOrderIndex = homeworkQuestion.getOrderIndex();

        // 更新其他题目的顺序以保持连续性
        List<HomeworkQuestion> questions = homeworkQuestionRepository
                .findByHomeworkIdOrderByOrderIndexAsc(homeworkQuestion.getHomeworkId());

        for (HomeworkQuestion q : questions) {
            if (q.getId().equals(id)) {
                continue; // 跳过当前正在更新的题目
            }

            int currentIndex = q.getOrderIndex();

            if (oldOrderIndex < newOrderIndex) {
                // 向下移动
                if (currentIndex > oldOrderIndex && currentIndex <= newOrderIndex) {
                    q.setOrderIndex(currentIndex - 1);
                    homeworkQuestionRepository.save(q);
                }
            } else if (oldOrderIndex > newOrderIndex) {
                // 向上移动
                if (currentIndex >= newOrderIndex && currentIndex < oldOrderIndex) {
                    q.setOrderIndex(currentIndex + 1);
                    homeworkQuestionRepository.save(q);
                }
            }
        }

        // 更新当前题目的顺序
        homeworkQuestion.setOrderIndex(newOrderIndex);
        return homeworkQuestionRepository.save(homeworkQuestion);
    }

    @Override
    @Transactional
    public List<HomeworkQuestion> reorderHomeworkQuestions(Long homeworkId, List<Long> questionIdsInOrder) {
        List<HomeworkQuestion> result = new ArrayList<>();
        Map<Long, HomeworkQuestion> questionMap = new HashMap<>();

        // 获取作业中所有的题目
        List<HomeworkQuestion> existingQuestions = homeworkQuestionRepository
                .findByHomeworkIdOrderByOrderIndexAsc(homeworkId);
        for (HomeworkQuestion q : existingQuestions) {
            questionMap.put(q.getQuestionId(), q);
        }

        // 根据提供的顺序重新排序
        int orderIndex = 0;
        for (Long questionId : questionIdsInOrder) {
            HomeworkQuestion question = questionMap.get(questionId);
            if (question != null) {
                question.setOrderIndex(orderIndex++);
                result.add(homeworkQuestionRepository.save(question));
            }
        }

        return result;
    }

    @Override
    @Transactional
    public void removeQuestionFromHomework(Long homeworkId, Long questionId) {
        Optional<HomeworkQuestion> question = homeworkQuestionRepository
                .findByHomeworkIdAndQuestionId(homeworkId, questionId);

        if (question.isPresent()) {
            HomeworkQuestion homeworkQuestion = question.get();
            int orderIndex = homeworkQuestion.getOrderIndex();

            // 删除题目
            homeworkQuestionRepository.delete(homeworkQuestion);

            // 更新其他题目的顺序
            List<HomeworkQuestion> questions = homeworkQuestionRepository
                    .findByHomeworkIdOrderByOrderIndexAsc(homeworkId);

            for (HomeworkQuestion q : questions) {
                if (q.getOrderIndex() > orderIndex) {
                    q.setOrderIndex(q.getOrderIndex() - 1);
                    homeworkQuestionRepository.save(q);
                }
            }
        }
    }

    @Override
    @Transactional
    public void removeAllQuestionsFromHomework(Long homeworkId) {
        homeworkQuestionRepository.deleteByHomeworkId(homeworkId);
    }

    @Override
    public List<HomeworkQuestion> getQuestionsByHomeworkId(Long homeworkId) {
        return homeworkQuestionRepository.findByHomeworkIdOrderByOrderIndexAsc(homeworkId);
    }

    @Override
    public boolean isQuestionInHomework(Long homeworkId, Long questionId) {
        return homeworkQuestionRepository.existsByHomeworkIdAndQuestionId(homeworkId, questionId);
    }

    @Override
    public Long getQuestionCountByHomeworkId(Long homeworkId) {
        return homeworkQuestionRepository.countByHomeworkId(homeworkId);
    }
}