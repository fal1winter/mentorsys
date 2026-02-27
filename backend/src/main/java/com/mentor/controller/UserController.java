package com.mentor.controller;

import com.mentor.entity.User;
import com.mentor.service.UserService;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * User Controller
 * 用户信息管理控制器
 */
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get current user's profile
     * 获取当前用户资料
     */
    @GetMapping("/profile")
    @RequiresAuthentication
    public Map<String, Object> getCurrentUserProfile() {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.getCurrentUserProfile();
            response.put("code", 0);
            response.put("message", "Success");
            response.put("data", user);
        } catch (Exception e) {
            response.put("code", 1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Get user profile by ID
     * 根据ID获取用户资料
     */
    @GetMapping("/{userId}")
    @RequiresAuthentication
    public Map<String, Object> getUserProfile(@PathVariable Integer userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.getUserProfile(userId);
            response.put("code", 0);
            response.put("message", "Success");
            response.put("data", user);
        } catch (Exception e) {
            response.put("code", 1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Update current user's own profile
     * 更新当前用户自己的资料
     */
    @PutMapping("/profile")
    @RequiresAuthentication
    public Map<String, Object> updateOwnProfile(@RequestBody User updateData) {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = userService.getCurrentUserProfile();
            User updatedUser = userService.updateOwnProfile(currentUser.getId(), updateData);
            response.put("code", 0);
            response.put("message", "Profile updated successfully");
            response.put("data", updatedUser);
        } catch (Exception e) {
            response.put("code", 1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Update any user's profile (admin only)
     * 更新任意用户资料（仅管理员）
     */
    @PutMapping("/{userId}")
    @RequiresRoles("ADMIN")
    public Map<String, Object> updateUserProfileByAdmin(
            @PathVariable Integer userId,
            @RequestBody User updateData) {
        Map<String, Object> response = new HashMap<>();
        try {
            User updatedUser = userService.updateUserProfileByAdmin(userId, updateData);
            response.put("code", 0);
            response.put("message", "User profile updated successfully");
            response.put("data", updatedUser);
        } catch (Exception e) {
            response.put("code", 1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Get all users with pagination (admin only)
     * 获取所有用户列表（仅管理员）
     */
    @GetMapping("")
    @RequiresRoles("ADMIN")
    public Map<String, Object> getAllUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = userService.getAllUsers(page, limit);
            response.put("code", 0);
            response.put("message", "Success");
            response.put("data", result.get("data"));
            response.put("total", result.get("total"));
            response.put("page", result.get("page"));
            response.put("limit", result.get("limit"));
        } catch (Exception e) {
            response.put("code", 1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Get complete user profile with role-specific data
     * 获取完整的用户资料（包括角色特定数据）
     */
    @GetMapping("/profile/complete")
    @RequiresAuthentication
    public Map<String, Object> getCompleteUserProfile() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> profileData = userService.getCompleteUserProfile();
            response.put("code", 0);
            response.put("message", "Success");
            response.put("data", profileData);
        } catch (Exception e) {
            response.put("code", 1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Update complete user profile with role-specific data
     * 更新完整的用户资料（包括角色特定数据）
     */
    @PutMapping("/profile/complete")
    @RequiresAuthentication
    public Map<String, Object> updateCompleteUserProfile(@RequestBody Map<String, Object> updateData) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> updatedProfile = userService.updateCompleteUserProfile(updateData);
            response.put("code", 0);
            response.put("message", "Profile updated successfully");
            response.put("data", updatedProfile);
        } catch (Exception e) {
            response.put("code", 1);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
