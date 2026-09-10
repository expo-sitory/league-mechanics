package dev.ixpu.leaguemechanics.manager;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CombatStateManager {

    private static CombatStateManager instance;

    private final Map<UUID, Map<UUID, Long>> lastHitTimes = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastPlayerAttacker = new ConcurrentHashMap<>();
    private final Map<UUID, LivingEntity> lastMobDamager = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastKillTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> killStreak = new ConcurrentHashMap<>();
    private final Set<UUID> processedDeaths = ConcurrentHashMap.newKeySet();
    private final Set<UUID> letRunesThroughMap = ConcurrentHashMap.newKeySet();

    private CombatStateManager() {}

    public static CombatStateManager getInstance() {
        if (instance == null) {
            instance = new CombatStateManager();
        }
        return instance;
    }

    public void recordHit(Player victim, Player attacker) {
        if (attacker == null || victim == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        lastHitTimes
                .computeIfAbsent(victim.getUniqueId(), k -> new HashMap<>())
                .put(attacker.getUniqueId(), System.currentTimeMillis());
        lastPlayerAttacker.put(victim.getUniqueId(), attacker.getUniqueId());
    }

    public Map<UUID, Long> getLastHitTimes(UUID victimId) {
        return lastHitTimes.remove(victimId);
    }

    public UUID getLastPlayerAttacker(UUID victimId) {
        return lastPlayerAttacker.remove(victimId);
    }

    public void setLastMobDamager(Player player, LivingEntity mob) {
        lastMobDamager.put(player.getUniqueId(), mob);
    }

    public LivingEntity getAndRemoveLastMobDamager(UUID playerId) {
        return lastMobDamager.remove(playerId);
    }

    public void clearLastMobDamager(UUID playerId) {
        lastMobDamager.remove(playerId);
    }

    public int getKillStreak(UUID playerId) {
        return killStreak.getOrDefault(playerId, 0);
    }

    public void setKillStreak(UUID playerId, int streak) {
        killStreak.put(playerId, streak);
    }

    public void removeKillStreak(UUID playerId) {
        killStreak.remove(playerId);
    }

    public Long getLastKillTime(UUID playerId) {
        return lastKillTime.get(playerId);
    }

    public void setLastKillTime(UUID playerId, long time) {
        lastKillTime.put(playerId, time);
    }

    public void removeLastKillTime(UUID playerId) {
        lastKillTime.remove(playerId);
    }

    public boolean isDeathProcessed(UUID playerId) {
        return processedDeaths.contains(playerId);
    }

    public boolean addProcessedDeath(UUID playerId) {
        return processedDeaths.add(playerId);
    }

    public void clearProcessedDeath(UUID playerId) {
        processedDeaths.remove(playerId);
    }

    public void addLetRunesThrough(UUID playerId) {
        letRunesThroughMap.add(playerId);
    }

    public void removeLetRunesThrough(UUID playerId) {
        letRunesThroughMap.remove(playerId);
    }

    public boolean hasLetRunesThrough(UUID playerId) {
        return letRunesThroughMap.contains(playerId);
    }

    public void clearPlayer(UUID playerId) {
        lastHitTimes.remove(playerId);
        lastPlayerAttacker.remove(playerId);
        lastMobDamager.remove(playerId);
        lastKillTime.remove(playerId);
        killStreak.remove(playerId);
        processedDeaths.remove(playerId);
        letRunesThroughMap.remove(playerId);
    }

    public void clearAll() {
        lastHitTimes.clear();
        lastPlayerAttacker.clear();
        lastMobDamager.clear();
        lastKillTime.clear();
        killStreak.clear();
        processedDeaths.clear();
        letRunesThroughMap.clear();
    }
}
