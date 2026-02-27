package com.mentor.mapper;

import com.mentor.entity.Keyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Keyword Mapper
 * 关键词数据访问接口
 */
@Mapper
public interface KeywordMapper {

    /**
     * Insert keyword
     */
    void insertKeyword(Keyword keyword);

    /**
     * Update keyword
     */
    void updateKeyword(Keyword keyword);

    /**
     * Delete keyword by ID
     */
    void deleteKeywordById(@Param("id") Integer id);

    /**
     * Get keyword by ID
     */
    Keyword getKeywordById(@Param("id") Integer id);

    /**
     * Get keyword by name
     */
    Keyword getKeywordByName(@Param("name") String name);

    /**
     * Get all keywords with pagination
     */
    List<Keyword> getKeywordList(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Search keywords by keyword
     */
    List<Keyword> searchKeywords(@Param("keyword") String keyword, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Get keywords by category
     */
    List<Keyword> getKeywordsByCategory(@Param("category") String category);

    /**
     * Count total keywords
     */
    int countKeywords();

    /**
     * Count search results
     */
    int countSearchResults(@Param("keyword") String keyword);

    /**
     * Get all keywords (no pagination)
     */
    List<Keyword> getAllKeywords();
}
