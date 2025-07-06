package com.XuebaoMaster.backend.File.impl;
import com.XuebaoMaster.backend.File.FileEntity;
import com.XuebaoMaster.backend.File.FileRepository;
import com.XuebaoMaster.backend.File.FileService;
import com.XuebaoMaster.backend.config.FileStorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
@Service
public class FileServiceImpl implements FileService {
    private final Path fileStorageLocation;
    private final FileRepository fileRepository;
    @Autowired
    public FileServiceImpl(FileStorageProperties fileStorageProperties, FileRepository fileRepository) {
        this.fileRepository = fileRepository;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }
    @Override
    public Map<String, Object> uploadFiles(MultipartFile[] files, String path, boolean overwrite) {
        Map<String, Object> result = new HashMap<>();
        List<FileEntity> uploadedFiles = new ArrayList<>();
        List<String> failedUploads = new ArrayList<>();
        String uploadPath = path != null ? path : "/";
        Path targetLocation = this.fileStorageLocation.resolve(uploadPath.substring(1));
        try {
            Files.createDirectories(targetLocation);
            for (MultipartFile file : files) {
                try {
                    String fileName = StringUtils.cleanPath(file.getOriginalFilename());
                    if (fileName.contains("..")) {
                        failedUploads.add(fileName + ": Filename contains invalid path sequence");
                        continue;
                    }
                    Path filePath = targetLocation.resolve(fileName);
                    Optional<FileEntity> existingFile = fileRepository.findByFilePathAndFileName(
                            uploadPath, fileName);
                    if (existingFile.isPresent() && !overwrite) {
                        failedUploads.add(fileName + ": File already exists");
                        continue;
                    }
                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    FileEntity fileEntity = existingFile.orElse(new FileEntity());
                    fileEntity.setFileName(fileName);
                    fileEntity.setFilePath(uploadPath);
                    fileEntity.setMimeType(file.getContentType());
                    fileEntity.setFileSize(file.getSize());
                    fileEntity.setDirectory(false);
                    fileEntity.setParentPath(uploadPath);
                    uploadedFiles.add(fileRepository.save(fileEntity));
                } catch (Exception ex) {
                    failedUploads.add(file.getOriginalFilename() + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not create upload directory", ex);
        }
        result.put("uploadedFiles", uploadedFiles);
        result.put("failedUploads", failedUploads);
        return result;
    }
    @Override
    public FileEntity createFolder(String folderName, String parentPath) {
        if (folderName == null || folderName.isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be empty");
        }
        String normalizedParentPath = parentPath != null ? parentPath : "/";
        if (!normalizedParentPath.startsWith("/")) {
            normalizedParentPath = "/" + normalizedParentPath;
        }
        if (!normalizedParentPath.endsWith("/")) {
            normalizedParentPath = normalizedParentPath + "/";
        }
        Optional<FileEntity> existingFolder = fileRepository.findByFilePathAndFileName(
                normalizedParentPath, folderName);
        if (existingFolder.isPresent()) {
            throw new RuntimeException("Folder already exists");
        }
        Path folderPath = this.fileStorageLocation.resolve(
                normalizedParentPath.substring(1) + folderName);
        try {
            Files.createDirectories(folderPath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create folder", ex);
        }
        FileEntity folder = new FileEntity();
        folder.setFileName(folderName);
        folder.setFilePath(normalizedParentPath + folderName + "/");
        folder.setMimeType("directory");
        folder.setFileSize(0L);
        folder.setDirectory(true);
        folder.setParentPath(normalizedParentPath);
        return fileRepository.save(folder);
    }
    @Override
    public List<FileEntity> getFileList(String path, boolean includeFiles) {
        String normalizedPath = path != null ? path : "/";
        if (!normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath + "/";
        }
        List<FileEntity> entries = fileRepository.findByParentPathOrderByIsDirectoryDescFileNameAsc(normalizedPath);
        if (!includeFiles) {
            entries = entries.stream()
                    .filter(FileEntity::isDirectory)
                    .collect(Collectors.toList());
        }
        return entries;
    }
    @Override
    public FileEntity renameFolder(Long folderId, String newName) {
        FileEntity folder = fileRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        if (!folder.isDirectory()) {
            throw new RuntimeException("Not a folder");
        }
        Optional<FileEntity> existingFolder = fileRepository.findByFilePathAndFileName(
                folder.getParentPath(), newName);
        if (existingFolder.isPresent()) {
            throw new RuntimeException("A folder with this name already exists");
        }
        Path oldPath = this.fileStorageLocation.resolve(folder.getFilePath().substring(1));
        Path newPath = oldPath.getParent().resolve(newName);
        try {
            Files.move(oldPath, newPath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not rename folder", ex);
        }
        String oldFolderPath = folder.getFilePath();
        String newFolderPath = folder.getParentPath() + newName + "/";
        folder.setFileName(newName);
        folder.setFilePath(newFolderPath);
        fileRepository.save(folder);
        List<FileEntity> children = fileRepository.findAllByPathStartingWith(oldFolderPath);
        for (FileEntity child : children) {
            String newChildPath = child.getFilePath().replace(oldFolderPath, newFolderPath);
            String newParentPath = child.getParentPath().replace(oldFolderPath, newFolderPath);
            child.setFilePath(newChildPath);
            child.setParentPath(newParentPath);
            fileRepository.save(child);
        }
        return folder;
    }
    @Override
    public FileEntity moveFolder(Long folderId, String targetPath) {
        FileEntity folder = fileRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        if (!folder.isDirectory()) {
            throw new RuntimeException("Not a folder");
        }
        String normalizedTargetPath = targetPath;
        if (!normalizedTargetPath.endsWith("/")) {
            normalizedTargetPath = normalizedTargetPath + "/";
        }
        Optional<FileEntity> targetFolder = fileRepository.findByFilePathAndFileName(
                normalizedTargetPath, folder.getFileName());
        if (targetFolder.isPresent()) {
            throw new RuntimeException("A folder with the same name already exists at the target location");
        }
        Path sourcePath = this.fileStorageLocation.resolve(folder.getFilePath().substring(1));
        Path targetLocationPath = this.fileStorageLocation.resolve(normalizedTargetPath.substring(1));
        Path destinationPath = targetLocationPath.resolve(folder.getFileName());
        try {
            Files.createDirectories(targetLocationPath);
            Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not move folder", ex);
        }
        String oldFolderPath = folder.getFilePath();
        String newFolderPath = normalizedTargetPath + folder.getFileName() + "/";
        folder.setFilePath(newFolderPath);
        folder.setParentPath(normalizedTargetPath);
        fileRepository.save(folder);
        List<FileEntity> children = fileRepository.findAllByPathStartingWith(oldFolderPath);
        for (FileEntity child : children) {
            String newChildPath = child.getFilePath().replace(oldFolderPath, newFolderPath);
            String newParentPath = child.getParentPath().replace(oldFolderPath, newFolderPath);
            child.setFilePath(newChildPath);
            child.setParentPath(newParentPath);
            fileRepository.save(child);
        }
        return folder;
    }
    @Override
    public void deleteFolder(Long folderId, boolean force) {
        FileEntity folder = fileRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        if (!folder.isDirectory()) {
            throw new RuntimeException("Not a folder");
        }
        List<FileEntity> children = fileRepository
                .findByParentPathOrderByIsDirectoryDescFileNameAsc(folder.getFilePath());
        if (!children.isEmpty() && !force) {
            throw new RuntimeException("Folder is not empty");
        }
        Path folderPath = this.fileStorageLocation.resolve(folder.getFilePath().substring(1));
        try {
            deleteDirectoryRecursively(folderPath.toFile());
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete folder", ex);
        }
        List<FileEntity> allChildren = fileRepository.findAllByPathStartingWith(folder.getFilePath());
        fileRepository.deleteAll(allChildren);
        fileRepository.delete(folder);
    }
    private void deleteDirectoryRecursively(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectoryRecursively(file);
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete " + directory);
        }
    }
    @Override
    public void deleteFoldersBatch(List<Long> folderIds, boolean force) {
        for (Long folderId : folderIds) {
            deleteFolder(folderId, force);
        }
    }
    @Override
    public Resource downloadFile(Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        if (file.isDirectory()) {
            throw new RuntimeException("Cannot download a folder");
        }
        try {
            Path filePath = this.fileStorageLocation.resolve(
                    file.getFilePath().substring(1) + file.getFileName());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found");
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found", ex);
        }
    }
    @Override
    public Resource downloadBatch(List<Long> fileIds, String zipName) {
        if (zipName == null || zipName.isEmpty()) {
            zipName = "download.zip";
        }
        try {
            Path tempFile = Files.createTempFile("download_", ".zip");
            ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(tempFile.toFile()));
            for (Long fileId : fileIds) {
                FileEntity file = fileRepository.findById(fileId)
                        .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
                if (file.isDirectory()) {
                    continue; 
                }
                Path filePath = this.fileStorageLocation.resolve(
                        file.getFilePath().substring(1) + file.getFileName());
                if (Files.exists(filePath)) {
                    ZipEntry zipEntry = new ZipEntry(file.getFileName());
                    zipOut.putNextEntry(zipEntry);
                    Files.copy(filePath, zipOut);
                    zipOut.closeEntry();
                }
            }
            zipOut.close();
            return new UrlResource(tempFile.toUri());
        } catch (IOException ex) {
            throw new RuntimeException("Could not create zip file", ex);
        }
    }
    @Override
    public FileEntity renameFile(Long fileId, String newName) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        if (file.isDirectory()) {
            throw new RuntimeException("Use renameFolder for directories");
        }
        Optional<FileEntity> existingFile = fileRepository.findByFilePathAndFileName(
                file.getParentPath(), newName);
        if (existingFile.isPresent()) {
            throw new RuntimeException("A file with this name already exists");
        }
        Path oldPath = this.fileStorageLocation.resolve(
                file.getParentPath().substring(1) + file.getFileName());
        Path newPath = oldPath.getParent().resolve(newName);
        try {
            Files.move(oldPath, newPath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not rename file", ex);
        }
        file.setFileName(newName);
        return fileRepository.save(file);
    }
    @Override
    public FileEntity moveFile(Long fileId, String targetPath) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        if (file.isDirectory()) {
            throw new RuntimeException("Use moveFolder for directories");
        }
        String normalizedTargetPath = targetPath;
        if (!normalizedTargetPath.endsWith("/")) {
            normalizedTargetPath = normalizedTargetPath + "/";
        }
        Optional<FileEntity> targetFile = fileRepository.findByFilePathAndFileName(
                normalizedTargetPath, file.getFileName());
        if (targetFile.isPresent()) {
            throw new RuntimeException("A file with the same name already exists at the target location");
        }
        Path sourcePath = this.fileStorageLocation.resolve(
                file.getParentPath().substring(1) + file.getFileName());
        Path targetLocationPath = this.fileStorageLocation.resolve(normalizedTargetPath.substring(1));
        Path destinationPath = targetLocationPath.resolve(file.getFileName());
        try {
            Files.createDirectories(targetLocationPath);
            Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not move file", ex);
        }
        file.setParentPath(normalizedTargetPath);
        file.setFilePath(normalizedTargetPath);
        return fileRepository.save(file);
    }
    @Override
    public FileEntity copyFile(Long fileId, String targetPath, String newName) {
        FileEntity sourceFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        if (sourceFile.isDirectory()) {
            throw new RuntimeException("Cannot copy directories");
        }
        String normalizedTargetPath = targetPath;
        if (!normalizedTargetPath.endsWith("/")) {
            normalizedTargetPath = normalizedTargetPath + "/";
        }
        String fileName = newName != null && !newName.isEmpty() ? newName : sourceFile.getFileName();
        Optional<FileEntity> existingFile = fileRepository.findByFilePathAndFileName(
                normalizedTargetPath, fileName);
        if (existingFile.isPresent()) {
            throw new RuntimeException("A file with this name already exists at the target location");
        }
        Path sourcePath = this.fileStorageLocation.resolve(
                sourceFile.getParentPath().substring(1) + sourceFile.getFileName());
        Path targetLocationPath = this.fileStorageLocation.resolve(normalizedTargetPath.substring(1));
        Path destinationPath = targetLocationPath.resolve(fileName);
        try {
            Files.createDirectories(targetLocationPath);
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not copy file", ex);
        }
        FileEntity newFile = new FileEntity();
        newFile.setFileName(fileName);
        newFile.setFilePath(normalizedTargetPath);
        newFile.setMimeType(sourceFile.getMimeType());
        newFile.setFileSize(sourceFile.getFileSize());
        newFile.setDirectory(false);
        newFile.setParentPath(normalizedTargetPath);
        return fileRepository.save(newFile);
    }
    @Override
    public void deleteFile(Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        if (file.isDirectory()) {
            throw new RuntimeException("Use deleteFolder for directories");
        }
        Path filePath = this.fileStorageLocation.resolve(
                file.getParentPath().substring(1) + file.getFileName());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file", ex);
        }
        fileRepository.delete(file);
    }
    @Override
    public void deleteFilesBatch(List<Long> fileIds) {
        for (Long fileId : fileIds) {
            deleteFile(fileId);
        }
    }
    @Override
    public Resource previewFile(Long fileId, String type) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        if (file.isDirectory()) {
            throw new RuntimeException("Cannot preview a directory");
        }
        try {
            Path filePath = this.fileStorageLocation.resolve(
                    file.getParentPath().substring(1) + file.getFileName());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found");
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found", ex);
        }
    }
    @Override
    public FileEntity getFileInfo(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }
    @Override
    public List<FileEntity> searchFiles(String query, String type, String path, int page, int limit) {
        String searchPath = path != null ? path : "/";
        if (type != null && !type.isEmpty()) {
            return fileRepository.findByMimeTypeStartingWithAndParentPathStartingWith(type + "/", searchPath);
        } else {
            return fileRepository.findByFileNameContainingIgnoreCaseAndIsDirectory(query, false);
        }
    }
    @Override
    public Path getFileStoragePath() {
        return this.fileStorageLocation;
    }
}
