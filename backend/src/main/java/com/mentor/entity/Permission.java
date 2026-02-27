package com.mentor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * Permission Entity
 * 权限实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    private Integer id;

    /**
     * 权限名
     */
    private String permissionName;

    /**
     * 资源
     */
    private String resource;

    /**
     * 操作
     */
    private String action;

    /**
     * 创建时间
     */
    private Date createTime;
}
