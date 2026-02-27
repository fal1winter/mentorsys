package com.mentor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * Application Entity
 * 申请匹配实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 申请ID
     */
    private Integer id;

    /**
     * 学生ID
     */
    private Integer studentId;

    /**
     * 导师ID
     */
    private Integer mentorId;

    /**
     * 状态: pending, accepted, rejected, withdrawn
     */
    private String status;

    /**
     * 申请信
     */
    private String applicationLetter;

    /**
     * 研究计划
     */
    private String researchProposal;

    /**
     * 学生留言
     */
    private String studentMessage;

    /**
     * 导师反馈
     */
    private String mentorFeedback;

    /**
     * 申请时间
     */
    private Date applyTime;

    /**
     * 回复时间
     */
    private Date responseTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    // Transient fields
    /**
     * 学生信息
     */
    private transient Student student;

    /**
     * 导师信息
     */
    private transient Mentor mentor;
}
