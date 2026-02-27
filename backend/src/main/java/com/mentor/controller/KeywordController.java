package com.mentor.controller;

import com.mentor.entity.Keyword;
import com.mentor.service.KeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keyword Controller
 * 关键词控制器
 */
@RestController
@RequestMapping("/keywords")
public class KeywordController {

    @Autowired
    private KeywordService keywordService;

    /**
     * Create Keyword
     * 创建关键词
     */
    @PostMapping
    public Map<String, Object> createKeyword(@RequestBody Keyword keyword) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Validate required fields
            if (keyword.getName() == null || keyword.getName().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "Keyword name is required");
                return result;
            }

            Keyword createdKeyword = keywordService.createKeyword(keyword);

            result.put("code", 0);
            result.put("message", "Keyword created successfully");
            result.put("data", createdKeyword);

        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to create keyword: " + e.getMessage());
        }

        return result;
    }

    /**
     * Update Keyword
     * 更新关键词
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateKeyword(@PathVariable Integer id, @RequestBody Keyword keyword) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Check if keyword exists
            Keyword existingKeyword = keywordService.getKeywordById(id);
            if (existingKeyword == null) {
                result.put("code", 404);
                result.put("message", "Keyword not found");
                return result;
            }

            // Validate required fields
            if (keyword.getName() == null || keyword.getName().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "Keyword name is required");
                return result;
            }

            keyword.setId(id);
            keywordService.updateKeyword(keyword);

            result.put("code", 0);
            result.put("message", "Keyword updated successfully");

        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to update keyword: " + e.getMessage());
        }

        return result;
    }

    /**
     * Delete Keyword
     * 删除关键词
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteKeyword(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Check if keyword exists
            Keyword existingKeyword = keywordService.getKeywordById(id);
            if (existingKeyword == null) {
                result.put("code", 404);
                result.put("message", "Keyword not found");
                return result;
            }

            keywordService.deleteKeyword(id);

            result.put("code", 0);
            result.put("message", "Keyword deleted successfully");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to delete keyword: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Keyword by ID
     * 根据ID获取关键词
     */
    @GetMapping("/{id}")
    public Map<String, Object> getKeywordById(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Keyword keyword = keywordService.getKeywordById(id);

            if (keyword == null) {
                result.put("code", 404);
                result.put("message", "Keyword not found");
                return result;
            }

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", keyword);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get keyword: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Keyword List
     * 获取关键词列表
     */
    @GetMapping
    public Map<String, Object> getKeywordList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Keyword> keywords = keywordService.getKeywordList(page, limit);
            int total = keywordService.countKeywords();

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", keywords);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get keyword list: " + e.getMessage());
        }

        return result;
    }

    /**
     * Search Keywords
     * 搜索关键词
     */
    @GetMapping("/search")
    public Map<String, Object> searchKeywords(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "Search keyword is required");
                return result;
            }

            List<Keyword> keywords = keywordService.searchKeywords(keyword, page, limit);
            int total = keywordService.countSearchResults(keyword);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", keywords);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to search keywords: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get All Keywords (no pagination)
     * 获取所有关键词（不分页，用于下拉选择）
     */
    @GetMapping("/all")
    public Map<String, Object> getAllKeywords() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Keyword> keywords = keywordService.getAllKeywords();

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", keywords);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get all keywords: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Keywords by Category
     * 根据分类获取关键词
     */
    @GetMapping("/category/{category}")
    public Map<String, Object> getKeywordsByCategory(@PathVariable String category) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Keyword> keywords = keywordService.getKeywordsByCategory(category);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", keywords);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get keywords by category: " + e.getMessage());
        }

        return result;
    }
}
