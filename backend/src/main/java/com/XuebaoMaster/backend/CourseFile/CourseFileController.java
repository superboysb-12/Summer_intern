package com.XuebaoMaster.backend.CourseFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.XuebaoMaster.backend.CourseFile.dto.CourseFileDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/course-files")
public class CourseFileController {

    @Autowired
    private CourseFileService courseFileService;

    /**
     * 将文件关联到课程
     */
    @PostMapping("/associate")
    public ResponseEntity<?> associateFileToCourse(@RequestBody Map<String, Object> request) {
        try {
            Long courseId = Long.valueOf(request.get("courseId").toString());
            Long fileId = Long.valueOf(request.get("fileId").toString());
            String resourceType = (String) request.get("resourceType");
            String description = (String) request.getOrDefault("description", "");
            boolean isVisible = Boolean.TRUE.equals(request.getOrDefault("isVisible", true));

            CourseFile courseFile = courseFileService.associateFileToCourse(courseId, fileId, resourceType, description,
                    isVisible);
            return ResponseEntity.status(HttpStatus.CREATED).body(CourseFileDTO.fromEntity(courseFile));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 批量将文件关联到课程
     */
    @PostMapping("/associate-batch")
    public ResponseEntity<?> associateFilesToCourse(@RequestBody Map<String, Object> request) {
        try {
            Long courseId = Long.valueOf(request.get("courseId").toString());
            @SuppressWarnings("unchecked")
            List<Long> fileIds = (List<Long>) request.get("fileIds");
            String resourceType = (String) request.get("resourceType");
            boolean isVisible = Boolean.TRUE.equals(request.getOrDefault("isVisible", true));

            List<CourseFile> courseFiles = courseFileService.associateFilesToCourse(courseId, fileIds, resourceType,
                    isVisible);
            List<CourseFileDTO> dtos = courseFiles.stream()
                    .map(CourseFileDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.CREATED).body(dtos);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 将文件夹及其所有内部文件关联到课程
     */
    @PostMapping("/associate-folder")
    public ResponseEntity<?> associateFolderToCourse(@RequestBody Map<String, Object> request) {
        try {
            Long courseId = Long.valueOf(request.get("courseId").toString());
            Long folderId = Long.valueOf(request.get("folderId").toString());
            String resourceType = (String) request.get("resourceType");
            boolean isVisible = Boolean.TRUE.equals(request.getOrDefault("isVisible", true));

            List<CourseFile> courseFiles = courseFileService.associateFolderToCourse(
                    courseId, folderId, resourceType, isVisible);

            List<CourseFileDTO> dtos = courseFiles.stream()
                    .map(CourseFileDTO::fromEntity)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文件夹及其内容已关联到课程");
            response.put("data", dtos);
            response.put("count", dtos.size());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 获取课程的所有关联文件
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getCourseFiles(@PathVariable Long courseId) {
        try {
            List<CourseFile> courseFiles = courseFileService.getCourseFiles(courseId);
            List<CourseFileDTO> dtos = courseFiles.stream()
                    .map(CourseFileDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 获取课程的可见关联文件
     */
    @GetMapping("/course/{courseId}/visible")
    public ResponseEntity<?> getVisibleCourseFiles(@PathVariable Long courseId) {
        try {
            List<CourseFile> courseFiles = courseFileService.getVisibleCourseFiles(courseId);
            List<CourseFileDTO> dtos = courseFiles.stream()
                    .map(CourseFileDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 获取课程的特定类型关联文件
     */
    @GetMapping("/course/{courseId}/type/{resourceType}")
    public ResponseEntity<?> getCourseFilesByType(
            @PathVariable Long courseId,
            @PathVariable String resourceType) {
        try {
            List<CourseFile> courseFiles = courseFileService.getCourseFilesByType(courseId, resourceType);
            List<CourseFileDTO> dtos = courseFiles.stream()
                    .map(CourseFileDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 获取课程的特定类型且可见的关联文件
     */
    @GetMapping("/course/{courseId}/type/{resourceType}/visible")
    public ResponseEntity<?> getVisibleCourseFilesByType(
            @PathVariable Long courseId,
            @PathVariable String resourceType) {
        try {
            List<CourseFile> courseFiles = courseFileService.getVisibleCourseFilesByType(courseId, resourceType);
            List<CourseFileDTO> dtos = courseFiles.stream()
                    .map(CourseFileDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 更新课程文件关联信息
     */
    @PutMapping("/{courseFileId}")
    public ResponseEntity<?> updateCourseFile(
            @PathVariable Long courseFileId,
            @RequestBody Map<String, Object> updates) {
        try {
            CourseFile courseFile = courseFileService.updateCourseFile(courseFileId, updates);
            return ResponseEntity.ok(CourseFileDTO.fromEntity(courseFile));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 更新课程文件的显示顺序
     */
    @PutMapping("/{courseFileId}/order")
    public ResponseEntity<?> updateCourseFileOrder(
            @PathVariable Long courseFileId,
            @RequestBody Map<String, Integer> request) {
        try {
            Integer displayOrder = request.get("displayOrder");
            CourseFile courseFile = courseFileService.updateCourseFileOrder(courseFileId, displayOrder);
            return ResponseEntity.ok(CourseFileDTO.fromEntity(courseFile));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 更新课程文件的可见性
     */
    @PutMapping("/{courseFileId}/visibility")
    public ResponseEntity<?> updateCourseFileVisibility(
            @PathVariable Long courseFileId,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean isVisible = request.get("isVisible");
            CourseFile courseFile = courseFileService.updateCourseFileVisibility(courseFileId, isVisible);
            return ResponseEntity.ok(CourseFileDTO.fromEntity(courseFile));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 移除课程文件关联
     */
    @DeleteMapping("/{courseFileId}")
    public ResponseEntity<Map<String, Object>> removeCourseFile(@PathVariable Long courseFileId) {
        try {
            courseFileService.removeCourseFile(courseFileId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "课程文件关联已删除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 移除课程的所有文件关联
     */
    @DeleteMapping("/course/{courseId}")
    public ResponseEntity<Map<String, Object>> removeAllCourseFiles(@PathVariable Long courseId) {
        try {
            courseFileService.removeAllCourseFiles(courseId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "课程的所有文件关联已删除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 获取文件关联的所有课程
     */
    @GetMapping("/file/{fileId}")
    public ResponseEntity<?> getFileAssociations(@PathVariable Long fileId) {
        try {
            List<CourseFile> courseFiles = courseFileService.getFileAssociations(fileId);
            List<CourseFileDTO> dtos = courseFiles.stream()
                    .map(CourseFileDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}