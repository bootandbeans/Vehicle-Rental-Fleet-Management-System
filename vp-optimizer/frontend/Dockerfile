# ---------------------------------------------------------------------------
# Build stage - Vite builds the static bundle
# ---------------------------------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY tsconfig*.json vite.config.ts vitest.config.ts index.html ./
COPY src ./src

# `npm run build` also runs `tsc -b`, so a type error fails the image build.
RUN npm run build

# ---------------------------------------------------------------------------
# Runtime stage - nginx serves the SPA and proxies /api to the backend
# ---------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget -qO- http://localhost/ >/dev/null || exit 1
