# syntax=docker/dockerfile:1
# ============ STAGE 1: BUILD ============
FROM node:20-alpine AS build
WORKDIR /app

# package-lock.json d'abord : "npm ci" = build reproductible, couche cachée tant que les dépendances ne changent pas
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

COPY . .
RUN npm run build -- --configuration production

# ============ STAGE 2: RUNTIME ============
# alpine-slim : nginx sans modules optionnels (~5x plus léger que nginx:alpine)
FROM nginx:1.27-alpine-slim

LABEL org.opencontainers.image.title="g-banque-frontend" \
      org.opencontainers.image.description="G-Banque — Angular servi par nginx" \
      org.opencontainers.image.source="https://github.com/HmaydaAbdo/g-banque"

RUN apk upgrade --no-cache \
    && rm /etc/nginx/conf.d/default.conf \
    && addgroup -S nginxgroup && adduser -S nginxuser -G nginxgroup \
    && chown -R nginxuser:nginxgroup /var/cache/nginx /var/log/nginx

COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build --chown=nginxuser:nginxgroup /app/dist/gbanque/browser /usr/share/nginx/html

USER nginxuser
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:80/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
