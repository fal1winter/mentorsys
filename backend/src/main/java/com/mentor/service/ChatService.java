package com.mentor.service;

import com.mentor.entity.Application;
import com.mentor.entity.ChatMessage;
import com.mentor.mapper.ApplicationMapper;
import com.mentor.mapper.ChatMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Chat Service
 * 聊天服务
 */
@Service
public class ChatService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Send message
     * 发送消息
     */
    @Transactional
    public ChatMessage sendMessage(ChatMessage message) {
        message.setCreateTime(new Date());
        message.setIsRead(false);

        // 如果有applicationId但缺少studentId/mentorId，从申请中查出并填充
        if (message.getApplicationId() != null && (message.getStudentId() == null || message.getMentorId() == null)) {
            Application application = applicationMapper.getApplicationById(message.getApplicationId());
            if (application != null) {
                message.setStudentId(application.getStudentId());
                message.setMentorId(application.getMentorId());
            }
        }

        // Save to database
        chatMessageMapper.insertChatMessage(message);

        // Send via WebSocket - 同时发送到两个频道
        if (message.getApplicationId() != null) {
            String destination = "/topic/chat/" + message.getApplicationId();
            messagingTemplate.convertAndSend(destination, message);
        }
        if (message.getStudentId() != null && message.getMentorId() != null) {
            String directDestination = "/topic/direct/direct_" + message.getStudentId() + "_" + message.getMentorId();
            messagingTemplate.convertAndSend(directDestination, message);
        }

        return message;
    }

    /**
     * Get messages by application ID
     * 根据申请ID获取消息列表
     */
    public List<ChatMessage> getMessages(Integer applicationId, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return chatMessageMapper.getMessagesByApplicationId(applicationId, offset, limit);
    }

    /**
     * Mark message as read
     * 标记消息已读
     */
    @Transactional
    public void markAsRead(Long messageId) {
        chatMessageMapper.markMessageAsRead(messageId);
    }

    /**
     * Count unread messages
     * 统计未读消息数
     */
    public int countUnreadMessages(Integer applicationId, String receiverType) {
        return chatMessageMapper.countUnreadMessages(applicationId, receiverType);
    }

    /**
     * Send message by student and mentor IDs
     * 根据学生ID和导师ID发送消息
     */
    @Transactional
    public ChatMessage sendMessageByStudentAndMentor(ChatMessage message) {
        message.setCreateTime(new Date());
        message.setIsRead(false);

        // Save to database
        chatMessageMapper.insertChatMessage(message);

        // Send via WebSocket
        String destination = "/topic/chat/" + message.getStudentId() + "/" + message.getMentorId();
        messagingTemplate.convertAndSend(destination, message);

        return message;
    }

    /**
     * Get messages by student and mentor IDs
     * 根据学生ID和导师ID获取消息列表
     */
    public List<ChatMessage> getMessagesByStudentAndMentor(Integer studentId, Integer mentorId, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        return chatMessageMapper.getMessagesByStudentAndMentor(studentId, mentorId, offset, limit);
    }

    /**
     * Count unread messages by student and mentor IDs
     * 根据学生ID和导师ID统计未读消息数
     */
    public int countUnreadMessagesByStudentAndMentor(Integer studentId, Integer mentorId, String receiverType) {
        return chatMessageMapper.countUnreadMessagesByStudentAndMentor(studentId, mentorId, receiverType);
    }

    /**
     * Get conversation list for a user
     * 获取用户的聊天会话列表
     */
    public List<Map<String, Object>> getConversations(Integer userId, String userType) {
        return chatMessageMapper.getConversationsByUserId(userId, userType);
    }

    /**
     * Count total unread messages for a user
     * 统计用户的总未读消息数
     */
    public int countTotalUnreadMessages(Integer userId, String userType) {
        return chatMessageMapper.countTotalUnreadMessages(userId, userType);
    }
}
