package com.mentor.controller;

import com.mentor.entity.Application;
import com.mentor.entity.Mentor;
import com.mentor.entity.Student;
import com.mentor.mapper.MentorMapper;
import com.mentor.mapper.StudentMapper;
import com.mentor.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application Controller
 * 申请控制器
 */
@RestController
@RequestMapping("/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private MentorMapper mentorMapper;

    /**
     * Create Application
     * 创建申请
     */
    @PostMapping
    public Map<String, Object> createApplication(@RequestBody Application application) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (application.getStudentId() == null) {
                result.put("code", 400);
                result.put("message", "学生ID不能为空");
                return result;
            }
            if (application.getMentorId() == null) {
                result.put("code", 400);
                result.put("message", "导师ID不能为空");
                return result;
            }

            Application createdApplication = applicationService.createApplication(application);

            result.put("code", 0);
            result.put("message", "申请提交成功");
            result.put("data", createdApplication);

        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "申请提交失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Accept Application
     * 接受申请
     */
    @PostMapping("/{id}/accept")
    public Map<String, Object> acceptApplication(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String mentorFeedback = params != null ? params.get("mentorFeedback") : null;
            applicationService.acceptApplication(id, mentorFeedback);

            result.put("code", 0);
            result.put("message", "申请已接受");

        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "接受申请失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Reject Application
     * 拒绝申请
     */
    @PostMapping("/{id}/reject")
    public Map<String, Object> rejectApplication(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String mentorFeedback = params != null ? params.get("mentorFeedback") : null;
            applicationService.rejectApplication(id, mentorFeedback);

            result.put("code", 0);
            result.put("message", "申请已拒绝");

        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "拒绝申请失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Withdraw Application
     * 撤回申请
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> withdrawApplication(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            applicationService.withdrawApplication(id);

            result.put("code", 0);
            result.put("message", "申请已撤回");

        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "撤回申请失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Applications with filters
     * 获取申请列表（支持按studentId/mentorId/status过滤）
     */
    @GetMapping
    public Map<String, Object> getApplications(
            @RequestParam(required = false) Integer studentId,
            @RequestParam(required = false) Integer mentorId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Application> applications;
            int total;

            if (studentId != null) {
                // 前端传的是userId，需要转换为students表的id
                Student student = studentMapper.getStudentByUserId(studentId);
                Integer actualStudentId = student != null ? student.getId() : studentId;
                applications = applicationService.getApplicationsByStudentId(actualStudentId, page, limit);
                if (status != null && !status.isEmpty()) {
                    applications.removeIf(app -> !app.getStatus().equals(status));
                }
                total = applicationService.countApplicationsByStudent(actualStudentId);
            } else if (mentorId != null) {
                // 前端传的是userId，需要转换为mentors表的id
                Mentor mentor = mentorMapper.getMentorByUserId(mentorId);
                Integer actualMentorId = mentor != null ? mentor.getId() : mentorId;
                applications = applicationService.getApplicationsByMentorId(actualMentorId, page, limit);
                if (status != null && !status.isEmpty()) {
                    applications.removeIf(app -> !app.getStatus().equals(status));
                }
                total = applicationService.countApplicationsByMentor(actualMentorId);
            } else {
                applications = applicationService.getAllApplications(page, limit);
                total = applicationService.countAllApplications();
            }

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", applications);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取申请列表失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Application by ID
     * 根据ID获取申请
     */
    @GetMapping("/{id}")
    public Map<String, Object> getApplicationById(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Application application = applicationService.getApplicationById(id);

            if (application == null) {
                result.put("code", 404);
                result.put("message", "申请不存在");
                return result;
            }

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", application);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取申请失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Applications by Student ID
     * 根据学生ID获取申请列表
     */
    @GetMapping("/student/{studentId}")
    public Map<String, Object> getApplicationsByStudentId(
            @PathVariable Integer studentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Application> applications = applicationService.getApplicationsByStudentId(studentId, page, limit);
            int total = applicationService.countApplicationsByStudent(studentId);

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", applications);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取申请列表失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Applications by Mentor ID
     * 根据导师ID获取申请列表
     */
    @GetMapping("/mentor/{mentorId}")
    public Map<String, Object> getApplicationsByMentorId(
            @PathVariable Integer mentorId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Application> applications = applicationService.getApplicationsByMentorId(mentorId, page, limit);
            int total = applicationService.countApplicationsByMentor(mentorId);
            int pendingCount = applicationService.countPendingApplicationsByMentor(mentorId);

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", applications);
            result.put("total", total);
            result.put("pendingCount", pendingCount);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取申请列表失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Applications by Status
     * 根据状态获取申请列表
     */
    @GetMapping("/status/{status}")
    public Map<String, Object> getApplicationsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Application> applications = applicationService.getApplicationsByStatus(status, page, limit);

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", applications);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取申请列表失败: " + e.getMessage());
        }

        return result;
    }
}
