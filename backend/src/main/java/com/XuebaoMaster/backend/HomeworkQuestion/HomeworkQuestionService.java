package com.XuebaoMaster.backend.HomeworkQuestion;

import java.util.List;

public interface HomeworkQuestionService {

    // 创建作业题目关联
    HomeworkQuestion createHomeworkQuestion(HomeworkQuestion homeworkQuestion);

    // 批量添加题目到作业
    List<HomeworkQuestion> addQuestionsToHomework(Long homeworkId, List<Long> questionIds);

    // 更新题目顺序
    HomeworkQuestion updateQuestionOrder(Long id, Integer newOrderIndex);

    // 调整作业中多个题目的顺序
    List<HomeworkQuestion> reorderHomeworkQuestions(Long homeworkId, List<Long> questionIdsInOrder);

    // 从作业中删除题目
    void removeQuestionFromHomework(Long homeworkId, Long questionId);

    // 删除作业中的所有题目
    void removeAllQuestionsFromHomework(Long homeworkId);

    // 获取作业中的所有题目（按顺序）
    List<HomeworkQuestion> getQuestionsByHomeworkId(Long homeworkId);

    // 检查题目是否存在于作业中
    boolean isQuestionInHomework(Long homeworkId, Long questionId);

    // 获取作业中的题目数量
    Long getQuestionCountByHomeworkId(Long homeworkId);
}