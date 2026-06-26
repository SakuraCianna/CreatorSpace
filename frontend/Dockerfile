# syntax=docker/dockerfile:1

FROM node:24-alpine AS build

WORKDIR /workspace/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --registry=https://registry.npmjs.org/

COPY frontend/ ./

ARG VITE_API_BASE_URL=
ARG VITE_API_TIMEOUT_MS=15000
ARG VITE_UPLOAD_MAX_FILE_SIZE_MB=20

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_API_TIMEOUT_MS=${VITE_API_TIMEOUT_MS}
ENV VITE_UPLOAD_MAX_FILE_SIZE_MB=${VITE_UPLOAD_MAX_FILE_SIZE_MB}

RUN npm run build

FROM nginx:1.29-alpine AS runtime

COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1
