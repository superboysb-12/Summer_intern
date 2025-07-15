package com.XuebaoMaster.backend.CourseFile.dto;

import com.XuebaoMaster.backend.CourseFile.CourseFile;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CourseFileDTO {
    private Long id;
    private Long courseId;
    private String courseName;
    private Long fileId;
    private String fileName;
    private String filePath;
    private String mimeType;
    private Long fileSize;
    private boolean isDirectory;
    private String resourceType;
    private String description;
    private boolean isVisible;
    private int displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 从CourseFile实体转换为DTO
    public static CourseFileDTO fromEntity(CourseFile courseFile) {
        CourseFileDTO dto = new CourseFileDTO();
        dto.setId(courseFile.getId());
        dto.setCourseId(courseFile.getCourse().getCourseId());
        dto.setCourseName(courseFile.getCourse().getName());
        dto.setFileId(courseFile.getFile().getId());
        dto.setFileName(courseFile.getFile().getFileName());
        dto.setFilePath(courseFile.getFile().getFilePath());
        dto.setMimeType(courseFile.getFile().getMimeType());
        dto.setFileSize(courseFile.getFile().getFileSize());
        dto.setDirectory(courseFile.getFile().isDirectory());
        dto.setResourceType(courseFile.getResourceType());
        dto.setDescription(courseFile.getDescription());
        dto.setVisible(courseFile.isVisible());
        dto.setDisplayOrder(courseFile.getDisplayOrder());
        dto.setCreatedAt(courseFile.getCreatedAt());
        dto.setUpdatedAt(courseFile.getUpdatedAt());
        return dto;
    }
}