@echo off
chcp 65001 > nul
echo ================================
echo NoobHubPay 编译部署工具
echo ================================
echo.

cd /d "%~dp0plugin"

echo [1/3] 清理旧文件...
call mvn clean -q

echo [2/3] 编译插件...
call mvn package -DskipTests -q
if %errorlevel% neq 0 (
    echo.
    echo [错误] 编译失败! 请检查代码
    pause
    exit /b 1
)

echo [3/3] 部署到服务器...
copy /Y "target\NoobHubPay-1.0.0.jar" "D:\project_collection\code_project\NoobHub\plugins\NoobHubPay.jar" > nul
if %errorlevel% neq 0 (
    echo.
    echo [错误] 部署失败! 请检查路径
    pause
    exit /b 1
)

echo.
echo ================================
echo ✓ 编译部署成功!
echo ================================
echo.
echo 生成文件: D:\project_collection\code_project\NoobHub\plugins\NoobHubPay.jar
echo.
echo 下一步:
echo   1. 重启Minecraft服务器
echo   2. 检查插件加载: /plugins
echo   3. 查看状态: /payadmin status
echo.

pause
