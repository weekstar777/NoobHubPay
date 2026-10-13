# 配置工具使用说明

## 问题: init-config.bat运行不成功?

不用担心,提供了3种配置方式,任选其一:

---

## 方式1: 自动配置 (推荐)

**脚本**: `init-config.bat`

**功能**: 自动复制配置文件 + 生成随机密钥

**使用步骤**:
```
1. 双击运行 init-config.bat
2. 如果提示已存在配置,输入 Y 覆盖
3. 等待完成,会显示生成的密钥
4. 复制密钥,按照提示修改配置文件
```

**如果失败**: 尝试方式2或方式3

---

## 方式2: 快速配置 (最简单)

**脚本**: `quick-setup.bat`

**功能**: 只复制配置文件,不生成密钥

**使用步骤**:
```
1. 双击运行 quick-setup.bat
2. 自动打开记事本显示 config.json
3. 找到这行: "secret_key": "change-this-to-random-secret-key"
4. 改为: "secret_key": "你的随机密钥"
5. 保存关闭
```

**如何生成密钥**:
- 随便敲键盘32个字符(字母+数字)
- 例如: `aB3dEf9hIj2kLm4nOp7qRs1tUv6wXy0z`
- 或使用在线工具: https://www.random.org/strings/

---

## 方式3: 手动配置 (最可靠)

**脚本**: `setup-manual.bat`

**功能**: 详细的手动配置向导

**使用步骤**:
```
1. 双击运行 setup-manual.bat
2. 按照屏幕上的详细说明操作
3. 会自动打开配置文件供编辑
```

---

## 纯手动配置 (不用脚本)

如果所有脚本都失败,完全手动操作:

### 步骤1: 复制配置文件
```
复制: monitor\config.json.example
粘贴为: monitor\config.json
```

### 步骤2: 编辑配置
用记事本打开 `monitor\config.json`

找到:
```json
"secret_key": "change-this-to-random-secret-key"
```

改为:
```json
"secret_key": "aB3dEf9hIj2kLm4nOp7qRs1tUv6wXy0z"
```
(换成你自己的随机字符串)

### 步骤3: 同步服务器配置
编辑 `plugins\NoobHubPay\config.yml`

找到:
```yaml
secret-key: "change-this-to-random-secret-key"
```

改为:
```yaml
secret-key: "aB3dEf9hIj2kLm4nOp7qRs1tUv6wXy0z"
```
(使用相同的密钥!)

### 完成!

---

## 故障排查

### 问题: 脚本双击没反应

**原因**: 可能路径或权限问题

**解决**:
1. 右键脚本 → "以管理员身份运行"
2. 或者用命令行运行:
   ```
   cd D:\project_collection\code_project\NoobHubPay
   init-config.bat
   ```

---

### 问题: 脚本闪退

**诊断**:
1. 运行 `diagnose.bat` 查看环境状态
2. 检查输出找到具体错误

**常见原因**:
- PowerShell被禁用
- 文件不存在
- 路径错误

---

### 问题: 找不到config.json.example

**确认**:
运行 `diagnose.bat` 检查文件

**如果确实不存在**:
手动创建 `monitor\config.json` 内容如下:

```json
{
    "http_server": {
        "url": "http://127.0.0.1:8787/payment",
        "secret_key": "your-secret-key-here"
    },
    "wechat": {
        "enabled": true,
        "window_title": "服务通知",
        "fields": {
            "收款金额": "amount",
            "收款项": "product",
            "付款方信息": "player"
        },
        "check_interval": 2.0
    },
    "alipay": {
        "enabled": true,
        "window_title": "支付宝通知",
        "fields": {
            "收款金额": "amount",
            "收款项": "product",
            "付款方信息": "player"
        },
        "check_interval": 2.0
    },
    "ocr": {
        "engine": "easyocr",
        "languages": ["ch_sim", "en"],
        "confidence_threshold": 0.6
    },
    "log": {
        "level": "INFO",
        "file": "payment_monitor.log"
    }
}
```

---

## 配置检查清单

完成配置后,检查:

- [ ] `monitor\config.json` 已创建
- [ ] 密钥已修改(不是默认值)
- [ ] `plugins\NoobHubPay\config.yml` 密钥已同步
- [ ] 两个密钥完全一致
- [ ] 窗口标题根据实际情况调整

全部完成后,继续 TESTING.md 进行测试!

---

## 工具对比

| 工具 | 难度 | 自动化程度 | 适用场景 |
|------|------|-----------|---------|
| init-config.bat | 简单 | 高(自动生成密钥) | 首次部署,推荐 |
| quick-setup.bat | 最简单 | 中(需手动输入密钥) | init失败时备用 |
| setup-manual.bat | 简单 | 低(详细向导) | 想了解每一步 |
| 纯手动 | 中等 | 无 | 所有脚本都失败 |
| diagnose.bat | - | - | 排查环境问题 |

---

## 推荐流程

```
首次部署 → init-config.bat
     ↓ (失败)
quick-setup.bat
     ↓ (还是有问题)
运行 diagnose.bat 查看问题
     ↓
根据诊断结果修复
     ↓
setup-manual.bat (最后手段)
     ↓ (还是不行)
纯手动操作
```

需要帮助?查看诊断工具输出!
