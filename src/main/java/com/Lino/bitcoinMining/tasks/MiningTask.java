package com.Lino.bitcoinMining.tasks;

import com.yourserver.bitcoinmining.BitcoinMining;
import com.yourserver.bitcoinmining.models.MiningRig;
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

        if (System.currentTimeMillis() - lastSaveTime > 60000) {
            plugin.getMiningRigManager().saveAllRigs();
            lastSaveTime = System.currentTimeMillis();
        }
    }

    private void processRig(MiningRig rig) {
        double fuelConsumption = rig.getEffectiveFuelConsumption() / 3600;

        if (rig.consumeFuel(fuelConsumption)) {
            double miningDifficulty = plugin.getConfig().getDouble("mining-difficulty", 1.0);
            double bitcoinMined = (rig.getEffectiveHashRate() / 3600) / miningDifficulty;

            rig.addMinedBitcoin(bitcoinMined);
            plugin.getBitcoinManager().addBitcoin(rig.getOwnerId(), bitcoinMined);
            plugin.getDatabaseManager().updateTotalMined(rig.getOwnerId(), bitcoinMined);

            if (Math.random() < 0.1) {
                rig.getLocation().getWorld().spawnParticle(
                        Particle.VILLAGER_HAPPY,
                        rig.getLocation().clone().add(0.5, 1.5, 0.5),
                        5, 0.3, 0.3, 0.3, 0.02
                );
            }

            if (Math.random() < 0.05) {
                rig.getLocation().getWorld().playSound(
                        rig.getLocation(),
                        Sound.BLOCK_STONE_BREAK,
                        0.3f, 1.2f
                );
            }
        }
    }
}