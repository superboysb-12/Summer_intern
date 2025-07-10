package com.XuebaoMaster.backend.RAG;
import java.util.List;
import java.util.Map;
public class RAGResponse {
    private boolean success;
    private String message;
    private Object data;
    private String errorDetails;
    public RAGResponse() {
    }
    public RAGResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    public RAGResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    public RAGResponse(boolean success, String message, String errorDetails) {
        this.success = success;
        this.message = message;
        this.errorDetails = errorDetails;
    }
    public static RAGResponse success(String message) {
        return new RAGResponse(true, message);
    }
    public static RAGResponse success(String message, Object data) {
        return new RAGResponse(true, message, data);
    }
    public static RAGResponse error(String message) {
        return new RAGResponse(false, message);
    }
    public static RAGResponse error(String message, String errorDetails) {
        return new RAGResponse(false, message, errorDetails);
    }
    public boolean isSuccess() {
        return success;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }
    public String getErrorDetails() {
        return errorDetails;
    }
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}
