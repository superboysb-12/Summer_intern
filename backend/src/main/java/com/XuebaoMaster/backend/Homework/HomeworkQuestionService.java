package com.XuebaoMaster.backend.Homework;

import java.util.List;
import java.util.Map;

public interface HomeworkQuestionService {

    /**
     * 为作业添加问题
     * 
     * @param homeworkId    作业ID
     * @param questionId    问题ID
     * @param questionOrder 题目顺序号
     * @param scoreWeight   分值权重
     * @return 保存的关联记录
     */
    HomeworkQuestion addQuestionToHomework(Long homeworkId, Long questionId, Integer questionOrder,
            Integer scoreWeight);

    /**
     * 批量为作业添加问题
     * 
     * @param homeworkId 作业ID
     * @param questions  问题列表，包含问题ID、顺序号和分值权重
     * @return 保存的关联记录列表
     */
    List<HomeworkQuestion> addQuestionsToHomework(Long homeworkId, List<Map<String, Object>> questions);

    /**
     * 从作业中移除问题
     * 
     * @param homeworkId 作业ID
     * @param questionId 问题ID
     * @return 操作成功返回true
     */
    boolean removeQuestionFromHomework(Long homeworkId, Long questionId);

    /**
     * 更新作业问题的顺序号或分值权重
     * 
     * @param id            关联记录ID
     * @param questionOrder 新的顺序号
     * @param scoreWeight   新的分值权重
     * @return 更新后的关联记录
     */
    HomeworkQuestion updateHomeworkQuestion(Long id, Integer questionOrder, Integer scoreWeight);

    /**
     * 获取作业中的所有问题
     * 
     * @param homeworkId 作业ID
     * @return 问题列表，按顺序号排序
     */
    List<HomeworkQuestion> getHomeworkQuestions(Long homeworkId);

    /**
     * 获取包含特定问题的所有作业
     * 
     * @param questionId 问题ID
     * @return 作业列表
     */
    List<HomeworkQuestion> getHomeworksByQuestionId(Long questionId);

    /**
     * 清空作业中的所有问题
     * 
     * @param homeworkId 作业ID
     * @return 操作成功返回true
     */
    boolean clearHomeworkQuestions(Long homeworkId);
}