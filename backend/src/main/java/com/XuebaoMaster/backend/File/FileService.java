package com.XuebaoMaster.backend.File;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface FileService {
    Map<String, Object> uploadFiles(MultipartFile[] files, String path, boolean overwrite);

    FileEntity createFolder(String folderName, String parentPath);

    List<FileEntity> getFileList(String path, boolean includeFiles);

    FileEntity renameFolder(Long folderId, String newName);

    FileEntity moveFolder(Long folderId, String targetPath);

    void deleteFolder(Long folderId, boolean force);

    void deleteFoldersBatch(List<Long> folderIds, boolean force);

    Resource downloadFile(Long fileId);

    Resource downloadBatch(List<Long> fileIds, String zipName);

    FileEntity renameFile(Long fileId, String newName);

    FileEntity moveFile(Long fileId, String targetPath);

    FileEntity copyFile(Long fileId, String targetPath, String newName);

    void deleteFile(Long fileId);

    void deleteFilesBatch(List<Long> fileIds);

    Resource previewFile(Long fileId, String type);

    FileEntity getFileInfo(Long fileId);

    List<FileEntity> searchFiles(String query, String type, String path, int page, int limit);

    Path getFileStoragePath();
}
