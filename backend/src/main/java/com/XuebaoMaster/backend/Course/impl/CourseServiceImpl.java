package com.XuebaoMaster.backend.Course.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.Course.Course;
import com.XuebaoMaster.backend.Course.CourseRepository;
import com.XuebaoMaster.backend.Course.CourseService;
import java.util.List;
@Service
public class CourseServiceImpl implements CourseService {
    @Autowired
    private CourseRepository courseRepository;
    @Override
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }
    @Override
    public Course updateCourse(Course course) {
        Course existingCourse = courseRepository.findById(course.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));
        existingCourse.setName(course.getName());
        existingCourse.setDescription(course.getDescription());
        return courseRepository.save(existingCourse);
    }
    @Override
    public void deleteCourse(Long courseId) {
        courseRepository.deleteById(courseId);
    }
    @Override
    public Course getCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }
    @Override
    public Course getCourseByName(String name) {
        return courseRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }
    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }
    @Override
    public List<Course> searchCourses(String keyword) {
        return courseRepository.findByNameContaining(keyword);
    }
    @Override
    public boolean checkNameExists(String name) {
        return courseRepository.existsByName(name);
    }
} 
