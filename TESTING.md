# 快速测试指南

## 前置条件检查

### 1. 插件已加载
```
/plugins
```
应该看到: `NoobHubPay v1.0.0 (enabled)`

### 2. 依赖已安装
```
/plugins
```
确认: `Vault` 和经济插件(如Essentials)已启用

### 3. HTTP服务器运行
```
/payadmin status
```
应该看到: `HTTP服务器: §a运行中`

---

## 测试1: 游戏货币支付

### 步骤
1. 给测试玩家添加余额:
   ```
   /eco give <玩家名> 100
   ```

2. 玩家执行测试购买:
   ```
   /payadmin test 64个钻石块
   ```

### 预期结果
- 控制台显示: `✓ 测试购买成功!`
- 玩家收到消息: `[NoobHubPay] 支付成功! 商品已发货到您的背包。`
- 玩家背包获得64个钻石块
- 余额扣除68元

### 失败排查
- `余额不足`: 用 `/eco give` 添加更多余额
- `商品不存在`: 检查config.yml商品配置
- `权限不足`: 给玩家OP或添加权限

---

## 测试2: HTTP接口

### 步骤
```bash
cd D:\project_collection\code_project\NoobHubPay\monitor
python test_api.py
```

### 预期结果
```
NoobHubPay 接口测试
==================================================
[1] 测试连接: http://127.0.0.1:8787/payment
✓ 服务器在线

[2] 发送测试支付
   玩家: TestPlayer
   商品: 64个钻石块
   金额: ¥68.00

[3] 服务器响应
   状态码: 200
   响应: {"success":true,"message":"Payment processed"}

✓ 支付测试成功!
```

### 失败排查

#### HTTP 403 (密钥错误)
1. 检查monitor/config.json:
   ```json
   "secret_key": "你的密钥"
   ```

2. 检查plugins/NoobHubPay/config.yml:
   ```yaml
   http-server:
     secret-key: "你的密钥"
   ```

3. 确保两者完全一致(大小写敏感)

#### 无法连接
1. 确认服务器运行: `/payadmin status`
2. 检查端口占用: `netstat -ano | findstr 8787`
3. 查看服务器日志: `logs/latest.log`

#### 玩家不在线
1. 确保TestPlayer在线: `/list`
2. 或修改test_api.py中的player名称为在线玩家

---

## 测试3: OCR识别(微信/支付宝)

### 准备工作

1. 安装Python依赖:
   ```bash
   cd D:\project_collection\code_project\NoobHubPay\monitor
   pip install -r requirements.txt
   ```

2. 首次运行会下载OCR模型(约200MB),耐心等待

### 步骤

1. 复制配置:
   ```bash
   copy config.json.example config.json
   ```

2. 编辑config.json,修改密钥

3. 启动监控:
   ```bash
   start.bat
   ```
   或
   ```bash
   python payment_monitor.py
   ```

4. 打开微信/支付宝收款通知窗口(测试用)

5. 观察日志输出

### 预期日志
```
2026-10-13 20:40:00 - INFO - WindowMonitor initialized
2026-10-13 20:40:10 - INFO - EasyOCR initialized with languages: ['ch_sim', 'en']
2026-10-13 20:40:15 - INFO - Starting monitor for wechat: 服务通知
2026-10-13 20:40:20 - DEBUG - OCR result:
收款金额
¥68.00
收款项
64个钻石块
付款方信息
Steve
2026-10-13 20:40:21 - INFO - Payment sent successfully: Steve - 64个钻石块 - ¥68.00
```

### 调试技巧

#### 窗口未找到
```
2026-10-13 20:40:00 - DEBUG - Window '服务通知' not found
```
**解决**: 
1. 使用Spy++查看真实窗口标题
2. 修改config.json中的window_title

#### OCR识别乱码
```
2026-10-13 20:40:10 - DEBUG - OCR result:
收致金顿  <-- 应该是"收款金额"
```
**解决**:
1. 降低confidence_threshold: `0.6 → 0.4`
2. 确保窗口完整显示,无遮挡
3. 等待几秒让OCR重新识别

#### 字段未匹配
```
2026-10-13 20:40:15 - DEBUG - Field '收款金额' not found in text
```
**解决**:
1. 查看DEBUG日志中的OCR result
2. 找到实际识别的文字
3. 修改config.json的fields配置

---

## 测试4: 完整支付流程

### 模拟真实支付

1. 玩家Steve在游戏内准备接收商品

2. 通过微信转账给服主: `¥68.00`

3. 转账备注: `Steve 64个钻石块`

4. 微信弹出收款通知

5. 监控程序识别并发送到插件

6. 插件验证并执行发货命令

7. Steve收到商品和消息

### 检查点

**监控程序日志** (monitor/payment_monitor.log):
```
INFO - Payment sent successfully: Steve - 64个钻石块 - ¥68.00
```

**服务器日志** (logs/latest.log):
```
[NoobHubPay] Payment received: wechat - Steve - 68.00 - 64个钻石块
[NoobHubPay] External payment processed: Steve bought 64个钻石块 for 68.00 via wechat
```

**游戏内**:
- Steve收到消息: `[NoobHub支付] 支付成功! 商品已发货到您的背包。`
- 背包获得64个钻石块

---

## 性能测试

### 连续支付测试

修改test_api.py添加循环:
```python
for i in range(10):
    test_payment()
    time.sleep(0.5)
```

### 预期结果
- 前1次成功,后9次被防重复机制拦截
- 日志显示: `Duplicate transaction detected`

---

## 故障模拟

### 1. 玩家不在线
- 支付通知到达
- 插件日志: `Player not online: Steve`
- HTTP返回400错误
- 监控程序不记录为已处理(可重试)

### 2. 商品不存在
- 发送不存在的商品名
- 插件日志: `Product not found: 错误商品`
- 玩家收到: `商品不存在!`

### 3. 金额不匹配
- OCR识别金额: 50.00
- 商品价格: 68.00
- 插件日志: `Price mismatch`
- 支付失败

---

## 压力测试

不建议在生产环境执行,仅用于开发测试:

```python
import threading
import requests

def stress_test():
    for i in range(100):
        threading.Thread(target=test_payment).start()
```

**注意**: 
- 可能触发服务器保护机制
- 建议在测试服务器执行
- 观察CPU和内存使用

---

## 测试检查清单

- [ ] 插件成功加载
- [ ] HTTP服务器启动
- [ ] Vault经济系统正常
- [ ] 游戏货币支付测试通过
- [ ] HTTP接口测试通过
- [ ] OCR识别准确
- [ ] 防重复机制生效
- [ ] 日志记录完整
- [ ] 配置热重载(/payadmin reload)
- [ ] 密钥验证正常

全部通过即可投入生产使用!
