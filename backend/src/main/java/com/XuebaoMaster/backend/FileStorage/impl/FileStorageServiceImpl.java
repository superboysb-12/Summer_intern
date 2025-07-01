package com.XuebaoMaster.backend.FileStorage.impl;

import com.XuebaoMaster.backend.FileStorage.FileNotFoundException;
import com.XuebaoMaster.backend.FileStorage.FileStorageException;
import com.XuebaoMaster.backend.FileStorage.FileStorageService;
import com.XuebaoMaster.backend.config.FileStorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    @Autowired
    public FileStorageServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("无法创建文件存储目录", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        // 清理文件名
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            // 检查文件名是否包含无效字符
            if (originalFileName.contains("..")) {
                throw new FileStorageException("文件名包含无效的路径序列 " + originalFileName);
            }

            // 生成唯一文件名（使用时间戳）
            String fileExtension = "";
            if (originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            String newFileName = timestamp + fileExtension;

            // 复制文件到目标位置
            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return newFileName;
        } catch (IOException ex) {
            throw new FileStorageException("无法存储文件 " + originalFileName, ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileNotFoundException("文件未找到: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileNotFoundException("文件未找到: " + fileName, ex);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.fileStorageLocation, 1)
                    .filter(path -> !path.equals(this.fileStorageLocation))
                    .map(this.fileStorageLocation::relativize);
        } catch (IOException ex) {
            throw new FileStorageException("无法读取存储的文件", ex);
        }
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            boolean deleted = Files.deleteIfExists(filePath);
            if (!deleted) {
                throw new FileNotFoundException("文件未找到: " + fileName);
            }
        } catch (IOException ex) {
            throw new FileStorageException("无法删除文件: " + fileName, ex);
        }
    }
} 