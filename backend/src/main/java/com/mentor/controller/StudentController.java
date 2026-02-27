package com.mentor.controller;

import com.mentor.entity.Student;
import com.mentor.service.StudentService;
import com.mentor.service.SemanticRecommendationService;
import com.mentor.service.EnhancedRecommendationService;
import com.mentor.service.MilvusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Student Controller
 * 学生控制器
 */
@RestController
@RequestMapping("/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private SemanticRecommendationService semanticRecommendationService;

    @Autowired
    private EnhancedRecommendationService enhancedRecommendationService;

    @Autowired
    private MilvusService milvusService;

    /**
     * Create Student
     * 创建学生
     */
    @PostMapping
    public Map<String, Object> createStudent(@RequestBody Student student) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (student.getName() == null || student.getName().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "Student name is required");
                return result;
            }

            Student createdStudent = studentService.createStudent(student);

            // 创建后索引到向量数据库
            try {
                milvusService.indexStudent(createdStudent);
            } catch (Exception e) {
                // 向量索引失败不影响主流程
            }

            result.put("code", 0);
            result.put("message", "Student created successfully");
            result.put("data", createdStudent);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to create student: " + e.getMessage());
        }

        return result;
    }

    /**
     * Update Student
     * 更新学生信息
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateStudent(@PathVariable Integer id, @RequestBody Student student) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 先尝试按学生ID查找，如果找不到再按用户ID查找
            Student existingStudent = studentService.getStudentById(id);
            if (existingStudent == null) {
                existingStudent = studentService.getStudentByUserId(id);
            }
            
            if (existingStudent == null) {
                result.put("code", 404);
                result.put("message", "Student not found");
                return result;
            }

            // 使用实际的学生ID进行更新
            student.setId(existingStudent.getId());
            studentService.updateStudent(student);

            // 获取更新后的完整学生信息用于索引
            Student updatedStudent = studentService.getStudentById(existingStudent.getId());

            // 更新向量索引（异步，不阻塞主流程）
            try {
                milvusService.indexStudent(updatedStudent);
            } catch (Exception e) {
                // 向量索引失败不影响主流程，只记录日志
            }

            // 清除该学生的推荐缓存
            try {
                semanticRecommendationService.invalidateStudentCache(existingStudent.getId());
                enhancedRecommendationService.invalidateMentorRecommendationCache(existingStudent.getId());
            } catch (Exception e) {
                // 缓存清除失败不影响主流程
            }

            result.put("code", 0);
            result.put("message", "Student updated successfully");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to update student: " + e.getMessage());
        }

        return result;
    }

    /**
     * Delete Student
     * 删除学生
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteStudent(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Student existingStudent = studentService.getStudentById(id);
            if (existingStudent == null) {
                result.put("code", 404);
                result.put("message", "Student not found");
                return result;
            }

            studentService.deleteStudent(id);

            // 从向量数据库中删除
            try {
                milvusService.deleteStudentProfile(Long.valueOf(id));
            } catch (Exception e) {
                // 向量删除失败不影响主流程
            }

            result.put("code", 0);
            result.put("message", "Student deleted successfully");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to delete student: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Student by ID
     * 根据ID获取学生
     */
    @GetMapping("/{id}")
    public Map<String, Object> getStudentById(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Student student = studentService.getStudentById(id);

            // 如果按学生ID找不到，尝试按用户ID查找
            if (student == null) {
                student = studentService.getStudentByUserId(id);
            }

            if (student == null) {
                result.put("code", 404);
                result.put("message", "Student not found");
                return result;
            }

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", student);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get student: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Student by User ID
     * 根据用户ID获取学生
     */
    @GetMapping("/user/{userId}")
    public Map<String, Object> getStudentByUserId(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Student student = studentService.getStudentByUserId(userId);

            if (student == null) {
                result.put("code", 404);
                result.put("message", "Student not found for user: " + userId);
                return result;
            }

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", student);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get student: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Student List with filters
     * 获取学生列表（支持筛选）
     */
    @GetMapping
    public Map<String, Object> getStudentList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String degreeLevel,
            @RequestParam(required = false) String researchInterest,
            @RequestParam(required = false) String skill) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Student> students = studentService.getStudentListWithFilters(
                page, limit, keyword, institution, major, degreeLevel, researchInterest, skill);
            int total = studentService.countStudentsWithFilters(
                keyword, institution, major, degreeLevel, researchInterest, skill);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", students);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get student list: " + e.getMessage());
        }

        return result;
    }

    /**
     * Search Students
     * 搜索学生
     */
    @GetMapping("/search")
    public Map<String, Object> searchStudents(
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

            List<Student> students = studentService.searchStudents(keyword, page, limit);
            int total = studentService.countSearchResults(keyword);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", students);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to search students: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Students by Mentor ID
     * 根据导师ID获取学生列表
     */
    @GetMapping("/mentor/{mentorId}")
    public Map<String, Object> getStudentsByMentorId(
            @PathVariable Integer mentorId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Student> students = studentService.getStudentsByMentorId(mentorId, page, limit);
            int total = studentService.countStudentsByMentor(mentorId);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", students);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get students: " + e.getMessage());
        }

        return result;
    }
}
