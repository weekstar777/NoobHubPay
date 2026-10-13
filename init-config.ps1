# NoobHubPay Configuration Setup Script
# PowerShell version - Better Unicode support

Write-Host "================================" -ForegroundColor Cyan
Write-Host "NoobHubPay 配置初始化" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Change to script directory
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath

# Check monitor directory
if (-not (Test-Path "monitor")) {
    Write-Host "[错误] monitor目录不存在!" -ForegroundColor Red
    Write-Host "当前目录: $(Get-Location)"
    pause
    exit 1
}

Set-Location "monitor"

# Check template file
if (-not (Test-Path "config.json.example")) {
    Write-Host "[错误] 配置模板文件不存在: config.json.example" -ForegroundColor Red
    Write-Host "当前目录: $(Get-Location)"
    Get-ChildItem
    pause
    exit 1
}

# Check if config already exists
if (Test-Path "config.json") {
    Write-Host "[警告] config.json 已存在!" -ForegroundColor Yellow
    Write-Host ""
    $confirm = Read-Host "是否覆盖? (Y/N)"
    if ($confirm -ne "Y" -and $confirm -ne "y") {
        Write-Host "已取消" -ForegroundColor Gray
        pause
        exit 0
    }
}

Write-Host ""
Write-Host "[1/2] 复制配置模板..." -ForegroundColor Green
Copy-Item "config.json.example" "config.json" -Force
Write-Host "完成!" -ForegroundColor Green

Write-Host "[2/2] 生成随机密钥..." -ForegroundColor Green
$chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
$secretKey = -join ((1..32) | ForEach-Object { $chars[(Get-Random -Maximum $chars.Length)] })
Write-Host "完成!" -ForegroundColor Green

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "✓ 配置文件已创建!" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "文件位置: $(Get-Location)\config.json"
Write-Host ""
Write-Host "生成的随机密钥:" -ForegroundColor Yellow
Write-Host $secretKey -ForegroundColor White
Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "下一步操作:" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. 编辑配置文件: monitor\config.json"
Write-Host "   找到: `"secret_key`": `"change-this-to-random-secret-key`""
Write-Host "   替换为: `"secret_key`": `"$secretKey`""
Write-Host ""
Write-Host "2. 同步修改服务器配置: plugins\NoobHubPay\config.yml"
Write-Host "   找到: secret-key: `"change-this-to-random-secret-key`""
Write-Host "   替换为: secret-key: `"$secretKey`""
Write-Host ""
Write-Host "3. 调整窗口标题和字段映射(根据实际情况)"
Write-Host ""
Write-Host "4. 重启Minecraft服务器"
Write-Host ""

# Ask if want to open config file
$openFile = Read-Host "是否现在打开配置文件进行编辑? (Y/N)"
if ($openFile -eq "Y" -or $openFile -eq "y") {
    notepad "config.json"
}

Write-Host ""
Write-Host "按任意键退出..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
