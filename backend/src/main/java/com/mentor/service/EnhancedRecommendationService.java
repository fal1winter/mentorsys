package com.mentor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentor.entity.Mentor;
import com.mentor.entity.Student;
import com.mentor.mapper.MentorMapper;
import com.mentor.mapper.StudentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Enhanced Recommendation Service
 * 增强版推荐服务
 */
@Slf4j
@Service
public class EnhancedRecommendationService {

    @Autowired
    private MilvusService milvusService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private RecommendationScorer scorer;

    @Autowired
    private LLMService llmService;

    @Autowired
    private MentorMapper mentorMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${recommendation.cache-ttl:3600}")
    private Long cacheTtl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String REDIS_KEY_PREFIX_MENTOR_REC = "recommendation:mentor:";
    private static final String REDIS_KEY_PREFIX_STUDENT_REC = "recommendation:student:";

    /**
     * Get mentor recommendations for student
     * 为学生获取导师推荐
     */
    public List<Map<String, Object>> getMentorRecommendations(Integer studentId, Integer limit) {
        try {
            // Check cache first
            String cacheKey = REDIS_KEY_PREFIX_MENTOR_REC + studentId;
            List<Map<String, Object>> cachedResult = getCachedRecommendations(cacheKey);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                log.info("Using cached mentor recommendations for student: {}", studentId);
                return cachedResult.stream().limit(limit).collect(Collectors.toList());
            }

            // Get student profile
            Student student = studentMapper.getStudentById(studentId);
            if (student == null) {
                throw new RuntimeException("Student not found: " + studentId);
            }

            // Generate student embedding
            List<Float> studentEmbedding = embeddingService.generateStudentEmbedding(student);

            // Sync student to Milvus if needed
            syncStudentToMilvus(student, studentEmbedding);

            // Search for similar mentors in Milvus
            List<Map<String, Object>> similarMentors = milvusService.searchSimilarMentors(studentEmbedding, 50);

            // Score and rank mentors
            List<Map<String, Object>> scoredMentors = new ArrayList<>();
            for (Map<String, Object> milvusResult : similarMentors) {
                Long mentorId = (Long) milvusResult.get("id");
                Double vectorSimilarity = (Double) milvusResult.get("score");

                // Get full mentor details
                Mentor mentor = mentorMapper.getMentorById(mentorId.intValue());
                if (mentor == null) {
                    continue;
                }

                // Calculate multi-dimensional score
                Map<String, Object> scoreResult = scorer.scoreStudentToMentor(student, mentor, vectorSimilarity);
                Double totalScore = (Double) scoreResult.get("totalScore");

                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("mentor", mentor);
                recommendation.put("score", totalScore);
                recommendation.put("matchDetails", scoreResult.get("details"));

                scoredMentors.add(recommendation);
            }

            // Sort by total score
            scoredMentors.sort((r1, r2) -> Double.compare(
                    (Double) r2.get("score"),
                    (Double) r1.get("score")
            ));

            // Take top 20 for LLM analysis
            List<Map<String, Object>> topMentors = scoredMentors.stream()
                    .limit(20)
                    .collect(Collectors.toList());

            // Generate recommendation reasons using LLM
            generateMentorRecommendationReasons(student, topMentors);

            // Take final top N
            List<Map<String, Object>> finalRecommendations = topMentors.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            // Cache the results
            cacheRecommendations(cacheKey, finalRecommendations);

            log.info("Generated {} mentor recommendations for student: {}", finalRecommendations.size(), studentId);
            return finalRecommendations;

        } catch (Exception e) {
            log.error("Failed to get mentor recommendations for student: {}", studentId, e);
            throw new RuntimeException("Failed to get mentor recommendations", e);
        }
    }

    /**
     * Get student recommendations for mentor
     * 为导师获取学生推荐
     */
    public List<Map<String, Object>> getStudentRecommendations(Integer mentorId, Integer limit) {
        try {
            // Check cache first
            String cacheKey = REDIS_KEY_PREFIX_STUDENT_REC + mentorId;
            List<Map<String, Object>> cachedResult = getCachedRecommendations(cacheKey);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                log.info("Using cached student recommendations for mentor: {}", mentorId);
                return cachedResult.stream().limit(limit).collect(Collectors.toList());
            }

            // Get mentor profile
            Mentor mentor = mentorMapper.getMentorById(mentorId);
            if (mentor == null) {
                throw new RuntimeException("Mentor not found: " + mentorId);
            }

            // Generate mentor embedding
            List<Float> mentorEmbedding = embeddingService.generateMentorEmbedding(mentor);

            // Sync mentor to Milvus if needed
            syncMentorToMilvus(mentor, mentorEmbedding);

            // Search for similar students in Milvus
            List<Map<String, Object>> similarStudents = milvusService.searchSimilarStudents(mentorEmbedding, 50);

            // Score and rank students
            List<Map<String, Object>> scoredStudents = new ArrayList<>();
            for (Map<String, Object> milvusResult : similarStudents) {
                Long studentId = (Long) milvusResult.get("id");
                Double vectorSimilarity = (Double) milvusResult.get("score");

                // Get full student details
                Student student = studentMapper.getStudentById(studentId.intValue());
                if (student == null) {
                    continue;
                }

                // Calculate multi-dimensional score
                Map<String, Object> scoreResult = scorer.scoreMentorToStudent(mentor, student, vectorSimilarity);
                Double totalScore = (Double) scoreResult.get("totalScore");

                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("student", student);
                recommendation.put("score", totalScore);
                recommendation.put("matchDetails", scoreResult.get("details"));

                scoredStudents.add(recommendation);
            }

            // Sort by total score
            scoredStudents.sort((r1, r2) -> Double.compare(
                    (Double) r2.get("score"),
                    (Double) r1.get("score")
            ));

            // Take top 20 for LLM analysis
            List<Map<String, Object>> topStudents = scoredStudents.stream()
                    .limit(20)
                    .collect(Collectors.toList());

            // Generate recommendation reasons using LLM
            generateStudentRecommendationReasons(mentor, topStudents);

            // Take final top N
            List<Map<String, Object>> finalRecommendations = topStudents.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            // Cache the results
            cacheRecommendations(cacheKey, finalRecommendations);

            log.info("Generated {} student recommendations for mentor: {}", finalRecommendations.size(), mentorId);
            return finalRecommendations;

        } catch (Exception e) {
            log.error("Failed to get student recommendations for mentor: {}", mentorId, e);
            throw new RuntimeException("Failed to get student recommendations", e);
        }
    }

    /**
     * Sync mentor to Milvus
     * 同步导师到Milvus
     */
    private void syncMentorToMilvus(Mentor mentor, List<Float> embedding) {
        try {
            milvusService.upsertMentorProfile(
                    mentor.getId().longValue(),
                    embedding,
                    mentor.getResearchAreas() != null ? mentor.getResearchAreas() : "",
                    mentor.getInstitution() != null ? mentor.getInstitution() : "",
                    mentor.getRatingAvg() != null ? mentor.getRatingAvg().floatValue() : 0.0f,
                    mentor.getAcceptingStudents() != null ? mentor.getAcceptingStudents() : false,
                    mentor.getCurrentStudents() != null ? mentor.getCurrentStudents() : 0,
                    mentor.getMaxStudents() != null ? mentor.getMaxStudents() : 10
            );
            log.debug("Synced mentor {} to Milvus", mentor.getId());
        } catch (Exception e) {
            log.warn("Failed to sync mentor {} to Milvus", mentor.getId(), e);
        }
    }

    /**
     * Sync student to Milvus
     * 同步学生到Milvus
     */
    private void syncStudentToMilvus(Student student, List<Float> embedding) {
        try {
            milvusService.upsertStudentProfile(
                    student.getId().longValue(),
                    embedding,
                    student.getResearchInterests() != null ? student.getResearchInterests() : "",
                    student.getCurrentInstitution() != null ? student.getCurrentInstitution() : "",
                    student.getDegreeLevel() != null ? student.getDegreeLevel() : "",
                    student.getGpa() != null ? student.getGpa().floatValue() : 0.0f,
                    student.getMajor() != null ? student.getMajor() : "",
                    student.getGraduationYear() != null ? student.getGraduationYear() : 0
            );
            log.debug("Synced student {} to Milvus", student.getId());
        } catch (Exception e) {
            log.warn("Failed to sync student {} to Milvus", student.getId(), e);
        }
    }

    /**
     * Generate recommendation reasons for mentors using LLM
     * 使用LLM生成导师推荐理由
     */
    private void generateMentorRecommendationReasons(Student student, List<Map<String, Object>> recommendations) {
        for (Map<String, Object> recommendation : recommendations) {
            try {
                Mentor mentor = (Mentor) recommendation.get("mentor");
                @SuppressWarnings("unchecked")
                Map<String, Double> matchDetails = (Map<String, Double>) recommendation.get("matchDetails");

                // Build prompt for LLM
                String prompt = buildMentorRecommendationPrompt(student, mentor, matchDetails);

                // Call LLM to generate reason
                String reason = llmService.callLLM(prompt);

                // Clean up the response
                reason = reason.trim();
                if (reason.startsWith("\"") && reason.endsWith("\"")) {
                    reason = reason.substring(1, reason.length() - 1);
                }

                recommendation.put("reason", reason);

            } catch (Exception e) {
                log.warn("Failed to generate recommendation reason for mentor {}", 
                        ((Mentor) recommendation.get("mentor")).getId(), e);
                recommendation.put("reason", "该导师的研究方向与您的兴趣高度匹配，值得考虑。");
            }
        }
    }

    /**
     * Generate recommendation reasons for students using LLM
     * 使用LLM生成学生推荐理由
     */
    private void generateStudentRecommendationReasons(Mentor mentor, List<Map<String, Object>> recommendations) {
        for (Map<String, Object> recommendation : recommendations) {
            try {
                Student student = (Student) recommendation.get("student");
                @SuppressWarnings("unchecked")
                Map<String, Double> matchDetails = (Map<String, Double>) recommendation.get("matchDetails");

                // Build prompt for LLM
                String prompt = buildStudentRecommendationPrompt(mentor, student, matchDetails);

                // Call LLM to generate reason
                String reason = llmService.callLLM(prompt);

                // Clean up the response
                reason = reason.trim();
                if (reason.startsWith("\"") && reason.endsWith("\"")) {
                    reason = reason.substring(1, reason.length() - 1);
                }

                recommendation.put("reason", reason);

            } catch (Exception e) {
                log.warn("Failed to generate recommendation reason for student {}", 
                        ((Student) recommendation.get("student")).getId(), e);
                recommendation.put("reason", "该学生的研究兴趣与您的方向高度契合，值得考虑。");
            }
        }
    }

    /**
     * Build prompt for mentor recommendation
     * 构建导师推荐提示词
     */
    private String buildMentorRecommendationPrompt(Student student, Mentor mentor, Map<String, Double> matchDetails) {
        return String.format(
                "请为学生推荐导师，生成简短的推荐理由（50字以内）。\n\n" +
                "学生信息：\n" +
                "- 姓名：%s\n" +
                "- 学历：%s\n" +
                "- 专业：%s\n" +
                "- 研究兴趣：%s\n\n" +
                "导师信息：\n" +
                "- 姓名：%s\n" +
                "- 职称：%s\n" +
                "- 机构：%s\n" +
                "- 研究方向：%s\n" +
                "- 平均评分：%.1f\n\n" +
                "匹配详情：\n" +
                "- 研究方向匹配度：%.2f\n" +
                "- 导师质量得分：%.2f\n" +
                "- 接收能力得分：%.2f\n" +
                "- 工作强度得分：%.2f\n\n" +
                "请直接返回推荐理由，不要包含其他内容。",
                student.getName(),
                student.getDegreeLevel() != null ? student.getDegreeLevel() : "未知",
                student.getMajor() != null ? student.getMajor() : "未知",
                student.getResearchInterests() != null ? student.getResearchInterests() : "未知",
                mentor.getName(),
                mentor.getTitle() != null ? mentor.getTitle() : "未知",
                mentor.getInstitution() != null ? mentor.getInstitution() : "未知",
                mentor.getResearchAreas() != null ? mentor.getResearchAreas() : "未知",
                mentor.getRatingAvg() != null ? mentor.getRatingAvg() : 0.0,
                matchDetails.get("researchMatch"),
                matchDetails.get("qualityScore"),
                matchDetails.get("availabilityScore"),
                matchDetails.get("workloadScore")
        );
    }

    /**
     * Build prompt for student recommendation
     * 构建学生推荐提示词
     */
    private String buildStudentRecommendationPrompt(Mentor mentor, Student student, Map<String, Double> matchDetails) {
        return String.format(
                "请为导师推荐学生，生成简短的推荐理由（50字以内）。\n\n" +
                "导师信息：\n" +
                "- 姓名：%s\n" +
                "- 职称：%s\n" +
                "- 研究方向：%s\n\n" +
                "学生信息：\n" +
                "- 姓名：%s\n" +
                "- 学历：%s\n" +
                "- 专业：%s\n" +
                "- GPA：%.2f\n" +
                "- 研究兴趣：%s\n" +
                "- 毕业年份：%d\n\n" +
                "匹配详情：\n" +
                "- 研究兴趣匹配度：%.2f\n" +
                "- 学术能力得分：%.2f\n" +
                "- 背景匹配得分：%.2f\n" +
                "- 时间匹配得分：%.2f\n\n" +
                "请直接返回推荐理由，不要包含其他内容。",
                mentor.getName(),
                mentor.getTitle() != null ? mentor.getTitle() : "未知",
                mentor.getResearchAreas() != null ? mentor.getResearchAreas() : "未知",
                student.getName(),
                student.getDegreeLevel() != null ? student.getDegreeLevel() : "未知",
                student.getMajor() != null ? student.getMajor() : "未知",
                student.getGpa() != null ? student.getGpa() : 0.0,
                student.getResearchInterests() != null ? student.getResearchInterests() : "未知",
                student.getGraduationYear() != null ? student.getGraduationYear() : 0,
                matchDetails.get("researchMatch"),
                matchDetails.get("academicAbility"),
                matchDetails.get("backgroundMatch"),
                matchDetails.get("timeMatch")
        );
    }

    /**
     * Get cached recommendations from Redis
     * 从Redis获取缓存的推荐结果
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getCachedRecommendations(String cacheKey) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                return (List<Map<String, Object>>) cached;
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get cached recommendations: {}", cacheKey, e);
            return null;
        }
    }

    /**
     * Cache recommendations in Redis
     * 在Redis中缓存推荐结果
     */
    private void cacheRecommendations(String cacheKey, List<Map<String, Object>> recommendations) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, recommendations, cacheTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache recommendations: {}", cacheKey, e);
        }
    }

    /**
     * Invalidate mentor recommendation cache for student
     * 使学生的导师推荐缓存失效
     */
    public void invalidateMentorRecommendationCache(Integer studentId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String cacheKey = REDIS_KEY_PREFIX_MENTOR_REC + studentId;
            redisTemplate.delete(cacheKey);
            log.debug("Invalidated mentor recommendation cache for student: {}", studentId);
        } catch (Exception e) {
            log.warn("Failed to invalidate mentor recommendation cache: {}", studentId, e);
        }
    }

    /**
     * Invalidate student recommendation cache for mentor
     * 使导师的学生推荐缓存失效
     */
    public void invalidateStudentRecommendationCache(Integer mentorId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String cacheKey = REDIS_KEY_PREFIX_STUDENT_REC + mentorId;
            redisTemplate.delete(cacheKey);
            log.debug("Invalidated student recommendation cache for mentor: {}", mentorId);
        } catch (Exception e) {
            log.warn("Failed to invalidate student recommendation cache: {}", mentorId, e);
        }
    }
}
