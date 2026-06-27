@echo off
cd /d "%~dp0"

echo ========================================
echo AI Music Backend - MiniMax Music
echo ========================================
echo.
echo This script does NOT save your MiniMax API key to any file.
echo Paste the key when prompted. It is only used for this terminal session.
echo.

set "DB_URL=jdbc:h2:file:./data/aimusic_demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
set DB_USER=sa
set DB_PASSWORD=

set AI_PROVIDER=minimax
set MINIMAX_MODEL=music-2.6-free
set MINIMAX_OUTPUT_FORMAT=url
set MINIMAX_INSTRUMENTAL=true
set MINIMAX_REQUEST_TIMEOUT_MS=300000

for /f "usebackq delims=" %%A in (`powershell -NoProfile -Command "$p=Read-Host 'Paste MiniMax API Key and press Enter' -AsSecureString; $b=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($p); try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($b) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($b) }"`) do set "MINIMAX_API_KEY=%%A"

if "%MINIMAX_API_KEY%"=="" (
    echo.
    echo MiniMax API Key is empty. Backend will not start.
    pause
    exit /b 1
)

echo.
echo DB_URL=%DB_URL%
echo DB_USER=%DB_USER%
echo AI_PROVIDER=%AI_PROVIDER%
echo MINIMAX_MODEL=%MINIMAX_MODEL%
echo MINIMAX_OUTPUT_FORMAT=%MINIMAX_OUTPUT_FORMAT%
echo MINIMAX_REQUEST_TIMEOUT_MS=%MINIMAX_REQUEST_TIMEOUT_MS%
echo.
echo Starting backend on http://0.0.0.0:8080 ...
echo Keep this window open while testing the Android app.
echo.

call gradlew.bat run

echo.
echo Backend process exited. If you see an error above, send it to Codex.
pause
