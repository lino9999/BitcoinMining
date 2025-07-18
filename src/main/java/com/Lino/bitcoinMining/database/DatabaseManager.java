package com.Lino.bitcoinMining.database;

import com.Lino.bitcoinMining.BitcoinMining;
import com.Lino.bitcoinMining.models.MiningRig;
import org.bukkit.Location;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final BitcoinMining plugin;
    private Connection connection;

    public DatabaseManager(BitcoinMining plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/database.db");
            createTables();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS balances (" +
                            "uuid VARCHAR(36) PRIMARY KEY," +
                            "balance DOUBLE DEFAULT 0.0," +
                            "total_mined DOUBLE DEFAULT 0.0" +
                            ")"
            );

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS mining_rigs (" +
                            "id VARCHAR(36) PRIMARY KEY," +
                            "owner_uuid VARCHAR(36)," +
                            "world VARCHAR(100)," +
                            "x INTEGER," +
                            "y INTEGER," +
                            "z INTEGER," +
                            "level INTEGER DEFAULT 1," +
                            "fuel DOUBLE DEFAULT 0.0," +
                            "active BOOLEAN DEFAULT FALSE," +
                            "overclock DOUBLE DEFAULT 1.0," +
                            "total_mined DOUBLE DEFAULT 0.0" +
                            ")"
            );

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS transactions (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "from_uuid VARCHAR(36)," +
                            "to_uuid VARCHAR(36)," +
                            "amount DOUBLE," +
                            "fee DOUBLE," +
                            "timestamp BIGINT" +
                            ")"
            );
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    public synchronized void saveBalance(UUID playerUuid, double balance) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO balances (uuid, balance) VALUES (?, ?)"
        )) {
            ps.setString(1, playerUuid.toString());
            ps.setDouble(2, balance);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save balance: " + e.getMessage());
        }
    }

    public void loadAllBalances(Map<UUID, Double> balances) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM balances")) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                double balance = rs.getDouble("balance");
                balances.put(uuid, balance);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load balances: " + e.getMessage());
        }
    }

    public synchronized void saveMiningRig(MiningRig rig) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO mining_rigs (id, owner_uuid, world, x, y, z, level, fuel, active, overclock, total_mined) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )) {
            ps.setString(1, rig.getId().toString());
            ps.setString(2, rig.getOwnerId().toString());
            ps.setString(3, rig.getLocation().getWorld().getName());
            ps.setInt(4, rig.getLocation().getBlockX());
            ps.setInt(5, rig.getLocation().getBlockY());
            ps.setInt(6, rig.getLocation().getBlockZ());
            ps.setInt(7, rig.getLevel());
            ps.setDouble(8, rig.getFuel());
            ps.setBoolean(9, rig.isActive());
            ps.setDouble(10, rig.getOverclock());
            ps.setDouble(11, rig.getTotalBitcoinMined());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save mining rig: " + e.getMessage());
        }
    }

    public List<MiningRig> loadAllMiningRigs() {
        List<MiningRig> rigs = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM mining_rigs")) {

            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                UUID ownerId = UUID.fromString(rs.getString("owner_uuid"));
                String worldName = rs.getString("world");

                if (plugin.getServer().getWorld(worldName) == null) {
                    plugin.getLogger().warning("World " + worldName + " not found for rig " + id);
                    continue;
                }

                Location location = new Location(
                        plugin.getServer().getWorld(worldName),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z")
                );

                int level = rs.getInt("level");

                MiningRig rig = new MiningRig(id, ownerId, location, level);
                rig.addFuel((int)rs.getDouble("fuel"));
                rig.setActive(rs.getBoolean("active"));
                rig.setOverclock(rs.getDouble("overclock"));
                rig.addMinedBitcoin(rs.getDouble("total_mined"));

                rigs.add(rig);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load mining rigs: " + e.getMessage());
        }
        return rigs;
    }

    public synchronized void deleteMiningRig(UUID rigId) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM mining_rigs WHERE id = ?")) {
            ps.setString(1, rigId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete mining rig: " + e.getMessage());
        }
    }

    public void saveTransaction(UUID from, UUID to, double amount, double fee) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO transactions (from_uuid, to_uuid, amount, fee, timestamp) VALUES (?, ?, ?, ?, ?)"
        )) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            ps.setDouble(3, amount);
            ps.setDouble(4, fee);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save transaction: " + e.getMessage());
        }
    }

    public Map<UUID, Double> getTopMiners(int limit) {
        Map<UUID, Double> topMiners = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, total_mined FROM balances ORDER BY total_mined DESC LIMIT ?"
        )) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                double totalMined = rs.getDouble("total_mined");
                topMiners.put(uuid, totalMined);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get top miners: " + e.getMessage());
        }
        return topMiners;
    }

    public synchronized void updateTotalMined(UUID playerUuid, double amount) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE balances SET total_mined = total_mined + ? WHERE uuid = ?"
        )) {
            ps.setDouble(1, amount);
            ps.setString(2, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update total mined: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database: " + e.getMessage());
        }
    }
}