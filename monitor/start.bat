@echo off
echo ================================
echo NoobHubPay Payment Monitor
echo ================================
echo.

cd /d "%~dp0"

if not exist "config.json" (
    echo [ERROR] config.json not found!
    echo Please run setup.bat first.
    pause
    exit /b 1
)

echo Checking Python...
python --version > nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python not found!
    echo Please install Python 3.8+
    pause
    exit /b 1
)

echo Checking dependencies...
python -c "import easyocr" > nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Dependencies not installed.
    echo Installing...
    pip install -r requirements.txt
    if %errorlevel% neq 0 (
        echo [ERROR] Installation failed!
        pause
        exit /b 1
    )
)

echo.
echo Starting monitor...
echo First run will download OCR models (~200MB)
echo.

python payment_monitor.py

pause
