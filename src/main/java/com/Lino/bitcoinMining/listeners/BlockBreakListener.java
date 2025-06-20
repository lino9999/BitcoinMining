package com.Lino.bitcoinMining.listeners;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.models.MiningRig;
import com.Lino.bitcoinMining.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class BlockBreakListener implements Listener {

    private final BitcoinMining plugin;

    public BlockBreakListener(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        MiningRig rig = plugin.getMiningRigManager().getRigAt(block.getLocation());
        if (rig == null) return;

        event.setCancelled(true);

        if (!rig.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("bitcoinmining.admin")) {
            plugin.getMessageManager().sendMessage(player, "rig-not-owner");
            return;
        }

        if (!player.hasPermission("bitcoinmining.break")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return;
        }

        plugin.getMiningRigManager().removeRig(rig);

        block.setType(Material.AIR);

        ItemStack rigItem = createRigItem(rig.getLevel());
        block.getWorld().dropItemNaturally(block.getLocation(), rigItem);

        if (rig.getFuel() > 0) {
            int totalFuel = (int)rig.getFuel();
            int coalBlocks = totalFuel / 9;
            int coal = totalFuel % 9;

            if (coalBlocks > 0) {
                block.getWorld().dropItemNaturally(
                        block.getLocation(),
                        new ItemStack(Material.COAL_BLOCK, coalBlocks)
                );
            }

            if (coal > 0) {
                block.getWorld().dropItemNaturally(
                        block.getLocation(),
                        new ItemStack(Material.COAL, coal)
                );
            }
        }

        plugin.getMessageManager().sendMessage(player, "rig-broken");
    }

    private ItemStack createRigItem(int level) {
        String displayName = plugin.getConfig().getString("rig-levels.level-" + level + ".display-name",
                "§6Mining Rig §7[§eLevel " + level + "§7]");

        double hashRate = plugin.getConfig().getDouble("rig-levels.level-" + level + ".hash-rate", 0.001 * level);
        int fuelCapacity = plugin.getConfig().getInt("rig-levels.level-" + level + ".fuel-capacity", 64 + (level * 20));
        double fuelConsumption = plugin.getConfig().getDouble("rig-levels.level-" + level + ".fuel-consumption", 1.0 - (level * 0.02));

        NamespacedKey key = new NamespacedKey(plugin, "mining_rig_level");

        ItemStack item = new ItemBuilder(Material.OBSERVER)
                .setName(displayName)
                .setLore(Arrays.asList(
                        "§7",
                        "§7Level: §e" + level,
                        "§7Hash Rate: §e" + String.format("%.6f", hashRate) + " BTC/hour",
                        "§7Fuel Capacity: §e" + fuelCapacity,
                        "§7Fuel Efficiency: §e" + String.format("%.2f", (2.0 - fuelConsumption) * 100) + "%",
                        "§7",
                        "§e§lRIGHT CLICK§7 to place"
                ))
                .setGlowing(true)
                .setPersistentData(key, PersistentDataType.INTEGER, level)
                .build();

        return item;
    }
}