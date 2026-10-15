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
    private static final double DEFAULT_ARMOR_PERCENT = 60.0;
    private static final double DEFAULT_ATTACK_SPEED = 20.0;
    private static final double DEFAULT_RAGNAROK_ATTACK_SPEED = 30.0;
    private static final double DEFAULT_RAGNAROK_MOVEMENT_SPEED = 30.0;
    private static final double DEFAULT_SANGUINE_POOL_MOVEMENT_SPEED = 30.0;
    private static final double DEFAULT_INSPIRE_MOVEMENT_SPEED = 30.0;
    private static final double DEFAULT_INSPIRE_ATTACK_SPEED = 80.0;
    private static final double DEFAULT_BATTLE_ROAR_MOVEMENT_SPEED = 50.0;
    private static final double DEFAULT_MEDITATE_DAMAGE_REDUCTION = 90.0;

    public record ActiveStatus(String name, String iconKey, double remainingSeconds) {}

    private record StatusDefinition(String name, String iconKey, List<AbilityEffectType> effects) {}

    private static final List<StatusDefinition> STATUSES = List.of(
            new StatusDefinition("INVULNERABLE", "status.invulnerable", List.of(AbilityEffectType.INVULNERABLE)),
            new StatusDefinition("UNTARGETABLE", "status.untargetable", List.of(AbilityEffectType.SANGUINE_POOL)),
            new StatusDefinition("UNKILLABLE", "status.unkillable", List.of(AbilityEffectType.UNDYING_RAGE)),
            new StatusDefinition("UNSTOPPABLE", "status.cc-immune", List.of(AbilityEffectType.RAGNAROK))
    );

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
        return cfg(type, "duration-seconds", type.getDefaultDurationSeconds());
    }

    public double getConfiguredArmorPercent() {
        return cfg(AbilityEffectType.SHIELD_OF_DURAND, "armor-percent", DEFAULT_ARMOR_PERCENT);
    }

    public double getConfiguredAttackSpeed() {
        return cfg(AbilityEffectType.FINAL_HOUR, "attack-speed", DEFAULT_ATTACK_SPEED);
    }

    public double getConfiguredRagnarokAttackSpeed() {
        return cfg(AbilityEffectType.RAGNAROK, "attack-speed", DEFAULT_RAGNAROK_ATTACK_SPEED);
    }

    public double getConfiguredRagnarokMovementSpeed() {
        return cfg(AbilityEffectType.RAGNAROK, "movement-speed", DEFAULT_RAGNAROK_MOVEMENT_SPEED);
    }

    public double getConfiguredSanguinePoolMovementSpeed() {
        return cfg(AbilityEffectType.SANGUINE_POOL, "movement-speed", DEFAULT_SANGUINE_POOL_MOVEMENT_SPEED);
    }

    public double getConfiguredBattleRoarMovementSpeed() {
        return cfg(AbilityEffectType.BATTLE_ROAR, "movement-speed", DEFAULT_BATTLE_ROAR_MOVEMENT_SPEED);
    }

    public double getConfiguredMeditateDamageReduction() {
        return cfg(AbilityEffectType.MEDITATE, "damage-reduction-percent", DEFAULT_MEDITATE_DAMAGE_REDUCTION);
    }

    public double getConfiguredInspireMovementSpeed() {
        return cfg(AbilityEffectType.INSPIRE, "movement-speed", DEFAULT_INSPIRE_MOVEMENT_SPEED);
    }

    public double getConfiguredInspireAttackSpeed() {
        return cfg(AbilityEffectType.INSPIRE, "attack-speed", DEFAULT_INSPIRE_ATTACK_SPEED);
    }

    public void activate(Player player, AbilityEffectType type) {
        if (player == null || type == null) return;
        long durationMs = (long) (getDurationSeconds(type) * 1000.0);
        expiry.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(type, System.currentTimeMillis() + durationMs);
    }

    public void activate(Player player, AbilityEffectType type, double seconds) {
        if (player == null || type == null || seconds <= 0) return;
        expiry.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(type, System.currentTimeMillis() + (long) (seconds * 1000.0));
    }

    public void end(Player player, AbilityEffectType type) {
        if (player == null || type == null) return;
        UUID uuid = player.getUniqueId();
        Map<AbilityEffectType, Long> map = expiry.get(uuid);
        if (map == null) return;
        map.remove(type);
        expiry.computeIfPresent(uuid, (k, v) -> v.isEmpty() ? null : v);
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
        double bonus = 0.0;
        if (isActive(player, AbilityEffectType.FINAL_HOUR)) bonus += getConfiguredAttackSpeed();
        if (isActive(player, AbilityEffectType.RAGNAROK)) bonus += getConfiguredRagnarokAttackSpeed();
        if (isActive(player, AbilityEffectType.INSPIRE)) bonus += getConfiguredInspireAttackSpeed();
        return bonus;
    }

    public double getMovementSpeedBonus(Player player) {
        double bonus = 0.0;
        if (isActive(player, AbilityEffectType.RAGNAROK)) bonus += getConfiguredRagnarokMovementSpeed();
        if (isActive(player, AbilityEffectType.SANGUINE_POOL)) bonus += getConfiguredSanguinePoolMovementSpeed();
        if (isActive(player, AbilityEffectType.INSPIRE)) bonus += getConfiguredInspireMovementSpeed();
        if (isActive(player, AbilityEffectType.BATTLE_ROAR)) bonus += getConfiguredBattleRoarMovementSpeed();
        return bonus;
    }

    public ActiveStatus getActiveStatus(Player player) {
        for (StatusDefinition status : STATUSES) {
            double remaining = 0;
            for (AbilityEffectType type : status.effects()) {
                remaining = Math.max(remaining, getRemainingSeconds(player, type));
            }
            if (remaining > 0) {
                return new ActiveStatus(status.name(), status.iconKey(), remaining);
            }
        }
        return null;
    }

    public boolean isArmorDurabilityProtected(Player player) {
        return isActive(player, AbilityEffectType.INSPIRE);
    }

    public boolean isInvulnerable(Player player) {
        return isActive(player, AbilityEffectType.INVULNERABLE);
    }

    public boolean isMeditating(Player player) {
        return isActive(player, AbilityEffectType.MEDITATE);
    }

    public boolean isRooted(Player player) {
        return isMeditating(player);
    }

    public boolean isKnockbackImmune(Player player) {
        return isActive(player, AbilityEffectType.RAGNAROK);
    }

    public boolean isCrowdControlImmune(Player player) {
        return isActive(player, AbilityEffectType.RAGNAROK);
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