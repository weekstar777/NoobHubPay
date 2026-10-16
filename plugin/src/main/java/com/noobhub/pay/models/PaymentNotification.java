package com.noobhub.pay.models;

import java.util.Map;

/**
 * 支付通知模型
 *
 * 从Monitor接收的支付信息：
 * - method: 支付方式（wechat/alipay）
 * - playerName: 玩家名（从付款方信息提取）
 * - amount: 金额（用于匹配商品）
 * - rawData: 原始OCR识别数据
 */
public class PaymentNotification {
    private String method;
    private String playerName;
    private double amount;
    private Map<String, String> rawData;
    private long timestamp;

    public PaymentNotification(String method, String playerName, String productName,
                             double amount, Map<String, String> rawData) {
        this.method = method;
        this.playerName = playerName;
        // productName已废弃，Monitor不再发送，但保留参数兼容性
        this.amount = amount;
        this.rawData = rawData;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMethod() {
        return method;
    }

    public String getPlayerName() {
        return playerName;
    }

    public double getAmount() {
        return amount;
    }

    public Map<String, String> getRawData() {
        return rawData;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
