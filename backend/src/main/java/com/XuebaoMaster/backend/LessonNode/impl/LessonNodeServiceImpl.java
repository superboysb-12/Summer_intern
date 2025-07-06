package com.XuebaoMaster.backend.LessonNode.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.XuebaoMaster.backend.LessonNode.LessonNode;
import com.XuebaoMaster.backend.LessonNode.LessonNodeRepository;
import com.XuebaoMaster.backend.LessonNode.LessonNodeService;
import java.util.List;
@Service
public class LessonNodeServiceImpl implements LessonNodeService {
    @Autowired
    private LessonNodeRepository lessonNodeRepository;
    @Override
    @Transactional
    public LessonNode createLessonNode(LessonNode lessonNode) {
        System.out.println("鍒涘缓璇炬椂鑺傜偣: 鏍囬=" + lessonNode.getTitle()
                + ", 鑺傜偣椤哄簭=" + lessonNode.getNodeOrder()
                + ", 璇剧▼ID=" + (lessonNode.getCourse() != null ? lessonNode.getCourse().getCourseId() : "null"));
        LessonNode savedNode = lessonNodeRepository.save(lessonNode);
        System.out.println("淇濆瓨鍚庣殑璇炬椂鑺傜偣: ID=" + savedNode.getId()
                + ", 鏍囬=" + savedNode.getTitle());
        return savedNode;
    }
    @Override
    @Transactional
    public LessonNode updateLessonNode(LessonNode lessonNode) {
        LessonNode existingNode = lessonNodeRepository.findById(lessonNode.getId())
                .orElseThrow(() -> new RuntimeException("Lesson node not found"));
        existingNode.setCourse(lessonNode.getCourse());
        existingNode.setNodeOrder(lessonNode.getNodeOrder());
        existingNode.setTitle(lessonNode.getTitle());
        System.out.println("鏇存柊璇炬椂鑺傜偣锛孖D: " + existingNode.getId());
        return lessonNodeRepository.save(existingNode);
    }
    @Override
    public void deleteLessonNode(Long id) {
        lessonNodeRepository.deleteById(id);
    }
    @Override
    public LessonNode getLessonNodeById(Long id) {
        return lessonNodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson node not found"));
    }
    @Override
    public List<LessonNode> getLessonNodesByCourseId(Long courseId) {
        return lessonNodeRepository.findByCourseCourseId(courseId);
    }
    @Override
    public List<LessonNode> getLessonNodesByCourseIdOrdered(Long courseId) {
        return lessonNodeRepository.findByCourseCourseIdOrderByNodeOrder(courseId);
    }
    @Override
    public LessonNode getLessonNodeByCourseIdAndOrder(Long courseId, Integer nodeOrder) {
        return lessonNodeRepository.findByCourseCourseIdAndNodeOrder(courseId, nodeOrder)
                .orElseThrow(() -> new RuntimeException("Lesson node not found"));
    }
    @Override
    public List<LessonNode> getAllLessonNodes() {
        return lessonNodeRepository.findAll();
    }
    @Override
    public List<LessonNode> searchLessonNodes(String keyword) {
        return lessonNodeRepository.findByTitleContaining(keyword);
    }
}
