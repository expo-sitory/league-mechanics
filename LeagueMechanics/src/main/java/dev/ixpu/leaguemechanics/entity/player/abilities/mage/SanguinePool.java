package dev.ixpu.leaguemechanics.entity.player.abilities.mage;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class SanguinePool extends ClassAbilities {
    private static final String CFG = "skills.saguine-pool.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 60.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 15.0;
    private static final double DEFAULT_RADIUS = 5.0;
    private static final double DEFAULT_SCALE_PERCENT = 99.0;

    private static final long TASK_INTERVAL_TICKS = 2L;
    private static final int PARTICLE_EVERY_TASKS = 2;
    private static final int RING_POINTS = 60;
    private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.5f);

    public SanguinePool(LeagueMechanics plugin) {
        super(plugin, "sanguine-pool", "Sanguine Pool", PlayerClassType.MAGE,
                "passives.saguine_pool",
                "leaguemechanics:saguine-pool",
                "minecraft:redstone");
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
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.SANGUINE_POOL);
    }

    private double getRadius() {
        return Math.max(1.0, plugin.getConfig().getDouble(CFG + "radius", DEFAULT_RADIUS));
    }

    private double getScalePercent() {
        return Math.max(10.0, Math.min(100.0, plugin.getConfig().getDouble(CFG + "scale-percent", DEFAULT_SCALE_PERCENT)));
    }

    @Override
    public List<String> getDescription() {
        AbilityEffectManager effects = AbilityEffectManager.getInstance();
        return List.of(
                "§7You sink into a pool of blood, becoming untargetable for§7",
                "§f" + trim(getEffectSeconds()) + " seconds§7. While active, you gain §f" + trim(effects.getConfiguredSanguinePoolMovementSpeed()) + "%§7 bonus movement",
                "§7speed to maneuver around your enemies."
        );
    }


    @Override
    public boolean cast(Player caster) {
        AbilityEffectManager.getInstance().activate(caster, AbilityEffectType.SANGUINE_POOL);
        applyScale(caster);
        refreshStats(caster);

        World world = caster.getWorld();
        Location origin = caster.getLocation().add(0, 0.5, 0);
        world.spawnParticle(Particle.DUST, origin, 40, 0.5, 0.5, 0.5, 0, RED);
        world.playSound(origin, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 0.6f);
        world.playSound(origin, Sound.ENTITY_WITCH_DRINK, 0.8f, 0.7f);

        double radius = getRadius();
        int[] counter = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!caster.isOnline()) {
                task.cancel();
                return;
            }
            if (!isActive(caster)) {
                task.cancel();
                removeScale(caster);
                refreshStats(caster);
                if (!caster.isDead()) {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.4f);
                }
                return;
            }
            if (counter[0]++ % PARTICLE_EVERY_TASKS == 0) {
                drawPool(caster, radius);
            }
        }, 1L, TASK_INTERVAL_TICKS);
        return true;
    }

    public static boolean isActive(Player player) {
        return AbilityEffectManager.getInstance().isActive(player, AbilityEffectType.SANGUINE_POOL);
    }

    public static void cleanup(Player player) {
        removeScale(player);
    }

    private void drawPool(Player player, double radius) {
        World world = player.getWorld();
        Location center = player.getLocation().add(0, 0.1, 0);
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = 2 * Math.PI * i / RING_POINTS;
            world.spawnParticle(Particle.DUST,
                    center.getX() + Math.cos(angle) * radius, center.getY(), center.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0, RED);
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            double angle = random.nextDouble(0, Math.PI * 2);
            double distance = Math.sqrt(random.nextDouble()) * radius;
            world.spawnParticle(Particle.DUST,
                    center.getX() + Math.cos(angle) * distance, center.getY(), center.getZ() + Math.sin(angle) * distance,
                    1, 0, 0, 0, 0, RED);
        }
    }

    private static NamespacedKey scaleKey() {
        return new NamespacedKey(LeagueMechanics.getInstance(), "sanguine_pool_scale");
    }

    private void applyScale(Player player) {
        AttributeInstance scale = player.getAttribute(Attribute.SCALE);
        if (scale == null) return;
        removeScale(player);
        double amount = getScalePercent() / 100.0 - 1.0;
        scale.addModifier(new AttributeModifier(scaleKey(), amount, AttributeModifier.Operation.ADD_SCALAR));
    }

    private static void removeScale(Player player) {
        AttributeInstance scale = player.getAttribute(Attribute.SCALE);
        if (scale == null) return;
        NamespacedKey key = scaleKey();
        for (AttributeModifier modifier : List.copyOf(scale.getModifiers())) {
            if (key.equals(modifier.getKey())) {
                scale.removeModifier(modifier);
            }
        }
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
