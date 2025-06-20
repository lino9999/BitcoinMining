package com.Lino.bitcoinMining.commands;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.models.MiningRig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class MinerCommand implements CommandExecutor, TabCompleter {

    private final BitcoinMining plugin;
    private final DecimalFormat df = new DecimalFormat("#,##0.00000000");

    public MinerCommand(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("bitcoinmining.miner")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("list")) {
            showMinerInfo(player);
        } else {
            plugin.getMessageManager().sendMessage(player, "usage-miner");
        }

        return true;
    }

    private void showMinerInfo(Player player) {
        List<MiningRig> rigs = plugin.getMiningRigManager().getPlayerRigs(player.getUniqueId());

        if (rigs.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "miner-info-none");
            return;
        }

        plugin.getMessageManager().sendMessageNoPrefix(player, "miner-info-header");

        int number = 1;
        for (MiningRig rig : rigs) {
            String status = getStatusString(rig);
            String rigName = "Level " + rig.getLevel();

            String message = plugin.getMessageManager().getMessage("miner-info-entry",
                    "%number%", String.valueOf(number),
                    "%type%", rigName,
                    "%status%", status,
                    "%fuel%", String.valueOf((int)rig.getFuel()),
                    "%max_fuel%", String.valueOf(rig.getFuelCapacity()),
                    "%hashrate%", df.format(rig.getEffectiveHashRate()),
                    "%world%", rig.getLocation().getWorld().getName(),
                    "%x%", String.valueOf(rig.getLocation().getBlockX()),
                    "%y%", String.valueOf(rig.getLocation().getBlockY()),
                    "%z%", String.valueOf(rig.getLocation().getBlockZ())
            );

            plugin.getMessageManager().sendRawMessage(player, message);
            number++;
        }

        plugin.getMessageManager().sendMessageNoPrefix(player, "miner-info-footer",
                "%total%", String.valueOf(rigs.size()),
                "%max%", String.valueOf(plugin.getMiningRigManager().getMaxRigsPerPlayer())
        );
    }

    private String getStatusString(MiningRig rig) {
        if (!rig.isActive()) {
            return plugin.getMessageManager().getMessage("rig-status-inactive");
        } else if (rig.getFuel() <= 0) {
            return plugin.getMessageManager().getMessage("rig-status-no-fuel");
        } else {
            return plugin.getMessageManager().getMessage("rig-status-active");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterCompletions(Arrays.asList("info", "list"), args[0]);
        }
        return new ArrayList<>();
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(completion);
            }
        }
        return filtered;
    }
}