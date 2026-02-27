package com.mentor.service;

import com.mentor.entity.Mentor;
import com.mentor.entity.Rating;
import com.mentor.mapper.MentorMapper;
import com.mentor.mapper.RatingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * Rating Service
 * 评分服务
 */
@Service
public class RatingService {

    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    private MentorMapper mentorMapper;

    /**
     * Create rating
     * 创建评分
     */
    @Transactional
    public Rating createRating(Rating rating) {
        // Check if student has already rated this mentor
        Rating existingRating = ratingMapper.getRatingByStudentAndMentor(
                rating.getStudentId(), rating.getMentorId());

        if (existingRating != null) {
            throw new RuntimeException("您已经评价过该导师");
        }

        // Validate rating value
        if (rating.getRating() < 0.1 || rating.getRating() > 10.0) {
            throw new RuntimeException("评分必须在0.1-10.0之间");
        }

        rating.setCreateTime(new Date());
        rating.setUpdateTime(new Date());
        if (rating.getHelpfulCount() == null) {
            rating.setHelpfulCount(0);
        }

        ratingMapper.insertRating(rating);

        // Update mentor's average rating
        updateMentorRating(rating.getMentorId());

        return rating;
    }

    /**
     * Update rating
     * 更新评分
     */
    @Transactional
    public void updateRating(Rating rating) {
        // Validate rating value
        if (rating.getRating() != null && (rating.getRating() < 0.1 || rating.getRating() > 10.0)) {
            throw new RuntimeException("评分必须在0.1-10.0之间");
        }

        rating.setUpdateTime(new Date());
        ratingMapper.updateRating(rating);

        // Update mentor's average rating
        Rating existingRating = ratingMapper.getRatingById(rating.getId());
        if (existingRating != null) {
            updateMentorRating(existingRating.getMentorId());
        }
    }

    /**
     * Delete rating
     * 删除评分
     */
    @Transactional
    public void deleteRating(Integer id) {
        Rating rating = ratingMapper.getRatingById(id);
        if (rating == null) {
            throw new RuntimeException("评分不存在");
        }

        ratingMapper.deleteRatingById(id);

        // Update mentor's average rating
        updateMentorRating(rating.getMentorId());
    }

    /**
     * Get rating by ID
     * 根据ID获取评分
     */
    public Rating getRatingById(Integer id) {
        return ratingMapper.getRatingById(id);
    }

    /**
     * Get ratings by mentor ID
     * 根据导师ID获取评分列表
     */
    public List<Rating> getRatingsByMentorId(Integer mentorId, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return ratingMapper.getRatingsByMentorId(mentorId, offset, limit);
    }

    /**
     * Get ratings by student ID
     * 根据学生ID获取评分列表
     */
    public List<Rating> getRatingsByStudentId(Integer studentId, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return ratingMapper.getRatingsByStudentId(studentId, offset, limit);
    }

    /**
     * Mark rating as helpful
     * 标记评分有用
     */
    @Transactional
    public void markRatingHelpful(Integer id) {
        ratingMapper.incrementHelpfulCount(id);
    }

    /**
     * Count ratings by mentor
     * 统计导师的评分数
     */
    public int countRatingsByMentor(Integer mentorId) {
        return ratingMapper.countRatingsByMentor(mentorId);
    }

    /**
     * Get average rating by mentor
     * 获取导师的平均评分
     */
    public Double getAverageRatingByMentor(Integer mentorId) {
        return ratingMapper.getAverageRatingByMentor(mentorId);
    }

    /**
     * Update mentor's rating statistics
     * 更新导师的评分统计
     */
    @Transactional
    public void updateMentorRating(Integer mentorId) {
        int count = ratingMapper.countRatingsByMentor(mentorId);
        Double average = ratingMapper.getAverageRatingByMentor(mentorId);

        if (average == null) {
            average = 0.0;
        }

        // Round to 2 decimal places
        BigDecimal avgDecimal = new BigDecimal(average).setScale(2, RoundingMode.HALF_UP);

        mentorMapper.updateMentorRating(mentorId, avgDecimal.doubleValue(), count);
    }
}
