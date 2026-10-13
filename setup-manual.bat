@echo off
echo ================================
echo NoobHubPay 手动配置向导
echo ================================
echo.
echo 此脚本将帮助你手动配置NoobHubPay
echo.
pause

cd /d "%~dp0monitor"

echo.
echo [步骤1] 检查文件...
echo.

if not exist "config.json.example" (
    echo [错误] 找不到 config.json.example
    echo 请确保在正确的目录运行此脚本
    pause
    exit /b 1
)

if exist "config.json" (
    echo 发现已有配置文件: config.json
    echo.
    set /p overwrite="是否要备份旧配置并创建新配置? (Y/N): "
    if /i "%overwrite%"=="Y" (
        echo 备份旧配置为 config.json.backup
        copy /Y "config.json" "config.json.backup" > nul
    ) else (
        echo 保留现有配置,退出
        pause
        exit /b 0
    )
)

echo.
echo [步骤2] 复制配置模板...
copy /Y "config.json.example" "config.json"
echo 完成!
echo.

echo [步骤3] 配置说明
echo.
echo 配置文件已创建: monitor\config.json
echo.
echo ================================
echo 重要: 需要手动修改以下内容
echo ================================
echo.
echo 1. 生成随机密钥
echo    - 方式A: 使用在线工具生成32位随机字符串
echo    - 方式B: 随便敲键盘32个字符 (字母+数字)
echo    - 示例: aB3dEf9hIj2kLm4nOp7qRs1tUv6wXy0z
echo.
echo 2. 修改 monitor\config.json
echo    找到: "secret_key": "change-this-to-random-secret-key"
echo    改为: "secret_key": "你生成的密钥"
echo.
echo 3. 修改服务器配置 plugins\NoobHubPay\config.yml
echo    找到: secret-key: "change-this-to-random-secret-key"
echo    改为: secret-key: "同样的密钥"
echo.
echo 4. 调整微信窗口配置 (在config.json)
echo    - window_title: 你的微信通知窗口标题
echo    - fields: 根据实际窗口内容调整
echo.
echo 5. 调整支付宝配置 (如需要)
echo.

echo ================================
echo 快速测试命令
echo ================================
echo.
echo 游戏内测试:
echo   /payadmin status
echo   /payadmin test 64个钻石块
echo.
echo Python接口测试:
echo   python test_api.py
echo.
echo 启动监控:
echo   start.bat
echo.

notepad "config.json"

echo.
echo 配置文件已在记事本中打开
echo 请按照上面的说明修改配置
echo.
pause
