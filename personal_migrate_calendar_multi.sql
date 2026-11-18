-- 多日历支持：sys_calendar_setting 从单条记录改为多条记录
-- 已有数据库执行一次；执行前建议先备份该表。

ALTER TABLE `sys_calendar_setting`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  ADD COLUMN `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Google 日历' AFTER `id`,
  ADD COLUMN `color` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#2f4a3a' AFTER `name`,
  ADD COLUMN `visible` tinyint(1) NOT NULL DEFAULT '1' AFTER `ics_url`;
