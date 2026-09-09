package com.tika.rag.embedding;

import com.tika.rag.config.EmbeddingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 嵌入服务 - 负责将文本转换为向量（Embedding）
 *
 * 【核心原理】
 * 文本本身无法直接进行数学比较，嵌入模型将文本映射到高维向量空间，
 * 使得语义相似的文本在空间中的距离更近。
 *
 * 例如：
 *   "机器学习" → [0.12, -0.34, 0.56, ...] (768维)
 *   "深度学习" → [0.11, -0.33, 0.55, ...] (768维，与"机器学习"很接近)
 *   "今天天气" → [0.78, 0.21, -0.45, ...] (768维，与上面两个距离较远)
 *
 * 【工作流程】
 * 1. 接收文本（单个或批量）
 * 2. 调用 Ollama 的 /api/embed 接口
 * 3. Ollama 使用 nomic-embed-text 模型计算向量
 * 4. 返回浮点数数组
 *
 * 【Ollama API 说明】
 * 请求: POST http://localhost:11434/api/embed
 * Body: { "model": "nomic-embed-text", "input": ["文本1", "文本2"] }
 * 响应: { "embeddings": [[0.1, 0.2, ...], [0.3, 0.4, ...]] }
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final RestClient restClient;
    private final String model;

    /**
     * 构造函数注入依赖。
     *
     * @param ollamaRestClient 由 EmbeddingConfig 创建的 RestClient，baseUrl 指向 Ollama
     * @param config           嵌入配置，包含模型名称
     */
    public EmbeddingService(RestClient ollamaRestClient, EmbeddingConfig config) {
        this.restClient = ollamaRestClient;
        this.model = config.getEmbeddingModel();
    }

    /**
     * 将单条文本转换为向量。
     * 内部调用 embedBatch()，取第一个结果。
     *
     * @param text 要转换的文本
     * @return float数组（768维）
     */
    public float[] embed(String text) {
        List<List<Float>> embeddings = embedBatch(List.of(text));
        if (embeddings.isEmpty()) {
            throw new RuntimeException("Failed to generate embedding for text");
        }
        return toFloatArray(embeddings.get(0));
    }

    /**
     * 批量生成嵌入向量。
     *
     * 【为什么用批量而不是逐条调用？】
     * - 减少网络往返次数，一次 HTTP 请求处理多条文本
     * - Ollama 内部可以做批处理优化
     * - 对于 100 个文档块，只需 1 次 HTTP 请求而非 100 次
     *
     * 【注意类型转换】
     * Ollama 返回的 JSON 中，数字会被 Jackson 反序列化为 Double 类型，
     * 但我们需要 float 类型来存储到 Qdrant，所以需要手动转换。
     * 使用 ((Number) item).floatValue() 兼容 Double 和 Float。
     *
     * @param texts 文本列表
     * @return 每个文本对应的向量（List of List of Float）
     */
    @SuppressWarnings("unchecked")
    public List<List<Float>> embedBatch(List<String> texts) {
        log.debug("Generating embeddings for {} texts", texts.size());

        // 构造请求体：指定模型名称和输入文本列表
        Map<String, Object> request = Map.of(
                "model", model,
                "input", texts
        );

        // 调用 Ollama REST API，返回结果自动解析为 Map
        Map<String, Object> response = restClient.post()
                .uri("/api/embed")
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("embeddings")) {
            throw new RuntimeException("Invalid response from Ollama embedding API");
        }

        // "embeddings" 字段是一个二维数组：[[0.1, 0.2, ...], [0.3, 0.4, ...]]
        return (List<List<Float>>) response.get("embeddings");
    }

    /**
     * 将 List<?> 转换为 float[]。
     *
     * 【为什么参数是 List<?> 而不是 List<Float>？】
     * 因为 JSON 反序列化时数字默认是 Double 类型，
     * 用 List<?> 配合 ((Number) item).floatValue() 可以安全处理 Double/Float/Integer 等。
     */
    private float[] toFloatArray(List<?> list) {
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = ((Number) list.get(i)).floatValue();
        }
        return result;
    }
}
