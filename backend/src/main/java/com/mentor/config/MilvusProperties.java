package com.mentor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Milvus Configuration Properties
 * Milvus配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {

    /**
     * Milvus host
     */
    private String host = "127.0.0.1";

    /**
     * Milvus port
     */
    private Integer port = 19530;

    /**
     * Database name
     */
    private String database = "mentor_system";

    /**
     * Collection names
     */
    private Map<String, String> collections;

    /**
     * Embedding configuration
     */
    private EmbeddingConfig embedding;

    @Data
    public static class EmbeddingConfig {
        /**
         * Vector dimension
         */
        private Integer dimension = 1536;

        /**
         * Metric type (COSINE, L2, IP)
         */
        private String metricType = "COSINE";

        /**
         * Index type (IVF_FLAT, HNSW, etc.)
         */
        private String indexType = "IVF_FLAT";

        /**
         * Number of cluster units (for IVF_FLAT)
         */
        private Integer nlist = 1024;
    }
}
