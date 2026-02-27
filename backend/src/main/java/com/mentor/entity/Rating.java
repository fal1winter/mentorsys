package com.mentor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Rating Entity
 * 导师评价实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评价ID
     */
    private Integer id;

    /**
     * 导师ID
     */
    private Integer mentorId;

    /**
     * 学生ID
     */
    private Integer studentId;

    /**
     * 评分 (0.1-10.0)
     */
    private Double rating;

    /**
     * 评价内容
     */
    private String comment;

    /**
     * 各方面评分 (JSON)
     * {"guidance": 5, "communication": 4, "resources": 5}
     */
    private String aspects;

    /**
     * 是否匿名
     */
    private Boolean isAnonymous;

    /**
     * 是否认证评价
     */
    private Boolean isVerified;

    /**
     * 有用数
     */
    private Integer helpfulCount;

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
     * 各方面评分 (解析后的)
     */
    private transient Map<String, Integer> aspectsMap;

    /**
     * 学生姓名 (关联查询)
     */
    private transient String studentName;

    /**
     * 导师姓名 (关联查询)
     */
    private transient String mentorName;
}
