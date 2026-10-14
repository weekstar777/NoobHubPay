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
     * 新逻辑：根据服务器已注册玩家名单匹配玩家
     */
    public boolean processExternalPayment(PaymentNotification notification) {
        // 生成交易唯一ID(防重复)
        String transactionId = generateTransactionId(notification);

        if (processedTransactions.contains(transactionId)) {
            plugin.getLogger().warning("Duplicate transaction detected: " + transactionId);
            return false;
        }

        // 匹配已注册玩家
        OfflinePlayer matchedPlayer = matchPlayer(notification.getPlayerName());
        if (matchedPlayer == null) {
            plugin.getLogger().warning("No registered player matched: " + notification.getPlayerName());
            return false;
        }

        Product product = plugin.getProductManager().getProductByFuzzyName(notification.getProductName());
        if (product == null) {
            plugin.getLogger().warning("Product not found: " + notification.getProductName());
            return false;
        }

        // 验证金额
        if (Math.abs(product.getPrice() - notification.getAmount()) > 0.01) {
            plugin.getLogger().warning(String.format(
                "Price mismatch for %s: expected %.2f, got %.2f",
                product.getName(), product.getPrice(), notification.getAmount()
            ));
            return false;
        }

        // 标记为已处理
        processedTransactions.add(transactionId);

        // 检查玩家是否在线
        if (matchedPlayer.isOnline()) {
            // 在线玩家：直接发货
            Player onlinePlayer = matchedPlayer.getPlayer();
            boolean success = deliverProduct(onlinePlayer, product);
            if (success) {
                sendMessage(onlinePlayer, getMessage("payment-success"));
                plugin.getLogger().info(String.format(
                    "External payment processed (online): %s bought %s for %.2f via %s",
                    onlinePlayer.getName(), product.getName(), notification.getAmount(), notification.getMethod()
                ));
            }
            return success;
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

        // 发货
        boolean success = deliverProduct(player, product);
        if (success) {
            sendMessage(player, getMessage("payment-success"));
            plugin.getLogger().info(String.format(
                "Vault payment processed: %s bought %s for %.2f",
                player.getName(), product.getName(), product.getPrice()
            ));
        } else {
            // 发货失败,退款
            economy.depositPlayer(player, product.getPrice());
            sendMessage(player, getMessage("payment-failed")
                .replace("{reason}", "发货失败,已退款"));
        }

        return success;
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


