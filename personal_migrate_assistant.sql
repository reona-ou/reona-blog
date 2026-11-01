-- 站点助手配置表：名称、接口、API Key、模型、生成参数和可编辑 Prompt
-- 已有数据库执行一次；默认配置行会在后端首次读取时自动创建。

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
