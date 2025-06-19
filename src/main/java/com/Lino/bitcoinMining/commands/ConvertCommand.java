package com.Lino.bitcoinMining.commands;

import com.Lino.bitcoinmining.BitcoinMining;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class ConvertCommand implements CommandExecutor, TabCompleter {

    private final BitcoinMining plugin;
    private final DecimalFormat btcFormat = new DecimalFormat("#,##0.00000000");
    private final DecimalFormat moneyFormat = new DecimalFormat("$#,##0.00");

    public ConvertCommand(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("bitcoinmining.convert")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return true;
        }

        if (!plugin.getConfig().getBoolean("conversion-enabled", true)) {
            plugin.getMessageManager().sendMessage(player, "error-database");
            return true;
        }

        if (args.length != 2) {
            plugin.getMessageManager().sendMessage(player, "usage-convert");
            return true;
        }

        String direction = args[0].toLowerCase();
        double amount;

        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "invalid-amount");
            return true;
        }

        if (amount <= 0 || amount < plugin.getConfig().getDouble("min-conversion-amount", 0.00000001)) {
            plugin.getMessageManager().sendMessage(player, "invalid-amount");
            return true;
        }

        switch (direction) {
            case "to":
                handleConvertToMoney(player, amount);
                break;
            case "from":
                handleConvertFromMoney(player, amount);
                break;
            default:
                plugin.getMessageManager().sendMessage(player, "usage-convert");
        }

        return true;
    }

    private void handleConvertToMoney(Player player, double bitcoinAmount) {
        if (plugin.getBitcoinManager().getBalance(player.getUniqueId()) < bitcoinAmount) {
            plugin.getMessageManager().sendMessage(player, "not-enough-bitcoin");
            return;
        }

        double serverAmount = plugin.getBitcoinManager().convertToServerMoney(player.getUniqueId(), bitcoinAmount);

        if (serverAmount > 0) {
            plugin.getMessageManager().sendMessage(player, "convert-to-money",
                    "%btc%", btcFormat.format(bitcoinAmount),
                    "%money%", moneyFormat.format(serverAmount));
        } else {
            plugin.getMessageManager().sendMessage(player, "error-database");
        }
    }

    private void handleConvertFromMoney(Player player, double serverAmount) {
        if (plugin.getEconomy().getBalance(player) < serverAmount) {
            plugin.getMessageManager().sendMessage(player, "not-enough-money");
            return;
        }

        double bitcoinAmount = plugin.getBitcoinManager().convertFromServerMoney(player, serverAmount);

        if (bitcoinAmount > 0) {
            plugin.getMessageManager().sendMessage(player, "convert-from-money",
                    "%money%", moneyFormat.format(serverAmount),
                    "%btc%", btcFormat.format(bitcoinAmount));
        } else {
            plugin.getMessageManager().sendMessage(player, "error-database");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterCompletions(Arrays.asList("to", "from"), args[0]);
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