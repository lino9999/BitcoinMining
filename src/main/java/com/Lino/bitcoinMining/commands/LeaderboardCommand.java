package com.Lino.bitcoinMining.commands;

import com.Lino.bitcoinmining.BitcoinMining;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaderboardCommand implements CommandExecutor {

    private final BitcoinMining plugin;

    public LeaderboardCommand(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bitcoinmining.leaderboard")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        String leaderboard = plugin.getLeaderboardManager().formatLeaderboard();
        sender.sendMessage(leaderboard);

        if (sender instanceof Player) {
            Player player = (Player) sender;
            int rank = plugin.getLeaderboardManager().getPlayerRank(player.getUniqueId());

            if (rank > 0 && rank > plugin.getConfig().getInt("leaderboard-size", 10)) {
                sender.sendMessage("§7Your rank: §e#" + rank);
            }
        }

        return true;
    }
}