@echo off
:: 最简单的配置初始化脚本
echo 复制配置文件...

cd /d "%~dp0monitor"

if exist "config.json" (
    echo config.json 已存在,备份为 config.json.old
    copy /Y "config.json" "config.json.old" > nul
)

copy /Y "config.json.example" "config.json"

echo.
echo 完成! 配置文件已创建: monitor\config.json
echo.
echo 下一步: 用记事本打开 monitor\config.json
echo 修改 secret_key 为随机字符串
echo.

notepad "config.json"

pause
