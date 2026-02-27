-- ============================================
-- Migration: Add student preference fields
-- ============================================

-- Add preference fields to students table
ALTER TABLE `students` 
ADD COLUMN `personal_abilities` TEXT COMMENT '个人能力描述' AFTER `bio`,
ADD COLUMN `expected_research_direction` TEXT COMMENT '期望研究方向' AFTER `personal_abilities`,
ADD COLUMN `preferred_mentor_style` VARCHAR(500) COMMENT '期望导师风格' AFTER `expected_research_direction`,
ADD COLUMN `available_time` VARCHAR(200) COMMENT '可用时间' AFTER `preferred_mentor_style`,
ADD COLUMN `programming_skills` JSON COMMENT '编程技能' AFTER `available_time`,
ADD COLUMN `publications_count` INT DEFAULT 0 COMMENT '发表论文数' AFTER `programming_skills`,
ADD COLUMN `project_experience` TEXT COMMENT '项目经验' AFTER `publications_count`;

-- Add indexes for better search performance
ALTER TABLE `students`
ADD INDEX `idx_research_interests` ((CAST(research_interests AS CHAR(255))));
