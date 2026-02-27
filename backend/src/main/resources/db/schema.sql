-- ============================================
-- MENTOR SYSTEM DATABASE SCHEMA
-- Database: mentor_system
-- ============================================

CREATE DATABASE IF NOT EXISTS `mentor_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `mentor_system`;

-- ============================================
-- 1. USERS TABLE (Authentication)
-- ============================================
CREATE TABLE `users` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
  `salt` VARCHAR(100) COMMENT '盐值',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
  `phone` VARCHAR(50) COMMENT '手机号',
  `user_type` VARCHAR(20) NOT NULL COMMENT '用户类型: student, mentor, admin',
  `status` TINYINT DEFAULT 1 COMMENT '状态 (0=禁用, 1=启用)',
  `last_login_time` DATETIME COMMENT '最后登录时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  INDEX `idx_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. MENTORS TABLE
-- ============================================
CREATE TABLE `mentors` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL COMMENT '关联用户ID (from users table)',
  `name` VARCHAR(100) NOT NULL COMMENT '导师姓名',
  `title` VARCHAR(100) COMMENT '职称 (Professor, Associate Professor, etc.)',
  `institution` VARCHAR(200) COMMENT '所属机构',
  `department` VARCHAR(200) COMMENT '院系',
  `email` VARCHAR(100) COMMENT '邮箱',
  `phone` VARCHAR(50) COMMENT '电话',
  `office_location` VARCHAR(200) COMMENT '办公室位置',
  `research_areas` JSON COMMENT '研究方向列表 ["AI", "Machine Learning"]',
  `keywords` JSON COMMENT '关键词列表',
  `bio` TEXT COMMENT '个人简介',
  `group_direction` TEXT COMMENT '组内研究方向',
  `expected_student_qualities` TEXT COMMENT '期望学生素质',
  `mentoring_style` VARCHAR(500) COMMENT '指导风格',
  `available_positions` INT DEFAULT 5 COMMENT '可用名额',
  `funding_status` VARCHAR(200) COMMENT '经费状况',
  `collaboration_opportunities` TEXT COMMENT '合作机会',
  `education_background` TEXT COMMENT '教育背景',
  `avatar` VARCHAR(500) COMMENT '头像URL',
  `homepage_url` VARCHAR(500) COMMENT '个人主页',
  `google_scholar_url` VARCHAR(500) COMMENT 'Google Scholar链接',
  `accepting_students` BOOLEAN DEFAULT TRUE COMMENT '是否接收学生',
  `max_students` INT DEFAULT 5 COMMENT '最多接收学生数',
  `current_students` INT DEFAULT 0 COMMENT '当前学生数',
  `rating_avg` DECIMAL(3,2) DEFAULT 0.00 COMMENT '平均评分',
  `rating_count` INT DEFAULT 0 COMMENT '评分数量',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `status` TINYINT DEFAULT 1 COMMENT '状态 (0=禁用, 1=启用)',
  `is_verified` BOOLEAN DEFAULT FALSE COMMENT '是否认证',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_institution` (`institution`),
  INDEX `idx_rating` (`rating_avg`),
  INDEX `idx_status` (`status`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导师信息表';

-- ============================================
-- 3. STUDENTS TABLE
-- ============================================
CREATE TABLE `students` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL COMMENT '关联用户ID',
  `name` VARCHAR(100) NOT NULL COMMENT '学生姓名',
  `email` VARCHAR(100) COMMENT '邮箱',
  `phone` VARCHAR(50) COMMENT '电话',
  `current_institution` VARCHAR(200) COMMENT '当前学校',
  `major` VARCHAR(100) COMMENT '专业',
  `degree_level` VARCHAR(50) COMMENT '学位级别 (Bachelor, Master, PhD)',
  `graduation_year` INT COMMENT '毕业年份',
  `gpa` DECIMAL(3,2) COMMENT 'GPA',
  `research_interests` JSON COMMENT '研究兴趣列表',
  `keywords` JSON COMMENT '关键词列表',
  `bio` TEXT COMMENT '个人简介',
  `personal_abilities` TEXT COMMENT '个人能力描述',
  `expected_research_direction` TEXT COMMENT '期望研究方向',
  `preferred_mentor_style` VARCHAR(500) COMMENT '期望导师风格',
  `available_time` VARCHAR(200) COMMENT '可用时间',
  `programming_skills` JSON COMMENT '编程技能',
  `publications_count` INT DEFAULT 0 COMMENT '发表论文数',
  `project_experience` TEXT COMMENT '项目经验',
  `cv_url` VARCHAR(500) COMMENT '简历URL',
  `avatar` VARCHAR(500) COMMENT '头像URL',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_id` (`user_id`),
  INDEX `idx_institution` (`current_institution`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生信息表';

-- ============================================
-- 4. PUBLICATIONS TABLE (Scholar Works)
-- ============================================
CREATE TABLE `publications` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `mentor_id` INT NOT NULL COMMENT '导师ID',
  `title` VARCHAR(500) NOT NULL COMMENT '论文标题',
  `authors` TEXT COMMENT '作者列表',
  `venue` VARCHAR(200) COMMENT '发表会议/期刊',
  `year` INT COMMENT '发表年份',
  `abstract` TEXT COMMENT '摘要',
  `keywords` JSON COMMENT '关键词',
  `doi` VARCHAR(200) COMMENT 'DOI',
  `pdf_url` VARCHAR(500) COMMENT 'PDF链接',
  `citation_count` INT DEFAULT 0 COMMENT '引用次数',
  `publication_type` VARCHAR(50) COMMENT '类型 (Journal, Conference, Workshop)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_mentor_id` (`mentor_id`),
  INDEX `idx_year` (`year`),
  FOREIGN KEY (`mentor_id`) REFERENCES `mentors`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学者作品/论文表';

-- ============================================
-- 5. APPLICATIONS TABLE (Matching Workflow)
-- ============================================
CREATE TABLE `applications` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `student_id` INT NOT NULL COMMENT '学生ID',
  `mentor_id` INT NOT NULL COMMENT '导师ID',
  `status` VARCHAR(50) DEFAULT 'pending' COMMENT '状态: pending, accepted, rejected, withdrawn',
  `application_letter` TEXT COMMENT '申请信',
  `research_proposal` TEXT COMMENT '研究计划',
  `student_message` TEXT COMMENT '学生留言',
  `mentor_feedback` TEXT COMMENT '导师反馈',
  `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `response_time` DATETIME COMMENT '回复时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_student_id` (`student_id`),
  INDEX `idx_mentor_id` (`mentor_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_apply_time` (`apply_time`),
  FOREIGN KEY (`student_id`) REFERENCES `students`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`mentor_id`) REFERENCES `mentors`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申请匹配表';

-- ============================================
-- 6. RATINGS TABLE
-- ============================================
CREATE TABLE `ratings` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `mentor_id` INT NOT NULL COMMENT '导师ID',
  `student_id` INT NOT NULL COMMENT '学生ID',
  `rating` TINYINT NOT NULL COMMENT '评分 (1-5)',
  `comment` TEXT COMMENT '评价内容',
  `aspects` JSON COMMENT '各方面评分 {"guidance": 5, "communication": 4, "resources": 5}',
  `is_anonymous` BOOLEAN DEFAULT FALSE COMMENT '是否匿名',
  `is_verified` BOOLEAN DEFAULT FALSE COMMENT '是否认证评价',
  `helpful_count` INT DEFAULT 0 COMMENT '有用数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_mentor_id` (`mentor_id`),
  INDEX `idx_student_id` (`student_id`),
  INDEX `idx_rating` (`rating`),
  FOREIGN KEY (`mentor_id`) REFERENCES `mentors`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`student_id`) REFERENCES `students`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导师评分表';

-- ============================================
-- 7. BROWSING HISTORY TABLE (For Recommendations)
-- ============================================
CREATE TABLE `browsing_history` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL COMMENT '用户ID',
  `user_type` VARCHAR(20) NOT NULL COMMENT '用户类型: student, mentor',
  `target_type` VARCHAR(50) NOT NULL COMMENT '目标类型: mentor, publication, application',
  `target_id` INT NOT NULL COMMENT '目标ID',
  `action_type` VARCHAR(50) NOT NULL COMMENT '行为类型: view, search, apply, rate',
  `duration_seconds` INT COMMENT '停留时长(秒)',
  `additional_data` JSON COMMENT '额外数据',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_target` (`target_type`, `target_id`),
  INDEX `idx_action` (`action_type`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览历史表';

-- ============================================
-- 8. USER PREFERENCES TABLE (For LLM Analysis)
-- ============================================
CREATE TABLE `user_preferences` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL COMMENT '用户ID',
  `user_type` VARCHAR(20) NOT NULL COMMENT '用户类型',
  `preference_text` TEXT COMMENT 'LLM生成的偏好描述',
  `preference_keywords` JSON COMMENT '偏好关键词',
  `preference_topics` JSON COMMENT '偏好主题',
  `last_analyzed_log_count` INT DEFAULT 0 COMMENT '上次分析时的日志数',
  `current_log_count` INT DEFAULT 0 COMMENT '当前日志数',
  `analysis_count` INT DEFAULT 0 COMMENT '分析次数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_id_type` (`user_id`, `user_type`),
  INDEX `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好分析表';

-- ============================================
-- 9. CHAT MESSAGES TABLE
-- ============================================
CREATE TABLE `chat_messages` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `application_id` INT NOT NULL COMMENT '关联申请ID',
  `sender_id` INT NOT NULL COMMENT '发送者ID',
  `sender_type` VARCHAR(20) NOT NULL COMMENT '发送者类型: student, mentor',
  `message_type` VARCHAR(50) DEFAULT 'text' COMMENT '消息类型: text, file, image',
  `content` TEXT COMMENT '消息内容',
  `file_url` VARCHAR(500) COMMENT '文件URL',
  `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_application_id` (`application_id`),
  INDEX `idx_sender` (`sender_id`, `sender_type`),
  INDEX `idx_create_time` (`create_time`),
  FOREIGN KEY (`application_id`) REFERENCES `applications`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ============================================
-- 10. ROLES TABLE (Shiro RBAC)
-- ============================================
CREATE TABLE `roles` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名',
  `description` VARCHAR(200) COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ============================================
-- 11. PERMISSIONS TABLE (Shiro RBAC)
-- ============================================
CREATE TABLE `permissions` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名',
  `resource` VARCHAR(200) COMMENT '资源',
  `action` VARCHAR(50) COMMENT '操作',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_permission` (`permission_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ============================================
-- 12. USER_ROLES TABLE (Shiro RBAC)
-- ============================================
CREATE TABLE `user_roles` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `role_id` INT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ============================================
-- 13. ROLE_PERMISSIONS TABLE (Shiro RBAC)
-- ============================================
CREATE TABLE `role_permissions` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `role_id` INT NOT NULL,
  `permission_id` INT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`permission_id`) REFERENCES `permissions`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================================
-- INSERT DEFAULT DATA
-- ============================================

-- Insert default roles
INSERT INTO `roles` (`role_name`, `description`) VALUES
('ADMIN', '系统管理员'),
('MENTOR', '导师'),
('STUDENT', '学生');

-- Insert default permissions
INSERT INTO `permissions` (`permission_name`, `resource`, `action`) VALUES
('mentor:view', 'mentor', 'view'),
('mentor:edit', 'mentor', 'edit'),
('mentor:delete', 'mentor', 'delete'),
('student:view', 'student', 'view'),
('student:edit', 'student', 'edit'),
('application:create', 'application', 'create'),
('application:view', 'application', 'view'),
('application:respond', 'application', 'respond'),
('rating:create', 'rating', 'create'),
('admin:all', 'admin', 'all');

-- Assign permissions to roles
-- ADMIN role gets all permissions
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r, `permissions` p WHERE r.role_name = 'ADMIN';

-- MENTOR role permissions
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r, `permissions` p
WHERE r.role_name = 'MENTOR' AND p.permission_name IN ('mentor:view', 'mentor:edit', 'student:view', 'application:view', 'application:respond');

-- STUDENT role permissions
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r, `permissions` p
WHERE r.role_name = 'STUDENT' AND p.permission_name IN ('mentor:view', 'student:view', 'student:edit', 'application:create', 'application:view', 'rating:create');

-- ============================================
-- 14. VECTOR SYNC STATUS TABLE (For Milvus)
-- ============================================
CREATE TABLE `vector_sync_status` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `entity_type` VARCHAR(20) NOT NULL COMMENT '实体类型: mentor, student',
  `entity_id` INT NOT NULL COMMENT '实体ID',
  `vector_id` VARCHAR(100) COMMENT 'Milvus中的向量ID',
  `last_sync_time` DATETIME COMMENT '最后同步时间',
  `content_hash` VARCHAR(64) COMMENT '内容哈希，用于检测变化',
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态: synced, pending, failed',
  `error_message` TEXT COMMENT '错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_entity` (`entity_type`, `entity_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_last_sync_time` (`last_sync_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='向量同步状态表';
