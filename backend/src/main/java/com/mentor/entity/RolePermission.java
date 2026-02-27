package com.mentor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * RolePermission Entity
 * 角色权限关联实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Integer id;

    /**
     * 角色ID
     */
    private Integer roleId;

    /**
     * 权限ID
     */
    private Integer permissionId;

    /**
     * 创建时间
     */
    private Date createTime;
}
