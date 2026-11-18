-- 旧版单日历迁移：仅保存一条私密 iCal 地址（id = 1）
-- 历史环境升级到多日历时，请继续执行 personal_migrate_calendar_multi.sql。

CREATE TABLE IF NOT EXISTS `sys_calendar_setting` (
  `id` int NOT NULL,
  `ics_url` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
