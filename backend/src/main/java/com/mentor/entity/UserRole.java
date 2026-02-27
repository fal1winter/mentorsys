package com.mentor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * UserRole Entity
 * 用户角色关联实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 角色ID
     */
    private Integer roleId;

    /**
     * 创建时间
     */
    private Date createTime;
}
