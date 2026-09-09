package com.tika.rag.model.request;

/**
 * 语义搜索请求模型。
 *
 * 【record 是什么？】
 * Java 14+ 引入的 record 是一种不可变数据载体，自动生成构造函数、getter、equals、hashCode、toString。
 * 比 class 更简洁，适合做 DTO（数据传输对象）。
 *
 * 【字段说明】
 * - query: 用户的查询文本（自然语言，如"什么是机器学习"）
 * - topK: 返回最相似的前 K 个结果（默认 5）
 *
 * 【请求示例】
 * POST /api/v1/documents/search
 * Content-Type: application/json
 * { "query": "网络安全检查要求", "topK": 10 }
 */
public record SearchRequest(
        String query,
        Integer topK
) {
    /**
     * 获取有效的 topK 值。
     * 如果未指定或无效，返回默认值 5。
     *
     * 【为什么需要 effective 方法？】
     * 前端可能不传 topK，或传了非法值（如负数），
     * 这里做防御性编程，确保总有一个合理的默认值。
     */
    public int effectiveTopK() {
        return topK != null && topK > 0 ? topK : 5;
    }
}
