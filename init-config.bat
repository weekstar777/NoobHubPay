@echo off
chcp 65001 > nul
echo ================================
echo NoobHubPay 配置初始化
echo ================================
echo.

cd /d "%~dp0monitor"

if exist "config.json" (
    echo [警告] config.json 已存在!
    echo.
    choice /C YN /M "是否覆盖现有配置"
    if errorlevel 2 (
        echo 已取消
        pause
        exit /b 0
    )
)

echo [1/2] 复制配置模板...
copy /Y "config.json.example" "config.json" > nul

echo [2/2] 生成随机密钥...
setlocal enabledelayedexpansion

:: 生成32位随机密钥
set "chars=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
set "key="
for /L %%i in (1,1,32) do (
    set /a "rand=!random! %% 62"
    for %%j in (!rand!) do set "key=!key!!chars:~%%j,1!"
)

echo.
echo ================================
echo ✓ 配置文件已创建!
echo ================================
echo.
echo 文件位置: monitor\config.json
echo 生成的密钥: !key!
echo.
echo 重要提示:
echo   1. 编辑 monitor\config.json
echo   2. 将密钥 !key! 替换到配置文件中的 secret_key
echo   3. 同步修改服务器配置 plugins\NoobHubPay\config.yml
echo   4. 调整窗口标题和字段映射
echo.

endlocal
pause
