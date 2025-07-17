package com.XuebaoMaster.backend.RAG.Course;

import com.XuebaoMaster.backend.Course.Course;
import com.XuebaoMaster.backend.RAG.RAG;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRagMappingRepository extends JpaRepository<CourseRagMapping, Long> {
    List<CourseRagMapping> findByCourse(Course course);

    List<CourseRagMapping> findByRag(RAG rag);

    List<CourseRagMapping> findByCourse_CourseId(Long courseId);

    List<CourseRagMapping> findByRag_Id(Long ragId);

    boolean existsByCourseAndRag(Course course, RAG rag);
}