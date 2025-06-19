package com.Lino.bitcoinMining.managers;

import com.yourserver.bitcoinmining.BitcoinMining;
import com.yourserver.bitcoinmining.models.MiningRig;
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

    public MiningRig createRig(Player player, Block block, MiningRig.RigType type) {
        Location location = block.getLocation();
        UUID rigId = UUID.randomUUID();

        MiningRig rig = new MiningRig(rigId, player.getUniqueId(), location, type);

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
        Material type = block.getType();
        return type == Material.COPPER_BLOCK ||
                type == Material.IRON_BLOCK ||
                type == Material.GOLD_BLOCK ||
                type == Material.DIAMOND_BLOCK;
    }

    public MiningRig.RigType getRigTypeFromBlock(Block block) {
        switch (block.getType()) {
            case COPPER_BLOCK: return MiningRig.RigType.BRONZE;
            case IRON_BLOCK: return MiningRig.RigType.SILVER;
            case GOLD_BLOCK: return MiningRig.RigType.GOLD;
            case DIAMOND_BLOCK: return MiningRig.RigType.DIAMOND;
            default: return null;
        }
    }

    public Material getBlockFromRigType(MiningRig.RigType type) {
        switch (type) {
            case BRONZE: return Material.COPPER_BLOCK;
            case SILVER: return Material.IRON_BLOCK;
            case GOLD: return Material.GOLD_BLOCK;
            case DIAMOND: return Material.DIAMOND_BLOCK;
            default: return Material.STONE;
        }
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