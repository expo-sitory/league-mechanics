package dev.ixpu.leaguemechanics.rune.slots.primary.domination;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.paths.RunePath;
import dev.ixpu.leaguemechanics.rune.slots.RuneSlot;
import dev.ixpu.leaguemechanics.util.Icons;
import dev.ixpu.leaguemechanics.util.DebugLogger;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.UUID;
import net.kyori.adventure.text.Component;

public class SuddenImpact extends CooldownHandler {

    private static final double BASE_TRUE_DAMAGE_PER_LEVEL = 5.0; // 5 * levelBasedBonusForLevel() bonus true damage
    private static final int DASH_WINDOW_SECONDS = 2; // 2 seconds window after dash/double jump
    private static final int COOLDOWN_SECONDS = 10; // 10 second cooldown

    // Track when players last used dash or double jump
    private static final java.util.Map<UUID, Long> lastDashOrDoubleJumpTime =
            new java.util.HashMap<>();

    public SuddenImpact(ConfigurationSection config) {
        super("sudden-impact", RunePath.DOMINATION, RuneSlot.PRIMARY_SLOT_1);

        ConfigurationSection section = config.getConfigurationSection("runes.primary-slots.domination.slot-1.sudden-impact");
        if (section != null) {
            // Config values can be read here if needed for future customization
        }
        // Set the cooldown duration for this rune
        setCooldownSeconds(COOLDOWN_SECONDS);
    }

    @Override
    public void onAttack(Player attacker, Entity target) {
        if (!letRunesThrough(attacker)) return;
        if (!(target instanceof LivingEntity livingTarget)) return;

        // Check if on cooldown
        if (isOnCooldown(attacker)) {
            return;
        }

        UUID attackerUUID = attacker.getUniqueId();
        Long lastDashTime = lastDashOrDoubleJumpTime.get(attackerUUID);
        long currentTime = System.currentTimeMillis();

        boolean recentlyDashed = lastDashTime != null &&
                                (currentTime - lastDashTime) <= (DASH_WINDOW_SECONDS * 1000);

        if (recentlyDashed) {
            PlayerStats playerStats = PlayerStats.getOrCreate(attacker);
            int level = playerStats.getLeagueLevel();
            double trueDamage = BASE_TRUE_DAMAGE_PER_LEVEL * level;

            // Apply true damage (bypasses armor)
            livingTarget.damage(trueDamage, attacker);

            // Start cooldown
            resetCooldown(attacker);

            DebugLogger.debug(attacker, "§f[§dSource§f] [§eSudden Impact§f] Dealt §d" +
                    Math.ceil(trueDamage * 100) / 100.0 + " §fbonus true damage (recent dash)");
        }
    }

    // Method to be called when player uses dash or double jump
    public static void onDashOrDoubleJump(Player player) {
        UUID playerUUID = player.getUniqueId();
        lastDashOrDoubleJumpTime.put(playerUUID, System.currentTimeMillis());

        // Clean up old entries occasionally to prevent memory leak
        if (lastDashOrDoubleJumpTime.size() > 1000) {
            long cutoffTime = System.currentTimeMillis() - (DASH_WINDOW_SECONDS * 1000 * 2); // 2x window for safety
            lastDashOrDoubleJumpTime.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
        }
    }

    @Override
    public String getDisplaySection(Player player) {
        // Show cooldown if active
        double cooldownRemaining = getRemainingCooldown(player);
        if (cooldownRemaining > 0) {
            return Icons.get("runes-slots.sudden-impact") + " §7(" + String.format("%.1fs", cooldownRemaining) + ")";
        }

        // Show dash window if active
        UUID attackerUUID = player.getUniqueId();
        Long lastDashTime = lastDashOrDoubleJumpTime.get(attackerUUID);
        long currentTime = System.currentTimeMillis();

        if (lastDashTime != null && (currentTime - lastDashTime) <= (DASH_WINDOW_SECONDS * 1000)) {
            long remainingSeconds = (DASH_WINDOW_SECONDS * 1000 - (currentTime - lastDashTime)) / 1000;
            return Icons.get("runes-slots.sudden-impact") + " §7(" + remainingSeconds + "s dash)";
        }

        return Icons.get("runes-slots.sudden-impact");
    }

    private boolean letRunesThrough(Player player) {
        return LeagueMechanics.getInstance().getPlayerEventListener().letRunesThrough(player);
    }
}