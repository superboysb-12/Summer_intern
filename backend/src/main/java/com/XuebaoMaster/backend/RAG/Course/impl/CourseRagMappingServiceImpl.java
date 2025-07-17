package com.XuebaoMaster.backend.RAG.Course.impl;

import com.XuebaoMaster.backend.Course.Course;
import com.XuebaoMaster.backend.Course.CourseRepository;
import com.XuebaoMaster.backend.RAG.Course.CourseRagMapping;
import com.XuebaoMaster.backend.RAG.Course.CourseRagMappingRepository;
import com.XuebaoMaster.backend.RAG.Course.CourseRagMappingService;
import com.XuebaoMaster.backend.RAG.RAG;
import com.XuebaoMaster.backend.RAG.RAGRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseRagMappingServiceImpl implements CourseRagMappingService {

    @Autowired
    private CourseRagMappingRepository courseRagMappingRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RAGRepository ragRepository;

    @Override
    public CourseRagMapping createMapping(Long courseId, Long ragId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("课程不存在，ID: " + courseId));

        RAG rag = ragRepository.findById(ragId)
                .orElseThrow(() -> new EntityNotFoundException("RAG不存在，ID: " + ragId));

        return createMapping(course, rag);
    }

    @Override
    public CourseRagMapping createMapping(Course course, RAG rag) {
        if (courseRagMappingRepository.existsByCourseAndRag(course, rag)) {
            throw new IllegalStateException("该课程和RAG的映射关系已存在");
        }

        CourseRagMapping mapping = new CourseRagMapping(course, rag);
        return courseRagMappingRepository.save(mapping);
    }

    @Override
    public void deleteMapping(Long mappingId) {
        if (!courseRagMappingRepository.existsById(mappingId)) {
            throw new EntityNotFoundException("映射关系不存在，ID: " + mappingId);
        }
        courseRagMappingRepository.deleteById(mappingId);
    }

    @Override
    public void deleteMappingByCourseAndRag(Long courseId, Long ragId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("课程不存在，ID: " + courseId));

        RAG rag = ragRepository.findById(ragId)
                .orElseThrow(() -> new EntityNotFoundException("RAG不存在，ID: " + ragId));

        List<CourseRagMapping> mappings = courseRagMappingRepository.findAll().stream()
                .filter(mapping -> mapping.getCourse().getCourseId().equals(courseId) &&
                        mapping.getRag().getId().equals(ragId))
                .collect(Collectors.toList());

        courseRagMappingRepository.deleteAll(mappings);
    }

    @Override
    public List<RAG> findRagsByCourseId(Long courseId) {
        List<CourseRagMapping> mappings = courseRagMappingRepository.findByCourse_CourseId(courseId);
        return mappings.stream()
                .map(CourseRagMapping::getRag)
                .collect(Collectors.toList());
    }

    @Override
    public List<Course> findCoursesByRagId(Long ragId) {
        List<CourseRagMapping> mappings = courseRagMappingRepository.findByRag_Id(ragId);
        return mappings.stream()
                .map(CourseRagMapping::getCourse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean mappingExists(Long courseId, Long ragId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        RAG rag = ragRepository.findById(ragId).orElse(null);

        if (course == null || rag == null) {
            return false;
        }

        return courseRagMappingRepository.existsByCourseAndRag(course, rag);
    }
}