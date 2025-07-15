package com.XuebaoMaster.backend.File;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByParentPathOrderByIsDirectoryDescFileNameAsc(String parentPath);

    Optional<FileEntity> findByFilePathAndFileName(String filePath, String fileName);

    // 查找以指定路径开头的所有文件和文件夹
    List<FileEntity> findByFilePathStartingWith(String filePath);

    // 查找指定MIME类型和路径的文件
    List<FileEntity> findByMimeTypeStartingWithAndParentPathStartingWith(String mimeType, String parentPath);

    // 根据文件名模糊查询非目录文件
    List<FileEntity> findByFileNameContainingIgnoreCaseAndIsDirectory(String fileName, boolean isDirectory);
}
