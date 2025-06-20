package com.Lino.bitcoinMining.managers;

import com.Lino.bitcoinMining.BitcoinMining;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class BlackMarketManager {

    private final BitcoinMining plugin;
    private final Map<String, BlackMarketItem> items;
    private final Map<String, Integer> stock;
    private boolean isOpen;
    private LocalTime openTime;
    private LocalTime closeTime;

    public BlackMarketManager(BitcoinMining plugin) {
        this.plugin = plugin;
        this.items = new HashMap<>();
        this.stock = new HashMap<>();
        this.isOpen = false;
        loadConfig();
    }

    public void loadConfig() {
        items.clear();

        String timeRange = plugin.getConfig().getString("black-market.time-range", "21:00-22:00");
        String[] times = timeRange.split("-");
        openTime = LocalTime.parse(times[0]);
        closeTime = LocalTime.parse(times[1]);

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("black-market.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    BlackMarketItem item = BlackMarketItem.fromConfig(itemSection);
                    if (item != null) {
                        items.put(key, item);
                    }
                }
            }
        }

        addDefaultRigs();
        resetStock();
    }

    private void addDefaultRigs() {
        for (int level = 1; level <= 20; level++) {
            String key = "rig_level_" + level;
            if (!items.containsKey(key)) {
                double price = plugin.getConfig().getDouble("rig-levels.level-" + level + ".black-market-price", 0.1 * level);
                int maxStock = plugin.getConfig().getInt("black-market.default-rig-stock", 3);

                String displayName = plugin.getConfig().getString("rig-levels.level-" + level + ".display-name",
                        "§6Mining Rig §7[§eLevel " + level + "§7]");

                ItemStack rigItem = new ItemStack(Material.OBSERVER);
                ItemMeta meta = rigItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(displayName);
                    rigItem.setItemMeta(meta);
                }

                BlackMarketItem item = new BlackMarketItem(rigItem, price, maxStock);
                items.put(key, item);
            }
        }
    }

    public void addItem(String key, ItemStack item, double price) {
        int maxStock = plugin.getConfig().getInt("black-market.default-stock", 10);
        BlackMarketItem marketItem = new BlackMarketItem(item, price, maxStock);
        items.put(key, marketItem);
        saveItem(key, marketItem);
    }

    private void saveItem(String key, BlackMarketItem item) {
        ConfigurationSection section = plugin.getConfig().createSection("black-market.items." + key);
        section.set("material", item.getItem().getType().name());
        section.set("amount", item.getItem().getAmount());
        section.set("price", item.getPrice());
        section.set("max-stock", item.getMaxStock());

        ItemMeta meta = item.getItem().getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                section.set("display-name", meta.getDisplayName());
            }
            if (meta.hasLore()) {
                section.set("lore", meta.getLore());
            }
        }

        plugin.saveConfig();
    }

    public void open() {
        isOpen = true;
        resetStock();
        Bukkit.broadcastMessage(plugin.getMessageManager().getMessage("black-market-open"));
    }

    public void close() {
        isOpen = false;
        Bukkit.broadcastMessage(plugin.getMessageManager().getMessage("black-market-closed"));
    }

    public void resetStock() {
        stock.clear();
        for (Map.Entry<String, BlackMarketItem> entry : items.entrySet()) {
            stock.put(entry.getKey(), entry.getValue().getMaxStock());
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean shouldBeOpen() {
        LocalTime now = LocalTime.now();
        if (openTime.isBefore(closeTime)) {
            return now.isAfter(openTime) && now.isBefore(closeTime);
        } else {
            return now.isAfter(openTime) || now.isBefore(closeTime);
        }
    }

    public String getTimeUntilOpen() {
        LocalTime now = LocalTime.now();
        long minutesUntilOpen;

        if (now.isBefore(openTime)) {
            minutesUntilOpen = ChronoUnit.MINUTES.between(now, openTime);
        } else {
            minutesUntilOpen = ChronoUnit.MINUTES.between(now, openTime.plusHours(24));
        }

        long hours = minutesUntilOpen / 60;
        long minutes = minutesUntilOpen % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    public boolean purchaseItem(String key, UUID playerUuid) {
        if (!isOpen) return false;

        BlackMarketItem item = items.get(key);
        if (item == null) return false;

        int currentStock = stock.getOrDefault(key, 0);
        if (currentStock <= 0) return false;

        if (plugin.getBitcoinManager().removeBitcoin(playerUuid, item.getPrice())) {
            stock.put(key, currentStock - 1);
            return true;
        }

        return false;
    }

    public Map<String, BlackMarketItem> getItems() {
        return new HashMap<>(items);
    }

    public int getStock(String key) {
        return stock.getOrDefault(key, 0);
    }

    public void reload() {
        loadConfig();
    }

    public static class BlackMarketItem {
        private final ItemStack item;
        private final double price;
        private final int maxStock;

        public BlackMarketItem(ItemStack item, double price, int maxStock) {
            this.item = item.clone();
            this.price = price;
            this.maxStock = maxStock;
        }

        public ItemStack getItem() { return item.clone(); }
        public double getPrice() { return price; }
        public int getMaxStock() { return maxStock; }

        public static BlackMarketItem fromConfig(ConfigurationSection section) {
            try {
                Material material = Material.valueOf(section.getString("material", "STONE"));
                int amount = section.getInt("amount", 1);
                double price = section.getDouble("price", 1.0);
                int maxStock = section.getInt("max-stock", 10);

                ItemStack item = new ItemStack(material, amount);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (section.contains("display-name")) {
                        meta.setDisplayName(section.getString("display-name"));
                    }
                    if (section.contains("lore")) {
                        meta.setLore(section.getStringList("lore"));
                    }
                    item.setItemMeta(meta);
                }

                return new BlackMarketItem(item, price, maxStock);
            } catch (Exception e) {
                return null;
            }
        }
    }
}