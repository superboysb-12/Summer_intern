package com.XuebaoMaster.backend.HomeworkQuestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkQuestionRepository extends JpaRepository<HomeworkQuestion, Long> {

    // 根据作业ID查询所有题目，按orderIndex排序
    List<HomeworkQuestion> findByHomeworkIdOrderByOrderIndexAsc(Long homeworkId);

    // 根据作业ID和题目ID查询
    Optional<HomeworkQuestion> findByHomeworkIdAndQuestionId(Long homeworkId, Long questionId);

    // 查询作业下的题目数量
    Long countByHomeworkId(Long homeworkId);

    // 根据题目ID查询所有相关的作业题目关联
    List<HomeworkQuestion> findByQuestionId(Long questionId);

    // 删除作业下的所有题目
    void deleteByHomeworkId(Long homeworkId);

    // 检查题目是否已存在于作业中
    boolean existsByHomeworkIdAndQuestionId(Long homeworkId, Long questionId);

    // 查询作业下最大的orderIndex
    Optional<HomeworkQuestion> findTopByHomeworkIdOrderByOrderIndexDesc(Long homeworkId);
}