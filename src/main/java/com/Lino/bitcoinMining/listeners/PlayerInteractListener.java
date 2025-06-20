package com.Lino.bitcoinMining.listeners;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.gui.MiningRigGUI;
import com.Lino.bitcoinMining.models.MiningRig;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final BitcoinMining plugin;

    public PlayerInteractListener(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.OBSERVER) return;

        MiningRig rig = plugin.getMiningRigManager().getRigAt(block.getLocation());
        if (rig == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();

        if (!rig.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("bitcoinmining.admin")) {
            plugin.getMessageManager().sendMessage(player, "rig-not-owner");
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.COAL || itemInHand.getType() == Material.COAL_BLOCK) {
            int fuelToAdd = itemInHand.getType() == Material.COAL ? itemInHand.getAmount() : itemInHand.getAmount() * 9;
            double currentFuel = rig.getFuel();
            int maxFuel = rig.getFuelCapacity();
            int maxCanAdd = maxFuel - (int)currentFuel;
            int actualFuelAdded = Math.min(fuelToAdd, maxCanAdd);

            if (actualFuelAdded > 0) {
                rig.addFuel(actualFuelAdded);

                int itemsToRemove;
                if (itemInHand.getType() == Material.COAL) {
                    itemsToRemove = actualFuelAdded;
                } else {
                    itemsToRemove = (actualFuelAdded + 8) / 9;
                }

                itemInHand.setAmount(itemInHand.getAmount() - itemsToRemove);

                plugin.getMessageManager().sendMessage(player, "fuel-added",
                        "%amount%", String.valueOf(actualFuelAdded),
                        "%total%", String.valueOf((int)rig.getFuel()));

                plugin.getMiningRigManager().saveRig(rig);
            } else {
                plugin.getMessageManager().sendMessage(player, "fuel-tank-full");
            }
        } else {
            new MiningRigGUI(plugin, rig).open(player);
        }
    }
}