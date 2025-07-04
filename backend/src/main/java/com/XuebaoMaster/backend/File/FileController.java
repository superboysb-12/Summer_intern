package com.XuebaoMaster.backend.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileService fileService;

    // 通用响应结构
    private <T> Map<String, Object> createResponse(int code, String message, T data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "path", required = false, defaultValue = "/") String path,
            @RequestParam(value = "overwrite", required = false, defaultValue = "false") boolean overwrite) {

        Map<String, Object> result = fileService.uploadFiles(files, path, overwrite);
        return ResponseEntity.ok(createResponse(200, "Upload completed", result));
    }

    /**
     * 创建文件夹
     */
    @PostMapping("/folders")
    public ResponseEntity<Map<String, Object>> createFolder(@RequestBody Map<String, String> request) {
        String folderName = request.get("folderName");
        String parentPath = request.get("parentPath");

        FileEntity folder = fileService.createFolder(folderName, parentPath);
        return ResponseEntity.status(HttpStatus.CREATED).body(createResponse(200, "Folder created", folder));
    }

    /**
     * 获取文件列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getFileList(
            @RequestParam(value = "path", required = false, defaultValue = "/") String path,
            @RequestParam(value = "includeFiles", required = false, defaultValue = "true") boolean includeFiles) {

        List<FileEntity> files = fileService.getFileList(path, includeFiles);
        return ResponseEntity.ok(createResponse(200, "success", files));
    }

    /**
     * 重命名文件夹
     */
    @PutMapping("/folders/{folderId}/rename")
    public ResponseEntity<Map<String, Object>> renameFolder(
            @PathVariable Long folderId,
            @RequestBody Map<String, String> request) {

        String newName = request.get("newName");
        FileEntity folder = fileService.renameFolder(folderId, newName);
        return ResponseEntity.ok(createResponse(200, "Folder renamed", folder));
    }

    /**
     * 移动文件夹
     */
    @PutMapping("/folders/{folderId}/move")
    public ResponseEntity<Map<String, Object>> moveFolder(
            @PathVariable Long folderId,
            @RequestBody Map<String, String> request) {

        String targetPath = request.get("targetPath");
        FileEntity folder = fileService.moveFolder(folderId, targetPath);
        return ResponseEntity.ok(createResponse(200, "Folder moved", folder));
    }

    /**
     * 删除文件夹
     */
    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<Map<String, Object>> deleteFolder(
            @PathVariable Long folderId,
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {

        fileService.deleteFolder(folderId, force);
        return ResponseEntity.ok(createResponse(200, "Folder deleted", null));
    }

    /**
     * 批量删除文件夹
     */
    @DeleteMapping("/folders/batch")
    public ResponseEntity<Map<String, Object>> deleteFoldersBatch(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> folderIds = (List<Long>) request.get("folderIds");
        boolean force = request.containsKey("force") ? (boolean) request.get("force") : false;

        fileService.deleteFoldersBatch(folderIds, force);
        return ResponseEntity.ok(createResponse(200, "Folders deleted", null));
    }

    /**
     * 文件下载
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId, HttpServletRequest request) {
        Resource resource = fileService.downloadFile(fileId);
        FileEntity fileInfo = fileService.getFileInfo(fileId);

        return getResourceResponseEntity(resource, fileInfo.getFileName(), request);
    }

    /**
     * 批量下载
     */
    @PostMapping("/download/batch")
    public ResponseEntity<Resource> downloadBatch(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        @SuppressWarnings("unchecked")
        List<Long> fileIds = (List<Long>) request.get("fileIds");
        String zipName = (String) request.get("zipName");

        Resource resource = fileService.downloadBatch(fileIds, zipName);

        return getResourceResponseEntity(resource, zipName, httpRequest);
    }

    /**
     * 获取文件资源的ResponseEntity
     */
    private ResponseEntity<Resource> getResourceResponseEntity(
            Resource resource, String filename, HttpServletRequest request) {

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // 默认内容类型
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * 重命名文件
     */
    @PutMapping("/{fileId}/rename")
    public ResponseEntity<Map<String, Object>> renameFile(
            @PathVariable Long fileId,
            @RequestBody Map<String, String> request) {

        String newName = request.get("newName");
        FileEntity file = fileService.renameFile(fileId, newName);
        return ResponseEntity.ok(createResponse(200, "File renamed", file));
    }

    /**
     * 移动文件
     */
    @PutMapping("/{fileId}/move")
    public ResponseEntity<Map<String, Object>> moveFile(
            @PathVariable Long fileId,
            @RequestBody Map<String, String> request) {

        String targetPath = request.get("targetPath");
        FileEntity file = fileService.moveFile(fileId, targetPath);
        return ResponseEntity.ok(createResponse(200, "File moved", file));
    }

    /**
     * 复制文件
     */
    @PostMapping("/{fileId}/copy")
    public ResponseEntity<Map<String, Object>> copyFile(
            @PathVariable Long fileId,
            @RequestBody Map<String, String> request) {

        String targetPath = request.get("targetPath");
        String newName = request.get("newName");
        FileEntity file = fileService.copyFile(fileId, targetPath, newName);
        return ResponseEntity.ok(createResponse(200, "File copied", file));
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable Long fileId) {
        fileService.deleteFile(fileId);
        return ResponseEntity.ok(createResponse(200, "File deleted", null));
    }

    /**
     * 批量删除文件
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> deleteFilesBatch(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> fileIds = (List<Long>) request.get("fileIds");

        fileService.deleteFilesBatch(fileIds);
        return ResponseEntity.ok(createResponse(200, "Files deleted", null));
    }

    /**
     * 预览文件
     */
    @GetMapping("/{fileId}/preview")
    public ResponseEntity<Resource> previewFile(
            @PathVariable Long fileId,
            @RequestParam(value = "type", required = false) String type,
            HttpServletRequest request) {

        Resource resource = fileService.previewFile(fileId, type);
        FileEntity fileInfo = fileService.getFileInfo(fileId);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // 默认内容类型
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileInfo.getFileName() + "\"")
                .body(resource);
    }

    /**
     * 文件信息
     */
    @GetMapping("/{fileId}/info")
    public ResponseEntity<Map<String, Object>> getFileInfo(@PathVariable Long fileId) {
        FileEntity file = fileService.getFileInfo(fileId);
        return ResponseEntity.ok(createResponse(200, "success", file));
    }

    /**
     * 搜索文件
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchFiles(
            @RequestParam("q") String query,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {

        List<FileEntity> files = fileService.searchFiles(query, type, path, page, limit);
        return ResponseEntity.ok(createResponse(200, "success", files));
    }
}