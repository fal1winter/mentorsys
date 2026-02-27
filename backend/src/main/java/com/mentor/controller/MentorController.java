package com.mentor.controller;

import com.mentor.entity.Mentor;
import com.mentor.service.MentorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mentor Controller
 * 导师控制器
 */
@RestController
@RequestMapping("/mentors")
public class MentorController {

    @Autowired
    private MentorService mentorService;

    /**
     * Create Mentor
     * 创建导师
     */
    @PostMapping
    public Map<String, Object> createMentor(@RequestBody Mentor mentor) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Validate required fields
            if (mentor.getName() == null || mentor.getName().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "Mentor name is required");
                return result;
            }

            Mentor createdMentor = mentorService.createMentor(mentor);

            result.put("code", 0);
            result.put("message", "Mentor created successfully");
            result.put("data", createdMentor);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to create mentor: " + e.getMessage());
        }

        return result;
    }

    /**
     * Update Mentor
     * 更新导师信息
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateMentor(@PathVariable Integer id, @RequestBody Mentor mentor) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Check if mentor exists
            Mentor existingMentor = mentorService.getMentorById(id);
            if (existingMentor == null) {
                result.put("code", 404);
                result.put("message", "Mentor not found");
                return result;
            }

            mentor.setId(id);
            mentorService.updateMentor(mentor);

            result.put("code", 0);
            result.put("message", "Mentor updated successfully");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to update mentor: " + e.getMessage());
        }

        return result;
    }

    /**
     * Delete Mentor
     * 删除导师
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteMentor(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Check if mentor exists
            Mentor existingMentor = mentorService.getMentorById(id);
            if (existingMentor == null) {
                result.put("code", 404);
                result.put("message", "Mentor not found");
                return result;
            }

            mentorService.deleteMentor(id);

            result.put("code", 0);
            result.put("message", "Mentor deleted successfully");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to delete mentor: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Mentor by ID
     * 根据ID获取导师
     */
    @GetMapping("/{id}")
    public Map<String, Object> getMentorById(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Mentor mentor = mentorService.getMentorById(id);

            if (mentor == null) {
                result.put("code", 404);
                result.put("message", "Mentor not found");
                return result;
            }

            // Increment view count
            mentorService.incrementViewCount(id);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", mentor);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get mentor: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Mentor List with Filters
     * 获取导师列表（支持筛选）
     */
    @GetMapping
    public Map<String, Object> getMentorList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) String researchArea,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean acceptingStudents) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Mentor> mentors = mentorService.getMentorListWithFilters(
                    keyword, institution, researchArea, title, department, acceptingStudents, page, limit);
            int total = mentorService.countMentorsWithFilters(
                    keyword, institution, researchArea, title, department, acceptingStudents);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", mentors);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get mentor list: " + e.getMessage());
        }

        return result;
    }

    /**
     * Search Mentors
     * 搜索导师
     */
    @GetMapping("/search")
    public Map<String, Object> searchMentors(
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

            List<Mentor> mentors = mentorService.searchMentors(keyword, page, limit);
            int total = mentorService.countSearchResults(keyword);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", mentors);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to search mentors: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Mentors by Institution
     * 根据机构获取导师
     */
    @GetMapping("/institution/{institution}")
    public Map<String, Object> getMentorsByInstitution(
            @PathVariable String institution,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Mentor> mentors = mentorService.getMentorsByInstitution(institution, page, limit);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", mentors);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get mentors by institution: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Mentors by Research Area
     * 根据研究方向获取导师
     */
    @GetMapping("/research-area/{researchArea}")
    public Map<String, Object> getMentorsByResearchArea(
            @PathVariable String researchArea,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Mentor> mentors = mentorService.getMentorsByResearchArea(researchArea, page, limit);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", mentors);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get mentors by research area: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Mentor by User ID
     * 根据用户ID获取导师
     */
    @GetMapping("/user/{userId}")
    public Map<String, Object> getMentorByUserId(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Mentor mentor = mentorService.getMentorByUserId(userId);

            if (mentor == null) {
                result.put("code", 404);
                result.put("message", "Mentor not found for user ID: " + userId);
                return result;
            }

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", mentor);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get mentor by user ID: " + e.getMessage());
        }

        return result;
    }
}
