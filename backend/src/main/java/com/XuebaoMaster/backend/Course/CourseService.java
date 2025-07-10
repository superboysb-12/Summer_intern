package com.XuebaoMaster.backend.Course;
import java.util.List;
public interface CourseService {
    Course createCourse(Course course);
    Course updateCourse(Course course);
    void deleteCourse(Long courseId);
    Course getCourseById(Long courseId);
    Course getCourseByName(String name);
    List<Course> getAllCourses();
    List<Course> searchCourses(String keyword);
    boolean checkNameExists(String name);
} 
