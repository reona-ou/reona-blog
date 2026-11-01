-- 角色调整：把博客主人设为唯一超级管理员。
-- 把 owner@example.com 换成你的邮箱后执行；已有超级管理员会先被降为普通管理员。
-- 新库请优先执行 personal_migrate_single_super_admin.sql（包含唯一约束）。

UPDATE `sys_user`
SET `role` = 'ROLE_ADMIN'
WHERE `role` = 'ROLE_SUPER_ADMIN' AND `email` <> 'owner@example.com';

UPDATE `sys_user`
SET `role` = 'ROLE_SUPER_ADMIN'
WHERE `email` = 'owner@example.com';

-- 普通管理员保持 ROLE_ADMIN；注册用户保持 ROLE_USER。
