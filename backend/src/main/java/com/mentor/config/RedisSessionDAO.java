package com.mentor.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis Session DAO for Shiro
 * 使用Redis存储Shiro Session，解决后端重启后Session丢失问题
 */
@Slf4j
@Component
public class RedisSessionDAO extends AbstractSessionDAO {

    private static final String SESSION_PREFIX = "mentor:session:";

    private final RedisTemplate<String, Object> sessionRedisTemplate;

    @Value("${shiro.session-timeout:1800000}")
    private Long sessionTimeout; // 默认30分钟

    public RedisSessionDAO(@Qualifier("sessionRedisTemplate") RedisTemplate<String, Object> sessionRedisTemplate) {
        this.sessionRedisTemplate = sessionRedisTemplate;
    }

    /**
     * 创建Session
     */
    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = generateSessionId(session);
        assignSessionId(session, sessionId);
        saveSession(session);
        log.info("创建Session: {}", sessionId);
        return sessionId;
    }

    /**
     * 读取Session
     */
    @Override
    protected Session doReadSession(Serializable sessionId) {
        if (sessionId == null) {
            return null;
        }
        String key = getKey(sessionId);
        try {
            Session session = (Session) sessionRedisTemplate.opsForValue().get(key);
            if (session != null) {
                log.debug("读取Session: {}", sessionId);
            }
            return session;
        } catch (Exception e) {
            log.error("读取Session失败: {}, 错误: {}", sessionId, e.getMessage());
            return null;
        }
    }

    /**
     * 更新Session
     */
    @Override
    public void update(Session session) throws UnknownSessionException {
        if (session == null || session.getId() == null) {
            return;
        }
        saveSession(session);
        log.debug("更新Session: {}", session.getId());
    }

    /**
     * 删除Session
     */
    @Override
    public void delete(Session session) {
        if (session == null || session.getId() == null) {
            return;
        }
        String key = getKey(session.getId());
        try {
            sessionRedisTemplate.delete(key);
            log.info("删除Session: {}", session.getId());
        } catch (Exception e) {
            log.error("删除Session失败: {}, 错误: {}", session.getId(), e.getMessage());
        }
    }

    /**
     * 获取活跃Session集合
     */
    @Override
    public Collection<Session> getActiveSessions() {
        Set<Session> sessions = new HashSet<>();
        try {
            Set<String> keys = sessionRedisTemplate.keys(SESSION_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    try {
                        Session session = (Session) sessionRedisTemplate.opsForValue().get(key);
                        if (session != null) {
                            sessions.add(session);
                        }
                    } catch (Exception e) {
                        log.warn("读取Session失败: {}", key);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取活跃Session失败: {}", e.getMessage());
        }
        return sessions;
    }

    /**
     * 保存Session到Redis
     */
    private void saveSession(Session session) {
        if (session == null || session.getId() == null) {
            return;
        }
        String key = getKey(session.getId());
        try {
            // 使用Session的超时时间，如果没有则使用默认值
            long timeout = session.getTimeout();
            if (timeout <= 0) {
                timeout = sessionTimeout;
            }
            sessionRedisTemplate.opsForValue().set(key, session, timeout, TimeUnit.MILLISECONDS);
            log.debug("保存Session到Redis: {}, 超时: {}ms", session.getId(), timeout);
        } catch (Exception e) {
            log.error("保存Session失败: {}, 错误: {}", session.getId(), e.getMessage());
        }
    }

    /**
     * 生成Redis Key
     */
    private String getKey(Serializable sessionId) {
        return SESSION_PREFIX + sessionId;
    }
}
