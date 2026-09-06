package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.rune.DebuffType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DebuffManager {
    private static DebuffManager instance;

    private final Map<UUID, Map<DebuffType, Long>> playerDebuffs = new HashMap<>();
    private final Map<UUID, Map<DebuffType, Long>> debuffDurationsMs = new HashMap<>();
    private final Map<UUID, Map<DebuffType, Double>> debuffStrengths = new HashMap<>();

    public static DebuffManager getInstance() {
        if (instance == null) {
            instance = new DebuffManager();
        }
        return instance;
    }

    public void applyDebuff(Player target, DebuffType type, int durationTicks) {
        applyDebuff(target, type, durationTicks, 0.0);
    }

    public void applyDebuff(Player target, DebuffType type, int durationTicks, double strength) {
        if (target == null) return;
        long durationMs = durationTicks * 50L;
        long expiry = System.currentTimeMillis() + durationMs;
        UUID uuid = target.getUniqueId();
        playerDebuffs.computeIfAbsent(uuid, k -> new HashMap<>()).put(type, expiry);
        debuffDurationsMs.computeIfAbsent(uuid, k -> new HashMap<>()).put(type, durationMs);
        debuffStrengths.computeIfAbsent(uuid, k -> new HashMap<>()).put(type, Math.max(0.0, strength));
    }


    private double getTenacityPercent(Player target, DebuffType type) {
        if (target == null || type != DebuffType.SLOW) return 0;
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return 0;
        ItemStatsManager statsManager = plugin.getStatsManager();
        if (statsManager == null) return 0;
        return Math.max(0.0, statsManager.getItemTN(target));
    }

    private long getEffectiveExpiry(Player target, DebuffType type, long rawExpiry) {
        double tn = getTenacityPercent(target, type);
        if (tn <= 0) return rawExpiry;
        Map<DebuffType, Long> durations = debuffDurationsMs.get(target.getUniqueId());
        if (durations == null) return rawExpiry;
        Long durationMs = durations.get(type);
        if (durationMs == null || durationMs <= 0) return rawExpiry;
        long reduction = (long) (durationMs * (tn / 100.0));
        return rawExpiry - reduction;
    }

    public boolean hasDebuff(Player target, DebuffType type) {
        if (target == null) return false;
        Map<DebuffType, Long> debuffs = playerDebuffs.get(target.getUniqueId());
        if (debuffs == null) return false;
        Long expiry = debuffs.get(type);
        if (expiry == null) return false;
        long effectiveExpiry = getEffectiveExpiry(target, type, expiry);
        if (System.currentTimeMillis() >= effectiveExpiry) {
            debuffs.remove(type);
            clearAuxiliaryMaps(target.getUniqueId(), type);
            return false;
        }
        return true;
    }

    public double getRemainingSeconds(Player target, DebuffType type) {
        if (target == null) return 0;
        Map<DebuffType, Long> debuffs = playerDebuffs.get(target.getUniqueId());
        if (debuffs == null) return 0;
        Long expiry = debuffs.get(type);
        if (expiry == null) return 0;
        long effectiveExpiry = getEffectiveExpiry(target, type, expiry);
        long remainingMs = effectiveExpiry - System.currentTimeMillis();
        if (remainingMs <= 0) {
            debuffs.remove(type);
            clearAuxiliaryMaps(target.getUniqueId(), type);
            return 0;
        }
        return remainingMs / 1000.0;
    }

    public double getDebuffStrength(Player target, DebuffType type) {
        if (target == null) return 0;
        Map<DebuffType, Double> strengths = debuffStrengths.get(target.getUniqueId());
        if (strengths == null) return 0;
        Double value = strengths.get(type);
        return value != null ? value : 0.0;
    }

    private void clearAuxiliaryMaps(UUID uuid, DebuffType type) {
        Map<DebuffType, Long> durations = debuffDurationsMs.get(uuid);
        if (durations != null) durations.remove(type);
        Map<DebuffType, Double> strengths = debuffStrengths.get(uuid);
        if (strengths != null) strengths.remove(type);
    }

    public void clearDebuffs(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        playerDebuffs.remove(uuid);
        debuffDurationsMs.remove(uuid);
        debuffStrengths.remove(uuid);
    }

    public void tickAll() {
        long now = System.currentTimeMillis();
        for (UUID uuid : new java.util.ArrayList<>(playerDebuffs.keySet())) {
            Map<DebuffType, Long> map = playerDebuffs.get(uuid);
            if (map == null) continue;
            map.entrySet().removeIf(e -> now >= e.getValue());
            Map<DebuffType, Long> durations = debuffDurationsMs.get(uuid);
            if (durations != null) {
                durations.keySet().retainAll(map.keySet());
                if (durations.isEmpty()) debuffDurationsMs.remove(uuid);
            }
            Map<DebuffType, Double> strengths = debuffStrengths.get(uuid);
            if (strengths != null) {
                strengths.keySet().retainAll(map.keySet());
                if (strengths.isEmpty()) debuffStrengths.remove(uuid);
            }
            if (map.isEmpty()) playerDebuffs.remove(uuid);
        }
    }
}
