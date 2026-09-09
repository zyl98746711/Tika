package com.tika.rag.model.response;

import java.util.List;

/**
 * 语义搜索响应模型。
 *
 * 【响应结构】
 * {
 *   "query": "原始查询文本",
 *   "totalResults": 5,
 *   "results": [
 *     {
 *       "text": "匹配的文本块内容...",
 *       "score": 0.8234,          // 余弦相似度，越接近1越相似
 *       "documentId": "uuid",     // 来源文档ID
 *       "fileName": "文档.pdf",    // 来源文件名
 *       "chunkIndex": 3,          // 块索引
 *       "format": "application/pdf",
 *       "startOffset": 1200,      // 在原文中的起始位置
 *       "endOffset": 2000,        // 在原文中的结束位置
 *       "approximateTokenCount": 150
 *     },
 *     ...
 *   ]
 * }
 *
 * 【嵌套 record】
 * SearchResultItem 是 SearchResponse 的内部 record，
 * Java 支持在 record 中嵌套定义 record，保持代码组织紧凑。
 */
public record SearchResponse(
        String query,           // 原始查询文本（回显给前端）
        int totalResults,       // 结果总数
        List<SearchResultItem> results  // 结果列表（按相似度降序）
) {
    /**
     * 单条搜索结果。
     *
     * 【score 的含义】
     * score 是余弦相似度（Cosine Similarity），范围 [0, 1]：
     * - 1.0: 完全相同
     * - 0.7~0.9: 高度相关
     * - 0.4~0.7: 中等相关
     * - <0.3: 关联度较低
     */
    public record SearchResultItem(
            String text,              // 匹配的文本块原文
            double score,             // 相似度分数
            String documentId,        // 来源文档ID
            String fileName,          // 来源文件名
            int chunkIndex,           // 块在文档中的索引
            String format,            // 文件格式
            int startOffset,          // 文本起始偏移量
            int endOffset,            // 文本结束偏移量
            int approximateTokenCount // 近似token数
    ) {}
}
