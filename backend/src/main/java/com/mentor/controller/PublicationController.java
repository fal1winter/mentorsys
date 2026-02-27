package com.mentor.controller;

import com.mentor.entity.Mentor;
import com.mentor.entity.Publication;
import com.mentor.entity.PublicationAuthor;
import com.mentor.entity.Scholar;
import com.mentor.entity.User;
import com.mentor.mapper.MentorMapper;
import com.mentor.service.AuthService;
import com.mentor.service.PublicationService;
import com.mentor.service.ScholarService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Publication Controller
 * 学者作品/论文控制器
 */
@RestController
@RequestMapping("/publications")
public class PublicationController {

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private ScholarService scholarService;

    @Autowired
    private AuthService authService;

    @Autowired
    private MentorMapper mentorMapper;

    /**
     * 检查当前用户是否有权限操作指定导师的成果
     * 只有管理员或导师本人可以操作
     */
    private boolean canManagePublications(Integer mentorId) {
        try {
            Subject subject = SecurityUtils.getSubject();
            if (!subject.isAuthenticated()) return false;
            String username = (String) subject.getPrincipal();
            if (username == null) return false;
            User user = authService.getUserByUsername(username);
            if (user == null) return false;
            // 管理员可以操作所有
            if ("admin".equalsIgnoreCase(user.getUserType())) return true;
            // 导师只能操作自己的
            if ("mentor".equalsIgnoreCase(user.getUserType())) {
                Mentor mentor = mentorMapper.getMentorById(mentorId);
                return mentor != null && mentor.getUserId().equals(user.getId());
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Create Publication
     * 创建论文
     */
    @PostMapping
    public Map<String, Object> createPublication(@RequestBody Publication publication) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (publication.getTitle() == null || publication.getTitle().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "Publication title is required");
                return result;
            }
            if (publication.getMentorId() == null) {
                result.put("code", 400);
                result.put("message", "Mentor ID is required");
                return result;
            }

            if (!canManagePublications(publication.getMentorId())) {
                result.put("code", 403);
                result.put("message", "无权限操作");
                return result;
            }

            Publication createdPublication = publicationService.createPublication(publication);

            result.put("code", 0);
            result.put("message", "Publication created successfully");
            result.put("data", createdPublication);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to create publication: " + e.getMessage());
        }

        return result;
    }

    /**
     * Update Publication
     * 更新论文信息
     */
    @PutMapping("/{id}")
    public Map<String, Object> updatePublication(@PathVariable Integer id, @RequestBody Publication publication) {
        Map<String, Object> result = new HashMap<>();

        try {
            Publication existingPublication = publicationService.getPublicationById(id);
            if (existingPublication == null) {
                result.put("code", 404);
                result.put("message", "Publication not found");
                return result;
            }

            if (!canManagePublications(existingPublication.getMentorId())) {
                result.put("code", 403);
                result.put("message", "无权限操作");
                return result;
            }

            publication.setId(id);
            publicationService.updatePublication(publication);

            result.put("code", 0);
            result.put("message", "Publication updated successfully");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to update publication: " + e.getMessage());
        }

        return result;
    }

    /**
     * Delete Publication
     * 删除论文
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deletePublication(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Publication existingPublication = publicationService.getPublicationById(id);
            if (existingPublication == null) {
                result.put("code", 404);
                result.put("message", "Publication not found");
                return result;
            }

            if (!canManagePublications(existingPublication.getMentorId())) {
                result.put("code", 403);
                result.put("message", "无权限操作");
                return result;
            }

            publicationService.deletePublication(id);

            result.put("code", 0);
            result.put("message", "Publication deleted successfully");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to delete publication: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Publication by ID
     * 根据ID获取论文
     */
    @GetMapping("/{id}")
    public Map<String, Object> getPublicationById(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Publication publication = publicationService.getPublicationById(id);

            if (publication == null) {
                result.put("code", 404);
                result.put("message", "Publication not found");
                return result;
            }

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", publication);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get publication: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Publications by Mentor ID
     * 根据导师ID获取论文列表
     */
    @GetMapping("/mentor/{mentorId}")
    public Map<String, Object> getPublicationsByMentorId(
            @PathVariable Integer mentorId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Publication> publications = publicationService.getPublicationsByMentorId(mentorId, page, limit);
            int total = publicationService.countPublicationsByMentor(mentorId);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", publications);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get publications: " + e.getMessage());
        }

        return result;
    }

    /**
     * Search Publications
     * 搜索论文 (支持空关键词，返回所有论文)
     */
    @GetMapping("/search")
    public Map<String, Object> searchPublications(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Publication> publications;
            int total;

            // If keyword is empty or null, return all publications
            if (keyword == null || keyword.trim().isEmpty()) {
                publications = publicationService.getAllPublications(page, limit);
                total = publicationService.countAllPublications();
            } else {
                publications = publicationService.searchPublications(keyword, page, limit);
                total = publicationService.countSearchResults(keyword);
            }

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", publications);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to search publications: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Publications by Year
     * 根据年份获取论文
     */
    @GetMapping("/year/{year}")
    public Map<String, Object> getPublicationsByYear(
            @PathVariable Integer year,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Publication> publications = publicationService.getPublicationsByYear(year, page, limit);

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", publications);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get publications by year: " + e.getMessage());
        }

        return result;
    }

    /**
     * Add Author to Publication
     * 为论文添加作者
     */
    @PostMapping("/{id}/authors")
    public Map<String, Object> addAuthorToPublication(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            Publication publication = publicationService.getPublicationById(id);
            if (publication == null) {
                result.put("code", 404);
                result.put("message", "Publication not found");
                return result;
            }

            if (!canManagePublications(publication.getMentorId())) {
                result.put("code", 403);
                result.put("message", "无权限操作");
                return result;
            }

            Integer scholarId = (Integer) params.get("scholarId");
            Integer authorOrder = params.get("authorOrder") != null ? (Integer) params.get("authorOrder") : 1;
            Boolean isCorresponding = params.get("isCorresponding") != null ? (Boolean) params.get("isCorresponding") : false;

            if (scholarId == null) {
                result.put("code", 400);
                result.put("message", "Scholar ID is required");
                return result;
            }

            Scholar scholar = scholarService.getScholarById(scholarId);
            if (scholar == null) {
                result.put("code", 404);
                result.put("message", "Scholar not found");
                return result;
            }

            PublicationAuthor publicationAuthor = publicationService.addAuthorToPublication(id, scholarId, authorOrder, isCorresponding);

            result.put("code", 0);
            result.put("message", "Author added successfully");
            result.put("data", publicationAuthor);

        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to add author: " + e.getMessage());
        }

        return result;
    }

    /**
     * Remove Author from Publication
     * 从论文中移除作者
     */
    @DeleteMapping("/{id}/authors/{scholarId}")
    public Map<String, Object> removeAuthorFromPublication(
            @PathVariable Integer id,
            @PathVariable Integer scholarId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Publication publication = publicationService.getPublicationById(id);
            if (publication == null) {
                result.put("code", 404);
                result.put("message", "Publication not found");
                return result;
            }

            if (!canManagePublications(publication.getMentorId())) {
                result.put("code", 403);
                result.put("message", "无权限操作");
                return result;
            }

            publicationService.removeAuthorFromPublication(id, scholarId);

            result.put("code", 0);
            result.put("message", "Author removed successfully");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to remove author: " + e.getMessage());
        }

        return result;
    }

    /**
     * Update Publication Author Relationship
     * 更新论文作者关系
     */
    @PutMapping("/authors/{authorId}")
    public Map<String, Object> updatePublicationAuthor(
            @PathVariable Integer authorId,
            @RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 通过 authorId 查找对应的 publication，再检查权限
            PublicationAuthor pa = publicationService.getPublicationAuthorById(authorId);
            if (pa == null) {
                result.put("code", 404);
                result.put("message", "Author relationship not found");
                return result;
            }
            Publication pub = publicationService.getPublicationById(pa.getPublicationId());
            if (pub == null) {
                result.put("code", 404);
                result.put("message", "Publication not found");
                return result;
            }
            if (!canManagePublications(pub.getMentorId())) {
                result.put("code", 403);
                result.put("message", "无权限操作");
                return result;
            }

            Integer authorOrder = params.get("authorOrder") != null ? (Integer) params.get("authorOrder") : null;
            Boolean isCorresponding = params.get("isCorresponding") != null ? (Boolean) params.get("isCorresponding") : null;

            publicationService.updatePublicationAuthor(authorId, authorOrder, isCorresponding);

            result.put("code", 0);
            result.put("message", "Author relationship updated successfully");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to update author relationship: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get Authors for Publication
     * 获取论文的作者列表（包含学者详细信息）
     */
    @GetMapping("/{id}/authors")
    public Map<String, Object> getAuthorsForPublication(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Publication publication = publicationService.getPublicationById(id);
            if (publication == null) {
                result.put("code", 404);
                result.put("message", "Publication not found");
                return result;
            }

            List<PublicationAuthor> publicationAuthors = publicationService.getAuthorsByPublicationId(id);
            List<Map<String, Object>> authorsWithDetails = new ArrayList<>();

            for (PublicationAuthor pa : publicationAuthors) {
                Scholar scholar = scholarService.getScholarById(pa.getScholarId());
                if (scholar != null) {
                    Map<String, Object> authorInfo = new HashMap<>();
                    authorInfo.put("id", pa.getId());
                    authorInfo.put("scholarId", scholar.getId());
                    authorInfo.put("name", scholar.getName());
                    authorInfo.put("institution", scholar.getInstitution());
                    authorInfo.put("email", scholar.getEmail());
                    authorInfo.put("avatarUrl", scholar.getAvatarUrl());
                    authorInfo.put("authorOrder", pa.getAuthorOrder());
                    authorInfo.put("isCorresponding", pa.getIsCorresponding());
                    authorsWithDetails.add(authorInfo);
                }
            }

            result.put("code", 0);
            result.put("message", "Success");
            result.put("data", authorsWithDetails);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "Failed to get authors: " + e.getMessage());
        }

        return result;
    }
}
