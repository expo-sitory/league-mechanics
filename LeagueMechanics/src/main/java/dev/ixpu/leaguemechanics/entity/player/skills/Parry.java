package dev.ixpu.leaguemechanics.entity.player.skills;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import dev.ixpu.leaguemechanics.LeagueMechanics;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Parry {
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private final Map<UUID, Long> cooldownDurations = new HashMap<>();
    private final Map<UUID, BossBar> parryBars = new HashMap<>();

    private static final long MISSED_COOLDOWN_MS = 7000L;
    private static final long SUCCESSFUL_COOLDOWN_MS = 2000L;
    private static final long PARRY_WINDOW_MS = 700L;

    public void resetCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        playerCooldowns.put(uuid, System.currentTimeMillis());
        cooldownDurations.put(uuid, SUCCESSFUL_COOLDOWN_MS);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission unset oldcombatmechanics.swordblock");
        showSuccessfulCooldownBar(player);
    }

    public void startMissedCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        playerCooldowns.put(uuid, System.currentTimeMillis());
        cooldownDurations.put(uuid, MISSED_COOLDOWN_MS);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission unset oldcombatmechanics.swordblock");
        showMissedCooldownBar(player);
    }

    public boolean isOnCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        Long lastActivation = playerCooldowns.get(uuid);
        Long duration = cooldownDurations.get(uuid);
        if (lastActivation == null || duration == null) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - lastActivation;
        return elapsed < duration;
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

    private void showSuccessfulCooldownBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = parryBars.get(uuid);
        if (bar == null) {
            bar = Bukkit.createBossBar("§bParry §f" + formatSeconds(SUCCESSFUL_COOLDOWN_MS), BarColor.BLUE, BarStyle.SOLID);
            bar.addPlayer(player);
            parryBars.put(uuid, bar);
        }

        bar.setProgress(0.0);
        startCooldownAnimation(player, SUCCESSFUL_COOLDOWN_MS);
    }

    private void showMissedCooldownBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = parryBars.get(uuid);
        if (bar == null) {
            bar = Bukkit.createBossBar("§bParry §f" + formatSeconds(MISSED_COOLDOWN_MS), BarColor.BLUE, BarStyle.SOLID);
            bar.addPlayer(player);
            parryBars.put(uuid, bar);
        }

        bar.setProgress(0.0);
        startCooldownAnimation(player, MISSED_COOLDOWN_MS);
    }

    private void startCooldownAnimation(Player player, long cooldownMs) {
        UUID uuid = player.getUniqueId();
        long startTime = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskTimer(LeagueMechanics.getInstance(), task -> {
            if (!player.isOnline()) {
                task.cancel();
                BossBar bar = parryBars.remove(uuid);
                if (bar != null) bar.removeAll();
                cooldownDurations.remove(uuid);
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
                cooldownDurations.remove(uuid);
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

        playerCooldowns.remove(uuid);
        cooldownDurations.remove(uuid);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set oldcombatmechanics.swordblock true");
    }
}