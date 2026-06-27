@echo off
cd /d "%~dp0"

echo ========================================
echo AI Music Backend
echo ========================================
echo.

set "DB_URL=jdbc:h2:file:./data/aimusic_demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
set DB_USER=sa
set DB_PASSWORD=
set AI_PROVIDER=fake

echo DB_URL=%DB_URL%
echo DB_USER=%DB_USER%
echo AI_PROVIDER=%AI_PROVIDER%
echo.
echo Starting backend on http://0.0.0.0:8080 ...
echo Keep this window open while testing the Android app.
echo.

call gradlew.bat run

echo.
echo Backend process exited. If you see an error above, send it to Codex.
pause
