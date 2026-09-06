package dev.ixpu.leaguemechanics.player;

import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlayerKDA {
    private static PlayerKDA instance;
    private final ConcurrentMap<UUID, KDAData> playerData = new ConcurrentHashMap<>();
    private final Path dataFile;

    private PlayerKDA() {
        File pluginFolder = dev.ixpu.leaguemechanics.LeagueMechanics.getInstance().getDataFolder();
        dataFile = pluginFolder.toPath().resolve("kda-data.txt");
        loadFromFile();
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

        try {
            java.util.List<String> lines = Files.exists(dataFile) ? Files.readAllLines(dataFile) : new java.util.ArrayList<>();
            boolean found = false;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith(uuid.toString() + ":")) {
                    lines.set(i, uuid.toString() + ":" + data.kills + ":" + data.deaths + ":" + data.assists);
                    found = true;
                    break;
                }
            }

            if (!found) {
                lines.add(uuid.toString() + ":" + data.kills + ":" + data.deaths + ":" + data.assists);
            }

            Files.createDirectories(dataFile.getParent());
            Files.write(dataFile, lines);
        } catch (IOException e) {
            dev.ixpu.leaguemechanics.LeagueMechanics.getInstance().getLogger().warning("Failed to save KDA for " + uuid + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        for (UUID uuid : playerData.keySet()) {
            saveForPlayer(uuid);
        }
    }

    private void loadFromFile() {
        if (!Files.exists(dataFile)) return;

        try {
            for (String line : Files.readAllLines(dataFile)) {
                String[] parts = line.split(":");
                if (parts.length >= 4) {
                    try {
                        UUID uuid = UUID.fromString(parts[0]);
                        int kills = Integer.parseInt(parts[1]);
                        int deaths = Integer.parseInt(parts[2]);
                        int assists = Integer.parseInt(parts[3]);
                        KDAData data = new KDAData();
                        data.kills = kills;
                        data.deaths = deaths;
                        data.assists = assists;
                        playerData.put(uuid, data);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            dev.ixpu.leaguemechanics.LeagueMechanics.getInstance().getLogger().warning("Failed to load KDA data: " + e.getMessage());
        }
    }

    private static class KDAData {
        int kills = 0;
        int deaths = 0;
        int assists = 0;
    }
}
