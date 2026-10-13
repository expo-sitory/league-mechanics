package dev.ixpu.leaguemechanics.entity.player.abilities.fighter;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilitiesRegistry;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import dev.ixpu.leaguemechanics.manager.ClassAbilitiesManager;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.util.Icons;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;

public class Ragnarok extends ClassAbilities {
    private static final String CFG = "skills.ragnarok.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final long AURA_INTERVAL_TICKS = 5L;

    private static final PotionEffectType[] HARMFUL_EFFECTS = {
            PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.NAUSEA,
            PotionEffectType.BLINDNESS, PotionEffectType.HUNGER, PotionEffectType.WEAKNESS,
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.LEVITATION,
            PotionEffectType.DARKNESS, PotionEffectType.UNLUCK
    };

    public Ragnarok(LeagueMechanics plugin) {
        super(plugin, "ragnarok", "Ragnarok", PlayerClassType.FIGHTER,
                "passives.ragnarok",
                "leaguemechanics:ragnarok",
                "minecraft:netherite_axe");
    }

    @Override
    public boolean canCastWhileCrowdControlled() {
        return true;
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
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.RAGNAROK);
    }

    @Override
    public List<String> getDescription() {
        AbilityEffectManager effects = AbilityEffectManager.getInstance();
        return List.of(
                "§7You passively gain increased attack speed and movement speed.§7",
                "§7Activate to become immune to disables for §f" + trim(effects.getDurationSeconds(AbilityEffectType.RAGNAROK)) + " §7seconds.§7",
                "§7This effect can last as long as you keep attacking."
        );
    }



    @Override
    public boolean cast(Player caster) {
        DebuffManager.getInstance().clearDebuffs(caster);
        for (PotionEffectType type : HARMFUL_EFFECTS) {
            caster.removePotionEffect(type);
        }
        caster.setFireTicks(0);

        AbilityEffectManager effects = AbilityEffectManager.getInstance();
        effects.activate(caster, AbilityEffectType.RAGNAROK);
        refreshStats(caster);

        long durationTicks = Math.max(1L, Math.round(effects.getDurationSeconds(AbilityEffectType.RAGNAROK) * 20.0));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (caster.isOnline()) refreshStats(caster);
        }, durationTicks + 2L);

        Location origin = caster.getLocation().add(0, 1.0, 0);
        caster.getWorld().spawnParticle(Particle.FLAME, origin, 40, 0.5, 0.8, 0.5, 0.08);
        caster.getWorld().spawnParticle(Particle.LAVA, origin, 12, 0.5, 0.6, 0.5, 0.0);
        caster.getWorld().playSound(origin, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);
        caster.getWorld().playSound(origin, Sound.ITEM_TOTEM_USE, 0.5f, 0.7f);

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!caster.isOnline() || caster.isDead() || !isActive(caster)) {
                task.cancel();
                if (caster.isOnline()) refreshStats(caster);
                return;
            }
            Location body = caster.getLocation().add(0, 1.0, 0);
            caster.getWorld().spawnParticle(Particle.FLAME, body, 3, 0.35, 0.6, 0.35, 0.01);
        }, AURA_INTERVAL_TICKS, AURA_INTERVAL_TICKS);
        return true;
    }

    public static void refreshOnAttack(Player attacker) {
        if (!isActive(attacker)) return;
        AbilityEffectManager.getInstance().activate(attacker, AbilityEffectType.RAGNAROK);
        ClassAbilities skill = ClassAbilitiesRegistry.getInstance().get("ragnarok");
        if (skill != null) ClassAbilitiesManager.getInstance().refreshEffect(attacker, skill);
    }

    public static boolean isActive(Player player) {
        return AbilityEffectManager.getInstance().isActive(player, AbilityEffectType.RAGNAROK);
    }

    private void refreshStats(Player player) {
        if (plugin.getPlayerEventListener() != null) {
            plugin.getPlayerEventListener().applyPlayerStats(player);
        }
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}