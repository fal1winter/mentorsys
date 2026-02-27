package com.mentor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Student Entity
 * 学生实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 学生ID
     */
    private Integer id;

    /**
     * 关联用户ID
     */
    private Integer userId;

    /**
     * 学生姓名
     */
    private String name;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 电话
     */
    private String phone;

    /**
     * 当前学校
     */
    private String currentInstitution;

    /**
     * 专业
     */
    private String major;

    /**
     * 学位级别 (Bachelor, Master, PhD)
     */
    private String degreeLevel;

    /**
     * 毕业年份
     */
    private Integer graduationYear;

    /**
     * GPA
     */
    private BigDecimal gpa;

    /**
     * 研究兴趣列表 (JSON)
     */
    private String researchInterests;

    /**
     * 关键词列表 (JSON)
     */
    private String keywords;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 个人能力描述
     */
    private String personalAbilities;

    /**
     * 期望研究方向
     */
    private String expectedResearchDirection;

    /**
     * 期望导师风格
     */
    private String preferredMentorStyle;

    /**
     * 可用时间
     */
    private String availableTime;

    /**
     * 编程技能 (JSON)
     */
    private String programmingSkills;

    /**
     * 发表论文数
     */
    private Integer publicationsCount;

    /**
     * 项目经验
     */
    private String projectExperience;

    /**
     * 简历URL
     */
    private String cvUrl;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    // Transient fields (not in database)
    /**
     * 研究兴趣列表 (解析后的)
     */
    private transient List<String> researchInterestsList;

    /**
     * 关键词列表 (解析后的)
     */
    private transient List<String> keywordsList;
}
