@echo off
chcp 65001 > nul
echo ================================
echo NoobHubPay 监控程序
echo ================================
echo.

cd /d "%~dp0"

if not exist "config.json" (
    echo [错误] 配置文件 config.json 不存在!
    pause
    exit /b 1
)

echo [信息] 检查Python环境...
python --version > nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Python! 请先安装Python 3.8+
    pause
    exit /b 1
)

echo [信息] 检查依赖...
python -c "import easyocr" > nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] 依赖未安装,正在安装...
    pip install -r requirements.txt
    if %errorlevel% neq 0 (
        echo [错误] 依赖安装失败!
        pause
        exit /b 1
    )
)

echo.
echo [信息] 启动监控程序...
echo [提示] 首次运行会下载OCR模型,请耐心等待
echo.

python payment_monitor.py

pause
