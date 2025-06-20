package com.Lino.bitcoinMining.gui;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.models.MiningRig;
import com.Lino.bitcoinMining.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.Arrays;

public class FuelGUI {

    private final BitcoinMining plugin;
    private final MiningRig rig;

    public FuelGUI(BitcoinMining plugin, MiningRig rig) {
        this.plugin = plugin;
        this.rig = rig;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, "§c⛽ Fuel Management");

        ItemStack border = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setName("§7")
                .build();

        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
            gui.setItem(36 + i, border);
        }
        for (int i = 9; i < 36; i += 9) {
            gui.setItem(i, border);
            gui.setItem(i + 8, border);
        }

        gui.setItem(13, createFuelInfoItem());

        gui.setItem(20, createAddCoalItem());
        gui.setItem(22, createAddCoalBlockItem());
        gui.setItem(24, createAddStackItem());

        gui.setItem(31, createBackItem());

        player.openInventory(gui);
    }

    private ItemStack createFuelInfoItem() {
        int fuelPercentage = (int) ((double) rig.getFuel() / rig.getFuelCapacity() * 100);
        String fuelBar = createProgressBar(fuelPercentage, 20);

        return new ItemBuilder(Material.COAL)
                .setName("§c§lCurrent Fuel Status")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Fuel: §e" + rig.getFuel() + "/" + rig.getFuelCapacity(),
                        "§7Percentage: §e" + fuelPercentage + "%",
                        "§7",
                        fuelBar,
                        "§7",
                        "§7Consumption Rate: §e" + rig.getEffectiveFuelConsumption() + "/hour",
                        "§7Time Remaining: §e" + formatTimeRemaining()
                ))
                .setGlowing(true)
                .build();
    }

    private ItemStack createAddCoalItem() {
        return new ItemBuilder(Material.COAL)
                .setName("§e§lAdd 1 Coal")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Click to add 1 coal",
                        "§7to the fuel tank",
                        "§7",
                        "§e§lCLICK§7 to add"
                ))
                .build();
    }

    private ItemStack createAddCoalBlockItem() {
        return new ItemBuilder(Material.COAL_BLOCK)
                .setName("§e§lAdd 1 Coal Block")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Click to add 9 coal",
                        "§7to the fuel tank",
                        "§7",
                        "§e§lCLICK§7 to add"
                ))
                .build();
    }

    private ItemStack createAddStackItem() {
        return new ItemBuilder(Material.FURNACE)
                .setName("§e§lAdd All Coal")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Click to add all coal",
                        "§7from your inventory",
                        "§7",
                        "§e§lCLICK§7 to add all"
                ))
                .build();
    }

    private ItemStack createBackItem() {
        return new ItemBuilder(Material.ARROW)
                .setName("§c§lBack")
                .setLore(Arrays.asList("§7Return to main menu"))
                .build();
    }

    private String createProgressBar(int percentage, int length) {
        int filled = (percentage * length) / 100;
        StringBuilder bar = new StringBuilder("§c[");

        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("§a█");
            } else {
                bar.append("§7█");
            }
        }

        bar.append("§c]");
        return bar.toString();
    }

    private String formatTimeRemaining() {
        if (rig.getFuel() <= 0 || !rig.isActive()) {
            return "N/A";
        }

        double hoursRemaining = rig.getFuel() / rig.getEffectiveFuelConsumption();

        if (hoursRemaining < 1) {
            return String.format("%.0f minutes", hoursRemaining * 60);
        } else if (hoursRemaining < 24) {
            return String.format("%.1f hours", hoursRemaining);
        } else {
            return String.format("%.1f days", hoursRemaining / 24);
        }
    }
}