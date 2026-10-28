package com.noobhub.pay.managers;

import com.noobhub.pay.NoobHubPay;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RedPacketManager {
    private final NoobHubPay plugin;
    private final File redPacketFile;

    public RedPacketManager(NoobHubPay plugin) {
        this.plugin = plugin;
        this.redPacketFile = new File(plugin.getDataFolder(), "兑换数据.txt");
    }

    public boolean addRedPacket(Player player, String phone, double amount) {
        try (FileWriter fw = new FileWriter(redPacketFile, true);
             PrintWriter pw = new PrintWriter(fw)) {

            String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String record = String.format("%s+%s+%s+应发金额%.0f元 [%s]",
                player.getUniqueId(),
                player.getName(),
                phone,
                amount,
                timestamp
            );

            pw.println(record);
            plugin.getLogger().info("Added red packet record: " + record);
            return true;

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write red packet record");
            e.printStackTrace();
            return false;
        }
    }
}
