package com.mentor.service;

import com.mentor.entity.Publication;
import com.mentor.entity.PublicationAuthor;
import com.mentor.mapper.PublicationMapper;
import com.mentor.mapper.PublicationAuthorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Publication Service
 * 学者作品/论文服务
 */
@Service
public class PublicationService {

    @Autowired
    private PublicationMapper publicationMapper;

    @Autowired
    private PublicationAuthorMapper publicationAuthorMapper;

    /**
     * Create publication
     * 创建论文
     */
    @Transactional
    public Publication createPublication(Publication publication) {
        publication.setCreateTime(new Date());
        publication.setUpdateTime(new Date());
        if (publication.getCitationCount() == null) {
            publication.setCitationCount(0);
        }

        publicationMapper.insertPublication(publication);
        return publication;
    }

    /**
     * Update publication
     * 更新论文信息
     */
    @Transactional
    public void updatePublication(Publication publication) {
        publication.setUpdateTime(new Date());
        publicationMapper.updatePublication(publication);
    }

    /**
     * Delete publication
     * 删除论文
     */
    @Transactional
    public void deletePublication(Integer id) {
        publicationMapper.deletePublicationById(id);
    }

    /**
     * Get publication by ID
     * 根据ID获取论文
     */
    public Publication getPublicationById(Integer id) {
        return publicationMapper.getPublicationById(id);
    }

    /**
     * Get publications by mentor ID
     * 根据导师ID获取论文列表
     */
    public List<Publication> getPublicationsByMentorId(Integer mentorId, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return publicationMapper.getPublicationsByMentorId(mentorId, offset, limit);
    }

    /**
     * Search publications
     * 搜索论文
     */
    public List<Publication> searchPublications(String keyword, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return publicationMapper.searchPublications(keyword, offset, limit);
    }

    /**
     * Get publications by year
     * 根据年份获取论文
     */
    public List<Publication> getPublicationsByYear(Integer year, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return publicationMapper.getPublicationsByYear(year, offset, limit);
    }

    /**
     * Count publications by mentor
     * 统计导师的论文数
     */
    public int countPublicationsByMentor(Integer mentorId) {
        return publicationMapper.countPublicationsByMentor(mentorId);
    }

    /**
     * Count search results
     * 统计搜索结果数
     */
    public int countSearchResults(String keyword) {
        return publicationMapper.countSearchResults(keyword);
    }

    /**
     * Get all publications
     * 获取所有论文
     */
    public List<Publication> getAllPublications(Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return publicationMapper.getAllPublications(offset, limit);
    }

    /**
     * Count all publications
     * 统计所有论文数
     */
    public int countAllPublications() {
        return publicationMapper.countAllPublications();
    }

    /**
     * Add author to publication
     * 为论文添加作者
     */
    @Transactional
    public PublicationAuthor addAuthorToPublication(Integer publicationId, Integer scholarId, Integer authorOrder, Boolean isCorresponding) {
        // Check if relationship already exists
        PublicationAuthor existing = publicationAuthorMapper.getPublicationAuthor(publicationId, scholarId);
        if (existing != null) {
            throw new RuntimeException("This scholar is already an author of this publication");
        }

        PublicationAuthor publicationAuthor = new PublicationAuthor();
        publicationAuthor.setPublicationId(publicationId);
        publicationAuthor.setScholarId(scholarId);
        publicationAuthor.setAuthorOrder(authorOrder != null ? authorOrder : 1);
        publicationAuthor.setIsCorresponding(isCorresponding != null ? isCorresponding : false);

        publicationAuthorMapper.insertPublicationAuthor(publicationAuthor);
        return publicationAuthor;
    }

    /**
     * Remove author from publication
     * 从论文中移除作者
     */
    @Transactional
    public void removeAuthorFromPublication(Integer publicationId, Integer scholarId) {
        publicationAuthorMapper.deleteAuthorFromPublication(publicationId, scholarId);
    }

    /**
     * Update publication author relationship
     * 更新论文作者关系
     */
    @Transactional
    public void updatePublicationAuthor(Integer id, Integer authorOrder, Boolean isCorresponding) {
        PublicationAuthor publicationAuthor = new PublicationAuthor();
        publicationAuthor.setId(id);
        publicationAuthor.setAuthorOrder(authorOrder);
        publicationAuthor.setIsCorresponding(isCorresponding);
        publicationAuthorMapper.updatePublicationAuthor(publicationAuthor);
    }

    /**
     * Get publication author by ID
     * 根据ID获取论文作者关系
     */
    public PublicationAuthor getPublicationAuthorById(Integer id) {
        return publicationAuthorMapper.getPublicationAuthorById(id);
    }

    /**
     * Get authors for a publication
     * 获取论文的作者列表
     */
    public List<PublicationAuthor> getAuthorsByPublicationId(Integer publicationId) {
        return publicationAuthorMapper.getAuthorsByPublicationId(publicationId);
    }

    /**
     * Get publications for a scholar
     * 获取学者的论文列表
     */
    public List<PublicationAuthor> getPublicationsByScholarId(Integer scholarId) {
        return publicationAuthorMapper.getPublicationsByScholarId(scholarId);
    }

    /**
     * Delete all authors for a publication
     * 删除论文的所有作者
     */
    @Transactional
    public void deleteAuthorsByPublicationId(Integer publicationId) {
        publicationAuthorMapper.deleteAuthorsByPublicationId(publicationId);
    }
}
