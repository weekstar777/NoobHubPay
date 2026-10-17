package com.noobhub.pay.commands;

import com.noobhub.pay.NoobHubPay;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PhoneCommand implements CommandExecutor {
    private final NoobHubPay plugin;

    public PhoneCommand(NoobHubPay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令仅玩家可用!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            String phone = plugin.getPhoneManager().getPhone(player.getUniqueId());
            if (phone != null) {
                sender.sendMessage("§a您当前绑定的手机号: §e" + maskPhone(phone));
                sender.sendMessage("§7如需换绑，请输入: §e/phone <旧手机号> <新手机号>");
            } else {
                sender.sendMessage("§c您还未绑定手机号!");
                sender.sendMessage("§7请输入: §e/phone <手机号> §7进行绑定");
            }
            return true;
        }

        if (args.length == 1) {
            return handleBind(player, args[0]);
        } else if (args.length == 2) {
            return handleRebind(player, args[0], args[1]);
        } else {
            sender.sendMessage("§c用法错误!");
            sender.sendMessage("§7绑定: §e/phone <手机号>");
            sender.sendMessage("§7换绑: §e/phone <旧手机号> <新手机号>");
            return true;
        }
    }

    private boolean handleBind(Player player, String phone) {
        if (plugin.getPhoneManager().hasPhone(player.getUniqueId())) {
            player.sendMessage("§c您已经绑定过手机号了!");
            player.sendMessage("§7如需换绑，请输入: §e/phone <旧手机号> <新手机号>");
            return true;
        }

        if (!plugin.getPhoneManager().isValidPhone(phone)) {
            player.sendMessage("§c手机号格式不正确!");
            player.sendMessage("§7请输入11位有效的手机号");
            return true;
        }

        if (plugin.getPhoneManager().bindPhone(player.getUniqueId(), phone)) {
            player.sendMessage("§a§l手机号绑定成功!");
            player.sendMessage("§7您的手机号: §e" + maskPhone(phone));
        } else {
            player.sendMessage("§c手机号绑定失败，请联系管理员!");
        }

        return true;
    }

    private boolean handleRebind(Player player, String oldPhone, String newPhone) {
        if (!plugin.getPhoneManager().hasPhone(player.getUniqueId())) {
            player.sendMessage("§c您还未绑定手机号!");
            player.sendMessage("§7请先使用: §e/phone <手机号> §7进行绑定");
            return true;
        }

        if (!plugin.getPhoneManager().isValidPhone(newPhone)) {
            player.sendMessage("§c新手机号格式不正确!");
            player.sendMessage("§7请输入11位有效的手机号");
            return true;
        }

        if (plugin.getPhoneManager().rebindPhone(player.getUniqueId(), oldPhone, newPhone)) {
            player.sendMessage("§a§l手机号换绑成功!");
            player.sendMessage("§7新手机号: §e" + maskPhone(newPhone));
        } else {
            player.sendMessage("§c换绑失败! 旧手机号不正确!");
            player.sendMessage("§7当前绑定: §e" + maskPhone(
                plugin.getPhoneManager().getPhone(player.getUniqueId())));
        }

        return true;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
