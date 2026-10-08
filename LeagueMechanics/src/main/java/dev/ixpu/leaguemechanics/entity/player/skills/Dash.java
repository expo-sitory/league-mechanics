package dev.ixpu.leaguemechanics.entity.player.skills;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.ClassPassives;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Dash {
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private final Map<UUID, Long> cooldownDurations = new HashMap<>();
    private final Map<UUID, BossBar> dashBars = new HashMap<>();

    private static final long BASE_COOLDOWN_MS = 7000L;

    private static long cooldownMs(Player player) {
        return (long) (BASE_COOLDOWN_MS * ClassPassives.getMobilityCooldownMultiplier(player));
    }

    public boolean isOnCooldown(Player player) {
        return getRemainingMs(player) > 0;
    }

    public long getRemainingMs(Player player) {
        UUID uuid = player.getUniqueId();
        Long lastActivation = playerCooldowns.get(uuid);
        if (lastActivation == null) {
            return 0L;
        }
        long duration = cooldownDurations.getOrDefault(uuid, cooldownMs(player));
        return Math.max(0L, duration - (System.currentTimeMillis() - lastActivation));
    }

    public void startCooldown(Player player) {
        startCooldown(player, cooldownMs(player));
    }

    public void startCooldown(Player player, long durationMs) {
        UUID uuid = player.getUniqueId();
        playerCooldowns.put(uuid, System.currentTimeMillis());
        cooldownDurations.put(uuid, durationMs);
        showCooldownBar(player, durationMs);
    }

    private static String formatSeconds(long remainingMs) {
        return String.format(Locale.ROOT, "(%.1fs)", Math.max(0L, remainingMs) / 1000.0);
    }

    private void showCooldownBar(Player player, long durationMs) {
        UUID uuid = player.getUniqueId();
        BossBar bar = dashBars.get(uuid);
        if (bar == null) {
            bar = Bukkit.createBossBar("§bDash §f" + formatSeconds(durationMs), BarColor.BLUE, BarStyle.SOLID);
            bar.addPlayer(player);
            dashBars.put(uuid, bar);
        }

        bar.setProgress(0.0);
        startCooldownAnimation(player, durationMs);
    }

    private void startCooldownAnimation(Player player, long cooldownMs) {
        UUID uuid = player.getUniqueId();
        long startTime = playerCooldowns.get(uuid);

        Bukkit.getScheduler().runTaskTimer(LeagueMechanics.getInstance(), task -> {
            if (!Long.valueOf(startTime).equals(playerCooldowns.get(uuid))) {
                task.cancel();
                return;
            }

            if (!player.isOnline()) {
                task.cancel();
                BossBar bar = dashBars.remove(uuid);
                if (bar != null) bar.removeAll();
                return;
            }

            BossBar bar = dashBars.get(uuid);
            if (bar == null) {
                task.cancel();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / cooldownMs);
            bar.setProgress(progress);
            bar.setTitle("§bDash §f" + formatSeconds(cooldownMs - elapsed));

            if (elapsed >= cooldownMs) {
                task.cancel();
                bar.removeAll();
                dashBars.remove(uuid);
            }
        }, 0L, 1L);
    }

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = dashBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
        playerCooldowns.remove(uuid);
        cooldownDurations.remove(uuid);
    }
}