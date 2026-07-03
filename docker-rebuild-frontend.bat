@echo off
setlocal

cd /d "%~dp0"

if not defined POSTGRES_PORT set POSTGRES_PORT=15432

echo [CreatorSpace] Workdir: %cd%
echo [CreatorSpace] Host Postgres port: %POSTGRES_PORT%
echo [CreatorSpace] Checking docker compose config...
docker compose config --quiet
if errorlevel 1 goto :compose_error

echo [CreatorSpace] Building frontend image...
docker compose build frontend
if errorlevel 1 goto :build_error

echo [CreatorSpace] Starting frontend service...
docker compose up -d frontend
if errorlevel 1 goto :up_error

echo [CreatorSpace] Current services:
docker compose ps

echo.
echo [CreatorSpace] Done.
echo [CreatorSpace] Open http://localhost:8088/admin/pages
pause
exit /b 0

:compose_error
echo.
echo [CreatorSpace] docker compose config failed. Check .env and docker-compose.yml in this project.
pause
exit /b 1

:build_error
echo.
echo [CreatorSpace] frontend image build failed.
echo [CreatorSpace] If the message mentions registry-1.docker.io, node:24-alpine, nginx:1.29-alpine, handshake, timeout, or proxy, switch your network node and run this file again.
pause
exit /b 1

:up_error
echo.
echo [CreatorSpace] frontend service failed to start. Run docker compose ps in the project folder for details.
pause
exit /b 1
