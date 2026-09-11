package dev.ixpu.leaguemechanics.util;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.MySQLManager;
import dev.ixpu.leaguemechanics.player.PlayerClassType;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.UUID;

public class RunePersistence {
    private final MySQLManager mysqlManager;

    public RunePersistence(Plugin plugin) {
        this.mysqlManager = new MySQLManager((LeagueMechanics) plugin);
    }


    public String[] loadPlayerRunes(UUID playerUUID) {
        return mysqlManager.loadPlayerRunes(playerUUID);
    }

    public void savePrimaryPath(UUID playerUUID, RunePath path) {
        String primaryPathId = path != null ? path.getId() : null;
        String[] existingData = loadPlayerRunes(playerUUID);
        String secondaryPath = existingData != null ? existingData[1] : null;
        String keystoneRune = existingData != null ? existingData[2] : null;
        String primarySlot1 = existingData != null ? existingData[3] : null;
        String primarySlot2 = existingData != null ? existingData[4] : null;
        String primarySlot3 = existingData != null ? existingData[5] : null;
        String secondarySlot1 = existingData != null ? existingData[6] : null;
        String secondarySlot2 = existingData != null ? existingData[7] : null;
        String shardsRow1 = existingData != null ? existingData[8] : null;
        String shardsRow2 = existingData != null ? existingData[9] : null;
        String shardsRow3 = existingData != null ? existingData[10] : null;
        String playerClass = existingData != null ? existingData[11] : null;

        mysqlManager.savePlayerRunes(
            playerUUID,
            primaryPathId,
            secondaryPath,
            keystoneRune,
            primarySlot1,
            primarySlot2,
            primarySlot3,
            secondarySlot1,
            secondarySlot2,
            shardsRow1,
            shardsRow2,
            shardsRow3,
            playerClass
        );
    }

    public RunePath loadPrimaryPath(UUID playerUUID) {
        String[] data = loadPlayerRunes(playerUUID);
        if (data == null) {
            return null;
        }
        String pathId = data[0];
        if (pathId == null) {
            return null;
        }
        return RunePath.fromId(pathId);
    }

    public void saveSecondaryPath(UUID playerUUID, RunePath path) {
        String secondaryPathId = path != null ? path.getId() : null;
        String[] existingData = loadPlayerRunes(playerUUID);
        String primaryPath = existingData != null ? existingData[0] : null;
        String keystoneRune = existingData != null ? existingData[2] : null;
        String primarySlot1 = existingData != null ? existingData[3] : null;
        String primarySlot2 = existingData != null ? existingData[4] : null;
        String primarySlot3 = existingData != null ? existingData[5] : null;
        String secondarySlot1 = existingData != null ? existingData[6] : null;
        String secondarySlot2 = existingData != null ? existingData[7] : null;
        String shardsRow1 = existingData != null ? existingData[8] : null;
        String shardsRow2 = existingData != null ? existingData[9] : null;
        String shardsRow3 = existingData != null ? existingData[10] : null;
        String playerClass = existingData != null ? existingData[11] : null;

        mysqlManager.savePlayerRunes(
            playerUUID,
            primaryPath,
            secondaryPathId,
            keystoneRune,
            primarySlot1,
            primarySlot2,
            primarySlot3,
            secondarySlot1,
            secondarySlot2,
            shardsRow1,
            shardsRow2,
            shardsRow3,
            playerClass
        );
    }

    public RunePath loadSecondaryPath(UUID playerUUID) {
        String[] data = loadPlayerRunes(playerUUID);
        if (data == null) {
            return null;
        }
        String pathId = data[1];
        if (pathId == null) {
            return null;
        }
        return RunePath.fromId(pathId);
    }

    public void saveKeystoneRune(UUID playerUUID, String runeId) {
        String[] existingData = loadPlayerRunes(playerUUID);
        String primaryPath = existingData != null ? existingData[0] : null;
        String secondaryPath = existingData != null ? existingData[1] : null;
        String keystoneRune = existingData != null ? existingData[2] : null;
        String primarySlot1 = existingData != null ? existingData[3] : null;
        String primarySlot2 = existingData != null ? existingData[4] : null;
        String primarySlot3 = existingData != null ? existingData[5] : null;
        String secondarySlot1 = existingData != null ? existingData[6] : null;
        String secondarySlot2 = existingData != null ? existingData[7] : null;
        String shardsRow1 = existingData != null ? existingData[8] : null;
        String shardsRow2 = existingData != null ? existingData[9] : null;
        String shardsRow3 = existingData != null ? existingData[10] : null;
        String playerClass = existingData != null ? existingData[11] : null;

        mysqlManager.savePlayerRunes(
            playerUUID,
            primaryPath,
            secondaryPath,
            runeId,
            primarySlot1,
            primarySlot2,
            primarySlot3,
            secondarySlot1,
            secondarySlot2,
            shardsRow1,
            shardsRow2,
            shardsRow3,
            playerClass
        );
    }

    public String loadKeystoneRune(UUID playerUUID) {
        String[] data = loadPlayerRunes(playerUUID);
        if (data == null) {
            return null;
        }
        return data[2];
    }

    public void savePlayerClass(UUID playerUUID, PlayerClassType classType) {
        String[] existingData = loadPlayerRunes(playerUUID);
        String primaryPath = existingData != null ? existingData[0] : null;
        String secondaryPath = existingData != null ? existingData[1] : null;
        String keystoneRune = existingData != null ? existingData[2] : null;
        String primarySlot1 = existingData != null ? existingData[3] : null;
        String primarySlot2 = existingData != null ? existingData[4] : null;
        String primarySlot3 = existingData != null ? existingData[5] : null;
        String secondarySlot1 = existingData != null ? existingData[6] : null;
        String secondarySlot2 = existingData != null ? existingData[7] : null;
        String shardsRow1 = existingData != null ? existingData[8] : null;
        String shardsRow2 = existingData != null ? existingData[9] : null;
        String shardsRow3 = existingData != null ? existingData[10] : null;
        String playerClass = classType != null ? classType.getId() : null;

        mysqlManager.savePlayerRunes(
            playerUUID,
            primaryPath,
            secondaryPath,
            keystoneRune,
            primarySlot1,
            primarySlot2,
            primarySlot3,
            secondarySlot1,
            secondarySlot2,
            shardsRow1,
            shardsRow2,
            shardsRow3,
            playerClass
        );
    }

    public PlayerClassType loadPlayerClass(UUID playerUUID) {
        String[] data = loadPlayerRunes(playerUUID);
        if (data == null) {
            return null;
        }
        String classId = data[11];
        if (classId == null) {
            return null;
        }
        return PlayerClassType.fromId(classId);
    }

    private void saveRuneSlot(UUID playerUUID, String slotKey, String runeId) {
        String[] existingData = loadPlayerRunes(playerUUID);
        String primaryPath = existingData != null ? existingData[0] : null;
        String secondaryPath = existingData != null ? existingData[1] : null;
        String keystoneRune = existingData != null ? existingData[2] : null;
        String primarySlot1 = existingData != null ? existingData[3] : null;
        String primarySlot2 = existingData != null ? existingData[4] : null;
        String primarySlot3 = existingData != null ? existingData[5] : null;
        String secondarySlot1 = existingData != null ? existingData[6] : null;
        String secondarySlot2 = existingData != null ? existingData[7] : null;
        String shardsRow1 = existingData != null ? existingData[8] : null;
        String shardsRow2 = existingData != null ? existingData[9] : null;
        String shardsRow3 = existingData != null ? existingData[10] : null;
        String playerClass = existingData != null ? existingData[11] : null;

        switch (slotKey) {
            case "primary-slot-1":
                primarySlot1 = runeId;
                break;
            case "primary-slot-2":
                primarySlot2 = runeId;
                break;
            case "primary-slot-3":
                primarySlot3 = runeId;
                break;
            case "secondary-slot-1":
                secondarySlot1 = runeId;
                break;
            case "secondary-slot-2":
                secondarySlot2 = runeId;
                break;
            default:
                return;
        }

        mysqlManager.savePlayerRunes(
            playerUUID,
            primaryPath,
            secondaryPath,
            keystoneRune,
            primarySlot1,
            primarySlot2,
            primarySlot3,
            secondarySlot1,
            secondarySlot2,
            shardsRow1,
            shardsRow2,
            shardsRow3,
            playerClass
        );
    }

    private String loadRuneSlot(UUID playerUUID, String slotKey) {
        String[] data = loadPlayerRunes(playerUUID);
        if (data == null) {
            return null;
        }
        switch (slotKey) {
            case "primary-slot-1":
                return data[3];
            case "primary-slot-2":
                return data[4];
            case "primary-slot-3":
                return data[5];
            case "secondary-slot-1":
                return data[6];
            case "secondary-slot-2":
                return data[7];
            default:
                return null;
        }
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
        String[] existingData = loadPlayerRunes(playerUUID);
        String primaryPath = existingData != null ? existingData[0] : null;
        String secondaryPath = existingData != null ? existingData[1] : null;
        String keystoneRune = existingData != null ? existingData[2] : null;
        String primarySlot1 = existingData != null ? existingData[3] : null;
        String primarySlot2 = existingData != null ? existingData[4] : null;
        String primarySlot3 = existingData != null ? existingData[5] : null;
        String secondarySlot1 = existingData != null ? existingData[6] : null;
        String secondarySlot2 = existingData != null ? existingData[7] : null;
        String shardsRow1 = existingData != null ? existingData[8] : null;
        String shardsRow2 = existingData != null ? existingData[9] : null;
        String shardsRow3 = existingData != null ? existingData[10] : null;
        String playerClass = existingData != null ? existingData[11] : null;

        mysqlManager.savePlayerRunes(
            playerUUID,
            primaryPath,
            secondaryPath,
            keystoneRune,
            primarySlot1,
            primarySlot2,
            primarySlot3,
            secondarySlot1,
            secondarySlot2,
            row1,
            row2,
            row3,
            playerClass
        );
    }

    public String[] loadRuneShards(UUID playerUUID) {
        String[] data = loadPlayerRunes(playerUUID);
        if (data == null) {
            return null;
        }
        return new String[]{data[8], data[9], data[10]};
    }

    public void clearAllRunes(UUID playerUUID) {
        mysqlManager.savePlayerRunes(
            playerUUID,
            null, // primary-path
            null, // secondary-path
            null, // keystone-rune
            null, // primary-slot-1-rune
            null, // primary-slot-2-rune
            null, // primary-slot-3-rune
            null, // secondary-slot-1-rune
            null, // secondary-slot-2-rune
            null, // rune-shards-row1
            null, // rune-shards-row2
            null, // rune-shards-row3
            null  // player-class
        );
    }
}