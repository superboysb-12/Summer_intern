package com.XuebaoMaster.backend.TeachingPlanGenerator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessageResponse {
    private String event;
    private String conversation_id;
    private String message_id;
    private long created_at;
    private String task_id;
    private String id;
    private Map<String, Object> metadata;
    private List<FileInfo> files;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileInfo {
        private String dify_model_identity;
        private String id;
        private String tenant_id;
        private String type;
        private String transfer_method;
        private String remote_url;
        private String related_id;
        private String filename;
        private String extension;
        private String mime_type;
        private long size;
        private String url;

        public String getDify_model_identity() {
            return dify_model_identity;
        }

        public void setDify_model_identity(String dify_model_identity) {
            this.dify_model_identity = dify_model_identity;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTenant_id() {
            return tenant_id;
        }

        public void setTenant_id(String tenant_id) {
            this.tenant_id = tenant_id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTransfer_method() {
            return transfer_method;
        }

        public void setTransfer_method(String transfer_method) {
            this.transfer_method = transfer_method;
        }

        public String getRemote_url() {
            return remote_url;
        }

        public void setRemote_url(String remote_url) {
            this.remote_url = remote_url;
        }

        public String getRelated_id() {
            return related_id;
        }

        public void setRelated_id(String related_id) {
            this.related_id = related_id;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getExtension() {
            return extension;
        }

        public void setExtension(String extension) {
            this.extension = extension;
        }

        public String getMime_type() {
            return mime_type;
        }

        public void setMime_type(String mime_type) {
            this.mime_type = mime_type;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getConversation_id() {
        return conversation_id;
    }

    public void setConversation_id(String conversation_id) {
        this.conversation_id = conversation_id;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public long getCreated_at() {
        return created_at;
    }

    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }

    public String getTask_id() {
        return task_id;
    }

    public void setTask_id(String task_id) {
        this.task_id = task_id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<FileInfo> getFiles() {
        return files;
    }

    public void setFiles(List<FileInfo> files) {
        this.files = files;
    }
}