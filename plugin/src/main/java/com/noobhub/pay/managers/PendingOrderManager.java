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

        loadOrders();
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
            Product product = plugin.getProductManager().getProductByFuzzyName(order.getProductName());
            if (product == null) {
                plugin.getLogger().warning("Product not found for pending order: " + order.getProductName());
                continue; // 跳过此订单但不删除，等待管理员处理
            }

            boolean success = deliverProduct(player, product);
            if (success) {
                player.sendMessage(getMessage("prefix") + getMessage("offline-order-delivered")
                    .replace("{product}", product.getName()));
                plugin.getLogger().info(String.format(
                    "Delivered pending order %s to %s",
                    order.getOrderId(), player.getName()
                ));
            } else {
                failedOrders.add(order);
            }
        }

        // 删除成功发货的订单
        if (failedOrders.size() < orders.size()) {
            if (failedOrders.isEmpty()) {
                pendingOrders.remove(playerId);
            } else {
                pendingOrders.put(playerId, failedOrders);
            }
            saveOrders();
        }
    }

    /**
     * 发货
     */
    private boolean deliverProduct(Player player, Product product) {
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String command : product.getCommands()) {
                    String cmd = command.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            });
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to deliver product: " + e.getMessage());
            e.printStackTrace();
            return false;
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
            plugin.getLogger().info("No pending orders file found");
            return;
        }

        try (FileReader reader = new FileReader(ordersFile)) {
            Type listType = new TypeToken<List<PendingOrder>>(){}.getType();
            List<PendingOrder> allOrders = gson.fromJson(reader, listType);

            if (allOrders != null) {
                pendingOrders.clear();
                for (PendingOrder order : allOrders) {
                    pendingOrders.computeIfAbsent(order.getPlayerId(), k -> new ArrayList<>()).add(order);
                }
                plugin.getLogger().info("Loaded " + allOrders.size() + " pending orders");
            }
        } catch (IOException e) {
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

