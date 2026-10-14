package com.noobhub.pay.listeners;

import com.noobhub.pay.NoobHubPay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 玩家事件监听器
 * 负责处理玩家上线时的待发货订单
 */
public class PlayerListener implements Listener {
    private final NoobHubPay plugin;

    public PlayerListener(NoobHubPay plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 处理待发货订单
        plugin.getPaymentManager().onPlayerJoin(event.getPlayer());
    }
}
