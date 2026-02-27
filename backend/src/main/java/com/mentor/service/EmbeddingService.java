package com.mentor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentor.entity.Mentor;
import com.mentor.entity.Student;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Embedding Service for Feature Vector Generation
 * 特征向量生成服务
 */
@Slf4j
@Service
public class EmbeddingService {

    @Value("${semantic.service-url}")
    private String semanticServiceUrl;

    @Value("${semantic.timeout:10000}")
    private Integer timeout;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String REDIS_KEY_PREFIX_MENTOR = "embedding:mentor:";
    private static final String REDIS_KEY_PREFIX_STUDENT = "embedding:student:";
    private static final long CACHE_TTL_HOURS = 24; // Cache for 24 hours

    /**
     * Generate embedding for mentor profile
     * 为导师档案生成向量
     */
    public List<Float> generateMentorEmbedding(Mentor mentor) {
        try {
            // Check cache first
            String cacheKey = REDIS_KEY_PREFIX_MENTOR + mentor.getId();
            List<Float> cachedEmbedding = getCachedEmbedding(cacheKey);
            if (cachedEmbedding != null) {
                log.debug("Using cached embedding for mentor: {}", mentor.getId());
                return cachedEmbedding;
            }

            // Build feature text
            String featureText = buildMentorFeatureText(mentor);

            // Generate embedding
            List<Float> embedding = callEmbeddingAPI(featureText);

            // Cache the result
            cacheEmbedding(cacheKey, embedding);

            log.info("Generated embedding for mentor: {}", mentor.getId());
            return embedding;

        } catch (Exception e) {
            log.error("Failed to generate embedding for mentor: {}", mentor.getId(), e);
            throw new RuntimeException("Failed to generate mentor embedding", e);
        }
    }

    /**
     * Generate embedding for student profile
     * 为学生档案生成向量
     */
    public List<Float> generateStudentEmbedding(Student student) {
        try {
            // Check cache first
            String cacheKey = REDIS_KEY_PREFIX_STUDENT + student.getId();
            List<Float> cachedEmbedding = getCachedEmbedding(cacheKey);
            if (cachedEmbedding != null) {
                log.debug("Using cached embedding for student: {}", student.getId());
                return cachedEmbedding;
            }

            // Build feature text
            String featureText = buildStudentFeatureText(student);

            // Generate embedding
            List<Float> embedding = callEmbeddingAPI(featureText);

            // Cache the result
            cacheEmbedding(cacheKey, embedding);

            log.info("Generated embedding for student: {}", student.getId());
            return embedding;

        } catch (Exception e) {
            log.error("Failed to generate embedding for student: {}", student.getId(), e);
            throw new RuntimeException("Failed to generate student embedding", e);
        }
    }

    /**
     * Build feature text for mentor
     * 构建导师特征文本
     */
    private String buildMentorFeatureText(Mentor mentor) {
        StringBuilder text = new StringBuilder();

        text.append("导师：").append(mentor.getName()).append("\n");

        if (mentor.getTitle() != null) {
            text.append("职称：").append(mentor.getTitle()).append("\n");
        }

        if (mentor.getInstitution() != null) {
            text.append("机构：").append(mentor.getInstitution()).append("\n");
        }

        if (mentor.getResearchAreas() != null) {
            text.append("研究方向：").append(mentor.getResearchAreas()).append("\n");
        }

        if (mentor.getBio() != null) {
            text.append("个人简介：").append(mentor.getBio()).append("\n");
        }

        if (mentor.getEducationBackground() != null) {
            text.append("教育背景：").append(mentor.getEducationBackground()).append("\n");
        }

        if (mentor.getKeywords() != null) {
            text.append("关键词：").append(mentor.getKeywords()).append("\n");
        }

        return text.toString();
    }

    /**
     * Build feature text for student
     * 构建学生特征文本
     */
    private String buildStudentFeatureText(Student student) {
        StringBuilder text = new StringBuilder();

        text.append("学生：").append(student.getName()).append("\n");

        if (student.getDegreeLevel() != null) {
            text.append("学历：").append(student.getDegreeLevel()).append("\n");
        }

        if (student.getMajor() != null) {
            text.append("专业：").append(student.getMajor()).append("\n");
        }

        if (student.getCurrentInstitution() != null) {
            text.append("学校：").append(student.getCurrentInstitution()).append("\n");
        }

        if (student.getGpa() != null) {
            text.append("GPA：").append(student.getGpa()).append("\n");
        }

        if (student.getResearchInterests() != null) {
            text.append("研究兴趣：").append(student.getResearchInterests()).append("\n");
        }

        if (student.getBio() != null) {
            text.append("个人简介：").append(student.getBio()).append("\n");
        }

        if (student.getKeywords() != null) {
            text.append("关键词：").append(student.getKeywords()).append("\n");
        }

        return text.toString();
    }

    /**
     * Call embedding API to generate vector
     * 调用嵌入API生成向量
     */
    private List<Float> callEmbeddingAPI(String text) throws Exception {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            HttpPost request = new HttpPost(semanticServiceUrl + "/embedding");

            // Set headers
            request.setHeader("Content-Type", "application/json");

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", text);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            request.setEntity(new StringEntity(jsonBody, "UTF-8"));

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode != 200) {
                    throw new RuntimeException("Embedding API returned status " + statusCode + ": " + responseBody);
                }

                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                // Extract embedding vector (Python service returns "vector" field)
                JsonNode vectorNode = jsonResponse.path("vector");
                if (vectorNode.isMissingNode() || !vectorNode.isArray()) {
                    throw new RuntimeException("Invalid response from embedding API: missing or invalid vector field");
                }

                List<Float> embedding = new ArrayList<>();
                for (JsonNode value : vectorNode) {
                    embedding.add((float) value.asDouble());
                }

                return embedding;
            }
        }
    }

    /**
     * Get cached embedding from Redis
     * 从Redis获取缓存的向量
     */
    @SuppressWarnings("unchecked")
    private List<Float> getCachedEmbedding(String cacheKey) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                return (List<Float>) cached;
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get cached embedding: {}", cacheKey, e);
            return null;
        }
    }

    /**
     * Cache embedding in Redis
     * 在Redis中缓存向量
     */
    private void cacheEmbedding(String cacheKey, List<Float> embedding) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, embedding, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache embedding: {}", cacheKey, e);
        }
    }

    /**
     * Invalidate mentor embedding cache
     * 使导师向量缓存失效
     */
    public void invalidateMentorCache(Integer mentorId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String cacheKey = REDIS_KEY_PREFIX_MENTOR + mentorId;
            redisTemplate.delete(cacheKey);
            log.debug("Invalidated mentor embedding cache: {}", mentorId);
        } catch (Exception e) {
            log.warn("Failed to invalidate mentor cache: {}", mentorId, e);
        }
    }

    /**
     * Invalidate student embedding cache
     * 使学生向量缓存失效
     */
    public void invalidateStudentCache(Integer studentId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String cacheKey = REDIS_KEY_PREFIX_STUDENT + studentId;
            redisTemplate.delete(cacheKey);
            log.debug("Invalidated student embedding cache: {}", studentId);
        } catch (Exception e) {
            log.warn("Failed to invalidate student cache: {}", studentId, e);
        }
    }

    /**
     * Generate embedding from raw text
     * 从原始文本生成向量
     */
    public List<Float> generateEmbedding(String text) {
        try {
            return callEmbeddingAPI(text);
        } catch (Exception e) {
            log.error("Failed to generate embedding from text", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }
}
