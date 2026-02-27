package com.mentor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentor.entity.Student;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;

/**
 * Milvus Service for Vector Storage and Search
 * Milvus向量存储和搜索服务 - 通过Python服务实现
 */
@Slf4j
@Service
public class MilvusService {

    @Value("${semantic.service-url}")
    private String semanticServiceUrl;

    @Value("${semantic.timeout:10000}")
    private Integer timeout;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initialize service
     * 初始化服务
     */
    @PostConstruct
    public void init() {
        log.info("MilvusService initialized - using Python semantic search service at {}", semanticServiceUrl);
    }

    /**
     * Cleanup
     * 清理资源
     */
    @PreDestroy
    public void destroy() {
        log.info("MilvusService destroyed");
    }

    /**
     * Upsert mentor profile
     * 插入/更新导师档案
     */
    public void upsertMentorProfile(Long mentorId, List<Float> embedding, String researchAreas,
                                    String institution, Float ratingAvg, Boolean acceptingStudents,
                                    Integer currentStudents, Integer maxStudents) {
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build();

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build()) {

                HttpPost request = new HttpPost(semanticServiceUrl + "/mentor/index");
                request.setHeader("Content-Type", "application/json");

                // Build request body
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("id", mentorId.intValue());
                requestBody.put("researchAreas", researchAreas != null ? researchAreas : "");
                requestBody.put("institution", institution != null ? institution : "");
                requestBody.put("name", ""); // Will be filled by embedding text
                requestBody.put("bio", "");
                requestBody.put("title", "");

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                request.setEntity(new StringEntity(jsonBody, "UTF-8"));

                // Execute request
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode != 200) {
                        throw new RuntimeException("Mentor index API returned status " + statusCode + ": " + responseBody);
                    }

                    log.info("Successfully indexed mentor profile: {}", mentorId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to upsert mentor profile: {}", mentorId, e);
            throw new RuntimeException("Failed to upsert mentor profile", e);
        }
    }

    /**
     * Upsert student profile
     * 插入/更新学生档案
     */
    public void upsertStudentProfile(Long studentId, List<Float> embedding, String researchInterests,
                                     String institution, String degreeLevel, Float gpa,
                                     String major, Integer graduationYear) {
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build();

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build()) {

                HttpPost request = new HttpPost(semanticServiceUrl + "/student/index");
                request.setHeader("Content-Type", "application/json");

                // Build request body
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("id", studentId.intValue());
                requestBody.put("researchInterests", researchInterests != null ? researchInterests : "");
                requestBody.put("institution", institution != null ? institution : "");
                requestBody.put("name", ""); // Will be filled by embedding text
                requestBody.put("bio", "");
                requestBody.put("major", major != null ? major : "");

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                request.setEntity(new StringEntity(jsonBody, "UTF-8"));

                // Execute request
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode != 200) {
                        throw new RuntimeException("Student index API returned status " + statusCode + ": " + responseBody);
                    }

                    log.info("Successfully indexed student profile: {}", studentId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to upsert student profile: {}", studentId, e);
            throw new RuntimeException("Failed to upsert student profile", e);
        }
    }

    /**
     * Index student profile from Student entity
     * 从Student实体索引学生档案（简化版）
     */
    public void indexStudent(Student student) {
        if (student == null || student.getId() == null) {
            log.warn("Cannot index null student or student without ID");
            return;
        }

        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build();

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build()) {

                HttpPost request = new HttpPost(semanticServiceUrl + "/student/index");
                request.setHeader("Content-Type", "application/json");

                // 组合所有相关文本字段用于语义索引
                StringBuilder combinedText = new StringBuilder();
                if (student.getName() != null) combinedText.append(student.getName()).append(" ");
                if (student.getResearchInterests() != null) combinedText.append(student.getResearchInterests()).append(" ");
                if (student.getBio() != null) combinedText.append(student.getBio()).append(" ");
                if (student.getPersonalAbilities() != null) combinedText.append(student.getPersonalAbilities()).append(" ");
                if (student.getExpectedResearchDirection() != null) combinedText.append(student.getExpectedResearchDirection()).append(" ");
                if (student.getProgrammingSkills() != null) combinedText.append(student.getProgrammingSkills()).append(" ");
                if (student.getKeywords() != null) combinedText.append(student.getKeywords()).append(" ");
                if (student.getProjectExperience() != null) combinedText.append(student.getProjectExperience()).append(" ");

                // Build request body
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("id", student.getId());
                requestBody.put("name", student.getName() != null ? student.getName() : "");
                requestBody.put("researchInterests", combinedText.toString().trim());
                requestBody.put("bio", student.getBio() != null ? student.getBio() : "");
                requestBody.put("institution", student.getCurrentInstitution() != null ? student.getCurrentInstitution() : "");
                requestBody.put("major", student.getMajor() != null ? student.getMajor() : "");

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                request.setEntity(new StringEntity(jsonBody, "UTF-8"));

                // Execute request
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode != 200) {
                        log.warn("Student index API returned status {}: {}", statusCode, responseBody);
                    } else {
                        log.info("Successfully indexed student profile: {} ({})", student.getId(), student.getName());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to index student profile: {} - {}", student.getId(), e.getMessage());
            // 不抛出异常，索引失败不影响主流程
        }
    }

    /**
     * Search similar mentors
     * 搜索相似导师
     */
    public List<Map<String, Object>> searchSimilarMentors(List<Float> queryEmbedding, int topK) {
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build();

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build()) {

                HttpPost request = new HttpPost(semanticServiceUrl + "/mentor/search");
                request.setHeader("Content-Type", "application/json");

                // Build request body
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("query", ""); // Empty query, will use vector directly
                requestBody.put("topK", topK);

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                request.setEntity(new StringEntity(jsonBody, "UTF-8"));

                // Execute request
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode != 200) {
                        throw new RuntimeException("Mentor search API returned status " + statusCode + ": " + responseBody);
                    }

                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    JsonNode resultsNode = jsonResponse.path("results");

                    List<Map<String, Object>> results = new ArrayList<>();
                    if (resultsNode.isArray()) {
                        for (JsonNode resultNode : resultsNode) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("id", resultNode.path("id").asLong());
                            result.put("score", resultNode.path("score").asDouble());
                            result.put("researchAreas", resultNode.path("researchAreas").asText(""));
                            result.put("institution", resultNode.path("institution").asText(""));
                            results.add(result);
                        }
                    }

                    log.info("Found {} similar mentors", results.size());
                    return results;
                }
            }
        } catch (Exception e) {
            log.error("Failed to search similar mentors", e);
            return new ArrayList<>();
        }
    }

    /**
     * Search similar students
     * 搜索相似学生
     */
    public List<Map<String, Object>> searchSimilarStudents(List<Float> queryEmbedding, int topK) {
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build();

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build()) {

                HttpPost request = new HttpPost(semanticServiceUrl + "/student/search");
                request.setHeader("Content-Type", "application/json");

                // Build request body
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("query", ""); // Empty query, will use vector directly
                requestBody.put("topK", topK);

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                request.setEntity(new StringEntity(jsonBody, "UTF-8"));

                // Execute request
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode != 200) {
                        throw new RuntimeException("Student search API returned status " + statusCode + ": " + responseBody);
                    }

                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    JsonNode resultsNode = jsonResponse.path("results");

                    List<Map<String, Object>> results = new ArrayList<>();
                    if (resultsNode.isArray()) {
                        for (JsonNode resultNode : resultsNode) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("id", resultNode.path("id").asLong());
                            result.put("score", resultNode.path("score").asDouble());
                            result.put("researchInterests", resultNode.path("researchInterests").asText(""));
                            result.put("institution", resultNode.path("institution").asText(""));
                            results.add(result);
                        }
                    }

                    log.info("Found {} similar students", results.size());
                    return results;
                }
            }
        } catch (Exception e) {
            log.error("Failed to search similar students", e);
            return new ArrayList<>();
        }
    }
    /**
     * Delete mentor profile
     * 删除导师档案
     */
    public void deleteMentorProfile(Long mentorId) {
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build();

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build()) {

                HttpPost request = new HttpPost(semanticServiceUrl + "/mentor/delete");
                request.setHeader("Content-Type", "application/json");

                // Build request body
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("id", mentorId.intValue());

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                request.setEntity(new StringEntity(jsonBody, "UTF-8"));

                // Execute request
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode != 200) {
                        throw new RuntimeException("Mentor delete API returned status " + statusCode + ": " + responseBody);
                    }

                    log.info("Successfully deleted mentor profile: {}", mentorId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to delete mentor profile: {}", mentorId, e);
            throw new RuntimeException("Failed to delete mentor profile", e);
        }
    }

    /**
     * Delete student profile
     * 删除学生档案
     */
    public void deleteStudentProfile(Long studentId) {
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build();

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build()) {

                HttpPost request = new HttpPost(semanticServiceUrl + "/student/delete");
                request.setHeader("Content-Type", "application/json");

                // Build request body
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("id", studentId.intValue());

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                request.setEntity(new StringEntity(jsonBody, "UTF-8"));

                // Execute request
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode != 200) {
                        throw new RuntimeException("Student delete API returned status " + statusCode + ": " + responseBody);
                    }

                    log.info("Successfully deleted student profile: {}", studentId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to delete student profile: {}", studentId, e);
            throw new RuntimeException("Failed to delete student profile", e);
        }
    }

    /**
     * Check if service is connected
     * 检查服务是否连接
     */
    public boolean isConnected() {
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build();

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build()) {

                HttpPost request = new HttpPost(semanticServiceUrl + "/health");
                request.setHeader("Content-Type", "application/json");

                // Execute request
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    return statusCode == 200;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check service connection", e);
            return false;
        }
    }
}
