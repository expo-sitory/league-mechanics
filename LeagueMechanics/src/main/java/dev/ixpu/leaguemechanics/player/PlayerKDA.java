package dev.ixpu.leaguemechanics.player;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.MySQLManager;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public class PlayerKDA {
    private static PlayerKDA instance;
    private final ConcurrentMap<UUID, KDAData> playerData = new ConcurrentHashMap<>();
    private final MySQLManager mysqlManager;

    private PlayerKDA() {
        this.mysqlManager = new MySQLManager(LeagueMechanics.getInstance());
        loadAllFromDatabase();
    }

    public void close() {
        if (mysqlManager != null) {
            mysqlManager.close();
        }
    }

    /**
     * Clears all KDA data from the database.
     */
    public void clearAllKda() {
        String query = "DELETE FROM player_kda";

        try (Connection connection = mysqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            int rowsDeleted = statement.executeUpdate();
            LeagueMechanics.getInstance().getLogger().info("Cleared " + rowsDeleted + " KDA records from database");

        } catch (SQLException e) {
            LeagueMechanics.getInstance().getLogger().warning("Failed to clear KDA data: " + e.getMessage());
        }
    }

    public static PlayerKDA getInstance() {
        if (instance == null) {
            instance = new PlayerKDA();
        }
        return instance;
    }

    public void recordKill(Player player) {
        playerData.computeIfAbsent(player.getUniqueId(), k -> new KDAData()).kills++;
    }

    public void recordDeath(Player player) {
        playerData.computeIfAbsent(player.getUniqueId(), k -> new KDAData()).deaths++;
    }

    public void recordAssist(Player player) {
        playerData.computeIfAbsent(player.getUniqueId(), k -> new KDAData()).assists++;
    }

    public int getKills(UUID uuid) {
        KDAData data = playerData.get(uuid);
        return data != null ? data.kills : 0;
    }

    public int getDeaths(UUID uuid) {
        KDAData data = playerData.get(uuid);
        return data != null ? data.deaths : 0;
    }

    public int getAssists(UUID uuid) {
        KDAData data = playerData.get(uuid);
        return data != null ? data.assists : 0;
    }

    public String getFormattedKDA(UUID uuid) {
        return getKills(uuid) + "/" + getDeaths(uuid) + "/" + getAssists(uuid);
    }

    public void saveForPlayer(UUID uuid) {
        KDAData data = playerData.get(uuid);
        if (data == null) return;

        mysqlManager.savePlayerKDA(uuid, data.kills, data.deaths, data.assists);
    }

    public void saveAll() {
        for (UUID uuid : playerData.keySet()) {
            saveForPlayer(uuid);
        }
    }

    public void loadAllFromDatabase() {
        playerData.clear();
        String query = "SELECT uuid, kills, deaths, assists FROM player_kda";

        try (Connection connection = mysqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                int kills = resultSet.getInt("kills");
                int deaths = resultSet.getInt("deaths");
                int assists = resultSet.getInt("assists");
                KDAData data = new KDAData();
                data.kills = kills;
                data.deaths = deaths;
                data.assists = assists;
                playerData.put(uuid, data);
            }

        } catch (SQLException e) {
            dev.ixpu.leaguemechanics.LeagueMechanics.getInstance().getLogger().warning("Failed to load KDA data from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadForPlayer(UUID uuid) {
        int[] kda = mysqlManager.loadPlayerKDA(uuid);
        if (kda != null) {
            KDAData data = new KDAData();
            data.kills = kda[0];
            data.deaths = kda[1];
            data.assists = kda[2];
            playerData.put(uuid, data);
        }
    }

    private static class KDAData {
        int kills = 0;
        int deaths = 0;
        int assists = 0;
    }
}
