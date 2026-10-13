@echo off
:: NoobHubPay Build and Deploy Script
:: Pure ASCII - no encoding issues

echo ================================
echo NoobHubPay Build and Deploy
echo ================================
echo.

cd /d "%~dp0plugin"

echo [1/3] Cleaning...
call mvn clean -q

echo [2/3] Compiling...
call mvn package -DskipTests -q
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Build failed! Check your code.
    pause
    exit /b 1
)

echo [3/3] Deploying to server...
copy /Y "target\NoobHubPay-1.0.0.jar" "D:\project_collection\code_project\NoobHub\plugins\NoobHubPay.jar" > nul
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Deploy failed! Check the path.
    pause
    exit /b 1
)

echo.
echo ================================
echo Build and Deploy SUCCESS!
echo ================================
echo.
echo Output: D:\project_collection\code_project\NoobHub\plugins\NoobHubPay.jar
echo.
echo Next steps:
echo   1. Restart Minecraft server
echo   2. Check plugin loaded: /plugins
echo   3. Check status: /payadmin status
echo.

pause
