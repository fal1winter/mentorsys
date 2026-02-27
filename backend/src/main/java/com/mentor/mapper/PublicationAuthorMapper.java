package com.mentor.mapper;

import com.mentor.entity.PublicationAuthor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Publication Author Mapper
 * 论文-作者关系数据访问接口
 */
@Mapper
public interface PublicationAuthorMapper {

    /**
     * Insert publication author relationship
     */
    void insertPublicationAuthor(PublicationAuthor publicationAuthor);

    /**
     * Delete publication author relationship by ID
     */
    void deletePublicationAuthorById(@Param("id") Integer id);

    /**
     * Delete all authors for a publication
     */
    void deleteAuthorsByPublicationId(@Param("publicationId") Integer publicationId);

    /**
     * Delete a specific author from a publication
     */
    void deleteAuthorFromPublication(@Param("publicationId") Integer publicationId, @Param("scholarId") Integer scholarId);

    /**
     * Get publication author relationship by ID
     */
    PublicationAuthor getPublicationAuthorById(@Param("id") Integer id);

    /**
     * Get all authors for a publication
     */
    List<PublicationAuthor> getAuthorsByPublicationId(@Param("publicationId") Integer publicationId);

    /**
     * Get all publications for a scholar
     */
    List<PublicationAuthor> getPublicationsByScholarId(@Param("scholarId") Integer scholarId);

    /**
     * Check if a scholar is already an author of a publication
     */
    PublicationAuthor getPublicationAuthor(@Param("publicationId") Integer publicationId, @Param("scholarId") Integer scholarId);

    /**
     * Update publication author relationship
     */
    void updatePublicationAuthor(PublicationAuthor publicationAuthor);
}
