package dev.ixpu.leaguemechanics.entity.player;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerClass {

    public record ClassStats(double baseAD, double baseAP, double baseHP,
                            double baseAR, double baseMR, double baseAS,
                            double attackSpeedRatio) {}

    private static final Map<UUID, PlayerClassType> playerClassCache = new HashMap<>();
    private static final Map<PlayerClassType, ClassStats> CLASS_STATS = new EnumMap<>(PlayerClassType.class);

    public static ClassStats getClassStats(PlayerClassType type) {
        return CLASS_STATS.get(type);
    }

    static {
        CLASS_STATS.put(PlayerClassType.FIGHTER,   new ClassStats(10.0,  0.0, 0.0, 10.0,  0.0, 0.90, 0.90));
        CLASS_STATS.put(PlayerClassType.SUPPORT,   new ClassStats( 7.5,  5.0, 3.0,  5.0, 5.0, 0.70, 0.70));
        CLASS_STATS.put(PlayerClassType.ASSASSIN,  new ClassStats( 4.5,  2.5, 0.0,  0.0,  0.0, 1.15, 1.15));
        CLASS_STATS.put(PlayerClassType.MAGE,      new ClassStats( 0.0, 15.0, 0.0,  0.0,  0.0, 1.05, 1.05));
        CLASS_STATS.put(PlayerClassType.TANK,      new ClassStats( 3.5,  2.0, 6.0, 25.0, 15.0, 0.50, 0.50));
        CLASS_STATS.put(PlayerClassType.MARKSMAN,  new ClassStats( 6.0,  0.0, 0.0,  5.0,  0.0, 1.30, 1.30));
    }

    public static void setPlayerClass(Player player, PlayerClassType classType) {
        UUID uuid = player.getUniqueId();
        playerClassCache.put(uuid, classType);

        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin != null && plugin.getRunePersistence() != null) {
            plugin.getRunePersistence().savePlayerClass(uuid, classType);
        }
    }

    public static PlayerClassType getPlayerClass(Player player) {
        UUID uuid = player.getUniqueId();

        if (playerClassCache.containsKey(uuid)) {
            return playerClassCache.get(uuid);
        }

        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin != null && plugin.getRunePersistence() != null) {
            PlayerClassType loaded = plugin.getRunePersistence().loadPlayerClass(uuid);
            if (loaded != null) {
                playerClassCache.put(uuid, loaded);
                return loaded;
            }
        }

        return null;
    }

    public static void loadPlayerClass(Player player) {
        UUID uuid = player.getUniqueId();
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin != null && plugin.getRunePersistence() != null) {
            PlayerClassType loaded = plugin.getRunePersistence().loadPlayerClass(uuid);
            if (loaded != null) {
                playerClassCache.put(uuid, loaded);
            }
        }
    }

    public static void clearPlayerClass(Player player) {
        UUID uuid = player.getUniqueId();
        playerClassCache.remove(uuid);

        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin != null && plugin.getRunePersistence() != null) {
            plugin.getRunePersistence().savePlayerClass(uuid, null);
        }

        if (plugin != null && plugin.getPlayerEventListener() != null) {
            plugin.getPlayerEventListener().applyPlayerStats(player);
        }
    }

    public static void unloadPlayer(UUID uuid) {
        playerClassCache.remove(uuid);
    }

    private static double stat(Player player, java.util.function.ToDoubleFunction<ClassStats> extractor) {
        PlayerClassType classType = getPlayerClass(player);
        if (classType == null) return 0.0;
        ClassStats stats = CLASS_STATS.get(classType);
        return stats != null ? extractor.applyAsDouble(stats) : 0.0;
    }

    public static double getPlayerClassBaseAD(Player player) {
        return stat(player, ClassStats::baseAD);
    }

    public static double getPlayerClassBaseAP(Player player) {
        return stat(player, ClassStats::baseAP);
    }

    public static double getPlayerClassBaseHP(Player player) {
        return stat(player, ClassStats::baseHP);
    }

    public static double getPlayerClassBaseAR(Player player) {
        return stat(player, ClassStats::baseAR);
    }

    public static double getPlayerClassBaseMR(Player player) {
        return stat(player, ClassStats::baseMR);
    }

    public static double getPlayerClassBaseAS(Player player) {
        return stat(player, ClassStats::baseAS);
    }

    public static double getPlayerClassAttackSpeedRatio(Player player) {
        return stat(player, ClassStats::attackSpeedRatio);
    }
}
