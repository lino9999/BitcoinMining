package com.Lino.bitcoinMining.models;

import org.bukkit.Location;
import java.util.UUID;

public class MiningRig {

    public enum RigType {
        BRONZE("Bronze", 0.001, 64, 1.0, 100),
        SILVER("Silver", 0.003, 128, 0.8, 500),
        GOLD("Gold", 0.008, 256, 0.6, 1500),
        DIAMOND("Diamond", 0.02, 512, 0.4, 5000);

        private final String name;
        private final double baseHashRate;
        private final int fuelCapacity;
        private final double fuelConsumption;
        private final double upgradeCost;

        RigType(String name, double baseHashRate, int fuelCapacity, double fuelConsumption, double upgradeCost) {
            this.name = name;
            this.baseHashRate = baseHashRate;
            this.fuelCapacity = fuelCapacity;
            this.fuelConsumption = fuelConsumption;
            this.upgradeCost = upgradeCost;
        }

        public String getName() { return name; }
        public double getBaseHashRate() { return baseHashRate; }
        public int getFuelCapacity() { return fuelCapacity; }
        public double getFuelConsumption() { return fuelConsumption; }
        public double getUpgradeCost() { return upgradeCost; }
    }

    private final UUID id;
    private final UUID ownerId;
    private final Location location;
    private RigType type;
    private int fuel;
    private boolean active;
    private double overclock;
    private long lastMiningTime;
    private double totalBitcoinMined;

    public MiningRig(UUID id, UUID ownerId, Location location, RigType type) {
        this.id = id;
        this.ownerId = ownerId;
        this.location = location;
        this.type = type;
        this.fuel = 0;
        this.active = false;
        this.overclock = 1.0;
        this.lastMiningTime = System.currentTimeMillis();
        this.totalBitcoinMined = 0.0;
    }

    public double getEffectiveHashRate() {
        return type.getBaseHashRate() * overclock;
    }

    public double getEffectiveFuelConsumption() {
        return type.getFuelConsumption() * overclock;
    }

    public boolean consumeFuel(double amount) {
        if (fuel >= amount) {
            fuel -= amount;
            return true;
        }
        return false;
    }

    public void addFuel(int amount) {
        fuel = Math.min(fuel + amount, type.getFuelCapacity());
    }

    public boolean canUpgrade() {
        return type.ordinal() < RigType.values().length - 1;
    }

    public RigType getNextTier() {
        if (canUpgrade()) {
            return RigType.values()[type.ordinal() + 1];
        }
        return null;
    }

    public void upgrade() {
        if (canUpgrade()) {
            type = getNextTier();
        }
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public Location getLocation() { return location; }
    public RigType getType() { return type; }
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