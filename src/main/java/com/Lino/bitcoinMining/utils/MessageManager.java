package com.Lino.bitcoinMining.utils;

import com.yourserver.bitcoinmining.BitcoinMining;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

public class MessageManager {

    private final BitcoinMining plugin;
    private FileConfiguration messagesConfig;
    private String prefix;

    public MessageManager(BitcoinMining plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = colorize(messagesConfig.getString("prefix", "&6[BitcoinMining] &7"));
    }

    public void reload() {
        loadMessages();
    }

    public String getMessage(String key, String... replacements) {
        String message = messagesConfig.getString(key, "Message not found: " + key);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        return colorize(message);
    }

    public void sendMessage(CommandSender sender, String key, String... replacements) {
        String message = getMessage(key, replacements);
        sender.sendMessage(prefix + message);
    }

    public void sendMessageNoPrefix(CommandSender sender, String key, String... replacements) {
        String message = getMessage(key, replacements);
        sender.sendMessage(message);
    }

    public void sendRawMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getPrefix() {
        return prefix;
    }
}