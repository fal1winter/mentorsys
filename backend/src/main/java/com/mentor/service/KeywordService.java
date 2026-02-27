package com.mentor.service;

import com.mentor.entity.Keyword;
import com.mentor.mapper.KeywordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Keyword Service
 * 关键词服务
 */
@Service
public class KeywordService {

    @Autowired
    private KeywordMapper keywordMapper;

    /**
     * Create keyword
     * 创建关键词
     */
    @Transactional
    public Keyword createKeyword(Keyword keyword) {
        // Check if keyword with same name already exists
        Keyword existing = keywordMapper.getKeywordByName(keyword.getName());
        if (existing != null) {
            throw new RuntimeException("Keyword with name '" + keyword.getName() + "' already exists");
        }

        keyword.setCreateTime(new Date());
        keyword.setUpdateTime(new Date());
        keywordMapper.insertKeyword(keyword);
        return keyword;
    }

    /**
     * Update keyword
     * 更新关键词
     */
    @Transactional
    public void updateKeyword(Keyword keyword) {
        // Check if another keyword with same name exists
        Keyword existing = keywordMapper.getKeywordByName(keyword.getName());
        if (existing != null && !existing.getId().equals(keyword.getId())) {
            throw new RuntimeException("Keyword with name '" + keyword.getName() + "' already exists");
        }

        keyword.setUpdateTime(new Date());
        keywordMapper.updateKeyword(keyword);
    }

    /**
     * Delete keyword
     * 删除关键词
     */
    @Transactional
    public void deleteKeyword(Integer id) {
        keywordMapper.deleteKeywordById(id);
    }

    /**
     * Get keyword by ID
     * 根据ID获取关键词
     */
    public Keyword getKeywordById(Integer id) {
        return keywordMapper.getKeywordById(id);
    }

    /**
     * Get keyword by name
     * 根据名称获取关键词
     */
    public Keyword getKeywordByName(String name) {
        return keywordMapper.getKeywordByName(name);
    }

    /**
     * Get keyword list with pagination
     * 分页获取关键词列表
     */
    public List<Keyword> getKeywordList(Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return keywordMapper.getKeywordList(offset, limit);
    }

    /**
     * Search keywords
     * 搜索关键词
     */
    public List<Keyword> searchKeywords(String keyword, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return keywordMapper.searchKeywords(keyword, offset, limit);
    }

    /**
     * Get keywords by category
     * 根据分类获取关键词
     */
    public List<Keyword> getKeywordsByCategory(String category) {
        return keywordMapper.getKeywordsByCategory(category);
    }

    /**
     * Get all keywords (no pagination)
     * 获取所有关键词（不分页）
     */
    public List<Keyword> getAllKeywords() {
        return keywordMapper.getAllKeywords();
    }

    /**
     * Count total keywords
     * 统计关键词总数
     */
    public int countKeywords() {
        return keywordMapper.countKeywords();
    }

    /**
     * Count search results
     * 统计搜索结果数
     */
    public int countSearchResults(String keyword) {
        return keywordMapper.countSearchResults(keyword);
    }
}
