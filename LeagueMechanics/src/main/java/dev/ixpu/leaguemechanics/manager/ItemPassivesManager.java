package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemPassivesManager {
    private static ItemPassivesManager instance;

    private final Map<String, Map<UUID, Integer>> killCounts = new HashMap<>();
    private final Map<String, Map<UUID, Boolean>> passiveDisabled = new HashMap<>();
    private final Map<String, Map<UUID, Long>> passiveCooldowns = new HashMap<>();
    private final Map<String, Long> passiveDurationsMs = new HashMap<>();

    public static ItemPassivesManager getInstance() {
        if (instance == null) {
            instance = new ItemPassivesManager();
        }
        return instance;
    }

    public int getKillCount(Player player, String itemId) {
        UUID uuid = player.getUniqueId();
        return killCounts.computeIfAbsent(itemId, k -> new HashMap<>()).getOrDefault(uuid, 0);
    }

    public void addKill(Player player, String itemId) {
        UUID uuid = player.getUniqueId();
        killCounts.computeIfAbsent(itemId, k -> new HashMap<>()).put(uuid, getKillCount(player, itemId) + 1);
    }

    public boolean isPassiveDisabled(Player player, String itemId) {
        UUID uuid = player.getUniqueId();
        return passiveDisabled.computeIfAbsent(itemId, k -> new HashMap<>()).getOrDefault(uuid, false);
    }

    public void disablePassive(Player player, String itemId) {
        UUID uuid = player.getUniqueId();
        passiveDisabled.computeIfAbsent(itemId, k -> new HashMap<>()).put(uuid, true);
    }

    public void setKillCount(Player player, String itemId, int count) {
        UUID uuid = player.getUniqueId();
        killCounts.computeIfAbsent(itemId, k -> new HashMap<>()).put(uuid, count);
    }

    private double getCooldownHastePercent(Player player) {
        if (player == null) return 0;
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return 0;
        ItemStatsManager statsManager = plugin.getStatsManager();
        if (statsManager == null) return 0;
        return Math.max(0.0, statsManager.getItemCH(player) + BuffManager.getInstance().getBonus(player, BuffManager.Stat.CH));
    }

    public boolean isOnCooldown(Player player, String passiveId) {
        if (player == null) return false;
        Map<UUID, Long> cd = passiveCooldowns.get(passiveId);
        if (cd == null) return false;
        Long expiry = cd.get(player.getUniqueId());
        if (expiry == null) return false;
        double ch = getCooldownHastePercent(player);
        long effectiveExpiry = expiry;
        if (ch > 0) {
            long durationMs = passiveDurationsMs.getOrDefault(passiveId, expiry - System.currentTimeMillis());
            if (durationMs > 0) {
                long reduction = (long) (durationMs * (ch / 100.0));
                effectiveExpiry = expiry - reduction;
            }
        }
        if (System.currentTimeMillis() >= effectiveExpiry) {
            cd.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void setCooldown(Player player, String passiveId, int durationTicks) {
        if (player == null) return;
        long durationMs = durationTicks * 50L;
        long expiry = System.currentTimeMillis() + durationMs;
        passiveDurationsMs.put(passiveId, durationMs);
        passiveCooldowns
                .computeIfAbsent(passiveId, k -> new HashMap<>())
                .put(player.getUniqueId(), expiry);
    }

    public double getRemainingCooldownSeconds(Player player, String passiveId) {
        if (player == null) return 0;
        Map<UUID, Long> cd = passiveCooldowns.get(passiveId);
        if (cd == null) return 0;
        Long expiry = cd.get(player.getUniqueId());
        if (expiry == null) return 0;
        double ch = getCooldownHastePercent(player);
        long effectiveExpiry = expiry;
        if (ch > 0) {
            long durationMs = passiveDurationsMs.getOrDefault(passiveId, 0L);
            if (durationMs > 0) {
                long reduction = (long) (durationMs * (ch / 100.0));
                effectiveExpiry = expiry - reduction;
            }
        }
        long remainingMs = effectiveExpiry - System.currentTimeMillis();
        if (remainingMs <= 0) {
            cd.remove(player.getUniqueId());
            return 0;
        }
        return remainingMs / 1000.0;
    }
}