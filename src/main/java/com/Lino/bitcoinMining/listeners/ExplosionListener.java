package com.Lino.bitcoinMining.listeners;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.models.MiningRig;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;

import java.util.Iterator;

public class ExplosionListener implements Listener {

    private final BitcoinMining plugin;

    public ExplosionListener(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            MiningRig rig = plugin.getMiningRigManager().getRigAt(block.getLocation());
            if (rig != null) {
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            MiningRig rig = plugin.getMiningRigManager().getRigAt(block.getLocation());
            if (rig != null) {
                iterator.remove();
            }
        }
    }
}