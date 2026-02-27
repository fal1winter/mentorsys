package com.mentor.mapper;

import com.mentor.entity.Mentor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mentor Mapper
 * 导师数据访问接口
 */
@Mapper
public interface MentorMapper {

    /**
     * Insert mentor
     */
    void insertMentor(Mentor mentor);

    /**
     * Update mentor
     */
    void updateMentor(Mentor mentor);

    /**
     * Delete mentor by ID
     */
    void deleteMentorById(@Param("id") Integer id);

    /**
     * Get mentor by ID
     */
    Mentor getMentorById(@Param("id") Integer id);

    /**
     * Get mentor by user ID
     */
    Mentor getMentorByUserId(@Param("userId") Integer userId);

    /**
     * Get all mentors with pagination
     */
    List<Mentor> getMentorList(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Search mentors by keyword
     */
    List<Mentor> searchMentors(@Param("keyword") String keyword, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Get mentors by institution
     */
    List<Mentor> getMentorsByInstitution(@Param("institution") String institution, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Get mentors by research area
     */
    List<Mentor> getMentorsByResearchArea(@Param("researchArea") String researchArea, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Count total mentors
     */
    int countMentors();

    /**
     * Count search results
     */
    int countSearchResults(@Param("keyword") String keyword);

    /**
     * Update mentor view count
     */
    void incrementViewCount(@Param("id") Integer id);

    /**
     * Update mentor rating
     */
    void updateMentorRating(@Param("id") Integer id, @Param("ratingAvg") Double ratingAvg, @Param("ratingCount") Integer ratingCount);

    /**
     * Update mentor student count
     */
    void updateStudentCount(@Param("id") Integer id, @Param("currentStudents") Integer currentStudents);

    /**
     * Get mentors with filters
     */
    List<Mentor> getMentorListWithFilters(
            @Param("keyword") String keyword,
            @Param("institution") String institution,
            @Param("researchArea") String researchArea,
            @Param("title") String title,
            @Param("department") String department,
            @Param("acceptingStudents") Boolean acceptingStudents,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit);

    /**
     * Count mentors with filters
     */
    int countMentorsWithFilters(
            @Param("keyword") String keyword,
            @Param("institution") String institution,
            @Param("researchArea") String researchArea,
            @Param("title") String title,
            @Param("department") String department,
            @Param("acceptingStudents") Boolean acceptingStudents);
}
