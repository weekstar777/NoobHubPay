package com.noobhub.pay.listeners;

import com.noobhub.pay.NoobHubPay;
import com.noobhub.pay.models.Product;
import com.noobhub.pay.utils.QRCodeMapRenderer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StoreListener implements Listener {
    private final NoobHubPay plugin;
    private final NamespacedKey productKey;
    private final NamespacedKey qrcodeKey;

    public StoreListener(NoobHubPay plugin) {
        this.plugin = plugin;
        this.productKey = new NamespacedKey(plugin, "product_name");
        this.qrcodeKey = new NamespacedKey(plugin, "qrcode_item");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle().equals("§6§l商店")) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            ItemMeta meta = clicked.getItemMeta();
            String productName = meta.getPersistentDataContainer().get(productKey, PersistentDataType.STRING);

            if (productName != null) {
                Product product = plugin.getProductManager().getProductByName(productName);
                if (product != null) {
                    if ("cashback".equalsIgnoreCase(product.getType())) {
                        handleCashbackExchange(player, product);
                    } else {
                        giveQRCodeItem(player, product);
                    }
                    player.closeInventory();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals("§6§l商店")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.getPersistentDataContainer().has(qrcodeKey, PersistentDataType.STRING)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c§l此物品无法丢弃!");
            }
        }
    }

    private void giveQRCodeItem(Player player, Product product) {
        ItemStack qrcode = createQRCodeItem(product);
        player.getInventory().addItem(qrcode);

        player.sendMessage("§a§l已获取付款码!");
        player.sendMessage("§e请在 §c1分钟 §e内完成支付");
        player.sendMessage("§7付款码将在1分钟后自动消失");

        scheduleItemRemoval(player, product.getName());
    }

    private ItemStack createQRCodeItem(Product product) {
        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6§l付款码 §8- §e" + product.getName());

            List<String> lore = new ArrayList<>();
            lore.add("§7请扫码支付 §a¥" + String.format("%.2f", product.getPrice()));
            lore.add("");
            lore.add("§c§l注意: 此物品将在1分钟后消失");
            lore.add("§7请尽快完成支付");
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(
                qrcodeKey,
                PersistentDataType.STRING,
                product.getName()
            );

            File qrImageFile = getQRCodeFile(product);
            if (qrImageFile != null && qrImageFile.exists()) {
                MapView mapView = Bukkit.createMap(plugin.getServer().getWorlds().get(0));
                mapView.getRenderers().clear();
                mapView.addRenderer(new QRCodeMapRenderer(qrImageFile, plugin.getLogger()));
                meta.setMapView(mapView);
            } else {
                plugin.getLogger().warning("QR code image not found for product: " + product.getName());
            }

            map.setItemMeta(meta);
        }

        return map;
    }

    private File getQRCodeFile(Product product) {
        String qrcodePath = product.getQrcodeImage();
        if (qrcodePath == null || qrcodePath.isEmpty()) {
            return null;
        }

        File file = new File(qrcodePath);
        if (!file.isAbsolute()) {
            file = new File(plugin.getDataFolder(), qrcodePath);
        }

        return file;
    }

    private void scheduleItemRemoval(Player player, String productName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                removeQRCodeItems(player, productName);
            }
        }.runTaskLater(plugin, 20 * 60);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeQRCodeItems(player, null);
    }

    private void handleCashbackExchange(Player player, Product product) {
        if (!plugin.getPhoneManager().hasPhone(player.getUniqueId())) {
            player.sendMessage("§c§l兑换失败！");
            player.sendMessage("§e您还未绑定手机号，请输入 §a/phone §e填写手机号");
            return;
        }

        Economy economy = plugin.getEconomy();
        if (economy == null) {
            player.sendMessage("§c经济系统未启用!");
            return;
        }

        double balance = economy.getBalance(player);
        if (balance < product.getPrice()) {
            player.sendMessage("§c余额不足!");
            player.sendMessage("§7需要: §e" + product.getPrice() + " §7当前: §e" + balance);
            return;
        }

        if (!economy.withdrawPlayer(player, product.getPrice()).transactionSuccess()) {
            player.sendMessage("§c扣款失败，请联系管理员!");
            return;
        }

        String phone = plugin.getPhoneManager().getPhone(player.getUniqueId());
        double cashAmount = 1.0;

        if (plugin.getRedPacketManager().addRedPacket(player, phone, cashAmount)) {
            player.sendMessage("§a§l1元现金红包兑换成功！");
            player.sendMessage("§e等待3个工作日即可到账");
            player.sendMessage("§7已扣除 §c" + product.getPrice() + " §7游戏币");
        } else {
            economy.depositPlayer(player, product.getPrice());
            player.sendMessage("§c红包记录失败，已退还游戏币!");
        }
    }

    private void removeQRCodeItems(Player player, String specificProduct) {
        boolean removed = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                String qrProduct = meta.getPersistentDataContainer().get(qrcodeKey, PersistentDataType.STRING);
                if (qrProduct != null && (specificProduct == null || specificProduct.equals(qrProduct))) {
                    player.getInventory().remove(item);
                    removed = true;
                }
            }
        }
        if (removed && specificProduct != null) {
            player.sendMessage("§e§l付款码已过期并自动消失");
        }
    }
}



