package com.XuebaoMaster.backend.RAG.Course;

import com.XuebaoMaster.backend.Course.Course;
import com.XuebaoMaster.backend.RAG.RAG;

import java.util.List;

public interface CourseRagMappingService {
    CourseRagMapping createMapping(Long courseId, Long ragId);

    CourseRagMapping createMapping(Course course, RAG rag);

    void deleteMapping(Long mappingId);

    void deleteMappingByCourseAndRag(Long courseId, Long ragId);

    List<RAG> findRagsByCourseId(Long courseId);

    List<Course> findCoursesByRagId(Long ragId);

    boolean mappingExists(Long courseId, Long ragId);
}