package com.mentor.controller;

import com.mentor.entity.Permission;
import com.mentor.entity.Role;
import com.mentor.entity.User;
import com.mentor.service.AuthService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication Controller
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * User Registration
     * 用户注册
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String username = params.get("username");
            String password = params.get("password");
            String email = params.get("email");
            String userType = params.get("userType"); // student, mentor, admin
            String accessCode = params.get("accessCode"); // 身份码

            // Validate input
            if (username == null || username.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "Username is required");
                return result;
            }
            if (password == null || password.length() < 6) {
                result.put("code", 400);
                result.put("message", "Password must be at least 6 characters");
                return result;
            }
            if (email == null || !email.contains("@")) {
                result.put("code", 400);
                result.put("message", "Valid email is required");
                return result;
            }

            // Convert userType to lowercase for case-insensitive comparison
            if (userType != null) {
                userType = userType.toLowerCase();
            }

            // 验证身份码并确定用户类型
            if (accessCode != null && !accessCode.trim().isEmpty()) {
                // 管理员身份码
                if ("123456".equals(accessCode)) {
                    userType = "admin";
                }
                // 可以添加更多身份码验证
                // 例如：导师身份码、学生身份码等
            }

            if (userType == null || (!userType.equals("student") && !userType.equals("mentor") && !userType.equals("admin"))) {
                result.put("code", 400);
                result.put("message", "User type must be 'student', 'mentor', or 'admin'");
                return result;
            }

            User user = authService.register(username, password, email, userType);

            result.put("code", 0);
            result.put("message", "Registration successful");
            result.put("data", user);

        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Registration failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * User Login
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();

        try {
            String username = params.get("username");
            String password = params.get("password");

            // Validate input
            if (username == null || username.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "Username is required");
                return result;
            }
            if (password == null || password.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "Password is required");
                return result;
            }

            // Get Shiro subject
            Subject subject = SecurityUtils.getSubject();
            UsernamePasswordToken token = new UsernamePasswordToken(username, password);

            // Attempt login
            subject.login(token);

            // Update last login time
            User user = authService.getUserByUsername(username);
            authService.updateLastLoginTime(user.getId());

            // Set authentication cookie
            String sessionId = (String) subject.getSession().getId();
            setAuthCookie(sessionId.toString(), response);

            // Remove sensitive information
            user.setPassword(null);
            user.setSalt(null);

            result.put("code", 0);
            result.put("message", "Login successful");
            result.put("data", user);

        } catch (AuthenticationException e) {
            result.put("code", 401);
            result.put("message", "Invalid username or password");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Login failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * User Logout
     * 用户登出
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();

        try {
            Subject subject = SecurityUtils.getSubject();
            subject.logout();

            // Clear authentication cookie
            clearAuthCookie(response);

            result.put("code", 0);
            result.put("message", "Logout successful");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Logout failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Current User
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Map<String, Object> getCurrentUser() {
        Map<String, Object> result = new HashMap<>();

        try {
            Subject subject = SecurityUtils.getSubject();

            if (!subject.isAuthenticated()) {
                result.put("code", 401);
                result.put("message", "Not authenticated");
                return result;
            }

            String username = (String) subject.getPrincipal();
            User user = authService.getUserByUsername(username);

            if (user != null) {
                // Remove sensitive information
                user.setPassword(null);
                user.setSalt(null);

                result.put("code", 0);
                result.put("message", "Success");
                result.put("data", user);
            } else {
                result.put("code", 404);
                result.put("message", "User not found");
            }

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get current user: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get User Roles
     * 获取用户角色
     */
    @GetMapping("/roles")
    public Map<String, Object> getUserRoles() {
        Map<String, Object> result = new HashMap<>();

        try {
            Subject subject = SecurityUtils.getSubject();

            if (!subject.isAuthenticated()) {
                result.put("code", 401);
                result.put("message", "Not authenticated");
                return result;
            }

            String username = (String) subject.getPrincipal();
            User user = authService.getUserByUsername(username);

            if (user != null) {
                List<Role> roles = authService.getUserRoles(user.getId());
                result.put("code", 0);
                result.put("message", "Success");
                result.put("data", roles);
            } else {
                result.put("code", 404);
                result.put("message", "User not found");
            }

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get user roles: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get User Permissions
     * 获取用户权限
     */
    @GetMapping("/permissions")
    public Map<String, Object> getUserPermissions() {
        Map<String, Object> result = new HashMap<>();

        try {
            Subject subject = SecurityUtils.getSubject();

            if (!subject.isAuthenticated()) {
                result.put("code", 401);
                result.put("message", "Not authenticated");
                return result;
            }

            String username = (String) subject.getPrincipal();
            User user = authService.getUserByUsername(username);

            if (user != null) {
                List<Permission> permissions = authService.getUserPermissions(user.getId());
                result.put("code", 0);
                result.put("message", "Success");
                result.put("data", permissions);
            } else {
                result.put("code", 404);
                result.put("message", "User not found");
            }

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get user permissions: " + e.getMessage());
        }

        return result;
    }

    /**
     * Unauthorized Access Handler
     * 未授权访问处理
     */
    @GetMapping("/unauthorized")
    public Map<String, Object> unauthorized() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("message", "Unauthorized access");
        return result;
    }

    /**
     * Set authentication cookie
     * 设置认证Cookie
     */
    private void setAuthCookie(String sessionId, HttpServletResponse response) {
        // Cookie expires in 7 days (604800 seconds)
        ResponseCookie cookie = ResponseCookie.from("MENTOR_SESSION_ID", sessionId)
                .maxAge(604800)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Clear authentication cookie
     * 清除认证Cookie
     */
    private void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("MENTOR_SESSION_ID", "")
                .maxAge(0)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
