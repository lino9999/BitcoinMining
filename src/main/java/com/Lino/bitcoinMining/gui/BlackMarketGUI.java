package com.Lino.bitcoinMining.gui;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.managers.BlackMarketManager;
import com.Lino.bitcoinMining.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

public class BlackMarketGUI {

    private final BitcoinMining plugin;
    private final DecimalFormat df = new DecimalFormat("#,##0.00000000");
    private final DecimalFormat moneyFormat = new DecimalFormat("$#,##0.00");
    private int currentPage = 0;

    public BlackMarketGUI(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        this.currentPage = page;
        Inventory gui = Bukkit.createInventory(null, 54, "§4§l⚔ Black Market ⚔");

        fillBorders(gui);

        Map<String, BlackMarketManager.BlackMarketItem> items = plugin.getBlackMarketManager().getItems();
        List<String> keys = new ArrayList<>(items.keySet());

        int startIndex = page * 28;
        int endIndex = Math.min(startIndex + 28, keys.size());

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            String key = keys.get(i);
            BlackMarketManager.BlackMarketItem marketItem = items.get(key);
            int stock = plugin.getBlackMarketManager().getStock(key);

            ItemStack displayItem = createMarketItem(marketItem, stock, key, player);
            gui.setItem(slot, displayItem);

            slot++;
            if ((slot - 8) % 9 == 0) slot += 2;
        }

        if (page > 0) {
            gui.setItem(45, createNavigationItem(Material.ARROW, "§e§lPrevious Page", page - 1));
        }

        if (endIndex < keys.size()) {
            gui.setItem(53, createNavigationItem(Material.ARROW, "§e§lNext Page", page + 1));
        }

        gui.setItem(49, createCloseItem());

        player.openInventory(gui);
    }

    private void fillBorders(Inventory gui) {
        ItemStack border = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
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

    private ItemStack createMarketItem(BlackMarketManager.BlackMarketItem marketItem, int stock, String key, Player player) {
        ItemStack baseItem = marketItem.getItem();
        ItemMeta meta = baseItem.getItemMeta();

        double btcPrice = plugin.getPriceManager().getCurrentPrice();
        double costInBTC = marketItem.getPriceUSD() / btcPrice;
        double playerBalance = plugin.getBitcoinManager().getBalance(player.getUniqueId());

        List<String> lore = new ArrayList<>();
        if (meta != null && meta.hasLore()) {
            lore.addAll(meta.getLore());
        }

        lore.add("§7");
        lore.add("§7Price: §a" + moneyFormat.format(marketItem.getPriceUSD()));
        lore.add("§7Cost in BTC: §e" + df.format(costInBTC) + " BTC");
        lore.add("§7Current BTC Price: §6" + moneyFormat.format(btcPrice));

        lore.add("§7");
        lore.add("§7Your Balance: §e" + df.format(playerBalance) + " BTC");

        if (playerBalance >= costInBTC) {
            lore.add("§a§l✓ You can afford this!");
        } else {
            double needed = costInBTC - playerBalance;
            lore.add("§c§l✗ You need §e" + df.format(needed) + " BTC§c more");
        }

        if (stock == -1) {
            lore.add("§7Stock: §a§lUNLIMITED");
        } else if (stock > 0) {
            lore.add("§7Stock: §a" + stock + "/" + marketItem.getMaxStock());
        } else {
            lore.add("§7Stock: §c§lOUT OF STOCK");
        }

        lore.add("§7");

        if (stock != 0 && playerBalance >= costInBTC) {
            lore.add("§e§lCLICK§7 to purchase");
        } else if (stock == 0) {
            lore.add("§c§lNOT AVAILABLE");
        } else {
            lore.add("§c§lINSUFFICIENT FUNDS");
        }

        ItemStack displayItem = baseItem.clone();
        ItemMeta displayMeta = displayItem.getItemMeta();
        if (displayMeta != null) {
            displayMeta.setLore(lore);
            displayItem.setItemMeta(displayMeta);
        }

        return displayItem;
    }

    private ItemStack createNavigationItem(Material material, String name, int page) {
        return new ItemBuilder(material)
                .setName(name)
                .setLore(Arrays.asList("§7Click to go to page " + (page + 1)))
                .build();
    }

    private ItemStack createCloseItem() {
        return new ItemBuilder(Material.BARRIER)
                .setName("§c§lClose")
                .setLore(Arrays.asList("§7Return to mining rig"))
                .build();
    }
}