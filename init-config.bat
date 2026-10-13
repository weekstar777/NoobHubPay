@echo off
chcp 65001 > nul
echo ================================
echo NoobHubPay 配置初始化
echo ================================
echo.

:: 切换到脚本所在目录
cd /d "%~dp0"

:: 检查monitor目录
if not exist "monitor\" (
    echo [错误] monitor目录不存在!
    echo 当前目录: %cd%
    pause
    exit /b 1
)

cd monitor

:: 检查模板文件
if not exist "config.json.example" (
    echo [错误] 配置模板文件不存在: config.json.example
    echo 当前目录: %cd%
    dir
    pause
    exit /b 1
)

:: 检查是否已存在配置
if exist "config.json" (
    echo [警告] config.json 已存在!
    echo.
    set /p confirm="是否覆盖? (Y/N): "
    if /i not "%confirm%"=="Y" (
        echo 已取消
        pause
        exit /b 0
    )
)

echo.
echo [1/2] 复制配置模板...
copy /Y "config.json.example" "config.json"
if %errorlevel% neq 0 (
    echo [错误] 复制失败!
    pause
    exit /b 1
)

echo [2/2] 生成随机密钥...

:: 使用PowerShell生成随机密钥(更可靠)
for /f "delims=" %%i in ('powershell -Command "$chars='abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'; -join ((1..32) | ForEach-Object {$chars[(Get-Random -Maximum $chars.Length)]})"') do set SECRET_KEY=%%i

if "%SECRET_KEY%"=="" (
    echo [警告] 自动生成密钥失败,使用默认密钥
    set SECRET_KEY=change-this-to-random-secret-key
)

echo.
echo ================================
echo 成功创建配置文件!
echo ================================
echo.
echo 文件位置: %cd%\config.json
echo.
echo 生成的随机密钥:
echo %SECRET_KEY%
echo.
echo ================================
echo 下一步操作:
echo ================================
echo.
echo 1. 编辑配置文件:
echo    monitor\config.json
echo.
echo 2. 找到这一行:
echo    "secret_key": "change-this-to-random-secret-key"
echo.
echo 3. 替换为:
echo    "secret_key": "%SECRET_KEY%"
echo.
echo 4. 同步修改服务器配置:
echo    plugins\NoobHubPay\config.yml
echo    找到: secret-key: "change-this-to-random-secret-key"
echo    替换为: secret-key: "%SECRET_KEY%"
echo.
echo 5. 调整窗口标题和字段映射(根据实际情况)
echo.

pause
