package com.mentor.mapper;

import com.mentor.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * User Preference Mapper
 * 用户偏好数据访问接口
 */
@Mapper
public interface UserPreferenceMapper {

    /**
     * Insert user preference
     */
    void insertUserPreference(UserPreference preference);

    /**
     * Update user preference
     */
    void updateUserPreference(UserPreference preference);

    /**
     * Get user preference by user ID
     */
    UserPreference getUserPreferenceByUserId(@Param("userId") Integer userId);
}
