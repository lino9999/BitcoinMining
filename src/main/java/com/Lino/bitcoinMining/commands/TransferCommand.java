package com.Lino.bitcoinMining.commands;

import com.Lino.bitcoinMining.BitcoinMining;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TransferCommand implements CommandExecutor, TabCompleter {

    private final BitcoinMining plugin;
    private final DecimalFormat df = new DecimalFormat("#,##0.00000000");

    public TransferCommand(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("bitcoinmining.transfer")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return true;
        }

        if (args.length != 2) {
            plugin.getMessageManager().sendMessage(player, "usage-transfer");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(player, "player-not-found",
                    "%player%", args[0]);
            return true;
        }

        if (target.equals(player)) {
            plugin.getMessageManager().sendMessage(player, "invalid-amount");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "invalid-amount");
            return true;
        }

        if (amount <= 0 || amount < plugin.getConfig().getDouble("min-transfer-amount", 0.00000001)) {
            plugin.getMessageManager().sendMessage(player, "invalid-amount");
            return true;
        }

        double fee = amount * plugin.getConfig().getDouble("transfer-fee", 0.01);

        if (plugin.getBitcoinManager().transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
            plugin.getMessageManager().sendMessage(player, "bitcoin-sent",
                    "%amount%", df.format(amount),
                    "%player%", target.getName(),
                    "%fee%", df.format(fee));

            plugin.getMessageManager().sendMessage(target, "bitcoin-received",
                    "%amount%", df.format(amount),
                    "%player%", player.getName());

            plugin.getDatabaseManager().saveTransaction(
                    player.getUniqueId(),
                    target.getUniqueId(),
                    amount,
                    fee
            );
        } else {
            plugin.getMessageManager().sendMessage(player, "transfer-failed");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            plugin.getServer().getOnlinePlayers().forEach(p -> {
                if (!p.equals(sender)) {
                    players.add(p.getName());
                }
            });
            return filterCompletions(players, args[0]);
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