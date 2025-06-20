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
                            "fuel INTEGER DEFAULT 0," +
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

            migrateTypeToLevel();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    private void migrateTypeToLevel() {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(mining_rigs)");
            boolean hasTypeColumn = false;
            boolean hasLevelColumn = false;

            while (rs.next()) {
                String columnName = rs.getString("name");
                if (columnName.equals("type")) hasTypeColumn = true;
                if (columnName.equals("level")) hasLevelColumn = true;
            }

            if (hasTypeColumn && !hasLevelColumn) {
                stmt.executeUpdate("ALTER TABLE mining_rigs ADD COLUMN level INTEGER DEFAULT 1");

                ResultSet rigData = stmt.executeQuery("SELECT id, type FROM mining_rigs");
                while (rigData.next()) {
                    String id = rigData.getString("id");
                    String type = rigData.getString("type");
                    int level = 1;

                    if (type != null && type.startsWith("LEVEL_")) {
                        try {
                            level = Integer.parseInt(type.replace("LEVEL_", ""));
                        } catch (NumberFormatException e) {
                            level = 1;
                        }
                    }

                    PreparedStatement ps = connection.prepareStatement("UPDATE mining_rigs SET level = ? WHERE id = ?");
                    ps.setInt(1, level);
                    ps.setString(2, id);
                    ps.executeUpdate();
                    ps.close();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().info("Migration check completed");
        }
    }

    public void saveBalance(UUID playerUuid, double balance) {
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

    public void saveMiningRig(MiningRig rig) {
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
            ps.setInt(8, rig.getFuel());
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
                Location location = new Location(
                        plugin.getServer().getWorld(rs.getString("world")),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z")
                );

                int level = 1;
                try {
                    level = rs.getInt("level");
                } catch (SQLException e) {
                    String type = rs.getString("type");
                    if (type != null && type.startsWith("LEVEL_")) {
                        try {
                            level = Integer.parseInt(type.replace("LEVEL_", ""));
                        } catch (NumberFormatException ex) {
                            level = 1;
                        }
                    }
                }

                MiningRig rig = new MiningRig(id, ownerId, location, level);
                rig.addFuel(rs.getInt("fuel"));
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

    public void deleteMiningRig(UUID rigId) {
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

    public void updateTotalMined(UUID playerUuid, double amount) {
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