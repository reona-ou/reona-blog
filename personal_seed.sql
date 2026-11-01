-- 个人博客：默认站点设置、助手和互动表
-- 不创建任何默认账号；唯一超级管理员由 OOBE 创建，普通管理员由主人在用户管理中提升已有用户
-- 在 reona_blog 数据库执行一次即可

CREATE TABLE IF NOT EXISTS `sys_site_setting` (
  `id` int NOT NULL,
  `site_name` varchar(64) NOT NULL,
  `email` varchar(128) NOT NULL,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `sys_site_setting` (`id`, `site_name`, `email`)
VALUES (1, 'Default Blog', 'default@example.com');

CREATE TABLE IF NOT EXISTS `sys_assistant_setting` (
  `id` int NOT NULL,
  `assistant_name` varchar(32) NOT NULL DEFAULT 'Default Assistant',
  `api_url` varchar(255) NOT NULL DEFAULT 'http://127.0.0.1:8080/v1/chat/completions',
  `api_key` varchar(255) DEFAULT '',
  `model` varchar(128) NOT NULL DEFAULT 'qwen3.5-4b',
  `temperature` decimal(3,2) NOT NULL DEFAULT '0.60',
  `max_tokens` int NOT NULL DEFAULT '420',
  `persona_prompt` text,
  `voice_zh` text,
  `voice_en` text,
  `voice_ja` text,
  `example_zh` text,
  `example_en` text,
  `example_ja` text,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_comment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `article_id` int NOT NULL,
  `parent_id` int NOT NULL DEFAULT '0',
  `user_id` int NOT NULL,
  `username` varchar(64) DEFAULT NULL,
  `nickname` varchar(64) DEFAULT NULL,
  `content` varchar(500) NOT NULL,
  `is_delete` tinyint(1) DEFAULT '0',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_article` (`article_id`),
  KEY `idx_comment_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_article_like` (
  `id` int NOT NULL AUTO_INCREMENT,
  `article_id` int NOT NULL,
  `user_id` int NOT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_like_user` (`article_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_comment_like` (
  `id` int NOT NULL AUTO_INCREMENT,
  `comment_id` int NOT NULL,
  `user_id` int NOT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_like_user` (`comment_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 示例文章由首次运行的 OOBE 在“示例内容”步骤按需创建；这里不再预置。
