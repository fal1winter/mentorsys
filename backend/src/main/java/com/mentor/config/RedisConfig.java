package com.mentor.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Configuration
 * Redis配置，支持Session序列化
 */
@Configuration
public class RedisConfig {

    /**
     * 通用RedisTemplate - 使用JSON序列化
     * 用于普通业务数据缓存
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 配置ObjectMapper支持类型信息
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 使用GenericJackson2JsonRedisSerializer序列化值
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // Key使用String序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 设置序列化器
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Session专用RedisTemplate - 使用JDK序列化
     * Shiro Session包含复杂对象，使用JDK序列化更可靠
     */
    @Bean(name = "sessionRedisTemplate")
    public RedisTemplate<String, Object> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key使用String序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        // Value使用JDK序列化（Shiro Session实现了Serializable接口）
        JdkSerializationRedisSerializer jdkSerializer = new JdkSerializationRedisSerializer();

        // 设置序列化器
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jdkSerializer);
        template.setHashValueSerializer(jdkSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
