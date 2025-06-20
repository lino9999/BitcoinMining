package com.Lino.bitcoinMining.gui;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.DecimalFormat;
import java.util.*;

public class TopMinersGUI {

    private final BitcoinMining plugin;
    private final DecimalFormat df = new DecimalFormat("#,##0.00000000");

    public TopMinersGUI(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6§l⛏ Top Miners ⛏");

        fillBorders(gui);

        Map<UUID, Double> topMiners = plugin.getLeaderboardManager().getLeaderboard();

        int[] slots = {4, 12, 14, 19, 21, 23, 25, 28, 30, 32, 34};
        int position = 1;

        for (Map.Entry<UUID, Double> entry : topMiners.entrySet()) {
            if (position > 10) break;

            OfflinePlayer miner = Bukkit.getOfflinePlayer(entry.getKey());
            ItemStack head = createPlayerHead(miner, position, entry.getValue());

            if (position <= slots.length) {
                gui.setItem(slots[position - 1], head);
            }

            position++;
        }

        int playerRank = plugin.getLeaderboardManager().getPlayerRank(player.getUniqueId());
        if (playerRank > 0) {
            double playerAmount = plugin.getBitcoinManager().getBalance(player.getUniqueId());
            gui.setItem(49, createOwnRankItem(player, playerRank, playerAmount));
        }

        gui.setItem(45, createBackItem());

        player.openInventory(gui);
    }

    private void fillBorders(Inventory gui) {
        ItemStack border = new ItemBuilder(Material.GOLD_BLOCK)
                .setName("§7")
                .build();

        for (int i = 0; i < 54; i++) {
            gui.setItem(i, border);
        }
    }

    private ItemStack createPlayerHead(OfflinePlayer player, int position, double amount) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(player);

            String displayName;
            if (position == 1) {
                displayName = "§6§l#1 §e" + (player.getName() != null ? player.getName() : "Unknown");
            } else if (position == 2) {
                displayName = "§7§l#2 §f" + (player.getName() != null ? player.getName() : "Unknown");
            } else if (position == 3) {
                displayName = "§c§l#3 §e" + (player.getName() != null ? player.getName() : "Unknown");
            } else {
                displayName = "§e#" + position + " §7" + (player.getName() != null ? player.getName() : "Unknown");
            }

            meta.setDisplayName(displayName);

            List<String> lore = Arrays.asList(
                    "§7",
                    "§7Total Mined: §e" + df.format(amount) + " BTC",
                    "§7USD Value: §a$" + String.format("%,.2f", amount * plugin.getPriceManager().getCurrentPrice()),
                    "§7",
                    getMedalForPosition(position)
            );

            meta.setLore(lore);
            head.setItemMeta(meta);
        }

        return head;
    }

    private ItemStack createOwnRankItem(Player player, int rank, double amount) {
        return new ItemBuilder(Material.EMERALD)
                .setName("§a§lYour Position")
                .setLore(Arrays.asList(
                        "§7",
                        "§7Rank: §e#" + rank,
                        "§7Total Mined: §e" + df.format(amount) + " BTC",
                        "§7",
                        "§aKeep mining to climb the ranks!"
                ))
                .setGlowing(true)
                .build();
    }

    private ItemStack createBackItem() {
        return new ItemBuilder(Material.ARROW)
                .setName("§c§lBack")
                .setLore(Arrays.asList("§7Return to mining rig"))
                .build();
    }

    private String getMedalForPosition(int position) {
        switch (position) {
            case 1:
                return "§6§l🏆 CHAMPION 🏆";
            case 2:
                return "§7§l🥈 RUNNER-UP 🥈";
            case 3:
                return "§c§l🥉 THIRD PLACE 🥉";
            default:
                return "§7Position " + position;
        }
    }
}