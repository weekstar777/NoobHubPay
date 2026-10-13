package com.noobhub.pay.managers;

import com.noobhub.pay.NoobHubPay;
import com.noobhub.pay.models.Product;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductManager {
    private final NoobHubPay plugin;
    private List<Product> products;

    public ProductManager(NoobHubPay plugin) {
        this.plugin = plugin;
        this.products = new ArrayList<>();
        loadProducts();
    }

    public void loadProducts() {
        products.clear();
        List<?> productList = plugin.getConfig().getList("products");

        if (productList == null) {
            plugin.getLogger().warning("No products found in config!");
            return;
        }

        for (Object obj : productList) {
            if (obj instanceof ConfigurationSection) {
                ConfigurationSection section = (ConfigurationSection) obj;
                String name = section.getString("name");
                double price = section.getDouble("price");
                List<String> commands = section.getStringList("commands");
                String description = section.getString("description", "");

                if (name != null && !commands.isEmpty()) {
                    products.add(new Product(name, price, commands, description));
                }
            }
        }

        plugin.getLogger().info("Loaded " + products.size() + " products");
    }

    public List<Product> getProducts() {
        return new ArrayList<>(products);
    }

    public Product getProductByName(String name) {
        return products.stream()
            .filter(p -> p.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public Product getProductByFuzzyName(String fuzzyName) {
        // 先精确匹配
        Product exact = getProductByName(fuzzyName);
        if (exact != null) return exact;

        // 模糊匹配(包含关系)
        String normalized = fuzzyName.replaceAll("[^0-9a-zA-Z\\u4e00-\\u9fa5]", "");
        return products.stream()
            .filter(p -> {
                String pName = p.getName().replaceAll("[^0-9a-zA-Z\\u4e00-\\u9fa5]", "");
                return pName.contains(normalized) || normalized.contains(pName);
            })
            .findFirst()
            .orElse(null);
    }

    public List<String> getProductNames() {
        return products.stream()
            .map(Product::getName)
            .collect(Collectors.toList());
    }
}
