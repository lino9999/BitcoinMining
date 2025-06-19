package com.Lino.bitcoinMining.listeners;

import com.Lino.bitcoinmining.BitcoinMining;
import com.Lino.bitcoinmining.models.MiningRig;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

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
        block.getWorld().dropItemNaturally(
                block.getLocation(),
                new ItemStack(plugin.getMiningRigManager().getBlockFromRigType(rig.getType()))
        );

        if (rig.getFuel() > 0) {
            int coalBlocks = rig.getFuel() / 9;
            int coal = rig.getFuel() % 9;

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
}