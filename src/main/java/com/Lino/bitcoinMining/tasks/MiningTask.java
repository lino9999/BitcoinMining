package com.Lino.bitcoinMining.tasks;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.models.MiningRig;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MiningTask extends BukkitRunnable {

    private final BitcoinMining plugin;
    private long lastSaveTime = System.currentTimeMillis();
    private final Set<UUID> processedRigs = new HashSet<>();

    public MiningTask(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        processedRigs.clear();

        for (MiningRig rig : plugin.getMiningRigManager().getAllRigs()) {
            if (!processedRigs.contains(rig.getId())) {
                processIndividualRig(rig);
                processedRigs.add(rig.getId());
            }
        }

        if (System.currentTimeMillis() - lastSaveTime > 60000) {
            plugin.getMiningRigManager().saveAllRigs();
            lastSaveTime = System.currentTimeMillis();
        }
    }

    private void processIndividualRig(MiningRig rig) {
        if (!rig.isActive()) {
            return;
        }

        if (rig.getFuel() <= 0) {
            rig.setActive(false);
            plugin.getMiningRigManager().saveRig(rig);
            return;
        }

        double fuelConsumptionPerSecond = rig.getEffectiveFuelConsumption() / 3600.0;

        if (!rig.consumeFuel(fuelConsumptionPerSecond)) {
            rig.setActive(false);
            plugin.getMiningRigManager().saveRig(rig);
            return;
        }

        double miningDifficulty = plugin.getConfig().getDouble("mining-difficulty", 1.0);
        double bitcoinMinedPerSecond = (rig.getEffectiveHashRate() / 3600.0) / miningDifficulty;

        rig.addMinedBitcoin(bitcoinMinedPerSecond);
        plugin.getBitcoinManager().addBitcoin(rig.getOwnerId(), bitcoinMinedPerSecond);
        plugin.getDatabaseManager().updateTotalMined(rig.getOwnerId(), bitcoinMinedPerSecond);

        if (Math.random() < 0.05) {
            rig.getLocation().getWorld().spawnParticle(
                    Particle.FLAME,
                    rig.getLocation().clone().add(0.5, 1.5, 0.5),
                    5, 0.3, 0.3, 0.3, 0.02
            );
        }

        if (Math.random() < 0.02) {
            rig.getLocation().getWorld().playSound(
                    rig.getLocation(),
                    Sound.BLOCK_STONE_BREAK,
                    0.3f, 1.2f
            );
        }
    }
}