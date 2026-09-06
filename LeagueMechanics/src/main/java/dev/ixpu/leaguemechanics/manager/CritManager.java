package dev.ixpu.leaguemechanics.manager;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CritManager {

    private static final int MAX_STREAK = 5;

    private static final double[] LUCK_MODIFIER_TABLE = buildLuckModifierTable();

    private final Map<UUID, Integer> critStreaks = new ConcurrentHashMap<>();

    private static final CritManager INSTANCE = new CritManager();

    public static CritManager getInstance() {
        return INSTANCE;
    }

    private static double[] buildLuckModifierTable() {
        double[] table = new double[201];
        for (int i = 0; i <= 200; i++) {
            double critChance = i / 2.0;
            table[i] = computeLuckModifier(critChance);
        }
        return table;
    }

    private static double computeLuckModifier(double critChance) {
        if (critChance <= 0) return 0;
        if (critChance >= 100) return 1;

        double C = critChance / 100.0;
        double a = C * (1 - C) * 2.0;
        return Math.clamp(a, 0, 1);
    }

    public boolean rollCrit(Player player, double critChancePercent) {
        UUID uuid = player.getUniqueId();

        if (critChancePercent <= 0) {
            critStreaks.put(uuid, 0);
            return false;
        }
        if (critChancePercent >= 99.9) {
            critStreaks.put(uuid, 0);
            return true;
        }

        int critStreak = critStreaks.getOrDefault(uuid, 0);
        int cappedStreak = Math.min(critStreak, MAX_STREAK);

        double cappedCritChance = Math.min(critChancePercent, 100.0);
        double bonusChance = (MAX_STREAK - cappedStreak) * 0.5; 
        double effectiveCritChance = Math.min(100.0, cappedCritChance + bonusChance);

        boolean isCrit = Math.random() * 100 < effectiveCritChance;

        if (isCrit) {
            critStreaks.put(uuid, critStreak + 1);
        } else {
            critStreaks.put(uuid, 0);
        }

        return isCrit;
    }

    public int getCritStreak(Player player) {
        return critStreaks.getOrDefault(player.getUniqueId(), 0);
    }

    public int getFailureStreak(Player player) {
        return getCritStreak(player);
    }

    public void resetFailureStreak(Player player) {
        critStreaks.remove(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        critStreaks.remove(player.getUniqueId());
    }

    public double getLuckModifierForPlayer(Player player, double critChancePercent) {
        int tableIndex = Math.clamp((int) Math.floor(critChancePercent * 2), 0, 200);
        return LUCK_MODIFIER_TABLE[tableIndex];
    }

    public static double getAverageCritMultiplier(double critChancePercent, double bonusCritDamage) {
        double critChance = critChancePercent / 100.0;
        return 1.0 + (critChance * (1.0 + bonusCritDamage));
    }
}
