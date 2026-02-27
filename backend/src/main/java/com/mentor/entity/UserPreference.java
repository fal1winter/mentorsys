package com.mentor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * UserPreference Entity
 * 用户偏好分析实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 偏好ID
     */
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 用户类型
     */
    private String userType;

    /**
     * LLM生成的偏好描述
     */
    private String preferenceText;

    /**
     * 偏好关键词 (JSON)
     */
    private String preferenceKeywords;

    /**
     * 偏好主题 (JSON)
     */
    private String preferenceTopics;

    /**
     * 上次分析时的日志数
     */
    private Integer lastAnalyzedLogCount;

    /**
     * 当前日志数
     */
    private Integer currentLogCount;

    /**
     * 分析次数
     */
    private Integer analysisCount;

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
     * 偏好关键词列表 (解析后的)
     */
    private transient List<String> preferenceKeywordsList;

    /**
     * 偏好主题列表 (解析后的)
     */
    private transient List<String> preferenceTopicsList;
}
