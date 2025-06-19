package com.Lino.bitcoinMining.listeners;

import com.yourserver.bitcoinmining.BitcoinMining;
import com.yourserver.bitcoinmining.gui.FuelGUI;
import com.yourserver.bitcoinmining.gui.MiningRigGUI;
import com.yourserver.bitcoinmining.gui.PriceChartGUI;
import com.yourserver.bitcoinmining.models.MiningRig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public class InventoryClickListener implements Listener {

    private final BitcoinMining plugin;

    public InventoryClickListener(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.startsWith("§6⛏ Mining Rig")) {
            handleMiningRigGUI(event, player, title);
        } else if (title.equals("§c⛽ Fuel Management")) {
            handleFuelGUI(event, player);
        } else if (title.equals("§e📈 Bitcoin Price Chart")) {
            handlePriceChartGUI(event, player);
        }
    }

    private void handleMiningRigGUI(InventoryClickEvent event, Player player, String title) {
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        MiningRig rig = findRigFromTitle(title);
        if (rig == null) return;

        int slot = event.getSlot();

        switch (slot) {
            case 20:
                handleFuelClick(player, rig);
                break;
            case 22:
                handleStatusClick(player, rig);
                break;
            case 29:
                handleOverclockClick(player, rig, event.getClick());
                break;
            case 31:
                handleUpgradeClick(player, rig);
                break;
            case 33:
                handlePriceChartClick(player);
                break;
            case 49:
                player.closeInventory();
                plugin.getMessageManager().sendMessage(player, "gui-close");
                break;
        }
    }

    private void handleFuelGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int slot = event.getSlot();

        MiningRig rig = findRigForPlayer(player);
        if (rig == null) return;

        switch (slot) {
            case 20:
                addFuelToRig(player, rig, 1);
                break;
            case 22:
                addFuelToRig(player, rig, 9);
                break;
            case 24:
                addAllFuelToRig(player, rig);
                break;
            case 31:
                new MiningRigGUI(plugin, rig).open(player);
                break;
        }
    }

    private void handlePriceChartGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int slot = event.getSlot();

        switch (slot) {
            case 49:
                MiningRig rig = findRigForPlayer(player);
                if (rig != null) {
                    new MiningRigGUI(plugin, rig).open(player);
                } else {
                    player.closeInventory();
                }
                break;
            case 53:
                plugin.getPriceManager().updatePrice();
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    new PriceChartGUI(plugin).open(player);
                }, 20L);
                break;
        }
    }

    private MiningRig findRigFromTitle(String title) {
        for (MiningRig rig : plugin.getMiningRigManager().getAllRigs()) {
            if (title.contains(rig.getType().getName())) {
                return rig;
            }
        }
        return null;
    }

    private void handleFuelClick(Player player, MiningRig rig) {
        new FuelGUI(plugin, rig).open(player);
    }

    private void handleStatusClick(Player player, MiningRig rig) {
        rig.setActive(!rig.isActive());
        plugin.getMiningRigManager().saveRig(rig);

        String status = rig.isActive() ? "enabled" : "disabled";
        plugin.getMessageManager().sendMessage(player, "gui-toggle-rig",
                "%status%", status);

        new MiningRigGUI(plugin, rig).open(player);
    }

    private void handleOverclockClick(Player player, MiningRig rig, ClickType clickType) {
        double currentOverclock = rig.getOverclock();
        double newOverclock = currentOverclock;

        if (clickType == ClickType.LEFT) {
            if (currentOverclock < 3.0) {
                newOverclock = Math.min(3.0, currentOverclock + 0.1);
                rig.setOverclock(newOverclock);
                plugin.getMessageManager().sendMessage(player, "gui-overclock-increased",
                        "%percent%", String.format("%.0f", newOverclock * 100));
            } else {
                plugin.getMessageManager().sendMessage(player, "gui-overclock-max");
            }
        } else if (clickType == ClickType.RIGHT) {
            if (currentOverclock > 1.0) {
                newOverclock = Math.max(1.0, currentOverclock - 0.1);
                rig.setOverclock(newOverclock);
                plugin.getMessageManager().sendMessage(player, "gui-overclock-decreased",
                        "%percent%", String.format("%.0f", newOverclock * 100));
            } else {
                plugin.getMessageManager().sendMessage(player, "gui-overclock-min");
            }
        }

        if (newOverclock != currentOverclock) {
            plugin.getMiningRigManager().saveRig(rig);
            new MiningRigGUI(plugin, rig).open(player);
        }
    }

    private void handleUpgradeClick(Player player, MiningRig rig) {
        if (!rig.canUpgrade()) {
            plugin.getMessageManager().sendMessage(player, "rig-upgrade-max-level");
            return;
        }

        MiningRig.RigType nextTier = rig.getNextTier();
        double cost = nextTier.getUpgradeCost();

        if (plugin.getBitcoinManager().removeBitcoin(player.getUniqueId(), cost)) {
            rig.upgrade();
            plugin.getMiningRigManager().saveRig(rig);

            rig.getLocation().getBlock().setType(
                    plugin.getMiningRigManager().getBlockFromRigType(rig.getType())
            );

            plugin.getMessageManager().sendMessage(player, "rig-upgraded",
                    "%tier%", rig.getType().getName());

            new MiningRigGUI(plugin, rig).open(player);
        } else {
            plugin.getMessageManager().sendMessage(player, "rig-upgrade-not-enough-btc",
                    "%cost%", String.format("%.8f", cost));
        }
    }

    private void handlePriceChartClick(Player player) {
        new PriceChartGUI(plugin).open(player);
    }

    private void addFuelToRig(Player player, MiningRig rig, int amount) {
        ItemStack coalItem = new ItemStack(Material.COAL, amount);

        if (!player.getInventory().containsAtLeast(coalItem, amount)) {
            plugin.getMessageManager().sendMessage(player, "not-enough-bitcoin");
            return;
        }

        int maxFuel = rig.getType().getFuelCapacity() - rig.getFuel();
        int actualAdded = Math.min(amount, maxFuel);

        if (actualAdded > 0) {
            player.getInventory().removeItem(new ItemStack(Material.COAL, actualAdded));
            rig.addFuel(actualAdded);
            plugin.getMiningRigManager().saveRig(rig);

            plugin.getMessageManager().sendMessage(player, "fuel-added",
                    "%amount%", String.valueOf(actualAdded),
                    "%total%", String.valueOf(rig.getFuel()));

            new FuelGUI(plugin, rig).open(player);
        } else {
            plugin.getMessageManager().sendMessage(player, "fuel-tank-full");
        }
    }

    private void addAllFuelToRig(Player player, MiningRig rig) {
        int totalCoal = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                if (item.getType() == Material.COAL) {
                    totalCoal += item.getAmount();
                } else if (item.getType() == Material.COAL_BLOCK) {
                    totalCoal += item.getAmount() * 9;
                }
            }
        }

        if (totalCoal == 0) {
            plugin.getMessageManager().sendMessage(player, "not-enough-bitcoin");
            return;
        }

        int maxFuel = rig.getType().getFuelCapacity() - rig.getFuel();
        int actualAdded = Math.min(totalCoal, maxFuel);

        if (actualAdded > 0) {
            int remainingToAdd = actualAdded;

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && remainingToAdd > 0) {
                    if (item.getType() == Material.COAL_BLOCK) {
                        int blocksToRemove = Math.min(item.getAmount(), remainingToAdd / 9);
                        if (blocksToRemove > 0) {
                            item.setAmount(item.getAmount() - blocksToRemove);
                            remainingToAdd -= blocksToRemove * 9;
                        }
                    }
                }
            }

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && remainingToAdd > 0) {
                    if (item.getType() == Material.COAL) {
                        int coalToRemove = Math.min(item.getAmount(), remainingToAdd);
                        item.setAmount(item.getAmount() - coalToRemove);
                        remainingToAdd -= coalToRemove;
                    }
                }
            }

            rig.addFuel(actualAdded);
            plugin.getMiningRigManager().saveRig(rig);

            plugin.getMessageManager().sendMessage(player, "fuel-added",
                    "%amount%", String.valueOf(actualAdded),
                    "%total%", String.valueOf(rig.getFuel()));

            new FuelGUI(plugin, rig).open(player);
        } else {
            plugin.getMessageManager().sendMessage(player, "fuel-tank-full");
        }
    }

    private MiningRig findRigForPlayer(Player player) {
        List<MiningRig> playerRigs = plugin.getMiningRigManager().getPlayerRigs(player.getUniqueId());
        return playerRigs.isEmpty() ? null : playerRigs.get(0);
    }
}