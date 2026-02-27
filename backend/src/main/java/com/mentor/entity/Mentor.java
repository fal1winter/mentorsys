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
 * Mentor Entity
 * 导师实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mentor implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 导师ID
     */
    private Integer id;

    /**
     * 关联用户ID
     */
    private Integer userId;

    /**
     * 导师姓名
     */
    private String name;

    /**
     * 职称 (Professor, Associate Professor, etc.)
     */
    private String title;

    /**
     * 所属机构
     */
    private String institution;

    /**
     * 院系
     */
    private String department;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 电话
     */
    private String phone;

    /**
     * 办公室位置
     */
    private String officeLocation;

    /**
     * 研究方向列表 (JSON)
     */
    private String researchAreas;

    /**
     * 关键词列表 (JSON)
     */
    private String keywords;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 组内研究方向
     */
    private String groupDirection;

    /**
     * 期望学生素质
     */
    private String expectedStudentQualities;

    /**
     * 指导风格
     */
    private String mentoringStyle;

    /**
     * 可用名额
     */
    private Integer availablePositions;

    /**
     * 经费状况
     */
    private String fundingStatus;

    /**
     * 合作机会
     */
    private String collaborationOpportunities;

    /**
     * 教育背景
     */
    private String educationBackground;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 个人主页
     */
    private String homepageUrl;

    /**
     * Google Scholar链接
     */
    private String googleScholarUrl;

    /**
     * 是否接收学生
     */
    private Boolean acceptingStudents;

    /**
     * 最多接收学生数
     */
    private Integer maxStudents;

    /**
     * 当前学生数
     */
    private Integer currentStudents;

    /**
     * 平均评分
     */
    private BigDecimal ratingAvg;

    /**
     * 评分数量
     */
    private Integer ratingCount;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 状态 (0=禁用, 1=启用)
     */
    private Integer status;

    /**
     * 是否认证
     */
    private Boolean isVerified;

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
     * 研究方向列表 (解析后的)
     */
    private transient List<String> researchAreasList;

    /**
     * 关键词列表 (解析后的)
     */
    private transient List<String> keywordsList;
}
