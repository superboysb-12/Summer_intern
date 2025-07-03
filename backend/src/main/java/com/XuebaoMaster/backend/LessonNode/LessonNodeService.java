package com.XuebaoMaster.backend.LessonNode;

import java.util.List;
import java.util.Map;

public interface LessonNodeService {
    LessonNode createLessonNode(LessonNode lessonNode);

    LessonNode updateLessonNode(LessonNode lessonNode);

    void deleteLessonNode(Long id);

    LessonNode getLessonNodeById(Long id);

    List<LessonNode> getLessonNodesByCourseId(Long courseId);
    
    List<LessonNode> getLessonNodesByCourseIdOrdered(Long courseId);
    
    LessonNode getLessonNodeByCourseIdAndOrder(Long courseId, Integer nodeOrder);

    List<LessonNode> getAllLessonNodes();

    List<LessonNode> searchLessonNodes(String keyword);
    
    /**
     * 向RAG服务发送生成请求
     * @param sourcePath 源文件路径
     * @return 请求结果
     */
    String generateRag(String sourcePath);
    
    /**
     * 向RAG服务发送查询请求
     * @param query 查询内容
     * @param ragPath RAG数据路径
     * @return 查询结果
     */
    String queryRag(String query, String ragPath);
    
    /**
     * 发送聊天消息
     * @param message 消息内容
     * @param additionalParams 附加参数
     * @return 聊天响应
     */
    String sendChatMessage(String message, Map<String, Object> additionalParams);
} 