package com.mentor.controller;

import com.mentor.entity.Rating;
import com.mentor.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rating Controller
 * 评分控制器
 */
@RestController
@RequestMapping("/ratings")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    /**
     * Create Rating
     * 创建评分
     */
    @PostMapping
    public Map<String, Object> createRating(@RequestBody Rating rating) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (rating.getMentorId() == null) {
                result.put("code", 400);
                result.put("message", "导师ID不能为空");
                return result;
            }
            if (rating.getStudentId() == null) {
                result.put("code", 400);
                result.put("message", "学生ID不能为空");
                return result;
            }
            if (rating.getRating() == null) {
                result.put("code", 400);
                result.put("message", "评分不能为空");
                return result;
            }

            Rating createdRating = ratingService.createRating(rating);

            result.put("code", 0);
            result.put("message", "评分提交成功");
            result.put("data", createdRating);

        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "评分提交失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Update Rating
     * 更新评分
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateRating(@PathVariable Integer id, @RequestBody Rating rating) {
        Map<String, Object> result = new HashMap<>();

        try {
            Rating existingRating = ratingService.getRatingById(id);
            if (existingRating == null) {
                result.put("code", 404);
                result.put("message", "评分不存在");
                return result;
            }

            rating.setId(id);
            ratingService.updateRating(rating);

            result.put("code", 0);
            result.put("message", "评分更新成功");

        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "评分更新失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Delete Rating
     * 删除评分
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteRating(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            ratingService.deleteRating(id);

            result.put("code", 0);
            result.put("message", "评分删除成功");

        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "评分删除失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Rating by ID
     * 根据ID获取评分
     */
    @GetMapping("/{id}")
    public Map<String, Object> getRatingById(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Rating rating = ratingService.getRatingById(id);

            if (rating == null) {
                result.put("code", 404);
                result.put("message", "评分不存在");
                return result;
            }

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", rating);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取评分失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Ratings by Mentor ID
     * 根据导师ID获取评分列表
     */
    @GetMapping("/mentor/{mentorId}")
    public Map<String, Object> getRatingsByMentorId(
            @PathVariable Integer mentorId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Rating> ratings = ratingService.getRatingsByMentorId(mentorId, page, limit);
            int total = ratingService.countRatingsByMentor(mentorId);
            Double average = ratingService.getAverageRatingByMentor(mentorId);

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", ratings);
            result.put("total", total);
            result.put("average", average != null ? average : 0.0);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取评分列表失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Ratings by Student ID
     * 根据学生ID获取评分列表
     */
    @GetMapping("/student/{studentId}")
    public Map<String, Object> getRatingsByStudentId(
            @PathVariable Integer studentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Rating> ratings = ratingService.getRatingsByStudentId(studentId, page, limit);

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", ratings);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取评分列表失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Mark Rating as Helpful
     * 标记评分有用
     */
    @PostMapping("/{id}/helpful")
    public Map<String, Object> markRatingHelpful(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            ratingService.markRatingHelpful(id);

            result.put("code", 0);
            result.put("message", "标记成功");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "标记失败: " + e.getMessage());
        }

        return result;
    }
}
