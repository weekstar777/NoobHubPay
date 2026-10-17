package com.noobhub.pay.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.noobhub.pay.NoobHubPay;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PhoneManager {
    private final NoobHubPay plugin;
    private final File dataFile;
    private final Gson gson;
    private Map<UUID, String> phoneData;

    public PhoneManager(NoobHubPay plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "phones.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.phoneData = new HashMap<>();
        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            saveData();
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Map<String, String> rawData = gson.fromJson(reader,
                new TypeToken<Map<String, String>>(){}.getType());

            if (rawData != null) {
                phoneData.clear();
                rawData.forEach((uuid, phone) -> {
                    try {
                        phoneData.put(UUID.fromString(uuid), phone);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in phones.json: " + uuid);
                    }
                });
            }

            plugin.getLogger().info("Loaded " + phoneData.size() + " phone bindings");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load phones.json");
            e.printStackTrace();
        }
    }

    private void saveData() {
        try {
            dataFile.getParentFile().mkdirs();

            Map<String, String> rawData = new HashMap<>();
            phoneData.forEach((uuid, phone) -> rawData.put(uuid.toString(), phone));

            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(rawData, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save phones.json");
            e.printStackTrace();
        }
    }

    public boolean hasPhone(UUID playerId) {
        return phoneData.containsKey(playerId);
    }

    public String getPhone(UUID playerId) {
        return phoneData.get(playerId);
    }

    public boolean bindPhone(UUID playerId, String phone) {
        if (!isValidPhone(phone)) {
            return false;
        }
        phoneData.put(playerId, phone);
        saveData();
        return true;
    }

    public boolean rebindPhone(UUID playerId, String oldPhone, String newPhone) {
        if (!isValidPhone(newPhone)) {
            return false;
        }

        String currentPhone = phoneData.get(playerId);
        if (currentPhone == null || !currentPhone.equals(oldPhone)) {
            return false;
        }

        phoneData.put(playerId, newPhone);
        saveData();
        return true;
    }

    public boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
}
