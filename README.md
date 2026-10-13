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

### 1. 初始化配置 (一键完成)

**运行配置工具**:
```bash
双击运行: setup.bat
```

会自动:
- 生成随机密钥 (例如: P6DYTyVt7RVVMV6gZaXxJCimb9jeVobE)
- 创建配置文件
- 打开记事本编辑

**手动修改两个配置**:
1. `monitor\config.json` → 修改 `secret_key`
2. `plugins\NoobHubPay\config.yml` → 修改 `secret-key`
   
⚠️ **重要**: 两个文件使用相同的密钥!

### 2. 插件安装

**依赖插件**:
- Vault (必需)
- Essentials 或其他经济插件 (游戏货币支付需要)

**部署插件**:
- 插件已编译: `plugin/target/NoobHubPay-1.0.0.jar`
- 或运行 `build.bat` 自动编译并部署

### 3. 安装Python依赖

```bash
cd monitor
pip install -r requirements.txt
```

首次运行会自动下载OCR模型(约200MB),请耐心等待。

### 4. 启动

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

### 配置脚本无法运行

**推荐**: 使用 `setup.bat` (纯ASCII,无编码问题)

**备用**: `quick-setup.bat` 或手动复制 `monitor/config.json.example`

**诊断**: 运行 `diagnose.bat` 检查环境

### SLF4J日志错误

**症状**: `SLF4J: No SLF4J providers were found`

**原因**: Paper服务器已提供SLF4J实现,插件无需内置

**解决**: 已修复,使用服务器提供的SLF4J(v1.0.1+)

### HTTP服务器启动失败

**症状**: 插件加载失败或端口错误

**排查**:
- 检查端口8787是否被占用: `netstat -ano | findstr 8787`
- 修改config.yml中的port配置
- 确认防火墙未拦截

### OCR识别不准确

**症状**: 支付通知识别失败或匹配错误

**调整**:
- 降低 `config.json` 中的 `confidence_threshold` (默认0.6)
- 确保窗口完整可见,无遮挡
- 检查 `fields` 配置与实际窗口文字完全一致
- 查看 `payment_monitor.log` 确认OCR识别内容

### 支付未发货

**症状**: 收到支付但玩家未收到物品

**排查步骤**:
1. 确认玩家在线: `/list`
2. 检查商品名称匹配: `/payadmin list`
3. 验证金额是否一致
4. 查看服务器日志: `logs/latest.log`
5. 查看监控日志: `monitor/payment_monitor.log`

详细测试步骤查看 `TESTING.md`

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

### v1.0.1 (2026-10-13)
- 修复SLF4J日志冲突问题
- 使用Paper服务器提供的SLF4J实现
- 添加build.bat编译部署脚本
- 添加init-config.bat配置初始化工具
- 完善故障排除文档

### v1.0.0 (2026-10-13)
- 初始版本
- 支持微信/支付宝/游戏货币三种支付方式
- OCR自动识别收款信息
- 可配置商品和发货命令
