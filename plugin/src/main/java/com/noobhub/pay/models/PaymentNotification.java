package com.noobhub.pay.models;

import java.util.Map;

public class PaymentNotification {
    private String method; // wechat, alipay
    private String playerName;
    private String productName;
    private double amount;
    private Map<String, String> rawData;
    private long timestamp;

    public PaymentNotification(String method, String playerName, String productName,
                             double amount, Map<String, String> rawData) {
        this.method = method;
        this.playerName = playerName;
        this.productName = productName;
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

    public String getProductName() {
        return productName;
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
