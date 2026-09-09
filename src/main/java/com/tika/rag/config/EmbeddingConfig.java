package com.tika.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Ollama 嵌入模型配置类
 *
 * 【什么是 Ollama？】
 * Ollama 是一个本地大模型运行工具，可以在自己的机器上运行各种 AI 模型。
 * 这里我们用它来运行嵌入模型（nomic-embed-text），将文本转换为向量。
 *
 * 【什么是 Embedding（嵌入）？】
 * Embedding 是将文本转换为固定维度的数值向量的过程。
 * 例如："你好世界" → [0.023, -0.156, 0.891, ..., 0.042]（768个数字）
 * 语义相近的文本，转换后的向量在空间中距离也近，这就是语义搜索的基础。
 *
 * 【为什么用本地模型而不是 OpenAI API？】
 * - 免费，无需 API Key
 * - 数据不出本地，隐私安全
 * - 无网络依赖，延迟更低
 */
@Configuration
@ConfigurationProperties(prefix = "ollama")
public class EmbeddingConfig {

    /** Ollama 服务的 API 地址，默认 http://localhost:11434 */
    private String baseUrl;

    /**
     * 嵌入模型名称。
     * nomic-embed-text 是一个轻量但效果好的嵌入模型：
     * - 输出 768 维向量
     * - 模型大小约 274MB
     * - 支持中英文
     * 通过 `ollama pull nomic-embed-text` 下载
     */
    private String embeddingModel;

    /**
     * 创建 Ollama REST 客户端 Bean。
     * 用于调用 Ollama 的 /api/embed 接口生成文本向量。
     */
    @Bean
    public RestClient ollamaRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
}
