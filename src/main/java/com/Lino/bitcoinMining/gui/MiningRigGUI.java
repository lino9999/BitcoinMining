package com.Lino.bitcoinMining.gui;

import com.Lino.bitcoinmining.BitcoinMining;
import com.Lino.bitcoinmining.models.MiningRig;
import com.Lino.bitcoinmining.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
        Inventory gui = Bukkit.createInventory(null, 54, "§6⛏ Mining Rig - " + rig.getType().getName());

        fillBorders(gui);

        gui.setItem(4, createInfoItem());

        gui.setItem(20, createFuelItem());
        gui.setItem(22, createStatusItem());
        gui.setItem(24, createStatisticsItem());

        gui.setItem(29, createOverclockItem());
        gui.setItem(31, createUpgradeItem());
        gui.setItem(33, createPriceChartItem());

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
        return new ItemBuilder(getMaterialForRig())
                .setName("§6§l" + rig.getType().getName() + " Mining Rig")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Owner: §e" + Bukkit.getOfflinePlayer(rig.getOwnerId()).getName(),
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
        int fuelPercentage = (int) ((double) rig.getFuel() / rig.getType().getFuelCapacity() * 100);
        return new ItemBuilder(Material.COAL)
                .setName("§c§lFuel Management")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Fuel: §e" + rig.getFuel() + "/" + rig.getType().getFuelCapacity(),
                        "§7Percentage: §e" + fuelPercentage + "%",
                        "§7Consumption: §e" + rig.getEffectiveFuelConsumption() + "/hour",
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
            MiningRig.RigType nextTier = rig.getNextTier();
            return new ItemBuilder(Material.ANVIL)
                    .setName("§a§lUpgrade Rig")
                    .setLore(Arrays.asList(
                            "§7",
                            "§7Current: §e" + rig.getType().getName(),
                            "§7Next: §a" + nextTier.getName(),
                            "§7",
                            "§7Upgrade Benefits:",
                            "§7  Hash Rate: §e" + df.format(nextTier.getBaseHashRate()) + " BTC/hour",
                            "§7  Fuel Capacity: §e" + nextTier.getFuelCapacity(),
                            "§7  Efficiency: §a+" + ((1 - nextTier.getFuelConsumption() / rig.getType().getFuelConsumption()) * 100) + "%",
                            "§7",
                            "§7Cost: §e" + df.format(nextTier.getUpgradeCost()) + " BTC",
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
                            "§7Current: §e" + rig.getType().getName()
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

    private ItemStack createCloseItem() {
        return new ItemBuilder(Material.BARRIER)
                .setName("§c§lClose")
                .setLore(Arrays.asList("§7Click to close this menu"))
                .build();
    }

    private Material getMaterialForRig() {
        switch (rig.getType()) {
            case BRONZE: return Material.COPPER_BLOCK;
            case SILVER: return Material.IRON_BLOCK;
            case GOLD: return Material.GOLD_BLOCK;
            case DIAMOND: return Material.DIAMOND_BLOCK;
            default: return Material.STONE;
        }
    }

    private String getPriceChangeColor() {
        double change = plugin.getPriceManager().getPriceChange();
        if (change > 0) return "§a+";
        else if (change < 0) return "§c";
        else return "§7";
    }
}