package com.Lino.bitcoinMining.models;

import com.Lino.bitcoinMining.BitcoinMining;
import org.bukkit.Location;
import java.util.UUID;

public class MiningRig {

    private final UUID id;
    private final UUID ownerId;
    private final Location location;
    private int level;
    private int fuel;
    private boolean active;
    private double overclock;
    private long lastMiningTime;
    private double totalBitcoinMined;

    public MiningRig(UUID id, UUID ownerId, Location location, int level) {
        this.id = id;
        this.ownerId = ownerId;
        this.location = location;
        this.level = level;
        this.fuel = 0;
        this.active = false;
        this.overclock = 1.0;
        this.lastMiningTime = System.currentTimeMillis();
        this.totalBitcoinMined = 0.0;
    }

    public double getBaseHashRate() {
        return BitcoinMining.getInstance().getConfig().getDouble("rig-levels.level-" + level + ".hash-rate", 0.001 * level);
    }

    public int getFuelCapacity() {
        return BitcoinMining.getInstance().getConfig().getInt("rig-levels.level-" + level + ".fuel-capacity", 64 + (level * 20));
    }

    public double getFuelConsumption() {
        return BitcoinMining.getInstance().getConfig().getDouble("rig-levels.level-" + level + ".fuel-consumption", 1.0 - (level * 0.02));
    }

    public double getUpgradeCost() {
        if (level >= 20) return -1;
        return BitcoinMining.getInstance().getConfig().getDouble("rig-levels.level-" + (level + 1) + ".upgrade-cost", 100.0 * (level + 1));
    }

    public double getEffectiveHashRate() {
        return getBaseHashRate() * overclock;
    }

    public double getEffectiveFuelConsumption() {
        return getFuelConsumption() * overclock;
    }

    public boolean consumeFuel(double amount) {
        if (fuel >= amount) {
            fuel -= amount;
            return true;
        }
        return false;
    }

    public void addFuel(int amount) {
        fuel = Math.min(fuel + amount, getFuelCapacity());
    }

    public boolean canUpgrade() {
        return level < 20;
    }

    public int getNextLevel() {
        if (canUpgrade()) {
            return level + 1;
        }
        return -1;
    }

    public void upgrade() {
        if (canUpgrade()) {
            level++;
        }
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public Location getLocation() { return location; }
    public int getLevel() { return level; }
    public int getFuel() { return fuel; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public double getOverclock() { return overclock; }
    public void setOverclock(double overclock) { this.overclock = Math.max(1.0, Math.min(3.0, overclock)); }
    public long getLastMiningTime() { return lastMiningTime; }
    public void setLastMiningTime(long time) { this.lastMiningTime = time; }
    public double getTotalBitcoinMined() { return totalBitcoinMined; }
    public void addMinedBitcoin(double amount) { this.totalBitcoinMined += amount; }
}