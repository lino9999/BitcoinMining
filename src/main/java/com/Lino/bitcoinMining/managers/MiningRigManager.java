package com.Lino.bitcoinMining.managers;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.models.MiningRig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.*;

public class MiningRigManager {

    private final BitcoinMining plugin;
    private final Map<Location, MiningRig> rigsByLocation;
    private final Map<UUID, List<MiningRig>> rigsByPlayer;

    public MiningRigManager(BitcoinMining plugin) {
        this.plugin = plugin;
        this.rigsByLocation = new HashMap<>();
        this.rigsByPlayer = new HashMap<>();
        loadRigs();
    }

    private void loadRigs() {
        List<MiningRig> rigs = plugin.getDatabaseManager().loadAllMiningRigs();
        for (MiningRig rig : rigs) {
            rigsByLocation.put(rig.getLocation(), rig);
            rigsByPlayer.computeIfAbsent(rig.getOwnerId(), k -> new ArrayList<>()).add(rig);
        }
    }

    public MiningRig createRig(Player player, Block block, int level) {
        Location location = block.getLocation();
        UUID rigId = UUID.randomUUID();

        MiningRig rig = new MiningRig(rigId, player.getUniqueId(), location, level);

        rigsByLocation.put(location, rig);
        rigsByPlayer.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(rig);

        plugin.getDatabaseManager().saveMiningRig(rig);

        return rig;
    }

    public void removeRig(MiningRig rig) {
        rigsByLocation.remove(rig.getLocation());
        List<MiningRig> playerRigs = rigsByPlayer.get(rig.getOwnerId());
        if (playerRigs != null) {
            playerRigs.remove(rig);
        }

        plugin.getDatabaseManager().deleteMiningRig(rig.getId());
    }

    public MiningRig getRigAt(Location location) {
        return rigsByLocation.get(location);
    }

    public List<MiningRig> getPlayerRigs(UUID playerUuid) {
        return rigsByPlayer.getOrDefault(playerUuid, new ArrayList<>());
    }

    public Collection<MiningRig> getAllRigs() {
        return rigsByLocation.values();
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