package com.mentor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Mentor Recommendation System - Main Application
 *
 * @author Mentor System
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.mentor.mapper")
@EnableCaching
@EnableAsync
@EnableScheduling
public class MentorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MentorApplication.class, args);
        System.out.println("===========================================");
        System.out.println("Mentor Recommendation System Started Successfully!");
        System.out.println("API Documentation: http://localhost:7020/api/doc.html");
        System.out.println("===========================================");
    }
}
