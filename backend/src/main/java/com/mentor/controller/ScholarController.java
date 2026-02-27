package com.mentor.controller;

import com.mentor.entity.Scholar;
import com.mentor.service.ScholarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scholar Controller
 * 学者管理控制器
 */
@RestController
@RequestMapping("/scholars")
public class ScholarController {

    @Autowired
    private ScholarService scholarService;

    /**
     * Create Scholar
     * 创建学者
     */
    @PostMapping
    public Map<String, Object> createScholar(@RequestBody Scholar scholar) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (scholar.getName() == null || scholar.getName().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "Scholar name is required");
                return result;
            }

            Scholar createdScholar = scholarService.createScholar(scholar);

            result.put("code", 0);
            result.put("message", "Scholar created successfully");
            result.put("data", createdScholar);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to create scholar: " + e.getMessage());
        }

        return result;
    }

    /**
     * Update Scholar
     * 更新学者信息
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateScholar(@PathVariable Integer id, @RequestBody Scholar scholar) {
        Map<String, Object> result = new HashMap<>();

        try {
            Scholar existingScholar = scholarService.getScholarById(id);
            if (existingScholar == null) {
                result.put("code", 404);
                result.put("message", "Scholar not found");
                return result;
            }

            scholar.setId(id);
            scholarService.updateScholar(scholar);

            result.put("code", 0);
            result.put("message", "Scholar updated successfully");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to update scholar: " + e.getMessage());
        }

        return result;
    }

    /**
     * Delete Scholar
     * 删除学者
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteScholar(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Scholar existingScholar = scholarService.getScholarById(id);
            if (existingScholar == null) {
                result.put("code", 404);
                result.put("message", "Scholar not found");
                return result;
            }

            scholarService.deleteScholar(id);

            result.put("code", 0);
            result.put("message", "Scholar deleted successfully");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to delete scholar: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Scholar by ID
     * 根据ID获取学者
     */
    @GetMapping("/{id}")
    public Map<String, Object> getScholarById(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Scholar scholar = scholarService.getScholarById(id);

            if (scholar == null) {
                result.put("code", 404);
                result.put("message", "Scholar not found");
                return result;
            }

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", scholar);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get scholar: " + e.getMessage());
        }

        return result;
    }

    /**
     * Search Scholars
     * 搜索学者 (支持空关键词，返回所有学者)
     */
    @GetMapping("/search")
    public Map<String, Object> searchScholars(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Scholar> scholars;
            int total;

            // If keyword is empty or null, return all scholars
            if (keyword == null || keyword.trim().isEmpty()) {
                scholars = scholarService.getAllScholars(page, limit);
                total = scholarService.countScholars();
            } else {
                scholars = scholarService.searchScholars(keyword, page, limit);
                total = scholarService.countSearchResults(keyword);
            }

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", scholars);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to search scholars: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Scholars by Publication ID
     * 根据论文ID获取学者列表
     */
    @GetMapping("/publication/{publicationId}")
    public Map<String, Object> getScholarsByPublicationId(@PathVariable Integer publicationId) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Scholar> scholars = scholarService.getScholarsByPublicationId(publicationId);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", scholars);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get scholars: " + e.getMessage());
        }

        return result;
    }
}
