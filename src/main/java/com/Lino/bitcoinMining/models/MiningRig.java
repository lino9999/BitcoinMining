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

    public MiningRig(UUID id, UUID ownerId, Location location, RigType type) {
        this(id, ownerId, location, type.getLevel());
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

    public RigType getType() {
        return RigType.fromLevel(level);
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

    public enum RigType {
        LEVEL_1("Level 1", 1),
        LEVEL_2("Level 2", 2),
        LEVEL_3("Level 3", 3),
        LEVEL_4("Level 4", 4),
        LEVEL_5("Level 5", 5),
        LEVEL_6("Level 6", 6),
        LEVEL_7("Level 7", 7),
        LEVEL_8("Level 8", 8),
        LEVEL_9("Level 9", 9),
        LEVEL_10("Level 10", 10),
        LEVEL_11("Level 11", 11),
        LEVEL_12("Level 12", 12),
        LEVEL_13("Level 13", 13),
        LEVEL_14("Level 14", 14),
        LEVEL_15("Level 15", 15),
        LEVEL_16("Level 16", 16),
        LEVEL_17("Level 17", 17),
        LEVEL_18("Level 18", 18),
        LEVEL_19("Level 19", 19),
        LEVEL_20("Level 20 - MAX", 20);

        private final String name;
        private final int level;

        RigType(String name, int level) {
            this.name = name;
            this.level = level;
        }

        public String getName() { return name; }
        public int getLevel() { return level; }

        public static RigType fromLevel(int level) {
            for (RigType type : values()) {
                if (type.level == level) {
                    return type;
                }
            }
            return LEVEL_1;
        }

        public static RigType valueOf(int level) {
            return fromLevel(level);
        }

        public double getHashRate() {
            return BitcoinMining.getInstance().getConfig().getDouble("rig-levels.level-" + level + ".hash-rate", 0.001 * level);
        }

        public int getFuelCapacity() {
            return BitcoinMining.getInstance().getConfig().getInt("rig-levels.level-" + level + ".fuel-capacity", 64 + (level * 20));
        }

        public double getFuelConsumption() {
            return BitcoinMining.getInstance().getConfig().getDouble("rig-levels.level-" + level + ".fuel-consumption", 1.0 - (level * 0.02));
        }
    }
}