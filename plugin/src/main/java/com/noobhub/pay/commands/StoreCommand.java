package com.noobhub.pay.commands;

import com.noobhub.pay.NoobHubPay;
import com.noobhub.pay.gui.StoreGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StoreCommand implements CommandExecutor {
    private final NoobHubPay plugin;

    public StoreCommand(NoobHubPay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令仅玩家可用!");
            return true;
        }

        Player player = (Player) sender;
        StoreGUI gui = new StoreGUI(plugin);
        gui.open(player);
        return true;
    }
}
