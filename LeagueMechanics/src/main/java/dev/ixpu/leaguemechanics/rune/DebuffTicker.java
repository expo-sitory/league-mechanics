package dev.ixpu.leaguemechanics.rune;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.manager.ItemStatsManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DebuffTicker {
    private static final int INFLAME_INTERVAL_TICKS = 30;
    private static final double INFLAME_BASE_DAMAGE = 1.7;

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

                double inflameDamage = inflameDamage(attacker, target);

                if (target instanceof Player targetPlayer) {
                    double absorption = targetPlayer.getAbsorptionAmount();
                    if (inflameDamage > absorption) {
                        inflameDamage -= absorption;
                        targetPlayer.setAbsorptionAmount(0);
                    } else {
                        targetPlayer.setAbsorptionAmount(absorption - inflameDamage);
                        inflameDamage = 0;
                    }
                }

                double newHealth = Math.clamp(target.getHealth() - inflameDamage, 0, target.getMaxHealth());

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

    private double inflameDamage(Player source, Entity target) {
        ItemStatsManager statsManager = LeagueMechanics.getInstance().getStatsManager();
        DamageManager damageManager = new DamageManager(statsManager);
        damageManager.enableOnlyAP();
        return damageManager.DamageCalculation(source, target, 0, 0, 0, INFLAME_BASE_DAMAGE);
    }
}
