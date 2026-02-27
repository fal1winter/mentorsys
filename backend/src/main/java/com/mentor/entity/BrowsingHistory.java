package com.mentor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * BrowsingHistory Entity
 * 浏览历史实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrowsingHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 历史记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 用户类型: student, mentor
     */
    private String userType;

    /**
     * 目标类型: mentor, publication, application
     */
    private String targetType;

    /**
     * 目标ID
     */
    private Integer targetId;

    /**
     * 行为类型: view, search, apply, rate
     */
    private String actionType;

    /**
     * 停留时长(秒)
     */
    private Integer durationSeconds;

    /**
     * 额外数据 (JSON)
     */
    private String additionalData;

    /**
     * 创建时间
     */
    private Date createTime;
}
