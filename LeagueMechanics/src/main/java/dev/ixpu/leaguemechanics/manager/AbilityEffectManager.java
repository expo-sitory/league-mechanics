package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.util.ActionBarEntry;
import dev.ixpu.leaguemechanics.util.Icons;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityEffectManager {
    private static final double DEFAULT_DURATION_SECONDS = 10.0;
    private static final double DEFAULT_ARMOR_PERCENT = 60.0;
    private static final double DEFAULT_ATTACK_SPEED = 20.0;

    private static AbilityEffectManager instance;

    private final Map<UUID, Map<AbilityEffectType, Long>> expiry = new ConcurrentHashMap<>();

    public static AbilityEffectManager getInstance() {
        if (instance == null) {
            instance = new AbilityEffectManager();
        }
        return instance;
    }

    private static double cfg(AbilityEffectType type, String key, double fallback) {
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return fallback;
        return plugin.getConfig().getDouble("skills." + type.getConfigKey() + "." + key, fallback);
    }

    public double getDurationSeconds(AbilityEffectType type) {
        return cfg(type, "duration-seconds", DEFAULT_DURATION_SECONDS);
    }

    public double getConfiguredArmorPercent() {
        return cfg(AbilityEffectType.SHIELD_OF_DURAND, "armor-percent", DEFAULT_ARMOR_PERCENT);
    }

    public double getConfiguredAttackSpeed() {
        return cfg(AbilityEffectType.FINAL_HOUR, "attack-speed", DEFAULT_ATTACK_SPEED);
    }

    public void activate(Player player, AbilityEffectType type) {
        if (player == null || type == null) return;
        long durationMs = (long) (getDurationSeconds(type) * 1000.0);
        expiry.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(type, System.currentTimeMillis() + durationMs);
    }

    public boolean isActive(Player player, AbilityEffectType type) {
        return getRemainingSeconds(player, type) > 0;
    }

    public double getRemainingSeconds(Player player, AbilityEffectType type) {
        if (player == null) return 0;
        UUID uuid = player.getUniqueId();
        Map<AbilityEffectType, Long> map = expiry.get(uuid);
        if (map == null) return 0;
        Long until = map.get(type);
        if (until == null) return 0;
        long remainingMs = until - System.currentTimeMillis();
        if (remainingMs <= 0) {
            map.remove(type);
            expiry.computeIfPresent(uuid, (k, v) -> v.isEmpty() ? null : v);
            return 0;
        }
        return remainingMs / 1000.0;
    }

    public void clear(Player player) {
        if (player == null) return;
        expiry.remove(player.getUniqueId());
    }

    public void purge(UUID uuid) {
        expiry.remove(uuid);
    }

    public double getArmorMultiplier(Player player) {
        if (!isActive(player, AbilityEffectType.SHIELD_OF_DURAND)) return 1.0;
        return 1.0 + getConfiguredArmorPercent() / 100.0;
    }

    public double getAttackSpeedBonus(Player player) {
        if (!isActive(player, AbilityEffectType.FINAL_HOUR)) return 0.0;
        return getConfiguredAttackSpeed();
    }

    public List<ActionBarEntry> getActionBarEntries(Player player) {
        List<ActionBarEntry> entries = new ArrayList<>();
        if (player == null) return entries;
        for (AbilityEffectType type : AbilityEffectType.values()) {
            double seconds = getRemainingSeconds(player, type);
            if (seconds <= 0) continue;
            String icon = Icons.get(type.getIconKey());
            String label = icon.isEmpty() ? type.getDisplayName() : icon;
            entries.add(new ActionBarEntry("ability:" + type.name(),
                    "§f" + label + " " + type.getTimerColor() + "(" + String.format("%.1f", seconds) + "s)§f"));
        }
        return entries;
    }
}