package com.mentor.mapper;

import com.mentor.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Student Mapper
 * 学生数据访问接口
 */
@Mapper
public interface StudentMapper {

    /**
     * Insert student
     */
    void insertStudent(Student student);

    /**
     * Update student
     */
    void updateStudent(Student student);

    /**
     * Delete student by ID
     */
    void deleteStudentById(@Param("id") Integer id);

    /**
     * Get student by ID
     */
    Student getStudentById(@Param("id") Integer id);

    /**
     * Get student by user ID
     */
    Student getStudentByUserId(@Param("userId") Integer userId);

    /**
     * Get all students with pagination
     */
    List<Student> getStudentList(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Search students by keyword
     */
    List<Student> searchStudents(@Param("keyword") String keyword, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Count total students
     */
    int countStudents();

    /**
     * Count search results
     */
    int countSearchResults(@Param("keyword") String keyword);

    /**
     * Get students by mentor ID (students who have accepted applications with this mentor)
     * 根据导师ID获取学生列表（已接受申请的学生）
     */
    List<Student> getStudentsByMentorId(@Param("mentorId") Integer mentorId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Count students by mentor ID
     * 统计导师的学生数
     */
    int countStudentsByMentor(@Param("mentorId") Integer mentorId);

    /**
     * Get students with filters
     * 带筛选条件获取学生列表
     */
    List<Student> getStudentListWithFilters(
        @Param("offset") Integer offset, 
        @Param("limit") Integer limit,
        @Param("keyword") String keyword,
        @Param("institution") String institution,
        @Param("major") String major,
        @Param("degreeLevel") String degreeLevel,
        @Param("researchInterest") String researchInterest,
        @Param("skill") String skill);

    /**
     * Count students with filters
     * 统计带筛选条件的学生数
     */
    int countStudentsWithFilters(
        @Param("keyword") String keyword,
        @Param("institution") String institution,
        @Param("major") String major,
        @Param("degreeLevel") String degreeLevel,
        @Param("researchInterest") String researchInterest,
        @Param("skill") String skill);
}
