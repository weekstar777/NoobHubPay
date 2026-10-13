# NoobHubPay 使用示例

## 场景一: 玩家购买钻石块

### 1. 商品配置 (config.yml)
```yaml
products:
  - name: "64个钻石块"
    price: 68.00
    commands:
      - "/give {player} minecraft:diamond_block 64"
    description: "§6购买64个钻石块"
```

### 2. 玩家操作流程

1. 玩家在游戏内记住自己的ID: `Steve`
2. 通过微信转账给服主: `¥68.00`
3. 转账备注填写: `Steve 64个钻石块`
4. 微信弹出收款通知窗口(标题"服务通知")

### 3. 系统自动处理

监控程序识别窗口内容:
```
收款金额
¥68.00
收款项
64个钻石块
付款方信息
Steve
```

解析为:
- 玩家: Steve
- 商品: 64个钻石块  
- 金额: 68.00

发送到插件 → 验证通过 → 执行命令 → 玩家收到钻石块

### 4. 玩家收到消息

```
[NoobHub支付] 支付成功! 商品已发货到您的背包。
```

---

## 场景二: 配置自定义商品

### 示例: VIP会员套餐

```yaml
products:
  - name: "VIP会员月卡"
    price: 30.00
    commands:
      - "/lp user {player} parent add vip"
      - "/give {player} minecraft:diamond 10"
      - "/tell {player} §a欢迎成为VIP会员!"
    description: "§6§lVIP会员月卡 §7- 权限组+10钻石"
```

### 示例: 建筑工具包

```yaml
products:
  - name: "建筑工具包"
    price: 50.00
    commands:
      - "/give {player} minecraft:diamond_pickaxe{Enchantments:[{id:efficiency,lvl:5}]} 1"
      - "/give {player} minecraft:diamond_axe{Enchantments:[{id:efficiency,lvl:5}]} 1"
      - "/give {player} minecraft:diamond_shovel{Enchantments:[{id:efficiency,lvl:5}]} 1"
    description: "§b建筑三件套 - 效率V"
```

---

## 场景三: 支付宝支付配置

### 1. 配置窗口标题
```json
"alipay": {
    "enabled": true,
    "window_title": "支付宝通知",  // 根据实际窗口标题修改
    "fields": {
        "收款金额": "amount",
        "商品名称": "product",      // 支付宝可能叫"商品名称"而不是"收款项"
        "买家信息": "player"
    }
}
```

### 2. 测试识别

运行监控程序后:
1. 打开支付宝通知窗口
2. 查看日志 `payment_monitor.log`
3. 确认OCR识别的字段名是否正确
4. 调整 `fields` 配置匹配实际文字

---

## 场景四: 游戏内货币购买

### 玩家使用命令

服主可以设置特殊NPC或命令方块:
```
/payadmin test 64个钻石块
```

或者集成到GUI插件中,点击商品图标自动执行。

### 扣款逻辑

1. 检查玩家余额是否足够
2. 扣除游戏货币(Vault)
3. 执行发货命令
4. 如果发货失败,自动退款

---

## 场景五: 批量发货

### 配置多个命令

```yaml
products:
  - name: "新手大礼包"
    price: 88.00
    commands:
      - "/give {player} minecraft:iron_sword{Enchantments:[{id:sharpness,lvl:3}]} 1"
      - "/give {player} minecraft:iron_helmet 1"
      - "/give {player} minecraft:iron_chestplate 1"
      - "/give {player} minecraft:iron_leggings 1"
      - "/give {player} minecraft:iron_boots 1"
      - "/give {player} minecraft:cooked_beef 64"
      - "/give {player} minecraft:golden_apple 5"
      - "/eco give {player} 1000"
      - "/tell {player} §6§l欢迎来到NoobHub服务器!"
    description: "§a新手大礼包 - 装备+食物+金币"
```

命令按顺序执行,全部成功后玩家才收到确认消息。

---

## 场景六: 防重复支付

### 问题
同一笔支付被识别多次怎么办?

### 解决方案

系统自动防重复:

1. **监控程序**: 
   - 记录已处理的支付ID
   - 同样的内容在30分钟内只发送一次

2. **插件**:
   - 生成交易唯一ID (方式+玩家+商品+金额+时间戳)
   - 已处理的交易不会重复发货

### 清理机制

- 监控程序: 永久记录(进程重启清空)
- 插件: 每30分钟自动清理记录

---

## 场景七: 调试技巧

### 1. 测试OCR识别

手动截图测试:
```python
from PIL import Image
import easyocr

reader = easyocr.Reader(['ch_sim', 'en'])
img = Image.open('test_screenshot.png')
results = reader.readtext(img)
for (bbox, text, confidence) in results:
    print(f"{text} (置信度: {confidence:.2f})")
```

### 2. 查看HTTP请求

插件日志会记录:
```
[INFO] Payment received: wechat - Steve - 68.00 - 64个钻石块
```

监控程序日志:
```
2026-10-13 15:50:23 - INFO - Payment sent successfully: Steve - 64个钻石块 - ¥68.00
```

### 3. 手动测试HTTP接口

```bash
curl -X POST http://127.0.0.1:8787/payment \
  -H "Content-Type: application/json" \
  -H "X-Secret-Key: your-secret-key" \
  -d '{
    "method": "test",
    "player": "Steve",
    "product": "64个钻石块",
    "amount": 68.00,
    "raw_data": {}
  }'
```

### 4. 模糊匹配测试

商品名 "64个钻石块" 可以匹配:
- "64个钻石块" (完全匹配)
- "64钻石块" (部分匹配)
- "钻石块64个" (包含所有字符)

OCR识别可能出现错误时,模糊匹配能提高成功率。

---

## 常见配置错误

### 错误1: 字段名不匹配
```
❌ "收款金额" 实际OCR识别为 "收款金顿"
```
解决: 检查OCR日志,调整fields配置

### 错误2: 窗口标题错误
```
❌ 配置 "服务通知" 实际窗口标题 "微信通知"
```
解决: 使用Spy++等工具查看真实窗口标题

### 错误3: 命令格式错误
```
❌ "/give {player} diamond 64"
✔ "/give {player} minecraft:diamond 64"
```
解决: 使用完整的物品ID

### 错误4: 权限不足
```
❌ noobhub.pay.admin 权限未配置
```
解决: 给管理员添加权限或使用OP

---

## 性能优化建议

1. **调整检查间隔**
   - 高频交易: `check_interval: 1.0`
   - 低频交易: `check_interval: 3.0`

2. **关闭不需要的方式**
   ```json
   "wechat": { "enabled": false }
   ```

3. **限制OCR区域**
   修改代码只截取窗口关键区域,减少OCR计算量

4. **使用GPU加速**
   ```json
   "ocr": { "gpu": true }
   ```
   需要安装CUDA和cuDNN

---

## 安全建议

1. **定期更换密钥**
   ```yaml
   secret-key: "$(openssl rand -hex 32)"
   ```

2. **监控异常日志**
   ```bash
   grep "ERROR\|Failed" payment_monitor.log
   ```

3. **限制金额范围**
   在代码中添加最大金额检查:
   ```java
   if (amount > 1000) {
       // 大额交易需要管理员确认
   }
   ```

4. **备份交易记录**
   定期导出日志备份
