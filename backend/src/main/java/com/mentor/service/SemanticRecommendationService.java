package com.mentor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentor.entity.Mentor;
import com.mentor.entity.Student;
import com.mentor.entity.UserPreference;
import com.mentor.mapper.MentorMapper;
import com.mentor.mapper.StudentMapper;
import com.mentor.mapper.UserPreferenceMapper;
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
 * Semantic Recommendation Service
 * 基于语义检索的增强推荐服务
 * 对用户的每条偏好进行语义检索，综合匹配结果进行推荐
 */
@Slf4j
@Service
public class SemanticRecommendationService {

    @Autowired
    private MentorMapper mentorMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private UserPreferenceMapper userPreferenceMapper;

    @Autowired
    private LLMService llmService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${semantic.service-url}")
    private String semanticServiceUrl;

    @Value("${semantic.timeout:10000}")
    private Integer timeout;

    @Value("${recommendation.cache-ttl:3600}")
    private Long cacheTtl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CACHE_PREFIX_MENTOR = "semantic:mentor:";
    private static final String CACHE_PREFIX_STUDENT = "semantic:student:";

    /**
     * 为学生获取导师推荐（基于多维度语义检索）
     */
    public List<Map<String, Object>> getMentorRecommendationsForStudent(Integer studentId, Integer limit) {
        try {
            // 检查缓存
            String cacheKey = CACHE_PREFIX_MENTOR + studentId;
            List<Map<String, Object>> cached = getCachedResult(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                return cached.stream().limit(limit).collect(Collectors.toList());
            }

            // 获取学生信息
            Student student = studentMapper.getStudentById(studentId);
            if (student == null) {
                log.warn("Student not found: {}, returning fallback recommendations", studentId);
                return getFallbackMentorRecommendations(studentId, limit);
            }

            // 获取用户偏好（LLM分析的）
            UserPreference preference = userPreferenceMapper.getUserPreferenceByUserId(studentId);

            // 构建多维度查询条件
            List<SearchCriteria> criteriaList = buildStudentSearchCriteria(student, preference);

            // 对每个条件进行语义检索
            Map<Integer, MentorMatchResult> mentorScores = new HashMap<>();
            
            for (SearchCriteria criteria : criteriaList) {
                List<Map<String, Object>> searchResults = semanticSearchMentors(criteria.query, 30);
                
                for (Map<String, Object> result : searchResults) {
                    Integer mentorId = ((Number) result.get("id")).intValue();
                    double score = ((Number) result.get("score")).doubleValue();
                    
                    MentorMatchResult matchResult = mentorScores.computeIfAbsent(
                        mentorId, k -> new MentorMatchResult(mentorId)
                    );
                    matchResult.addScore(criteria.dimension, score * criteria.weight);
                }
            }

            // 获取导师详情并计算综合得分
            List<Map<String, Object>> recommendations = new ArrayList<>();
            for (MentorMatchResult matchResult : mentorScores.values()) {
                Mentor mentor = mentorMapper.getMentorById(matchResult.mentorId);
                if (mentor == null || mentor.getStatus() != 1) continue;

                // 计算综合得分
                double totalScore = matchResult.getTotalScore();
                
                // 添加额外评分因素
                double bonus = calculateMentorBonus(mentor);
                totalScore += bonus;
                
                // 将 bonus 也加入到 matchDetails
                Map<String, Double> detailScores = matchResult.getDetailScores();
                if (bonus > 0) {
                    detailScores.put("quality_bonus", bonus);
                }

                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("mentor", mentor);
                recommendation.put("score", Math.min(totalScore, 1.0));
                recommendation.put("matchDetails", detailScores);
                
                recommendations.add(recommendation);
            }

            // 如果语义检索没有结果，使用基于关键词的简单匹配
            if (recommendations.isEmpty()) {
                log.info("Semantic search returned no results, falling back to keyword matching for student: {}", studentId);
                recommendations = getKeywordBasedMentorRecommendations(student, preference, limit * 2);
            }

            // 按得分排序
            recommendations.sort((a, b) -> Double.compare(
                (Double) b.get("score"), (Double) a.get("score")
            ));

            // 取 top15 进行 LLM 细排，输出 top10
            List<Map<String, Object>> topCandidates = recommendations.stream()
                .limit(15)
                .collect(Collectors.toList());

            List<Map<String, Object>> finalResult;
            if (topCandidates.size() > 0) {
                finalResult = llmRerankMentors(student, preference, topCandidates, limit);
            } else {
                finalResult = topCandidates;
            }

            // 缓存结果
            cacheResult(cacheKey, finalResult);

            return finalResult;

        } catch (Exception e) {
            log.error("Failed to get mentor recommendations for student: {}, using fallback", studentId, e);
            return getFallbackMentorRecommendations(studentId, limit);
        }
    }

    /**
     * 为导师获取学生推荐（基于多维度语义检索）
     */
    public List<Map<String, Object>> getStudentRecommendationsForMentor(Integer mentorId, Integer limit) {
        try {
            // 检查缓存
            String cacheKey = CACHE_PREFIX_STUDENT + mentorId;
            List<Map<String, Object>> cached = getCachedResult(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                return cached.stream().limit(limit).collect(Collectors.toList());
            }

            // 获取导师信息
            Mentor mentor = mentorMapper.getMentorById(mentorId);
            if (mentor == null) {
                throw new RuntimeException("Mentor not found: " + mentorId);
            }

            // 构建多维度查询条件
            List<SearchCriteria> criteriaList = buildMentorSearchCriteria(mentor);

            // 对每个条件进行语义检索
            Map<Integer, StudentMatchResult> studentScores = new HashMap<>();
            
            for (SearchCriteria criteria : criteriaList) {
                List<Map<String, Object>> searchResults = semanticSearchStudents(criteria.query, 30);
                
                for (Map<String, Object> result : searchResults) {
                    Integer studentId = ((Number) result.get("id")).intValue();
                    double score = ((Number) result.get("score")).doubleValue();
                    
                    StudentMatchResult matchResult = studentScores.computeIfAbsent(
                        studentId, k -> new StudentMatchResult(studentId)
                    );
                    matchResult.addScore(criteria.dimension, score * criteria.weight);
                }
            }

            // 获取学生详情并计算综合得分
            List<Map<String, Object>> recommendations = new ArrayList<>();
            for (StudentMatchResult matchResult : studentScores.values()) {
                Student student = studentMapper.getStudentById(matchResult.studentId);
                if (student == null || student.getStatus() != 1) continue;

                // 计算综合得分
                double totalScore = matchResult.getTotalScore();
                
                // 添加额外评分因素
                double bonus = calculateStudentBonus(student);
                totalScore += bonus;
                
                // 将 bonus 也加入到 matchDetails
                Map<String, Double> detailScores = matchResult.getDetailScores();
                if (bonus > 0) {
                    detailScores.put("student_bonus", bonus);
                }

                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("student", student);
                recommendation.put("score", Math.min(totalScore, 1.0));
                recommendation.put("matchDetails", detailScores);
                
                recommendations.add(recommendation);
            }

            // 按得分排序
            recommendations.sort((a, b) -> Double.compare(
                (Double) b.get("score"), (Double) a.get("score")
            ));

            // 取 top15 进行 LLM 细排，输出 top10
            List<Map<String, Object>> topCandidates = recommendations.stream()
                .limit(15)
                .collect(Collectors.toList());

            List<Map<String, Object>> finalResult;
            if (topCandidates.size() > 0) {
                finalResult = llmRerankStudents(mentor, topCandidates, limit);
            } else {
                finalResult = topCandidates;
            }

            // 缓存结果
            cacheResult(cacheKey, finalResult);

            return finalResult;

        } catch (Exception e) {
            log.error("Failed to get student recommendations for mentor: {}", mentorId, e);
            throw new RuntimeException("获取推荐失败", e);
        }
    }

    /**
     * 构建学生的多维度搜索条件
     */
    private List<SearchCriteria> buildStudentSearchCriteria(Student student, UserPreference preference) {
        List<SearchCriteria> criteria = new ArrayList<>();

        // 1. 研究兴趣匹配 (权重: 0.30)
        if (student.getResearchInterests() != null && !student.getResearchInterests().isEmpty()) {
            criteria.add(new SearchCriteria(
                "research_interests",
                "研究方向: " + student.getResearchInterests(),
                0.30
            ));
        }

        // 2. 期望研究方向 (权重: 0.25)
        if (student.getExpectedResearchDirection() != null && !student.getExpectedResearchDirection().isEmpty()) {
            criteria.add(new SearchCriteria(
                "expected_direction",
                "期望研究方向: " + student.getExpectedResearchDirection(),
                0.25
            ));
        }

        // 3. 个人能力匹配 (权重: 0.15)
        if (student.getPersonalAbilities() != null && !student.getPersonalAbilities().isEmpty()) {
            criteria.add(new SearchCriteria(
                "personal_abilities",
                "学生能力: " + student.getPersonalAbilities(),
                0.15
            ));
        }

        // 4. 编程技能匹配 (权重: 0.10)
        if (student.getProgrammingSkills() != null && !student.getProgrammingSkills().isEmpty()) {
            criteria.add(new SearchCriteria(
                "programming_skills",
                "编程技能: " + student.getProgrammingSkills(),
                0.10
            ));
        }

        // 5. LLM分析的偏好 (权重: 0.20)
        if (preference != null && preference.getPreferenceText() != null) {
            criteria.add(new SearchCriteria(
                "llm_preference",
                "用户偏好: " + preference.getPreferenceText(),
                0.20
            ));
        }

        // 如果没有任何条件，使用默认查询
        if (criteria.isEmpty()) {
            criteria.add(new SearchCriteria(
                "default",
                "计算机科学 人工智能 机器学习",
                1.0
            ));
        }

        return criteria;
    }

    /**
     * 构建导师的多维度搜索条件
     */
    private List<SearchCriteria> buildMentorSearchCriteria(Mentor mentor) {
        List<SearchCriteria> criteria = new ArrayList<>();

        // 1. 研究方向匹配 (权重: 0.30)
        if (mentor.getResearchAreas() != null && !mentor.getResearchAreas().isEmpty()) {
            criteria.add(new SearchCriteria(
                "research_areas",
                "研究方向: " + mentor.getResearchAreas(),
                0.30
            ));
        }

        // 2. 组内方向匹配 (权重: 0.25)
        if (mentor.getGroupDirection() != null && !mentor.getGroupDirection().isEmpty()) {
            criteria.add(new SearchCriteria(
                "group_direction",
                "组内研究方向: " + mentor.getGroupDirection(),
                0.25
            ));
        }

        // 3. 期望学生素质 (权重: 0.25)
        if (mentor.getExpectedStudentQualities() != null && !mentor.getExpectedStudentQualities().isEmpty()) {
            criteria.add(new SearchCriteria(
                "expected_qualities",
                "期望学生: " + mentor.getExpectedStudentQualities(),
                0.25
            ));
        }

        // 4. 关键词匹配 (权重: 0.20)
        if (mentor.getKeywords() != null && !mentor.getKeywords().isEmpty()) {
            criteria.add(new SearchCriteria(
                "keywords",
                "关键词: " + mentor.getKeywords(),
                0.20
            ));
        }

        // 如果没有任何条件，使用默认查询
        if (criteria.isEmpty()) {
            criteria.add(new SearchCriteria(
                "default",
                "计算机科学 人工智能 机器学习",
                1.0
            ));
        }

        return criteria;
    }

    /**
     * 语义搜索导师
     */
    private List<Map<String, Object>> semanticSearchMentors(String query, int topK) {
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

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("query", query);
                requestBody.put("topK", topK);

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                request.setEntity(new StringEntity(jsonBody, "UTF-8"));

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode != 200) {
                        log.warn("Mentor search failed: {}", responseBody);
                        return Collections.emptyList();
                    }

                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    JsonNode resultsNode = jsonResponse.path("results");

                    List<Map<String, Object>> results = new ArrayList<>();
                    if (resultsNode.isArray()) {
                        for (JsonNode node : resultsNode) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("id", node.path("id").asLong());
                            result.put("score", node.path("score").asDouble());
                            results.add(result);
                        }
                    }
                    return results;
                }
            }
        } catch (Exception e) {
            log.error("Semantic search mentors failed: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * 语义搜索学生
     */
    private List<Map<String, Object>> semanticSearchStudents(String query, int topK) {
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

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("query", query);
                requestBody.put("topK", topK);

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                request.setEntity(new StringEntity(jsonBody, "UTF-8"));

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode != 200) {
                        log.warn("Student search failed: {}", responseBody);
                        return Collections.emptyList();
                    }

                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    JsonNode resultsNode = jsonResponse.path("results");

                    List<Map<String, Object>> results = new ArrayList<>();
                    if (resultsNode.isArray()) {
                        for (JsonNode node : resultsNode) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("id", node.path("id").asLong());
                            result.put("score", node.path("score").asDouble());
                            results.add(result);
                        }
                    }
                    return results;
                }
            }
        } catch (Exception e) {
            log.error("Semantic search students failed: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * 计算导师额外加分
     */
    private double calculateMentorBonus(Mentor mentor) {
        double bonus = 0.0;
        
        // 评分加分
        if (mentor.getRatingAvg() != null) {
            bonus += mentor.getRatingAvg().doubleValue() * 0.02; // 最多0.1
        }
        
        // 接收学生加分
        if (Boolean.TRUE.equals(mentor.getAcceptingStudents())) {
            bonus += 0.05;
        }
        
        // 有名额加分
        if (mentor.getMaxStudents() != null && mentor.getCurrentStudents() != null) {
            int available = mentor.getMaxStudents() - mentor.getCurrentStudents();
            if (available > 0) {
                bonus += Math.min(available * 0.02, 0.1);
            }
        }
        
        return bonus;
    }

    /**
     * 计算学生额外加分
     */
    private double calculateStudentBonus(Student student) {
        double bonus = 0.0;
        
        // GPA加分
        if (student.getGpa() != null) {
            bonus += (student.getGpa().doubleValue() - 2.0) * 0.05; // 3.0 GPA = 0.05, 4.0 GPA = 0.1
        }
        
        // 发表论文加分
        if (student.getPublicationsCount() != null && student.getPublicationsCount() > 0) {
            bonus += Math.min(student.getPublicationsCount() * 0.03, 0.15);
        }
        
        return Math.max(bonus, 0);
    }

    /**
     * LLM 细排：对 top15 导师候选进行重排序，输出 top10 及理由
     */
    private List<Map<String, Object>> llmRerankMentors(Student student, UserPreference preference,
                                                        List<Map<String, Object>> candidates, int limit) {
        if (candidates.isEmpty()) return candidates;
        
        try {
            // 构建重排序 prompt
            String prompt = buildMentorRerankPrompt(student, preference, candidates, limit);
            log.info("LLM rerank prompt length: {}", prompt.length());
            
            String response = llmService.callLLM(prompt);
            log.info("LLM rerank response: {}", response);
            
            // 解析 LLM 响应，获取排序结果
            List<Map<String, Object>> rerankedResult = parseMentorRerankResponse(response, candidates, limit);
            
            if (rerankedResult != null && !rerankedResult.isEmpty()) {
                return rerankedResult;
            }
        } catch (Exception e) {
            log.warn("LLM rerank failed, using original order with template reasons", e);
        }
        
        // 降级：使用原排序 + 模板理由
        return candidates.stream()
            .limit(limit)
            .peek(rec -> {
                Mentor mentor = (Mentor) rec.get("mentor");
                rec.put("reason", generateTemplateReason(mentor, student));
            })
            .collect(Collectors.toList());
    }

    /**
     * 构建导师重排序 prompt
     */
    private String buildMentorRerankPrompt(Student student, UserPreference preference,
                                           List<Map<String, Object>> candidates, int limit) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个导师推荐系统的排序专家。请根据学生信息，从以下候选导师中选出最匹配的")
          .append(limit).append("位，并按匹配度从高到低排序。\n\n");
        
        // 学生信息
        sb.append("【学生信息】\n");
        sb.append("姓名：").append(student.getName()).append("\n");
        sb.append("当前学校：").append(nullSafe(student.getCurrentInstitution())).append("\n");
        sb.append("专业：").append(nullSafe(student.getMajor())).append("\n");
        sb.append("学位：").append(nullSafe(student.getDegreeLevel())).append("\n");
        sb.append("研究兴趣：").append(nullSafe(student.getResearchInterests())).append("\n");
        sb.append("期望研究方向：").append(nullSafe(student.getExpectedResearchDirection())).append("\n");
        sb.append("个人能力：").append(nullSafe(student.getPersonalAbilities())).append("\n");
        sb.append("编程技能：").append(nullSafe(student.getProgrammingSkills())).append("\n");
        sb.append("期望导师风格：").append(nullSafe(student.getPreferredMentorStyle())).append("\n");
        if (preference != null && preference.getPreferenceText() != null) {
            sb.append("系统分析偏好：").append(preference.getPreferenceText()).append("\n");
        }
        
        // 候选导师列表
        sb.append("\n【候选导师列表】\n");
        for (int i = 0; i < candidates.size(); i++) {
            Mentor mentor = (Mentor) candidates.get(i).get("mentor");
            Double score = (Double) candidates.get(i).get("score");
            sb.append("编号").append(i + 1).append("：\n");
            sb.append("  姓名：").append(mentor.getName()).append("\n");
            sb.append("  职称：").append(nullSafe(mentor.getTitle())).append("\n");
            sb.append("  机构：").append(nullSafe(mentor.getInstitution())).append(" ").append(nullSafe(mentor.getDepartment())).append("\n");
            sb.append("  研究方向：").append(nullSafe(mentor.getResearchAreas())).append("\n");
            sb.append("  关键词：").append(nullSafe(mentor.getKeywords())).append("\n");
            sb.append("  组内方向：").append(nullSafe(mentor.getGroupDirection())).append("\n");
            sb.append("  期望学生：").append(nullSafe(mentor.getExpectedStudentQualities())).append("\n");
            sb.append("  指导风格：").append(nullSafe(mentor.getMentoringStyle())).append("\n");
            sb.append("  招生状态：").append(Boolean.TRUE.equals(mentor.getAcceptingStudents()) ? "正在招生" : "暂不招生").append("\n");
            sb.append("  可招名额：").append(mentor.getAvailablePositions() != null ? mentor.getAvailablePositions() : 0).append("\n");
            sb.append("  初筛得分：").append(String.format("%.2f", score)).append("\n\n");
        }
        
        // 输出格式要求
        sb.append("【输出要求】\n");
        sb.append("请严格按以下JSON格式返回，选出").append(limit).append("位最匹配的导师：\n");
        sb.append("```json\n");
        sb.append("[\n");
        sb.append("  {\"rank\": 1, \"id\": 编号, \"reason\": \"推荐理由（60字以内）\"},\n");
        sb.append("  {\"rank\": 2, \"id\": 编号, \"reason\": \"推荐理由（60字以内）\"},\n");
        sb.append("  ...\n");
        sb.append("]\n");
        sb.append("```\n");
        sb.append("注意：id 是上面候选列表中的编号（1-").append(candidates.size()).append("），不是导师ID。");
        
        return sb.toString();
    }

    /**
     * 解析导师重排序响应
     */
    private List<Map<String, Object>> parseMentorRerankResponse(String response, 
                                                                  List<Map<String, Object>> candidates, int limit) {
        if (response == null || response.isEmpty()) return null;
        
        try {
            // 提取 JSON 部分
            String jsonStr = extractJsonArray(response);
            if (jsonStr == null) return null;
            
            JsonNode jsonArray = objectMapper.readTree(jsonStr);
            if (!jsonArray.isArray()) return null;
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode item : jsonArray) {
                if (result.size() >= limit) break;
                
                int id = item.path("id").asInt();
                String reason = item.path("reason").asText();
                
                // id 是 1-based 索引
                if (id >= 1 && id <= candidates.size()) {
                    Map<String, Object> rec = new HashMap<>(candidates.get(id - 1));
                    rec.put("reason", reason != null && !reason.isEmpty() ? reason : 
                        generateTemplateReason((Mentor) rec.get("mentor"), null));
                    rec.put("llmRank", result.size() + 1);
                    result.add(rec);
                }
            }
            
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            log.warn("Failed to parse LLM rerank response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * LLM 细排：对 top15 学生候选进行重排序，输出 top10 及理由
     */
    private List<Map<String, Object>> llmRerankStudents(Mentor mentor,
                                                         List<Map<String, Object>> candidates, int limit) {
        if (candidates.isEmpty()) return candidates;
        
        try {
            // 构建重排序 prompt
            String prompt = buildStudentRerankPrompt(mentor, candidates, limit);
            log.info("LLM student rerank prompt length: {}", prompt.length());
            
            String response = llmService.callLLM(prompt);
            log.info("LLM student rerank response: {}", response);
            
            // 解析 LLM 响应，获取排序结果
            List<Map<String, Object>> rerankedResult = parseStudentRerankResponse(response, candidates, mentor, limit);
            
            if (rerankedResult != null && !rerankedResult.isEmpty()) {
                return rerankedResult;
            }
        } catch (Exception e) {
            log.warn("LLM student rerank failed, using original order with template reasons", e);
        }
        
        // 降级：使用原排序 + 模板理由
        return candidates.stream()
            .limit(limit)
            .peek(rec -> {
                Student student = (Student) rec.get("student");
                rec.put("reason", generateStudentTemplateReason(student, mentor));
            })
            .collect(Collectors.toList());
    }

    /**
     * 构建学生重排序 prompt
     */
    private String buildStudentRerankPrompt(Mentor mentor, List<Map<String, Object>> candidates, int limit) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个学生推荐系统的排序专家。请根据导师信息，从以下候选学生中选出最匹配的")
          .append(limit).append("位，并按匹配度从高到低排序。\n\n");
        
        // 导师信息
        sb.append("【导师信息】\n");
        sb.append("姓名：").append(mentor.getName()).append("\n");
        sb.append("职称：").append(nullSafe(mentor.getTitle())).append("\n");
        sb.append("机构：").append(nullSafe(mentor.getInstitution())).append(" ").append(nullSafe(mentor.getDepartment())).append("\n");
        sb.append("研究方向：").append(nullSafe(mentor.getResearchAreas())).append("\n");
        sb.append("关键词：").append(nullSafe(mentor.getKeywords())).append("\n");
        sb.append("组内方向：").append(nullSafe(mentor.getGroupDirection())).append("\n");
        sb.append("期望学生素质：").append(nullSafe(mentor.getExpectedStudentQualities())).append("\n");
        sb.append("指导风格：").append(nullSafe(mentor.getMentoringStyle())).append("\n");
        sb.append("可招名额：").append(mentor.getAvailablePositions() != null ? mentor.getAvailablePositions() : 0).append("\n");
        
        // 候选学生列表
        sb.append("\n【候选学生列表】\n");
        for (int i = 0; i < candidates.size(); i++) {
            Student student = (Student) candidates.get(i).get("student");
            Double score = (Double) candidates.get(i).get("score");
            sb.append("编号").append(i + 1).append("：\n");
            sb.append("  姓名：").append(student.getName()).append("\n");
            sb.append("  学校：").append(nullSafe(student.getCurrentInstitution())).append("\n");
            sb.append("  专业：").append(nullSafe(student.getMajor())).append("\n");
            sb.append("  学位：").append(nullSafe(student.getDegreeLevel())).append("\n");
            sb.append("  GPA：").append(student.getGpa() != null ? student.getGpa().toString() : "未填写").append("\n");
            sb.append("  研究兴趣：").append(nullSafe(student.getResearchInterests())).append("\n");
            sb.append("  期望方向：").append(nullSafe(student.getExpectedResearchDirection())).append("\n");
            sb.append("  个人能力：").append(nullSafe(student.getPersonalAbilities())).append("\n");
            sb.append("  编程技能：").append(nullSafe(student.getProgrammingSkills())).append("\n");
            sb.append("  论文数：").append(student.getPublicationsCount() != null ? student.getPublicationsCount() : 0).append("\n");
            sb.append("  项目经验：").append(nullSafe(student.getProjectExperience())).append("\n");
            sb.append("  初筛得分：").append(String.format("%.2f", score)).append("\n\n");
        }
        
        // 输出格式要求
        sb.append("【输出要求】\n");
        sb.append("请严格按以下JSON格式返回，选出").append(limit).append("位最匹配的学生：\n");
        sb.append("```json\n");
        sb.append("[\n");
        sb.append("  {\"rank\": 1, \"id\": 编号, \"reason\": \"推荐理由（60字以内）\"},\n");
        sb.append("  {\"rank\": 2, \"id\": 编号, \"reason\": \"推荐理由（60字以内）\"},\n");
        sb.append("  ...\n");
        sb.append("]\n");
        sb.append("```\n");
        sb.append("注意：id 是上面候选列表中的编号（1-").append(candidates.size()).append("），不是学生ID。");
        
        return sb.toString();
    }

    /**
     * 解析学生重排序响应
     */
    private List<Map<String, Object>> parseStudentRerankResponse(String response, 
                                                                   List<Map<String, Object>> candidates, 
                                                                   Mentor mentor, int limit) {
        if (response == null || response.isEmpty()) return null;
        
        try {
            // 提取 JSON 部分
            String jsonStr = extractJsonArray(response);
            if (jsonStr == null) return null;
            
            JsonNode jsonArray = objectMapper.readTree(jsonStr);
            if (!jsonArray.isArray()) return null;
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode item : jsonArray) {
                if (result.size() >= limit) break;
                
                int id = item.path("id").asInt();
                String reason = item.path("reason").asText();
                
                // id 是 1-based 索引
                if (id >= 1 && id <= candidates.size()) {
                    Map<String, Object> rec = new HashMap<>(candidates.get(id - 1));
                    rec.put("reason", reason != null && !reason.isEmpty() ? reason : 
                        generateStudentTemplateReason((Student) rec.get("student"), mentor));
                    rec.put("llmRank", result.size() + 1);
                    result.add(rec);
                }
            }
            
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            log.warn("Failed to parse LLM student rerank response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 LLM 响应中提取 JSON 数组
     */
    private String extractJsonArray(String response) {
        if (response == null) return null;
        
        // 尝试找到 JSON 数组
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        
        return null;
    }

    /**
     * 生成导师推荐理由（优化版：一次LLM调用批量生成）
     */
    private void generateMentorReasons(Student student, UserPreference preference, 
                                       List<Map<String, Object>> recommendations) {
        int llmLimit = Math.min(5, recommendations.size());
        if (llmLimit == 0) return;
        
        try {
            // 构建批量prompt
            String batchPrompt = buildBatchMentorReasonPrompt(student, preference, 
                recommendations.subList(0, llmLimit));
            String response = llmService.callLLM(batchPrompt);
            
            // 解析批量响应
            String[] reasons = parseBatchReasons(response, llmLimit);
            for (int i = 0; i < llmLimit && i < reasons.length; i++) {
                String reason = cleanLLMResponse(reasons[i]);
                if (reason != null && !reason.isEmpty()) {
                    recommendations.get(i).put("reason", reason);
                } else {
                    recommendations.get(i).put("reason", 
                        generateTemplateReason((Mentor) recommendations.get(i).get("mentor"), student));
                }
            }
        } catch (Exception e) {
            log.warn("Batch LLM call failed, using templates", e);
            for (int i = 0; i < llmLimit; i++) {
                recommendations.get(i).put("reason", 
                    generateTemplateReason((Mentor) recommendations.get(i).get("mentor"), student));
            }
        }
        
        // 其余使用模板理由
        for (int i = llmLimit; i < recommendations.size(); i++) {
            Map<String, Object> rec = recommendations.get(i);
            Mentor mentor = (Mentor) rec.get("mentor");
            rec.put("reason", generateTemplateReason(mentor, student));
        }
    }

    /**
     * 构建批量导师推荐理由prompt
     */
    private String buildBatchMentorReasonPrompt(Student student, UserPreference preference,
                                                 List<Map<String, Object>> recs) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为学生推荐导师，为以下").append(recs.size()).append("位导师各生成一条简短推荐理由（每条40字以内）。\n\n");
        sb.append("学生信息：姓名=").append(student.getName());
        sb.append("，研究兴趣=").append(nullSafe(student.getResearchInterests()));
        sb.append("，期望方向=").append(nullSafe(student.getExpectedResearchDirection())).append("\n\n");
        
        for (int i = 0; i < recs.size(); i++) {
            Mentor mentor = (Mentor) recs.get(i).get("mentor");
            sb.append("导师").append(i + 1).append("：").append(mentor.getName());
            sb.append("，研究方向=").append(nullSafe(mentor.getResearchAreas())).append("\n");
        }
        
        sb.append("\n请按格式返回，每行一条：\n1. 理由1\n2. 理由2\n...");
        return sb.toString();
    }

    /**
     * 解析批量推荐理由响应
     */
    private String[] parseBatchReasons(String response, int count) {
        String[] reasons = new String[count];
        if (response == null) return reasons;
        
        String[] lines = response.split("\n");
        int idx = 0;
        for (String line : lines) {
            if (idx >= count) break;
            line = line.trim();
            // 移除序号前缀如 "1." "1、" "1:" 等
            if (line.matches("^\\d+[.、:：].*")) {
                line = line.replaceFirst("^\\d+[.、:：]\\s*", "");
            }
            if (!line.isEmpty()) {
                reasons[idx++] = line;
            }
        }
        return reasons;
    }

    /**
     * 生成模板推荐理由
     */
    private String generateTemplateReason(Mentor mentor, Student student) {
        String researchAreas = mentor.getResearchAreas();
        String studentInterests = student != null ? student.getResearchInterests() : null;
        
        if (researchAreas != null && studentInterests != null) {
            return String.format("%s教授在%s领域有深厚造诣，与您的研究兴趣高度契合。", 
                mentor.getName(), 
                researchAreas.length() > 20 ? researchAreas.substring(0, 20) + "..." : researchAreas);
        }
        if (researchAreas != null) {
            return String.format("%s教授在%s领域有深厚造诣，值得关注。", 
                mentor.getName(), 
                researchAreas.length() > 20 ? researchAreas.substring(0, 20) + "..." : researchAreas);
        }
        return "该导师的研究方向与您的兴趣高度匹配。";
    }

    /**
     * 生成学生推荐理由（优化版：一次LLM调用批量生成）
     */
    private void generateStudentReasons(Mentor mentor, List<Map<String, Object>> recommendations) {
        int llmLimit = Math.min(5, recommendations.size());
        if (llmLimit == 0) return;
        
        try {
            // 构建批量prompt
            String batchPrompt = buildBatchStudentReasonPrompt(mentor, 
                recommendations.subList(0, llmLimit));
            String response = llmService.callLLM(batchPrompt);
            
            // 解析批量响应
            String[] reasons = parseBatchReasons(response, llmLimit);
            for (int i = 0; i < llmLimit && i < reasons.length; i++) {
                String reason = cleanLLMResponse(reasons[i]);
                if (reason != null && !reason.isEmpty()) {
                    recommendations.get(i).put("reason", reason);
                } else {
                    recommendations.get(i).put("reason", 
                        generateStudentTemplateReason((Student) recommendations.get(i).get("student"), mentor));
                }
            }
        } catch (Exception e) {
            log.warn("Batch LLM call failed, using templates", e);
            for (int i = 0; i < llmLimit; i++) {
                recommendations.get(i).put("reason", 
                    generateStudentTemplateReason((Student) recommendations.get(i).get("student"), mentor));
            }
        }
        
        // 其余使用模板理由
        for (int i = llmLimit; i < recommendations.size(); i++) {
            Map<String, Object> rec = recommendations.get(i);
            Student student = (Student) rec.get("student");
            rec.put("reason", generateStudentTemplateReason(student, mentor));
        }
    }

    /**
     * 构建批量学生推荐理由prompt
     */
    private String buildBatchStudentReasonPrompt(Mentor mentor, List<Map<String, Object>> recs) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为导师推荐学生，为以下").append(recs.size()).append("位学生各生成一条简短推荐理由（每条40字以内）。\n\n");
        sb.append("导师信息：姓名=").append(mentor.getName());
        sb.append("，研究方向=").append(nullSafe(mentor.getResearchAreas()));
        sb.append("，期望学生=").append(nullSafe(mentor.getExpectedStudentQualities())).append("\n\n");
        
        for (int i = 0; i < recs.size(); i++) {
            Student student = (Student) recs.get(i).get("student");
            sb.append("学生").append(i + 1).append("：").append(student.getName());
            sb.append("，研究兴趣=").append(nullSafe(student.getResearchInterests()));
            sb.append("，能力=").append(nullSafe(student.getPersonalAbilities())).append("\n");
        }
        
        sb.append("\n请按格式返回，每行一条：\n1. 理由1\n2. 理由2\n...");
        return sb.toString();
    }

    /**
     * 生成学生模板推荐理由
     */
    private String generateStudentTemplateReason(Student student, Mentor mentor) {
        String interests = student.getResearchInterests();
        String mentorAreas = mentor.getResearchAreas();
        
        if (interests != null && mentorAreas != null) {
            return String.format("%s同学在%s方面有研究兴趣，与您的研究方向契合度高。", 
                student.getName(), 
                interests.length() > 15 ? interests.substring(0, 15) + "..." : interests);
        }
        return "该学生的背景与您的研究方向高度契合。";
    }

    private String buildMentorReasonPrompt(Student student, UserPreference preference, 
                                           Mentor mentor, Map<String, Double> details) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为学生推荐导师，生成简短的推荐理由（60字以内）。\n\n");
        sb.append("学生信息：\n");
        sb.append("- 姓名：").append(student.getName()).append("\n");
        sb.append("- 研究兴趣：").append(nullSafe(student.getResearchInterests())).append("\n");
        sb.append("- 期望方向：").append(nullSafe(student.getExpectedResearchDirection())).append("\n");
        sb.append("- 个人能力：").append(nullSafe(student.getPersonalAbilities())).append("\n");
        
        if (preference != null && preference.getPreferenceText() != null) {
            sb.append("- 系统分析偏好：").append(preference.getPreferenceText()).append("\n");
        }
        
        sb.append("\n导师信息：\n");
        sb.append("- 姓名：").append(mentor.getName()).append("\n");
        sb.append("- 职称：").append(nullSafe(mentor.getTitle())).append("\n");
        sb.append("- 机构：").append(nullSafe(mentor.getInstitution())).append("\n");
        sb.append("- 研究方向：").append(nullSafe(mentor.getResearchAreas())).append("\n");
        sb.append("- 组内方向：").append(nullSafe(mentor.getGroupDirection())).append("\n");
        sb.append("- 指导风格：").append(nullSafe(mentor.getMentoringStyle())).append("\n");
        
        sb.append("\n匹配详情：\n");
        for (Map.Entry<String, Double> entry : details.entrySet()) {
            sb.append("- ").append(entry.getKey()).append("：")
              .append(String.format("%.0f%%", entry.getValue() * 100)).append("\n");
        }
        
        sb.append("\n请直接返回推荐理由，突出匹配亮点。");
        return sb.toString();
    }

    private String buildStudentReasonPrompt(Mentor mentor, Student student, Map<String, Double> details) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为导师推荐学生，生成简短的推荐理由（60字以内）。\n\n");
        sb.append("导师信息：\n");
        sb.append("- 姓名：").append(mentor.getName()).append("\n");
        sb.append("- 研究方向：").append(nullSafe(mentor.getResearchAreas())).append("\n");
        sb.append("- 组内方向：").append(nullSafe(mentor.getGroupDirection())).append("\n");
        sb.append("- 期望学生：").append(nullSafe(mentor.getExpectedStudentQualities())).append("\n");
        
        sb.append("\n学生信息：\n");
        sb.append("- 姓名：").append(student.getName()).append("\n");
        sb.append("- 学历：").append(nullSafe(student.getDegreeLevel())).append("\n");
        sb.append("- 专业：").append(nullSafe(student.getMajor())).append("\n");
        sb.append("- 研究兴趣：").append(nullSafe(student.getResearchInterests())).append("\n");
        sb.append("- 个人能力：").append(nullSafe(student.getPersonalAbilities())).append("\n");
        sb.append("- 编程技能：").append(nullSafe(student.getProgrammingSkills())).append("\n");
        
        if (student.getGpa() != null) {
            sb.append("- GPA：").append(student.getGpa()).append("\n");
        }
        
        sb.append("\n匹配详情：\n");
        for (Map.Entry<String, Double> entry : details.entrySet()) {
            sb.append("- ").append(entry.getKey()).append("：")
              .append(String.format("%.0f%%", entry.getValue() * 100)).append("\n");
        }
        
        sb.append("\n请直接返回推荐理由，突出学生优势。");
        return sb.toString();
    }

    private String nullSafe(String s) {
        return s != null ? s : "未填写";
    }

    private String cleanLLMResponse(String response) {
        if (response == null) return "";
        response = response.trim();
        if (response.startsWith("\"") && response.endsWith("\"")) {
            response = response.substring(1, response.length() - 1);
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getCachedResult(String key) {
        if (redisTemplate == null) return null;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                return (List<Map<String, Object>>) cached;
            }
        } catch (Exception e) {
            log.warn("Cache read failed: {}", key);
        }
        return null;
    }

    private void cacheResult(String key, List<Map<String, Object>> result) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().set(key, result, cacheTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Cache write failed: {}", key);
        }
    }

    /**
     * 清除学生的推荐缓存
     */
    public void invalidateStudentCache(Integer studentId) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(CACHE_PREFIX_MENTOR + studentId);
        } catch (Exception e) {
            log.warn("Cache invalidate failed");
        }
    }

    /**
     * 清除导师的推荐缓存
     */
    public void invalidateMentorCache(Integer mentorId) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(CACHE_PREFIX_STUDENT + mentorId);
        } catch (Exception e) {
            log.warn("Cache invalidate failed");
        }
    }

    /**
     * 基于关键词的简单导师匹配
     */
    private List<Map<String, Object>> getKeywordBasedMentorRecommendations(Student student, UserPreference preference, int limit) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        // 收集学生的关键词
        Set<String> studentKeywords = new HashSet<>();
        if (student.getResearchInterests() != null) {
            studentKeywords.addAll(extractKeywords(student.getResearchInterests()));
        }
        if (student.getExpectedResearchDirection() != null) {
            studentKeywords.addAll(extractKeywords(student.getExpectedResearchDirection()));
        }
        if (student.getKeywords() != null) {
            studentKeywords.addAll(extractKeywords(student.getKeywords()));
        }
        if (student.getProgrammingSkills() != null) {
            studentKeywords.addAll(extractKeywords(student.getProgrammingSkills()));
        }
        if (preference != null && preference.getPreferenceKeywords() != null) {
            studentKeywords.addAll(extractKeywords(preference.getPreferenceKeywords()));
        }
        
        log.info("Student keywords for matching: {}", studentKeywords);
        
        // 获取所有活跃导师
        List<Mentor> allMentors = mentorMapper.getMentorList(0, 100);
        
        for (Mentor mentor : allMentors) {
            if (mentor.getStatus() != 1) continue;
            
            // 收集导师的关键词
            Set<String> mentorKeywords = new HashSet<>();
            if (mentor.getResearchAreas() != null) {
                mentorKeywords.addAll(extractKeywords(mentor.getResearchAreas()));
            }
            if (mentor.getKeywords() != null) {
                mentorKeywords.addAll(extractKeywords(mentor.getKeywords()));
            }
            if (mentor.getGroupDirection() != null) {
                mentorKeywords.addAll(extractKeywords(mentor.getGroupDirection()));
            }
            
            // 计算关键词匹配度
            double matchScore = calculateKeywordMatchScore(studentKeywords, mentorKeywords);
            
            // 添加导师质量加分
            double bonus = calculateMentorBonus(mentor);
            
            // 即使没有关键词匹配，也添加到推荐列表（基于质量分）
            Map<String, Object> recommendation = new HashMap<>();
            recommendation.put("mentor", mentor);
            recommendation.put("score", Math.min(matchScore + bonus + 0.3, 1.0)); // 基础分0.3
            
            Map<String, Double> matchDetails = new HashMap<>();
            matchDetails.put("keyword_match", matchScore);
            matchDetails.put("quality_bonus", bonus);
            recommendation.put("matchDetails", matchDetails);
            
            recommendations.add(recommendation);
        }
        
        // 按得分排序
        recommendations.sort((a, b) -> Double.compare(
            (Double) b.get("score"), (Double) a.get("score")
        ));
        
        log.info("Keyword-based recommendations count: {}", recommendations.size());
        
        return recommendations.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 降级方案：返回热门导师
     */
    private List<Map<String, Object>> getFallbackMentorRecommendations(Integer studentId, int limit) {
        log.info("Using fallback recommendations for student: {}", studentId);
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        try {
            // 获取热门导师（按评分和浏览量排序）
            List<Mentor> mentors = mentorMapper.getMentorList(0, limit * 2);
            
            // 按评分和浏览量排序
            mentors.sort((a, b) -> {
                double scoreA = (a.getRatingAvg() != null ? a.getRatingAvg().doubleValue() : 0) * 0.7 
                              + (a.getViewCount() != null ? Math.min(a.getViewCount() / 100.0, 0.3) : 0);
                double scoreB = (b.getRatingAvg() != null ? b.getRatingAvg().doubleValue() : 0) * 0.7 
                              + (b.getViewCount() != null ? Math.min(b.getViewCount() / 100.0, 0.3) : 0);
                return Double.compare(scoreB, scoreA);
            });
            
            for (Mentor mentor : mentors) {
                if (mentor.getStatus() != 1) continue;
                if (recommendations.size() >= limit) break;
                
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("mentor", mentor);
                recommendation.put("score", 0.5 + calculateMentorBonus(mentor));
                recommendation.put("reason", "热门导师推荐，研究方向：" + nullSafe(mentor.getResearchAreas()));
                
                Map<String, Double> matchDetails = new HashMap<>();
                matchDetails.put("popularity", 0.5);
                matchDetails.put("quality_bonus", calculateMentorBonus(mentor));
                recommendation.put("matchDetails", matchDetails);
                
                recommendations.add(recommendation);
            }
        } catch (Exception e) {
            log.error("Fallback recommendations also failed", e);
        }
        
        return recommendations;
    }

    /**
     * 从文本中提取关键词
     */
    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();
        if (text == null || text.isEmpty()) return keywords;
        
        // 尝试解析JSON数组
        try {
            if (text.startsWith("[")) {
                List<String> list = objectMapper.readValue(text, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                for (String item : list) {
                    keywords.add(item.toLowerCase().trim());
                }
                return keywords;
            }
        } catch (Exception ignored) {}
        
        // 按逗号、顿号、分号分割
        String[] parts = text.split("[,，、;；\\s]+");
        for (String part : parts) {
            String keyword = part.toLowerCase().trim();
            if (keyword.length() > 1) {
                keywords.add(keyword);
            }
        }
        
        return keywords;
    }

    /**
     * 计算关键词匹配得分
     */
    private double calculateKeywordMatchScore(Set<String> studentKeywords, Set<String> mentorKeywords) {
        if (studentKeywords.isEmpty() || mentorKeywords.isEmpty()) {
            return 0.0;
        }
        
        int matchCount = 0;
        for (String sk : studentKeywords) {
            for (String mk : mentorKeywords) {
                // 完全匹配或包含关系
                if (sk.equals(mk) || sk.contains(mk) || mk.contains(sk)) {
                    matchCount++;
                    break;
                }
            }
        }
        
        // 计算Jaccard相似度的变体
        double score = (double) matchCount / Math.max(studentKeywords.size(), 1);
        return Math.min(score, 0.7); // 最高0.7，留空间给其他因素
    }

    // 内部类：搜索条件
    private static class SearchCriteria {
        String dimension;
        String query;
        double weight;

        SearchCriteria(String dimension, String query, double weight) {
            this.dimension = dimension;
            this.query = query;
            this.weight = weight;
        }
    }

    // 内部类：导师匹配结果
    private static class MentorMatchResult {
        int mentorId;
        Map<String, Double> scores = new HashMap<>();

        MentorMatchResult(int mentorId) {
            this.mentorId = mentorId;
        }

        void addScore(String dimension, double score) {
            scores.merge(dimension, score, Double::sum);
        }

        double getTotalScore() {
            return scores.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        Map<String, Double> getDetailScores() {
            return new HashMap<>(scores);
        }
    }

    // 内部类：学生匹配结果
    private static class StudentMatchResult {
        int studentId;
        Map<String, Double> scores = new HashMap<>();

        StudentMatchResult(int studentId) {
            this.studentId = studentId;
        }

        void addScore(String dimension, double score) {
            scores.merge(dimension, score, Double::sum);
        }

        double getTotalScore() {
            return scores.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        Map<String, Double> getDetailScores() {
            return new HashMap<>(scores);
        }
    }
}
