# BookaBeeka - Render Deployment Guide

This repository is fully configured for deployment on **Render**:
- **Backend**: Spring Boot 4 / Java 21 containerized with Docker (`Web Service`)
- **Frontend**: React + Vite + Tailwind Single-Page Application (`Static Site`)

---

## Method 1: Automatic 1-Click Deployment (Render Blueprint) - Recommended

Render's Blueprint feature reads the root [`render.yaml`](./render.yaml) file to automatically provision both the Backend Web Service and the Frontend Static Site with their configurations linked.

1. Push your latest changes to GitHub/GitLab:
   ```bash
   git add .
   git commit -m "Prepare project for Render deployment"
   git push origin <your-branch>
   ```
2. Log in to [Render Dashboard](https://dashboard.render.com).
3. Click **New +** > **Blueprint**.
4. Connect your repository.
5. Render will detect [`render.yaml`](./render.yaml) and list both services:
   - `bookabeeka-backend` (Docker)
   - `bookabeeka-frontend` (Static Site)
6. Fill in the required environment variables (marked with `sync: false`):
   - `SPRING_DATASOURCE_URL`: e.g. `jdbc:postgresql://<host>:5432/<dbname>?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME`: database username
   - `SPRING_DATASOURCE_PASSWORD`: database password
   - `CLOUDINARY_CLOUD_NAME`: your Cloudinary cloud name
   - `CLOUDINARY_API_KEY`: your Cloudinary API key
   - `CLOUDINARY_API_SECRET`: your Cloudinary API secret
   - `SPRING_MAIL_USERNAME`: your Gmail address
   - `SPRING_MAIL_PASSWORD`: your Google App Password (16 characters)
7. Click **Apply**. Render will automatically build both services!

---

## Method 2: Manual Deployment via Render Dashboard

If you prefer to configure each service manually:

### 1. Deploy the Backend (Docker Web Service)

1. On the Render Dashboard, click **New +** > **Web Service**.
2. Connect your Git repository.
3. Configure the service settings:
   - **Name**: `bookabeeka-backend`
   - **Language / Runtime**: **Docker**
   - **Root Directory**: `Backend` (or leave empty if using `Backend/Dockerfile`)
   - **Dockerfile Path**: `Backend/Dockerfile`
   - **Docker Context**: `Backend`
   - **Instance Type**: Free (or Starter)
   - **Health Check Path**: `/api/health`
4. Under **Environment Variables**, add:

   | Key | Value / Example | Notes |
   | :--- | :--- | :--- |
   | `PORT` | `8080` | Render injects this automatically; default fallback is 8080 |
   | `SPRING_PROFILES_ACTIVE` | `prod` | Production profile |
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host>:5432/<dbname>?sslmode=require` | Neon or Render PostgreSQL JDBC URL |
   | `SPRING_DATASOURCE_USERNAME` | `<db_user>` | Database user |
   | `SPRING_DATASOURCE_PASSWORD` | `<db_password>` | Database password |
   | `JWT_SECRET_KEY` | *(64-hex string)* | Keep secure |
   | `CORS_ALLOWED_ORIGINS` | `*` or `https://your-frontend.onrender.com` | Allowed frontend domains |
   | `CLOUDINARY_CLOUD_NAME` | `<cloud_name>` | Cloudinary account name |
   | `CLOUDINARY_API_KEY` | `<api_key>` | Cloudinary API Key |
   | `CLOUDINARY_API_SECRET` | `<api_secret>` | Cloudinary API Secret |
   | `APP_MAIL_ENABLED` | `true` | Set to true to send transactional emails |
   | `SPRING_MAIL_HOST` | `smtp.gmail.com` | SMTP host |
   | `SPRING_MAIL_PORT` | `587` | SMTP port |
   | `SPRING_MAIL_USERNAME` | `your_email@gmail.com` | SMTP email |
   | `SPRING_MAIL_PASSWORD` | `your_app_password` | Gmail App Password |
   | `APP_BASE_URL` | `https://your-frontend.onrender.com` | URL used in email activation links |

5. Click **Create Web Service**. Wait for the build and deployment to complete.
6. Note down the backend URL (e.g., `https://bookabeeka-backend.onrender.com`).

---

### 2. Deploy the Frontend (Static Site)

1. On the Render Dashboard, click **New +** > **Static Site**.
2. Connect the same Git repository.
3. Configure the static site settings:
   - **Name**: `bookabeeka-frontend`
   - **Root Directory**: `FrontEnd`
   - **Build Command**: `npm install && npm run build`
   - **Publish Directory**: `dist`
4. Under **Redirects/Rewrites**:
   - Render will automatically use [`FrontEnd/public/_redirects`](./FrontEnd/public/_redirects), but you can also explicitly add:
     - **Type**: `Rewrite`
     - **Source**: `/*`
     - **Destination**: `/index.html`
5. Under **Environment Variables**, add:

   | Key | Value | Notes |
   | :--- | :--- | :--- |
   | `VITE_API_BASE_URL` | `https://bookabeeka-backend.onrender.com` | URL of your deployed backend Web Service |

6. Click **Create Static Site**.
7. Once deployed, open your frontend site URL and test login and navigation.

---

## Key Technical Optimizations Applied

1. **JVM Memory Optimization (`JAVA_OPTS`)**:
   Render's free tier has a 512MB RAM cap. The backend Dockerfile is configured with:
   `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`
   This prevents Out-Of-Memory (OOM) termination on Render.

2. **Render Dynamic Port Binding**:
   Render dynamically assigns an internal port through the `$PORT` environment variable. Spring Boot's configuration and Docker entrypoint bind to `${PORT}` dynamically.

3. **Dedicated Health Check Endpoint**:
   [`HealthController`](./Backend/src/main/java/com/system/booking/config/HealthController.java) provides `/health` and `/api/health` (publicly allowed in `SecurityConfig`), allowing Render's load balancer to verify service health accurately without authentication blocks.

4. **SPA Client-Side Routing**:
   Both [`render.yaml`](./render.yaml) rewrite rules and the [`_redirects`](./FrontEnd/public/_redirects) file ensure that refreshing on deep React Router paths (e.g. `/login/owner`, `/admin/dashboard`) correctly routes to `/index.html` instead of displaying 404 errors.

5. **Safe Protocol Normalization**:
   Frontend API configuration (`FrontEnd/src/config/api.ts`) automatically normalizes `VITE_API_BASE_URL` to include `https://` if entered as a plain domain.
