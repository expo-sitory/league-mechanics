package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.debuff.DebuffType;
import dev.ixpu.leaguemechanics.util.ActionBarEntry;
import dev.ixpu.leaguemechanics.util.Icons;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DebuffManager {
    private static final double SLOW_PERCENT_PER_LEVEL = 20.0;

    private static final EnumSet<DebuffType> CROWD_CONTROL = EnumSet.of(
            DebuffType.STUN, DebuffType.GROUNDED, DebuffType.NEARSIGHTED, DebuffType.SLOW);

    private static DebuffManager instance;

    private final Map<UUID, Map<DebuffType, Long>> playerDebuffs = new HashMap<>();
    private final Map<UUID, Map<DebuffType, Long>> debuffDurationsMs = new HashMap<>();
    private final Map<UUID, Map<DebuffType, Double>> debuffStrengths = new HashMap<>();
    private final Map<UUID, DebuffType> latestCrowdControl = new HashMap<>();

    public static boolean isCrowdControl(DebuffType type) {
        return CROWD_CONTROL.contains(type);
    }

    public static DebuffManager getInstance() {
        if (instance == null) {
            instance = new DebuffManager();
        }
        return instance;
    }

    public void applyDebuff(Player target, DebuffType type, int durationTicks) {
        applyDebuff(target, type, durationTicks, 0.0);
    }

    public void applyDebuff(Player target, DebuffType type, int durationTicks, Player source) {
        applyDebuff(target, type, durationTicks, 0.0, source);
    }

    /**
     * Applies a debuff and, when it is crowd control applied by another player, notifies that
     * player's runes (see {@link dev.ixpu.leaguemechanics.rune.CooldownHandler#onCrowdControl}).
     */
    public void applyDebuff(Player target, DebuffType type, int durationTicks, double strength, Player source) {
        applyDebuff(target, type, durationTicks, strength);
        if (target == null || source == null || source.equals(target) || !isCrowdControl(type)) return;

        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null || plugin.getRuneManager() == null) return;
        var runeData = plugin.getRuneManager().getPlayerRuneData(source);
        if (runeData == null) return;
        for (var rune : runeData.getAllRunes()) {
            if (rune != null) {
                rune.onCrowdControl(source, target, type);
            }
        }
    }

    public void applyDebuff(Player target, DebuffType type, int durationTicks, double strength) {
        if (target == null) return;
        long durationMs = durationTicks * 50L;
        long expiry = System.currentTimeMillis() + durationMs;
        UUID uuid = target.getUniqueId();
        playerDebuffs.computeIfAbsent(uuid, k -> new HashMap<>()).put(type, expiry);
        debuffDurationsMs.computeIfAbsent(uuid, k -> new HashMap<>()).put(type, durationMs);
        debuffStrengths.computeIfAbsent(uuid, k -> new HashMap<>()).put(type, Math.max(0.0, strength));
        if (isCrowdControl(type)) {
            latestCrowdControl.put(uuid, type);
        }

        if (type == DebuffType.SLOW && strength > 0) {
            double tenacity = Math.min(100.0, getTenacityPercent(target, type));
            int effectiveTicks = (int) (durationTicks * (1.0 - tenacity / 100.0));
            int amplifier = Math.max(1, (int) Math.round(strength / SLOW_PERCENT_PER_LEVEL)) - 1;
            if (effectiveTicks > 0) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, effectiveTicks, amplifier, false, false));
            }
        }

        if (type == DebuffType.NEARSIGHTED) {
            double tenacity = Math.min(100.0, getTenacityPercent(target, type));
            int effectiveTicks = (int) (durationTicks * (1.0 - tenacity / 100.0));
            if (effectiveTicks > 0) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, effectiveTicks, 0, false, false));
            }
        }
    }


    private double getTenacityPercent(Player target, DebuffType type) {
        if (target == null || !isCrowdControl(type)) return 0;
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return 0;
        ItemStatsManager statsManager = plugin.getStatsManager();
        if (statsManager == null) return 0;
        return Math.max(0.0, statsManager.getItemTN(target) + BuffManager.getInstance().getBonus(target, BuffManager.Stat.TN));
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

    public DebuffType getLatestCrowdControl(Player player) {
        if (player == null) return null;
        DebuffType type = latestCrowdControl.get(player.getUniqueId());
        if (type == null) return null;
        return hasDebuff(player, type) ? type : null;
    }

    public static String getCrowdControlName(DebuffType type) {
        return switch (type) {
            case STUN -> "STUNNED";
            case GROUNDED -> "GROUNDED";
            case NEARSIGHTED -> "NEARSIGHTED";
            case SLOW -> "SLOWED";
            default -> type.name();
        };
    }

    public static String getCrowdControlIconKey(DebuffType type) {
        return switch (type) {
            case STUN -> "status.stunned";
            case GROUNDED -> "status.grounded";
            case NEARSIGHTED -> "status.nearsighted";
            case SLOW -> "status.slowed";
            default -> "";
        };
    }

    public List<ActionBarEntry> getActionBarEntries(Player player) {
        List<ActionBarEntry> entries = new ArrayList<>();
        if (player == null) return entries;
        if (hasDebuff(player, DebuffType.GRIEVOUS_WOUNDS)) {
            entries.add(status("debuff:grievous_wounds", Icons.get("status.grievous-wounds"), "§4", getRemainingSeconds(player, DebuffType.GRIEVOUS_WOUNDS)));
        }
        if (hasDebuff(player, DebuffType.INFLAME)) {
            entries.add(status("debuff:inflame", Icons.get("status.burned"), "§6", getRemainingSeconds(player, DebuffType.INFLAME)));
        }
        if (hasDebuff(player, DebuffType.BLIND)) {
            entries.add(status("debuff:blind", Icons.get("status.blind"), "§7", getRemainingSeconds(player, DebuffType.BLIND)));
        }
        if (hasDebuff(player, DebuffType.GROUNDED)) {
            entries.add(status("debuff:grounded", Icons.get("status.grounded"), "§e", getRemainingSeconds(player, DebuffType.GROUNDED)));
        }
        if (hasDebuff(player, DebuffType.STUN)) {
            entries.add(status("debuff:stun", Icons.get("status.stunned"), "§d", getRemainingSeconds(player, DebuffType.STUN)));
        }
        PotionEffect darkness = player.getPotionEffect(PotionEffectType.DARKNESS);
        if (darkness != null && darkness.getDuration() > 0) {
            entries.add(status("debuff:nearsighted", Icons.get("status.nearsighted"), "§8", darkness.getDuration() / 20.0));
        }
        PotionEffect slow = player.getPotionEffect(PotionEffectType.SLOWNESS);
        if (slow != null && slow.getDuration() > 0) {
            entries.add(status("debuff:slow", Icons.get("status.slowed"), "§b", slow.getDuration() / 20.0));
        }
        return entries;
    }

    private ActionBarEntry status(String key, String icon, String color, double remainingSeconds) {
        return new ActionBarEntry(key, "§f" + icon + " " + color + "(" + String.format("%.1f", remainingSeconds) + "s)§f");
    }

    public void clearDebuffs(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        playerDebuffs.remove(uuid);
        debuffDurationsMs.remove(uuid);
        debuffStrengths.remove(uuid);
        latestCrowdControl.remove(uuid);
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