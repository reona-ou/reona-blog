# Reona Blog

[简体中文](README.md) | [English](README.en.md) | [日本語](README.ja.md)

A personal blog built with Vue 3, Spring Boot and MySQL. It ships posts, comments and replies, post/comment likes, an admin area, a trilingual UI, a configurable AI site assistant and a read-only Google Calendar view.

## Highlights

- **No default accounts**: the OOBE creates the only super admin (the blog owner) on first run. The repository contains no accounts, passwords or sample credentials.
- **Trilingual UI**: Chinese / English / Japanese across storefront and admin area; the chosen language is persisted per user.
- **AI site assistant**: OpenAI Chat Completions-compatible, publicly available, rate-limited by client IP, streamed responses; name / endpoint / model / prompt / tone are configurable in the admin area.
- **Google Calendar**: the super admin pastes a calendar's private iCal address in the admin area and views events in a read-only month view; visible only to the super admin.
- **Layered roles**: exactly one super admin (enforced by a database unique key); regular admins are optional and promoted by the owner; site and assistant settings are super-admin only.

---

## Tech stack

- Frontend: Vue 3, Vite, Pinia, Vue Router, Vue I18n, Element Plus, marked, DOMPurify
- Backend: Spring Boot 4.1.1, MyBatis-Plus, JWT, BCrypt, Hutool
- Database: MySQL 8
- AI: any OpenAI Chat Completions-compatible service; local Qwen by default

## Layout

| Path | Description |
| --- | --- |
| `web/` | Vue 3 storefront and admin UI |
| `BlogApi/` | Spring Boot backend |
| `database_schema.sql` | Schema for an empty database (9 blog-related tables) |
| `personal_seed.sql` | Default site settings and interaction tables; accounts come from the OOBE, sample posts on demand |
| `personal_migrate_*.sql` | Migration scripts for existing databases |
| `README.md` / `README.en.md` / `README.ja.md` | Trilingual documentation |

## Features

- Posts: publish, draft, Markdown editing, label filtering and search.
- Comments: logged-in users can comment, reply and delete their own comments; admins can delete any comment.
- Likes: like/unlike posts and comments.
- Users and roles: exactly one super admin (the blog owner). Regular admins are optional; the owner promotes an existing user from the Users page. Admins manage regular users. Site and assistant settings are super-admin only.
- Front-end authoring: admins and super admins see "Write a post" on the home/blog pages and "Edit post" on article pages. Super admins land on the storefront after login.
- Account settings: logged-in users can update their display name, email, phone, address and password; admins can reset passwords for users they manage.
- Trilingual UI: Chinese / English / Japanese; the selected language is remembered.
- Site settings: site name and contact email. Assistant settings: name, API, model, prompt and unified tone. Both are super-admin only.
- AI assistant: public, rate-limited by client IP, streamed responses; connects to local Qwen by default.
- Google Calendar: the super admin can paste a calendar's private iCal address in the admin area and view events in a read-only month view; visible only to the super admin.
- File uploads: admins only; jpg/jpeg/png/gif/webp, up to 10MB.
- First-run setup (OOBE): when no super admin exists, the site opens `/setup` to create the site, owner account, and optional assistant and sample posts; it logs in automatically and cleans up a legacy defaultadmin test account if present.

---

## Requirements

- JDK 25
- Maven 3.6+
- Node.js 20.19+ / 22.12+
- MySQL 8

## Local setup

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

## Production deployment

1. Build: `cd BlogApi && mvn -DskipTests package`, `cd web && npm run build` (output in `web/dist/`).
2. Serve the static files with Nginx and proxy `/api/` to the backend; run the backend as a systemd service with the environment file `/etc/reona-blog.env`:

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
   DB_PASSWORD=<database password>
   UPLOAD_PATH=/mnt/app-data/reona-blog/upload/
   CORS_ALLOWED_ORIGINS=https://your-domain
   TRUSTED_PROXIES=127.0.0.1,::1
   ```

   ```bash
   java -Dserver.port=8090 -jar /opt/reona-blog/reona-blog.jar
   ```

3. Open the site once and finish the OOBE. The initialization API only accepts local/private-network requests; if the site is already exposed on a public domain, complete setup from the LAN first.

## First-run setup and the super admin

An existing database that already has a super admin will not show the OOBE. To change the owner, run `personal_migrate_single_super_admin.sql` (replace `owner@example.com` with your email) or run manually:

```sql
UPDATE sys_user SET role = 'ROLE_ADMIN' WHERE role = 'ROLE_SUPER_ADMIN' AND email <> 'your@example.com';
UPDATE sys_user SET role = 'ROLE_SUPER_ADMIN' WHERE email = 'your@example.com';
```

A database unique key enforces at most one super admin. Admins can reset passwords for regular users from the Users page.

## AI assistant (optional)

Start any OpenAI Chat Completions-compatible local service. The default URL is `http://127.0.0.1:8080/v1/chat/completions` and the default model is `qwen3.5-4b`; both can be changed in the admin assistant settings.

---

## Environment variables

| Variable | Description | Default |
| --- | --- | --- |
| `DB_URL` | MySQL JDBC URL | local `reona_blog`; uses `connectionTimeZone=Asia/Tokyo` |
| `DB_USERNAME` | Database user | `root` |
| `DB_PASSWORD` | Database password | none; use local config or environment variable |
| `UPLOAD_PATH` | Upload directory | `./data/upload/` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins, comma-separated | `http://localhost:5173,http://127.0.0.1:5173` |
| `TRUSTED_PROXIES` | Trusted reverse proxies for client IP detection | local loopback addresses |

## Default content

- Site name, contact email and owner account: configured by the OOBE.
- Sample posts: when "Sample content" is selected in the OOBE, `Default Article 1` / `Default Article 2` / `Default Article 3` are created; otherwise the blog starts empty.
- Assistant name: `Default Assistant`.

Posts are managed in `/admin/articles`; site and assistant settings are super-admin only (`/admin/settings`, `/admin/assistant`). Before going live, adjust CORS and the database password for your environment.

---

[简体中文](README.md) | [日本語](README.ja.md)
