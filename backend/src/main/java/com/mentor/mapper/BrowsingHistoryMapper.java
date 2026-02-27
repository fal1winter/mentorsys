package com.mentor.mapper;

import com.mentor.entity.BrowsingHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Browsing History Mapper
 * 浏览历史数据访问接口
 */
@Mapper
public interface BrowsingHistoryMapper {

    /**
     * Insert browsing history
     */
    void insertBrowsingHistory(BrowsingHistory history);

    /**
     * Get user history
     */
    List<BrowsingHistory> getUserHistory(@Param("userId") Integer userId, @Param("limit") Integer limit);

    /**
     * Get recent mentor views
     */
    List<BrowsingHistory> getRecentMentorViews(@Param("userId") Integer userId, @Param("limit") Integer limit);

    /**
     * Count user history
     */
    int countUserHistory(@Param("userId") Integer userId);
}
