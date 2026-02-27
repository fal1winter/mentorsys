package com.mentor.service;

import com.mentor.entity.Application;
import com.mentor.entity.Mentor;
import com.mentor.entity.Student;
import com.mentor.mapper.ApplicationMapper;
import com.mentor.mapper.MentorMapper;
import com.mentor.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Application Service
 * 申请服务
 */
@Service
public class ApplicationService {

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private MentorMapper mentorMapper;

    @Autowired
    private StudentMapper studentMapper;

    /**
     * Create application
     * 创建申请
     */
    @Transactional
    public Application createApplication(Application application) {
        // Look up student ID from user ID if needed
        // The frontend sends user ID as studentId, so we need to look up the actual student ID
        Integer userId = application.getStudentId();
        Student student = studentMapper.getStudentByUserId(userId);

        // If student record doesn't exist, create one automatically
        if (student == null) {
            student = new Student();
            student.setUserId(userId);
            student.setName("学生用户"); // Default name, user can update later
            student.setStatus(1);
            student.setCreateTime(new Date());
            student.setUpdateTime(new Date());
            studentMapper.insertStudent(student);
        }

        // Replace with actual student ID from students table
        Integer actualStudentId = student.getId();
        application.setStudentId(actualStudentId);

        // Check if student has already applied to this mentor
        Application existingApp = applicationMapper.getApplicationByStudentAndMentor(
                actualStudentId, application.getMentorId());

        if (existingApp != null && !existingApp.getStatus().equals("withdrawn")) {
            throw new RuntimeException("您已经向该导师提交过申请");
        }

        // Check if mentor is accepting students
        Mentor mentor = mentorMapper.getMentorById(application.getMentorId());
        if (mentor == null) {
            throw new RuntimeException("导师不存在");
        }
        if (!mentor.getAcceptingStudents()) {
            throw new RuntimeException("该导师当前不接收学生");
        }
        if (mentor.getCurrentStudents() >= mentor.getMaxStudents()) {
            throw new RuntimeException("该导师已达到最大学生数");
        }

        application.setStatus("pending");
        application.setApplyTime(new Date());
        application.setCreateTime(new Date());
        application.setUpdateTime(new Date());

        applicationMapper.insertApplication(application);
        return application;
    }

    /**
     * Update application
     * 更新申请
     */
    @Transactional
    public void updateApplication(Application application) {
        application.setUpdateTime(new Date());
        applicationMapper.updateApplication(application);
    }

    /**
     * Accept application
     * 接受申请
     */
    @Transactional
    public void acceptApplication(Integer id, String mentorFeedback) {
        Application application = applicationMapper.getApplicationById(id);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!application.getStatus().equals("pending")) {
            throw new RuntimeException("只能接受待处理的申请");
        }

        application.setStatus("accepted");
        application.setMentorFeedback(mentorFeedback);
        application.setResponseTime(new Date());
        application.setUpdateTime(new Date());

        applicationMapper.updateApplication(application);

        // Update mentor's current student count
        Mentor mentor = mentorMapper.getMentorById(application.getMentorId());
        if (mentor != null) {
            mentorMapper.updateStudentCount(mentor.getId(), mentor.getCurrentStudents() + 1);
        }
    }

    /**
     * Reject application
     * 拒绝申请
     */
    @Transactional
    public void rejectApplication(Integer id, String mentorFeedback) {
        Application application = applicationMapper.getApplicationById(id);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!application.getStatus().equals("pending")) {
            throw new RuntimeException("只能拒绝待处理的申请");
        }

        application.setStatus("rejected");
        application.setMentorFeedback(mentorFeedback);
        application.setResponseTime(new Date());
        application.setUpdateTime(new Date());

        applicationMapper.updateApplication(application);
    }

    /**
     * Withdraw application
     * 撤回申请
     */
    @Transactional
    public void withdrawApplication(Integer id) {
        Application application = applicationMapper.getApplicationById(id);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!application.getStatus().equals("pending")) {
            throw new RuntimeException("只能撤回待处理的申请");
        }

        application.setStatus("withdrawn");
        application.setUpdateTime(new Date());

        applicationMapper.updateApplication(application);
    }

    /**
     * Get application by ID
     * 根据ID获取申请
     */
    public Application getApplicationById(Integer id) {
        Application application = applicationMapper.getApplicationById(id);
        if (application != null) {
            // Load related student and mentor info
            Student student = studentMapper.getStudentById(application.getStudentId());
            Mentor mentor = mentorMapper.getMentorById(application.getMentorId());
            application.setStudent(student);
            application.setMentor(mentor);
        }
        return application;
    }

    /**
     * Get applications by student ID
     * 根据学生ID获取申请列表
     */
    public List<Application> getApplicationsByStudentId(Integer studentId, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        List<Application> applications = applicationMapper.getApplicationsByStudentId(studentId, offset, limit);

        // Load mentor info for each application
        for (Application app : applications) {
            Mentor mentor = mentorMapper.getMentorById(app.getMentorId());
            app.setMentor(mentor);
        }

        return applications;
    }

    /**
     * Get applications by mentor ID
     * 根据导师ID获取申请列表
     */
    public List<Application> getApplicationsByMentorId(Integer mentorId, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        List<Application> applications = applicationMapper.getApplicationsByMentorId(mentorId, offset, limit);

        // Load student info for each application
        for (Application app : applications) {
            Student student = studentMapper.getStudentById(app.getStudentId());
            app.setStudent(student);
        }

        return applications;
    }

    /**
     * Get applications by status
     * 根据状态获取申请列表
     */
    public List<Application> getApplicationsByStatus(String status, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return applicationMapper.getApplicationsByStatus(status, offset, limit);
    }

    /**
     * Count applications by student
     * 统计学生的申请数
     */
    public int countApplicationsByStudent(Integer studentId) {
        return applicationMapper.countApplicationsByStudent(studentId);
    }

    /**
     * Count applications by mentor
     * 统计导师的申请数
     */
    public int countApplicationsByMentor(Integer mentorId) {
        return applicationMapper.countApplicationsByMentor(mentorId);
    }

    /**
     * Count pending applications by mentor
     * 统计导师的待处理申请数
     */
    public int countPendingApplicationsByMentor(Integer mentorId) {
        return applicationMapper.countPendingApplicationsByMentor(mentorId);
    }

    /**
     * Get all applications with pagination
     * 获取所有申请列表（分页）
     */
    public List<Application> getAllApplications(Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        List<Application> applications = applicationMapper.getAllApplications(offset, limit);

        // Load student and mentor info for each application
        for (Application app : applications) {
            Student student = studentMapper.getStudentById(app.getStudentId());
            Mentor mentor = mentorMapper.getMentorById(app.getMentorId());
            app.setStudent(student);
            app.setMentor(mentor);
        }

        return applications;
    }

    /**
     * Count all applications
     * 统计所有申请数
     */
    public int countAllApplications() {
        return applicationMapper.countAllApplications();
    }
}
