# NoobHubPay - 多方式支付插件

## 功能特性

支持三种支付方式:
1. **微信支付** - 通过监控PC端微信窗口,识别收款通知
2. **支付宝支付** - 通过监控PC端支付宝窗口,识别收款通知  
3. **游戏金额** - 使用Essentials+Vault扣除游戏内货币

### 工作原理

- **外部监控程序**(Python): 监控微信/支付宝窗口,OCR识别收款信息,通过HTTP发送到插件
- **Minecraft插件**(Java): 接收支付通知,验证订单,自动执行发货命令

## 快速开始

### 1. 插件安装

将 `plugin/target/NoobHubPay-1.0.0.jar` 复制到服务器的 `plugins` 目录

**依赖插件**:
- Vault (必需)
- Essentials 或其他经济插件 (游戏货币支付需要)

### 2. 配置插件

编辑 `plugins/NoobHubPay/config.yml`:

```yaml
http-server:
  secret-key: "修改为随机字符串"  # 重要!用于验证监控程序请求

products:
  - name: "64个钻石块"
    price: 68.00
    commands:
      - "/give {player} minecraft:diamond_block 64"
```

### 3. 配置监控程序

编辑 `monitor/config.json`:

```json
{
    "http_server": {
        "secret_key": "与插件config.yml保持一致"
    },
    "wechat": {
        "window_title": "服务通知",
        "fields": {
            "收款金额": "amount",
            "收款项": "product",
            "付款方信息": "player"
        }
    }
}
```

### 4. 安装Python依赖

```bash
cd monitor
pip install -r requirements.txt
```

首次运行会自动下载OCR模型(约200MB),请耐心等待。

### 5. 启动

1. 启动Minecraft服务器,插件会自动启动HTTP服务器(默认端口8787)
2. 运行监控程序: `python payment_monitor.py`

## 使用方法

### 微信/支付宝支付流程

1. 玩家在游戏内输入自己的游戏ID
2. 玩家微信/支付宝转账给服主
3. 在转账备注中填写: 玩家ID + 商品名称
4. 收款通知弹出后,监控程序自动识别
5. 插件自动为玩家发货

### 游戏货币支付

使用命令: `/payadmin test <商品名>`

## 管理命令

- `/payadmin reload` - 重新加载配置
- `/payadmin list` - 查看商品列表
- `/payadmin test <商品名>` - 测试购买(使用游戏货币)
- `/payadmin status` - 查看系统状态

## 配置项说明

### 商品配置

每个商品包含:
- `name`: 商品名称(用于匹配)
- `price`: 价格
- `commands`: 发货命令列表,支持变量 `{player}`
- `description`: 商品描述

### 窗口监控配置

- `window_title`: 窗口标题(微信默认"服务通知")
- `fields`: 字段映射
  - 键: OCR识别的文字(如"收款金额")  
  - 值: 字段类型(amount/product/player)
- `check_interval`: 检查间隔(秒)

## 常见问题

### 1. HTTP服务器启动失败

检查端口8787是否被占用,可在config.yml修改端口

### 2. OCR识别不准确

- 调整 `config.json` 中的 `confidence_threshold`
- 确保窗口内容清晰可见
- 检查 `fields` 配置是否与实际窗口文字匹配

### 3. 支付未发货

查看日志:
- 插件日志: `logs/latest.log`
- 监控程序日志: `monitor/payment_monitor.log`

## 技术架构

```
┌─────────────────┐         HTTP          ┌──────────────────┐
│  Python监控程序  │  ──────────────────>  │  Minecraft插件   │
│  - 窗口截图     │   POST /payment       │  - 接收通知      │
│  - OCR识别      │                       │  - 验证订单      │
│  - 解析键值对   │                       │  - 执行命令      │
└─────────────────┘                       └──────────────────┘
       ↑                                           ↓
       │                                    执行发货命令
       │                                           ↓
  ┌────────┐                            ┌──────────────┐
  │ 微信窗口│                            │  游戏服务器  │
  └────────┘                            └──────────────┘
```

## 安全建议

1. 修改默认 `secret-key` 为强随机字符串
2. 仅绑定本地地址 `127.0.0.1` (默认配置)
3. 定期检查 `payment_monitor.log` 异常记录
4. 为管理员命令设置合适的权限

## 开发信息

- 版本: 1.0.0
- 协议: MIT
- 依赖:
  - Paper API 1.21.1
  - Vault API 1.7
  - Javalin 5.6.3 (HTTP服务器)
  - EasyOCR (OCR识别)

## 更新日志

### v1.0.0 (2026-10-13)
- 初始版本
- 支持微信/支付宝/游戏货币三种支付方式
- OCR自动识别收款信息
- 可配置商品和发货命令
