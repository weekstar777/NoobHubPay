# NoobHubPay - Minecraft支付系统

支持微信/支付宝支付通知OCR识别，自动发货到Minecraft服务器。

## 核心特性

- **区域识别**：识别窗口内特定位置的文字内容
- **离线发货**：玩家离线时存储订单，上线自动发货
- **智能匹配**：根据服务器已注册玩家自动匹配
- **调试模式**：保存截图便于调试坐标问题

## 安装步骤

### 1. Monitor端（Python）

安装依赖：
```bash
cd monitor
pip install -r requirements.txt
```

### 2. 配置坐标

使用坐标辅助工具确定区域位置：
```bash
python coordinate_helper.py "服务通知"
```

会生成 `window_grid.png`，打开查看网格坐标，找到要识别的区域。

### 3. 编辑配置

编辑 `monitor/config.json`：
```json
{
  "wechat": {
    "window_title": "服务通知",
    "regions": {
      "收款金额": {"x": 100, "y": 200, "width": 200, "height": 40},
      "收款项": {"x": 100, "y": 250, "width": 300, "height": 40},
      "付款方信息": {"x": 100, "y": 300, "width": 300, "height": 40}
    }
  },
  "debug": {
    "enabled": true,
    "output_dir": "debug_screenshots"
  }
}
```

**坐标说明**：
- `x, y`：区域左上角相对于窗口客户区的坐标
- `width, height`：区域的宽度和高度
- 坐标是相对于窗口内容区域（不包括标题栏和边框）

### 4. Plugin端（Java）

编译插件：
```bash
cd plugin
mvn clean package
```

复制到服务器：
```bash
cp target/NoobHubPay-1.0.0.jar <服务器路径>/plugins/
```

配置商品（`plugins/NoobHubPay/config.yml`）：
```yaml
products:
  - name: "64个钻石块"
    price: 68.00
    commands:
      - "/give {player} minecraft:diamond_block 64"
```

重启服务器。

## 使用方法

### 启动Monitor

```bash
cd monitor
双击运行 start.bat
```

或手动运行：
```bash
python payment_monitor.py
```

### 工作流程

1. Monitor检测支付窗口
2. 截取配置的区域并OCR识别
3. 匹配商品名称和金额
4. 发送到Plugin服务器
5. Plugin匹配已注册玩家
6. **在线玩家**：直接发货
7. **离线玩家**：存储到 `pending_orders.json`
8. 玩家上线时自动处理待发货订单

## 调试模式

启用调试模式会保存每次截图到 `debug_screenshots/` 目录：

- `{时间}_{区域名}_raw.png` - 原始截图
- `{时间}_{区域名}_processed.png` - 预处理后的截图

**用途**：
1. 确认坐标是否正确
2. 检查OCR能否识别文字
3. 调整区域位置和大小

## 故障排查

### 1. 无法识别文字

**检查步骤**：
1. 启用调试模式查看截图
2. 使用 `coordinate_helper.py` 确认坐标
3. 调整 `confidence_threshold`（降低到0.2或0.1）
4. 确认窗口标题正确

**示例**：
```json
{
  "ocr": {
    "confidence_threshold": 0.2
  }
}
```

### 2. 玩家匹配失败

**原因**：
- 玩家未在服务器注册过
- 付款方信息区域识别内容不对

**解决**：
1. 查看日志中识别到的内容
2. 确保玩家至少登录过一次服务器
3. 支持模糊匹配（包含匹配）

### 3. 订单未发货

**检查**：
1. 查看 `plugins/NoobHubPay/pending_orders.json`
2. 确认玩家是否上线
3. 查看服务器控制台日志

### 4. 坐标不准确

**步骤**：
1. 运行 `coordinate_helper.py` 生成网格图
2. 打开 `window_grid.png`
3. 找到要识别的区域
4. 记录左上角坐标和宽高
5. 更新 `config.json`

## 管理命令

```
/payadmin status  - 查看状态
/payadmin list    - 商品列表
/payadmin reload  - 重载配置
```

## 技术说明

### 坐标系统

使用 `ClientToScreen` 获取窗口客户区坐标，配置的坐标是相对于客户区的，避免了边框和标题栏的影响。

### OCR引擎

支持 EasyOCR（推荐）和 PaddleOCR，EasyOCR更稳定。

### 离线发货

订单存储在 `plugins/NoobHubPay/pending_orders.json`，玩家上线时自动检查并发货，发货成功后删除记录。

---

**版本**: v1.1.0 | **协议**: MIT
