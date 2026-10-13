# Python Dependencies Installation Guide

## Problem

If `start.bat` fails with pip error, install manually.

## Solution

Open Command Prompt (cmd) and run:

```bash
cd D:\project_collection\code_project\NoobHubPay\monitor
python -m pip install -r requirements.txt
```

## Wait Time

- First time: ~5-10 minutes (downloading packages)
- First run of monitor: ~5 minutes (downloading OCR models ~200MB)

## What will be installed

- easyocr - OCR recognition engine
- pillow - Image processing
- pywin32 - Windows API
- mss - Screenshot tool
- requests - HTTP client

## After Installation

Run the monitor:
```bash
python payment_monitor.py
```

Or just double-click `start.bat` again.

## Troubleshooting

### "pip not found"
```bash
python -m ensurepip
python -m pip install --upgrade pip
```

### "Permission denied"
Run Command Prompt as Administrator

### Still failing?
Install packages one by one:
```bash
python -m pip install easyocr
python -m pip install pillow
python -m pip install pywin32
python -m pip install mss
python -m pip install requests
```
