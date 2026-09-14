FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --silent
COPY . .
RUN npm run build -- --configuration production

FROM nginx:1.25-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/dist/gbanque/browser /usr/share/nginx/html

RUN addgroup -S nginxgroup && adduser -S nginxuser -G nginxgroup \
    && chown -R nginxuser:nginxgroup /usr/share/nginx/html \
    && chown -R nginxuser:nginxgroup /var/cache/nginx \
    && chown -R nginxuser:nginxgroup /var/log/nginx \
    && touch /var/run/nginx.pid \
    && chown -R nginxuser:nginxgroup /var/run/nginx.pid

USER nginxuser
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:80/health || exit 1

CMD ["nginx", "-g", "daemon off;"]