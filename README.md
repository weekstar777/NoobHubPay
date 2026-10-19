# NoobHubPay

<div align="center">

**Minecraft服务器支付系统**

微信/支付宝支付OCR识别 + Minecraft自动发货系统，支持离线订单、智能玩家匹配、现金红包兑换

[![Python](https://img.shields.io/badge/Python-3.8+-blue.svg)](https://www.python.org/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

</div>

---

## 📋 目录

- [系统架构](#-系统架构)
- [核心功能](#-核心功能)
- [快速开始](#-快速开始)
- [Monitor端配置](#-monitor端配置)
- [Plugin端配置](#-plugin端配置)
- [商品管理](#-商品管理)
- [故障排查](#-故障排查)
- [技术说明](#-技术说明)

## 🏗️ 系统架构

```
┌──────────────┐       ┌───────────────┐       ┌─────────────────┐
│              │       │               │       │                 │
│  微信/支付宝  │ ───▶  │  Monitor端    │ ───▶  │  Plugin端       │
│  收款通知    │       │  (Python OCR) │       │  (Minecraft)    │
│              │       │               │       │                 │
└──────────────┘       └───────────────┘       └─────────────────┘
      实时截图              识别金额+玩家名            自动发货到玩家
```

### 工作流程

1. **玩家支付**: 微信转账指定金额，备注填写游戏ID
2. **OCR识别**: Monitor监控支付窗口，提取金额和玩家名
3. **HTTP通信**: Monitor发送数据到Plugin的HTTP服务器
4. **智能匹配**: Plugin匹配商品（按金额）和玩家（模糊匹配）
5. **自动发货**: 在线立即发货，离线存储订单，上线后自动发货

## ✨ 核心功能

### 💰 支付识别
- **多平台支持** - 微信支付、支付宝双通道
- **实时OCR识别** - EasyOCR高精度文字识别
- **智能区域定位** - 可视化坐标配置工具
- **调试模式** - 实时截图保存，便于调试坐标

### 🎮 游戏发货
- **按金额匹配商品** - 无需复杂订单号，支付金额即商品ID
- **智能玩家匹配** - 支持精确/模糊/忽略大小写匹配
- **离线订单系统** - 玩家不在线时保存订单，上线自动发货
- **命令执行** - 支持任意Minecraft命令（发放物品/权限/称号等）

### 🔥 特色功能
- **现金红包兑换** - 玩家可将游戏币兑换成现金红包
- **手机号绑定** - 绑定手机号后才能兑换现金
- **二维码商店** - 生成专属支付二维码，地图物品展示
- **GUI商店系统** - `/store` 命令打开可视化商店界面

## 🚀 快速开始

### 系统要求

**Monitor端（客户端机器）:**
- Windows 10/11
- Python 3.8+
- 微信PC版 或 支付宝PC版
- 至少4GB可用内存（EasyOCR模型）

**Plugin端（服务器）:**
- Paper 1.21.1+
- Java 17+
- Vault + 经济插件（Essentials）

### 第一步：安装Monitor端

```bash
cd monitor
pip install -r requirements.txt
```

**依赖项:**
- `easyocr` - OCR文字识别引擎
- `pillow` - 图像处理
- `pywin32` - Windows窗口API
- `requests` - HTTP通信

### 第二步：配置坐标

使用坐标辅助工具确定识别区域:

```bash
python coordinate_helper.py "微信支付"
```

打开生成的 `window_grid.png`，找到以下区域的坐标:
- **收款金额区域** - 包含"￥XX.XX"的矩形
- **付款方信息区域** - 包含付款方备注的矩形

更新 `config.json`:
```json
{
  "wechat": {
    "window_title": "微信支付",
    "regions": {
      "收款金额": {
        "x": 60,
        "y": 185,
        "width": 270,
        "height": 80
      },
      "付款方信息": {
        "x": 140,
        "y": 325,
        "width": 200,
        "height": 30
      }
    }
  },
  "alipay": {
    "window_title": "支付宝",
    "regions": {
      "金额": {...},
      "付款人": {...}
    }
  }
}
```

### 第三步：启动Monitor

```bash
start.bat
```

Monitor会自动:
1. 加载EasyOCR模型（首次启动较慢）
2. 监控指定窗口
3. 识别到支付信息后发送到Plugin

### 第四步：编译Plugin

```bash
cd plugin
mvn clean package
```

将 `target/NoobHubPay-1.0.0.jar` 复制到服务器 `plugins/` 目录。

### 第五步：配置商品

编辑 `plugins/NoobHubPay/config.yml`:

```yaml
# HTTP服务器配置
http:
  port: 8787
  enabled: true

# 商品列表（每个价格必须唯一）
products:
  - name: "64个钻石块"
    price: 68.00
    commands:
      - "give {player} minecraft:diamond_block 64"
    description: "§6购买64个钻石块"
  
  - name: "1月VIP会员"
    price: 88.00
    commands:
      - "lp user {player} parent add vip"
      - "lp user {player} permission settemp vip.fly true 30d"
    description: "§61个月VIP会员 + 飞行权限"
  
  - name: "自定义称号"
    price: 128.00
    commands:
      - "nick {player} &6[土豪] &f{player}"
    description: "§e自定义土豪称号"
```

重启服务器即可。

## 🖥️ Monitor端配置

### config.json详解

```json
{
  // 微信支付配置
  "wechat": {
    "window_title": "微信支付",  // 窗口标题（精确匹配）
    "regions": {
      "收款金额": {
        "x": 60,       // 距离客户区左边距
        "y": 185,      // 距离客户区上边距
        "width": 270,  // 区域宽度
        "height": 80   // 区域高度
      },
      "付款方信息": {
        "x": 140,
        "y": 325,
        "width": 200,
        "height": 30
      }
    }
  },
  
  // 支付宝配置（同上）
  "alipay": {...},
  
  // 服务器地址
  "server": {
    "url": "http://127.0.0.1:8787/payment",  // Plugin的HTTP端点
    "timeout": 10
  },
  
  // 调试模式
  "debug": {
    "enabled": false,                    // 是否保存截图
    "output_dir": "debug_screenshots"    // 截图保存目录
  },
  
  // OCR设置
  "ocr": {
    "languages": ["ch_sim", "en"],  // 简体中文 + 英文
    "gpu": false                    // 是否使用GPU加速
  }
}
```

### 坐标配置技巧

1. **先启用调试模式**
```json
"debug": {"enabled": true}
```

2. **运行一次Monitor**，查看 `debug_screenshots/` 中的截图

3. **使用coordinate_helper.py生成网格**
```bash
python coordinate_helper.py "微信支付"
```

4. **对照网格图调整坐标**，确保完整覆盖文字区域

5. **关闭调试模式**（生产环境）

### 坐标系统说明

使用 `ClientToScreen` API获取窗口客户区坐标，配置的坐标是**相对于客户区**的，自动排除标题栏和边框。

## 🎮 Plugin端配置

### config.yml完整配置

```yaml
# HTTP服务器
http:
  port: 8787              # 监听端口
  enabled: true           # 是否启用HTTP服务
  bind_address: "0.0.0.0" # 绑定地址（0.0.0.0允许外网）

# 商品列表
products:
  - name: "商品名称"
    price: 68.00          # 价格（必须唯一，最多2位小数）
    commands:             # 发货命令列表
      - "give {player} item 64"
      - "lp user {player} permission set xxx"
    description: "商品描述"  # 显示在GUI中

# 现金红包系统
cashback:
  enabled: true
  exchange_rate: 0.8      # 游戏币→人民币汇率（1元游戏币=0.8元现金）
  min_amount: 10.0        # 最低兑换金额
  require_phone: true     # 是否需要绑定手机号

# 二维码商店
qrcode:
  enabled: true
  payment_url: "wxp://xxxxx"  # 微信支付链接

# GUI商店
store:
  enabled: true
  title: "§6§l商店"
```

### 玩家匹配规则

Plugin按以下优先级匹配玩家:

1. **精确匹配** - `dream` → UUID为`dream`的玩家
2. **忽略大小写** - `DREAM` → `dream`
3. **包含匹配** - `dream123` → 包含`dream`的玩家

匹配范围: **所有已注册玩家**（在线+离线，基于 `usercache.json`）

## 🛒 商品管理

### 添加商品

编辑 `plugins/NoobHubPay/config.yml`:

```yaml
products:
  - name: "新商品"
    price: 199.00  # 确保价格唯一
    commands:
      - "give {player} minecraft:elytra 1"
      - "give {player} minecraft:firework_rocket 64"
    description: "§b鞘翅 + 64烟花"
```

重载配置:
```bash
/payadmin reload
```

### 修改价格

**重要**: 修改价格后，玩家需要支付**新价格**，Monitor端**无需修改**。

```yaml
  - name: "VIP会员"
    price: 128.00  # 从88改为128
    commands:
      - "lp user {player} parent add vip"
```

### 删除商品

直接从配置文件中删除对应条目，重载即可。

### 商品命令变量

| 变量 | 说明 | 示例 |
|------|------|------|
| `{player}` | 玩家名 | `give {player} diamond 64` |
| `{amount}` | 支付金额 | `eco give {player} {amount}` |
| `{uuid}` | 玩家UUID | `lp user {uuid} permission set xxx` |

## 🐛 故障排查

### 问题1: Monitor连接失败

**现象:**
```
Connection failed: Cannot connect to http://127.0.0.1:8787/payment
```

**排查步骤:**
1. 确认服务器已启动
2. 查看服务器日志: `[NoobHubPay] HTTP server started on port 8787`
3. 检查防火墙是否拦截8787端口
4. 如果Monitor和服务器不在同一台机器，修改 `config.json` 中的URL为服务器IP

**解决:**
```json
{
  "server": {
    "url": "http://服务器IP:8787/payment"
  }
}
```

### 问题2: OCR识别失败

**现象:**
```
Region '收款金额': 
(识别结果为空)
```

**排查步骤:**
1. 启用调试模式，查看截图
```json
"debug": {"enabled": true}
```

2. 检查 `debug_screenshots/` 中的截图:
   - 区域是否完整覆盖文字？
   - 文字是否清晰？
   - 是否有背景干扰？

3. 调整坐标后重试

**解决:**
使用 `coordinate_helper.py` 重新确定坐标。

### 问题3: 商品匹配失败

**现象:**
```
[NoobHubPay] No product matched for amount: 68.0
```

**原因:**
- 配置文件中没有价格为68.00的商品
- 价格精度问题（如配置了68.001）

**解决:**
```yaml
products:
  - name: "钻石块"
    price: 68.00  # 精确到2位小数
```

### 问题4: 玩家匹配失败

**现象:**
```
[NoobHubPay] No registered player matched: abc123
```

**原因:**
- 玩家从未登录过服务器
- 付款方备注填写错误

**解决:**
1. 确认玩家至少登录过一次（生成UUID缓存）
2. 提醒玩家正确填写游戏ID
3. 支持模糊匹配: `dream` 可以匹配 `dream123`

### 问题5: 离线订单未发货

**现象:**
玩家上线后没有收到离线订单。

**排查:**
1. 检查 `plugins/NoobHubPay/pending_orders.json` 是否存在订单
2. 查看服务器日志是否有 `Processing X pending orders for player XXX`
3. 确认玩家UUID正确

**手动清理失败订单:**
编辑 `pending_orders.json`，删除对应条目，或使用:
```bash
/payadmin orders clear <玩家>
```

## 🎯 命令与权限

### 玩家命令

| 命令 | 说明 |
|------|------|
| `/store` | 打开商店GUI |
| `/bindphone <手机号>` | 绑定手机号 |
| `/cashback <金额>` | 兑换现金红包 |
| `/qrcode` | 获取支付二维码地图物品 |

### 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/payadmin reload` | `pay.admin` | 重载配置 |
| `/payadmin status` | `pay.admin` | 查看系统状态 |
| `/payadmin list` | `pay.admin` | 查看商品列表 |
| `/payadmin orders <玩家>` | `pay.admin` | 查看玩家离线订单 |
| `/payadmin test <金额> <玩家>` | `pay.admin` | 测试支付流程 |

## 🔧 技术说明

### 坐标系统

使用Windows API `ClientToScreen` 获取窗口客户区坐标:

```python
import win32gui

hwnd = win32gui.FindWindow(None, "微信支付")
rect = win32gui.GetClientRect(hwnd)  # 客户区尺寸
left, top = win32gui.ClientToScreen(hwnd, (0, 0))  # 客户区左上角屏幕坐标
```

配置的坐标是**相对于客户区左上角**的偏移量，避免标题栏和边框的影响。

### OCR引擎

使用 **EasyOCR** 进行文字识别:

```python
import easyocr

reader = easyocr.Reader(['ch_sim', 'en'], gpu=False)
result = reader.readtext(image)
```

**优势:**
- 支持多语言混合识别
- 无需额外训练，开箱即用
- CPU模式即可达到实用精度

### HTTP通信协议

**请求格式 (Monitor → Plugin):**
```json
POST /payment
Content-Type: application/json

{
  "amount": 68.00,
  "player": "dream",
  "platform": "wechat",
  "timestamp": 1729350000
}
```

**响应格式 (Plugin → Monitor):**
```json
{
  "success": true,
  "message": "Payment processed successfully",
  "product": "64个钻石块",
  "player_uuid": "b04851e5-a7f0-3e14-a856-2eff1e77a3c7"
}
```

### 离线订单存储

保存在 `pending_orders.json`:

```json
{
  "b04851e5-a7f0-3e14-a856-2eff1e77a3c7": [
    {
      "product": "64个钻石块",
      "amount": 68.00,
      "timestamp": 1729350000,
      "platform": "wechat"
    }
  ]
}
```

玩家登录时触发 `PlayerJoinEvent`，检查并发货。

## 🏗️ 项目结构

```
NoobHubPay/
├── monitor/                    # Python Monitor端
│   ├── main.py                 # 主程序
│   ├── config.json             # 配置文件
│   ├── coordinate_helper.py    # 坐标辅助工具
│   ├── requirements.txt        # Python依赖
│   ├── start.bat               # 启动脚本
│   └── 兑换数据.txt             # 兑换记录（不提交）
│
├── plugin/                     # Java Plugin端
│   ├── src/main/java/com/noobhub/pay/
│   │   ├── NoobHubPay.java     # 插件主类
│   │   ├── http/               # HTTP服务器
│   │   ├── commands/           # 命令处理
│   │   ├── managers/           # 业务逻辑
│   │   └── utils/              # 工具类
│   ├── src/main/resources/
│   │   ├── config.yml          # 默认配置
│   │   └── plugin.yml
│   └── pom.xml
│
└── README.md
```

## 📝 更新日志

### v1.2.0 (2024-10-19)
- ✨ 添加现金红包兑换功能
- 📱 添加手机号绑定系统
- 🛒 添加GUI商店系统
- 🔧 优化玩家匹配算法

### v1.1.0 (2024-10-15)
- 🎯 添加二维码地图物品功能
- 🐛 修复离线订单不显示bug
- 📊 优化日志输出

### v1.0.0 (2024-10-10)
- 🎉 初始版本发布
- 💰 微信/支付宝OCR识别
- 🎮 自动发货系统
- 📦 离线订单功能

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议

---

<div align="center">

**NoobHub 无政府服务器** | 端口 1723

Made with ❤️ by weekstar777

[问题反馈](https://github.com/weekstar777/NoobHubPay/issues) • [功能建议](https://github.com/weekstar777/NoobHubPay/discussions)

</div>
