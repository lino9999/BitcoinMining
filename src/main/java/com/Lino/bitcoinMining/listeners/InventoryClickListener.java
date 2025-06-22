package com.Lino.bitcoinMining.listeners;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.gui.*;
import com.Lino.bitcoinMining.managers.BlackMarketManager;
import com.Lino.bitcoinMining.models.MiningRig;
import com.Lino.bitcoinMining.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventoryClickListener implements Listener {

    private final BitcoinMining plugin;
    private final Map<UUID, MiningRig> playerOpenRigs = new HashMap<>();

    public InventoryClickListener(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.startsWith("§6⛏ Mining Rig") || title.contains("Level")) {
            handleMiningRigGUI(event, player, title);
        } else if (title.equals("§c⛽ Fuel Management")) {
            handleFuelGUI(event, player);
        } else if (title.equals("§e📈 Bitcoin Price Chart")) {
            handlePriceChartGUI(event, player);
        } else if (title.equals("§4§l⚔ Black Market ⚔")) {
            handleBlackMarketGUI(event, player);
        } else if (title.equals("§6§l⛏ Top Miners ⛏")) {
            handleTopMinersGUI(event, player);
        }
    }

    private void handleMiningRigGUI(InventoryClickEvent event, Player player, String title) {
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        MiningRig rig = playerOpenRigs.get(player.getUniqueId());
        if (rig == null) {
            rig = findRigFromTitle(title);
        }
        if (rig == null) {
            player.closeInventory();
            return;
        }

        playerOpenRigs.put(player.getUniqueId(), rig);

        int slot = event.getSlot();

        switch (slot) {
            case 20:
                handleFuelClick(player, rig);
                break;
            case 22:
                handleStatusClick(player, rig);
                break;
            case 29:
                handleOverclockClick(player, rig, event.getClick());
                break;
            case 31:
                handleUpgradeClick(player, rig);
                break;
            case 33:
                handlePriceChartClick(player);
                break;
            case 38:
                handleBlackMarketClick(player);
                break;
            case 40:
                handleTopMinersClick(player);
                break;
            case 49:
                player.closeInventory();
                playerOpenRigs.remove(player.getUniqueId());
                plugin.getMessageManager().sendMessage(player, "gui-close");
                break;
        }
    }

    private void handleFuelGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int slot = event.getSlot();

        MiningRig rig = playerOpenRigs.get(player.getUniqueId());
        if (rig == null) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 20:
                addFuelToRig(player, rig, 1);
                break;
            case 22:
                addFuelToRig(player, rig, 9);
                break;
            case 24:
                addAllFuelToRig(player, rig);
                break;
            case 31:
                new MiningRigGUI(plugin, rig).open(player);
                break;
        }
    }

    private void handlePriceChartGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int slot = event.getSlot();

        switch (slot) {
            case 49:
                MiningRig rig = playerOpenRigs.get(player.getUniqueId());
                if (rig != null) {
                    new MiningRigGUI(plugin, rig).open(player);
                } else {
                    player.closeInventory();
                }
                break;
            case 53:
                plugin.getPriceManager().updatePrice();
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    new PriceChartGUI(plugin).open(player);
                }, 20L);
                break;
        }
    }

    private void handleBlackMarketGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int slot = event.getSlot();

        if (slot == 45 || slot == 53) {
            int currentPage = 0;
            if (slot == 45) currentPage = Math.max(0, currentPage - 1);
            else currentPage++;
            new BlackMarketGUI(plugin).open(player, currentPage);
            return;
        }

        if (slot == 49) {
            MiningRig rig = playerOpenRigs.get(player.getUniqueId());
            if (rig != null) {
                new MiningRigGUI(plugin, rig).open(player);
            } else {
                player.closeInventory();
            }
            return;
        }

        if (slot >= 10 && slot <= 43) {
            int index = slot - 10;
            if ((slot - 8) % 9 < 7) {
                List<String> keys = new java.util.ArrayList<>(plugin.getBlackMarketManager().getItems().keySet());
                if (index < keys.size()) {
                    String itemKey = keys.get(index);
                    BlackMarketManager.BlackMarketItem item = plugin.getBlackMarketManager().getItems().get(itemKey);

                    if (plugin.getBlackMarketManager().purchaseItem(itemKey, player.getUniqueId())) {
                        ItemStack purchasedItem = item.getItem();

                        if (itemKey.startsWith("rig_level_")) {
                            int level = Integer.parseInt(itemKey.replace("rig_level_", ""));
                            purchasedItem = createRigItem(level);
                        }

                        player.getInventory().addItem(purchasedItem);
                        plugin.getMessageManager().sendMessage(player, "black-market-purchase-success",
                                "%item%", purchasedItem.getType().name(),
                                "%price%", String.format("%.8f", item.getPriceUSD()));

                        new BlackMarketGUI(plugin).open(player, 0);
                    } else {
                        plugin.getMessageManager().sendMessage(player, "black-market-purchase-failed");
                    }
                }
            }
        }
    }

    private void handleTopMinersGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int slot = event.getSlot();

        if (slot == 45) {
            MiningRig rig = playerOpenRigs.get(player.getUniqueId());
            if (rig != null) {
                new MiningRigGUI(plugin, rig).open(player);
            } else {
                player.closeInventory();
            }
        }
    }

    private MiningRig findRigFromTitle(String title) {
        for (MiningRig rig : plugin.getMiningRigManager().getAllRigs()) {
            String rigName = plugin.getConfig().getString("rig-levels.level-" + rig.getLevel() + ".display-name",
                    "Mining Rig [Level " + rig.getLevel() + "]");
            if (title.contains(rigName) || title.contains("Level " + rig.getLevel())) {
                return rig;
            }
        }
        return null;
    }

    private void handleFuelClick(Player player, MiningRig rig) {
        new FuelGUI(plugin, rig).open(player);
    }

    private void handleStatusClick(Player player, MiningRig rig) {
        rig.setActive(!rig.isActive());
        plugin.getMiningRigManager().saveRig(rig);

        String status = rig.isActive() ? "enabled" : "disabled";
        plugin.getMessageManager().sendMessage(player, "gui-toggle-rig",
                "%status%", status);

        new MiningRigGUI(plugin, rig).open(player);
    }

    private void handleOverclockClick(Player player, MiningRig rig, ClickType clickType) {
        double currentOverclock = rig.getOverclock();
        double newOverclock = currentOverclock;

        if (clickType == ClickType.LEFT) {
            if (currentOverclock < 3.0) {
                newOverclock = Math.min(3.0, currentOverclock + 0.1);
                rig.setOverclock(newOverclock);
                plugin.getMessageManager().sendMessage(player, "gui-overclock-increased",
                        "%percent%", String.format("%.0f", newOverclock * 100));
            } else {
                plugin.getMessageManager().sendMessage(player, "gui-overclock-max");
            }
        } else if (clickType == ClickType.RIGHT) {
            if (currentOverclock > 1.0) {
                newOverclock = Math.max(1.0, currentOverclock - 0.1);
                rig.setOverclock(newOverclock);
                plugin.getMessageManager().sendMessage(player, "gui-overclock-decreased",
                        "%percent%", String.format("%.0f", newOverclock * 100));
            } else {
                plugin.getMessageManager().sendMessage(player, "gui-overclock-min");
            }
        }

        if (newOverclock != currentOverclock) {
            plugin.getMiningRigManager().saveRig(rig);
            new MiningRigGUI(plugin, rig).open(player);
        }
    }

    private void handleUpgradeClick(Player player, MiningRig rig) {
        if (!rig.canUpgrade()) {
            plugin.getMessageManager().sendMessage(player, "rig-upgrade-max-level");
            return;
        }

        double cost = rig.getUpgradeCost();

        if (plugin.getBitcoinManager().removeBitcoin(player.getUniqueId(), cost)) {
            rig.upgrade();
            plugin.getMiningRigManager().saveRig(rig);

            plugin.getMessageManager().sendMessage(player, "rig-upgraded",
                    "%level%", String.valueOf(rig.getLevel()));

            new MiningRigGUI(plugin, rig).open(player);
        } else {
            plugin.getMessageManager().sendMessage(player, "rig-upgrade-not-enough-btc",
                    "%cost%", String.format("%.8f", cost));
        }
    }

    private void handlePriceChartClick(Player player) {
        new PriceChartGUI(plugin).open(player);
    }

    private void handleBlackMarketClick(Player player) {
        if (plugin.getBlackMarketManager().isOpen()) {
            new BlackMarketGUI(plugin).open(player, 0);
        } else {
            plugin.getMessageManager().sendMessage(player, "black-market-closed-try-later",
                    "%time%", plugin.getBlackMarketManager().getTimeUntilOpen());
        }
    }

    private void handleTopMinersClick(Player player) {
        new TopMinersGUI(plugin).open(player);
    }

    private void addFuelToRig(Player player, MiningRig rig, int amount) {
        ItemStack coalItem = new ItemStack(Material.COAL, amount);

        if (!player.getInventory().containsAtLeast(coalItem, amount)) {
            plugin.getMessageManager().sendMessage(player, "not-enough-coal");
            return;
        }

        double currentFuel = rig.getFuel();
        int maxFuel = rig.getFuelCapacity();
        int maxCanAdd = maxFuel - (int)currentFuel;
        int actualAdded = Math.min(amount, maxCanAdd);

        if (actualAdded > 0) {
            player.getInventory().removeItem(new ItemStack(Material.COAL, actualAdded));
            rig.addFuel(actualAdded);
            plugin.getMiningRigManager().saveRig(rig);

            plugin.getMessageManager().sendMessage(player, "fuel-added",
                    "%amount%", String.valueOf(actualAdded),
                    "%total%", String.valueOf((int)rig.getFuel()));

            new FuelGUI(plugin, rig).open(player);
        } else {
            plugin.getMessageManager().sendMessage(player, "fuel-tank-full");
        }
    }

    private void addAllFuelToRig(Player player, MiningRig rig) {
        int totalCoal = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                if (item.getType() == Material.COAL) {
                    totalCoal += item.getAmount();
                } else if (item.getType() == Material.COAL_BLOCK) {
                    totalCoal += item.getAmount() * 9;
                }
            }
        }

        if (totalCoal == 0) {
            plugin.getMessageManager().sendMessage(player, "not-enough-coal");
            return;
        }

        double currentFuel = rig.getFuel();
        int maxFuel = rig.getFuelCapacity();
        int maxCanAdd = maxFuel - (int)currentFuel;
        int actualAdded = Math.min(totalCoal, maxCanAdd);

        if (actualAdded > 0) {
            int remainingToAdd = actualAdded;

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && remainingToAdd > 0) {
                    if (item.getType() == Material.COAL_BLOCK) {
                        int blocksToRemove = Math.min(item.getAmount(), remainingToAdd / 9);
                        if (blocksToRemove > 0) {
                            item.setAmount(item.getAmount() - blocksToRemove);
                            remainingToAdd -= blocksToRemove * 9;
                        }
                    }
                }
            }

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && remainingToAdd > 0) {
                    if (item.getType() == Material.COAL) {
                        int coalToRemove = Math.min(item.getAmount(), remainingToAdd);
                        item.setAmount(item.getAmount() - coalToRemove);
                        remainingToAdd -= coalToRemove;
                    }
                }
            }

            rig.addFuel(actualAdded);
            plugin.getMiningRigManager().saveRig(rig);

            plugin.getMessageManager().sendMessage(player, "fuel-added",
                    "%amount%", String.valueOf(actualAdded),
                    "%total%", String.valueOf((int)rig.getFuel()));

            new FuelGUI(plugin, rig).open(player);
        } else {
            plugin.getMessageManager().sendMessage(player, "fuel-tank-full");
        }
    }

    private ItemStack createRigItem(int level) {
        String displayName = plugin.getConfig().getString("rig-levels.level-" + level + ".display-name",
                "§6Mining Rig §7[§eLevel " + level + "§7]");

        double hashRate = plugin.getConfig().getDouble("rig-levels.level-" + level + ".hash-rate", 0.001 * level);
        int fuelCapacity = plugin.getConfig().getInt("rig-levels.level-" + level + ".fuel-capacity", 64 + (level * 20));
        double fuelConsumption = plugin.getConfig().getDouble("rig-levels.level-" + level + ".fuel-consumption", 1.0 - (level * 0.02));

        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "mining_rig_level");

        ItemStack item = new ItemBuilder(Material.OBSERVER)
                .setName(displayName)
                .setLore(java.util.Arrays.asList(
                        "§7",
                        "§7Level: §e" + level,
                        "§7Hash Rate: §e" + String.format("%.6f", hashRate) + " BTC/hour",
                        "§7Fuel Capacity: §e" + fuelCapacity,
                        "§7Fuel Efficiency: §e" + String.format("%.2f", (2.0 - fuelConsumption) * 100) + "%",
                        "§7",
                        "§e§lRIGHT CLICK§7 to place"
                ))
                .setGlowing(true)
                .setPersistentData(key, org.bukkit.persistence.PersistentDataType.INTEGER, level)
                .build();

        return item;
    }

    public void setPlayerOpenRig(Player player, MiningRig rig) {
        playerOpenRigs.put(player.getUniqueId(), rig);
    }
}