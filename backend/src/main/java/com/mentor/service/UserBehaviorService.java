package com.mentor.service;

import com.mentor.entity.BrowsingHistory;
import com.mentor.mapper.BrowsingHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * User Behavior Service
 * 用户行为跟踪服务
 */
@Service
public class UserBehaviorService {

    @Autowired
    private BrowsingHistoryMapper browsingHistoryMapper;

    /**
     * Track user behavior
     * 记录用户行为
     */
    @Transactional
    public void trackBehavior(Integer userId, String userType, String targetType,
                             Integer targetId, String actionType, Integer durationSeconds) {
        BrowsingHistory history = BrowsingHistory.builder()
                .userId(userId)
                .userType(userType)
                .targetType(targetType)
                .targetId(targetId)
                .actionType(actionType)
                .durationSeconds(durationSeconds)
                .createTime(new Date())
                .build();

        browsingHistoryMapper.insertBrowsingHistory(history);
    }

    /**
     * Get user browsing history
     * 获取用户浏览历史
     */
    public List<BrowsingHistory> getUserHistory(Integer userId, Integer limit) {
        return browsingHistoryMapper.getUserHistory(userId, limit);
    }

    /**
     * Get recent mentor views
     * 获取最近浏览的导师
     */
    public List<BrowsingHistory> getRecentMentorViews(Integer userId, Integer limit) {
        return browsingHistoryMapper.getRecentMentorViews(userId, limit);
    }

    /**
     * Count user history
     * 统计用户历史记录数
     */
    public int countUserHistory(Integer userId) {
        return browsingHistoryMapper.countUserHistory(userId);
    }
}
