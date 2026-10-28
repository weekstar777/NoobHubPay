# NoobHubPay

Minecraft服务器支付系统 - 微信/支付宝OCR识别 + 自动发货

## 🏗️ 系统架构

```
微信/支付宝收款通知
      ↓
Monitor端 (Python OCR)
      ↓
Plugin端 (Minecraft)
      ↓
自动发货到玩家
```

## ✨ 功能特性

**Monitor端（Python）:**
- EasyOCR文字识别
- 实时监控支付窗口
- 提取金额和玩家名
- 自动发送到服务器

**Plugin端（Java）:**
- HTTP服务器接收通知
- 按金额匹配商品
- 智能玩家匹配（精确/模糊）
- 在线立即发货，离线保存订单
- 上线自动发货

**扩展功能:**
- 现金红包兑换
- 手机号绑定
- 二维码商店
- GUI商店界面

## 🚀 快速开始

### Monitor端

**1. 安装依赖:**
```bash
cd monitor
pip install -r requirements.txt
```

**2. 配置坐标:**

使用辅助工具确定识别区域:
```bash
python coordinate_helper.py "微信支付"
```

查看生成的 `window_grid.png`，找到区域坐标。

更新 `config.json`:
```json
{
  "wechat": {
    "window_title": "微信支付",
    "regions": {
      "收款金额": {"x": 60, "y": 185, "width": 270, "height": 80},
      "备注信息": {"x": 140, "y": 325, "width": 200, "height": 30}
    }
  },
  "server": {
    "url": "http://127.0.0.1:8787/payment"
  }
}
```

**3. 启动:**
```bash
start.bat
```

### Plugin端

**1. 编译:**
```bash
cd plugin
mvn clean package
```

**2. 安装:**

复制 `target/NoobHubPay-1.0.0.jar` 到服务器 `plugins/` 目录。

**3. 配置商品:**

编辑 `plugins/NoobHubPay/config.yml`:
```yaml
http:
  port: 8787
  enabled: true

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
    description: "§61个月VIP会员"
```

重启服务器。

## 💰 使用流程

1. 玩家微信转账指定金额（如¥68.00）
2. 备注填写游戏ID（如"dream"）
3. Monitor识别金额和玩家名
4. 发送到Plugin HTTP服务器
5. Plugin匹配商品和玩家
6. 自动执行发货命令

## ⚙️ 配置说明

### 坐标配置

坐标是**相对于窗口客户区**的偏移量。

使用 `coordinate_helper.py` 生成网格图，对照调整。

### 商品配置

**规则:**
- 每个商品价格必须唯一
- 价格精确到2位小数
- 命令中使用 `{player}` 作为玩家名变量

### 玩家匹配

Plugin按以下顺序匹配:
1. 精确匹配
2. 忽略大小写
3. 包含匹配（模糊）

匹配范围: 所有已注册玩家（在线+离线）

## 🎯 命令

**玩家:**
```bash
/store                  # 打开商店GUI
/bindphone <手机号>     # 绑定手机号
/cashback <金额>        # 兑换现金红包
```

**管理员:**
```bash
/payadmin reload        # 重载配置
/payadmin status        # 查看系统状态
/payadmin list          # 查看商品列表
/payadmin test <金额> <玩家>  # 测试支付流程
```

## 📦 依赖

**Monitor端:**
- Python 3.8+
- Windows 10/11
- 微信PC版 或 支付宝PC版

**Plugin端:**
- Paper 1.21.1+
- Java 17+
- Vault + Essentials

## 🐛 故障排查

**1. Monitor连接失败**

检查:
- 服务器是否启动
- 日志是否有 `HTTP server started on port 8787`
- 防火墙是否拦截端口

**2. OCR识别失败**

启用调试模式:
```json
"debug": {"enabled": true}
```

查看 `debug_screenshots/` 中截图，调整坐标。

**3. 商品匹配失败**

确认配置文件中有对应价格的商品。

**4. 玩家匹配失败**

确认玩家至少登录过一次服务器。

## 📁 项目结构

```
NoobHubPay/
├── monitor/               # Python Monitor端
│   ├── payment_monitor.py
│   ├── coordinate_helper.py
│   ├── config.json
│   └── requirements.txt
├── plugin/                # Java Plugin端
│   ├── src/main/java/
│   └── pom.xml
├── build.bat
└── README.md
```

## 📝 许可证

MIT License

---

NoobHub 无政府服务器 | 端口 1723
