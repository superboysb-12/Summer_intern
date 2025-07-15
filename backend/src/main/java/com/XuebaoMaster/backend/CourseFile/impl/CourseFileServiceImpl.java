package com.XuebaoMaster.backend.CourseFile.impl;

import com.XuebaoMaster.backend.CourseFile.CourseFile;
import com.XuebaoMaster.backend.CourseFile.CourseFileRepository;
import com.XuebaoMaster.backend.CourseFile.CourseFileService;
import com.XuebaoMaster.backend.Course.Course;
import com.XuebaoMaster.backend.Course.CourseRepository;
import com.XuebaoMaster.backend.File.FileEntity;
import com.XuebaoMaster.backend.File.FileRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CourseFileServiceImpl implements CourseFileService {

    @Autowired
    private CourseFileRepository courseFileRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private FileRepository fileRepository;

    @Override
    @Transactional
    public CourseFile associateFileToCourse(Long courseId, Long fileId, String resourceType, String description,
            boolean isVisible) {
        // 验证课程是否存在
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));

        // 验证文件是否存在
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        // 检查是否已经存在关联
        Optional<CourseFile> existingAssociation = courseFileRepository.findByCourse_CourseIdAndFile_Id(courseId,
                fileId);
        if (existingAssociation.isPresent()) {
            throw new RuntimeException("该文件已经关联到此课程");
        }

        // 创建新的关联
        CourseFile courseFile = new CourseFile();
        courseFile.setCourse(course);
        courseFile.setFile(file);
        courseFile.setResourceType(resourceType);
        courseFile.setDescription(description);
        courseFile.setVisible(isVisible);

        // 获取当前课程文件数量作为新文件的顺序
        List<CourseFile> existingFiles = courseFileRepository.findByCourse_CourseId(courseId);
        courseFile.setDisplayOrder(existingFiles.size());

        return courseFileRepository.save(courseFile);
    }

    @Override
    @Transactional
    public List<CourseFile> associateFilesToCourse(Long courseId, List<Long> fileIds, String resourceType,
            boolean isVisible) {
        List<CourseFile> result = new ArrayList<>();

        // 验证课程是否存在
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));

        // 获取当前课程文件数量作为新文件的起始顺序
        List<CourseFile> existingFiles = courseFileRepository.findByCourse_CourseId(courseId);
        int startOrder = existingFiles.size();

        for (int i = 0; i < fileIds.size(); i++) {
            Long fileId = fileIds.get(i);

            // 验证文件是否存在
            FileEntity file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("文件ID " + fileId + " 不存在"));

            // 检查是否已经存在关联
            Optional<CourseFile> existingAssociation = courseFileRepository.findByCourse_CourseIdAndFile_Id(courseId,
                    fileId);
            if (existingAssociation.isPresent()) {
                continue; // 跳过已存在的关联
            }

            // 创建新的关联
            CourseFile courseFile = new CourseFile();
            courseFile.setCourse(course);
            courseFile.setFile(file);
            courseFile.setResourceType(resourceType);
            courseFile.setDescription(file.getFileName()); // 使用文件名作为默认描述
            courseFile.setVisible(isVisible);
            courseFile.setDisplayOrder(startOrder + i);

            result.add(courseFileRepository.save(courseFile));
        }

        return result;
    }

    @Override
    @Transactional
    public List<CourseFile> associateFolderToCourse(Long courseId, Long folderId, String resourceType,
            boolean isVisible) {
        // 验证课程是否存在
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));

        // 验证文件夹是否存在
        FileEntity folder = fileRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("文件夹不存在"));

        // 确保是文件夹
        if (!folder.isDirectory()) {
            throw new RuntimeException("指定的ID不是文件夹");
        }

        List<CourseFile> result = new ArrayList<>();

        // 获取文件夹内所有文件（包括子文件夹中的文件）
        List<FileEntity> allFiles = getAllFilesInFolder(folder);

        // 获取当前课程文件数量作为新文件的起始顺序
        List<CourseFile> existingFiles = courseFileRepository.findByCourse_CourseId(courseId);
        int startOrder = existingFiles.size();

        // 批量关联文件
        for (int i = 0; i < allFiles.size(); i++) {
            FileEntity file = allFiles.get(i);

            // 跳过文件夹本身，只关联文件
            if (file.isDirectory()) {
                continue;
            }

            // 检查是否已经存在关联
            Optional<CourseFile> existingAssociation = courseFileRepository.findByCourse_CourseIdAndFile_Id(courseId,
                    file.getId());
            if (existingAssociation.isPresent()) {
                continue; // 跳过已存在的关联
            }

            // 创建新的关联
            CourseFile courseFile = new CourseFile();
            courseFile.setCourse(course);
            courseFile.setFile(file);
            courseFile.setResourceType(resourceType);
            courseFile.setDescription(file.getFileName()); // 使用文件名作为默认描述
            courseFile.setVisible(isVisible);
            courseFile.setDisplayOrder(startOrder + i);

            result.add(courseFileRepository.save(courseFile));
        }

        return result;
    }

    /**
     * 递归获取文件夹中的所有文件（包括子文件夹中的文件）
     * 
     * @param folder 文件夹
     * @return 所有文件列表
     */
    private List<FileEntity> getAllFilesInFolder(FileEntity folder) {
        List<FileEntity> result = new ArrayList<>();

        // 获取文件夹的路径
        String folderPath = folder.getFilePath();

        // 查找该路径下的所有文件和文件夹
        List<FileEntity> contents = fileRepository.findByFilePathStartingWith(folderPath);

        // 添加所有文件（不含文件夹）到结果列表
        for (FileEntity entity : contents) {
            if (!entity.isDirectory()) {
                result.add(entity);
            }
        }

        return result;
    }

    @Override
    public List<CourseFile> getCourseFiles(Long courseId) {
        return courseFileRepository.findByCourse_CourseIdOrderByDisplayOrderAsc(courseId);
    }

    @Override
    public List<CourseFile> getVisibleCourseFiles(Long courseId) {
        return courseFileRepository.findByCourse_CourseIdAndIsVisibleOrderByDisplayOrderAsc(courseId, true);
    }

    @Override
    public List<CourseFile> getCourseFilesByType(Long courseId, String resourceType) {
        return courseFileRepository.findByCourse_CourseIdAndResourceType(courseId, resourceType);
    }

    @Override
    public List<CourseFile> getVisibleCourseFilesByType(Long courseId, String resourceType) {
        return courseFileRepository.findByCourse_CourseIdAndResourceTypeAndIsVisible(courseId, resourceType, true);
    }

    @Override
    @Transactional
    public CourseFile updateCourseFile(Long courseFileId, Map<String, Object> updates) {
        CourseFile courseFile = courseFileRepository.findById(courseFileId)
                .orElseThrow(() -> new RuntimeException("课程文件关联不存在"));

        // 更新字段
        if (updates.containsKey("resourceType")) {
            courseFile.setResourceType((String) updates.get("resourceType"));
        }

        if (updates.containsKey("description")) {
            courseFile.setDescription((String) updates.get("description"));
        }

        if (updates.containsKey("isVisible")) {
            courseFile.setVisible((Boolean) updates.get("isVisible"));
        }

        if (updates.containsKey("displayOrder")) {
            courseFile.setDisplayOrder((Integer) updates.get("displayOrder"));
        }

        return courseFileRepository.save(courseFile);
    }

    @Override
    @Transactional
    public CourseFile updateCourseFileOrder(Long courseFileId, int displayOrder) {
        CourseFile courseFile = courseFileRepository.findById(courseFileId)
                .orElseThrow(() -> new RuntimeException("课程文件关联不存在"));

        courseFile.setDisplayOrder(displayOrder);
        return courseFileRepository.save(courseFile);
    }

    @Override
    @Transactional
    public CourseFile updateCourseFileVisibility(Long courseFileId, boolean isVisible) {
        CourseFile courseFile = courseFileRepository.findById(courseFileId)
                .orElseThrow(() -> new RuntimeException("课程文件关联不存在"));

        courseFile.setVisible(isVisible);
        return courseFileRepository.save(courseFile);
    }

    @Override
    @Transactional
    public void removeCourseFile(Long courseFileId) {
        courseFileRepository.deleteById(courseFileId);
    }

    @Override
    @Transactional
    public void removeAllCourseFiles(Long courseId) {
        List<CourseFile> courseFiles = courseFileRepository.findByCourse_CourseId(courseId);
        courseFileRepository.deleteAll(courseFiles);
    }

    @Override
    public List<CourseFile> getFileAssociations(Long fileId) {
        return courseFileRepository.findByFile_Id(fileId);
    }
}