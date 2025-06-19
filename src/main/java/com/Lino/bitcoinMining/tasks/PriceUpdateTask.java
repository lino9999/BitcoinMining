package com.Lino.bitcoinMining.tasks;

import com.yourserver.bitcoinmining.BitcoinMining;
import org.bukkit.scheduler.BukkitRunnable;

public class PriceUpdateTask extends BukkitRunnable {

    private final BitcoinMining plugin;

    public PriceUpdateTask(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getPriceManager().updatePrice();
    }
}