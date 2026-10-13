@echo off
:: PowerShell Script Launcher
:: Bypasses execution policy restrictions

powershell -ExecutionPolicy Bypass -File "%~dp0init-config.ps1"

pause
