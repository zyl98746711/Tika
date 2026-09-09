package com.tika.rag.controller;

import com.tika.rag.model.request.ChunkingRequest;
import com.tika.rag.model.request.SearchRequest;
import com.tika.rag.model.response.SearchResponse;
import com.tika.rag.model.response.StoreResponse;
import com.tika.rag.service.SearchService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 搜索控制器 - 提供向量存储和语义搜索的 REST API
 *
 * 【REST API 设计说明】
 * 所有端点都在 /api/v1/documents 路径下，与 DocumentController 共享前缀。
 * 版本前缀 /v1/ 方便未来 API 升级时保持向后兼容。
 *
 * 【新增的 4 个端点】
 * POST   /store            文档入库（上传文件 → 解析 → 分块 → 嵌入 → 存储）
 * POST   /search           语义搜索（查询文本 → 向量搜索 → 返回匹配块）
 * DELETE /{documentId}     删除文档（移除指定文档的所有向量）
 * GET    /stats            统计信息（查看向量数据库状态）
 */
@RestController
@RequestMapping("/api/v1/documents")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 文档入库接口。
     *
     * 【consumes = MULTIPART_FORM_DATA_VALUE 的含义】
     * 表示此接口接收 multipart/form-data 格式的请求（即文件上传）。
     * 前端通过 FormData 对象提交文件。
     *
     * 【分块参数说明】
     * - strategy: 分块策略（fixedSize=固定字符数, tokenBased=按token数, hierarchical=按文档结构）
     * - chunkSize: 固定大小策略下每块的字符数
     * - overlap: 相邻块之间的重叠字符数（保证上下文连贯）
     * - tokenLimit: token策略下的每块token上限
     * - overlapTokens: token策略下的重叠token数
     * - minChunkSize: 最小块大小（太小的块会被合并）
     */
    @PostMapping(value = "/store", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoreResponse> storeDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "strategy", defaultValue = "fixedSize") String strategy,
            @RequestParam(value = "chunkSize", required = false) Integer chunkSize,
            @RequestParam(value = "overlap", required = false) Integer overlap,
            @RequestParam(value = "tokenLimit", required = false) Integer tokenLimit,
            @RequestParam(value = "overlapTokens", required = false) Integer overlapTokens,
            @RequestParam(value = "minChunkSize", required = false) Integer minChunkSize) {

        // 将请求参数封装为 ChunkingRequest 对象
        ChunkingRequest request = new ChunkingRequest(
                strategy, chunkSize, overlap, tokenLimit, overlapTokens, minChunkSize
        );

        StoreResponse response = searchService.storeDocument(file, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 语义搜索接口。
     *
     * 【与 DocumentController 的区别】
     * DocumentController 的接口是"解析"和"分块"（不存储），
     * 本控制器的接口是"入库"和"搜索"（涉及向量数据库）。
     *
     * 【请求体格式】
     * POST /search
     * Content-Type: application/json
     * Body: { "query": "什么是网络安全", "topK": 5 }
     *
     * @param request 搜索请求（query=查询文本, topK=返回数量）
     */
    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除文档接口。
     *
     * 根据文档 ID 从 Qdrant 中删除该文档的所有向量块。
     * 使用场景：重新入库前清理旧数据，或移除不再需要的文档。
     *
     * @param documentId 文档唯一标识（入库时返回的 UUID）
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable String documentId) {
        searchService.deleteDocument(documentId);
        return ResponseEntity.ok(Map.of(
                "documentId", documentId,
                "status", "deleted"
        ));
    }

    /**
     * 统计信息接口。
     *
     * 返回 Qdrant collection 的状态：
     * - pointsCount: 总点数（所有文档块的总数）
     * - vectorsCount: 总向量数
     * - vectorSize: 向量维度（768）
     * - collectionName: 集合名称
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = searchService.getStats();
        return ResponseEntity.ok(stats);
    }
}
