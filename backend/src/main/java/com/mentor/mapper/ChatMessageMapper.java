package com.mentor.mapper;

import com.mentor.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Chat Message Mapper
 * 聊天消息数据访问接口
 */
@Mapper
public interface ChatMessageMapper {

    /**
     * Insert chat message
     */
    void insertChatMessage(ChatMessage message);

    /**
     * Get messages by application ID
     */
    List<ChatMessage> getMessagesByApplicationId(@Param("applicationId") Integer applicationId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Mark message as read
     */
    void markMessageAsRead(@Param("id") Long id);

    /**
     * Count unread messages
     */
    int countUnreadMessages(@Param("applicationId") Integer applicationId, @Param("receiverType") String receiverType);

    /**
     * Get messages by student and mentor IDs
     */
    List<ChatMessage> getMessagesByStudentAndMentor(@Param("studentId") Integer studentId, @Param("mentorId") Integer mentorId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Count unread messages by student and mentor IDs
     */
    int countUnreadMessagesByStudentAndMentor(@Param("studentId") Integer studentId, @Param("mentorId") Integer mentorId, @Param("receiverType") String receiverType);

    /**
     * Get conversation list for a user
     * 获取用户的聊天会话列表
     */
    List<Map<String, Object>> getConversationsByUserId(@Param("userId") Integer userId, @Param("userType") String userType);

    /**
     * Count total unread messages for a user
     * 统计用户的总未读消息数
     */
    int countTotalUnreadMessages(@Param("userId") Integer userId, @Param("userType") String userType);
}
