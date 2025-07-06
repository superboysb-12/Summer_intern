package com.XuebaoMaster.backend.LessonNode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface LessonNodeRepository extends JpaRepository<LessonNode, Long> {
    List<LessonNode> findByCourseCourseId(Long courseId);
    List<LessonNode> findByCourseCourseIdOrderByNodeOrder(Long courseId);
    Optional<LessonNode> findByCourseCourseIdAndNodeOrder(Long courseId, Integer nodeOrder);
    List<LessonNode> findByTitleContaining(String keyword);
} 
