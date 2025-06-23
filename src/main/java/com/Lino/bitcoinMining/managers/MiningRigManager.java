package com.Lino.bitcoinMining.managers;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.models.MiningRig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiningRigManager {

    private final BitcoinMining plugin;
    private final Map<Location, MiningRig> rigsByLocation;
    private final Map<UUID, List<MiningRig>> rigsByPlayer;
    private final Set<UUID> hologramsCreated; // Track which rigs have holograms

    public MiningRigManager(BitcoinMining plugin) {
        this.plugin = plugin;
        this.rigsByLocation = new ConcurrentHashMap<>();
        this.rigsByPlayer = new ConcurrentHashMap<>();
        this.hologramsCreated = Collections.synchronizedSet(new HashSet<>());
        loadRigs();
    }

    private void loadRigs() {
        List<MiningRig> rigs = plugin.getDatabaseManager().loadAllMiningRigs();

        // Delay hologram creation to ensure HologramManager is initialized
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (MiningRig rig : rigs) {
                if (rig.getLocation().getBlock().getType() == Material.OBSERVER) {
                    rigsByLocation.put(rig.getLocation(), rig);
                    rigsByPlayer.computeIfAbsent(rig.getOwnerId(), k -> new ArrayList<>()).add(rig);

                    // Create hologram only once per rig
                    if (plugin.getHologramManager() != null && !hologramsCreated.contains(rig.getId())) {
                        plugin.getHologramManager().createHologram(rig);
                        hologramsCreated.add(rig.getId());
                    }
                } else {
                    plugin.getDatabaseManager().deleteMiningRig(rig.getId());
                }
            }
        }, 40L); // Wait 2 seconds to ensure everything is loaded
    }

    public MiningRig createRig(Player player, Block block, int level) {
        Location location = block.getLocation();
        UUID rigId = UUID.randomUUID();

        MiningRig rig = new MiningRig(rigId, player.getUniqueId(), location, level);

        rigsByLocation.put(location, rig);
        rigsByPlayer.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(rig);

        plugin.getDatabaseManager().saveMiningRig(rig);

        // Create hologram for new rig
        if (plugin.getHologramManager() != null && !hologramsCreated.contains(rig.getId())) {
            plugin.getHologramManager().createHologram(rig);
            hologramsCreated.add(rig.getId());
        }

        return rig;
    }

    public void removeRig(MiningRig rig) {
        rigsByLocation.remove(rig.getLocation());
        List<MiningRig> playerRigs = rigsByPlayer.get(rig.getOwnerId());
        if (playerRigs != null) {
            playerRigs.remove(rig);
        }

        // Remove from tracking set
        hologramsCreated.remove(rig.getId());

        // Remove hologram
        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().removeHologram(rig.getId());
        }

        plugin.getDatabaseManager().deleteMiningRig(rig.getId());
    }

    public MiningRig getRigAt(Location location) {
        return rigsByLocation.get(location);
    }

    public List<MiningRig> getPlayerRigs(UUID playerUuid) {
        return new ArrayList<>(rigsByPlayer.getOrDefault(playerUuid, new ArrayList<>()));
    }

    public Collection<MiningRig> getAllRigs() {
        return new ArrayList<>(rigsByLocation.values());
    }

    public boolean isValidRigBlock(Block block) {
        return block.getType() == Material.OBSERVER;
    }

    public void saveRig(MiningRig rig) {
        plugin.getDatabaseManager().saveMiningRig(rig);
    }

    public void saveAllRigs() {
        for (MiningRig rig : getAllRigs()) {
            saveRig(rig);
        }
    }

    public int getMaxRigsPerPlayer() {
        return plugin.getConfig().getInt("max-rigs-per-player", 5);
    }

    public boolean canPlayerPlaceRig(Player player) {
        List<MiningRig> playerRigs = getPlayerRigs(player.getUniqueId());
        return playerRigs.size() < getMaxRigsPerPlayer();
    }
}