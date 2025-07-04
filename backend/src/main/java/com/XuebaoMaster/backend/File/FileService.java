package com.XuebaoMaster.backend.File;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface FileService {

    // 文件上传
    Map<String, Object> uploadFiles(MultipartFile[] files, String path, boolean overwrite);

    // 创建文件夹
    FileEntity createFolder(String folderName, String parentPath);

    // 获取文件列表
    List<FileEntity> getFileList(String path, boolean includeFiles);

    // 重命名文件夹
    FileEntity renameFolder(Long folderId, String newName);

    // 移动文件夹
    FileEntity moveFolder(Long folderId, String targetPath);

    // 删除文件夹
    void deleteFolder(Long folderId, boolean force);

    // 批量删除文件夹
    void deleteFoldersBatch(List<Long> folderIds, boolean force);

    // 文件下载
    Resource downloadFile(Long fileId);

    // 批量下载
    Resource downloadBatch(List<Long> fileIds, String zipName);

    // 重命名文件
    FileEntity renameFile(Long fileId, String newName);

    // 移动文件
    FileEntity moveFile(Long fileId, String targetPath);

    // 复制文件
    FileEntity copyFile(Long fileId, String targetPath, String newName);

    // 删除文件
    void deleteFile(Long fileId);

    // 批量删除文件
    void deleteFilesBatch(List<Long> fileIds);

    // 预览文件
    Resource previewFile(Long fileId, String type);

    // 获取文件信息
    FileEntity getFileInfo(Long fileId);

    // 搜索文件
    List<FileEntity> searchFiles(String query, String type, String path, int page, int limit);

    // 辅助方法
    Path getFileStoragePath();
}