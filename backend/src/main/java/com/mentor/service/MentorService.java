package com.mentor.service;

import com.mentor.entity.Mentor;
import com.mentor.mapper.MentorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Mentor Service
 * 导师服务
 */
@Service
public class MentorService {

    @Autowired
    private MentorMapper mentorMapper;

    /**
     * Create mentor
     * 创建导师
     */
    @Transactional
    public Mentor createMentor(Mentor mentor) {
        mentor.setCreateTime(new Date());
        mentor.setUpdateTime(new Date());
        mentor.setStatus(1);
        mentor.setViewCount(0);
        mentor.setRatingAvg(java.math.BigDecimal.ZERO);
        mentor.setRatingCount(0);
        mentor.setCurrentStudents(0);

        mentorMapper.insertMentor(mentor);
        return mentor;
    }

    /**
     * Update mentor
     * 更新导师信息
     */
    @Transactional
    public void updateMentor(Mentor mentor) {
        mentor.setUpdateTime(new Date());
        mentorMapper.updateMentor(mentor);
    }

    /**
     * Delete mentor
     * 删除导师
     */
    @Transactional
    public void deleteMentor(Integer id) {
        mentorMapper.deleteMentorById(id);
    }

    /**
     * Get mentor by ID
     * 根据ID获取导师
     */
    public Mentor getMentorById(Integer id) {
        return mentorMapper.getMentorById(id);
    }

    /**
     * Get mentor by user ID
     * 根据用户ID获取导师
     */
    public Mentor getMentorByUserId(Integer userId) {
        return mentorMapper.getMentorByUserId(userId);
    }

    /**
     * Get mentor list with pagination
     * 分页获取导师列表
     */
    public List<Mentor> getMentorList(Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return mentorMapper.getMentorList(offset, limit);
    }

    /**
     * Search mentors
     * 搜索导师
     */
    public List<Mentor> searchMentors(String keyword, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return mentorMapper.searchMentors(keyword, offset, limit);
    }

    /**
     * Get mentors by institution
     * 根据机构获取导师
     */
    public List<Mentor> getMentorsByInstitution(String institution, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return mentorMapper.getMentorsByInstitution(institution, offset, limit);
    }

    /**
     * Get mentors by research area
     * 根据研究方向获取导师
     */
    public List<Mentor> getMentorsByResearchArea(String researchArea, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return mentorMapper.getMentorsByResearchArea(researchArea, offset, limit);
    }

    /**
     * Count total mentors
     * 统计导师总数
     */
    public int countMentors() {
        return mentorMapper.countMentors();
    }

    /**
     * Count search results
     * 统计搜索结果数
     */
    public int countSearchResults(String keyword) {
        return mentorMapper.countSearchResults(keyword);
    }

    /**
     * Increment view count
     * 增加浏览次数（异步执行）
     */
    @Async
    @Transactional
    public void incrementViewCount(Integer id) {
        mentorMapper.incrementViewCount(id);
    }

    /**
     * Update mentor rating
     * 更新导师评分
     */
    @Transactional
    public void updateMentorRating(Integer id, Double ratingAvg, Integer ratingCount) {
        mentorMapper.updateMentorRating(id, ratingAvg, ratingCount);
    }

    /**
     * Update student count
     * 更新学生数量
     */
    @Transactional
    public void updateStudentCount(Integer id, Integer currentStudents) {
        mentorMapper.updateStudentCount(id, currentStudents);
    }

    /**
     * Get mentors with filters
     * 带筛选条件获取导师列表
     */
    public List<Mentor> getMentorListWithFilters(String keyword, String institution, String researchArea,
                                                  String title, String department, Boolean acceptingStudents,
                                                  Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return mentorMapper.getMentorListWithFilters(keyword, institution, researchArea, title, department, acceptingStudents, offset, limit);
    }

    /**
     * Count mentors with filters
     * 统计带筛选条件的导师数量
     */
    public int countMentorsWithFilters(String keyword, String institution, String researchArea,
                                       String title, String department, Boolean acceptingStudents) {
        return mentorMapper.countMentorsWithFilters(keyword, institution, researchArea, title, department, acceptingStudents);
    }
}
