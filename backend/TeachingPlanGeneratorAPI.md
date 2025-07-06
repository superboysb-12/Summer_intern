# 教学计划生成器 API 文档

## 概述

教学计划生成器模块提供了一个 API 接口，用于根据用户提供的查询生成教学计划文档。该模块会将生成的教学计划作为文件保存在系统中，并返回相应的文件实体信息。

## API 端点

### 生成教学计划

生成一个基于提供的查询参数的教学计划文档。

**URL:** `/api/teaching-plan/generate`

**方法:** `POST`

**请求体:**
```json
{
  "query": "string",         // 必填，用于生成教学计划的查询内容
  "conversationId": "string", // 可选，会话ID，用于跟踪对话上下文
  "user": "string"           // 可选，用户标识，默认为"system"
}
```

**请求参数说明:**
- `query`: 必填字段，包含用于生成教学计划的查询内容或指令
- `conversationId`: 可选字段，用于维护与AI服务的对话上下文
- `user`: 可选字段，指定请求的用户标识，默认为"system"

**响应:**
```json
{
  "id": "long",              // 文件实体ID
  "fileName": "string",      // 文件名称
  "filePath": "string",      // 文件相对路径
  "mimeType": "string",      // 文件MIME类型
  "fileSize": "long",        // 文件大小（字节）
  "directory": "boolean",    // 是否为目录（始终为false）
  "parentPath": "string"     // 父目录路径
}
```

**响应状态码:**
- `200 OK`: 请求成功，返回生成的教学计划文件实体
- `400 Bad Request`: 请求参数无效
- `500 Internal Server Error`: 服务器内部错误

## 实现细节

该API通过以下流程生成教学计划：

1. 接收用户的查询请求
2. 将请求转发到配置的AI服务（通过`chat.api.url`配置）
3. 处理AI服务返回的流式响应
4. 下载AI生成的教学计划文件
5. 将文件保存到系统中（保存路径：`/teaching_plans/`）
6. 在数据库中创建对应的文件实体记录
7. 返回文件实体信息给客户端

## 示例

### 请求示例

```http
POST /api/teaching-plan/generate HTTP/1.1
Host: example.com
Content-Type: application/json

{
  "query": "为高中一年级学生设计一个为期两周的物理力学入门教学计划",
  "conversationId": "conv_123456",
  "user": "teacher1"
}
```

### 响应示例

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 42,
  "fileName": "物理力学入门教学计划.docx",
  "filePath": "/teaching_plans/物理力学入门教学计划.docx",
  "mimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "fileSize": 24680,
  "directory": false,
  "parentPath": "/teaching_plans/"
}
```

## 错误处理

当生成教学计划过程中出现错误时，API将返回适当的HTTP状态码和错误信息。常见错误包括：

- 无效的请求参数
- AI服务连接失败
- 文件下载或保存失败
- 数据库操作失败

错误响应将包含描述性的错误消息，以帮助客户端识别和解决问题。 