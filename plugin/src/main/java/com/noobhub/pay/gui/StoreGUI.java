package com.noobhub.pay.gui;

import com.noobhub.pay.NoobHubPay;
import com.noobhub.pay.models.Product;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class StoreGUI {
    private final NoobHubPay plugin;
    private final NamespacedKey productKey;

    public StoreGUI(NoobHubPay plugin) {
        this.plugin = plugin;
        this.productKey = new NamespacedKey(plugin, "product_name");
    }

    public void open(Player player) {
        List<Product> products = plugin.getProductManager().getProducts();
        int size = calculateInventorySize(products.size());

        Inventory inv = Bukkit.createInventory(null, size, "§6§l商店");

        for (int i = 0; i < products.size() && i < size; i++) {
            Product product = products.get(i);
            inv.setItem(i, createProductItem(product));
        }

        player.openInventory(inv);
    }

    private int calculateInventorySize(int productCount) {
        int rows = (int) Math.ceil(productCount / 9.0);
        return Math.min(Math.max(rows, 1) * 9, 54);
    }

    private ItemStack createProductItem(Product product) {
        Material material = parseMaterial(product.getIcon());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§e§l" + product.getName());

            List<String> lore = new ArrayList<>();
            lore.add("§7" + product.getDescription());
            lore.add("");

            // 1元现金红包使用美元符号，其他商品使用人民币符号
            if ("1元现金红包".equals(product.getName())) {
                lore.add("§6价格: §a$" + String.format("%.2f", product.getPrice()));
            } else {
                lore.add("§6价格: §a¥" + String.format("%.2f", product.getPrice()));
                lore.add("");
                lore.add("§e点击获取付款码");
            }

            meta.setLore(lore);

            meta.getPersistentDataContainer().set(
                productKey,
                PersistentDataType.STRING,
                product.getName()
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    private Material parseMaterial(String iconName) {
        try {
            return Material.valueOf(iconName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid icon: " + iconName);
            return Material.PAPER;
        }
    }

    public NamespacedKey getProductKey() {
        return productKey;
    }
}

