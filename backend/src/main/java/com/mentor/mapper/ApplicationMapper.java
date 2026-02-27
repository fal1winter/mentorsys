package com.mentor.mapper;

import com.mentor.entity.Application;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Application Mapper
 * 申请数据访问接口
 */
@Mapper
public interface ApplicationMapper {

    /**
     * Insert application
     */
    void insertApplication(Application application);

    /**
     * Update application
     */
    void updateApplication(Application application);

    /**
     * Get application by ID
     */
    Application getApplicationById(@Param("id") Integer id);

    /**
     * Get application by student and mentor
     */
    Application getApplicationByStudentAndMentor(@Param("studentId") Integer studentId, @Param("mentorId") Integer mentorId);

    /**
     * Get applications by student ID
     */
    List<Application> getApplicationsByStudentId(@Param("studentId") Integer studentId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Get applications by mentor ID
     */
    List<Application> getApplicationsByMentorId(@Param("mentorId") Integer mentorId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Get applications by status
     */
    List<Application> getApplicationsByStatus(@Param("status") String status, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Count applications by student
     */
    int countApplicationsByStudent(@Param("studentId") Integer studentId);

    /**
     * Count applications by mentor
     */
    int countApplicationsByMentor(@Param("mentorId") Integer mentorId);

    /**
     * Count pending applications by mentor
     */
    int countPendingApplicationsByMentor(@Param("mentorId") Integer mentorId);

    /**
     * Get all applications with pagination
     */
    List<Application> getAllApplications(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Count all applications
     */
    int countAllApplications();
}
