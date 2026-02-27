package com.mentor.service;

import com.mentor.entity.*;
import com.mentor.mapper.AuthMapper;
import com.mentor.mapper.StudentMapper;
import com.mentor.mapper.MentorMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Authentication Service
 * 认证服务
 */
@Service
public class AuthService {

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private MentorMapper mentorMapper;

    /**
     * Register new user
     * 注册新用户
     */
    @Transactional
    public User register(String username, String password, String email, String userType) {
        // Check if username already exists
        User existingUser = authMapper.getUserByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        User existingEmail = authMapper.getUserByEmail(email);
        if (existingEmail != null) {
            throw new RuntimeException("Email already exists");
        }

        // Generate salt and hash password
        String salt = UUID.randomUUID().toString();
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Create user
        User user = User.builder()
                .username(username)
                .password(hashedPassword)
                .salt(salt)
                .email(email)
                .userType(userType)
                .status(1)
                .createTime(new Date())
                .updateTime(new Date())
                .build();

        authMapper.insertUser(user);

        // Assign default role based on user type
        Role role = authMapper.getRoleByName(userType.toUpperCase());
        if (role != null) {
            authMapper.insertUserRole(user.getId(), role.getId());
        }

        // Auto-create student or mentor record based on user type
        if ("STUDENT".equalsIgnoreCase(userType) || "student".equalsIgnoreCase(userType)) {
            Student student = new Student();
            student.setUserId(user.getId());
            student.setName(username); // Use username as default name
            student.setEmail(email);
            student.setStatus(1);
            student.setCreateTime(new Date());
            student.setUpdateTime(new Date());
            studentMapper.insertStudent(student);
        } else if ("MENTOR".equalsIgnoreCase(userType) || "mentor".equalsIgnoreCase(userType)) {
            Mentor mentor = new Mentor();
            mentor.setUserId(user.getId());
            mentor.setName(username); // Use username as default name
            mentor.setEmail(email);
            mentor.setAcceptingStudents(true);
            mentor.setMaxStudents(5);
            mentor.setCurrentStudents(0);
            mentor.setStatus(1);
            mentor.setViewCount(0);
            mentor.setRatingAvg(java.math.BigDecimal.ZERO);
            mentor.setRatingCount(0);
            mentor.setCreateTime(new Date());
            mentor.setUpdateTime(new Date());
            mentorMapper.insertMentor(mentor);
        }

        return user;
    }

    /**
     * Verify user password
     * 验证用户密码
     */
    public boolean verifyPassword(String username, String password) {
        User user = authMapper.getUserByUsername(username);
        if (user == null) {
            return false;
        }
        return BCrypt.checkpw(password, user.getPassword());
    }

    /**
     * Get user by username
     * 根据用户名获取用户
     */
    public User getUserByUsername(String username) {
        return authMapper.getUserByUsername(username);
    }

    /**
     * Get user by email
     * 根据邮箱获取用户
     */
    public User getUserByEmail(String email) {
        return authMapper.getUserByEmail(email);
    }

    /**
     * Get user by ID
     * 根据ID获取用户
     */
    public User getUserById(Integer userId) {
        return authMapper.getUserById(userId);
    }

    /**
     * Update last login time
     * 更新最后登录时间
     */
    public void updateLastLoginTime(Integer userId) {
        authMapper.updateLastLoginTime(userId, new Date());
    }

    /**
     * Get user roles
     * 获取用户角色
     */
    public List<Role> getUserRoles(Integer userId) {
        return authMapper.getUserRoles(userId);
    }

    /**
     * Get user permissions
     * 获取用户权限
     */
    public List<Permission> getUserPermissions(Integer userId) {
        return authMapper.getUserPermissions(userId);
    }

    /**
     * Check if user has role
     * 检查用户是否有指定角色
     */
    public boolean hasRole(Integer userId, String roleName) {
        List<Role> roles = getUserRoles(userId);
        return roles.stream().anyMatch(role -> role.getRoleName().equals(roleName));
    }

    /**
     * Check if user has permission
     * 检查用户是否有指定权限
     */
    public boolean hasPermission(Integer userId, String permissionName) {
        List<Permission> permissions = getUserPermissions(userId);
        return permissions.stream().anyMatch(perm -> perm.getPermissionName().equals(permissionName));
    }

    /**
     * Update user password
     * 更新用户密码
     */
    @Transactional
    public void updatePassword(Integer userId, String newPassword) {
        String salt = UUID.randomUUID().toString();
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        authMapper.updatePassword(userId, hashedPassword, salt);
    }

    /**
     * Update user status
     * 更新用户状态
     */
    @Transactional
    public void updateUserStatus(Integer userId, Integer status) {
        authMapper.updateUserStatus(userId, status);
    }
}
