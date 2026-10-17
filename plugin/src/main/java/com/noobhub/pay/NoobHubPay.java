package com.noobhub.pay;

import com.noobhub.pay.commands.PayAdminCommand;
import com.noobhub.pay.commands.PhoneCommand;
import com.noobhub.pay.commands.StoreCommand;
import com.noobhub.pay.listeners.PlayerListener;
import com.noobhub.pay.listeners.StoreListener;
import com.noobhub.pay.managers.HttpServerManager;
import com.noobhub.pay.managers.PaymentManager;
import com.noobhub.pay.managers.PhoneManager;
import com.noobhub.pay.managers.ProductManager;
import com.noobhub.pay.managers.RedPacketManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class NoobHubPay extends JavaPlugin {
    private ProductManager productManager;
    private PaymentManager paymentManager;
    private HttpServerManager httpServerManager;
    private PhoneManager phoneManager;
    private RedPacketManager redPacketManager;
    private Economy economy;

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();

        // 初始化Vault经济系统（可选）
        if (!setupEconomy()) {
            getLogger().warning("Vault not found! In-game currency payment disabled");
        }

        // 初始化管理器
        productManager = new ProductManager(this);
        paymentManager = new PaymentManager(this);
        httpServerManager = new HttpServerManager(this);
        phoneManager = new PhoneManager(this);
        redPacketManager = new RedPacketManager(this);

        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new StoreListener(this), this);

        // 启动HTTP服务器
        httpServerManager.start();

        // 注册命令
        StoreCommand storeCommand = new StoreCommand(this);
        getCommand("store").setExecutor(storeCommand);

        PhoneCommand phoneCommand = new PhoneCommand(this);
        getCommand("phone").setExecutor(phoneCommand);

        PayAdminCommand adminCommand = new PayAdminCommand(this);
        getCommand("payadmin").setExecutor(adminCommand);
        getCommand("payadmin").setTabCompleter(adminCommand);

        getLogger().info("NoobHubPay v" + getDescription().getVersion() + " enabled!");
        getLogger().info("Loaded " + productManager.getProducts().size() + " products");
    }

    @Override
    public void onDisable() {
        // 停止HTTP服务器
        if (httpServerManager != null) {
            httpServerManager.stop();
        }

        getLogger().info("NoobHubPay disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
            .getRegistration(Economy.class);

        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    public ProductManager getProductManager() {
        return productManager;
    }

    public PaymentManager getPaymentManager() {
        return paymentManager;
    }

    public HttpServerManager getHttpServerManager() {
        return httpServerManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public PhoneManager getPhoneManager() {
        return phoneManager;
    }

    public RedPacketManager getRedPacketManager() {
        return redPacketManager;
    }
}
