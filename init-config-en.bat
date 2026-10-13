@echo off
echo ================================
echo NoobHubPay Configuration Setup
echo ================================
echo.

cd /d "%~dp0monitor"

if not exist "config.json.example" (
    echo [ERROR] config.json.example not found!
    echo Current directory: %cd%
    pause
    exit /b 1
)

if exist "config.json" (
    echo [WARNING] config.json already exists!
    echo.
    set /p confirm="Overwrite? (Y/N): "
    if /i not "%confirm%"=="Y" (
        echo Cancelled.
        pause
        exit /b 0
    )
)

echo.
echo [1/2] Copying config template...
copy /Y "config.json.example" "config.json" > nul
if %errorlevel% neq 0 (
    echo [ERROR] Copy failed!
    pause
    exit /b 1
)
echo OK!

echo [2/2] Generating random secret key...
for /f "delims=" %%i in ('powershell -Command "$chars='abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'; -join ((1..32) | ForEach-Object {$chars[(Get-Random -Maximum $chars.Length)]})"') do set SECRET_KEY=%%i

if "%SECRET_KEY%"=="" (
    echo [WARNING] Failed to generate key, using default
    set SECRET_KEY=change-this-to-random-secret-key
)

echo OK!
echo.
echo ================================
echo Configuration file created!
echo ================================
echo.
echo Location: %cd%\config.json
echo.
echo Generated secret key:
echo %SECRET_KEY%
echo.
echo ================================
echo Next steps:
echo ================================
echo.
echo 1. Edit: monitor\config.json
echo    Find: "secret_key": "change-this-to-random-secret-key"
echo    Replace with: "secret_key": "%SECRET_KEY%"
echo.
echo 2. Edit: plugins\NoobHubPay\config.yml
echo    Find: secret-key: "change-this-to-random-secret-key"
echo    Replace with: secret-key: "%SECRET_KEY%"
echo.
echo 3. Restart Minecraft server
echo.
echo 4. Test: /payadmin status
echo.

pause
