package com.Lino.bitcoinMining.commands;

import com.Lino.bitcoinMining.BitcoinMining;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class BlackMarketCommand implements CommandExecutor, TabCompleter {

    private final BitcoinMining plugin;

    public BlackMarketCommand(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (plugin.getBlackMarketManager().isOpen()) {
                    plugin.getMessageManager().sendMessage(player, "black-market-status-open");
                } else {
                    plugin.getMessageManager().sendMessage(player, "black-market-status-closed",
                            "%time%", plugin.getBlackMarketManager().getTimeUntilOpen());
                }
            } else {
                plugin.getMessageManager().sendMessage(sender, "usage-blackmarket");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "additem":
                handleAddItem(sender, args);
                break;
            case "open":
                handleOpen(sender);
                break;
            case "close":
                handleClose(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                plugin.getMessageManager().sendMessage(sender, "usage-blackmarket");
        }

        return true;
    }

    private void handleAddItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return;
        }

        if (!sender.hasPermission("bitcoinmining.admin")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "usage-blackmarket-additem");
            return;
        }

        Player player = (Player) sender;
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType().isAir()) {
            plugin.getMessageManager().sendMessage(player, "black-market-no-item-in-hand");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "invalid-amount");
            return;
        }

        if (price <= 0) {
            plugin.getMessageManager().sendMessage(player, "invalid-amount");
            return;
        }

        String key = "custom_" + System.currentTimeMillis();
        plugin.getBlackMarketManager().addItem(key, itemInHand, price);

        plugin.getMessageManager().sendMessage(player, "black-market-item-added",
                "%item%", itemInHand.getType().name(),
                "%price%", String.format("%.8f", price));
    }

    private void handleOpen(CommandSender sender) {
        if (!sender.hasPermission("bitcoinmining.admin")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return;
        }

        plugin.getBlackMarketManager().open();
        plugin.getMessageManager().sendMessage(sender, "black-market-force-opened");
    }

    private void handleClose(CommandSender sender) {
        if (!sender.hasPermission("bitcoinmining.admin")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return;
        }

        plugin.getBlackMarketManager().close();
        plugin.getMessageManager().sendMessage(sender, "black-market-force-closed");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("bitcoinmining.admin")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return;
        }

        plugin.getBlackMarketManager().reload();
        plugin.getMessageManager().sendMessage(sender, "black-market-reloaded");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("bitcoinmining.admin")) {
                completions.addAll(Arrays.asList("additem", "open", "close", "reload"));
            }
            return filterCompletions(completions, args[0]);
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