package com.mentor.service;

import com.mentor.entity.Mentor;
import com.mentor.entity.Rating;
import com.mentor.entity.Student;
import com.mentor.mapper.RatingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Recommendation Scorer for Multi-dimensional Scoring
 * 多维度评分服务
 */
@Slf4j
@Service
public class RecommendationScorer {

    @Autowired
    private RatingMapper ratingMapper;

    // Student finding mentor weights
    @Value("${recommendation.weights.student.research-match:0.40}")
    private Double studentResearchMatchWeight;

    @Value("${recommendation.weights.student.quality-score:0.25}")
    private Double studentQualityScoreWeight;

    @Value("${recommendation.weights.student.availability:0.20}")
    private Double studentAvailabilityWeight;

    @Value("${recommendation.weights.student.workload:0.15}")
    private Double studentWorkloadWeight;

    // Mentor finding student weights
    @Value("${recommendation.weights.mentor.research-match:0.35}")
    private Double mentorResearchMatchWeight;

    @Value("${recommendation.weights.mentor.academic-ability:0.30}")
    private Double mentorAcademicAbilityWeight;

    @Value("${recommendation.weights.mentor.background-match:0.20}")
    private Double mentorBackgroundMatchWeight;

    @Value("${recommendation.weights.mentor.time-match:0.15}")
    private Double mentorTimeMatchWeight;

    /**
     * Calculate comprehensive score for student finding mentor
     * 计算学生找导师的综合得分
     */
    public Map<String, Object> scoreStudentToMentor(Student student, Mentor mentor, double vectorSimilarity) {
        Map<String, Object> result = new HashMap<>();

        // 1. Research match score (based on vector similarity)
        double researchMatchScore = vectorSimilarity;

        // 2. Quality score (based on ratings and verification)
        double qualityScore = calculateMentorQualityScore(mentor);

        // 3. Availability score (based on accepting students and capacity)
        double availabilityScore = calculateAvailabilityScore(mentor);

        // 4. Workload score (based on ratings analysis)
        double workloadScore = calculateWorkloadScore(mentor);

        // Calculate weighted total score
        double totalScore =
            researchMatchScore * studentResearchMatchWeight +
            qualityScore * studentQualityScoreWeight +
            availabilityScore * studentAvailabilityWeight +
            workloadScore * studentWorkloadWeight;

        // Store individual scores
        Map<String, Double> details = new HashMap<>();
        details.put("researchMatch", researchMatchScore);
        details.put("qualityScore", qualityScore);
        details.put("availabilityScore", availabilityScore);
        details.put("workloadScore", workloadScore);

        result.put("totalScore", totalScore);
        result.put("details", details);

        log.debug("Student {} to Mentor {} score: {}", student.getId(), mentor.getId(), totalScore);

        return result;
    }

    /**
     * Calculate comprehensive score for mentor finding student
     * 计算导师找学生的综合得分
     */
    public Map<String, Object> scoreMentorToStudent(Mentor mentor, Student student, double vectorSimilarity) {
        Map<String, Object> result = new HashMap<>();

        // 1. Research match score (based on vector similarity)
        double researchMatchScore = vectorSimilarity;

        // 2. Academic ability score (based on GPA and degree level)
        double academicAbilityScore = calculateAcademicAbilityScore(student);

        // 3. Background match score (based on major and institution)
        double backgroundMatchScore = calculateBackgroundMatchScore(mentor, student);

        // 4. Time match score (based on graduation year)
        double timeMatchScore = calculateTimeMatchScore(student);

        // Calculate weighted total score
        double totalScore =
            researchMatchScore * mentorResearchMatchWeight +
            academicAbilityScore * mentorAcademicAbilityWeight +
            backgroundMatchScore * mentorBackgroundMatchWeight +
            timeMatchScore * mentorTimeMatchWeight;

        // Store individual scores
        Map<String, Double> details = new HashMap<>();
        details.put("researchMatch", researchMatchScore);
        details.put("academicAbility", academicAbilityScore);
        details.put("backgroundMatch", backgroundMatchScore);
        details.put("timeMatch", timeMatchScore);

        result.put("totalScore", totalScore);
        result.put("details", details);

        log.debug("Mentor {} to Student {} score: {}", mentor.getId(), student.getId(), totalScore);

        return result;
    }

    /**
     * Calculate mentor quality score
     * 计算导师质量得分
     */
    private double calculateMentorQualityScore(Mentor mentor) {
        double score = 0.0;

        // Rating average (0-5 scale, normalize to 0-1)
        if (mentor.getRatingAvg() != null && mentor.getRatingAvg().doubleValue() > 0) {
            score += (mentor.getRatingAvg().doubleValue() / 5.0) * 0.6;
        }

        // Rating count (more ratings = more reliable)
        if (mentor.getRatingCount() != null && mentor.getRatingCount() > 0) {
            // Logarithmic scale: 1-10 ratings = 0.2, 10-100 = 0.3, 100+ = 0.4
            double ratingCountScore = Math.min(0.4, Math.log10(mentor.getRatingCount() + 1) / 2.0 * 0.4);
            score += ratingCountScore;
        }

        // Verification status
        if (mentor.getIsVerified() != null && mentor.getIsVerified()) {
            score += 0.2;
        }

        return Math.min(1.0, score);
    }

    /**
     * Calculate availability score
     * 计算接收能力得分
     */
    private double calculateAvailabilityScore(Mentor mentor) {
        // Not accepting students = 0 score
        if (mentor.getAcceptingStudents() == null || !mentor.getAcceptingStudents()) {
            return 0.0;
        }

        // Calculate remaining capacity ratio
        Integer currentStudents = mentor.getCurrentStudents() != null ? mentor.getCurrentStudents() : 0;
        Integer maxStudents = mentor.getMaxStudents() != null ? mentor.getMaxStudents() : 10;

        if (maxStudents <= 0) {
            return 0.5; // Default score if max is not set properly
        }

        double capacityRatio = (double) (maxStudents - currentStudents) / maxStudents;

        // Normalize: 100% capacity = 1.0, 0% capacity = 0.0
        return Math.max(0.0, Math.min(1.0, capacityRatio));
    }

    /**
     * Calculate workload score based on ratings
     * 计算工作强度得分（基于评价分析）
     */
    private double calculateWorkloadScore(Mentor mentor) {
        try {
            // Get recent ratings
            List<Rating> ratings = ratingMapper.getRatingsByMentorId(mentor.getId(), 0, 20);

            if (ratings == null || ratings.isEmpty()) {
                return 0.7; // Default neutral score if no ratings
            }

            int totalRatings = ratings.size();
            int negativeWorkloadCount = 0;

            // Keywords indicating high workload
            String[] highWorkloadKeywords = {
                "压力大", "工作量大", "很忙", "加班", "任务多",
                "要求高", "严格", "辛苦", "累"
            };

            for (Rating rating : ratings) {
                String comment = rating.getComment();
                if (comment != null) {
                    for (String keyword : highWorkloadKeywords) {
                        if (comment.contains(keyword)) {
                            negativeWorkloadCount++;
                            break;
                        }
                    }
                }
            }

            // Calculate score: fewer negative mentions = higher score
            double negativeRatio = (double) negativeWorkloadCount / totalRatings;
            return 1.0 - negativeRatio;

        } catch (Exception e) {
            log.warn("Failed to calculate workload score for mentor {}", mentor.getId(), e);
            return 0.7; // Default neutral score on error
        }
    }

    /**
     * Calculate academic ability score
     * 计算学术能力得分
     */
    private double calculateAcademicAbilityScore(Student student) {
        double score = 0.0;

        // GPA score (0-4.0 scale, normalize to 0-1)
        if (student.getGpa() != null && student.getGpa().doubleValue() > 0) {
            score += Math.min(1.0, student.getGpa().doubleValue() / 4.0) * 0.6;
        }

        // Degree level score
        String degreeLevel = student.getDegreeLevel();
        if (degreeLevel != null) {
            if (degreeLevel.contains("PhD") || degreeLevel.contains("博士")) {
                score += 0.4;
            } else if (degreeLevel.contains("Master") || degreeLevel.contains("硕士")) {
                score += 0.3;
            } else if (degreeLevel.contains("Bachelor") || degreeLevel.contains("本科")) {
                score += 0.2;
            }
        }

        return Math.min(1.0, score);
    }

    /**
     * Calculate background match score
     * 计算背景匹配得分
     */
    private double calculateBackgroundMatchScore(Mentor mentor, Student student) {
        double score = 0.0;

        // Major relevance (simple keyword matching)
        String studentMajor = student.getMajor();
        String mentorResearchAreas = mentor.getResearchAreas();

        if (studentMajor != null && mentorResearchAreas != null) {
            // Check if major keywords appear in research areas
            String[] majorKeywords = studentMajor.split("[,，、\\s]+");
            int matchCount = 0;

            for (String keyword : majorKeywords) {
                if (keyword.length() > 1 && mentorResearchAreas.contains(keyword)) {
                    matchCount++;
                }
            }

            if (matchCount > 0) {
                score += Math.min(0.6, matchCount * 0.2);
            }
        }

        // Institution prestige (simplified - can be enhanced with institution rankings)
        String studentInstitution = student.getCurrentInstitution();
        if (studentInstitution != null) {
            // Top universities get bonus points
            String[] topUniversities = {
                "清华", "北大", "复旦", "上海交通", "浙江大学",
                "MIT", "Stanford", "Harvard", "Cambridge", "Oxford"
            };

            for (String topUni : topUniversities) {
                if (studentInstitution.contains(topUni)) {
                    score += 0.4;
                    break;
                }
            }
        }

        return Math.min(1.0, score);
    }

    /**
     * Calculate time match score
     * 计算时间匹配得分
     */
    private double calculateTimeMatchScore(Student student) {
        Integer graduationYear = student.getGraduationYear();

        if (graduationYear == null) {
            return 0.5; // Default neutral score
        }

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int yearsUntilGraduation = graduationYear - currentYear;

        // Ideal: 0-2 years until graduation
        if (yearsUntilGraduation >= 0 && yearsUntilGraduation <= 2) {
            return 1.0;
        } else if (yearsUntilGraduation < 0) {
            // Already graduated - less ideal
            return Math.max(0.0, 1.0 - Math.abs(yearsUntilGraduation) * 0.2);
        } else {
            // Too far in the future
            return Math.max(0.0, 1.0 - (yearsUntilGraduation - 2) * 0.15);
        }
    }
}
