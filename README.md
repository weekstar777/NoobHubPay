# NoobHubPay - Minecraft支付系统

微信/支付宝支付通知OCR识别，自动发货到Minecraft服务器。

## 核心特性

- **按金额匹配商品**：只需支付对应金额，自动匹配商品
- **离线发货**：玩家离线时存储订单，上线自动发货
- **智能玩家匹配**：根据服务器已注册玩家自动匹配
- **仅需修改Plugin配置**：增删商品只需修改Plugin的config.yml

## 快速开始

### 1. Monitor端（Python）

**安装依赖：**
```bash
cd monitor
pip install -r requirements.txt
```

**配置坐标：**

使用坐标辅助工具确定区域位置：
```bash
python coordinate_helper.py "微信支付"
```

打开生成的 `window_grid.png`，找到要识别的区域坐标，更新 `config.json`：
```json
{
  "wechat": {
    "window_title": "微信支付",
    "regions": {
      "收款金额": {"x": 60, "y": 185, "width": 270, "height": 80},
      "付款方信息": {"x": 140, "y": 325, "width": 200, "height": 30}
    }
  }
}
```

**启动Monitor：**
```bash
start.bat
```

### 2. Plugin端（Java）

**编译插件：**
```bash
cd plugin
mvn clean package
```

**安装插件：**
```bash
cp target/NoobHubPay-1.0.0.jar <服务器路径>/plugins/
```

**配置商品：**

编辑 `plugins/NoobHubPay/config.yml`：
```yaml
# 每个商品价格必须唯一
products:
  - name: "64个钻石块"
    price: 68.00
    commands:
      - "/give {player} minecraft:diamond_block 64"
    description: "§6购买64个钻石块"

  - name: "1月VIP会员"
    price: 88.00
    commands:
      - "/lp user {player} parent add vip"
    description: "§61个月VIP会员"
```

重启服务器即可。

## 使用流程

### 玩家支付

1. 微信转账对应金额（如¥68.00）
2. 付款方备注填写游戏ID（如"dream"）
3. 系统自动识别并发货

### 工作原理

```
微信收款通知
    ↓
Monitor OCR识别（金额+玩家名）
    ↓
发送到Plugin HTTP服务器
    ↓
Plugin根据金额匹配商品
    ↓
Plugin根据玩家名匹配已注册玩家
    ↓
在线 → 直接发货
离线 → 存储订单，上线时自动发货
```

## 添加/修改商品

**只需修改Plugin配置：**

1. 编辑 `plugins/NoobHubPay/config.yml`
2. 添加商品，确保价格唯一：
```yaml
  - name: "新商品"
    price: 128.00  # 价格必须唯一
    commands:
      - "/give {player} minecraft:item 1"
    description: "§a新商品"
```
3. 执行 `/payadmin reload` 或重启服务器
4. **Monitor端无需修改**

## 调试模式

启用调试模式会保存每次截图：

```json
{
  "debug": {
    "enabled": true,
    "output_dir": "debug_screenshots"
  }
}
```

用于确认：
1. 坐标是否正确
2. OCR能否识别文字

调试完成后改为 `"enabled": false`。

## 故障排查

### 1. 连接失败

**Monitor日志：**
```
Connection failed: Cannot connect to http://127.0.0.1:8787/payment
```

**解决：**
- 确认服务器已启动
- 确认HTTP服务器端口8787未被占用
- 查看服务器日志确认Plugin加载成功

### 2. 识别失败

**Monitor日志：**
```
Region '收款金额': 
```

**解决：**
1. 启用调试模式查看截图
2. 使用 `coordinate_helper.py` 调整坐标
3. 确保区域完整包含文字

### 3. 商品匹配失败

**Plugin日志：**
```
No product matched for amount: 68.0
```

**解决：**
- 确认config.yml中有对应价格的商品
- 价格必须完全匹配（误差<0.01）

### 4. 玩家匹配失败

**Plugin日志：**
```
No registered player matched: dream
```

**解决：**
- 确认玩家至少登录过一次服务器
- 支付时付款方备注填写正确的游戏ID
- 系统支持模糊匹配（包含匹配）

## 管理命令

```
/payadmin status  - 查看系统状态
/payadmin list    - 查看商品列表
/payadmin reload  - 重载配置
```

## 离线发货说明

- 玩家离线时支付，订单存储到 `plugins/NoobHubPay/pending_orders.json`
- 玩家上线时自动检查并发货
- 发货成功后自动删除订单
- 服务器日志会显示 "Processing X pending orders for player XXX"

## 技术说明

### 坐标系统

使用 `ClientToScreen` 获取窗口客户区坐标，配置的坐标是相对于客户区的，避免边框和标题栏的影响。

### 商品匹配

完全按金额匹配，每个商品必须有唯一价格。Monitor不验证商品，只发送金额到Plugin。

### 玩家匹配

根据服务器所有已注册玩家（在线+离线）进行匹配，支持：
- 精确匹配
- 忽略大小写匹配  
- 包含匹配（模糊匹配）

---

**版本**: v1.2.0 | **协议**: MIT
