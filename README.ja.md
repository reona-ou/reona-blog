# Reona Blog

[简体中文](README.md) | [English](README.en.md) | [日本語](README.ja.md)

Vue 3、Spring Boot、MySQL で作った個人ブログです。記事、コメントと返信、記事・コメントのいいね、管理画面、3 言語 UI、設定可能な AI サイトアシスタント、読み取り専用の Google カレンダー表示を備えています。

## 主な特徴

- **既定アカウントなし**：初回デプロイ時の OOBE で唯一のスーパー管理者（ブログのオーナー）を作成します。リポジトリにアカウント・パスワード・サンプル認証情報はありません。
- **3 言語 UI**：中文 / English / 日本語 をフロントと管理画面の両方で完全対応。選択した言語はユーザーごとに保存されます。
- **AI サイトアシスタント**：OpenAI Chat Completions 互換。公開アクセス可、クライアント IP 単位のレート制限、ストリーミング出力。名前 / エンドポイント / モデル / Prompt / 口調は管理画面で変更できます。
- **Google カレンダー**：スーパー管理者が管理画面にカレンダーのプライベート iCal アドレスを登録し、読み取り専用の月表示で予定を確認できます。スーパー管理者のみ表示されます。
- **権限の階層化**：スーパー管理者は 1 人だけ（データベースの一意キーで強制）。一般管理者は任意で、オーナーが既存ユーザーを昇格して作成します。サイト設定とアシスタント設定はスーパー管理者のみ変更できます。

---

## 技術スタック

- フロントエンド：Vue 3、Vite、Pinia、Vue Router、Vue I18n、Element Plus、marked、DOMPurify
- バックエンド：Spring Boot 4.1.1、MyBatis-Plus、JWT、BCrypt、Hutool
- データベース：MySQL 8
- AI：OpenAI Chat Completions 互換サービス。既定ではローカル Qwen に接続

## ディレクトリ構成

| パス | 説明 |
| --- | --- |
| `web/` | Vue 3 のフロントエンドと管理画面 |
| `BlogApi/` | Spring Boot バックエンド |
| `database_schema.sql` | 空データベース用のテーブル定義（ブログ関連 9 テーブル） |
| `personal_seed.sql` | 既定のサイト設定とインタラクション用テーブル。アカウントは OOBE で作成し、サンプル記事は必要に応じて作成 |
| `personal_migrate_*.sql` | 既存データベース用の移行スクリプト |
| `README.md` / `README.en.md` / `README.ja.md` | 3 言語の説明 |

## 機能一覧

- 記事：公開、下書き、Markdown 編集、ラベル絞り込み、検索。
- コメント：ログイン後に投稿・返信・自分のコメント削除。管理者は任意のコメントを削除可能。
- いいね：記事とコメントのいいね/解除。
- ユーザーと権限：スーパー管理者は 1 人だけ（ブログのオーナー）。一般管理者は任意で、オーナーが「ユーザー管理」で既存ユーザーを昇格します。管理者は一般ユーザーを管理できます。サイト設定とアシスタント設定はスーパー管理者のみ変更できます。
- フロントの執筆入口：管理者/スーパー管理者でログインすると、ホームとブログ一覧に「記事を書く」、記事ページに「記事を編集」が表示されます。スーパー管理者はログイン後にフロントへ戻ります。
- アカウント設定：ログインユーザーは表示名、メール、電話、住所、パスワードを変更できます。管理者は管理対象ユーザーのパスワードを再設定できます。
- 3 言語 UI：中文 / English / 日本語。選択言語は保存されます。
- サイト設定：サイト名、連絡先メール。アシスタント設定：名前、API、モデル、Prompt、統一した口調。どちらもスーパー管理者のみ変更できます。
- AI アシスタント：公開。クライアント IP 単位で制限。ストリーミング出力。既定ではローカル Qwen に接続。
- Google カレンダー：スーパー管理者は管理画面にカレンダーのプライベート iCal アドレスを登録し、読み取り専用の月表示で予定を確認できます。スーパー管理者のみ表示されます。
- ファイルアップロード：管理者のみ。jpg/jpeg/png/gif/webp、最大 10MB。
- 初回セットアップ（OOBE）：スーパー管理者がいない場合に `/setup` へ移動し、サイト、オーナーアカウント、任意のアシスタントとサンプル記事を設定します。完了後は自動ログインし、履歴として残っている defaultadmin テストアカウントは削除されます。

---

## 必要な環境

- JDK 25
- Maven 3.6+
- Node.js 20.19+ / 22.12+
- MySQL 8

## ローカル起動

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

## 本番デプロイ

1. ビルド：`cd BlogApi && mvn -DskipTests package`、`cd web && npm run build`（成果物は `web/dist/`）。
2. 静的ファイルは Nginx で配信し、`/api/` をバックエンドへリバースプロキシします。バックエンドは systemd で常駐させ、環境変数は `/etc/reona-blog.env` に置きます：

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
   DB_PASSWORD=<データベースパスワード>
   UPLOAD_PATH=/mnt/app-data/reona-blog/upload/
   CORS_ALLOWED_ORIGINS=https://your-domain
   TRUSTED_PROXIES=127.0.0.1,::1
   ```

   ```bash
   java -Dserver.port=8090 -jar /opt/reona-blog/reona-blog.jar
   ```

3. サイトを一度開いて OOBE を完了させます。初期化 API はローカル/プライベートネットワークからのみ利用できます。公開ドメインで既に公開している場合は、先に LAN 内で初期化してください。

## 初回セットアップとスーパー管理者

既にスーパー管理者がいる既存 DB では OOBE は表示されません。オーナーを変更する場合は `personal_migrate_single_super_admin.sql`（`owner@example.com` を自分のメールに置換）または手動で実行してください：

```sql
UPDATE sys_user SET role = 'ROLE_ADMIN' WHERE role = 'ROLE_SUPER_ADMIN' AND email <> 'your@example.com';
UPDATE sys_user SET role = 'ROLE_SUPER_ADMIN' WHERE email = 'your@example.com';
```

データベースの一意キーにより、スーパー管理者は最大 1 人です。管理者は「ユーザー管理」から一般ユーザーのパスワードを再設定できます。

## AI アシスタント（任意）

OpenAI Chat Completions 互換のローカルサービスを起動します。既定の URL は `http://127.0.0.1:8080/v1/chat/completions`、モデルは `qwen3.5-4b` です。管理画面の「アシスタント設定」でも変更できます。

---

## 環境変数

| 変数 | 説明 | 既定値 |
| --- | --- | --- |
| `DB_URL` | MySQL JDBC URL | ローカル `reona_blog`（`connectionTimeZone=Asia/Tokyo`） |
| `DB_USERNAME` | DB ユーザー名 | `root` |
| `DB_PASSWORD` | DB パスワード | なし。ローカル設定または環境変数推奨 |
| `UPLOAD_PATH` | アップロード先ディレクトリ | `./data/upload/` |
| `CORS_ALLOWED_ORIGINS` | 許可するフロントエンド Origin（カンマ区切り） | `http://localhost:5173,http://127.0.0.1:5173` |
| `TRUSTED_PROXIES` | クライアント IP を読む際に信頼するリバースプロキシ | ローカルループバック |

## 既定コンテンツ

- サイト名、連絡先メール、オーナーアカウント：OOBE で設定。
- サンプル記事：OOBE で「サンプルコンテンツ」を選ぶと `Default Article 1` / `Default Article 2` / `Default Article 3` を作成。選ばなければ空のブログになります。
- アシスタント名：`Default Assistant`。

記事は `/admin/articles`、サイト設定とアシスタント設定はスーパー管理者のみ `/admin/settings`、`/admin/assistant` から変更できます。公開前に CORS と DB パスワードを環境に合わせて設定してください。

---

[简体中文](README.md) | [English](README.en.md)
