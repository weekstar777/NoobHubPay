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

        if (!plugin.getConfig().contains("products")) {
            plugin.getLogger().warning("No 'products' section found in config!");
            return;
        }

        List<?> productList = plugin.getConfig().getList("products");
        if (productList == null || productList.isEmpty()) {
            plugin.getLogger().warning("Products list is empty!");
            return;
        }

        for (Object obj : productList) {
            try {
                if (obj instanceof ConfigurationSection) {
                    ConfigurationSection section = (ConfigurationSection) obj;
                    loadProduct(section);
                } else if (obj instanceof java.util.Map) {
                    // 处理Map类型（常见的YAML解析结果）
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
                    String name = (String) map.get("name");
                    Object priceObj = map.get("price");
                    double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0.0;

                    @SuppressWarnings("unchecked")
                    List<String> commands = (List<String>) map.getOrDefault("commands", new ArrayList<>());
                    String description = (String) map.getOrDefault("description", "");
                    String icon = (String) map.getOrDefault("icon", "PAPER");
                    String qrcodeImage = (String) map.getOrDefault("qrcode-image", "");
                    String type = (String) map.getOrDefault("type", "normal");

                    if (name != null) {
                        // cashback类型商品可以没有commands
                        if (commands.isEmpty() && !"cashback".equalsIgnoreCase(type)) {
                            plugin.getLogger().warning("Product " + name + " has no commands and is not cashback type");
                        } else {
                            products.add(new Product(name, price, commands, description, icon, qrcodeImage, type));
                        }
                    }
                } else {
                    plugin.getLogger().warning("Unknown product format: " + obj.getClass().getName());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load product: " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + products.size() + " products");
    }

    private void loadProduct(ConfigurationSection section) {
        String name = section.getString("name");
        double price = section.getDouble("price");
        List<String> commands = section.getStringList("commands");
        String description = section.getString("description", "");
        String icon = section.getString("icon", "PAPER");
        String qrcodeImage = section.getString("qrcode-image", "");
        String type = section.getString("type", "normal");

        if (name != null) {
            // cashback类型商品可以没有commands
            if (commands.isEmpty() && !"cashback".equalsIgnoreCase(type)) {
                plugin.getLogger().warning("Product " + name + " has no commands and is not cashback type");
            } else {
                products.add(new Product(name, price, commands, description, icon, qrcodeImage, type));
            }
        }
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

    /**
     * 根据价格匹配商品（精确匹配）
     */
    public Product getProductByPrice(double price) {
        return products.stream()
            .filter(p -> Math.abs(p.getPrice() - price) < 0.01)
            .findFirst()
            .orElse(null);
    }

    public List<String> getProductNames() {
        return products.stream()
            .map(Product::getName)
            .collect(Collectors.toList());
    }
}
