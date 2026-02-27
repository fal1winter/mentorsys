package com.mentor.config;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shiro Configuration
 * Apache Shiro安全框架配置 - 使用Redis存储Session
 */
@Configuration
public class ShiroConfig {

    @Value("${shiro.session-timeout:1800000}")
    private Long sessionTimeout;

    @Autowired
    private RedisSessionDAO redisSessionDAO;

    /**
     * Create BCrypt Credentials Matcher
     */
    @Bean
    public BCryptCredentialsMatcher credentialsMatcher() {
        return new BCryptCredentialsMatcher();
    }

    /**
     * Create CustomRealm
     */
    @Bean
    public CustomRealm customRealm(BCryptCredentialsMatcher credentialsMatcher) {
        CustomRealm customRealm = new CustomRealm();
        customRealm.setCredentialsMatcher(credentialsMatcher);
        return customRealm;
    }

    /**
     * Create Session Manager with Redis support
     * 创建使用Redis的Session管理器
     */
    @Bean
    public SessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        
        // 使用Redis Session DAO
        sessionManager.setSessionDAO(redisSessionDAO);
        
        // Session超时时间（毫秒）
        sessionManager.setGlobalSessionTimeout(sessionTimeout);
        
        // 删除无效Session
        sessionManager.setDeleteInvalidSessions(true);
        
        // 开启Session验证调度器
        sessionManager.setSessionValidationSchedulerEnabled(true);
        
        // Session验证间隔（毫秒）
        sessionManager.setSessionValidationInterval(sessionTimeout / 2);
        
        // 禁用URL重写（不在URL中暴露SessionId）
        sessionManager.setSessionIdUrlRewritingEnabled(false);
        
        // Cookie配置
        sessionManager.setSessionIdCookieEnabled(true);
        
        return sessionManager;
    }

    /**
     * Create SecurityManager
     */
    @Bean
    public SecurityManager securityManager(CustomRealm customRealm, SessionManager sessionManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(customRealm);
        securityManager.setSessionManager(sessionManager);
        return securityManager;
    }

    /**
     * Shiro Filter Configuration
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);

        // Login URL
        shiroFilterFactoryBean.setLoginUrl("/auth/login");
        // Success URL
        shiroFilterFactoryBean.setSuccessUrl("/");
        // Unauthorized URL
        shiroFilterFactoryBean.setUnauthorizedUrl("/auth/unauthorized");

        // Filter chain definition
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();

        // Public endpoints (no authentication required)
        filterChainDefinitionMap.put("/auth/login", "anon");
        filterChainDefinitionMap.put("/auth/register", "anon");
        filterChainDefinitionMap.put("/auth/logout", "anon");
        filterChainDefinitionMap.put("/mentors", "anon");
        filterChainDefinitionMap.put("/mentors/**", "anon");
        filterChainDefinitionMap.put("/publications", "anon");
        filterChainDefinitionMap.put("/publications/**", "anon");
        filterChainDefinitionMap.put("/ratings", "anon");
        filterChainDefinitionMap.put("/ratings/**", "anon");
        filterChainDefinitionMap.put("/keywords", "anon");
        filterChainDefinitionMap.put("/keywords/**", "anon");
        filterChainDefinitionMap.put("/users", "anon");
        filterChainDefinitionMap.put("/users/**", "anon");
        filterChainDefinitionMap.put("/applications", "anon");
        filterChainDefinitionMap.put("/applications/**", "anon");
        filterChainDefinitionMap.put("/recommendations", "anon");
        filterChainDefinitionMap.put("/recommendations/**", "anon");
        filterChainDefinitionMap.put("/chat", "anon");
        filterChainDefinitionMap.put("/chat/**", "anon");
        filterChainDefinitionMap.put("/students", "anon");
        filterChainDefinitionMap.put("/students/**", "anon");
        filterChainDefinitionMap.put("/scholars", "anon");
        filterChainDefinitionMap.put("/scholars/**", "anon");

        // Swagger/Knife4j documentation
        filterChainDefinitionMap.put("/doc.html", "anon");
        filterChainDefinitionMap.put("/swagger-ui.html", "anon");
        filterChainDefinitionMap.put("/swagger-resources/**", "anon");
        filterChainDefinitionMap.put("/v2/api-docs", "anon");
        filterChainDefinitionMap.put("/webjars/**", "anon");

        // Druid monitoring
        filterChainDefinitionMap.put("/druid/**", "anon");

        // WebSocket
        filterChainDefinitionMap.put("/ws/**", "anon");

        // Admin batch import - templates are public, import requires auth
        filterChainDefinitionMap.put("/admin/batch/template/**", "anon");
        filterChainDefinitionMap.put("/admin/batch/import/**", "anon");

        // All other requests require authentication
        filterChainDefinitionMap.put("/**", "authc");

        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);

        return shiroFilterFactoryBean;
    }
}
