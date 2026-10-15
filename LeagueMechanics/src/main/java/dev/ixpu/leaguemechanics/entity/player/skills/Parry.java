package dev.ixpu.leaguemechanics.entity.player.skills;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.ClassPassives;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Parry {
    private final CooldownHandler successfulCooldown = scaledCooldown("parry-successful", SUCCESSFUL_COOLDOWN_MS);
    private final CooldownHandler missedCooldown = scaledCooldown("parry-missed", MISSED_COOLDOWN_MS);
    private final Map<UUID, BossBar> parryBars = new HashMap<>();

    private static final long MISSED_COOLDOWN_MS = 7000L;
    private static final long SUCCESSFUL_COOLDOWN_MS = 2000L;
    private static final long PARRY_WINDOW_MS = 700L;

    public void resetCooldown(Player player) {
        missedCooldown.clearPlayerCooldown(player);
        successfulCooldown.resetCooldown(player);
        long cooldownMs = scaled(player, SUCCESSFUL_COOLDOWN_MS);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission unset oldcombatmechanics.swordblock");
        showSuccessfulCooldownBar(player, cooldownMs);
    }

    public void startMissedCooldown(Player player) {
        successfulCooldown.clearPlayerCooldown(player);
        missedCooldown.resetCooldown(player);
        long cooldownMs = scaled(player, MISSED_COOLDOWN_MS);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission unset oldcombatmechanics.swordblock");
        showMissedCooldownBar(player, cooldownMs);
    }

    private static long scaled(Player player, long baseMs) {
        return (long) (baseMs * ClassPassives.getParryCooldownMultiplier(player));
    }

    private static CooldownHandler scaledCooldown(String id, long baseMs) {
        return new CooldownHandler(id) {
            @Override
            protected double effectiveCooldownSeconds(Player player) {
                return scaled(player, baseMs) / 1000.0;
            }
        };
    }

    public boolean isOnCooldown(Player player) {
        return successfulCooldown.isOnCooldown(player) || missedCooldown.isOnCooldown(player);
    }

    private static String formatSeconds(long remainingMs) {
        return String.format(Locale.ROOT, "(%.1fs)", Math.max(0L, remainingMs) / 1000.0);
    }

    public void showParryWindowBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = parryBars.get(uuid);
        if (bar != null) {
            bar.removeAll();
        }

        BossBar newBar = Bukkit.createBossBar("§bParry §f" + formatSeconds(PARRY_WINDOW_MS), BarColor.BLUE, BarStyle.SOLID);
        newBar.addPlayer(player);
        newBar.setProgress(1.0);
        parryBars.put(uuid, newBar);

        startParryWindowAnimation(player);
    }

    private void startParryWindowAnimation(Player player) {
        UUID uuid = player.getUniqueId();
        long startTime = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskTimer(LeagueMechanics.getInstance(), task -> {
            BossBar bar = parryBars.get(uuid);
            if (bar == null) {
                task.cancel();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.max(0.0, 1.0 - ((double) elapsed / PARRY_WINDOW_MS));
            bar.setProgress(progress);
            bar.setTitle("§bParry §f" + formatSeconds(PARRY_WINDOW_MS - elapsed));

            if (elapsed >= PARRY_WINDOW_MS) {
                task.cancel();
            }
        }, 0L, 1L);
    }

    public void hideParryWindowBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = parryBars.get(uuid);
        if (bar != null) {
            bar.removeAll();
            parryBars.remove(uuid);
        }
    }

    private void showSuccessfulCooldownBar(Player player, long cooldownMs) {
        UUID uuid = player.getUniqueId();
        BossBar bar = parryBars.get(uuid);
        if (bar == null) {
            bar = Bukkit.createBossBar("§bParry §f" + formatSeconds(cooldownMs), BarColor.BLUE, BarStyle.SOLID);
            bar.addPlayer(player);
            parryBars.put(uuid, bar);
        }

        bar.setProgress(0.0);
        startCooldownAnimation(player, cooldownMs);
    }

    private void showMissedCooldownBar(Player player, long cooldownMs) {
        UUID uuid = player.getUniqueId();
        BossBar bar = parryBars.get(uuid);
        if (bar == null) {
            bar = Bukkit.createBossBar("§bParry §f" + formatSeconds(cooldownMs), BarColor.BLUE, BarStyle.SOLID);
            bar.addPlayer(player);
            parryBars.put(uuid, bar);
        }

        bar.setProgress(0.0);
        startCooldownAnimation(player, cooldownMs);
    }

    private void startCooldownAnimation(Player player, long cooldownMs) {
        UUID uuid = player.getUniqueId();
        long startTime = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskTimer(LeagueMechanics.getInstance(), task -> {
            if (!player.isOnline()) {
                task.cancel();
                BossBar bar = parryBars.remove(uuid);
                if (bar != null) bar.removeAll();
                return;
            }

            BossBar bar = parryBars.get(uuid);
            if (bar == null) {
                task.cancel();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / cooldownMs);
            bar.setProgress(progress);
            bar.setTitle("§bParry §f" + formatSeconds(cooldownMs - elapsed));

            if (elapsed >= cooldownMs) {
                task.cancel();
                bar.removeAll();
                parryBars.remove(uuid);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set oldcombatmechanics.swordblock true");
            }
        }, 0L, 1L);
    }

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();

        BossBar bar = parryBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }

        successfulCooldown.clearPlayerCooldown(player);
        missedCooldown.clearPlayerCooldown(player);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set oldcombatmechanics.swordblock true");
    }
}