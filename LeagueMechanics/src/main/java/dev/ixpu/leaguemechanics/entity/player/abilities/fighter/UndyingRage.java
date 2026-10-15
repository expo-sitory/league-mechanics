package dev.ixpu.leaguemechanics.entity.player.abilities.fighter;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;
import java.util.Locale;

public class UndyingRage extends ClassAbilities {
    private static final String CFG = "skills.undying-rage.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_MIN_HEALTH = 1.0;
    private static final long AURA_INTERVAL_TICKS = 5L;

    public UndyingRage(LeagueMechanics plugin) {
        super(plugin, "undying-rage", "Undying Rage", PlayerClassType.FIGHTER,
                "passives.tryndamere_undying_rage",
                "leaguemechanics:undying-rage",
                "minecraft:totem_of_undying");
    }

    @Override
    public double getMinCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "min-cooldown-seconds", DEFAULT_MIN_COOLDOWN_SECONDS);
    }


    @Override
    public double getCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "cooldown-seconds", DEFAULT_COOLDOWN_SECONDS);
    }

    @Override
    public double getEffectSeconds() {
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.UNDYING_RAGE);
    }

    @Override
    public List<String> getDescription() {
        double seconds = AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.UNDYING_RAGE);
        return List.of(
                "§7Your lust for battle becomes so strong that you§7",
                "§7are unable to die for §f" + trim(seconds) + " seconds§7, no matter§7",
                "§7how wounded you become during this time."
        );
    }


    @Override
    public boolean cast(Player caster) {
        AbilityEffectManager.getInstance().activate(caster, AbilityEffectType.UNDYING_RAGE);

        Location origin = caster.getLocation().add(0, 1.0, 0);
        caster.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, origin, 60, 0.5, 0.8, 0.5, 0.4);
        caster.getWorld().playSound(origin, Sound.ITEM_TOTEM_USE, 0.8f, 1.0f);

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!caster.isOnline() || caster.isDead() || !isActive(caster)) {
                task.cancel();
                if (caster.isOnline() && !caster.isDead()) {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.2f);
                }
                return;
            }
            Location head = caster.getLocation().add(0, 2.0, 0);
            caster.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, head, 2, 0.3, 0.1, 0.3, 0.0);
        }, AURA_INTERVAL_TICKS, AURA_INTERVAL_TICKS);
        return true;
    }

    public static boolean isActive(Player player) {
        return AbilityEffectManager.getInstance().isActive(player, AbilityEffectType.UNDYING_RAGE);
    }

    public static void protect(Player player, EntityDamageEvent event) {
        if (!isActive(player)) return;

        switch (event.getCause()) {
            case VOID, SUICIDE, KILL -> {
                return;
            }
            default -> {
            }
        }

        LeagueMechanics plugin = LeagueMechanics.getInstance();
        double minHealth = plugin == null ? DEFAULT_MIN_HEALTH
                : Math.max(0.0, plugin.getConfig().getDouble(CFG + "min-health", DEFAULT_MIN_HEALTH));

        double allowed = player.getHealth() - minHealth;
        double incoming = event.getFinalDamage();
        if (incoming <= allowed) return;

        if (allowed <= 0.0) {
            event.setDamage(0.0);
            return;
        }
        event.setDamage(event.getDamage() * (allowed / incoming));
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}