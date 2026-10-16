package com.noobhub.pay.managers;

import com.noobhub.pay.NoobHubPay;
import com.noobhub.pay.models.PaymentNotification;
import com.noobhub.pay.models.Product;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class PaymentManager {
    private final NoobHubPay plugin;
    private final Economy economy;
    private final Set<String> processedTransactions;
    private final PendingOrderManager pendingOrderManager;

    public PaymentManager(NoobHubPay plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.processedTransactions = new HashSet<>();
        this.pendingOrderManager = new PendingOrderManager(plugin);

        // 每30分钟清理一次已处理交易记录(防止内存泄漏)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            processedTransactions.clear();
            plugin.getLogger().info("Cleared processed transactions cache");
        }, 36000L, 36000L);
    }

    /**
     * 处理外部支付通知(微信/支付宝)
     * 新逻辑：根据金额匹配商品，根据服务器已注册玩家匹配玩家
     */
    public boolean processExternalPayment(PaymentNotification notification) {
        // 生成交易唯一ID(防重复)
        String transactionId = generateTransactionId(notification);

        if (processedTransactions.contains(transactionId)) {
            plugin.getLogger().warning("Duplicate transaction detected: " + transactionId);
            return false;
        }

        // 1. 根据金额匹配商品
        Product product = plugin.getProductManager().getProductByPrice(notification.getAmount());
        if (product == null) {
            plugin.getLogger().warning("No product matched for amount: " + notification.getAmount());
            return false;
        }

        // 2. 匹配已注册玩家
        OfflinePlayer matchedPlayer = matchPlayer(notification.getPlayerName());
        if (matchedPlayer == null) {
            plugin.getLogger().warning("No registered player matched: " + notification.getPlayerName());
            return false;
        }

        // 标记为已处理
        processedTransactions.add(transactionId);

        // 检查玩家是否在线
        if (matchedPlayer.isOnline()) {
            // 在线玩家：异步发货（命令执行后会自动通知玩家）
            Player onlinePlayer = matchedPlayer.getPlayer();
            deliverProduct(onlinePlayer, product);
            plugin.getLogger().info(String.format(
                "External payment queued for delivery: %s - %s - %.2f via %s",
                onlinePlayer.getName(), product.getName(), notification.getAmount(), notification.getMethod()
            ));
            return true;
        } else {
            // 离线玩家：添加到待发货队列
            pendingOrderManager.addPendingOrder(
                matchedPlayer.getUniqueId(),
                product.getName(),
                notification.getAmount(),
                notification.getMethod()
            );
            plugin.getLogger().info(String.format(
                "External payment queued (offline): %s will receive %s for %.2f when online",
                matchedPlayer.getName(), product.getName(), notification.getAmount()
            ));
            return true;
        }
    }

    /**
     * 匹配已注册玩家
     * 支持：精确匹配、包含匹配
     */
    private OfflinePlayer matchPlayer(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        // 清理输入：去除空格和特殊字符
        String cleaned = input.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "");

        // 1. 精确匹配
        OfflinePlayer exact = Bukkit.getOfflinePlayer(cleaned);
        if (exact != null && exact.hasPlayedBefore()) {
            return exact;
        }

        // 2. 遍历所有已注册玩家
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            String playerName = player.getName();
            if (playerName == null) continue;

            // 3. 忽略大小写精确匹配
            if (playerName.equalsIgnoreCase(cleaned)) {
                return player;
            }

            // 4. 包含匹配（玩家名包含输入 或 输入包含玩家名）
            if (playerName.toLowerCase().contains(cleaned.toLowerCase()) ||
                cleaned.toLowerCase().contains(playerName.toLowerCase())) {
                return player;
            }
        }

        return null;
    }

    /**
     * 玩家上线时处理待发货订单
     */
    public void onPlayerJoin(Player player) {
        // 延迟1秒处理，确保玩家完全加载
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingOrderManager.processPendingOrders(player);
        }, 20L);
    }

    /**
     * 处理游戏内货币支付
     */
    public boolean processVaultPayment(Player player, Product product) {
        if (economy == null) {
            sendMessage(player, getMessage("payment-failed")
                .replace("{reason}", "经济系统未启用"));
            return false;
        }

        double balance = economy.getBalance(player);
        if (balance < product.getPrice()) {
            sendMessage(player, getMessage("insufficient-funds")
                .replace("{price}", String.format("%.2f", product.getPrice()))
                .replace("{balance}", String.format("%.2f", balance)));
            return false;
        }

        // 扣款
        if (!economy.withdrawPlayer(player, product.getPrice()).transactionSuccess()) {
            sendMessage(player, getMessage("payment-failed")
                .replace("{reason}", "扣款失败"));
            return false;
        }

        // 发货（游戏货币支付在主线程，可以同步执行）
        deliverProductSync(player, product);
        plugin.getLogger().info(String.format(
            "Vault payment processed: %s bought %s for %.2f",
            player.getName(), product.getName(), product.getPrice()
        ));

        return true;
    }

    /**
     * 同步发货 - 用于游戏内命令
     */
    private void deliverProductSync(Player player, Product product) {
        try {
            plugin.getLogger().info(String.format(
                "Delivering product '%s' to player %s (sync)",
                product.getName(), player.getName()
            ));

            boolean allSuccess = true;
            for (String command : product.getCommands()) {
                String cmd = command.replace("{player}", player.getName());
                // 去掉前导斜杠（dispatchCommand不需要）
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                plugin.getLogger().info("Executing command: " + cmd);

                boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

                if (result) {
                    plugin.getLogger().info("Command executed successfully: " + cmd);
                } else {
                    plugin.getLogger().warning("Command execution failed: " + cmd);
                    allSuccess = false;
                }
            }

            // 立即通知结果
            if (allSuccess) {
                sendMessage(player, getMessage("payment-success"));
            } else {
                sendMessage(player, getMessage("payment-failed").replace("{reason}", "命令执行失败"));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to deliver product: " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, getMessage("payment-failed").replace("{reason}", e.getMessage()));
        }
    }

    /**
     * 发货 - 异步执行命令并在完成后通知玩家
     */
    private void deliverProduct(Player player, Product product) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                plugin.getLogger().info(String.format(
                    "Delivering product '%s' to player %s",
                    product.getName(), player.getName()
                ));

                boolean allSuccess = true;
                for (String command : product.getCommands()) {
                    String cmd = command.replace("{player}", player.getName());
                    // 去掉前导斜杠（dispatchCommand不需要）
                    if (cmd.startsWith("/")) {
                        cmd = cmd.substring(1);
                    }
                    plugin.getLogger().info("Executing command: " + cmd);

                    boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

                    if (result) {
                        plugin.getLogger().info("Command executed successfully: " + cmd);
                    } else {
                        plugin.getLogger().warning("Command execution failed: " + cmd);
                        allSuccess = false;
                    }
                }

                // 命令执行完后才通知玩家
                if (allSuccess) {
                    sendMessage(player, getMessage("payment-success"));
                    plugin.getLogger().info(String.format(
                        "Product delivered successfully: %s -> %s",
                        product.getName(), player.getName()
                    ));
                } else {
                    sendMessage(player, getMessage("payment-failed").replace("{reason}", "命令执行失败"));
                    plugin.getLogger().warning(String.format(
                        "Product delivery failed: %s -> %s",
                        product.getName(), player.getName()
                    ));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to deliver product: " + e.getMessage());
                e.printStackTrace();
                sendMessage(player, getMessage("payment-failed").replace("{reason}", e.getMessage()));
            }
        });
    }

    /**
     * 发货待处理订单 - 玩家上线时处理离线订单
     */
    public boolean deliverPendingOrder(Player player, Product product, String orderId) {
        final boolean[] success = {false};

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                plugin.getLogger().info(String.format(
                    "Delivering pending order %s: %s to %s",
                    orderId, product.getName(), player.getName()
                ));

                boolean allSuccess = true;
                for (String command : product.getCommands()) {
                    String cmd = command.replace("{player}", player.getName());
                    if (cmd.startsWith("/")) {
                        cmd = cmd.substring(1);
                    }
                    plugin.getLogger().info("Executing command: " + cmd);

                    boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

                    if (result) {
                        plugin.getLogger().info("Command executed successfully: " + cmd);
                    } else {
                        plugin.getLogger().warning("Command execution failed: " + cmd);
                        allSuccess = false;
                    }
                }

                if (allSuccess) {
                    sendMessage(player, getMessage("offline-order-delivered")
                        .replace("{product}", product.getName()));
                    plugin.getLogger().info(String.format(
                        "Pending order delivered: %s -> %s",
                        orderId, player.getName()
                    ));
                    success[0] = true;
                } else {
                    plugin.getLogger().warning(String.format(
                        "Pending order delivery failed: %s -> %s",
                        orderId, player.getName()
                    ));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to deliver pending order: " + e.getMessage());
                e.printStackTrace();
            }
        });

        return success[0];
    }

    /**
     * 生成交易唯一ID
     */
    private String generateTransactionId(PaymentNotification notification) {
        return String.format("%s_%s_%s_%.2f_%d",
            notification.getMethod(),
            notification.getPlayerName(),
            notification.getProductName(),
            notification.getAmount(),
            notification.getTimestamp() / 10000 // 10秒内的交易视为同一笔
        );
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(getMessage("prefix") + message);
    }

    private String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "§c配置缺失: " + key);
    }

    public Economy getEconomy() {
        return economy;
    }
}


