package com.mentor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentor.entity.BrowsingHistory;
import com.mentor.entity.Mentor;
import com.mentor.entity.UserPreference;
import com.mentor.mapper.MentorMapper;
import com.mentor.mapper.UserPreferenceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommendation Service
 * 推荐服务
 */
@Service
public class RecommendationService {

    @Autowired
    private UserBehaviorService userBehaviorService;

    @Autowired
    private LLMService llmService;

    @Autowired
    private MentorMapper mentorMapper;

    @Autowired
    private UserPreferenceMapper userPreferenceMapper;

    @Value("${recommendation.min-history-count:30}")
    private Integer minHistoryCount;

    @Value("${recommendation.analysis-trigger-count:25}")
    private Integer analysisTriggerCount;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get personalized mentor recommendations
     * 获取个性化导师推荐
     */
    public List<Map<String, Object>> getRecommendations(Integer userId) {
        try {
            // Check if user has enough browsing history
            int historyCount = userBehaviorService.countUserHistory(userId);
            if (historyCount < minHistoryCount) {
                // Return popular mentors if not enough history
                return getPopularMentors();
            }

            // Get or update user preferences
            UserPreference preference = getUserPreference(userId);
            if (preference == null || shouldUpdatePreference(preference, historyCount)) {
                preference = analyzeAndUpdatePreference(userId, historyCount);
            }

            // Generate recommendations based on preferences
            return generateRecommendations(preference);

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to popular mentors on error
            return getPopularMentors();
        }
    }

    /**
     * Get user preference
     * 获取用户偏好
     */
    private UserPreference getUserPreference(Integer userId) {
        return userPreferenceMapper.getUserPreferenceByUserId(userId);
    }

    /**
     * Check if preference should be updated
     * 检查是否需要更新偏好
     */
    private boolean shouldUpdatePreference(UserPreference preference, int currentHistoryCount) {
        int newRecords = currentHistoryCount - preference.getLastAnalyzedLogCount();
        return newRecords >= analysisTriggerCount;
    }

    /**
     * Analyze and update user preference
     * 分析并更新用户偏好
     */
    @Transactional
    private UserPreference analyzeAndUpdatePreference(Integer userId, int historyCount) throws Exception {
        // Get recent browsing history
        List<BrowsingHistory> history = userBehaviorService.getUserHistory(userId, 100);

        // Build history data string for LLM
        StringBuilder historyData = new StringBuilder();
        for (BrowsingHistory h : history) {
            if ("mentor".equals(h.getTargetType())) {
                Mentor mentor = mentorMapper.getMentorById(h.getTargetId());
                if (mentor != null) {
                    historyData.append(String.format("- 浏览了导师：%s，研究方向：%s\n",
                            mentor.getName(), mentor.getResearchAreas()));
                }
            }
        }

        // Call LLM to analyze preferences
        String llmResponse = llmService.analyzeUserPreferences(historyData.toString());

        // Parse LLM response
        JsonNode jsonResponse = objectMapper.readTree(llmResponse);
        String summary = jsonResponse.path("summary").asText();
        String keywords = jsonResponse.path("keywords").toString();
        String topics = jsonResponse.path("topics").toString();

        // Save or update preference
        UserPreference preference = getUserPreference(userId);
        if (preference == null) {
            preference = UserPreference.builder()
                    .userId(userId)
                    .userType("student")
                    .preferenceText(summary)
                    .preferenceKeywords(keywords)
                    .preferenceTopics(topics)
                    .lastAnalyzedLogCount(historyCount)
                    .currentLogCount(historyCount)
                    .analysisCount(1)
                    .createTime(new Date())
                    .updateTime(new Date())
                    .build();
            userPreferenceMapper.insertUserPreference(preference);
        } else {
            preference.setPreferenceText(summary);
            preference.setPreferenceKeywords(keywords);
            preference.setPreferenceTopics(topics);
            preference.setLastAnalyzedLogCount(historyCount);
            preference.setCurrentLogCount(historyCount);
            preference.setAnalysisCount(preference.getAnalysisCount() + 1);
            preference.setUpdateTime(new Date());
            userPreferenceMapper.updateUserPreference(preference);
        }

        return preference;
    }

    /**
     * Generate recommendations based on preferences
     * 基于偏好生成推荐
     */
    private List<Map<String, Object>> generateRecommendations(UserPreference preference) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        try {
            // Parse preference topics
            JsonNode topics = objectMapper.readTree(preference.getPreferenceTopics());

            // Search mentors for each topic
            for (JsonNode topic : topics) {
                String topicStr = topic.asText();
                List<Mentor> mentors = mentorMapper.searchMentors(topicStr, 0, 5);

                for (Mentor mentor : mentors) {
                    Map<String, Object> recommendation = new HashMap<>();
                    recommendation.put("mentor", mentor);
                    recommendation.put("reason", "研究方向匹配：" + topicStr);
                    recommendation.put("score", mentor.getRatingAvg());
                    recommendations.add(recommendation);
                }
            }

            // Sort by rating and remove duplicates
            recommendations = recommendations.stream()
                    .collect(Collectors.toMap(
                            r -> ((Mentor) r.get("mentor")).getId(),
                            r -> r,
                            (r1, r2) -> r1
                    ))
                    .values()
                    .stream()
                    .sorted((r1, r2) -> Double.compare(
                            (Double) r2.get("score"),
                            (Double) r1.get("score")
                    ))
                    .limit(10)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return recommendations;
    }

    /**
     * Get popular mentors as fallback
     * 获取热门导师作为后备
     */
    private List<Map<String, Object>> getPopularMentors() {
        List<Mentor> mentors = mentorMapper.getMentorList(0, 10);
        return mentors.stream()
                .map(mentor -> {
                    Map<String, Object> recommendation = new HashMap<>();
                    recommendation.put("mentor", mentor);
                    recommendation.put("reason", "热门导师推荐");
                    recommendation.put("score", mentor.getRatingAvg());
                    return recommendation;
                })
                .collect(Collectors.toList());
    }
}
