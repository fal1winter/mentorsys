package com.mentor.service;

import com.mentor.entity.Student;
import com.mentor.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Student Service
 * 学生服务
 */
@Service
public class StudentService {

    @Autowired
    private StudentMapper studentMapper;

    /**
     * Create student
     * 创建学生
     */
    @Transactional
    public Student createStudent(Student student) {
        student.setCreateTime(new Date());
        student.setUpdateTime(new Date());
        student.setStatus(1);

        studentMapper.insertStudent(student);
        return student;
    }

    /**
     * Update student
     * 更新学生信息
     */
    @Transactional
    public void updateStudent(Student student) {
        student.setUpdateTime(new Date());
        studentMapper.updateStudent(student);
    }

    /**
     * Delete student
     * 删除学生
     */
    @Transactional
    public void deleteStudent(Integer id) {
        studentMapper.deleteStudentById(id);
    }

    /**
     * Get student by ID
     * 根据ID获取学生
     */
    public Student getStudentById(Integer id) {
        return studentMapper.getStudentById(id);
    }

    /**
     * Get student by user ID
     * 根据用户ID获取学生
     */
    public Student getStudentByUserId(Integer userId) {
        return studentMapper.getStudentByUserId(userId);
    }

    /**
     * Get student list with pagination
     * 分页获取学生列表
     */
    public List<Student> getStudentList(Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return studentMapper.getStudentList(offset, limit);
    }

    /**
     * Search students
     * 搜索学生
     */
    public List<Student> searchStudents(String keyword, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return studentMapper.searchStudents(keyword, offset, limit);
    }

    /**
     * Count total students
     * 统计学生总数
     */
    public int countStudents() {
        return studentMapper.countStudents();
    }

    /**
     * Count search results
     * 统计搜索结果数
     */
    public int countSearchResults(String keyword) {
        return studentMapper.countSearchResults(keyword);
    }

    /**
     * Get students by mentor ID
     * 根据导师ID获取学生列表
     */
    public List<Student> getStudentsByMentorId(Integer mentorId, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return studentMapper.getStudentsByMentorId(mentorId, offset, limit);
    }

    /**
     * Count students by mentor
     * 统计导师的学生数
     */
    public int countStudentsByMentor(Integer mentorId) {
        return studentMapper.countStudentsByMentor(mentorId);
    }

    /**
     * Get student list with filters
     * 带筛选条件获取学生列表
     */
    public List<Student> getStudentListWithFilters(Integer page, Integer limit, 
            String keyword, String institution, String major, String degreeLevel, 
            String researchInterest, String skill) {
        int offset = (page - 1) * limit;
        return studentMapper.getStudentListWithFilters(offset, limit, keyword, institution, major, degreeLevel, researchInterest, skill);
    }

    /**
     * Count students with filters
     * 统计带筛选条件的学生数
     */
    public int countStudentsWithFilters(String keyword, String institution, String major, 
            String degreeLevel, String researchInterest, String skill) {
        return studentMapper.countStudentsWithFilters(keyword, institution, major, degreeLevel, researchInterest, skill);
    }
}
