# ---- build the Angular app ------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- serve it with Caddy ---------------------------------------------------
# Caddy serves the SPA and reverse-proxies /api to the backend (see deploy/Caddyfile,
# mounted at runtime). Automatic HTTPS via Let's Encrypt.
FROM caddy:2-alpine
COPY --from=build /app/dist/seerah-web/browser /srv
