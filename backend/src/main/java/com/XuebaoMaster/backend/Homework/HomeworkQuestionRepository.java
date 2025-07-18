package com.XuebaoMaster.backend.Homework;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeworkQuestionRepository extends JpaRepository<HomeworkQuestion, Long> {
    // 根据作业ID查询所有关联的问题，按顺序号排序
    List<HomeworkQuestion> findByHomeworkIdOrderByQuestionOrder(Long homeworkId);

    // 根据问题ID查询所有包含该问题的作业
    List<HomeworkQuestion> findByQuestionId(Long questionId);

    // 删除作业的所有关联问题
    void deleteByHomeworkId(Long homeworkId);

    // 检查作业是否包含特定问题
    boolean existsByHomeworkIdAndQuestionId(Long homeworkId, Long questionId);
}