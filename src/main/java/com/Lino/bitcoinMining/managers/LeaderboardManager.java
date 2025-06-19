package com.Lino.bitcoinMining.managers;

import com.Lino.bitcoinMining.BitcoinMining;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class LeaderboardManager {

    private final BitcoinMining plugin;
    private Map<UUID, Double> cachedLeaderboard;
    private long lastUpdate;
    private final DecimalFormat df = new DecimalFormat("#,##0.00000000");

    public LeaderboardManager(BitcoinMining plugin) {
        this.plugin = plugin;
        this.cachedLeaderboard = new LinkedHashMap<>();
        this.lastUpdate = 0;
        startUpdateTask();
    }

    private void startUpdateTask() {
        int updateInterval = plugin.getConfig().getInt("leaderboard-update-interval", 300) * 20;

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            updateLeaderboard();
        }, 0L, updateInterval);
    }

    private void updateLeaderboard() {
        int size = plugin.getConfig().getInt("leaderboard-size", 10);
        Map<UUID, Double> topMiners = plugin.getDatabaseManager().getTopMiners(size);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            cachedLeaderboard = topMiners;
            lastUpdate = System.currentTimeMillis();
        });
    }

    public Map<UUID, Double> getLeaderboard() {
        if (System.currentTimeMillis() - lastUpdate > 60000) {
            updateLeaderboard();
        }
        return new LinkedHashMap<>(cachedLeaderboard);
    }

    public String formatLeaderboard() {
        StringBuilder leaderboard = new StringBuilder();
        leaderboard.append(plugin.getMessageManager().getMessage("leaderboard-header")).append("\n");

        int position = 1;
        for (Map.Entry<UUID, Double> entry : getLeaderboard().entrySet()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String playerName = player.getName() != null ? player.getName() : "Unknown";

            leaderboard.append(plugin.getMessageManager().getMessage("leaderboard-entry",
                    "%position%", String.valueOf(position),
                    "%player%", playerName,
                    "%amount%", df.format(entry.getValue())
            )).append("\n");

            position++;
        }

        leaderboard.append(plugin.getMessageManager().getMessage("leaderboard-footer"));
        return leaderboard.toString();
    }

    public int getPlayerRank(UUID playerUuid) {
        int rank = 1;
        for (UUID uuid : getLeaderboard().keySet()) {
            if (uuid.equals(playerUuid)) {
                return rank;
            }
            rank++;
        }
        return -1;
    }
}