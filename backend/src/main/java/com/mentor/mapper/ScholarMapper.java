package com.mentor.mapper;

import com.mentor.entity.Scholar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Scholar Mapper
 * 学者数据访问接口
 */
@Mapper
public interface ScholarMapper {

    /**
     * Insert scholar
     */
    void insertScholar(Scholar scholar);

    /**
     * Update scholar
     */
    void updateScholar(Scholar scholar);

    /**
     * Delete scholar by ID
     */
    void deleteScholarById(@Param("id") Integer id);

    /**
     * Get scholar by ID
     */
    Scholar getScholarById(@Param("id") Integer id);

    /**
     * Get all scholars with pagination
     */
    List<Scholar> getAllScholars(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Search scholars by keyword
     */
    List<Scholar> searchScholars(@Param("keyword") String keyword, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Count all scholars
     */
    int countScholars();

    /**
     * Count search results
     */
    int countSearchResults(@Param("keyword") String keyword);

    /**
     * Get scholars by publication ID
     */
    List<Scholar> getScholarsByPublicationId(@Param("publicationId") Integer publicationId);
}
