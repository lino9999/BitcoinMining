package com.Lino.bitcoinMining.tasks;

import com.Lino.bitcoinMining.BitcoinMining;
import org.bukkit.scheduler.BukkitRunnable;

public class BlackMarketTask extends BukkitRunnable {

    private final BitcoinMining plugin;

    public BlackMarketTask(BitcoinMining plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        boolean shouldBeOpen = plugin.getBlackMarketManager().shouldBeOpen();
        boolean isOpen = plugin.getBlackMarketManager().isOpen();

        if (shouldBeOpen && !isOpen) {
            plugin.getBlackMarketManager().open();
        } else if (!shouldBeOpen && isOpen) {
            plugin.getBlackMarketManager().close();
        }
    }
}