package com.mentor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM Service
 * LLM服务（使用OpenRouter API调用DeepSeek）
 */
@Service
public class LLMService {

    @Value("${llm.openrouter.api-key}")
    private String apiKey;

    @Value("${llm.openrouter.base-url}")
    private String baseUrl;

    @Value("${llm.openrouter.model}")
    private String model;

    @Value("${llm.openrouter.temperature}")
    private Double temperature;

    @Value("${llm.openrouter.max-tokens}")
    private Integer maxTokens;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Call LLM API
     * 调用LLM API
     */
    public String callLLM(String prompt) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(baseUrl);

            // Set headers
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + apiKey);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", new Map[]{message});

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            request.setEntity(new StringEntity(jsonBody, "UTF-8"));

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                // Extract response content
                return jsonResponse.path("choices").get(0)
                        .path("message").path("content").asText();
            }
        }
    }

    /**
     * Analyze user preferences from browsing history
     * 从浏览历史分析用户偏好
     */
    public String analyzeUserPreferences(String historyData) throws Exception {
        String prompt = String.format(
                "请分析以下学生的浏览历史，总结他们的研究兴趣和偏好。\n\n" +
                "浏览历史：\n%s\n\n" +
                "请用JSON格式返回分析结果，包含以下字段：\n" +
                "- summary: 简短总结（50字以内）\n" +
                "- keywords: 关键词列表（5-10个）\n" +
                "- topics: 研究主题列表（3-5个）\n" +
                "- recommendations: 推荐理由",
                historyData
        );

        return callLLM(prompt);
    }
}
