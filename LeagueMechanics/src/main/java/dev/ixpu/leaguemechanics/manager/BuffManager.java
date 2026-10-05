package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.buff.BuffType;
import dev.ixpu.leaguemechanics.debuff.DebuffType;
import dev.ixpu.leaguemechanics.util.Icons;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BuffManager {
    public enum Stat {
        AD(true), AP(true), AR(true), MR(true),
        CH(false), TN(false), SR(false), HR(false), AS(false), MS(false);

        private final boolean percent;

        Stat(boolean percent) {
            this.percent = percent;
        }

        public boolean isPercent() {
            return percent;
        }

        public String configKey() {
            return name().toLowerCase() + (percent ? "-percent" : "");
        }
    }

    private static final double DEFAULT_DURATION_SECONDS = 120.0;

    private static final Map<BuffType, String> DEFAULT_NAMES = new EnumMap<>(BuffType.class);
    private static final Map<BuffType, String> TIMER_COLORS = new EnumMap<>(BuffType.class);

    static {
        register(BuffType.CREST_OF_INSIGHT, "Crest of Insight", "§9");
        register(BuffType.CREST_OF_CINDERS, "Crest of Cinders", "§c");
        register(BuffType.CHEMTECH_BLIGHT, "Chemtech Blight", "§a");
        register(BuffType.CLOUDBRINGERS_GRACE, "Cloudbringer's Grace", "§7");
        register(BuffType.HEXTECH_PROWESS, "Hextech Prowess", "§b");
        register(BuffType.INFERNAL_MIGHT, "Infernal Might", "§6");
        register(BuffType.MOUNTAINOUS_VIGOR, "Mountainous Vigor", "§e");
        register(BuffType.OCEANIC_WILL, "Oceanic Will", "§3");
    }

    private static void register(BuffType type, String displayName, String timerColor) {
        DEFAULT_NAMES.put(type, displayName);
        TIMER_COLORS.put(type, timerColor);
    }

    private static BuffManager instance;

    private final Map<UUID, Map<BuffType, Long>> expiry = new ConcurrentHashMap<>();
    private final Map<BuffType, BuffConfig> configs = new EnumMap<>(BuffType.class);

    private static final class BuffConfig {
        private String displayName;
        private long durationMs;
        private final Map<Stat, Double> values = new EnumMap<>(Stat.class);
        private double healMultiplier = 1.0;
        private double slowStrength;
        private int slowTicks;
        private int inflameTicks;
    }

    public static String getId(BuffType type) {
        return type.name().toLowerCase();
    }

    public static BuffType fromId(String input) {
        if (input == null) return null;
        String normalized = input.toUpperCase().replace('-', '_');
        try {
            return BuffType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String getTimerColor(BuffType type) {
        return TIMER_COLORS.getOrDefault(type, "§f");
    }

    public static BuffManager getInstance() {
        if (instance == null) {
            instance = new BuffManager();
        }
        return instance;
    }

    public void reload(FileConfiguration config) {
        configs.clear();
        for (BuffType type : BuffType.values()) {
            BuffConfig buff = new BuffConfig();
            buff.displayName = DEFAULT_NAMES.get(type);
            buff.durationMs = (long) (DEFAULT_DURATION_SECONDS * 1000);

            ConfigurationSection section = config.getConfigurationSection("buffs." + getId(type));
            if (section != null) {
                buff.displayName = section.getString("display-name", buff.displayName);
                buff.durationMs = (long) (section.getDouble("duration-seconds", DEFAULT_DURATION_SECONDS) * 1000);
                for (Stat stat : Stat.values()) {
                    double value = section.getDouble(stat.configKey(), 0.0);
                    if (value != 0.0) buff.values.put(stat, value);
                }
                buff.healMultiplier = section.getDouble("heal-multiplier", 1.0);
                buff.slowStrength = section.getDouble("slow-strength", 0.0);
                buff.slowTicks = section.getInt("slow-duration-ticks", 0);
                buff.inflameTicks = section.getInt("inflame-duration-ticks", 0);
            }
            configs.put(type, buff);
        }
    }

    public void giveBuff(Player player, BuffType type) {
        if (player == null || type == null) return;
        BuffConfig buff = configs.get(type);
        long durationMs = buff != null ? buff.durationMs : (long) (DEFAULT_DURATION_SECONDS * 1000);
        expiry.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(type, System.currentTimeMillis() + durationMs);
        refreshStats(player);
    }

    public void removeBuff(Player player, BuffType type) {
        if (player == null || type == null) return;
        Map<BuffType, Long> map = expiry.get(player.getUniqueId());
        if (map == null || map.remove(type) == null) return;
        if (map.isEmpty()) expiry.remove(player.getUniqueId());
        refreshStats(player);
    }

    public void clearBuffs(Player player) {
        if (player == null) return;
        Map<BuffType, Long> removed = expiry.remove(player.getUniqueId());
        if (removed != null && !removed.isEmpty()) refreshStats(player);
    }

    public void purge(UUID uuid) {
        expiry.remove(uuid);
    }

    public boolean hasBuff(Player player, BuffType type) {
        return getRemainingSeconds(player, type) > 0;
    }

    public double getRemainingSeconds(Player player, BuffType type) {
        if (player == null) return 0;
        Map<BuffType, Long> map = expiry.get(player.getUniqueId());
        if (map == null) return 0;
        Long until = map.get(type);
        if (until == null) return 0;
        long remainingMs = until - System.currentTimeMillis();
        return remainingMs > 0 ? remainingMs / 1000.0 : 0;
    }

    public String getDisplayName(BuffType type) {
        BuffConfig buff = configs.get(type);
        return buff != null ? buff.displayName : DEFAULT_NAMES.get(type);
    }

    public double getDurationSeconds(BuffType type) {
        BuffConfig buff = configs.get(type);
        return buff != null ? buff.durationMs / 1000.0 : DEFAULT_DURATION_SECONDS;
    }

    public double getBonus(Player player, Stat stat) {
        if (player == null || stat.isPercent()) return 0;
        Map<BuffType, Long> map = expiry.get(player.getUniqueId());
        if (map == null) return 0;
        long now = System.currentTimeMillis();
        double total = 0;
        for (Map.Entry<BuffType, Long> entry : map.entrySet()) {
            if (now >= entry.getValue()) continue;
            BuffConfig buff = configs.get(entry.getKey());
            if (buff == null) continue;
            total += buff.values.getOrDefault(stat, 0.0);
        }
        return total;
    }

    public double getMultiplier(Player player, Stat stat) {
        if (player == null || !stat.isPercent()) return 1.0;
        Map<BuffType, Long> map = expiry.get(player.getUniqueId());
        if (map == null) return 1.0;
        long now = System.currentTimeMillis();
        double product = 1.0;
        for (Map.Entry<BuffType, Long> entry : map.entrySet()) {
            if (now >= entry.getValue()) continue;
            BuffConfig buff = configs.get(entry.getKey());
            if (buff == null) continue;
            product *= 1.0 + buff.values.getOrDefault(stat, 0.0) / 100.0;
        }
        return product;
    }

    public double getHealingMultiplier(Player player) {
        if (player == null) return 1.0;
        Map<BuffType, Long> map = expiry.get(player.getUniqueId());
        if (map == null) return 1.0;
        long now = System.currentTimeMillis();
        double product = 1.0;
        for (Map.Entry<BuffType, Long> entry : map.entrySet()) {
            if (now >= entry.getValue()) continue;
            BuffConfig buff = configs.get(entry.getKey());
            if (buff == null) continue;
            product *= buff.healMultiplier;
        }
        return product;
    }

    public String getActionBarSection(Player player) {
        if (player == null) return "";
        Map<BuffType, Long> map = expiry.get(player.getUniqueId());
        if (map == null || map.isEmpty()) return "";
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (BuffType type : BuffType.values()) {
            Long until = map.get(type);
            if (until == null || now >= until) continue;
            String icon = Icons.get("status." + getId(type).replace('_', '-'));
            String label = icon.isEmpty() ? getDisplayName(type) : icon;
            double seconds = (until - now) / 1000.0;
            if (sb.length() > 0) sb.append(" ");
            sb.append("§f").append(label).append(" ").append(getTimerColor(type)).append("(").append(String.format("%.1f", seconds)).append("s)§f");
        }
        return sb.toString();
    }

    public void onHit(Player attacker, Player target) {
        if (attacker == null || target == null) return;
        Map<BuffType, Long> map = expiry.get(attacker.getUniqueId());
        if (map == null) return;
        long now = System.currentTimeMillis();
        DebuffManager debuffs = DebuffManager.getInstance();
        for (Map.Entry<BuffType, Long> entry : map.entrySet()) {
            if (now >= entry.getValue()) continue;
            BuffConfig buff = configs.get(entry.getKey());
            if (buff == null) continue;
            if (buff.slowStrength > 0 && buff.slowTicks > 0) {
                debuffs.applyDebuff(target, DebuffType.SLOW, buff.slowTicks, buff.slowStrength);
            }
            if (buff.inflameTicks > 0) {
                debuffs.applyDebuff(target, DebuffType.INFLAME, buff.inflameTicks);
            }
        }
    }

    public void tickAll() {
        if (expiry.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (UUID uuid : new ArrayList<>(expiry.keySet())) {
            Map<BuffType, Long> map = expiry.get(uuid);
            if (map == null) continue;
            boolean removed = map.entrySet().removeIf(e -> now >= e.getValue());
            if (map.isEmpty()) expiry.remove(uuid);
            if (removed) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) refreshStats(player);
            }
        }
    }

    private void refreshStats(Player player) {
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null || plugin.getPlayerEventListener() == null || !player.isOnline()) return;
        plugin.getPlayerEventListener().applyPlayerStats(player);
    }
}