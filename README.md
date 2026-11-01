<a id="top"></a>
# Reona Blog

个人博客 / 個人ブログ / Personal blog

[中文](#zh) · [日本語](#ja) · [English](#en)

---

<a id="zh"></a>
## 中文

基于 Vue 3、Spring Boot 和 MySQL 的个人博客，包含文章、评论与回复、文章/评论点赞、后台管理、三语界面和可配置的 AI 站点助手。

### 默认账号

没有任何默认账号。首次部署打开站点时，由 OOBE 创建唯一的超级管理员（博客主人）。普通管理员是可选的，由主人登录后在「用户管理」里把已有用户提升为管理员。

### 技术栈

- 前端：Vue 3、Vite、Pinia、Vue Router、Vue I18n、Element Plus、marked、DOMPurify
- 后端：Spring Boot 4.1.1、MyBatis-Plus、JWT、BCrypt、Hutool
- 数据库：MySQL 8
- AI：兼容 OpenAI Chat Completions 的服务

### 目录

- `web/`：Vue 3 前台和管理后台
- `BlogApi/`：Spring Boot 后端
- `database_schema.sql`：空数据库表结构（博客相关 8 张表）
- `personal_seed.sql`：默认站点设置和互动表；账号由 OOBE 创建，示例文章由 OOBE 按需创建
- `personal_migrate_*.sql`：已有数据库的迁移脚本
- `README.md`：中文 / 日本語 / English 说明

### 功能

- 文章：发布、草稿、Markdown 编辑、标签筛选、搜索。
- 评论：登录后评论、回复、删除自己的评论；管理员可删除任意评论。
- 点赞：文章和评论点赞/取消。
- 用户与权限：唯一超级管理员（博客主人）；普通管理员可选，由主人在「用户管理」里提升已有用户；管理员可管理普通用户，超级管理员负责管理员权限。站点设置和助手配置仅超级管理员可修改。
- 前台作者入口：管理员和超级管理员登录后，在首页/博客列表看到「写文章」，文章页看到「编辑文章」；超级管理员默认登录后回到前台。
- 账户设置：登录用户可修改昵称、邮箱、电话、地址和密码；管理员可为可管理的用户重置密码。
- 三语界面：中文 / 日本語 / English，语言选择会记住。
- 站点设置：站点名称、联系邮箱；助手设置：名称、接口、模型、Prompt、统一语气。两项仅超级管理员可修改。
- AI 助手：公开访问，按客户端 IP 限流，流式输出，默认连接本地 Qwen。
- 文件上传：仅管理员，支持 jpg/jpeg/png/gif/webp，最大 10MB。
- 首次初始化（OOBE）：无超级管理员时自动进入 `/setup`，创建站点、主人账号，可选配置助手和示例文章；完成后自动登录，并清理历史遗留的 defaultadmin 测试账号。

### 环境要求

- JDK 25
- Maven 3.6+
- Node.js 20.19+ / 22.12+
- MySQL 8

### 本地运行

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

4. 首次初始化：打开站点（建议从本机或局域网访问），没有超级管理员时会自动进入 `/setup`。填写站点信息、主人账号，按需配置助手和示例文章；初始化完成后自动登录。普通管理员是可选的，由主人在「用户管理」里把已有用户提升为管理员。初始化接口只接受本机/私网请求；如果站点已经通过公网域名暴露，请先在内网完成初始化。

   已有数据库且已有超级管理员时不会显示 OOBE。若需要调整主人账号，执行 `personal_migrate_single_super_admin.sql`（把 `owner@example.com` 换成你的邮箱）或手动执行：

   ```sql
   UPDATE sys_user SET role = 'ROLE_ADMIN' WHERE role = 'ROLE_SUPER_ADMIN' AND email <> '你的邮箱';
   UPDATE sys_user SET role = 'ROLE_SUPER_ADMIN' WHERE email = '你的邮箱';
   ```

   数据库通过唯一键保证最多只有一个超级管理员。管理员可在「用户管理」里重置普通用户的密码。

5. AI 助手（可选）：启动兼容 OpenAI Chat Completions 的本地服务，默认地址为 `http://127.0.0.1:8080/v1/chat/completions`，模型名为 `qwen3.5-4b`；也可以在后台「助手设置」中修改。

### 环境变量

| 变量 | 说明 | 默认 |
| --- | --- | --- |
| `DB_URL` | MySQL JDBC 连接串 | 本机 `reona_blog` 库，`connectionTimeZone=Asia/Tokyo` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | 无；建议用本地配置或环境变量 |
| `UPLOAD_PATH` | 上传文件目录 | `./data/upload/` |
| `CORS_ALLOWED_ORIGINS` | 允许的前端来源，逗号分隔 | `http://localhost:5173,http://127.0.0.1:5173` |
| `TRUSTED_PROXIES` | 可信反向代理地址，用于读取客户端 IP | 本机回环地址 |

### 默认内容

- 站点名称、联系邮箱、主人账号：由 OOBE 设置。
- 示例文章：OOBE 勾选「示例内容」时创建 `Default Article 1` / `Default Article 2` / `Default Article 3`；不勾选则保持空白博客。
- 助手名称：`Default Assistant`。

文章在 `/admin/articles` 修改；站点设置和助手设置仅超级管理员可在 `/admin/settings`、`/admin/assistant` 修改。上线前请按需调整 CORS 和数据库密码。

---

<a id="ja"></a>
## 日本語

Vue 3、Spring Boot、MySQL で作った個人ブログです。記事、コメントと返信、記事・コメントのいいね、管理画面、3 言語 UI、設定可能な AI サイトアシスタントを備えています。

### 既定アカウント

既定のアカウントはありません。初回デプロイ時に OOBE で唯一のスーパー管理者（ブログのオーナー）を作成します。一般管理者は任意で、オーナーが「ユーザー管理」で既存ユーザーを管理者に昇格して作成します。

### 技術スタック

- フロントエンド：Vue 3、Vite、Pinia、Vue Router、Vue I18n、Element Plus、marked、DOMPurify
- バックエンド：Spring Boot 4.1.1、MyBatis-Plus、JWT、BCrypt、Hutool
- データベース：MySQL 8
- AI：OpenAI Chat Completions 互換サービス。既定ではローカル Qwen に接続

### ディレクトリ

- `web/`：Vue 3 のフロントエンドと管理画面
- `BlogApi/`：Spring Boot バックエンド
- `database_schema.sql`：空データベース用のテーブル定義（ブログ関連 8 テーブル）
- `personal_seed.sql`：既定のサイト設定とインタラクション用テーブル。アカウントは OOBE で作成し、サンプル記事は必要に応じて作成
- `personal_migrate_*.sql`：既存データベース用の移行スクリプト
- `README.md`：中文 / 日本語 / English の説明

### 機能

- 記事：公開、下書き、Markdown 編集、ラベル絞り込み、検索。
- コメント：ログイン後に投稿・返信・自分のコメント削除。管理者は任意のコメントを削除可能。
- いいね：記事とコメントのいいね/解除。
- ユーザーと権限：スーパー管理者は 1 人だけ（ブログのオーナー）。一般管理者は任意で、オーナーが「ユーザー管理」で既存ユーザーを昇格します。管理者は一般ユーザーを管理できます。サイト設定とアシスタント設定はスーパー管理者のみ変更できます。
- フロントの執筆入口：管理者/スーパー管理者でログインすると、ホームとブログ一覧に「記事を書く」、記事ページに「記事を編集」が表示されます。スーパー管理者はログイン後にフロントへ戻ります。
- アカウント設定：ログインユーザーは表示名、メール、電話、住所、パスワードを変更できます。管理者は管理対象ユーザーのパスワードを再設定できます。
- 3 言語 UI：中文 / 日本語 / English。選択言語は保存されます。
- サイト設定：サイト名、連絡先メール。アシスタント設定：名前、API、モデル、Prompt、統一した口調。どちらもスーパー管理者のみ変更できます。
- AI アシスタント：公開。クライアント IP 単位で制限。ストリーミング出力。既定ではローカル Qwen に接続。
- ファイルアップロード：管理者のみ。jpg/jpeg/png/gif/webp、最大 10MB。
- 初回セットアップ（OOBE）：スーパー管理者がいない場合に `/setup` へ移動し、サイト、オーナーアカウント、任意のアシスタントとサンプル記事を設定します。完了後は自動ログインし、履歴として残っている defaultadmin テストアカウントは削除されます。

### 必要な環境

- JDK 25
- Maven 3.6+
- Node.js 20.19+ / 22.12+
- MySQL 8

### ローカル起動

1. データベース `reona_blog` を作成し、`database_schema.sql` を投入してから `personal_seed.sql` を実行します。既存 DB では `personal_migrate_*.sql` を実行してください。

   > `database_schema.sql` は `DROP TABLE` を含みます。空のデータベースでのみ使用してください。

2. バックエンドを起動：

   ```bash
   cd BlogApi
   mvn -DskipTests package
   java -jar target/reona-blog-0.0.1-SNAPSHOT.jar
   ```

   ローカルの DB パスワードとアップロード先は `BlogApi/application-local.yml` に置きます（`.gitignore` 済みで、jar には入りません）：

   ```yaml
   spring:
     datasource:
       password: ローカル MySQL のパスワード
   files:
     upload:
       path: ./data/upload/
   ```

3. フロントエンドを起動：

   ```bash
   cd web
   npm install
   npm run dev
   ```

   `http://localhost:5173` を開きます。

4. 初回セットアップ：サイトを開くと、スーパー管理者がいない場合は自動的に `/setup` に移動します（ローカルまたは LAN からのアクセスを推奨）。サイト情報、オーナーアカウント、必要に応じてアシスタントとサンプル記事を設定してください。完了後は自動ログインします。一般管理者は任意で、オーナーが「ユーザー管理」で既存ユーザーを昇格して作成します。初期化 API はローカル/プライベートネットワークからのみ利用できます。公開ドメインで既に公開している場合は、先に LAN 内で初期化してください。

   既にスーパー管理者がいる既存 DB では OOBE は表示されません。オーナーを変更する場合は `personal_migrate_single_super_admin.sql`（`owner@example.com` を自分のメールに置換）または手動で実行してください：

   ```sql
   UPDATE sys_user SET role = 'ROLE_ADMIN' WHERE role = 'ROLE_SUPER_ADMIN' AND email <> 'your@example.com';
   UPDATE sys_user SET role = 'ROLE_SUPER_ADMIN' WHERE email = 'your@example.com';
   ```

   データベースの一意キーにより、スーパー管理者は最大 1 人です。管理者は「ユーザー管理」から一般ユーザーのパスワードを再設定できます。

5. AI アシスタント（任意）：OpenAI Chat Completions 互換のローカルサービスを起動します。既定の URL は `http://127.0.0.1:8080/v1/chat/completions`、モデルは `qwen3.5-4b` です。管理画面の「アシスタント設定」でも変更できます。

### 環境変数

| 変数 | 説明 | 既定値 |
| --- | --- | --- |
| `DB_URL` | MySQL JDBC URL | ローカル `reona_blog`（`connectionTimeZone=Asia/Tokyo`） |
| `DB_USERNAME` | DB ユーザー名 | `root` |
| `DB_PASSWORD` | DB パスワード | なし。ローカル設定または環境変数推奨 |
| `UPLOAD_PATH` | アップロード先ディレクトリ | `./data/upload/` |
| `CORS_ALLOWED_ORIGINS` | 許可するフロントエンド Origin（カンマ区切り） | `http://localhost:5173,http://127.0.0.1:5173` |
| `TRUSTED_PROXIES` | クライアント IP を読む際に信頼するリバースプロキシ | ローカルループバック |

### 既定コンテンツ

- サイト名、連絡先メール、オーナーアカウント：OOBE で設定。
- サンプル記事：OOBE で「サンプルコンテンツ」を選ぶと `Default Article 1` / `Default Article 2` / `Default Article 3` を作成。選ばなければ空のブログになります。
- アシスタント名：`Default Assistant`。

記事は `/admin/articles`、サイト設定とアシスタント設定はスーパー管理者のみ `/admin/settings`、`/admin/assistant` から変更できます。公開前に CORS と DB パスワードを環境に合わせて設定してください。

---

<a id="en"></a>
## English

A personal blog built with Vue 3, Spring Boot and MySQL. It includes posts, comments and replies, post/comment likes, an admin area, a trilingual UI and a configurable AI site assistant.

### Default accounts

There are no default accounts. On first deployment, the OOBE creates the only super admin (the blog owner). Regular admins are optional; the owner promotes an existing user to admin from the Users page.

### Tech stack

- Frontend: Vue 3, Vite, Pinia, Vue Router, Vue I18n, Element Plus, marked, DOMPurify
- Backend: Spring Boot 4.1.1, MyBatis-Plus, JWT, BCrypt, Hutool
- Database: MySQL 8
- AI: any OpenAI Chat Completions-compatible service; local Qwen by default

### Layout

- `web/`: Vue 3 storefront and admin UI
- `BlogApi/`: Spring Boot backend
- `database_schema.sql`: schema for an empty database (8 blog-related tables)
- `personal_seed.sql`: default site settings and interaction tables; accounts are created by the OOBE and sample posts on demand
- `personal_migrate_*.sql`: migration scripts for existing databases
- `README.md`: Chinese / Japanese / English documentation

### Features

- Posts: publish, draft, Markdown editing, label filtering and search.
- Comments: logged-in users can comment, reply and delete their own comments; admins can delete any comment.
- Likes: like/unlike posts and comments.
- Users and roles: exactly one super admin (the blog owner). Regular admins are optional; the owner promotes an existing user from the Users page. Admins manage regular users. Site and assistant settings are super-admin only.
- Front-end authoring: admins and super admins see "Write a post" on the home/blog pages and "Edit post" on article pages. Super admins land on the storefront after login.
- Account settings: logged-in users can update their display name, email, phone, address and password; admins can reset passwords for users they manage.
- Trilingual UI: Chinese / Japanese / English; the selected language is remembered.
- Site settings: site name and contact email. Assistant settings: name, API, model, prompt and unified tone. Both are super-admin only.
- AI assistant: public, rate-limited by client IP, streamed responses; connects to local Qwen by default.
- File uploads: admins only; jpg/jpeg/png/gif/webp, up to 10MB.
- First-run setup (OOBE): when no super admin exists, the site opens `/setup` to create the site, owner account, and optional assistant and sample posts; it logs in automatically and cleans up a legacy defaultadmin test account if present.

### Requirements

- JDK 25
- Maven 3.6+
- Node.js 20.19+ / 22.12+
- MySQL 8

### Local setup

1. Create the `reona_blog` database, import `database_schema.sql`, then run `personal_seed.sql`. For an existing database, run `personal_migrate_*.sql` instead.

   > `database_schema.sql` contains `DROP TABLE` statements. Use it only for an empty database.

2. Start the backend:

   ```bash
   cd BlogApi
   mvn -DskipTests package
   java -jar target/reona-blog-0.0.1-SNAPSHOT.jar
   ```

   Put the local database password and upload directory in `BlogApi/application-local.yml` (ignored by Git and not packaged into the jar):

   ```yaml
   spring:
     datasource:
       password: your-local-mysql-password
   files:
     upload:
       path: ./data/upload/
   ```

3. Start the frontend:

   ```bash
   cd web
   npm install
   npm run dev
   ```

   Open `http://localhost:5173`.

4. First-run setup: open the site (localhost or LAN is recommended). When no super admin exists, the router opens `/setup`. Enter the site details and owner account, then optionally configure the assistant and sample posts. Setup logs you in; regular admins are optional and are promoted from existing users in the Users page. The initialization API only accepts local/private-network requests; if the site is already exposed on a public domain, complete setup from the LAN first.

   An existing database that already has a super admin will not show the OOBE. To change the owner, run `personal_migrate_single_super_admin.sql` (replace `owner@example.com` with your email) or run manually:

   ```sql
   UPDATE sys_user SET role = 'ROLE_ADMIN' WHERE role = 'ROLE_SUPER_ADMIN' AND email <> 'your@example.com';
   UPDATE sys_user SET role = 'ROLE_SUPER_ADMIN' WHERE email = 'your@example.com';
   ```

   A database unique key enforces at most one super admin. Admins can reset passwords for regular users from the Users page.

5. AI assistant (optional): start any OpenAI Chat Completions-compatible local service. The default URL is `http://127.0.0.1:8080/v1/chat/completions` and the default model is `qwen3.5-4b`; both can be changed in the admin assistant settings.

### Environment variables

| Variable | Description | Default |
| --- | --- | --- |
| `DB_URL` | MySQL JDBC URL | local `reona_blog`; uses `connectionTimeZone=Asia/Tokyo` |
| `DB_USERNAME` | Database user | `root` |
| `DB_PASSWORD` | Database password | none; use local config or environment variable |
| `UPLOAD_PATH` | Upload directory | `./data/upload/` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins, comma-separated | `http://localhost:5173,http://127.0.0.1:5173` |
| `TRUSTED_PROXIES` | Trusted reverse proxies for client IP detection | local loopback addresses |

### Default content

- Site name, contact email and owner account: configured by the OOBE.
- Sample posts: when "Sample content" is selected in the OOBE, `Default Article 1` / `Default Article 2` / `Default Article 3` are created; otherwise the blog starts empty.
- Assistant name: `Default Assistant`.

Posts are managed in `/admin/articles`; site and assistant settings are super-admin only (`/admin/settings`, `/admin/assistant`). Before going live, adjust CORS and the database password for your environment.

---

[回到顶部 / トップへ / Back to top](#top)
