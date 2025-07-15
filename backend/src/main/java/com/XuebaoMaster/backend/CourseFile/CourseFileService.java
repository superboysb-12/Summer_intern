package com.XuebaoMaster.backend.CourseFile;

import java.util.List;
import java.util.Map;

public interface CourseFileService {

        /**
         * 将文件关联到课程
         * 
         * @param courseId     课程ID
         * @param fileId       文件ID
         * @param resourceType 资源类型
         * @param description  资源描述
         * @param isVisible    是否可见
         * @return 创建的课程文件关联
         */
        CourseFile associateFileToCourse(Long courseId, Long fileId, String resourceType, String description,
                        boolean isVisible);

        /**
         * 批量将文件关联到课程
         * 
         * @param courseId     课程ID
         * @param fileIds      文件ID列表
         * @param resourceType 资源类型
         * @param isVisible    是否可见
         * @return 创建的课程文件关联列表
         */
        List<CourseFile> associateFilesToCourse(Long courseId, List<Long> fileIds, String resourceType,
                        boolean isVisible);

        /**
         * 将文件夹及其所有内部文件关联到课程
         * 
         * @param courseId     课程ID
         * @param folderId     文件夹ID
         * @param resourceType 资源类型
         * @param isVisible    是否可见
         * @return 创建的课程文件关联列表
         */
        List<CourseFile> associateFolderToCourse(Long courseId, Long folderId, String resourceType, boolean isVisible);

        /**
         * 获取课程的所有关联文件
         * 
         * @param courseId 课程ID
         * @return 课程文件关联列表
         */
        List<CourseFile> getCourseFiles(Long courseId);

        /**
         * 获取课程的可见关联文件
         * 
         * @param courseId 课程ID
         * @return 可见的课程文件关联列表
         */
        List<CourseFile> getVisibleCourseFiles(Long courseId);

        /**
         * 获取课程的特定类型关联文件
         * 
         * @param courseId     课程ID
         * @param resourceType 资源类型
         * @return 特定类型的课程文件关联列表
         */
        List<CourseFile> getCourseFilesByType(Long courseId, String resourceType);

        /**
         * 获取课程的特定类型且可见的关联文件
         * 
         * @param courseId     课程ID
         * @param resourceType 资源类型
         * @return 特定类型且可见的课程文件关联列表
         */
        List<CourseFile> getVisibleCourseFilesByType(Long courseId, String resourceType);

        /**
         * 更新课程文件关联信息
         * 
         * @param courseFileId 课程文件关联ID
         * @param updates      更新的字段和值
         * @return 更新后的课程文件关联
         */
        CourseFile updateCourseFile(Long courseFileId, Map<String, Object> updates);

        /**
         * 更新课程文件的显示顺序
         * 
         * @param courseFileId 课程文件关联ID
         * @param displayOrder 新的显示顺序
         * @return 更新后的课程文件关联
         */
        CourseFile updateCourseFileOrder(Long courseFileId, int displayOrder);

        /**
         * 更新课程文件的可见性
         * 
         * @param courseFileId 课程文件关联ID
         * @param isVisible    是否可见
         * @return 更新后的课程文件关联
         */
        CourseFile updateCourseFileVisibility(Long courseFileId, boolean isVisible);

        /**
         * 移除课程文件关联
         * 
         * @param courseFileId 课程文件关联ID
         */
        void removeCourseFile(Long courseFileId);

        /**
         * 移除课程的所有文件关联
         * 
         * @param courseId 课程ID
         */
        void removeAllCourseFiles(Long courseId);

        /**
         * 获取文件关联的所有课程
         * 
         * @param fileId 文件ID
         * @return 文件关联的课程文件列表
         */
        List<CourseFile> getFileAssociations(Long fileId);
}