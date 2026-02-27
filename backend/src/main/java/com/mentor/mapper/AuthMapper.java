package com.mentor.mapper;

import com.mentor.entity.Permission;
import com.mentor.entity.Role;
import com.mentor.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Authentication Mapper
 * 认证数据访问接口
 */
@Mapper
public interface AuthMapper {

    /**
     * Insert new user
     */
    void insertUser(User user);

    /**
     * Get user by username
     */
    User getUserByUsername(@Param("username") String username);

    /**
     * Get user by email
     */
    User getUserByEmail(@Param("email") String email);

    /**
     * Get user by ID
     */
    User getUserById(@Param("userId") Integer userId);

    /**
     * Update last login time
     */
    void updateLastLoginTime(@Param("userId") Integer userId, @Param("lastLoginTime") Date lastLoginTime);

    /**
     * Update user password
     */
    void updatePassword(@Param("userId") Integer userId, @Param("password") String password, @Param("salt") String salt);

    /**
     * Update user status
     */
    void updateUserStatus(@Param("userId") Integer userId, @Param("status") Integer status);

    /**
     * Update user profile
     */
    void updateUserProfile(User user);

    /**
     * Get all users with pagination
     */
    List<User> getAllUsers(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Count total users
     */
    int countUsers();

    /**
     * Get role by name
     */
    Role getRoleByName(@Param("roleName") String roleName);

    /**
     * Insert user role
     */
    void insertUserRole(@Param("userId") Integer userId, @Param("roleId") Integer roleId);

    /**
     * Get user roles
     */
    List<Role> getUserRoles(@Param("userId") Integer userId);

    /**
     * Get user permissions
     */
    List<Permission> getUserPermissions(@Param("userId") Integer userId);
}
