package com.noobhub.pay.managers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.noobhub.pay.NoobHubPay;
import com.noobhub.pay.models.PaymentNotification;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;

public class HttpServerManager {
    private final NoobHubPay plugin;
    private final Gson gson;
    private Javalin app;
    private boolean running;

    public HttpServerManager(NoobHubPay plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.running = false;
    }

    public void start() {
        if (running) {
            plugin.getLogger().warning("HTTP server is already running!");
            return;
        }

        if (!plugin.getConfig().getBoolean("http-server.enabled", true)) {
            plugin.getLogger().info("HTTP server is disabled in config");
            return;
        }

        int port = plugin.getConfig().getInt("http-server.port", 8787);
        String bindAddress = plugin.getConfig().getString("http-server.bind-address", "127.0.0.1");

        try {
            app = Javalin.create(config -> {
                config.showJavalinBanner = false;
            }).start(bindAddress, port);

            app.post("/payment", this::handlePayment);
            app.get("/health", ctx -> ctx.json(Map.of("status", "ok")));

            running = true;
            plugin.getLogger().info("HTTP server started on " + bindAddress + ":" + port);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (app != null && running) {
            app.stop();
            running = false;
            plugin.getLogger().info("HTTP server stopped");
        }
    }

    private void handlePayment(Context ctx) {
        try {
            // 验证密钥
            String secretKey = plugin.getConfig().getString("http-server.secret-key");
            String providedKey = ctx.header("X-Secret-Key");

            if (secretKey != null && !secretKey.equals(providedKey)) {
                ctx.status(403).json(Map.of("error", "Invalid secret key"));
                plugin.getLogger().warning("Invalid secret key from " + ctx.ip());
                return;
            }

            // 解析请求
            JsonObject json = gson.fromJson(ctx.body(), JsonObject.class);

            String method = json.get("method").getAsString();
            String playerName = json.get("player").getAsString();
            double amount = json.get("amount").getAsDouble();

            // product字段现在是可选的（Monitor不再发送）
            String productName = json.has("product") ? json.get("product").getAsString() : null;

            Map<String, String> rawData = new HashMap<>();
            if (json.has("regions_data")) {
                JsonObject rawDataJson = json.getAsJsonObject("regions_data");
                rawDataJson.entrySet().forEach(entry ->
                    rawData.put(entry.getKey(), entry.getValue().getAsString())
                );
            }

            PaymentNotification notification = new PaymentNotification(
                method, playerName, productName, amount, rawData
            );

            // 异步处理支付
            boolean success = plugin.getPaymentManager().processExternalPayment(notification);

            if (success) {
                ctx.status(200).json(Map.of("success", true, "message", "Payment processed"));
                plugin.getLogger().info(String.format(
                    "Payment received: %s - %s - %.2f",
                    method, playerName, amount
                ));
            } else {
                ctx.status(400).json(Map.of("success", false, "message", "Payment failed"));
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error handling payment: " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }

    public boolean isRunning() {
        return running;
    }
}
