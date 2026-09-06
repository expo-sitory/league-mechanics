package dev.ixpu.leaguemechanics.rune;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.manager.ItemStatsManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.player.PlayerStats;
import dev.ixpu.leaguemechanics.rune.DebuffType;
import org.bukkit.entity.Player;

public class DebuffTicker {
    private static final int INFLAME_INTERVAL_TICKS = 10;
    private static final double INFLAME_BASE_DAMAGE = 2.5;
    private static final double INFLAME_AP_SCALING = 0.01;

    private final java.util.Map<java.util.UUID, Integer> inflameTickCounters = new java.util.HashMap<>();

    public void tick(Player target, Player attacker) {
        if (target == null) return;
        DebuffManager debuffManager = DebuffManager.getInstance();
        LeagueMechanics plugin = LeagueMechanics.getInstance();

        if (debuffManager.hasDebuff(target, DebuffType.INFLAME)) {
            int counter = inflameTickCounters.getOrDefault(target.getUniqueId(), 0) + 1;
            inflameTickCounters.put(target.getUniqueId(), counter);

            if (counter >= INFLAME_INTERVAL_TICKS) {
                inflameTickCounters.put(target.getUniqueId(), 0);

                double attackerAP = 0;
                if (attacker != null) {
                    attackerAP = PlayerStats.getOrCreate(attacker).getPlayerAP(attacker);
                }
                double inflameDamage = INFLAME_BASE_DAMAGE + (INFLAME_AP_SCALING * attackerAP);
                double newHealth = Math.max(0, target.getHealth() - inflameDamage);
                if (attacker != null) {
                    KillSourceTracker.getInstance().setSource(target, attacker);
                }
                target.setHealth(newHealth);
            }
        } else {
            inflameTickCounters.remove(target.getUniqueId());
        }

        if (plugin != null) {
            ItemStatsManager statsManager = plugin.getStatsManager();
            PlayerStats stats = PlayerStats.getOrCreate(target);
            if (statsManager != null && stats != null) {
                boolean hasSlow = debuffManager.hasDebuff(target, DebuffType.SLOW);
                double slowPercent = 0.0;
                if (hasSlow) {
                    slowPercent = debuffManager.getDebuffStrength(target, DebuffType.SLOW);
                    if (slowPercent > 0) {
                        double currentMS = stats.getPlayerMS(target);
                        double reduction = currentMS * (slowPercent / 100.0);
                        stats.setTemporaryMSModification(-reduction);
                    }
                }
                if (!hasSlow || slowPercent <= 0) {
                    stats.setTemporaryMSModification(0.0);
                }
            }
        }
    }
}
