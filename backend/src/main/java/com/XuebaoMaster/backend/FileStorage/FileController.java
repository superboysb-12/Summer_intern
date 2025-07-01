package com.XuebaoMaster.backend.FileStorage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件上传下载控制器
 * 提供文件上传、下载、列表和删除功能
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 上传单个文件
     * 
     * @param file 要上传的文件
     * @return 文件上传响应信息
     */
    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/download/")
                .path(fileName)
                .toUriString();

        FileResponse fileResponse = new FileResponse(
                fileName,
                fileDownloadUri,
                file.getContentType(),
                file.getSize());

        return ResponseEntity.ok().body(fileResponse);
    }

    /**
     * 上传多个文件
     * 
     * @param files 要上传的文件列表
     * @return 多个文件上传响应信息
     */
    @PostMapping("/uploadMultiple")
    public ResponseEntity<List<FileResponse>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        List<FileResponse> responses = Arrays.stream(files)
                .map(file -> {
                    String fileName = fileStorageService.storeFile(file);
                    String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/files/download/")
                            .path(fileName)
                            .toUriString();
                    return new FileResponse(
                            fileName,
                            fileDownloadUri,
                            file.getContentType(),
                            file.getSize());
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok().body(responses);
    }

    /**
     * 下载文件
     * 
     * @param fileName 文件名
     * @param request HTTP请求
     * @return 文件资源
     */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // 加载文件作为Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // 尝试确定文件的内容类型
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // 如果无法确定内容类型，则不指定内容类型
        }

        // 如果无法确定内容类型，则默认为二进制流
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * 获取所有文件列表
     * 
     * @return 文件列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles() {
        List<String> fileNames = fileStorageService.loadAll()
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());

        return ResponseEntity.ok().body(fileNames);
    }

    /**
     * 删除文件
     * 
     * @param fileName 文件名
     * @return 删除结果
     */
    @DeleteMapping("/delete/{fileName:.+}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        fileStorageService.deleteFile(fileName);
        return ResponseEntity.ok().body("文件 " + fileName + " 已成功删除");
    }
} 