package com.tika.rag.model.response;

/**
 * 文档入库响应模型。
 *
 * 【响应示例】
 * {
 *   "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
 *   "fileName": "网络安全法.pdf",
 *   "collectionName": "documents",
 *   "totalChunks": 42,
 *   "status": "stored"
 * }
 *
 * 【字段说明】
 * - documentId: 文档唯一标识（UUID），用于后续删除操作
 * - fileName: 原始文件名
 * - collectionName: 存入的 Qdrant 集合名称
 * - totalChunks: 文档被切分成的块数
 * - status: 操作状态（"stored" 表示成功存储）
 */
public record StoreResponse(
        String documentId,      // 文档唯一标识（UUID v4）
        String fileName,        // 原始文件名
        String collectionName,  // Qdrant 集合名
        int totalChunks,        // 分块数量
        String status           // 操作状态
) {
}
