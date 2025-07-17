package com.XuebaoMaster.backend.RAG;

import com.XuebaoMaster.backend.Course.Course;
import com.XuebaoMaster.backend.File.FileEntity;
import com.XuebaoMaster.backend.File.FileService;
import com.XuebaoMaster.backend.RAG.Course.CourseRagMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RAGController {

    @Autowired
    private RAGService ragService;

    @Autowired
    private FileService fileService;

    @Autowired
    private CourseRagMappingService courseRagMappingService;

    /**
     * 创建新RAG（手动添加）
     */
    @PostMapping
    public ResponseEntity<?> createRAG(@RequestBody RAG rag) {
        RAG savedRag = ragService.saveRAG(rag);
        return ResponseEntity.ok(RAGResponse.success("RAG创建成功", savedRag));
    }

    /**
     * 获取所有RAG
     */
    @GetMapping
    public ResponseEntity<?> getAllRAGs() {
        List<RAG> rags = ragService.getAllRAGs();
        return ResponseEntity.ok(RAGResponse.success("获取成功", rags));
    }

    /**
     * 通过ID获取RAG
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRAGById(@PathVariable Long id) {
        return ragService.getRAGById(id)
                .map(rag -> ResponseEntity.ok(RAGResponse.success("获取成功", rag)))
                .orElse(ResponseEntity.ok(RAGResponse.error("找不到指定ID的RAG")));
    }

    /**
     * 通过名称获取RAG
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<?> getRAGByName(@PathVariable String name) {
        return ragService.getRAGByName(name)
                .map(rag -> ResponseEntity.ok(RAGResponse.success("获取成功", rag)))
                .orElse(ResponseEntity.ok(RAGResponse.error("找不到指定名称的RAG")));
    }

    /**
     * 删除RAG
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRAG(@PathVariable Long id) {
        try {
            ragService.deleteRAG(id);
            return ResponseEntity.ok(RAGResponse.success("RAG删除成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(RAGResponse.error("删除RAG时发生错误: " + e.getMessage()));
        }
    }

    /**
     * 生成RAG数据（同步方法）
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateRAG(
            @RequestParam String sourceDir,
            @RequestParam String ragName,
            @RequestParam(defaultValue = "false") boolean forceRebuild) {

        // 确保sourceDir是绝对路径
        File sourceDirFile = new File(sourceDir);
        if (!sourceDirFile.isAbsolute()) {
            sourceDirFile = sourceDirFile.getAbsoluteFile();
            sourceDir = sourceDirFile.getAbsolutePath();
        }

        RAGResponse response = ragService.generateRAG(sourceDir, ragName, forceRebuild);
        return ResponseEntity.ok(response);
    }

    /**
     * 生成RAG数据（异步方法）
     */
    @PostMapping("/generate/async")
    public ResponseEntity<?> generateRAGAsync(
            @RequestParam String sourceDir,
            @RequestParam String ragName,
            @RequestParam(defaultValue = "false") boolean forceRebuild) {

        // 确保sourceDir是绝对路径
        File sourceDirFile = new File(sourceDir);
        if (!sourceDirFile.isAbsolute()) {
            sourceDirFile = sourceDirFile.getAbsoluteFile();
            sourceDir = sourceDirFile.getAbsolutePath();
        }

        RAGResponse response = ragService.generateRAGAsync(sourceDir, ragName, forceRebuild);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询RAG生成任务状态
     */
    @GetMapping("/generation-status/{id}")
    public ResponseEntity<?> checkRAGGenerationStatus(@PathVariable Long id) {
        RAGResponse response = ragService.checkRAGGenerationStatus(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 从文件系统中的文件夹生成RAG数据（同步方法）
     */
    @PostMapping("/generate/from-file")
    public ResponseEntity<?> generateRAGFromFile(
            @RequestParam Long folderId,
            @RequestParam String ragName,
            @RequestParam(defaultValue = "false") boolean forceRebuild) {

        try {
            // 获取文件夹信息
            FileEntity folder = fileService.getFileInfo(folderId);

            if (!folder.isDirectory()) {
                return ResponseEntity.ok(RAGResponse.error("所选项不是文件夹"));
            }

            // 获取文件存储的根路径
            Path fileStoragePath = fileService.getFileStoragePath();

            // 构建文件夹的绝对路径
            Path absoluteFolderPath = fileStoragePath.resolve(folder.getFilePath().substring(1));
            String sourceDir = absoluteFolderPath.toString();

            // 生成RAG
            RAGResponse response = ragService.generateRAG(sourceDir, ragName, forceRebuild);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.ok(RAGResponse.error("从文件夹生成RAG数据时发生错误: " + e.getMessage()));
        }
    }

    /**
     * 从文件系统中的文件夹异步生成RAG数据
     */
    @PostMapping("/generate/from-file/async")
    public ResponseEntity<?> generateRAGFromFileAsync(
            @RequestParam Long folderId,
            @RequestParam String ragName,
            @RequestParam(defaultValue = "false") boolean forceRebuild) {

        try {
            // 获取文件夹信息
            FileEntity folder = fileService.getFileInfo(folderId);

            if (!folder.isDirectory()) {
                return ResponseEntity.ok(RAGResponse.error("所选项不是文件夹"));
            }

            // 获取文件存储的根路径
            Path fileStoragePath = fileService.getFileStoragePath();

            // 构建文件夹的绝对路径
            Path absoluteFolderPath = fileStoragePath.resolve(folder.getFilePath().substring(1));
            String sourceDir = absoluteFolderPath.toString();

            // 异步生成RAG
            RAGResponse response = ragService.generateRAGAsync(sourceDir, ragName, forceRebuild);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.ok(RAGResponse.error("从文件夹生成RAG数据时发生错误: " + e.getMessage()));
        }
    }

    /**
     * 执行RAG查询（通过ID）
     */
    @GetMapping("/query/{id}")
    public ResponseEntity<?> queryRAG(
            @PathVariable Long id,
            @RequestParam String query,
            @RequestParam(required = false) Integer topK,
            @RequestParam(required = false) Boolean includeGraphContext,
            @RequestParam(required = false) Integer contextDepth) {

        RAGResponse response = ragService.performQuery(query, id, topK, includeGraphContext, contextDepth);
        return ResponseEntity.ok(response);
    }

    /**
     * 执行RAG查询（通过名称）
     */
    @GetMapping("/query/name/{name}")
    public ResponseEntity<?> queryRAGByName(
            @PathVariable String name,
            @RequestParam String query,
            @RequestParam(required = false) Integer topK,
            @RequestParam(required = false) Boolean includeGraphContext,
            @RequestParam(required = false) Integer contextDepth) {

        RAGResponse response = ragService.performQueryByName(query, name, topK, includeGraphContext, contextDepth);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取知识图谱数据（通过ID）
     */
    @GetMapping("/knowledge-graph/{id}")
    public ResponseEntity<?> getKnowledgeGraph(@PathVariable Long id) {
        RAGResponse response = ragService.getKnowledgeGraph(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取知识图谱数据（通过名称）
     */
    @GetMapping("/knowledge-graph/name/{name}")
    public ResponseEntity<?> getKnowledgeGraphByName(@PathVariable String name) {
        RAGResponse response = ragService.getKnowledgeGraphByName(name);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取文件系统中的文件夹列表，用于选择RAG生成的源目录
     */
    @GetMapping("/folders")
    public ResponseEntity<?> getFolders() {
        try {
            List<FileEntity> folders = fileService.getFileList("/", false);
            return ResponseEntity.ok(RAGResponse.success("获取文件夹列表成功", folders));
        } catch (Exception e) {
            return ResponseEntity.ok(RAGResponse.error("获取文件夹列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定路径下的文件夹列表
     */
    @GetMapping("/folders/{path}")
    public ResponseEntity<?> getFoldersByPath(@PathVariable String path) {
        try {
            String normalizedPath = "/" + path;
            if (!normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath + "/";
            }

            List<FileEntity> folders = fileService.getFileList(normalizedPath, false);
            return ResponseEntity.ok(RAGResponse.success("获取文件夹列表成功", folders));
        } catch (Exception e) {
            return ResponseEntity.ok(RAGResponse.error("获取文件夹列表失败: " + e.getMessage()));
        }
    }

    /**
     * 修复知识图谱路径（当路径结构有误时使用）
     */
    @PostMapping("/fix-knowledge-graph-path/{id}")
    public ResponseEntity<?> fixKnowledgeGraphPath(@PathVariable Long id) {
        try {
            RAGResponse response = ragService.fixKnowledgeGraphPath(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(RAGResponse.error("修复知识图谱路径失败: " + e.getMessage()));
        }
    }

    /**
     * 批量修复所有知识图谱路径
     */
    @PostMapping("/fix-all-knowledge-graph-paths")
    public ResponseEntity<?> fixAllKnowledgeGraphPaths() {
        try {
            RAGResponse response = ragService.fixAllKnowledgeGraphPaths();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(RAGResponse.error("批量修复知识图谱路径失败: " + e.getMessage()));
        }
    }

    /**
     * 获取与指定课程关联的所有RAG
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getRAGsByCourseId(@PathVariable Long courseId) {
        try {
            List<RAG> rags = courseRagMappingService.findRagsByCourseId(courseId);
            return ResponseEntity.ok(RAGResponse.success("获取成功", rags));
        } catch (Exception e) {
            return ResponseEntity.ok(RAGResponse.error("获取课程RAG失败: " + e.getMessage()));
        }
    }

    /**
     * 为课程添加关联的RAG
     */
    @PostMapping("/course/{courseId}/add/{ragId}")
    public ResponseEntity<?> addRAGToCourse(@PathVariable Long courseId, @PathVariable Long ragId) {
        try {
            // 检查映射是否已存在
            if (courseRagMappingService.mappingExists(courseId, ragId)) {
                return ResponseEntity.ok(RAGResponse.error("该RAG已与课程关联"));
            }

            courseRagMappingService.createMapping(courseId, ragId);
            return ResponseEntity.ok(RAGResponse.success("RAG成功关联到课程"));
        } catch (Exception e) {
            return ResponseEntity.ok(RAGResponse.error("关联RAG到课程失败: " + e.getMessage()));
        }
    }

    /**
     * 从课程中移除关联的RAG
     */
    @DeleteMapping("/course/{courseId}/remove/{ragId}")
    public ResponseEntity<?> removeRAGFromCourse(@PathVariable Long courseId, @PathVariable Long ragId) {
        try {
            // 检查映射是否存在
            if (!courseRagMappingService.mappingExists(courseId, ragId)) {
                return ResponseEntity.ok(RAGResponse.error("该RAG未与课程关联"));
            }

            courseRagMappingService.deleteMappingByCourseAndRag(courseId, ragId);
            return ResponseEntity.ok(RAGResponse.success("RAG成功从课程移除"));
        } catch (Exception e) {
            return ResponseEntity.ok(RAGResponse.error("从课程移除RAG失败: " + e.getMessage()));
        }
    }

    /**
     * 检查RAG是否与课程关联
     */
    @GetMapping("/course/{courseId}/has-rag/{ragId}")
    public ResponseEntity<?> checkCourseHasRAG(@PathVariable Long courseId, @PathVariable Long ragId) {
        try {
            boolean exists = courseRagMappingService.mappingExists(courseId, ragId);
            Map<String, Boolean> result = new HashMap<>();
            result.put("exists", exists);
            return ResponseEntity.ok(RAGResponse.success("检查成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(RAGResponse.error("检查关联失败: " + e.getMessage()));
        }
    }
}