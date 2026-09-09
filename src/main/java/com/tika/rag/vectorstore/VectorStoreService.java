package com.tika.rag.vectorstore;

import com.tika.rag.config.QdrantConfig;
import com.tika.rag.model.DocumentChunk;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * 向量存储服务 - 封装与 Qdrant 向量数据库的所有交互操作
 *
 * 【Qdrant 核心概念】
 * - Collection（集合）: 类似关系数据库的"表"，存储同一类型的向量
 * - Point（点）: 一条记录，包含：唯一ID + 向量 + Payload（附加信息）
 * - Vector（向量）: 嵌入模型输出的浮点数数组，如 [0.1, 0.2, ..., 0.5]（768维）
 * - Payload（负载）: 附加的元数据，如原文文本、文件名、块索引等
 * - Cosine Similarity（余弦相似度）: 衡量两个向量方向的相似程度，值域 [-1, 1]
 *
 * 【Qdrant REST API 端点】
 * PUT    /collections/{name}              创建集合
 * GET    /collections/{name}              获取集合信息
 * PUT    /collections/{name}/points       插入/更新点（upsert）
 * POST   /collections/{name}/points/search  向量搜索
 * POST   /collections/{name}/points/delete  删除点
 *
 * 【为什么用 REST API 而不是 gRPC 客户端？】
 * - 无需额外依赖（Qdrant gRPC 客户端需要 protobuf）
 * - REST API 足够简单直观，便于学习和调试
 * - 可以直接用 curl 测试，方便排查问题
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final RestClient restClient;
    private final QdrantConfig config;

    public VectorStoreService(RestClient qdrantRestClient, QdrantConfig config) {
        this.restClient = qdrantRestClient;
        this.config = config;
    }

    /**
     * 应用启动时自动执行。
     *
     * 【@PostConstruct 是什么？】
     * Spring Bean 初始化完成后自动调用的方法，常用于执行初始化逻辑。
     * 这里用来确保 Qdrant collection 存在。
     *
     * 【为什么用 try-catch 包裹？】
     * 开发/测试时 Qdrant 可能未启动，不应阻止应用启动。
     * 优雅降级：记录警告日志，向量功能暂不可用，但其他功能正常。
     */
    @PostConstruct
    public void init() {
        try {
            ensureCollectionExists();
        } catch (Exception e) {
            log.warn("Failed to connect to Qdrant on startup: {}. Vector store features will be unavailable.", e.getMessage());
        }
    }

    /**
     * 确保 Qdrant 中的 collection 已创建。
     * 如果 collection 已存在则跳过，不存在则创建。
     */
    public void ensureCollectionExists() {
        String collectionName = config.getCollectionName();
        try {
            // 尝试 GET 请求，如果成功说明 collection 已存在
            restClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Qdrant collection '{}' already exists", collectionName);
        } catch (Exception e) {
            // collection 不存在，创建新的
            log.info("Creating Qdrant collection '{}'", collectionName);
            createCollection(collectionName);
        }
    }

    /**
     * 创建新的 Qdrant collection。
     *
     * 【关键参数说明】
     * - size: 向量维度，必须与嵌入模型输出维度一致（768）
     * - distance: 距离度量方式
     *   - Cosine（余弦距离）: 适合文本语义比较，最常用
     *   - Euclid（欧氏距离）: 适合空间坐标等
     *   - DotProduct（点积）: 适合归一化后的向量
     */
    private void createCollection(String name) {
        Map<String, Object> vectorParams = Map.of(
                "size", config.getVectorSize(),
                "distance", "Cosine"
        );
        Map<String, Object> body = Map.of(
                "vectors", vectorParams
        );

        restClient.put()
                .uri("/collections/{name}", name)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("Created Qdrant collection '{}'", name);
    }

    /**
     * 将文档块及其嵌入向量批量存储到 Qdrant。
     *
     * 【存储流程】
     * 每个文档块 → 一个 Qdrant Point：
     *   - id: 基于 documentId + chunkIndex 的确定性 UUID（相同内容生成相同ID，支持幂等更新）
     *   - vector: 嵌入向量（768维 float[]）
     *   - payload: 元数据（原文、文件名、块索引、格式等）
     *
     * 【UUID.nameUUIDFromBytes 的作用】
     * 基于输入字节生成确定性的 UUID v3。
     * 好处：相同的 documentId + chunkIndex 总是生成相同的 pointId，
     * 再次 upsert 时会更新而非重复插入。
     *
     * @param documentId 文档唯一标识（上传时生成的 UUID）
     * @param fileName   原始文件名
     * @param format     文件格式（如 text/plain）
     * @param chunks     文档块列表
     * @param embeddings 对应的嵌入向量列表（与 chunks 一一对应）
     */
    public void storeChunks(String documentId, String fileName, String format,
                            List<DocumentChunk> chunks, List<float[]> embeddings) {
        String collectionName = config.getCollectionName();
        List<Map<String, Object>> points = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            float[] embedding = embeddings.get(i);

            // 生成确定性 UUID：相同 documentId + chunkIndex → 相同 pointId
            String pointId = UUID.nameUUIDFromBytes(
                    (documentId + ":" + chunk.index()).getBytes()
            ).toString();

            // 构造 Qdrant Point
            Map<String, Object> point = Map.of(
                    "id", pointId,
                    "vector", embedding,
                    "payload", Map.of(
                            "documentId", documentId,
                            "fileName", fileName,
                            "chunkIndex", chunk.index(),
                            "text", chunk.text(),           // 保存原文，搜索时直接返回
                            "format", format,
                            "startOffset", chunk.startOffset(),
                            "endOffset", chunk.endOffset(),
                            "approximateTokenCount", chunk.approximateTokenCount()
                    )
            );
            points.add(point);
        }

        // 批量 upsert：一次请求插入/更新多个点
        Map<String, Object> body = Map.of("points", points);

        restClient.put()
                .uri("/collections/{name}/points", collectionName)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("Stored {} chunks for document '{}' in collection '{}'",
                chunks.size(), documentId, collectionName);
    }

    /**
     * 向量相似度搜索。
     *
     * 【搜索原理】
     * 1. 将查询文本通过嵌入模型转为向量
     * 2. Qdrant 计算该向量与库中所有向量的余弦相似度
     * 3. 返回相似度最高的 topK 个结果
     *
     * 【余弦相似度】
     * cos(θ) = (A·B) / (|A|×|B|)
     * 值域 [-1, 1]，1 表示完全相同，0 表示无关，-1 表示完全相反。
     * 文本场景通常在 0.3~0.9 之间。
     *
     * @param queryVector 查询文本的嵌入向量
     * @param topK        返回最相似的前 K 个结果
     * @return 搜索结果列表，按相似度降序排列
     */
    @SuppressWarnings("unchecked")
    public List<SearchResult> search(float[] queryVector, int topK) {
        String collectionName = config.getCollectionName();

        // 构造搜索请求体
        Map<String, Object> body = Map.of(
                "vector", queryVector,    // 查询向量
                "limit", topK,            // 返回数量
                "with_payload", true       // 需要返回 payload（原文等信息）
        );

        // 调用 Qdrant 搜索 API
        Map<String, Object> response = restClient.post()
                .uri("/collections/{name}/points/search", collectionName)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("result")) {
            return Collections.emptyList();
        }

        // 解析搜索结果
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("result");
        List<SearchResult> searchResults = new ArrayList<>();

        for (Map<String, Object> result : results) {
            // payload 中存储了我们保存的元数据（text, fileName, chunkIndex 等）
            Map<String, Object> payload = (Map<String, Object>) result.get("payload");
            // score 是余弦相似度分数
            double score = ((Number) result.get("score")).doubleValue();

            searchResults.add(new SearchResult(
                    (String) payload.get("text"),
                    score,
                    (String) payload.get("documentId"),
                    (String) payload.get("fileName"),
                    ((Number) payload.get("chunkIndex")).intValue(),
                    (String) payload.get("format"),
                    ((Number) payload.get("startOffset")).intValue(),
                    ((Number) payload.get("endOffset")).intValue(),
                    ((Number) payload.get("approximateTokenCount")).intValue()
            ));
        }

        return searchResults;
    }

    /**
     * 按文档 ID 删除所有相关的向量点。
     *
     * 【Qdrant 过滤删除】
     * 使用 filter 按 payload 字段匹配删除。
     * "must" 类似 SQL 的 WHERE 条件，"match" 做精确匹配。
     * 相当于 SQL: DELETE FROM points WHERE documentId = ?
     */
    public void deleteByDocumentId(String documentId) {
        String collectionName = config.getCollectionName();

        Map<String, Object> filter = Map.of(
                "must", List.of(
                        Map.of(
                                "key", "documentId",
                                "match", Map.of("value", documentId)
                        )
                )
        );

        Map<String, Object> body = Map.of(
                "filter", filter
        );

        restClient.post()
                .uri("/collections/{name}/points/delete", collectionName)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("Deleted all points for document '{}'", documentId);
    }

    /**
     * 获取 collection 的统计信息。
     * 用于前端展示向量数据库的状态（总点数、向量数等）。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCollectionStats() {
        String collectionName = config.getCollectionName();

        Map<String, Object> response = restClient.get()
                .uri("/collections/{name}", collectionName)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("result")) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = (Map<String, Object>) response.get("result");
        Map<String, Object> pointsCount = (Map<String, Object>) result.get("points_count");
        Map<String, Object> vectorsCount = (Map<String, Object>) result.get("vectors_count");

        Map<String, Object> stats = new HashMap<>();
        stats.put("collectionName", collectionName);
        stats.put("vectorSize", config.getVectorSize());
        stats.put("pointsCount", pointsCount != null ? pointsCount.get("count") : 0);
        stats.put("vectorsCount", vectorsCount != null ? vectorsCount.get("count") : 0);
        stats.put("status", result.get("status"));

        return stats;
    }

    /**
     * 搜索结果记录。
     *
     * @param text                  匹配的文本块原文
     * @param score                 相似度分数（余弦相似度，0~1）
     * @param documentId            来源文档 ID
     * @param fileName              来源文件名
     * @param chunkIndex            块在原文中的索引
     * @param format                文件格式
     * @param startOffset           文本起始偏移
     * @param endOffset             文本结束偏移
     * @param approximateTokenCount 近似 token 数
     */
    public record SearchResult(
            String text,
            double score,
            String documentId,
            String fileName,
            int chunkIndex,
            String format,
            int startOffset,
            int endOffset,
            int approximateTokenCount
    ) {}
}
