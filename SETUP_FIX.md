# Configuration Setup - Encoding Issue Fixed!

## Problem: Chinese encoding causing init-config.bat to fail

## ✅ Solution: Use Pure English Scripts

---

## Recommended: Use setup.bat

**The simplest and most reliable option:**

```bash
Double-click: setup.bat
```

**Features:**
- Pure ASCII, no encoding issues
- Auto-generates 32-char random key
- Opens config file in Notepad automatically
- Works on all Windows versions

---

## Alternative Options

### Option 1: init-config-en.bat
Pure English version of init-config.bat

```bash
Double-click: init-config-en.bat
```

### Option 2: PowerShell Script (Best for Chinese support)
Use run-init.bat to launch PowerShell script

```bash
Double-click: run-init.bat
```

This runs `init-config.ps1` which has perfect Unicode support.

### Option 3: quick-setup.bat
Simplest - just copies file, no key generation

```bash
Double-click: quick-setup.bat
```

---

## Complete Manual Setup (No scripts needed)

If all scripts fail:

### Step 1: Copy template
```
Copy: monitor\config.json.example
Paste as: monitor\config.json
```

### Step 2: Generate a random key
Type 32 random characters (letters + numbers)
Example: `aB3dEf9hIj2kLm4nOp7qRs1tUv6wXy0z`

Or use online tool: https://www.random.org/strings/

### Step 3: Edit monitor\config.json
Open with Notepad and find:
```json
"secret_key": "change-this-to-random-secret-key"
```

Change to:
```json
"secret_key": "aB3dEf9hIj2kLm4nOp7qRs1tUv6wXy0z"
```
(use your own random key)

### Step 4: Edit plugins\NoobHubPay\config.yml
Find:
```yaml
secret-key: "change-this-to-random-secret-key"
```

Change to:
```yaml
secret-key: "aB3dEf9hIj2kLm4nOp7qRs1tUv6wXy0z"
```
**Important: Use THE SAME key in both files!**

### Step 5: Done!
Restart Minecraft server and test with `/payadmin status`

---

## Script Comparison

| Script | Encoding | Chinese Support | Recommended |
|--------|----------|----------------|-------------|
| **setup.bat** | ASCII | N/A | ⭐ Best choice |
| init-config-en.bat | ASCII | N/A | Good |
| run-init.bat → .ps1 | UTF-8 | Yes | Good for Chinese Windows |
| quick-setup.bat | ASCII | N/A | Simplest |
| Manual | N/A | N/A | 100% reliable |

---

## Quick Start (Recommended Path)

```
1. Run: setup.bat
   ↓
2. Copy the generated key
   ↓
3. Edit config.json (will open automatically)
   ↓
4. Edit config.yml with same key
   ↓
5. Done!
```

---

## Troubleshooting

### "PowerShell not found"
Use **setup.bat** instead - doesn't require PowerShell

### "Access denied"
Right-click script → Run as Administrator

### "File not found"
Run **diagnose.bat** to check environment

### Still not working?
Use the **Complete Manual Setup** above - always works!

---

## File Encoding Reference

If you want to edit the scripts:

- `.bat` files: Save as **ANSI** (not UTF-8)
- `.ps1` files: Save as **UTF-8 with BOM**
- `.json` files: Save as **UTF-8** (no BOM)
- `.yml` files: Save as **UTF-8** (no BOM)

---

## Need Help?

1. Run `diagnose.bat` first
2. Try `setup.bat` - most reliable
3. If fails, use manual setup
4. Check CONFIG_GUIDE.md for more details
