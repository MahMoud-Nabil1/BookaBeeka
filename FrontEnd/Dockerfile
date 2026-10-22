# ==============================================================================
# Stage 1: Build Vite / React Application
# ==============================================================================
FROM node:20-alpine AS builder
WORKDIR /app

# 1. Install dependencies first to leverage Docker layer caching
COPY package.json package-lock.json ./
RUN npm ci

# 2. Copy source code
COPY . .

# 3. Optional build argument for backend API URL
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# 4. Build optimized production bundle into /app/dist
RUN npm run build

# ==============================================================================
# Stage 2: Production Nginx Server
# ==============================================================================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Clean out default Nginx assets
RUN rm -rf ./*

# Copy custom SPA Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled frontend assets from builder stage
COPY --from=builder /app/dist .

# Expose default HTTP port
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
