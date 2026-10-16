package com.noobhub.pay.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.noobhub.pay.NoobHubPay;
import com.noobhub.pay.models.Product;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 离线订单管理器
 * 负责存储、加载和处理离线玩家的待发货订单
 */
public class PendingOrderManager {
    private final NoobHubPay plugin;
    private final File ordersFile;
    private final Gson gson;
    private final Map<UUID, List<PendingOrder>> pendingOrders;

    public PendingOrderManager(NoobHubPay plugin) {
        this.plugin = plugin;
        this.ordersFile = new File(plugin.getDataFolder(), "pending_orders.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.pendingOrders = new HashMap<>();

        // 详细记录文件路径
        plugin.getLogger().info("=== PendingOrderManager Initialization ===");
        plugin.getLogger().info("Data folder: " + plugin.getDataFolder().getAbsolutePath());
        plugin.getLogger().info("Orders file: " + ordersFile.getAbsolutePath());
        plugin.getLogger().info("File exists: " + ordersFile.exists());
        if (ordersFile.exists()) {
            plugin.getLogger().info("File size: " + ordersFile.length() + " bytes");
        }

        loadOrders();

        int totalOrders = pendingOrders.values().stream()
            .mapToInt(List::size)
            .sum();
        plugin.getLogger().info("Loaded " + totalOrders + " orders from "
            + pendingOrders.size() + " players");
        plugin.getLogger().info("==========================================");
    }

    /**
     * 添加待发货订单
     */
    public void addPendingOrder(UUID playerId, String productName, double amount, String method) {
        PendingOrder order = new PendingOrder(
            UUID.randomUUID().toString(),
            playerId,
            productName,
            amount,
            method,
            System.currentTimeMillis()
        );

        pendingOrders.computeIfAbsent(playerId, k -> new ArrayList<>()).add(order);
        saveOrders();

        plugin.getLogger().info(String.format(
            "Added pending order for player %s: %s (%.2f)",
            playerId, productName, amount
        ));
    }

    /**
     * 处理玩家的所有待发货订单
     */
    public void processPendingOrders(Player player) {
        UUID playerId = player.getUniqueId();
        List<PendingOrder> orders = pendingOrders.get(playerId);

        if (orders == null || orders.isEmpty()) {
            return;
        }

        plugin.getLogger().info(String.format(
            "Processing %d pending orders for player %s",
            orders.size(), player.getName()
        ));

        List<PendingOrder> failedOrders = new ArrayList<>();

        for (PendingOrder order : orders) {
            Product product = plugin.getProductManager().getProductByName(order.getProductName());
            if (product == null) {
                plugin.getLogger().warning("Product not found for pending order: " + order.getProductName());
                failedOrders.add(order);
                continue;
            }

            // 使用PaymentManager的发货逻辑
            boolean success = plugin.getPaymentManager().deliverPendingOrder(player, product, order.getOrderId());
            if (!success) {
                failedOrders.add(order);
            }
        }

        // 删除成功发货的订单
        if (failedOrders.size() < orders.size()) {
            int successCount = orders.size() - failedOrders.size();
            plugin.getLogger().info(String.format(
                "Delivered %d/%d orders for player %s",
                successCount, orders.size(), player.getName()
            ));

            if (failedOrders.isEmpty()) {
                pendingOrders.remove(playerId);
                plugin.getLogger().info(String.format(
                    "All orders delivered, removed player %s from pending list",
                    player.getName()
                ));
            } else {
                pendingOrders.put(playerId, failedOrders);
                plugin.getLogger().warning(String.format(
                    "Kept %d failed orders for player %s",
                    failedOrders.size(), player.getName()
                ));
            }
            saveOrders();
        }
    }

    /**
     * 获取玩家的待发货订单数量
     */
    public int getPendingOrderCount(UUID playerId) {
        List<PendingOrder> orders = pendingOrders.get(playerId);
        return orders == null ? 0 : orders.size();
    }

    /**
     * 保存订单到文件
     */
    private void saveOrders() {
        try (FileWriter writer = new FileWriter(ordersFile)) {
            // 扁平化存储结构
            List<PendingOrder> allOrders = pendingOrders.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

            plugin.getLogger().info(String.format(
                "Saving %d pending orders from %d players",
                allOrders.size(), pendingOrders.size()
            ));

            gson.toJson(allOrders, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save pending orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从文件加载订单
     */
    private void loadOrders() {
        if (!ordersFile.exists()) {
            plugin.getLogger().info("No pending orders file found, starting fresh");
            return;
        }

        try (FileReader reader = new FileReader(ordersFile)) {
            Type listType = new TypeToken<List<PendingOrder>>(){}.getType();
            List<PendingOrder> allOrders = gson.fromJson(reader, listType);

            if (allOrders != null && !allOrders.isEmpty()) {
                pendingOrders.clear();
                plugin.getLogger().info("Loading " + allOrders.size() + " orders from file...");

                for (PendingOrder order : allOrders) {
                    pendingOrders.computeIfAbsent(order.getPlayerId(), k -> new ArrayList<>()).add(order);
                    plugin.getLogger().info(String.format(
                        "  - Order %s: %s for player %s (%.2f)",
                        order.getOrderId().substring(0, 8),
                        order.getProductName(),
                        order.getPlayerId().toString().substring(0, 8),
                        order.getAmount()
                    ));
                }
            } else {
                plugin.getLogger().info("Orders file is empty or invalid");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load pending orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "§c配置缺失: " + key);
    }

    /**
     * 待发货订单数据结构
     */
    public static class PendingOrder {
        private final String orderId;
        private final UUID playerId;
        private final String productName;
        private final double amount;
        private final String method;
        private final long timestamp;

        public PendingOrder(String orderId, UUID playerId, String productName,
                          double amount, String method, long timestamp) {
            this.orderId = orderId;
            this.playerId = playerId;
            this.productName = productName;
            this.amount = amount;
            this.method = method;
            this.timestamp = timestamp;
        }

        public String getOrderId() { return orderId; }
        public UUID getPlayerId() { return playerId; }
        public String getProductName() { return productName; }
        public double getAmount() { return amount; }
        public String getMethod() { return method; }
        public long getTimestamp() { return timestamp; }
    }
}

