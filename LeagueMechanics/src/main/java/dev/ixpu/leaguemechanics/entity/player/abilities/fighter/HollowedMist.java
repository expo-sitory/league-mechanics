package dev.ixpu.leaguemechanics.entity.player.abilities.fighter;

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
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class HollowedMist extends ClassAbilities {
    private static final String CFG = "skills.hollowed-mist.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_RADIUS = 5.0;

    private static final int RING_POINTS = 72;
    private static final long PARTICLE_INTERVAL_TICKS = 5L;
    private static final Color DARK_AQUA = Color.fromRGB(0, 170, 170);
    private static final Color BLUE = Color.fromRGB(85, 85, 255);
    private static final Color WHITE = Color.fromRGB(255, 255, 255);
    private static final Particle.DustOptions[] RING_COLORS = {
            new Particle.DustOptions(DARK_AQUA, 1.3f),
            new Particle.DustOptions(BLUE, 1.3f),
            new Particle.DustOptions(WHITE, 1.1f)
    };
    private static final Particle.DustTransition FOG_AQUA = new Particle.DustTransition(DARK_AQUA, WHITE, 1.8f);
    private static final Particle.DustTransition FOG_BLUE = new Particle.DustTransition(BLUE, WHITE, 1.8f);

    private record Mist(Location center, double radius, List<Location> ring) {}

    private static final Map<UUID, Mist> active = new ConcurrentHashMap<>();

    public HollowedMist(LeagueMechanics plugin) {
        super(plugin, "hollowed-mist", "Hollowed Mist", PlayerClassType.FIGHTER,
                "passives.hollowed_mist",
                "leaguemechanics:hollowed-mist",
                "minecraft:phantom_membrane");
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
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.HOLLOWED_MIST);
    }

    private double getRadius() {
        return Math.max(1.0, plugin.getConfig().getDouble(CFG + "radius", DEFAULT_RADIUS));
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "§7You summon a §f" + trim(getRadius()) + "§7 block radius mist that protects you from§7",
                "§7enemies outside of it for §f" + trim(getEffectSeconds()) + " seconds§7. You can only be§7",
                "§7targeted and damaged by enemies who enter the mist."
        );
    }


    @Override
    public boolean cast(Player caster) {
        UUID uuid = caster.getUniqueId();
        Location center = caster.getLocation().clone();
        double radius = getRadius();

        AbilityEffectManager.getInstance().activate(caster, AbilityEffectType.HOLLOWED_MIST);
        Mist mist = new Mist(center, radius, buildRing(center, radius));
        active.put(uuid, mist);

        World world = center.getWorld();
        world.playSound(center, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.8f, 0.8f);
        world.spawnParticle(Particle.CLOUD, center.clone().add(0, 0.3, 0), 25, radius / 3.0, 0.1, radius / 3.0, 0.02);
        drawBurst(center, radius);

        int[] tick = {0};

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            boolean alive = caster.isOnline() && !caster.isDead() && isActive(caster);
            if (!alive) {
                task.cancel();
                active.remove(uuid, mist);
                return;
            }
            drawMist(mist, tick[0]++);
        }, 1L, PARTICLE_INTERVAL_TICKS);
        return true;
    }

    public static boolean isActive(Player player) {
        return active.containsKey(player.getUniqueId())
                && AbilityEffectManager.getInstance().isActive(player, AbilityEffectType.HOLLOWED_MIST);
    }

    public static boolean blocks(Player victim, Entity damager) {
        Mist mist = active.get(victim.getUniqueId());
        if (mist == null) return false;
        if (!AbilityEffectManager.getInstance().isActive(victim, AbilityEffectType.HOLLOWED_MIST)) {
            active.remove(victim.getUniqueId(), mist);
            return false;
        }
        if (damager == null) return false;

        Entity source = damager;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            source = shooter;
        }
        Location at = source.getLocation();
        if (at.getWorld() == null || !at.getWorld().equals(mist.center().getWorld())) return true;

        double dx = at.getX() - mist.center().getX();
        double dz = at.getZ() - mist.center().getZ();
        return dx * dx + dz * dz > mist.radius() * mist.radius();
    }

    public static void cleanup(UUID uuid) {
        active.remove(uuid);
    }

    private void drawBurst(Location center, double radius) {
        World world = center.getWorld();
        int points = 48;
        double[] scales = {0.4, 0.8, 1.0};
        for (int ringIndex = 0; ringIndex < scales.length; ringIndex++) {
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double distance = radius * scales[ringIndex];
                Location point = center.clone().add(Math.cos(angle) * distance, 0.15, Math.sin(angle) * distance);
                world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, RING_COLORS[(ringIndex + i) % RING_COLORS.length]);
            }
        }
    }

    private void drawMist(Mist mist, int tick) {
        World world = mist.center().getWorld();
        List<Location> ring = mist.ring();
        for (int i = 0; i < ring.size(); i++) {
            Location point = ring.get(i);
            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, RING_COLORS[((i + tick * 2) / 4) % RING_COLORS.length]);
            if ((i + tick) % 6 == 0) {
                world.spawnParticle(Particle.DUST_COLOR_TRANSITION, point.clone().add(0, 0.7, 0), 1, 0.05, 0.25, 0.05, 0,
                        (i / 6) % 2 == 0 ? FOG_AQUA : FOG_BLUE);
            }
        }

        for (int arm = 0; arm < 2; arm++) {
            for (int k = 1; k <= 5; k++) {
                double angle = tick * 0.35 + arm * Math.PI + k * 0.5;
                double distance = mist.radius() * k / 6.0;
                Location swirl = mist.center().clone().add(Math.cos(angle) * distance, 0.2, Math.sin(angle) * distance);
                world.spawnParticle(Particle.DUST_COLOR_TRANSITION, swirl, 1, 0.05, 0.05, 0.05, 0, arm == 0 ? FOG_AQUA : FOG_BLUE);
            }
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 2; i++) {
            double angle = random.nextDouble(0, Math.PI * 2);
            double distance = Math.sqrt(random.nextDouble()) * mist.radius();
            Location wisp = mist.center().clone().add(Math.cos(angle) * distance, 0.2, Math.sin(angle) * distance);
            world.spawnParticle(Particle.CLOUD, wisp, 1, 0.1, 0.05, 0.1, 0.005);
        }
    }

    private List<Location> buildRing(Location center, double radius) {
        List<Location> ring = new ArrayList<>(RING_POINTS);
        World world = center.getWorld();
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = 2 * Math.PI * i / RING_POINTS;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            ring.add(new Location(world, x, groundY(world, x, center.getY(), z) + 0.1, z));
        }
        return ring;
    }

    private double groundY(World world, double x, double y, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int start = (int) Math.floor(y) + 2;
        int min = Math.max(world.getMinHeight(), (int) Math.floor(y) - 4);
        for (int by = start; by >= min; by--) {
            if (!world.getBlockAt(bx, by, bz).isPassable()) {
                return by + 1.0;
            }
        }
        return y;
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}