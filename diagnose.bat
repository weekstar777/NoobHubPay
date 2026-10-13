@echo off
chcp 65001 > nul
echo ================================
echo NoobHubPay 环境诊断工具
echo ================================
echo.

echo [检查1] 当前目录
echo %cd%
echo.

echo [检查2] 脚本所在目录
cd /d "%~dp0"
echo %cd%
echo.

echo [检查3] monitor目录
if exist "monitor\" (
    echo ✓ monitor目录存在
) else (
    echo ✗ monitor目录不存在!
)
echo.

echo [检查4] monitor目录内容
if exist "monitor\" (
    dir "monitor\"
) else (
    echo 无法检查
)
echo.

echo [检查5] 配置文件
if exist "monitor\config.json.example" (
    echo ✓ config.json.example 存在
) else (
    echo ✗ config.json.example 不存在!
)

if exist "monitor\config.json" (
    echo ✓ config.json 已存在
) else (
    echo ○ config.json 不存在 (正常,首次需要创建)
)
echo.

echo [检查6] Python环境
python --version 2>nul
if %errorlevel% neq 0 (
    echo ✗ Python未安装或不在PATH中
) else (
    echo ✓ Python已安装
)
echo.

echo [检查7] PowerShell可用性
powershell -Command "Write-Host 'PowerShell可用'" 2>nul
if %errorlevel% neq 0 (
    echo ✗ PowerShell不可用
) else (
    echo ✓ PowerShell可用
)
echo.

echo [检查8] 服务器插件目录
if exist "D:\project_collection\code_project\NoobHub\plugins\" (
    echo ✓ 服务器plugins目录存在
    if exist "D:\project_collection\code_project\NoobHub\plugins\NoobHubPay.jar" (
        echo ✓ NoobHubPay.jar 已部署
    ) else (
        echo ○ NoobHubPay.jar 未部署
    )
) else (
    echo ✗ 服务器plugins目录不存在
)
echo.

echo ================================
echo 诊断完成
echo ================================
echo.

pause
