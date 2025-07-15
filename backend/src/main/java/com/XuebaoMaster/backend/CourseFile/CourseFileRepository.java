package com.XuebaoMaster.backend.CourseFile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseFileRepository extends JpaRepository<CourseFile, Long> {

    // 根据课程ID查找所有课程文件
    List<CourseFile> findByCourse_CourseId(Long courseId);

    // 根据课程ID和可见性查找课程文件
    List<CourseFile> findByCourse_CourseIdAndIsVisible(Long courseId, boolean isVisible);

    // 根据课程ID和资源类型查找课程文件
    List<CourseFile> findByCourse_CourseIdAndResourceType(Long courseId, String resourceType);

    // 根据课程ID、资源类型和可见性查找课程文件
    List<CourseFile> findByCourse_CourseIdAndResourceTypeAndIsVisible(Long courseId, String resourceType,
            boolean isVisible);

    // 根据文件ID查找所有课程文件
    List<CourseFile> findByFile_Id(Long fileId);

    // 检查特定课程和文件的关联是否存在
    Optional<CourseFile> findByCourse_CourseIdAndFile_Id(Long courseId, Long fileId);

    // 根据课程ID和排序查找课程文件
    List<CourseFile> findByCourse_CourseIdOrderByDisplayOrderAsc(Long courseId);

    // 根据课程ID和可见性排序查找课程文件
    List<CourseFile> findByCourse_CourseIdAndIsVisibleOrderByDisplayOrderAsc(Long courseId, boolean isVisible);
}