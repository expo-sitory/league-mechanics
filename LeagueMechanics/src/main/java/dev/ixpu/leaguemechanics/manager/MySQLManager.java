package dev.ixpu.leaguemechanics.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.ixpu.leaguemechanics.LeagueMechanics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;


public class MySQLManager {
    private final LeagueMechanics plugin;
    private HikariDataSource dataSource;

    public MySQLManager(LeagueMechanics plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        try {
            if (!plugin.getConfig().isConfigurationSection("general-settings.mysql")) {
                plugin.getLogger().warning("MySQL configuration not found in config.yml. Using default values.");
                createDefaultMySQLConfig();
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true",
                plugin.getConfig().getString("general-settings.mysql.host", "localhost"),
                plugin.getConfig().getInt("general-settings.mysql.port", 3306),
                plugin.getConfig().getString("general-settings.mysql.database", "leaguemechanics")
            ));
            config.setUsername(plugin.getConfig().getString("general-settings.mysql.username", "root"));
            config.setPassword(plugin.getConfig().getString("general-settings.mysql.password", ""));

            config.setMaximumPoolSize(plugin.getConfig().getInt("general-settings.mysql.pool-size", 10));
            config.setIdleTimeout(plugin.getConfig().getInt("general-settings.mysql.idle-timeout", 300000));
            config.setMaxLifetime(plugin.getConfig().getInt("general-settings.mysql.max-lifetime", 1800000));
            config.setConnectionTimeout(plugin.getConfig().getInt("general-settings.mysql.connection-timeout", 30000));

            dataSource = new HikariDataSource(config);

            try (Connection connection = dataSource.getConnection()) {
                plugin.getLogger().info("Successfully connected to MySQL database");
            }

            initializeTables();

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize MySQL connection: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "MySQL initialization error", e);
        }
    }

    private void createDefaultMySQLConfig() {
        plugin.getConfig().addDefault("mysql.host", "localhost");
        plugin.getConfig().addDefault("mysql.port", 3306);
        plugin.getConfig().addDefault("mysql.database", "leaguemechanics");
        plugin.getConfig().addDefault("mysql.username", "root");
        plugin.getConfig().addDefault("mysql.password", "");
        plugin.getConfig().addDefault("mysql.pool-size", 10);
        plugin.getConfig().addDefault("mysql.idle-timeout", 300000);
        plugin.getConfig().addDefault("mysql.max-lifetime", 1800000);
        plugin.getConfig().addDefault("mysql.connection-timeout", 30000);
        plugin.saveConfig();
    }

    private void initializeTables() {
        String[] tableCreationQueries = {
            "CREATE TABLE IF NOT EXISTS leaguemechanics_player_runes (" +
            "uuid VARCHAR(36) PRIMARY KEY," +
            "primary_path VARCHAR(50)," +
            "secondary_path VARCHAR(50)," +
            "keystone_rune VARCHAR(50)," +
            "primary_slot_1_rune VARCHAR(50)," +
            "primary_slot_2_rune VARCHAR(50)," +
            "primary_slot_3_rune VARCHAR(50)," +
            "secondary_slot_1_rune VARCHAR(50)," +
            "secondary_slot_2_rune VARCHAR(50)," +
            "rune_shards_row1 VARCHAR(50)," +
            "rune_shards_row2 VARCHAR(50)," +
            "rune_shards_row3 VARCHAR(50)," +
            "player_class VARCHAR(50)," +
            "league_level INT DEFAULT 0" +
            ")",

            "CREATE TABLE IF NOT EXISTS leaguemechanics_player_kda (" +
            "uuid VARCHAR(36) PRIMARY KEY," +
            "kills INT DEFAULT 0," +
            "deaths INT DEFAULT 0," +
            "assists INT DEFAULT 0" +
            ")"
        };

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            for (String query : tableCreationQueries) {
                statement.execute(query);
            }

            try {
                statement.execute("ALTER TABLE leaguemechanics_player_runes ADD COLUMN player_class VARCHAR(50)");
            } catch (SQLException e) {
                //
                if (e.getErrorCode() == 1060) {
                    plugin.getLogger().info("player_class column already exists in leaguemechanics_player_runes table");
                } else {
                    plugin.getLogger().warning("Failed to add player_class column: " + e.getMessage());
                }
            }

            try {
                statement.execute("ALTER TABLE leaguemechanics_player_runes ADD COLUMN league_level INT DEFAULT 0");
            } catch (SQLException e) {
                //
                if (e.getErrorCode() == 1060) {
                    plugin.getLogger().info("league_level column already exists in leaguemechanics_player_runes table");
                } else {
                    plugin.getLogger().warning("Failed to add league_level column: " + e.getMessage());
                }
            }

            try {
                statement.execute("ALTER TABLE leaguemechanics_player_kda ADD COLUMN health_percentage FLOAT DEFAULT 100.0");
            } catch (SQLException e) {
                //
                if (e.getErrorCode() == 1060) {
                    plugin.getLogger().info("health_percentage column already exists in leaguemechanics_player_kda table");
                } else {
                    plugin.getLogger().warning("Failed to add health_percentage column: " + e.getMessage());
                }
            }

            plugin.getLogger().info("MySQL tables initialized successfully");

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize MySQL tables: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "MySQL table initialization error", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initialize();
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public void savePlayerRunes(UUID uuid, String primaryPath, String secondaryPath,
                               String keystoneRune, String primarySlot1, String primarySlot2,
                               String primarySlot3, String secondarySlot1, String secondarySlot2,
                               String shardsRow1, String shardsRow2, String shardsRow3,
                               String playerClass, int leagueLevel) {
        String query = "INSERT INTO leaguemechanics_player_runes (uuid, primary_path, secondary_path, keystone_rune, " +
                      "primary_slot_1_rune, primary_slot_2_rune, primary_slot_3_rune, " +
                      "secondary_slot_1_rune, secondary_slot_2_rune, rune_shards_row1, " +
                      "rune_shards_row2, rune_shards_row3, player_class, league_level) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE " +
                      "primary_path = VALUES(primary_path), " +
                      "secondary_path = VALUES(secondary_path), " +
                      "keystone_rune = VALUES(keystone_rune), " +
                      "primary_slot_1_rune = VALUES(primary_slot_1_rune), " +
                      "primary_slot_2_rune = VALUES(primary_slot_2_rune), " +
                      "primary_slot_3_rune = VALUES(primary_slot_3_rune), " +
                      "secondary_slot_1_rune = VALUES(secondary_slot_1_rune), " +
                      "secondary_slot_2_rune = VALUES(secondary_slot_2_rune), " +
                      "rune_shards_row1 = VALUES(rune_shards_row1), " +
                      "rune_shards_row2 = VALUES(rune_shards_row2), " +
                      "rune_shards_row3 = VALUES(rune_shards_row3), " +
                      "player_class = VALUES(player_class), " +
                      "league_level = VALUES(league_level)";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, uuid.toString());
            statement.setString(2, primaryPath);
            statement.setString(3, secondaryPath);
            statement.setString(4, keystoneRune);
            statement.setString(5, primarySlot1);
            statement.setString(6, primarySlot2);
            statement.setString(7, primarySlot3);
            statement.setString(8, secondarySlot1);
            statement.setString(9, secondarySlot2);
            statement.setString(10, shardsRow1);
            statement.setString(11, shardsRow2);
            statement.setString(12, shardsRow3);
            statement.setString(13, playerClass);
            statement.setInt(14, leagueLevel);

            statement.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save player runes for " + uuid + ": " + e.getMessage());
        }
    }

    public String[] loadPlayerRunes(UUID uuid) {
        String query = "SELECT primary_path, secondary_path, keystone_rune, " +
                      "primary_slot_1_rune, primary_slot_2_rune, primary_slot_3_rune, " +
                      "secondary_slot_1_rune, secondary_slot_2_rune, " +
                      "rune_shards_row1, rune_shards_row2, rune_shards_row3, " +
                      "player_class, league_level " +
                      "FROM leaguemechanics_player_runes WHERE uuid = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new String[]{
                        resultSet.getString("primary_path"),
                        resultSet.getString("secondary_path"),
                        resultSet.getString("keystone_rune"),
                        resultSet.getString("primary_slot_1_rune"),
                        resultSet.getString("primary_slot_2_rune"),
                        resultSet.getString("primary_slot_3_rune"),
                        resultSet.getString("secondary_slot_1_rune"),
                        resultSet.getString("secondary_slot_2_rune"),
                        resultSet.getString("rune_shards_row1"),
                        resultSet.getString("rune_shards_row2"),
                        resultSet.getString("rune_shards_row3"),
                        resultSet.getString("player_class"),
                        String.valueOf(resultSet.getInt("league_level"))
                    };
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load player runes for " + uuid + ": " + e.getMessage());
        }

        return null;
    }

    public void savePlayerKDA(UUID uuid, int kills, int deaths, int assists) {
        String query = "INSERT INTO leaguemechanics_player_kda (uuid, kills, deaths, assists) " +
                      "VALUES (?, ?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE " +
                      "kills = VALUES(kills), " +
                      "deaths = VALUES(deaths), " +
                      "assists = VALUES(assists)";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, uuid.toString());
            statement.setInt(2, kills);
            statement.setInt(3, deaths);
            statement.setInt(4, assists);

            statement.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save player KDA for " + uuid + ": " + e.getMessage());
        }
    }

    public int[] loadPlayerKDA(UUID uuid) {
        String query = "SELECT kills, deaths, assists FROM leaguemechanics_player_kda WHERE uuid = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new int[]{
                        resultSet.getInt("kills"),
                        resultSet.getInt("deaths"),
                        resultSet.getInt("assists")
                    };
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load player KDA for " + uuid + ": " + e.getMessage());
        }

        return new int[]{0, 0, 0};
    }

    public void savePlayerHealthPercentage(UUID uuid, float healthPercentage) {
        String query = "INSERT INTO leaguemechanics_player_kda (uuid, health_percentage) " +
                      "VALUES (?, ?) " +
                      "ON DUPLICATE KEY UPDATE " +
                      "health_percentage = VALUES(health_percentage)";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, uuid.toString());
            statement.setFloat(2, healthPercentage);

            statement.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save player health percentage for " + uuid + ": " + e.getMessage());
        }
    }

    public float loadPlayerHealthPercentage(UUID uuid) {
        String query = "SELECT health_percentage FROM leaguemechanics_player_kda WHERE uuid = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getFloat("health_percentage");
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load player health percentage for " + uuid + ": " + e.getMessage());
        }

        return 100.0f;
    }
}