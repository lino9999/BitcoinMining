package com.Lino.bitcoinMining.tasks;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.models.MiningRig;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

public class MiningTask extends BukkitRunnable {

    private final BitcoinMining plugin;
    private long lastSaveTime = System.currentTimeMillis();

    public MiningTask(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (MiningRig rig : plugin.getMiningRigManager().getAllRigs()) {
            if (rig.isActive() && rig.getFuel() > 0) {
                processRig(rig);
            } else if (rig.isActive() && rig.getFuel() <= 0) {
                rig.setActive(false);
                plugin.getMiningRigManager().saveRig(rig);
            }
        }

        // Save all rigs every minute
        if (System.currentTimeMillis() - lastSaveTime > 60000) {
            plugin.getMiningRigManager().saveAllRigs();
            lastSaveTime = System.currentTimeMillis();
        }
    }

    private void processRig(MiningRig rig) {
        // Fixed: Fuel consumption per second (task runs every second)
        // Fuel consumption is per hour, so divide by 3600 for per second rate
        double fuelConsumptionPerSecond = rig.getEffectiveFuelConsumption() / 3600.0;

        if (rig.consumeFuel(fuelConsumptionPerSecond)) {
            double miningDifficulty = plugin.getConfig().getDouble("mining-difficulty", 1.0);
            // Bitcoin mined per second (hash rate is per hour)
            double bitcoinMinedPerSecond = (rig.getEffectiveHashRate() / 3600.0) / miningDifficulty;

            rig.addMinedBitcoin(bitcoinMinedPerSecond);
            plugin.getBitcoinManager().addBitcoin(rig.getOwnerId(), bitcoinMinedPerSecond);
            plugin.getDatabaseManager().updateTotalMined(rig.getOwnerId(), bitcoinMinedPerSecond);

            // Visual effects (reduced frequency)
            if (Math.random() < 0.05) { // 5% chance per second
                rig.getLocation().getWorld().spawnParticle(
                        Particle.FLAME,
                        rig.getLocation().clone().add(0.5, 1.5, 0.5),
                        5, 0.3, 0.3, 0.3, 0.02
                );
            }

            // Sound effects (reduced frequency)
            if (Math.random() < 0.02) { // 2% chance per second
                rig.getLocation().getWorld().playSound(
                        rig.getLocation(),
                        Sound.BLOCK_STONE_BREAK,
                        0.3f, 1.2f
                );
            }
        }
    }
}