# Reona Blog

[简体中文](README.md) | [English](README.en.md) | [日本語](README.ja.md)

基于 Vue 3、Spring Boot 和 MySQL 的个人博客，包含文章、评论与回复、文章/评论点赞、后台管理、三语界面、可配置的 AI 站点助手和只读 Google 日历视图。

## 核心特性

- **无默认账号**：首次部署由 OOBE 创建唯一的超级管理员（博客主人），仓库里不含任何账号、密码或示例凭据。
- **三语界面**：中文 / English / 日本語，前台与后台全部覆盖，语言选择按用户持久化。
- **AI 站点助手**：兼容 OpenAI Chat Completions 接口，公开访问、按客户端 IP 限流、流式输出，名称 / 接口 / 模型 / Prompt / 语气均可在后台配置。
- **Google 日历**：超级管理员在后台粘贴日历私密 iCal 地址，前台以只读月历查看日程，仅超级管理员可见。
- **权限分层**：超级管理员唯一（数据库唯一键约束），普通管理员由主人提升已有用户产生；站点设置与助手设置仅超级管理员可改。

---

## 技术栈

- 前端：Vue 3、Vite、Pinia、Vue Router、Vue I18n、Element Plus、marked、DOMPurify
- 后端：Spring Boot 4.1.1、MyBatis-Plus、JWT、BCrypt、Hutool
- 数据库：MySQL 8
- AI：任意兼容 OpenAI Chat Completions 的服务，默认连接本地 Qwen

## 目录结构

| 路径 | 说明 |
| --- | --- |
| `web/` | Vue 3 前台和管理后台 |
| `BlogApi/` | Spring Boot 后端 |
| `database_schema.sql` | 空数据库表结构（博客相关 9 张表） |
| `personal_seed.sql` | 默认站点设置和互动表；账号由 OOBE 创建，示例文章按需创建 |
| `personal_migrate_*.sql` | 已有数据库的迁移脚本 |
| `README.md` / `README.en.md` / `README.ja.md` | 三语说明 |

## 功能清单

- 文章：发布、草稿、Markdown 编辑、标签筛选、搜索。
- 评论：登录后评论、回复、删除自己的评论；管理员可删除任意评论。
- 点赞：文章和评论点赞/取消。
- 用户与权限：唯一超级管理员（博客主人）；普通管理员可选，由主人在「用户管理」里提升已有用户；管理员可管理普通用户，超级管理员负责管理员权限。站点设置和助手配置仅超级管理员可修改。
- 前台作者入口：管理员和超级管理员登录后，在首页/博客列表看到「写文章」，文章页看到「编辑文章」；超级管理员默认登录后回到前台。
- 账户设置：登录用户可修改昵称、邮箱、电话、地址和密码；管理员可为可管理的用户重置密码。
- 三语界面：中文 / English / 日本語，语言选择会记住。
- 站点设置：站点名称、联系邮箱；助手设置：名称、接口、模型、Prompt、统一语气。两项仅超级管理员可修改。
- AI 助手：公开访问，按客户端 IP 限流，流式输出，默认连接本地 Qwen。
- Google 日历：超级管理员可在后台粘贴日历私密 iCal 地址，以只读月历查看日程；仅超级管理员可见。
- 文件上传：仅管理员，支持 jpg/jpeg/png/gif/webp，最大 10MB。
- 首次初始化（OOBE）：无超级管理员时自动进入 `/setup`，创建站点、主人账号，可选配置助手和示例文章；完成后自动登录，并清理历史遗留的 defaultadmin 测试账号。

---

## 环境要求

- JDK 25
- Maven 3.6+
- Node.js 20.19+ / 22.12+
- MySQL 8

## 本地运行

1. 创建数据库 `reona_blog`，导入 `database_schema.sql` 后再执行 `personal_seed.sql`。已有数据库执行 `personal_migrate_*.sql`。

   > `database_schema.sql` 包含 `DROP TABLE`，只能用于空库初始化。

2. 启动后端：

   ```bash
   cd BlogApi
   mvn -DskipTests package
   java -jar target/reona-blog-0.0.1-SNAPSHOT.jar
   ```

   本机数据库密码和上传目录放在 `BlogApi/application-local.yml`（已加入 `.gitignore`，不会打进 jar）：

   ```yaml
   spring:
     datasource:
       password: 本机 MySQL 密码
   files:
     upload:
       path: ./data/upload/
   ```

3. 启动前台：

   ```bash
   cd web
   npm install
   npm run dev
   ```

   访问 `http://localhost:5173`。

## 生产部署

1. 构建：`cd BlogApi && mvn -DskipTests package`，`cd web && npm run build`，产物为 `web/dist/`。
2. 静态资源交给 Nginx，`/api/` 反向代理到后端；后端以 systemd 常驻，环境变量写入 `/etc/reona-blog.env`：

   ```nginx
   server {
       listen 8088;
       server_name _;
       root /opt/reona-blog/web;
       index index.html;
       client_max_body_size 12m;

       location /api/ {
           proxy_pass http://127.0.0.1:8090/;
           proxy_http_version 1.1;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
           proxy_buffering off;
           proxy_cache off;
           proxy_read_timeout 300s;
           proxy_send_timeout 300s;
       }

       location /assets/ {
           expires 30d;
           add_header Cache-Control "public, immutable";
           try_files $uri =404;
       }

       location / {
           try_files $uri $uri/ /index.html;
       }
   }
   ```

   ```ini
   # /etc/reona-blog.env
   DB_URL=jdbc:mysql://127.0.0.1:3306/reona_blog?characterEncoding=utf8&useUnicode=true&connectionTimeZone=Asia%2FTokyo&useSSL=false&allowPublicKeyRetrieval=true
   DB_USERNAME=reona_blog
   DB_PASSWORD=<数据库密码>
   UPLOAD_PATH=/mnt/app-data/reona-blog/upload/
   CORS_ALLOWED_ORIGINS=https://your-domain
   TRUSTED_PROXIES=127.0.0.1,::1
   ```

   ```bash
   java -Dserver.port=8090 -jar /opt/reona-blog/reona-blog.jar
   ```

3. 首次打开站点完成 OOBE。初始化接口只接受本机/私网请求；若站点已通过公网域名暴露，请先在内网完成初始化。

## 首次初始化与超级管理员

已有数据库且已有超级管理员时不会显示 OOBE。若需要调整主人账号，执行 `personal_migrate_single_super_admin.sql`（把 `owner@example.com` 换成你的邮箱）或手动执行：

```sql
UPDATE sys_user SET role = 'ROLE_ADMIN' WHERE role = 'ROLE_SUPER_ADMIN' AND email <> '你的邮箱';
UPDATE sys_user SET role = 'ROLE_SUPER_ADMIN' WHERE email = '你的邮箱';
```

数据库通过唯一键保证最多只有一个超级管理员。管理员可在「用户管理」里重置普通用户的密码。

## AI 助手（可选）

启动兼容 OpenAI Chat Completions 的本地服务，默认地址为 `http://127.0.0.1:8080/v1/chat/completions`，模型名为 `qwen3.5-4b`；也可以在后台「助手设置」中修改。

---

## 环境变量

| 变量 | 说明 | 默认 |
| --- | --- | --- |
| `DB_URL` | MySQL JDBC 连接串 | 本机 `reona_blog` 库，`connectionTimeZone=Asia/Tokyo` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | 无；建议用本地配置或环境变量 |
| `UPLOAD_PATH` | 上传文件目录 | `./data/upload/` |
| `CORS_ALLOWED_ORIGINS` | 允许的前端来源，逗号分隔 | `http://localhost:5173,http://127.0.0.1:5173` |
| `TRUSTED_PROXIES` | 可信反向代理地址，用于读取客户端 IP | 本机回环地址 |

## 默认内容

- 站点名称、联系邮箱、主人账号：由 OOBE 设置。
- 示例文章：OOBE 勾选「示例内容」时创建 `Default Article 1` / `Default Article 2` / `Default Article 3`；不勾选则保持空白博客。
- 助手名称：`Default Assistant`。

文章在 `/admin/articles` 修改；站点设置和助手设置仅超级管理员可在 `/admin/settings`、`/admin/assistant` 修改。上线前请按需调整 CORS 和数据库密码。

---

[English](README.en.md) | [日本語](README.ja.md)
