# NoobHubPay 重构计划

## 核心功能确认
1. ✅ 按金额匹配商品
2. ✅ 商品价格唯一
3. ✅ 只需修改Plugin配置
4. ✅ 订单序号防重复（Monitor端）
5. ✅ 离线订单支持
6. ✅ 命令执行后通知

## 需要清理的问题

### 1. Plugin端防重复机制冗余
**问题**：Plugin还保留transactionId防重复，但Monitor已用订单序号
**方案**：删除Plugin端的processedTransactions，完全依赖Monitor

### 2. 发货逻辑重复
**问题**：三个发货方法逻辑相似
- deliverProduct (在线支付)
- deliverProductSync (游戏货币)  
- deliverPendingOrder (离线订单)

**方案**：统一为一个核心方法

### 3. PaymentNotification字段冗余
**问题**：productName字段已不使用
**方案**：清理或标记为deprecated

### 4. 注释过时
**问题**：部分注释还说"匹配商品名"
**方案**：更新所有注释

### 5. 配置文件不一致
**问题**：默认config.yml和实际可能不同
**方案**：统一配置

## 整改顺序
1. Plugin核心逻辑
2. Monitor逻辑确认
3. 配置文件统一
4. 注释更新
5. 测试验证
