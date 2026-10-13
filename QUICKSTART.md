# NoobHubPay - Quick Start Guide

## Your Secret Key (from setup.bat)

```
P6DYTyVt7RVVMV6gZaXxJCimb9jeVobE
```

---

## Step-by-Step Setup

### 1. ✅ Configuration (DONE)
You've already run `setup.bat` successfully!

### 2. Edit Config Files

**File 1: monitor\config.json**
- Open with Notepad
- Find: `"secret_key": "change-this-to-random-secret-key"`
- Change to: `"secret_key": "P6DYTyVt7RVVMV6gZaXxJCimb9jeVobE"`
- Save

**File 2: plugins\NoobHubPay\config.yml**
- Find: `secret-key: "change-this-to-random-secret-key"`
- Change to: `secret-key: "P6DYTyVt7RVVMV6gZaXxJCimb9jeVobE"`
- Save

⚠️ **Important**: Both files MUST use the SAME key!

### 3. Install Python Dependencies

```bash
cd monitor
pip install -r requirements.txt
```

First run will download OCR models (~200MB), be patient.

### 4. Restart Minecraft Server

The plugin is already deployed at:
```
D:\project_collection\code_project\NoobHub\plugins\NoobHubPay.jar
```

Restart your server to load it.

### 5. Test Plugin

In-game commands:
```
/plugins
/payadmin status
/payadmin list
/payadmin test 64个钻石块
```

### 6. Start Monitor

```bash
cd monitor
start.bat
```

Or: `python payment_monitor.py`

---

## Quick Test

### Test 1: In-game Currency
```
/eco give <player> 100
/payadmin test 64个钻石块
```

### Test 2: HTTP API
```bash
cd monitor
python test_api.py
```

---

## Available Tools

| Tool | Purpose |
|------|---------|
| **setup.bat** | Generate config & key (✅ done) |
| **build.bat** | Compile & deploy plugin |
| **diagnose.bat** | Check environment |

---

## Troubleshooting

**Problem**: OCR not accurate
- Lower `confidence_threshold` in `config.json` (0.6 → 0.4)
- Check window title matches exactly

**Problem**: Payment not delivered
1. Check player is online: `/list`
2. Check product exists: `/payadmin list`
3. Check logs: `logs/latest.log`

**More**: See `TESTING.md` for detailed test guide

---

## Next Steps

1. Edit the two config files with your key ✓
2. Restart server
3. Test with `/payadmin status`
4. Start monitor program
5. Test a real payment!

Good luck! 🎉
