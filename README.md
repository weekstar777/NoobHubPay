# NoobHubPay - Minecraft多方式支付插件

支持微信/支付宝/游戏货币三种支付方式的自动发货插件。

## 快速开始

### 1. 配置

```bash
双击运行: setup.bat
```

会生成随机密钥并创建配置文件。

**然后编辑两个文件,填入相同的密钥**:
- `monitor\config.json` → `secret_key`
- `plugins\NoobHubPay\config.yml` → `secret-key`

### 2. 安装依赖

**服务器端**:
- Vault插件 (必需)
- Essentials或其他经济插件

**监控程序**:
```bash
cd monitor
pip install -r requirements.txt
```

首次运行会下载OCR模型(约200MB)。

### 3. 启动

1. 重启Minecraft服务器(加载插件)
2. 运行监控: `cd monitor && start.bat`

## 使用方式

### 微信/支付宝支付

1. 玩家转账时备注: `玩家ID 商品名称`
2. 收款通知弹出
3. 监控程序自动识别并发货

### 游戏货币支付

```
/payadmin test 商品名称
```

### 管理命令

- `/payadmin status` - 查看状态
- `/payadmin list` - 商品列表
- `/payadmin reload` - 重载配置

## 配置商品

编辑 `plugins/NoobHubPay/config.yml`:

```yaml
products:
  - name: "64个钻石块"
    price: 68.00
    commands:
      - "/give {player} minecraft:diamond_block 64"
```

## 常见问题

### setup.bat报错
手动操作:
1. 复制 `monitor/config.json.example` → `config.json`
2. 随便输入32位字符作为密钥
3. 两个配置文件使用相同密钥

### OCR识别不准
- 降低 `config.json` 中的 `confidence_threshold`
- 确保窗口完整显示
- 检查窗口标题是否匹配

### 支付未发货
1. 玩家是否在线? `/list`
2. 商品是否存在? `/payadmin list`
3. 查看日志: `logs/latest.log` 和 `monitor/payment_monitor.log`

## 编译部署

```bash
build.bat
```

自动编译并部署到服务器plugins目录。

---

**版本**: v1.0.1 | **协议**: MIT
