package com.tika.rag.service;

import com.tika.rag.chunker.ChunkingStrategy;
import com.tika.rag.chunker.ChunkerRegistry;
import com.tika.rag.config.QdrantConfig;
import com.tika.rag.embedding.EmbeddingService;
import com.tika.rag.model.DocumentChunk;
import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.request.ChunkingRequest;
import com.tika.rag.model.request.SearchRequest;
import com.tika.rag.model.response.SearchResponse;
import com.tika.rag.model.response.StoreResponse;
import com.tika.rag.parser.DocumentParser;
import com.tika.rag.vectorstore.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 搜索服务 - RAG 流程的核心编排层
 *
 * 【什么是编排层？】
 * 它不直接做具体的解析、嵌入或存储工作，而是协调各个服务完成完整流程。
 * 类似于一个"指挥官"，按顺序调用各个"士兵"（服务）完成任务。
 *
 * 【RAG 完整流程】
 *
 * 存储流程（storeDocument）:
 * ┌──────────┐    ┌──────────┐    ┌──────────────┐    ┌──────────┐
 * │ 上传文件  │ →  │ Tika解析  │ →  │ Chunker分块   │ →  │ Ollama   │
 * │(Multipart)│    │(提取文本) │    │(切分为小块)   │    │(转成向量) │
 * └──────────┘    └──────────┘    └──────────────┘    └──────────┘
 *                                                              │
 *                                                              ↓
 *                                                       ┌──────────┐
 *                                                       │  Qdrant  │
 *                                                       │(存储向量) │
 *                                                       └──────────┘
 *
 * 搜索流程（search）:
 * ┌──────────┐    ┌──────────────┐    ┌──────────┐    ┌──────────┐
 * │ 查询文本  │ →  │ Ollama转向量  │ →  │ Qdrant   │ →  │ 返回结果  │
 * └──────────┘    └──────────────┘    │(相似度搜索)│    │(按分数排序)│
 *                                      └──────────┘    └──────────┘
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    // ===== 依赖的四个服务 =====
    private final DocumentParser documentParser;       // Tika 文档解析器
    private final ChunkerRegistry chunkerRegistry;     // 分块策略注册表
    private final EmbeddingService embeddingService;   // Ollama 嵌入服务
    private final VectorStoreService vectorStoreService; // Qdrant 向量存储
    private final QdrantConfig qdrantConfig;           // Qdrant 配置

    /**
     * 构造函数注入所有依赖。
     *
     * 【为什么用构造函数注入而不是 @Autowired 字段注入？】
     * - 强制依赖明确（缺少任何一个都无法创建对象）
     * - 方便单元测试（可以直接 new 传入 mock 对象）
     * - 字段不可变（final），更安全
     */
    public SearchService(DocumentParser documentParser,
                         ChunkerRegistry chunkerRegistry,
                         EmbeddingService embeddingService,
                         VectorStoreService vectorStoreService,
                         QdrantConfig qdrantConfig) {
        this.documentParser = documentParser;
        this.chunkerRegistry = chunkerRegistry;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.qdrantConfig = qdrantConfig;
    }

    /**
     * 文档入库：解析 → 分块 → 嵌入 → 存储
     *
     * 这是 RAG 系统的核心写入流程。一个文档经过 5 个步骤最终变成可搜索的向量。
     *
     * @param file            上传的文件
     * @param chunkingRequest 分块参数（策略、块大小、重叠等）
     * @return 入库结果（文档ID、块数量等）
     */
    public StoreResponse storeDocument(MultipartFile file, ChunkingRequest chunkingRequest) {
        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
            // 生成唯一的文档 ID（UUID v4，完全随机）
            String documentId = UUID.randomUUID().toString();

            // ===== 步骤 1: 解析文档 =====
            // 使用 Apache Tika 从各种格式（PDF、Word、HTML等）中提取纯文本
            log.info("Step 1/5: Parsing document '{}'", fileName);
            ParsedDocument document = documentParser.parse(is, fileName, file.getSize());
            log.info("Parsed document '{}': {} chars", fileName, document.text().length());

            // ===== 步骤 2: 分块 =====
            // 将长文档切分为较小的块，每个块会独立存储为向量
            // 为什么要分块？因为嵌入模型有输入长度限制，且小块搜索更精准
            log.info("Step 2/5: Chunking document with strategy '{}'", chunkingRequest.effectiveStrategy());
            ChunkingStrategy strategy = chunkerRegistry.getStrategy(chunkingRequest.effectiveStrategy());
            List<DocumentChunk> chunks = strategy.chunk(document, chunkingRequest);
            log.info("Chunked into {} pieces", chunks.size());

            // ===== 步骤 3: 生成嵌入向量 =====
            // 批量调用 Ollama，将所有块的文本一次性转为向量
            log.info("Step 3/5: Generating embeddings for {} chunks via Ollama", chunks.size());
            List<String> chunkTexts = chunks.stream().map(DocumentChunk::text).toList();
            List<List<Float>> embeddings = embeddingService.embedBatch(chunkTexts);
            log.info("Generated {} embeddings", embeddings.size());

            // ===== 步骤 4: 类型转换 =====
            // JSON 返回的是 Double，Qdrant 需要 float[]
            List<float[]> embeddingArrays = embeddings.stream()
                    .map(this::toFloatArray)
                    .toList();

            // ===== 步骤 5: 存入 Qdrant =====
            log.info("Step 4/5: Storing {} chunks in Qdrant collection '{}'", chunks.size(), qdrantConfig.getCollectionName());
            vectorStoreService.storeChunks(documentId, fileName, document.format(), chunks, embeddingArrays);
            log.info("Step 5/5: Document stored successfully with ID '{}'", documentId);

            return new StoreResponse(
                    documentId,
                    fileName,
                    qdrantConfig.getCollectionName(),
                    chunks.size(),
                    "stored"
            );
        } catch (Exception e) {
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
            log.error("Failed to store document '{}': {}", fileName, e.getMessage(), e);
            throw new com.tika.rag.exception.DocumentParseException(
                    "Failed to store document '" + fileName + "': " + e.getMessage(), e);
        }
    }

    /**
     * 语义搜索：查询文本 → 嵌入 → 向量搜索 → 返回结果
     *
     * 这是 RAG 系统的核心读取流程。
     * 与关键词搜索不同，语义搜索能理解同义词和上下文含义。
     * 例如搜索"如何保护网络安全"，能找到存储的"网络安全防护措施"相关段落，
     * 即使两者没有完全相同的关键词。
     *
     * @param request 搜索请求（查询文本 + 返回数量）
     * @return 搜索结果（匹配的文本块列表，按相似度排序）
     */
    public SearchResponse search(SearchRequest request) {
        // 步骤 1: 将查询文本转为向量
        float[] queryVector = embeddingService.embed(request.query());

        // 步骤 2: 在 Qdrant 中搜索最相似的向量
        List<VectorStoreService.SearchResult> results =
                vectorStoreService.search(queryVector, request.effectiveTopK());

        // 步骤 3: 转换为响应格式
        List<SearchResponse.SearchResultItem> items = results.stream()
                .map(r -> new SearchResponse.SearchResultItem(
                        r.text(),
                        r.score(),
                        r.documentId(),
                        r.fileName(),
                        r.chunkIndex(),
                        r.format(),
                        r.startOffset(),
                        r.endOffset(),
                        r.approximateTokenCount()
                ))
                .toList();

        return new SearchResponse(request.query(), items.size(), items);
    }

    /**
     * 删除文档：从向量数据库中移除指定文档的所有块。
     * 当用户想重新入库或清理旧数据时使用。
     */
    public void deleteDocument(String documentId) {
        vectorStoreService.deleteByDocumentId(documentId);
    }

    /**
     * 获取向量数据库统计信息（总点数、向量维度等）。
     */
    public Map<String, Object> getStats() {
        return vectorStoreService.getCollectionStats();
    }

    /**
     * 将 List<?> 转换为 float[]。
     * 使用 Number 接口兼容 Double/Float/Integer 等数值类型。
     */
    private float[] toFloatArray(List<?> list) {
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = ((Number) list.get(i)).floatValue();
        }
        return result;
    }
}
