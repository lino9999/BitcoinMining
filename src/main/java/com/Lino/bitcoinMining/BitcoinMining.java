package com.Lino.bitcoinMining;

import com.Lino.bitcoinMining.commands.*;
import com.Lino.bitcoinMining.listeners.*;
import com.Lino.bitcoinMining.api.PriceManager;
import com.Lino.bitcoinMining.database.DatabaseManager;
import com.Lino.bitcoinMining.managers.*;
import com.Lino.bitcoinMining.tasks.BlackMarketTask;
import com.Lino.bitcoinMining.tasks.MiningTask;
import com.Lino.bitcoinMining.tasks.PriceUpdateTask;
import com.Lino.bitcoinMining.utils.MessageManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BitcoinMining extends JavaPlugin {

    private static BitcoinMining instance;
    private Economy economy;
    private DatabaseManager databaseManager;
    private MiningRigManager miningRigManager;
    private BitcoinManager bitcoinManager;
    private PriceManager priceManager;
    private LeaderboardManager leaderboardManager;
    private MessageManager messageManager;
    private BlackMarketManager blackMarketManager;
    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("messages.yml", false);

        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        messageManager = new MessageManager(this);
        databaseManager = new DatabaseManager(this);
        miningRigManager = new MiningRigManager(this);
        bitcoinManager = new BitcoinManager(this);
        priceManager = new PriceManager(this);
        leaderboardManager = new LeaderboardManager(this);
        blackMarketManager = new BlackMarketManager(this);
        hologramManager = new HologramManager(this);

        registerCommands();
        registerListeners();
        startTasks();

        getLogger().info("BitcoinMining plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("BitcoinMining plugin disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void registerCommands() {
        getCommand("bitcoin").setExecutor(new BitcoinCommand(this));
        getCommand("btc").setExecutor(new BitcoinCommand(this));
        getCommand("miner").setExecutor(new MinerCommand(this));
        getCommand("btctop").setExecutor(new LeaderboardCommand(this));
        getCommand("btctransfer").setExecutor(new TransferCommand(this));
        getCommand("blackmarket").setExecutor(new BlackMarketCommand(this));
        getCommand("getrig").setExecutor(new GetRigCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplosionListener(this), this);
    }

    private void startTasks() {
        new MiningTask(this).runTaskTimer(this, 20L, 20L);
        new PriceUpdateTask(this).runTaskTimerAsynchronously(this, 0L, 20L * 60);
        new BlackMarketTask(this).runTaskTimer(this, 20L, 20L);
    }

    public void reload() {
        reloadConfig();
        messageManager.reload();
        blackMarketManager.reload();
    }

    public static BitcoinMining getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MiningRigManager getMiningRigManager() {
        return miningRigManager;
    }

    public BitcoinManager getBitcoinManager() {
        return bitcoinManager;
    }

    public PriceManager getPriceManager() {
        return priceManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public BlackMarketManager getBlackMarketManager() {
        return blackMarketManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }
}