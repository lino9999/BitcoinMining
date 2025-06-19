package com.Lino.bitcoinMining.gui;

import com.yourserver.bitcoinmining.BitcoinMining;
import com.yourserver.bitcoinmining.api.PriceManager;
import com.yourserver.bitcoinmining.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class PriceChartGUI {

    private final BitcoinMining plugin;
    private final DecimalFormat moneyFormat = new DecimalFormat("$#,##0.00");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public PriceChartGUI(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§e📈 Bitcoin Price Chart");

        fillBorders(gui);

        gui.setItem(4, createCurrentPriceItem());

        createPriceChart(gui);

        gui.setItem(45, createLegendItem());
        gui.setItem(49, createBackItem());
        gui.setItem(53, createRefreshItem());

        player.openInventory(gui);
    }

    private void fillBorders(Inventory gui) {
        ItemStack border = new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE)
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

    private ItemStack createCurrentPriceItem() {
        double currentPrice = plugin.getPriceManager().getCurrentPrice();
        double priceChange = plugin.getPriceManager().getPriceChange();
        double priceChangePercent = plugin.getPriceManager().getPriceChangePercentage();

        String changeColor = priceChange >= 0 ? "§a" : "§c";
        String changeSign = priceChange >= 0 ? "+" : "";
        String changeArrow = priceChange >= 0 ? "⬆" : "⬇";

        return new ItemBuilder(Material.GOLD_INGOT)
                .setName("§6§lBitcoin Price")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Current: §a" + moneyFormat.format(currentPrice),
                        "§7Change: " + changeColor + changeSign + moneyFormat.format(priceChange),
                        "§7Percent: " + changeColor + changeSign + String.format("%.2f%%", priceChangePercent),
                        "§7",
                        changeColor + "§l" + changeArrow + " " + changeArrow + " " + changeArrow
                ))
                .setGlowing(true)
                .build();
    }

    private void createPriceChart(Inventory gui) {
        List<PriceManager.PriceData> history = plugin.getPriceManager().getPriceHistory();

        if (history.size() < 2) {
            ItemStack noData = new ItemBuilder(Material.BARRIER)
                    .setName("§c§lNo Price History")
                    .setLore(Arrays.asList("§7Not enough data to display chart"))
                    .build();
            gui.setItem(22, noData);
            return;
        }

        double minPrice = history.stream().mapToDouble(PriceManager.PriceData::getPrice).min().orElse(0);
        double maxPrice = history.stream().mapToDouble(PriceManager.PriceData::getPrice).max().orElse(0);
        double priceRange = maxPrice - minPrice;

        int chartWidth = 7;
        int chartHeight = 3;
        int startSlot = 19;

        int dataPointsToShow = Math.min(history.size(), chartWidth);
        int step = Math.max(1, history.size() / dataPointsToShow);

        for (int x = 0; x < chartWidth; x++) {
            int dataIndex = x * step;
            if (dataIndex >= history.size()) break;

            PriceManager.PriceData data = history.get(dataIndex);
            double normalizedPrice = (data.getPrice() - minPrice) / priceRange;
            int height = (int) (normalizedPrice * (chartHeight - 1));

            for (int y = 0; y < chartHeight; y++) {
                int slot = startSlot + (2 - y) * 9 + x;

                if (y == height) {
                    Material material = getChartMaterial(data.getPrice(), currentPrice(history));
                    ItemStack pricePoint = new ItemBuilder(material)
                            .setName("§e" + timeFormat.format(new Date(data.getTimestamp())))
                            .setLore(Arrays.asList(
                                    "§7Price: §a" + moneyFormat.format(data.getPrice()),
                                    "§7Time: §f" + new SimpleDateFormat("dd/MM HH:mm").format(new Date(data.getTimestamp()))
                            ))
                            .build();
                    gui.setItem(slot, pricePoint);
                } else {
                    gui.setItem(slot, new ItemStack(Material.AIR));
                }
            }
        }
    }

    private double currentPrice(List<PriceManager.PriceData> history) {
        return history.isEmpty() ? 0 : history.get(history.size() - 1).getPrice();
    }

    private Material getChartMaterial(double price, double currentPrice) {
        double percentDiff = ((price - currentPrice) / currentPrice) * 100;

        if (Math.abs(percentDiff) < 1) {
            return Material.YELLOW_CONCRETE;
        } else if (percentDiff > 0) {
            return Material.LIME_CONCRETE;
        } else {
            return Material.RED_CONCRETE;
        }
    }

    private ItemStack createLegendItem() {
        return new ItemBuilder(Material.BOOK)
                .setName("§6§lChart Legend")
                .setLore(Arrays.asList(
                        "§7",
                        "§a■ §7Higher than current",
                        "§e■ §7Same as current (±1%)",
                        "§c■ §7Lower than current",
                        "§7",
                        "§7Showing last 24 hours"
                ))
                .build();
    }

    private ItemStack createBackItem() {
        return new ItemBuilder(Material.ARROW)
                .setName("§c§lBack")
                .setLore(Arrays.asList("§7Return to mining rig"))
                .build();
    }

    private ItemStack createRefreshItem() {
        return new ItemBuilder(Material.CLOCK)
                .setName("§a§lRefresh")
                .setLore(Arrays.asList(
                        "§7Update price data",
                        "§7",
                        "§e§lCLICK§7 to refresh"
                ))
                .build();
    }
}