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
    List<FileEntity> findByFileNameContainingIgnoreCaseAndIsDirectory(String fileName, boolean isDirectory);
    @Query("SELECT f FROM FileEntity f WHERE f.parentPath LIKE :path% OR f.parentPath = :path")
    List<FileEntity> findAllByPathStartingWith(String path);
    List<FileEntity> findByMimeTypeStartingWithAndParentPathStartingWith(String mimeType, String parentPath);
}
