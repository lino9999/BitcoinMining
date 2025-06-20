package com.Lino.bitcoinMining.commands;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class GetRigCommand implements CommandExecutor, TabCompleter {

    private final BitcoinMining plugin;

    public GetRigCommand(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(sender, "usage-getrig");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("give")) {
            if (!sender.hasPermission("bitcoinmining.admin")) {
                plugin.getMessageManager().sendMessage(sender, "no-permission");
                return true;
            }

            if (args.length < 3) {
                plugin.getMessageManager().sendMessage(sender, "usage-getrig-give");
                return true;
            }

            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                plugin.getMessageManager().sendMessage(sender, "player-not-found", "%player%", args[1]);
                return true;
            }

            int level;
            try {
                level = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                plugin.getMessageManager().sendMessage(sender, "invalid-level");
                return true;
            }

            if (level < 1 || level > 20) {
                plugin.getMessageManager().sendMessage(sender, "invalid-level-range");
                return true;
            }

            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    amount = 1;
                }
            }

            ItemStack rigItem = createRigItem(level, amount);
            target.getInventory().addItem(rigItem);

            plugin.getMessageManager().sendMessage(sender, "rig-given",
                    "%player%", target.getName(),
                    "%level%", String.valueOf(level),
                    "%amount%", String.valueOf(amount));

            plugin.getMessageManager().sendMessage(target, "rig-received",
                    "%level%", String.valueOf(level),
                    "%amount%", String.valueOf(amount));

        } else if (subCommand.equals("get")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players!");
                return true;
            }

            if (!sender.hasPermission("bitcoinmining.getrig")) {
                plugin.getMessageManager().sendMessage(sender, "no-permission");
                return true;
            }

            if (args.length < 2) {
                plugin.getMessageManager().sendMessage(sender, "usage-getrig-get");
                return true;
            }

            Player player = (Player) sender;
            int level;
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                plugin.getMessageManager().sendMessage(sender, "invalid-level");
                return true;
            }

            if (level < 1 || level > 20) {
                plugin.getMessageManager().sendMessage(sender, "invalid-level-range");
                return true;
            }

            double cost = plugin.getConfig().getDouble("rig-levels.level-" + level + ".cost", 100.0 * level);

            if (!plugin.getEconomy().has(player, cost)) {
                plugin.getMessageManager().sendMessage(player, "not-enough-money-rig",
                        "%cost%", String.format("$%.2f", cost));
                return true;
            }

            plugin.getEconomy().withdrawPlayer(player, cost);
            ItemStack rigItem = createRigItem(level, 1);
            player.getInventory().addItem(rigItem);

            plugin.getMessageManager().sendMessage(player, "rig-purchased",
                    "%level%", String.valueOf(level),
                    "%cost%", String.format("$%.2f", cost));
        }

        return true;
    }

    private ItemStack createRigItem(int level, int amount) {
        String displayName = plugin.getConfig().getString("rig-levels.level-" + level + ".display-name",
                "§6Mining Rig §7[§eLevel " + level + "§7]");

        double hashRate = plugin.getConfig().getDouble("rig-levels.level-" + level + ".hash-rate", 0.001 * level);
        int fuelCapacity = plugin.getConfig().getInt("rig-levels.level-" + level + ".fuel-capacity", 64 + (level * 20));
        double fuelConsumption = plugin.getConfig().getDouble("rig-levels.level-" + level + ".fuel-consumption", 1.0 - (level * 0.02));

        NamespacedKey key = new NamespacedKey(plugin, "mining_rig_level");

        ItemStack item = new ItemBuilder(Material.OBSERVER)
                .setAmount(amount)
                .setName(displayName)
                .setLore(Arrays.asList(
                        "§7",
                        "§7Level: §e" + level,
                        "§7Hash Rate: §e" + String.format("%.6f", hashRate) + " BTC/hour",
                        "§7Fuel Capacity: §e" + fuelCapacity,
                        "§7Fuel Efficiency: §e" + String.format("%.2f", (2.0 - fuelConsumption) * 100) + "%",
                        "§7",
                        "§e§lRIGHT CLICK§7 to place"
                ))
                .setGlowing(true)
                .setPersistentData(key, PersistentDataType.INTEGER, level)
                .build();

        return item;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("bitcoinmining.getrig")) {
                completions.add("get");
            }
            if (sender.hasPermission("bitcoinmining.admin")) {
                completions.add("give");
            }
            return filterCompletions(completions, args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("bitcoinmining.admin")) {
                List<String> players = new ArrayList<>();
                plugin.getServer().getOnlinePlayers().forEach(p -> players.add(p.getName()));
                return filterCompletions(players, args[1]);
            } else if (args[0].equalsIgnoreCase("get")) {
                List<String> levels = new ArrayList<>();
                for (int i = 1; i <= 20; i++) {
                    levels.add(String.valueOf(i));
                }
                return filterCompletions(levels, args[1]);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("bitcoinmining.admin")) {
            List<String> levels = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                levels.add(String.valueOf(i));
            }
            return filterCompletions(levels, args[2]);
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