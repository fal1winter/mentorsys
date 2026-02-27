package com.mentor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * Role Entity
 * 角色实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    private Integer id;

    /**
     * 角色名
     */
    private String roleName;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;
}
