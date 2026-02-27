package com.mentor.mapper;

import com.mentor.entity.Publication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Publication Mapper
 * 学者作品/论文数据访问接口
 */
@Mapper
public interface PublicationMapper {

    /**
     * Insert publication
     */
    void insertPublication(Publication publication);

    /**
     * Update publication
     */
    void updatePublication(Publication publication);

    /**
     * Delete publication by ID
     */
    void deletePublicationById(@Param("id") Integer id);

    /**
     * Get publication by ID
     */
    Publication getPublicationById(@Param("id") Integer id);

    /**
     * Get publications by mentor ID
     */
    List<Publication> getPublicationsByMentorId(@Param("mentorId") Integer mentorId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Search publications by keyword
     */
    List<Publication> searchPublications(@Param("keyword") String keyword, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Get publications by year
     */
    List<Publication> getPublicationsByYear(@Param("year") Integer year, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Count publications by mentor
     */
    int countPublicationsByMentor(@Param("mentorId") Integer mentorId);

    /**
     * Count search results
     */
    int countSearchResults(@Param("keyword") String keyword);

    /**
     * Get all publications
     */
    List<Publication> getAllPublications(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Count all publications
     */
    int countAllPublications();
}
