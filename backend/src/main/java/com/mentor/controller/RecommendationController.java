package com.mentor.controller;

import com.mentor.entity.Student;
import com.mentor.entity.UserPreference;
import com.mentor.mapper.StudentMapper;
import com.mentor.mapper.UserPreferenceMapper;
import com.mentor.service.EnhancedRecommendationService;
import com.mentor.service.RecommendationService;
import com.mentor.service.SemanticRecommendationService;
import com.mentor.service.UserBehaviorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recommendation Controller
 * 推荐控制器
 */
@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private EnhancedRecommendationService enhancedRecommendationService;

    @Autowired
    private SemanticRecommendationService semanticRecommendationService;

    @Autowired
    private UserBehaviorService userBehaviorService;

    @Autowired
    private UserPreferenceMapper userPreferenceMapper;

    @Autowired
    private StudentMapper studentMapper;

    /**
     * Get personalized mentor recommendations for student (Enhanced)
     * 为学生获取个性化导师推荐（增强版 - 基于语义检索）
     */
    @GetMapping("/mentors")
    public Map<String, Object> getMentorRecommendations(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer studentId,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "true") Boolean useSemantic) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Support both userId and studentId for backward compatibility
            Integer inputId = studentId != null ? studentId : userId;

            if (inputId == null) {
                result.put("code", 400);
                result.put("message", "学生ID不能为空");
                return result;
            }

            // 先尝试按学生ID查找，如果找不到再按用户ID查找
            Student student = studentMapper.getStudentById(inputId);
            if (student == null) {
                student = studentMapper.getStudentByUserId(inputId);
            }
            
            Integer actualStudentId = student != null ? student.getId() : inputId;

            List<Map<String, Object>> recommendations;
            
            if (useSemantic) {
                // 使用语义推荐服务（多维度语义检索）
                recommendations = semanticRecommendationService.getMentorRecommendationsForStudent(actualStudentId, limit);
            } else {
                // 使用原有的增强推荐服务
                recommendations = enhancedRecommendationService.getMentorRecommendations(actualStudentId, limit);
            }

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", recommendations);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取导师推荐失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Get personalized student recommendations for mentor (Enhanced)
     * 为导师获取个性化学生推荐（增强版 - 基于语义检索）
     */
    @GetMapping("/students")
    public Map<String, Object> getStudentRecommendations(
            @RequestParam Integer mentorId,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "true") Boolean useSemantic) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (mentorId == null) {
                result.put("code", 400);
                result.put("message", "导师ID不能为空");
                return result;
            }

            List<Map<String, Object>> recommendations;
            
            if (useSemantic) {
                // 使用语义推荐服务（多维度语义检索）
                recommendations = semanticRecommendationService.getStudentRecommendationsForMentor(mentorId, limit);
            } else {
                // 使用原有的增强推荐服务
                recommendations = enhancedRecommendationService.getStudentRecommendations(mentorId, limit);
            }

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", recommendations);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取学生推荐失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Get user preferences
     * 获取用户偏好
     */
    @GetMapping("/preferences/{userId}")
    public Map<String, Object> getUserPreferences(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            UserPreference preference = userPreferenceMapper.getUserPreferenceByUserId(userId);
            
            if (preference != null) {
                result.put("code", 0);
                result.put("message", "成功");
                result.put("data", preference);
            } else {
                result.put("code", 0);
                result.put("message", "暂无偏好数据");
                result.put("data", null);
            }

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取偏好失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get complete user profile with preferences
     * 获取完整的用户档案（包含用户填写的偏好和系统分析的偏好）
     */
    @GetMapping("/profile/{userId}")
    public Map<String, Object> getUserProfile(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 先尝试按学生ID查找，如果找不到再按用户ID查找
            Student student = studentMapper.getStudentById(userId);
            if (student == null) {
                student = studentMapper.getStudentByUserId(userId);
            }
            
            // 获取系统分析的偏好（使用学生的实际ID）
            Integer actualStudentId = student != null ? student.getId() : userId;
            UserPreference preference = userPreferenceMapper.getUserPreferenceByUserId(actualStudentId);
            
            Map<String, Object> profile = new HashMap<>();
            
            if (student != null) {
                // 用户填写的偏好
                Map<String, Object> userPreferences = new HashMap<>();
                userPreferences.put("researchInterests", student.getResearchInterests());
                userPreferences.put("expectedResearchDirection", student.getExpectedResearchDirection());
                userPreferences.put("personalAbilities", student.getPersonalAbilities());
                userPreferences.put("preferredMentorStyle", student.getPreferredMentorStyle());
                userPreferences.put("programmingSkills", student.getProgrammingSkills());
                userPreferences.put("projectExperience", student.getProjectExperience());
                userPreferences.put("availableTime", student.getAvailableTime());
                userPreferences.put("keywords", student.getKeywords());
                userPreferences.put("bio", student.getBio());
                
                profile.put("userPreferences", userPreferences);
                profile.put("student", student);
            }
            
            if (preference != null) {
                // 系统分析的偏好
                Map<String, Object> systemAnalysis = new HashMap<>();
                systemAnalysis.put("summary", preference.getPreferenceText());
                systemAnalysis.put("keywords", preference.getPreferenceKeywords());
                systemAnalysis.put("topics", preference.getPreferenceTopics());
                systemAnalysis.put("analysisCount", preference.getAnalysisCount());
                systemAnalysis.put("updateTime", preference.getUpdateTime());
                
                profile.put("systemAnalysis", systemAnalysis);
            }
            
            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", profile);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取档案失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Analyze user preferences
     * 分析用户偏好
     */
    @PostMapping("/analyze/{userId}")
    public Map<String, Object> analyzePreferences(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Trigger preference analysis
            List<Map<String, Object>> recommendations = recommendationService.getRecommendations(userId);
            
            // Get updated preferences
            UserPreference preference = userPreferenceMapper.getUserPreferenceByUserId(userId);

            result.put("code", 0);
            result.put("message", "分析完成");
            result.put("data", preference);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "分析失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Track user behavior
     * 记录用户行为
     */
    @PostMapping("/track")
    public Map<String, Object> trackBehavior(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            Integer userId = (Integer) params.get("userId");
            String userType = (String) params.get("userType");
            String targetType = (String) params.get("targetType");
            Integer targetId = (Integer) params.get("targetId");
            String actionType = (String) params.get("actionType");
            Integer durationSeconds = (Integer) params.get("durationSeconds");

            if (userId == null || targetType == null || targetId == null || actionType == null) {
                result.put("code", 400);
                result.put("message", "缺少必要参数");
                return result;
            }

            userBehaviorService.trackBehavior(userId, userType, targetType, targetId, actionType, durationSeconds);

            result.put("code", 0);
            result.put("message", "行为记录成功");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "行为记录失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Invalidate recommendation cache
     * 使推荐缓存失效
     */
    @PostMapping("/invalidate-cache")
    public Map<String, Object> invalidateCache(
            @RequestParam(required = false) Integer studentId,
            @RequestParam(required = false) Integer mentorId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (studentId != null) {
                enhancedRecommendationService.invalidateMentorRecommendationCache(studentId);
                semanticRecommendationService.invalidateStudentCache(studentId);
            }
            if (mentorId != null) {
                enhancedRecommendationService.invalidateStudentRecommendationCache(mentorId);
                semanticRecommendationService.invalidateMentorCache(mentorId);
            }

            result.put("code", 0);
            result.put("message", "缓存已清除");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "清除缓存失败: " + e.getMessage());
        }

        return result;
    }
}
