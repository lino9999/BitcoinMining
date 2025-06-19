package com.Lino.bitcoinMining.listeners;

import com.Lino.bitcoinmining.BitcoinMining;
import com.Lino.bitcoinmining.models.MiningRig;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {

    private final BitcoinMining plugin;

    public BlockPlaceListener(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (!plugin.getMiningRigManager().isValidRigBlock(block)) {
            return;
        }

        if (!player.hasPermission("bitcoinmining.place")) {
            return;
        }

        if (!plugin.getMiningRigManager().canPlayerPlaceRig(player)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "rig-limit-reached",
                    "%limit%", String.valueOf(plugin.getMiningRigManager().getMaxRigsPerPlayer()));
            return;
        }

        MiningRig.RigType type = plugin.getMiningRigManager().getRigTypeFromBlock(block);
        plugin.getMiningRigManager().createRig(player, block, type);

        plugin.getMessageManager().sendMessage(player, "rig-placed");
    }
}