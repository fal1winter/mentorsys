package com.mentor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Publication Entity
 * 学术成果/论文实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Publication implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 论文ID
     */
    private Integer id;

    /**
     * 导师ID
     */
    private Integer mentorId;

    /**
     * 论文标题
     */
    private String title;

    /**
     * 作者列表
     */
    private String authors;

    /**
     * 发表会议/期刊
     */
    private String venue;

    /**
     * 发表年份
     */
    private Integer year;

    /**
     * 摘要
     */
    private String abstractText;

    /**
     * 关键词列表 (JSON)
     */
    private String keywords;

    /**
     * DOI
     */
    private String doi;

    /**
     * PDF链接
     */
    private String pdfUrl;

    /**
     * 引用次数
     */
    private Integer citationCount;

    /**
     * 类型 (Journal, Conference, Workshop)
     */
    private String publicationType;

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
     * 关键词列表 (解析后的)
     */
    private transient List<String> keywordsList;
}
