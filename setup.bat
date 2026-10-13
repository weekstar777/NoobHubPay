@echo off
:: Simple config setup without any Chinese characters
:: Works on all Windows versions

cd /d "%~dp0monitor"

if not exist "config.json.example" (
    echo ERROR: Template file not found
    pause
    exit /b 1
)

if exist "config.json" (
    echo Config file already exists. Backup to config.json.old
    copy /Y "config.json" "config.json.old" > nul
)

echo Copying template...
copy /Y "config.json.example" "config.json" > nul

echo Generating random key...
for /f "delims=" %%i in ('powershell -Command "$chars='abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'; -join ((1..32) | ForEach {$chars[(Get-Random -Max $chars.Length)]})"') do set KEY=%%i

echo.
echo ========================================
echo SUCCESS! Config file created.
echo ========================================
echo.
echo Your secret key:
echo %KEY%
echo.
echo Next steps:
echo 1. Edit monitor\config.json
echo    Change: "secret_key": "change-this-to-random-secret-key"
echo    To: "secret_key": "%KEY%"
echo.
echo 2. Edit plugins\NoobHubPay\config.yml
echo    Change: secret-key: "change-this-to-random-secret-key"
echo    To: secret-key: "%KEY%"
echo.
echo 3. Make sure BOTH files use THE SAME key!
echo.

notepad "config.json"

pause
