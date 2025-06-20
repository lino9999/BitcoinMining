package com.Lino.bitcoinMining.gui;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.listeners.InventoryClickListener;
import com.Lino.bitcoinMining.models.MiningRig;
import com.Lino.bitcoinMining.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredListener;
import java.text.DecimalFormat;
import java.util.Arrays;

public class MiningRigGUI {

    private final BitcoinMining plugin;
    private final MiningRig rig;
    private final DecimalFormat df = new DecimalFormat("#,##0.00000000");
    private final DecimalFormat moneyFormat = new DecimalFormat("$#,##0.00");

    public MiningRigGUI(BitcoinMining plugin, MiningRig rig) {
        this.plugin = plugin;
        this.rig = rig;
    }

    public void open(Player player) {
        for (RegisteredListener registeredListener : HandlerList.getRegisteredListeners(plugin)) {
            Listener listener = registeredListener.getListener();
            if (listener instanceof InventoryClickListener) {
                ((InventoryClickListener) listener).setPlayerOpenRig(player, rig);
                break;
            }
        }

        String rigName = plugin.getConfig().getString("rig-levels.level-" + rig.getLevel() + ".display-name",
                "Mining Rig [Level " + rig.getLevel() + "]");
        Inventory gui = Bukkit.createInventory(null, 54, "§6⛏ " + rigName);

        fillBorders(gui);

        gui.setItem(4, createInfoItem());

        gui.setItem(20, createFuelItem());
        gui.setItem(22, createStatusItem());
        gui.setItem(24, createStatisticsItem());

        gui.setItem(29, createOverclockItem());
        gui.setItem(31, createUpgradeItem());
        gui.setItem(33, createPriceChartItem());

        gui.setItem(38, createBlackMarketItem());
        gui.setItem(40, createTopMinersItem());
        gui.setItem(42, createSettingsItem());

        gui.setItem(49, createCloseItem());

        player.openInventory(gui);
    }

    private void fillBorders(Inventory gui) {
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName("§7")
                .build();

        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
            gui.setItem(45 + i, border);
        }
        for (int i = 9; i < 45; i += 9) {
            gui.setItem(i, border);
            gui.setItem(i + 8, border);
        }
    }

    private ItemStack createInfoItem() {
        String rigName = plugin.getConfig().getString("rig-levels.level-" + rig.getLevel() + ".display-name",
                "Mining Rig [Level " + rig.getLevel() + "]");

        return new ItemBuilder(Material.OBSERVER)
                .setName("§6§l" + rigName)
                .setLore(Arrays.asList(
                        "§7",
                        "§7Owner: §e" + Bukkit.getOfflinePlayer(rig.getOwnerId()).getName(),
                        "§7Level: §e" + rig.getLevel() + "/20",
                        "§7Hash Rate: §e" + df.format(rig.getEffectiveHashRate()) + " BTC/hour",
                        "§7Total Mined: §e" + df.format(rig.getTotalBitcoinMined()) + " BTC",
                        "§7",
                        "§7Location:",
                        "§7  World: §e" + rig.getLocation().getWorld().getName(),
                        "§7  X: §e" + rig.getLocation().getBlockX(),
                        "§7  Y: §e" + rig.getLocation().getBlockY(),
                        "§7  Z: §e" + rig.getLocation().getBlockZ()
                ))
                .setGlowing(true)
                .build();
    }

    private ItemStack createFuelItem() {
        int fuelPercentage = (int) ((double) rig.getFuel() / rig.getFuelCapacity() * 100);
        return new ItemBuilder(Material.COAL)
                .setName("§c§lFuel Management")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Fuel: §e" + (int)rig.getFuel() + "/" + rig.getFuelCapacity(),
                        "§7Percentage: §e" + fuelPercentage + "%",
                        "§7Consumption: §e" + String.format("%.2f", rig.getEffectiveFuelConsumption()) + "/hour",
                        "§7",
                        "§e§lCLICK§7 to add fuel"
                ))
                .build();
    }

    private ItemStack createStatusItem() {
        Material statusMaterial = rig.isActive() ? Material.LIME_DYE : Material.RED_DYE;
        String status = rig.isActive() ? "§a§lACTIVE" : "§c§lINACTIVE";

        return new ItemBuilder(statusMaterial)
                .setName(status)
                .setLore(Arrays.asList(
                        "§7",
                        "§7Status: " + (rig.isActive() ? "§aRunning" : "§cStopped"),
                        "§7Overclock: §e" + (rig.getOverclock() * 100) + "%",
                        "§7",
                        "§e§lCLICK§7 to toggle"
                ))
                .build();
    }

    private ItemStack createStatisticsItem() {
        double dailyProfit = plugin.getBitcoinManager().getDailyProfit(rig.getOwnerId());
        double btcPrice = plugin.getPriceManager().getCurrentPrice();

        return new ItemBuilder(Material.BOOK)
                .setName("§b§lStatistics")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Daily Profit: §e" + df.format(dailyProfit) + " BTC",
                        "§7Daily Profit (USD): §a" + moneyFormat.format(dailyProfit * btcPrice),
                        "§7",
                        "§7BTC Price: §a" + moneyFormat.format(btcPrice),
                        "§7Price Change: " + getPriceChangeColor() +
                                moneyFormat.format(plugin.getPriceManager().getPriceChange()) +
                                " (" + String.format("%.2f", plugin.getPriceManager().getPriceChangePercentage()) + "%)",
                        "§7",
                        "§7Your Balance: §e" + df.format(plugin.getBitcoinManager().getBalance(rig.getOwnerId())) + " BTC"
                ))
                .build();
    }

    private ItemStack createOverclockItem() {
        return new ItemBuilder(Material.REDSTONE)
                .setName("§c§lOverclocking")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Current: §e" + (rig.getOverclock() * 100) + "%",
                        "§7Max: §e300%",
                        "§7",
                        "§c⚠ Warning:",
                        "§7Higher overclock = More fuel consumption",
                        "§7",
                        "§e§lLEFT CLICK§7 to increase by 10%",
                        "§e§lRIGHT CLICK§7 to decrease by 10%"
                ))
                .build();
    }

    private ItemStack createUpgradeItem() {
        if (rig.canUpgrade()) {
            int nextLevel = rig.getNextLevel();
            double upgradeCost = rig.getUpgradeCost();

            return new ItemBuilder(Material.ANVIL)
                    .setName("§a§lUpgrade Rig")
                    .setLore(Arrays.asList(
                            "§7",
                            "§7Current Level: §e" + rig.getLevel(),
                            "§7Next Level: §a" + nextLevel,
                            "§7",
                            "§7Upgrade Benefits:",
                            "§7  Hash Rate: §e" + df.format(plugin.getConfig().getDouble("rig-levels.level-" + nextLevel + ".hash-rate")) + " BTC/hour",
                            "§7  Fuel Capacity: §e" + plugin.getConfig().getInt("rig-levels.level-" + nextLevel + ".fuel-capacity"),
                            "§7",
                            "§7Cost: §e" + df.format(upgradeCost) + " BTC",
                            "§7",
                            "§e§lCLICK§7 to upgrade"
                    ))
                    .build();
        } else {
            return new ItemBuilder(Material.BARRIER)
                    .setName("§c§lMax Level")
                    .setLore(Arrays.asList(
                            "§7",
                            "§7This rig is already at maximum level!",
                            "§7Current Level: §e" + rig.getLevel() + "/20"
                    ))
                    .build();
        }
    }

    private ItemStack createPriceChartItem() {
        return new ItemBuilder(Material.MAP)
                .setName("§e§lPrice Chart")
                .setLore(Arrays.asList(
                        "§7",
                        "§7View Bitcoin price history",
                        "§7and market trends",
                        "§7",
                        "§e§lCLICK§7 to view chart"
                ))
                .build();
    }

    private ItemStack createBlackMarketItem() {
        boolean isOpen = plugin.getBlackMarketManager().isOpen();
        Material material = isOpen ? Material.NETHERITE_SWORD : Material.IRON_BARS;

        return new ItemBuilder(material)
                .setName("§4§l⚔ Black Market ⚔")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Status: " + (isOpen ? "§a§lOPEN" : "§c§lCLOSED"),
                        "§7",
                        isOpen ? "§7Buy rare items with Bitcoin!" : "§7Opens in: §e" + plugin.getBlackMarketManager().getTimeUntilOpen(),
                        "§7",
                        isOpen ? "§e§lCLICK§7 to browse" : "§c§lNOT AVAILABLE"
                ))
                .setGlowing(isOpen)
                .build();
    }

    private ItemStack createTopMinersItem() {
        return new ItemBuilder(Material.GOLD_INGOT)
                .setName("§6§l⛏ Top Miners ⛏")
                .setLore(Arrays.asList(
                        "§7",
                        "§7View the leaderboard of",
                        "§7the best Bitcoin miners!",
                        "§7",
                        "§e§lCLICK§7 to view"
                ))
                .build();
    }

    private ItemStack createSettingsItem() {
        return new ItemBuilder(Material.COMPARATOR)
                .setName("§7§lSettings")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Configure your mining rig",
                        "§7",
                        "§c§lComing Soon"
                ))
                .build();
    }

    private ItemStack createCloseItem() {
        return new ItemBuilder(Material.BARRIER)
                .setName("§c§lClose")
                .setLore(Arrays.asList("§7Click to close this menu"))
                .build();
    }

    private String getPriceChangeColor() {
        double change = plugin.getPriceManager().getPriceChange();
        if (change > 0) return "§a+";
        else if (change < 0) return "§c";
        else return "§7";
    }
}