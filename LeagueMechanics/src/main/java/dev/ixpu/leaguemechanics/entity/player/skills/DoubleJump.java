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

public class DoubleJump {
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private final Map<UUID, BossBar> doubleJumpBars = new HashMap<>();

    private static final long BASE_COOLDOWN_MS = 7000L;

    private static long cooldownMs(Player player) {
        return (long) (BASE_COOLDOWN_MS * ClassPassives.getMobilityCooldownMultiplier(player));
    }

    public boolean isOnCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        Long lastActivation = playerCooldowns.get(uuid);
        if (lastActivation == null) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - lastActivation;
        return elapsed < cooldownMs(player);
    }

    public void startCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        playerCooldowns.put(uuid, System.currentTimeMillis());
        showCooldownBar(player);
    }

    private static String formatSeconds(long remainingMs) {
        return String.format(Locale.ROOT, "(%.1fs)", Math.max(0L, remainingMs) / 1000.0);
    }

    private void showCooldownBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = doubleJumpBars.get(uuid);
        if (bar == null) {
            bar = Bukkit.createBossBar("§bDouble Jump §f" + formatSeconds(cooldownMs(player)), BarColor.BLUE, BarStyle.SOLID);
            bar.addPlayer(player);
            doubleJumpBars.put(uuid, bar);
        }

        bar.setProgress(0.0);
        startCooldownAnimation(player);
    }

    private void startCooldownAnimation(Player player) {
        UUID uuid = player.getUniqueId();
        long startTime = System.currentTimeMillis();
        long cooldownMs = cooldownMs(player);

        Bukkit.getScheduler().runTaskTimer(LeagueMechanics.getInstance(), task -> {
            if (!player.isOnline()) {
                task.cancel();
                BossBar bar = doubleJumpBars.remove(uuid);
                if (bar != null) bar.removeAll();
                return;
            }

            BossBar bar = doubleJumpBars.get(uuid);
            if (bar == null) {
                task.cancel();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / cooldownMs);
            bar.setProgress(progress);
            bar.setTitle("§bDouble Jump §f" + formatSeconds(cooldownMs - elapsed));

            if (elapsed >= cooldownMs) {
                task.cancel();
                bar.removeAll();
                doubleJumpBars.remove(uuid);
            }
        }, 0L, 1L);
    }

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = doubleJumpBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
        playerCooldowns.remove(uuid);
    }
}