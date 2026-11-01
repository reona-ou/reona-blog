-- 已有站点助手表：增加中/英/日三套语言语气与示例字段
ALTER TABLE `sys_assistant_setting`
  ADD COLUMN `voice_zh` text NULL,
  ADD COLUMN `voice_en` text NULL,
  ADD COLUMN `voice_ja` text NULL,
  ADD COLUMN `example_zh` text NULL,
  ADD COLUMN `example_en` text NULL,
  ADD COLUMN `example_ja` text NULL;
