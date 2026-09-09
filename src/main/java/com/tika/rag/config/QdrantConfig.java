package com.tika.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Qdrant 向量数据库配置类
 *
 * 【什么是 Qdrant？】
 * Qdrant 是一个高性能的开源向量数据库，专门用于存储和搜索高维向量（embeddings）。
 * 在 RAG（检索增强生成）场景中，它将文档块的文本向量存储起来，
 * 当用户查询时，通过向量相似度搜索找到语义最相关的文本块。
 *
 * 【ConfigurationProperties 的作用】
 * 将 application.yml 中以 "qdrant" 开头的配置项自动绑定到本类的字段上。
 * 例如 yml 中 qdrant.base-url=http://localhost:6333 会自动赋值给 baseUrl 字段。
 */
@Configuration
@ConfigurationProperties(prefix = "qdrant")
public class QdrantConfig {

    /** Qdrant 服务的 REST API 地址，默认 http://localhost:6333 */
    private String baseUrl;

    /** 集合（collection）名称，类似关系数据库中的"表"概念 */
    private String collectionName;

    /**
     * 向量维度大小。nomic-embed-text 模型输出 768 维向量，
     * 即每个文本被转换为 768 个浮点数的数组。
     * 创建 Qdrant collection 时需要指定这个值。
     */
    private int vectorSize;

    /**
     * 创建 Qdrant REST 客户端 Bean。
     *
     * 【RestClient 是什么？】
     * RestClient 是 Spring 6.1+ 引入的同步 HTTP 客户端，
     * 用来调用 Qdrant 的 REST API（创建集合、存储向量、搜索等）。
     * baseUrl 设置为 Qdrant 的服务地址，后续所有请求都基于这个地址。
     */
    @Bean
    public RestClient qdrantRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getVectorSize() { return vectorSize; }
    public void setVectorSize(int vectorSize) { this.vectorSize = vectorSize; }
}
