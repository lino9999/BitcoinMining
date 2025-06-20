package com.Lino.bitcoinMining.listeners;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.models.MiningRig;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class BlockPlaceListener implements Listener {

    private final BitcoinMining plugin;

    public BlockPlaceListener(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (block.getType() != Material.OBSERVER) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "mining_rig_level");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
            return;
        }

        int level = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);

        if (!player.hasPermission("bitcoinmining.place")) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return;
        }

        if (!plugin.getMiningRigManager().canPlayerPlaceRig(player)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "rig-limit-reached",
                    "%limit%", String.valueOf(plugin.getMiningRigManager().getMaxRigsPerPlayer()));
            return;
        }

        plugin.getMiningRigManager().createRig(player, block, level);
        plugin.getMessageManager().sendMessage(player, "rig-placed");
    }
}