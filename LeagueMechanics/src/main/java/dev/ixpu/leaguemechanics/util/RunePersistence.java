package dev.ixpu.leaguemechanics.util;

import dev.ixpu.leaguemechanics.player.PlayerClassType;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.UUID;

public class RunePersistence {
    private final File dataFile;

    public RunePersistence(Plugin plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "player-runes.yml");
        if (!this.dataFile.exists()) {
            try {
                this.dataFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void savePrimaryPath(UUID playerUUID, RunePath path) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        if (path == null) {
            config.set(playerKey + ".primary-path", null);
        } else {
            config.set(playerKey + ".primary-path", path.getId());
        }
        try {
            config.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RunePath loadPrimaryPath(UUID playerUUID) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        String pathId = config.getString(playerKey + ".primary-path");
        if (pathId == null) {
            return null;
        }
        return RunePath.fromId(pathId);
    }

    public void saveSecondaryPath(UUID playerUUID, RunePath path) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        if (path == null) {
            config.set(playerKey + ".secondary-path", null);
        } else {
            config.set(playerKey + ".secondary-path", path.getId());
        }
        try {
            config.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RunePath loadSecondaryPath(UUID playerUUID) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        String pathId = config.getString(playerKey + ".secondary-path");
        if (pathId == null) {
            return null;
        }
        return RunePath.fromId(pathId);
    }

    public void saveKeystoneRune(UUID playerUUID, String runeId) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        config.set(playerKey + ".keystone-rune", runeId);
        try {
            config.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String loadKeystoneRune(UUID playerUUID) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        return config.getString(playerKey + ".keystone-rune");
    }

    public void savePlayerClass(UUID playerUUID, PlayerClassType classType) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        if (classType == null) {
            config.set(playerKey + ".player-class", null);
        } else {
            config.set(playerKey + ".player-class", classType.getId());
        }
        try {
            config.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PlayerClassType loadPlayerClass(UUID playerUUID) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        String classId = config.getString(playerKey + ".player-class");
        if (classId == null) {
            return null;
        }
        return PlayerClassType.fromId(classId);
    }

    private void saveRuneSlot(UUID playerUUID, String slotKey, String runeId) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        if (runeId == null) {
            config.set(playerKey + ".rune-slots." + slotKey, null);
        } else {
            config.set(playerKey + ".rune-slots." + slotKey, runeId);
        }
        try {
            config.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String loadRuneSlot(UUID playerUUID, String slotKey) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        return config.getString(playerKey + ".rune-slots." + slotKey);
    }

    public void savePrimarySlot1Rune(UUID playerUUID, String runeId) {
        saveRuneSlot(playerUUID, "primary-slot-1", runeId);
    }
    public String loadPrimarySlot1Rune(UUID playerUUID) {
        return loadRuneSlot(playerUUID, "primary-slot-1");
    }

    public void savePrimarySlot2Rune(UUID playerUUID, String runeId) {
        saveRuneSlot(playerUUID, "primary-slot-2", runeId);
    }
    public String loadPrimarySlot2Rune(UUID playerUUID) {
        return loadRuneSlot(playerUUID, "primary-slot-2");
    }

    public void savePrimarySlot3Rune(UUID playerUUID, String runeId) {
        saveRuneSlot(playerUUID, "primary-slot-3", runeId);
    }
    public String loadPrimarySlot3Rune(UUID playerUUID) {
        return loadRuneSlot(playerUUID, "primary-slot-3");
    }

    public void saveSecondarySlot1Rune(UUID playerUUID, String runeId) {
        saveRuneSlot(playerUUID, "secondary-slot-1", runeId);
    }
    public String loadSecondarySlot1Rune(UUID playerUUID) {
        return loadRuneSlot(playerUUID, "secondary-slot-1");
    }

    public void saveSecondarySlot2Rune(UUID playerUUID, String runeId) {
        saveRuneSlot(playerUUID, "secondary-slot-2", runeId);
    }
    public String loadSecondarySlot2Rune(UUID playerUUID) {
        return loadRuneSlot(playerUUID, "secondary-slot-2");
    }

    public void savePrimaryRuneSlot(UUID playerUUID, RuneSlot slot, String runeId) {
        switch (slot) {
            case KEYSTONE -> saveKeystoneRune(playerUUID, runeId);
            case PRIMARY_SLOT_1 -> savePrimarySlot1Rune(playerUUID, runeId);
            case PRIMARY_SLOT_2 -> savePrimarySlot2Rune(playerUUID, runeId);
            case PRIMARY_SLOT_3 -> savePrimarySlot3Rune(playerUUID, runeId);
            default -> throw new IllegalArgumentException("Not a primary slot: " + slot);
        }
    }

    public void saveSecondaryRuneSlot(UUID playerUUID, RuneSlot slot, String runeId) {
        switch (slot) {
            case SECONDARY_SLOT_1 -> saveSecondarySlot1Rune(playerUUID, runeId);
            case SECONDARY_SLOT_2 -> saveSecondarySlot2Rune(playerUUID, runeId);
            default -> throw new IllegalArgumentException("Not a secondary slot: " + slot);
        }
    }

    public void saveRuneShards(UUID playerUUID, String row1, String row2, String row3) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        config.set(playerKey + ".rune-shards.row1", row1);
        config.set(playerKey + ".rune-shards.row2", row2);
        config.set(playerKey + ".rune-shards.row3", row3);
        try {
            config.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[] loadRuneShards(UUID playerUUID) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        String row1 = config.getString(playerKey + ".rune-shards.row1");
        String row2 = config.getString(playerKey + ".rune-shards.row2");
        String row3 = config.getString(playerKey + ".rune-shards.row3");

        if (row1 == null || row2 == null || row3 == null) {
            return null;
        }
        return new String[]{row1, row2, row3};
    }

    public void clearAllRunes(UUID playerUUID) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerKey = playerUUID.toString();
        config.set(playerKey + ".primary-path", null);
        config.set(playerKey + ".keystone-rune", null);
        config.set(playerKey + ".secondary-path", null);
        config.set(playerKey + ".primary-slot-1-rune", null);
        config.set(playerKey + ".primary-slot-2-rune", null);
        config.set(playerKey + ".primary-slot-3-rune", null);
        config.set(playerKey + ".secondary-slot-1-rune", null);
        config.set(playerKey + ".secondary-slot-2-rune", null);
        config.set(playerKey + ".rune-shards", null);

        if (config.getConfigurationSection(playerKey) != null && config.getConfigurationSection(playerKey).getKeys(false).isEmpty()) {
            config.set(playerKey, null);
        }

        try {
            config.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}