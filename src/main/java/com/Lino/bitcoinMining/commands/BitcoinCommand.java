package com.Lino.bitcoinMining.commands;

import com.Lino.bitcoinmining.BitcoinMining;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BitcoinCommand implements CommandExecutor, TabCompleter {

    private final BitcoinMining plugin;
    private final DecimalFormat df = new DecimalFormat("#,##0.00000000");

    public BitcoinCommand(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                double balance = plugin.getBitcoinManager().getBalance(player.getUniqueId());
                plugin.getMessageManager().sendMessage(sender, "balance-check",
                        "%balance%", df.format(balance));
            } else {
                plugin.getMessageManager().sendMessage(sender, "usage-bitcoin");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "balance":
                handleBalance(sender, args);
                break;
            case "price":
                handlePrice(sender);
                break;
            case "help":
                handleHelp(sender);
                break;
            default:
                plugin.getMessageManager().sendMessage(sender, "usage-bitcoin");
        }

        return true;
    }

    private void handleBalance(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                plugin.getMessageManager().sendMessage(sender, "usage-bitcoin");
                return;
            }

            Player player = (Player) sender;
            double balance = plugin.getBitcoinManager().getBalance(player.getUniqueId());
            plugin.getMessageManager().sendMessage(sender, "balance-check",
                    "%balance%", df.format(balance));
        } else if (args.length == 2) {
            if (!sender.hasPermission("bitcoinmining.balance.others")) {
                plugin.getMessageManager().sendMessage(sender, "no-permission");
                return;
            }

            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                plugin.getMessageManager().sendMessage(sender, "player-not-found",
                        "%player%", args[1]);
                return;
            }

            double balance = plugin.getBitcoinManager().getBalance(target.getUniqueId());
            plugin.getMessageManager().sendMessage(sender, "balance-check-other",
                    "%player%", target.getName(),
                    "%balance%", df.format(balance));
        }
    }

    private void handlePrice(CommandSender sender) {
        double currentPrice = plugin.getPriceManager().getCurrentPrice();
        double priceChange = plugin.getPriceManager().getPriceChange();
        double priceChangePercent = plugin.getPriceManager().getPriceChangePercentage();

        String changeColor = priceChange >= 0 ? "§a" : "§c";
        String changeSign = priceChange >= 0 ? "+" : "";

        sender.sendMessage("§6§l===== Bitcoin Price =====");
        sender.sendMessage("§7Current Price: §a$" + String.format("%,.2f", currentPrice));
        sender.sendMessage("§7Change: " + changeColor + changeSign +
                String.format("$%,.2f (%.2f%%)", priceChange, priceChangePercent));
        sender.sendMessage("§6§l=====================");
    }

    private void handleHelp(CommandSender sender) {
        sender.sendMessage("§6§l===== BitcoinMining Help =====");
        sender.sendMessage("§e/bitcoin §7- Check your Bitcoin balance");
        sender.sendMessage("§e/bitcoin balance [player] §7- Check balance");
        sender.sendMessage("§e/bitcoin price §7- Check current Bitcoin price");
        sender.sendMessage("§e/btctransfer <player> <amount> §7- Transfer Bitcoin");
        sender.sendMessage("§e/btcconvert <to|from> <amount> §7- Convert currency");
        sender.sendMessage("§e/miner §7- Mining rig information");
        sender.sendMessage("§e/btctop §7- Top miners leaderboard");
        sender.sendMessage("§6§l===========================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterCompletions(Arrays.asList("balance", "price", "help"), args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("balance")) {
            if (sender.hasPermission("bitcoinmining.balance.others")) {
                List<String> players = new ArrayList<>();
                plugin.getServer().getOnlinePlayers().forEach(p -> players.add(p.getName()));
                return filterCompletions(players, args[1]);
            }
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