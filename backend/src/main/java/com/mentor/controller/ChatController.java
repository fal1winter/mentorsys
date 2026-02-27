package com.mentor.controller;

import com.mentor.entity.ChatMessage;
import com.mentor.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat Controller
 * 聊天控制器
 */
@Controller
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * WebSocket message handler
     * WebSocket消息处理
     */
    @MessageMapping("/chat/send")
    public void sendMessage(@Payload ChatMessage message) {
        // Check if this is a direct chat message (has studentId and mentorId)
        if (message.getStudentId() != null && message.getMentorId() != null) {
            // Direct chat between student and mentor
            chatService.sendMessageByStudentAndMentor(message);
        } else if (message.getApplicationId() != null) {
            // Application-based chat
            chatService.sendMessage(message);
        } else {
            throw new IllegalArgumentException("Message must have either (studentId and mentorId) or applicationId");
        }
    }

    /**
     * WebSocket direct message handler
     * WebSocket直接消息处理
     */
    @MessageMapping("/chat/direct/send")
    public void sendDirectMessage(@Payload Map<String, Object> messageData) {
        try {
            // 构建ChatMessage
            ChatMessage message = new ChatMessage();
            message.setStudentId((Integer) messageData.get("studentId"));
            message.setMentorId((Integer) messageData.get("mentorId"));
            message.setSenderId((Integer) messageData.get("senderId"));
            message.setSenderType((String) messageData.get("senderType"));
            message.setMessageType((String) messageData.get("messageType"));
            message.setContent((String) messageData.get("content"));

            // 保存消息
            ChatMessage savedMessage = chatService.sendMessageByStudentAndMentor(message);

            // 广播到直接聊天频道
            String chatRoomId = (String) messageData.get("chatRoomId");
            if (chatRoomId != null) {
                messagingTemplate.convertAndSend("/topic/direct/" + chatRoomId, savedMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get chat messages (REST API)
     * 获取聊天消息（REST API）
     */
    @GetMapping("/{applicationId}/messages")
    @ResponseBody
    public Map<String, Object> getMessages(
            @PathVariable Integer applicationId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<ChatMessage> messages = chatService.getMessages(applicationId, page, limit);

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", messages);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取消息失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Mark message as read
     * 标记消息已读
     */
    @PutMapping("/messages/{messageId}/read")
    @ResponseBody
    public Map<String, Object> markAsRead(@PathVariable Long messageId) {
        Map<String, Object> result = new HashMap<>();

        try {
            chatService.markAsRead(messageId);

            result.put("code", 0);
            result.put("message", "标记成功");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "标记失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get unread message count
     * 获取未读消息数
     */
    @GetMapping("/{applicationId}/unread")
    @ResponseBody
    public Map<String, Object> getUnreadCount(
            @PathVariable Integer applicationId,
            @RequestParam String receiverType) {
        Map<String, Object> result = new HashMap<>();

        try {
            int count = chatService.countUnreadMessages(applicationId, receiverType);

            result.put("code", 0);
            result.put("message", "成功");
            result.put("count", count);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取未读数失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get chat messages by student and mentor IDs (REST API)
     * 根据学生ID和导师ID获取聊天消息（REST API）
     */
    @GetMapping("/direct/{studentId}/{mentorId}/messages")
    @ResponseBody
    public Map<String, Object> getMessagesByStudentAndMentor(
            @PathVariable Integer studentId,
            @PathVariable Integer mentorId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<ChatMessage> messages = chatService.getMessagesByStudentAndMentor(studentId, mentorId, page, limit);

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", messages);
            result.put("page", page);
            result.put("limit", limit);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取消息失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get unread message count by student and mentor IDs
     * 根据学生ID和导师ID获取未读消息数
     */
    @GetMapping("/direct/{studentId}/{mentorId}/unread")
    @ResponseBody
    public Map<String, Object> getUnreadCountByStudentAndMentor(
            @PathVariable Integer studentId,
            @PathVariable Integer mentorId,
            @RequestParam String receiverType) {
        Map<String, Object> result = new HashMap<>();

        try {
            int count = chatService.countUnreadMessagesByStudentAndMentor(studentId, mentorId, receiverType);

            result.put("code", 0);
            result.put("message", "成功");
            result.put("count", count);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取未读数失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get conversation list for a user
     * 获取用户的聊天会话列表
     */
    @GetMapping("/conversations")
    @ResponseBody
    public Map<String, Object> getConversations(
            @RequestParam Integer userId,
            @RequestParam String userType) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> conversations = chatService.getConversations(userId, userType);

            result.put("code", 0);
            result.put("message", "成功");
            result.put("data", conversations);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取聊天列表失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get total unread message count for a user
     * 获取用户的总未读消息数
     */
    @GetMapping("/unread/total")
    @ResponseBody
    public Map<String, Object> getTotalUnreadCount(
            @RequestParam Integer userId,
            @RequestParam String userType) {
        Map<String, Object> result = new HashMap<>();

        try {
            int count = chatService.countTotalUnreadMessages(userId, userType);

            result.put("code", 0);
            result.put("message", "成功");
            result.put("count", count);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取未读数失败: " + e.getMessage());
        }

        return result;
    }
}
