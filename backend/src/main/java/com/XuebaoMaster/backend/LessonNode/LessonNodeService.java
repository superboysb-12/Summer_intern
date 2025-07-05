package com.XuebaoMaster.backend.LessonNode;

import java.util.List;

public interface LessonNodeService {
    LessonNode createLessonNode(LessonNode lessonNode);

    LessonNode updateLessonNode(LessonNode lessonNode);

    void deleteLessonNode(Long id);

    LessonNode getLessonNodeById(Long id);

    List<LessonNode> getLessonNodesByCourseId(Long courseId);

    List<LessonNode> getLessonNodesByCourseIdOrdered(Long courseId);

    LessonNode getLessonNodeByCourseIdAndOrder(Long courseId, Integer nodeOrder);

    List<LessonNode> getAllLessonNodes();

    List<LessonNode> searchLessonNodes(String keyword);
}