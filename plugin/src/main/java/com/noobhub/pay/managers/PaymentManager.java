package com.noobhub.pay.managers;

import com.noobhub.pay.NoobHubPay;
import com.noobhub.pay.models.PaymentNotification;
import com.noobhub.pay.models.Product;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class PaymentManager {
    private final NoobHubPay plugin;
    private final Economy economy;
    private final Set<String> processedTransactions;

    public PaymentManager(NoobHubPay plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.processedTransactions = new HashSet<>();

        // 每30分钟清理一次已处理交易记录(防止内存泄漏)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            processedTransactions.clear();
            plugin.getLogger().info("Cleared processed transactions cache");
        }, 36000L, 36000L);
    }

    /**
     * 处理外部支付通知(微信/支付宝)
     */
    public boolean processExternalPayment(PaymentNotification notification) {
        // 生成交易唯一ID(防重复)
        String transactionId = generateTransactionId(notification);

        if (processedTransactions.contains(transactionId)) {
            plugin.getLogger().warning("Duplicate transaction detected: " + transactionId);
            return false;
        }

        Player player = Bukkit.getPlayerExact(notification.getPlayerName());
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Player not online: " + notification.getPlayerName());
            return false;
        }

        Product product = plugin.getProductManager().getProductByFuzzyName(notification.getProductName());
        if (product == null) {
            plugin.getLogger().warning("Product not found: " + notification.getProductName());
            sendMessage(player, getMessage("product-not-found"));
            return false;
        }

        // 验证金额
        if (Math.abs(product.getPrice() - notification.getAmount()) > 0.01) {
            plugin.getLogger().warning(String.format(
                "Price mismatch for %s: expected %.2f, got %.2f",
                product.getName(), product.getPrice(), notification.getAmount()
            ));
            sendMessage(player, getMessage("payment-failed")
                .replace("{reason}", "金额不匹配"));
            return false;
        }

        // 发货
        boolean success = deliverProduct(player, product);
        if (success) {
            processedTransactions.add(transactionId);
            sendMessage(player, getMessage("payment-success"));
            plugin.getLogger().info(String.format(
                "External payment processed: %s bought %s for %.2f via %s",
                player.getName(), product.getName(), notification.getAmount(), notification.getMethod()
            ));
        }

        return success;
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
