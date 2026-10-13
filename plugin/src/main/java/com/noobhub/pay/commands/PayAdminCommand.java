package com.noobhub.pay.commands;

import com.noobhub.pay.NoobHubPay;
import com.noobhub.pay.models.Product;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PayAdminCommand implements CommandExecutor, TabCompleter {
    private final NoobHubPay plugin;

    public PayAdminCommand(NoobHubPay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("noobhub.pay.admin")) {
            sender.sendMessage("§c你没有权限使用此命令!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                plugin.getProductManager().loadProducts();
                sender.sendMessage("§a配置已重新加载!");
                break;

            case "list":
                List<Product> products = plugin.getProductManager().getProducts();
                sender.sendMessage("§6=== 商品列表 ===");
                for (Product product : products) {
                    sender.sendMessage(String.format("§7- §e%s §7价格: §a%.2f §7命令数: §b%d",
                        product.getName(), product.getPrice(), product.getCommands().size()));
                }
                sender.sendMessage("§6总计: §e" + products.size() + " §6个商品");
                break;

            case "test":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c此命令仅玩家可用!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /payadmin test <商品名>");
                    return true;
                }

                Player player = (Player) sender;
                String productName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Product product = plugin.getProductManager().getProductByFuzzyName(productName);

                if (product == null) {
                    sender.sendMessage("§c未找到商品: " + productName);
                    return true;
                }

                boolean success = plugin.getPaymentManager().processVaultPayment(player, product);
                if (success) {
                    sender.sendMessage("§a测试购买成功!");
                } else {
                    sender.sendMessage("§c测试购买失败!");
                }
                break;

            case "status":
                sender.sendMessage("§6=== NoobHubPay 状态 ===");
                sender.sendMessage("§7HTTP服务器: " +
                    (plugin.getHttpServerManager().isRunning() ? "§a运行中" : "§c已停止"));
                sender.sendMessage("§7Vault经济: " +
                    (plugin.getEconomy() != null ? "§a已启用" : "§c未启用"));
                sender.sendMessage("§7商品数量: §e" + plugin.getProductManager().getProducts().size());
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== NoobHubPay 管理命令 ===");
        sender.sendMessage("§e/payadmin reload §7- 重新加载配置");
        sender.sendMessage("§e/payadmin list §7- 查看商品列表");
        sender.sendMessage("§e/payadmin test <商品名> §7- 测试购买商品(使用游戏货币)");
        sender.sendMessage("§e/payadmin status §7- 查看系统状态");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("noobhub.pay.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "list", "test", "status");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            return plugin.getProductManager().getProductNames();
        }

        return new ArrayList<>();
    }
}
