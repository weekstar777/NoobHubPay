package com.noobhub.pay.managers;

import com.noobhub.pay.NoobHubPay;
import com.noobhub.pay.models.PaymentNotification;
import com.noobhub.pay.models.Product;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class PaymentManager {
    private final NoobHubPay plugin;
    private final Economy economy;
    private final PendingOrderManager pendingOrderManager;

    public PaymentManager(NoobHubPay plugin) {
        this.plugin = plugin;
        this.pendingOrderManager = new PendingOrderManager(plugin);

        // 初始化Vault经济系统
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);
            this.economy = (rsp != null) ? rsp.getProvider() : null;
        } else {
            this.economy = null;
        }
    }

    /**
     * 处理外部支付通知（微信/支付宝）
     *
     * 核心逻辑：
     * 1. 根据金额匹配商品（商品价格必须唯一）
     * 2. 根据玩家名匹配服务器已注册玩家
     * 3. 在线玩家立即发货，离线玩家加入待发货队列
     *
     * 防重复：由Monitor端的订单序号机制保证，Plugin端不再处理
     */
    public boolean processExternalPayment(PaymentNotification notification) {
        // 1. 根据金额匹配商品
        Product product = plugin.getProductManager().getProductByPrice(notification.getAmount());
        if (product == null) {
            plugin.getLogger().warning(String.format(
                "No product found for amount: %.2f", notification.getAmount()
            ));
            return false;
        }

        // 2. 匹配已注册玩家
        OfflinePlayer matchedPlayer = matchPlayer(notification.getPlayerName());
        if (matchedPlayer == null) {
            plugin.getLogger().warning(String.format(
                "No registered player matched: %s", notification.getPlayerName()
            ));
            return false;
        }

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
     * 核心：执行商品命令
     *
     * 所有发货逻辑复用此方法，确保命令执行的一致性
     *
     * @param player 玩家
     * @param product 商品
     * @return 是否全部成功
     */
    private boolean executeProductCommands(Player player, Product product) {
        plugin.getLogger().info(String.format(
            "Executing commands for product '%s' to player %s",
            product.getName(), player.getName()
        ));

        boolean allSuccess = true;
        for (String command : product.getCommands()) {
            String cmd = command.replace("{player}", player.getName());

            // 去掉前导斜杠（Bukkit.dispatchCommand不需要）
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

        return allSuccess;
    }

    /**
     * 游戏内货币购买发货（同步）
     */
    private void deliverProductSync(Player player, Product product) {
        try {
            boolean success = executeProductCommands(player, product);

            if (success) {
                sendMessage(player, getMessage("payment-success"));
            } else {
                sendMessage(player, getMessage("payment-failed")
                    .replace("{reason}", "命令执行失败"));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to deliver product: " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, getMessage("payment-failed")
                .replace("{reason}", e.getMessage()));
        }
    }

    /**
     * 在线支付发货（异步）
     */
    private void deliverProduct(Player player, Product product) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                boolean success = executeProductCommands(player, product);

                if (success) {
                    sendMessage(player, getMessage("payment-success"));
                    plugin.getLogger().info(String.format(
                        "Product delivered: %s -> %s",
                        product.getName(), player.getName()
                    ));
                } else {
                    sendMessage(player, getMessage("payment-failed")
                        .replace("{reason}", "命令执行失败"));
                    plugin.getLogger().warning(String.format(
                        "Product delivery failed: %s -> %s",
                        product.getName(), player.getName()
                    ));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to deliver product: " + e.getMessage());
                e.printStackTrace();
                sendMessage(player, getMessage("payment-failed")
                    .replace("{reason}", e.getMessage()));
            }
        });
    }

    /**
     * 离线订单发货（同步执行）
     *
     * 注意：此方法在玩家登录事件中调用，已经在主线程，无需异步
     */
    public boolean deliverPendingOrder(Player player, Product product, String orderId) {
        try {
            plugin.getLogger().info(String.format(
                "Delivering pending order %s: %s to %s",
                orderId, product.getName(), player.getName()
            ));

            boolean result = executeProductCommands(player, product);

            if (result) {
                sendMessage(player, getMessage("offline-order-delivered")
                    .replace("{product}", product.getName()));
                plugin.getLogger().info(String.format(
                    "Pending order delivered: %s -> %s",
                    orderId, player.getName()
                ));
                return true;
            } else {
                plugin.getLogger().warning(String.format(
                    "Pending order delivery failed: %s -> %s",
                    orderId, player.getName()
                ));
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to deliver pending order: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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


