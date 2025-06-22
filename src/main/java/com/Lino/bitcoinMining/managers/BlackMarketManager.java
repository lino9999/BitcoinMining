package com.Lino.bitcoinMining.managers;

import com.Lino.bitcoinMining.BitcoinMining;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalTime;
import java.time.Duration;
import java.util.*;

public class BlackMarketManager {

    private final BitcoinMining plugin;
    private final Map<String, BlackMarketItem> items;
    private final Map<String, Integer> stock;
    private boolean isOpen;
    private boolean alwaysOpen;
    private LocalTime openTime;
    private LocalTime closeTime;

    public BlackMarketManager(BitcoinMining plugin) {
        this.plugin = plugin;
        this.items = new HashMap<>();
        this.stock = new HashMap<>();
        this.isOpen = false;
        this.alwaysOpen = false;
        loadConfig();
    }

    public void loadConfig() {
        items.clear();

        alwaysOpen = plugin.getConfig().getBoolean("black-market.always-open", false);

        if (!alwaysOpen) {
            String timeRange = plugin.getConfig().getString("black-market.time-range", "21:00-22:00");
            String[] times = timeRange.split("-");
            openTime = LocalTime.parse(times[0]);
            closeTime = LocalTime.parse(times[1]);
        }

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
                double priceUSD = plugin.getConfig().getDouble("rig-levels.level-" + level + ".black-market-price-usd", 1000.0 * level);
                int maxStock = plugin.getConfig().getInt("black-market.default-rig-stock", 3);

                String displayName = plugin.getConfig().getString("rig-levels.level-" + level + ".display-name",
                        "§6Mining Rig §7[§eLevel " + level + "§7]");

                ItemStack rigItem = new ItemStack(Material.OBSERVER);
                ItemMeta meta = rigItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(displayName);
                    rigItem.setItemMeta(meta);
                }

                BlackMarketItem item = new BlackMarketItem(rigItem, priceUSD, maxStock);
                items.put(key, item);
            }
        }
    }

    public void addItem(String key, ItemStack item, double priceUSD) {
        int maxStock = plugin.getConfig().getInt("black-market.default-stock", 10);
        BlackMarketItem marketItem = new BlackMarketItem(item, priceUSD, maxStock);
        items.put(key, marketItem);
        saveItem(key, marketItem);
    }

    private void saveItem(String key, BlackMarketItem item) {
        ConfigurationSection section = plugin.getConfig().createSection("black-market.items." + key);
        section.set("material", item.getItem().getType().name());
        section.set("amount", item.getItem().getAmount());
        section.set("price-usd", item.getPriceUSD());
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
        if (alwaysOpen) return true;
        return isOpen;
    }

    public boolean shouldBeOpen() {
        if (alwaysOpen) return true;

        LocalTime now = LocalTime.now();
        if (openTime.isBefore(closeTime)) {
            return now.isAfter(openTime) && now.isBefore(closeTime);
        } else {
            return now.isAfter(openTime) || now.isBefore(closeTime);
        }
    }

    public String getTimeUntilOpen() {
        if (alwaysOpen) return "Always Open";

        LocalTime now = LocalTime.now();
        Duration duration;

        if (openTime.isAfter(now)) {
            duration = Duration.between(now, openTime);
        } else {
            duration = Duration.between(now, openTime).plusHours(24);
        }

        long totalMinutes = duration.toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    public boolean purchaseItem(String key, UUID playerUuid) {
        if (!isOpen() && !alwaysOpen) return false;

        BlackMarketItem item = items.get(key);
        if (item == null) return false;

        if (!alwaysOpen) {
            int currentStock = stock.getOrDefault(key, 0);
            if (currentStock <= 0) return false;
        }

        double btcPrice = plugin.getPriceManager().getCurrentPrice();
        double costInBTC = item.getPriceUSD() / btcPrice;

        if (plugin.getBitcoinManager().removeBitcoin(playerUuid, costInBTC)) {
            if (!alwaysOpen) {
                stock.put(key, stock.getOrDefault(key, 0) - 1);
            }
            return true;
        }

        return false;
    }

    public Map<String, BlackMarketItem> getItems() {
        return new HashMap<>(items);
    }

    public int getStock(String key) {
        if (alwaysOpen) return -1;
        return stock.getOrDefault(key, 0);
    }

    public void reload() {
        loadConfig();
    }

    public static class BlackMarketItem {
        private final ItemStack item;
        private final double priceUSD;
        private final int maxStock;

        public BlackMarketItem(ItemStack item, double priceUSD, int maxStock) {
            this.item = item.clone();
            this.priceUSD = priceUSD;
            this.maxStock = maxStock;
        }

        public ItemStack getItem() { return item.clone(); }
        public double getPriceUSD() { return priceUSD; }
        public int getMaxStock() { return maxStock; }

        public static BlackMarketItem fromConfig(ConfigurationSection section) {
            try {
                Material material = Material.valueOf(section.getString("material", "STONE"));
                int amount = section.getInt("amount", 1);
                double priceUSD = section.getDouble("price-usd", 100.0);
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

                return new BlackMarketItem(item, priceUSD, maxStock);
            } catch (Exception e) {
                return null;
            }
        }
    }
}