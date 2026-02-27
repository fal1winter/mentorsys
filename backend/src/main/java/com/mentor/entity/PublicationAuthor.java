package com.mentor.entity;

import java.util.Date;

/**
 * Publication Author Entity
 * 论文-作者关系实体类
 */
public class PublicationAuthor {
    private Integer id;
    private Integer publicationId;
    private Integer scholarId;
    private Integer authorOrder;
    private Boolean isCorresponding;
    private Date createTime;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(Integer publicationId) {
        this.publicationId = publicationId;
    }

    public Integer getScholarId() {
        return scholarId;
    }

    public void setScholarId(Integer scholarId) {
        this.scholarId = scholarId;
    }

    public Integer getAuthorOrder() {
        return authorOrder;
    }

    public void setAuthorOrder(Integer authorOrder) {
        this.authorOrder = authorOrder;
    }

    public Boolean getIsCorresponding() {
        return isCorresponding;
    }

    public void setIsCorresponding(Boolean isCorresponding) {
        this.isCorresponding = isCorresponding;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
