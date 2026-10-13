package dev.ixpu.leaguemechanics.entity.player.abilities.marksman;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SprayAndPray extends ClassAbilities {
    private static final String CFG = "skills.spray-and-pray.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_POISON_SECONDS = 10.0;
    private static final int MAX_TRAIL_TICKS = 20 * 15;

    private static final Particle.DustOptions LIME = new Particle.DustOptions(Color.fromRGB(150, 255, 40), 1.6f);
    private static final Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(40, 200, 60), 1.4f);

    private static final Set<UUID> marked = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Set<UUID>> pierced = new ConcurrentHashMap<>();

    public SprayAndPray(LeagueMechanics plugin) {
        super(plugin, "spray-and-pray", "Spray and Pray", PlayerClassType.MARKSMAN,
                "passives.spray_and_pray",
                "leaguemechanics:spray-and-pray",
                "minecraft:bow");
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
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.SPRAY_AND_PRAY);
    }

    private static double getPoisonSeconds() {
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return DEFAULT_POISON_SECONDS;
        return plugin.getConfig().getDouble(CFG + "poison-seconds", DEFAULT_POISON_SECONDS);
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "§7You unleash the full power of your arrow for §f" + trim(getEffectSeconds()) + " seconds§7,",
                "§7shooting bolts that pierce all enemies in their path and leave a§7",
                "§7toxic trail. Targets hit are §2poisoned§7 for §f" + trim(getPoisonSeconds()) + " seconds§7."
        );
    }


    @Override
    public boolean cast(Player caster) {
        AbilityEffectManager.getInstance().activate(caster, AbilityEffectType.SPRAY_AND_PRAY);

        Location origin = caster.getLocation().add(0, 1.0, 0);
        caster.getWorld().spawnParticle(Particle.DUST, origin, 40, 0.5, 0.8, 0.5, 0, LIME);
        caster.getWorld().spawnParticle(Particle.DUST, origin, 25, 0.5, 0.8, 0.5, 0, GREEN);
        caster.getWorld().playSound(origin, Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 0.6f);
        caster.getWorld().playSound(origin, Sound.ITEM_CROSSBOW_LOADING_END, 0.8f, 1.2f);
        return true;
    }

    public static boolean isActive(Player player) {
        return AbilityEffectManager.getInstance().isActive(player, AbilityEffectType.SPRAY_AND_PRAY);
    }

    public static boolean isPiercing(Entity projectile) {
        return projectile instanceof AbstractArrow && !(projectile instanceof Trident) && marked.contains(projectile.getUniqueId());
    }

    public static boolean registerPierceHit(Entity projectile, Entity target) {
        Set<UUID> hit = pierced.get(projectile.getUniqueId());
        return hit != null && hit.add(target.getUniqueId());
    }

    public static boolean hasPierced(Entity projectile, Entity target) {
        Set<UUID> hit = pierced.get(projectile.getUniqueId());
        return hit != null && hit.contains(target.getUniqueId());
    }

    public static void onLaunch(Projectile projectile) {
        if (!(projectile.getShooter() instanceof Player shooter) || !isActive(shooter)) return;

        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return;

        UUID id = projectile.getUniqueId();
        marked.add(id);
        pierced.put(id, ConcurrentHashMap.newKeySet());
        if (projectile instanceof AbstractArrow arrow && !(arrow instanceof Trident)) {
            arrow.setPierceLevel((byte) 127);
        }

        int[] age = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            boolean stuck = projectile instanceof AbstractArrow arrow && arrow.isInBlock();
            if (!projectile.isValid() || stuck || ++age[0] > MAX_TRAIL_TICKS) {
                task.cancel();
                marked.remove(id);
                pierced.remove(id);
                return;
            }
            Location loc = projectile.getLocation();
            loc.getWorld().spawnParticle(Particle.DUST, loc, 6, 0.1, 0.1, 0.1, 0, LIME);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 4, 0.15, 0.15, 0.15, 0, GREEN);
        }, 0L, 1L);
    }

    public static void onHit(Projectile projectile, Entity hit) {
        if (!marked.contains(projectile.getUniqueId())) return;
        if (!(hit instanceof LivingEntity target) || hit.equals(projectile.getShooter())) return;

        int ticks = Math.max(1, (int) Math.round(getPoisonSeconds() * 20.0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, ticks, 0));
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}