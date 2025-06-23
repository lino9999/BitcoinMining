package com.Lino.bitcoinMining.managers;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.models.MiningRig;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HologramManager {

    private final BitcoinMining plugin;
    private final Map<UUID, Hologram> holograms;
    private final DecimalFormat df = new DecimalFormat("#,##0.00000000");
    private BukkitTask updateTask;

    public HologramManager(BitcoinMining plugin) {
        this.plugin = plugin;
        this.holograms = new HashMap<>();

        // Only start update task if holograms are enabled
        if (plugin.getConfig().getBoolean("enable-holograms", true)) {
            startUpdateTask();
        }
    }

    public void createHologram(MiningRig rig) {
        // Check if holograms are enabled
        if (!plugin.getConfig().getBoolean("enable-holograms", true)) {
            return;
        }

        // Remove any existing hologram first to prevent duplicates
        removeHologram(rig.getId());

        Location baseLoc = rig.getLocation().clone().add(0.5, 2.5, 0.5);

        // Check if there are already armor stands at this location and remove them
        cleanupNearbyArmorStands(baseLoc);

        ArmorStand line1 = createArmorStand(baseLoc.clone());
        ArmorStand line2 = createArmorStand(baseLoc.clone().subtract(0, 0.25, 0));
        ArmorStand line3 = createArmorStand(baseLoc.clone().subtract(0, 0.5, 0));

        updateHologramText(rig, line1, line2, line3);

        Hologram hologram = new Hologram(line1, line2, line3);
        holograms.put(rig.getId(), hologram);
    }

    private void cleanupNearbyArmorStands(Location location) {
        // Remove any armor stands within 3 blocks radius that might be leftover
        location.getWorld().getNearbyEntities(location, 3, 3, 3).forEach(entity -> {
            if (entity instanceof ArmorStand) {
                ArmorStand stand = (ArmorStand) entity;
                if (stand.isCustomNameVisible() && !stand.isVisible() && stand.isMarker()) {
                    // This is likely a hologram armor stand
                    stand.remove();
                }
            }
        });
    }

    private ArmorStand createArmorStand(Location location) {
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomNameVisible(true);
        armorStand.setMarker(true);
        armorStand.setSmall(true);
        armorStand.setBasePlate(false);
        armorStand.setInvulnerable(true);
        armorStand.setCollidable(false);
        armorStand.setSilent(true);
        return armorStand;
    }

    public void removeHologram(UUID rigId) {
        Hologram hologram = holograms.remove(rigId);
        if (hologram != null) {
            hologram.remove();
        }
    }

    public void removeAllHolograms() {
        // Cancel the update task first
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Remove all holograms
        holograms.values().forEach(Hologram::remove);
        holograms.clear();
    }

    private void updateHologramText(MiningRig rig, ArmorStand line1, ArmorStand line2, ArmorStand line3) {
        if (line1 == null || !line1.isValid() ||
                line2 == null || !line2.isValid() ||
                line3 == null || !line3.isValid()) {
            return;
        }

        String rigName = plugin.getConfig().getString("rig-levels.level-" + rig.getLevel() + ".display-name",
                "§6Mining Rig §7[§eLevel " + rig.getLevel() + "§7]");

        line1.setCustomName(rigName);
        line2.setCustomName("§7Total Mined: §e" + df.format(rig.getTotalBitcoinMined()) + " BTC");

        int fuelPercent = (int) ((rig.getFuel() / rig.getFuelCapacity()) * 100);
        String fuelColor;
        if (fuelPercent > 50) {
            fuelColor = "§a";
        } else if (fuelPercent > 20) {
            fuelColor = "§e";
        } else {
            fuelColor = "§c";
        }

        line3.setCustomName("§7Fuel: " + fuelColor + (int)rig.getFuel() + "/" + rig.getFuelCapacity() + " §7(" + fuelPercent + "%)");
    }

    private void startUpdateTask() {
        long updateInterval = plugin.getConfig().getLong("hologram-update-interval", 20L);

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if holograms are still enabled
                if (!plugin.getConfig().getBoolean("enable-holograms", true)) {
                    removeAllHolograms();
                    this.cancel();
                    return;
                }

                for (MiningRig rig : plugin.getMiningRigManager().getAllRigs()) {
                    Hologram hologram = holograms.get(rig.getId());
                    if (hologram != null && hologram.isValid()) {
                        // Only update text, don't create new holograms
                        updateHologramText(rig, hologram.line1, hologram.line2, hologram.line3);
                    }
                    // Don't create new holograms here - they should only be created when rigs are placed/loaded
                }
            }
        }.runTaskTimer(plugin, updateInterval, updateInterval);
    }

    public void reload() {
        // Remove all holograms first
        removeAllHolograms();

        // If holograms are enabled, recreate them
        if (plugin.getConfig().getBoolean("enable-holograms", true)) {
            startUpdateTask();

            // Recreate holograms for all existing rigs
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (MiningRig rig : plugin.getMiningRigManager().getAllRigs()) {
                    createHologram(rig);
                }
            }, 20L);
        }
    }

    private static class Hologram {
        private final ArmorStand line1;
        private final ArmorStand line2;
        private final ArmorStand line3;

        public Hologram(ArmorStand line1, ArmorStand line2, ArmorStand line3) {
            this.line1 = line1;
            this.line2 = line2;
            this.line3 = line3;
        }

        public void remove() {
            if (line1 != null && line1.isValid()) line1.remove();
            if (line2 != null && line2.isValid()) line2.remove();
            if (line3 != null && line3.isValid()) line3.remove();
        }

        public boolean isValid() {
            return line1 != null && line1.isValid() &&
                    line2 != null && line2.isValid() &&
                    line3 != null && line3.isValid();
        }
    }
}