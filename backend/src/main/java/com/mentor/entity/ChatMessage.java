package com.mentor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * ChatMessage Entity
 * 聊天消息实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 关联申请ID（可选，用于向后兼容）
     */
    private Integer applicationId;

    /**
     * 学生ID
     */
    private Integer studentId;

    /**
     * 导师ID
     */
    private Integer mentorId;

    /**
     * 发送者ID
     */
    private Integer senderId;

    /**
     * 发送者类型: student, mentor
     */
    private String senderType;

    /**
     * 消息类型: text, file, image
     */
    private String messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 创建时间
     */
    private Date createTime;
}
