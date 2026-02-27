-- 学者表
CREATE TABLE IF NOT EXISTS `scholars` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '学者ID',
  `name` VARCHAR(100) NOT NULL COMMENT '姓名',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `institution` VARCHAR(200) DEFAULT NULL COMMENT '所属机构',
  `bio` TEXT COMMENT '个人简介',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `homepage` VARCHAR(255) DEFAULT NULL COMMENT '个人主页',
  `h_index` INT DEFAULT 0 COMMENT 'H指数',
  `citation_count` INT DEFAULT 0 COMMENT '总引用数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_institution` (`institution`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学者表';

-- 论文-作者关系表
CREATE TABLE IF NOT EXISTS `publication_authors` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `publication_id` INT NOT NULL COMMENT '论文ID',
  `scholar_id` INT NOT NULL COMMENT '学者ID',
  `author_order` INT DEFAULT 1 COMMENT '作者顺序(1=第一作者)',
  `is_corresponding` TINYINT(1) DEFAULT 0 COMMENT '是否通讯作者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pub_scholar` (`publication_id`, `scholar_id`),
  KEY `idx_publication_id` (`publication_id`),
  KEY `idx_scholar_id` (`scholar_id`),
  CONSTRAINT `fk_pub_authors_publication` FOREIGN KEY (`publication_id`) REFERENCES `publications` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pub_authors_scholar` FOREIGN KEY (`scholar_id`) REFERENCES `scholars` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论文-作者关系表';

-- 学者-导师关联表（可选，用于将系统中的导师关联到学者）
CREATE TABLE IF NOT EXISTS `mentor_scholars` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `mentor_id` INT NOT NULL COMMENT '导师ID',
  `scholar_id` INT NOT NULL COMMENT '学者ID',
  `verified` TINYINT(1) DEFAULT 0 COMMENT '是否已验证',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mentor_scholar` (`mentor_id`, `scholar_id`),
  KEY `idx_mentor_id` (`mentor_id`),
  KEY `idx_scholar_id` (`scholar_id`),
  CONSTRAINT `fk_mentor_scholars_mentor` FOREIGN KEY (`mentor_id`) REFERENCES `mentors` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_mentor_scholars_scholar` FOREIGN KEY (`scholar_id`) REFERENCES `scholars` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导师-学者关联表';
