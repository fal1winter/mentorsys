package com.mentor.service;

import com.mentor.entity.Scholar;
import com.mentor.mapper.ScholarMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scholar Service
 * 学者管理服务
 */
@Service
public class ScholarService {

    @Autowired
    private ScholarMapper scholarMapper;

    /**
     * Create scholar
     * 创建学者
     */
    @Transactional
    public Scholar createScholar(Scholar scholar) {
        // Set default values
        if (scholar.getHIndex() == null) {
            scholar.setHIndex(0);
        }
        if (scholar.getCitationCount() == null) {
            scholar.setCitationCount(0);
        }

        scholarMapper.insertScholar(scholar);
        return scholar;
    }

    /**
     * Update scholar
     * 更新学者信息
     */
    @Transactional
    public void updateScholar(Scholar scholar) {
        scholarMapper.updateScholar(scholar);
    }

    /**
     * Delete scholar
     * 删除学者
     */
    @Transactional
    public void deleteScholar(Integer id) {
        scholarMapper.deleteScholarById(id);
    }

    /**
     * Get scholar by ID
     * 根据ID获取学者
     */
    public Scholar getScholarById(Integer id) {
        return scholarMapper.getScholarById(id);
    }

    /**
     * Get all scholars with pagination
     * 获取所有学者（分页）
     */
    public List<Scholar> getAllScholars(Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return scholarMapper.getAllScholars(offset, limit);
    }

    /**
     * Search scholars by keyword
     * 搜索学者
     */
    public List<Scholar> searchScholars(String keyword, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return scholarMapper.searchScholars(keyword, offset, limit);
    }

    /**
     * Count all scholars
     * 统计所有学者数
     */
    public int countScholars() {
        return scholarMapper.countScholars();
    }

    /**
     * Count search results
     * 统计搜索结果数
     */
    public int countSearchResults(String keyword) {
        return scholarMapper.countSearchResults(keyword);
    }

    /**
     * Get scholars by publication ID
     * 根据论文ID获取学者列表
     */
    public List<Scholar> getScholarsByPublicationId(Integer publicationId) {
        return scholarMapper.getScholarsByPublicationId(publicationId);
    }
}
