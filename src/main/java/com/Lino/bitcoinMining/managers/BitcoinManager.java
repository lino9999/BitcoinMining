package com.Lino.bitcoinMining.managers;

import com.yourserver.bitcoinmining.BitcoinMining;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BitcoinManager {

    private final BitcoinMining plugin;
    private final Map<UUID, Double> balances;
    private final Map<UUID, Double> dailyProfits;

    public BitcoinManager(BitcoinMining plugin) {
        this.plugin = plugin;
        this.balances = new HashMap<>();
        this.dailyProfits = new HashMap<>();
        loadBalances();
    }

    private void loadBalances() {
        plugin.getDatabaseManager().loadAllBalances(balances);
    }

    public double getBalance(UUID playerUuid) {
        return balances.getOrDefault(playerUuid, 0.0);
    }

    public void setBalance(UUID playerUuid, double amount) {
        balances.put(playerUuid, Math.max(0, amount));
        plugin.getDatabaseManager().saveBalance(playerUuid, amount);
    }

    public void addBitcoin(UUID playerUuid, double amount) {
        setBalance(playerUuid, getBalance(playerUuid) + amount);
        addDailyProfit(playerUuid, amount);
    }

    public boolean removeBitcoin(UUID playerUuid, double amount) {
        double balance = getBalance(playerUuid);
        if (balance >= amount) {
            setBalance(playerUuid, balance - amount);
            return true;
        }
        return false;
    }

    public boolean transfer(UUID from, UUID to, double amount) {
        double fee = amount * plugin.getConfig().getDouble("transfer-fee", 0.01);
        double totalAmount = amount + fee;

        if (getBalance(from) >= totalAmount) {
            removeBitcoin(from, totalAmount);
            addBitcoin(to, amount);
            return true;
        }
        return false;
    }

    public double convertToServerMoney(UUID playerUuid, double bitcoinAmount) {
        double rate = plugin.getPriceManager().getCurrentPrice();
        double serverAmount = bitcoinAmount * rate;

        if (removeBitcoin(playerUuid, bitcoinAmount)) {
            plugin.getEconomy().depositPlayer(plugin.getServer().getOfflinePlayer(playerUuid), serverAmount);
            return serverAmount;
        }
        return 0;
    }

    public double convertFromServerMoney(Player player, double serverAmount) {
        double rate = plugin.getPriceManager().getCurrentPrice();
        double bitcoinAmount = serverAmount / rate;

        if (plugin.getEconomy().withdrawPlayer(player, serverAmount).transactionSuccess()) {
            addBitcoin(player.getUniqueId(), bitcoinAmount);
            return bitcoinAmount;
        }
        return 0;
    }

    public void addDailyProfit(UUID playerUuid, double amount) {
        dailyProfits.put(playerUuid, dailyProfits.getOrDefault(playerUuid, 0.0) + amount);
    }

    public double getDailyProfit(UUID playerUuid) {
        return dailyProfits.getOrDefault(playerUuid, 0.0);
    }

    public void resetDailyProfits() {
        dailyProfits.clear();
    }

    public Map<UUID, Double> getTopMiners(int limit) {
        return plugin.getDatabaseManager().getTopMiners(limit);
    }
}