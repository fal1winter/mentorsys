package com.mentor.mapper;

import com.mentor.entity.Rating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Rating Mapper
 * 评分数据访问接口
 */
@Mapper
public interface RatingMapper {

    /**
     * Insert rating
     */
    void insertRating(Rating rating);

    /**
     * Update rating
     */
    void updateRating(Rating rating);

    /**
     * Delete rating by ID
     */
    void deleteRatingById(@Param("id") Integer id);

    /**
     * Get rating by ID
     */
    Rating getRatingById(@Param("id") Integer id);

    /**
     * Get rating by student and mentor
     */
    Rating getRatingByStudentAndMentor(@Param("studentId") Integer studentId, @Param("mentorId") Integer mentorId);

    /**
     * Get ratings by mentor ID
     */
    List<Rating> getRatingsByMentorId(@Param("mentorId") Integer mentorId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Get ratings by student ID
     */
    List<Rating> getRatingsByStudentId(@Param("studentId") Integer studentId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Increment helpful count
     */
    void incrementHelpfulCount(@Param("id") Integer id);

    /**
     * Count ratings by mentor
     */
    int countRatingsByMentor(@Param("mentorId") Integer mentorId);

    /**
     * Get average rating by mentor
     */
    Double getAverageRatingByMentor(@Param("mentorId") Integer mentorId);
}
