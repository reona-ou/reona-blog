-- 互动功能迁移：为旧库增加评论回复与点赞

ALTER TABLE `sys_comment`
  ADD COLUMN `parent_id` int NOT NULL DEFAULT '0' AFTER `article_id`;

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

UPDATE `sys_article`
SET `content` = '这里是我的个人博客，用来记录日常、想法和一些还没想明白的事。\n\n![书](/blog/books.png)\n\n### 关于评论\n每篇文章下面都可以留言。登录之后就能评论，也可以回复和点赞。\n\n### 关于联系\n页面底部有邮箱。这里没有客服排班，看到就会回。\n\n> 这是一个可以慢慢更新的地方。'
WHERE `id` = 1001;
