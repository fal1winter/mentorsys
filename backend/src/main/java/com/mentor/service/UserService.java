package com.mentor.service;

import com.mentor.entity.User;
import com.mentor.entity.Mentor;
import com.mentor.entity.Student;
import com.mentor.mapper.AuthMapper;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User Service
 * 用户信息管理服务
 */
@Service
public class UserService {

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private MentorService mentorService;

    @Autowired
    private StudentService studentService;

    /**
     * Get user profile by ID
     * 根据ID获取用户资料
     */
    public User getUserProfile(Integer userId) {
        if (userId == null) {
            throw new RuntimeException("User ID cannot be null");
        }
        User user = authMapper.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        // Clear sensitive information
        user.setPassword(null);
        user.setSalt(null);
        return user;
    }

    /**
     * Update user's own profile
     * 用户更新自己的资料
     */
    @Transactional
    public User updateOwnProfile(Integer userId, User updateData) {
        if (userId == null) {
            throw new RuntimeException("User ID cannot be null");
        }

        // Get current user from Shiro
        Subject subject = SecurityUtils.getSubject();
        Integer currentUserId = (Integer) subject.getPrincipal();

        // Check if user is updating their own profile
        if (!userId.equals(currentUserId)) {
            throw new RuntimeException("You can only update your own profile");
        }

        // Get existing user
        User existingUser = authMapper.getUserById(userId);
        if (existingUser == null) {
            throw new RuntimeException("User not found");
        }

        // Validate and update allowed fields
        User userToUpdate = new User();
        userToUpdate.setId(userId);

        // Update email if provided and different
        if (updateData.getEmail() != null && !updateData.getEmail().equals(existingUser.getEmail())) {
            // Check if email is already taken by another user
            User userWithEmail = authMapper.getUserByEmail(updateData.getEmail());
            if (userWithEmail != null && !userWithEmail.getId().equals(userId)) {
                throw new RuntimeException("Email is already taken by another user");
            }
            userToUpdate.setEmail(updateData.getEmail());
        }

        // Update phone if provided
        if (updateData.getPhone() != null && !updateData.getPhone().equals(existingUser.getPhone())) {
            userToUpdate.setPhone(updateData.getPhone());
        }

        // Update username if provided and different
        if (updateData.getUsername() != null && !updateData.getUsername().equals(existingUser.getUsername())) {
            // Check if username is already taken by another user
            User userWithUsername = authMapper.getUserByUsername(updateData.getUsername());
            if (userWithUsername != null && !userWithUsername.getId().equals(userId)) {
                throw new RuntimeException("Username is already taken by another user");
            }
            userToUpdate.setUsername(updateData.getUsername());
        }

        // Update profile
        authMapper.updateUserProfile(userToUpdate);

        // Return updated user (without sensitive info)
        User updatedUser = authMapper.getUserById(userId);
        updatedUser.setPassword(null);
        updatedUser.setSalt(null);
        return updatedUser;
    }

    /**
     * Update any user's profile (admin only)
     * 管理员更新任意用户资料
     */
    @Transactional
    public User updateUserProfileByAdmin(Integer userId, User updateData) {
        if (userId == null) {
            throw new RuntimeException("User ID cannot be null");
        }

        // Check if current user is admin
        Subject subject = SecurityUtils.getSubject();
        Integer currentUserId = (Integer) subject.getPrincipal();
        if (!authService.hasRole(currentUserId, "ADMIN")) {
            throw new RuntimeException("Only administrators can update other users' profiles");
        }

        // Get existing user
        User existingUser = authMapper.getUserById(userId);
        if (existingUser == null) {
            throw new RuntimeException("User not found");
        }

        // Validate and update allowed fields
        User userToUpdate = new User();
        userToUpdate.setId(userId);

        // Update email if provided and different
        if (updateData.getEmail() != null && !updateData.getEmail().equals(existingUser.getEmail())) {
            // Check if email is already taken by another user
            User userWithEmail = authMapper.getUserByEmail(updateData.getEmail());
            if (userWithEmail != null && !userWithEmail.getId().equals(userId)) {
                throw new RuntimeException("Email is already taken by another user");
            }
            userToUpdate.setEmail(updateData.getEmail());
        }

        // Update phone if provided
        if (updateData.getPhone() != null && !updateData.getPhone().equals(existingUser.getPhone())) {
            userToUpdate.setPhone(updateData.getPhone());
        }

        // Update username if provided and different
        if (updateData.getUsername() != null && !updateData.getUsername().equals(existingUser.getUsername())) {
            // Check if username is already taken by another user
            User userWithUsername = authMapper.getUserByUsername(updateData.getUsername());
            if (userWithUsername != null && !userWithUsername.getId().equals(userId)) {
                throw new RuntimeException("Username is already taken by another user");
            }
            userToUpdate.setUsername(updateData.getUsername());
        }

        // Update profile
        authMapper.updateUserProfile(userToUpdate);

        // Return updated user (without sensitive info)
        User updatedUser = authMapper.getUserById(userId);
        updatedUser.setPassword(null);
        updatedUser.setSalt(null);
        return updatedUser;
    }

    /**
     * Get all users with pagination (admin only)
     * 获取所有用户列表（管理员）
     */
    public Map<String, Object> getAllUsers(Integer page, Integer limit) {
        // Check if current user is admin
        Subject subject = SecurityUtils.getSubject();
        String username = (String) subject.getPrincipal();
        if (username == null) {
            throw new RuntimeException("User not logged in");
        }

        User currentUser = authMapper.getUserByUsername(username);
        if (currentUser == null || !authService.hasRole(currentUser.getId(), "ADMIN")) {
            throw new RuntimeException("Only administrators can view all users");
        }

        if (page == null || page < 1) {
            page = 1;
        }
        if (limit == null || limit < 1) {
            limit = 10;
        }

        int offset = (page - 1) * limit;
        List<User> users = authMapper.getAllUsers(offset, limit);
        int total = authMapper.countUsers();

        // Clear sensitive information
        for (User user : users) {
            user.setPassword(null);
            user.setSalt(null);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", users);
        result.put("total", total);
        result.put("page", page);
        result.put("limit", limit);

        return result;
    }

    /**
     * Get current logged-in user's profile
     * 获取当前登录用户的资料
     */
    public User getCurrentUserProfile() {
        Subject subject = SecurityUtils.getSubject();
        String username = (String) subject.getPrincipal();

        if (username == null) {
            throw new RuntimeException("User not logged in");
        }

        User user = authMapper.getUserByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        return getUserProfile(user.getId());
    }

    /**
     * Get complete user profile with role-specific data
     * 获取完整的用户资料（包括角色特定数据）
     */
    public Map<String, Object> getCompleteUserProfile() {
        Subject subject = SecurityUtils.getSubject();
        String username = (String) subject.getPrincipal();

        if (username == null) {
            throw new RuntimeException("User not logged in");
        }

        User user = authMapper.getUserByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Clear sensitive information
        user.setPassword(null);
        user.setSalt(null);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);

        // Get role-specific data based on user type
        String userType = user.getUserType();
        if ("MENTOR".equalsIgnoreCase(userType) || "mentor".equalsIgnoreCase(userType)) {
            Mentor mentor = mentorService.getMentorByUserId(user.getId());
            result.put("mentorProfile", mentor);
        } else if ("STUDENT".equalsIgnoreCase(userType) || "student".equalsIgnoreCase(userType)) {
            Student student = studentService.getStudentByUserId(user.getId());
            result.put("studentProfile", student);
        }

        return result;
    }

    /**
     * Update complete user profile with role-specific data
     * 更新完整的用户资料（包括角色特定数据）
     */
    @Transactional
    public Map<String, Object> updateCompleteUserProfile(Map<String, Object> updateData) {
        Subject subject = SecurityUtils.getSubject();
        String username = (String) subject.getPrincipal();

        if (username == null) {
            throw new RuntimeException("User not logged in");
        }

        User user = authMapper.getUserByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Update basic user info if provided
        if (updateData.containsKey("username") || updateData.containsKey("email") || updateData.containsKey("phone")) {
            User userUpdate = new User();
            userUpdate.setId(user.getId());

            if (updateData.containsKey("username")) {
                userUpdate.setUsername((String) updateData.get("username"));
            }
            if (updateData.containsKey("email")) {
                userUpdate.setEmail((String) updateData.get("email"));
            }
            if (updateData.containsKey("phone")) {
                userUpdate.setPhone((String) updateData.get("phone"));
            }

            authMapper.updateUserProfile(userUpdate);
        }

        // Update role-specific data
        String userType = user.getUserType();
        if ("MENTOR".equalsIgnoreCase(userType) || "mentor".equalsIgnoreCase(userType)) {
            if (updateData.containsKey("mentorProfile")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mentorData = (Map<String, Object>) updateData.get("mentorProfile");
                Mentor mentor = mentorService.getMentorByUserId(user.getId());
                if (mentor != null) {
                    updateMentorFromMap(mentor, mentorData);
                    mentorService.updateMentor(mentor);
                }
            }
        } else if ("STUDENT".equalsIgnoreCase(userType) || "student".equalsIgnoreCase(userType)) {
            if (updateData.containsKey("studentProfile")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> studentData = (Map<String, Object>) updateData.get("studentProfile");
                Student student = studentService.getStudentByUserId(user.getId());
                if (student != null) {
                    updateStudentFromMap(student, studentData);
                    studentService.updateStudent(student);
                }
            }
        }

        return getCompleteUserProfile();
    }

    private void updateMentorFromMap(Mentor mentor, Map<String, Object> data) {
        if (data.containsKey("name")) mentor.setName((String) data.get("name"));
        if (data.containsKey("title")) mentor.setTitle((String) data.get("title"));
        if (data.containsKey("institution")) mentor.setInstitution((String) data.get("institution"));
        if (data.containsKey("department")) mentor.setDepartment((String) data.get("department"));
        if (data.containsKey("officeLocation")) mentor.setOfficeLocation((String) data.get("officeLocation"));
        if (data.containsKey("bio")) mentor.setBio((String) data.get("bio"));
        if (data.containsKey("educationBackground")) mentor.setEducationBackground((String) data.get("educationBackground"));
        if (data.containsKey("homepageUrl")) mentor.setHomepageUrl((String) data.get("homepageUrl"));
        if (data.containsKey("googleScholarUrl")) mentor.setGoogleScholarUrl((String) data.get("googleScholarUrl"));
        if (data.containsKey("acceptingStudents")) mentor.setAcceptingStudents((Boolean) data.get("acceptingStudents"));
        if (data.containsKey("maxStudents")) mentor.setMaxStudents((Integer) data.get("maxStudents"));
    }

    private void updateStudentFromMap(Student student, Map<String, Object> data) {
        if (data.containsKey("name")) student.setName((String) data.get("name"));
        if (data.containsKey("currentInstitution")) student.setCurrentInstitution((String) data.get("currentInstitution"));
        if (data.containsKey("major")) student.setMajor((String) data.get("major"));
        if (data.containsKey("degreeLevel")) student.setDegreeLevel((String) data.get("degreeLevel"));
        if (data.containsKey("graduationYear")) student.setGraduationYear((Integer) data.get("graduationYear"));
        if (data.containsKey("bio")) student.setBio((String) data.get("bio"));
        if (data.containsKey("cvUrl")) student.setCvUrl((String) data.get("cvUrl"));
    }
}
