-- 只保留博客主人一个超级管理员，并为用户表补齐唯一约束。
-- 把 owner@example.com 换成博客主人邮箱后执行一次；已有数据库请先备份。
-- 执行后：其他超级管理员会被降为普通管理员，owner 邮箱的账号会成为唯一超级管理员。

SET @owner_email = 'owner@example.com';

UPDATE `sys_user`
SET `role` = 'ROLE_ADMIN'
WHERE `role` = 'ROLE_SUPER_ADMIN' AND `email` <> @owner_email;

UPDATE `sys_user`
SET `role` = 'ROLE_SUPER_ADMIN'
WHERE `email` = @owner_email;

ALTER TABLE `sys_user`
  ADD COLUMN `super_admin_key` tinyint GENERATED ALWAYS AS (if((`role` = 'ROLE_SUPER_ADMIN'), 1, NULL)) STORED,
  ADD UNIQUE KEY `uk_user_username` (`username`),
  ADD UNIQUE KEY `uk_user_email` (`email`),
  ADD UNIQUE KEY `uk_user_super_admin` (`super_admin_key`);

ALTER TABLE `sys_comment`
  ADD KEY `idx_comment_parent` (`parent_id`);

ALTER TABLE `sys_file`
  ADD KEY `idx_file_md5` (`md5`);
