package dev.ixpu.leaguemechanics.entity.player.abilities.marksman;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class LambsRespite extends ClassAbilities {
    private static final String CFG = "skills.lambs-respite.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_RADIUS = 10.0;
    private static final double DEFAULT_VERTICAL_RANGE = 5.0;
    private static final double DEFAULT_MIN_HEALTH = 1.0;
    private static final double DEFAULT_HEAL_PERCENT = 30.0;

    private static final long TICK_INTERVAL = 4L;
    private static final double EDGE_SPACING = 0.9;
    private static final double[] EDGE_HEIGHTS = {0.1, 0.55, 1.0};
    private static final int SPIRAL_ARMS = 3;
    private static final int SPIRAL_POINTS = 14;
    private static final double SPIRAL_REACH = 0.7;
    private static final double SPIRAL_CURL = 0.5;
    private static final double SPIRAL_SPIN_PER_TICK = 0.25;
    private static final Color DARK_PURPLE = Color.fromRGB(90, 0, 150);
    private static final Color PURPLE = Color.fromRGB(180, 90, 255);
    private static final Color BLUE = Color.fromRGB(85, 85, 255);
    private static final Color GREEN = Color.fromRGB(85, 255, 120);
    private static final Color WHITE = Color.fromRGB(255, 255, 255);
    private static final Particle.DustOptions[] EDGE_COLORS = {
            new Particle.DustOptions(DARK_PURPLE, 1.3f),
            new Particle.DustOptions(PURPLE, 1.3f),
            new Particle.DustOptions(BLUE, 1.3f),
            new Particle.DustOptions(WHITE, 1.2f)
    };
    private static final Particle.DustOptions[] ARM_COLORS = {
            new Particle.DustOptions(PURPLE, 1.2f),
            new Particle.DustOptions(BLUE, 1.2f),
            new Particle.DustOptions(DARK_PURPLE, 1.2f)
    };
    private static final Particle.DustOptions WHITE_DUST = new Particle.DustOptions(WHITE, 1.2f);
    private static final Particle.DustTransition MOTE_GREEN = new Particle.DustTransition(GREEN, WHITE, 1.4f);
    private static final Particle.DustTransition MOTE_PURPLE = new Particle.DustTransition(PURPLE, BLUE, 1.4f);

    private static final class Zone {
        final Location center;
        final double radius;
        final double vertical;
        final long expiresAt;
        final Map<Long, Double> ground = new ConcurrentHashMap<>();

        Zone(Location center, double radius, double vertical, long expiresAt) {
            this.center = center;
            this.radius = radius;
            this.vertical = vertical;
            this.expiresAt = expiresAt;
        }

        boolean contains(Location at) {
            if (at.getWorld() == null || !at.getWorld().equals(center.getWorld())) return false;
            double dx = at.getX() - center.getX();
            double dz = at.getZ() - center.getZ();
            if (dx * dx + dz * dz > radius * radius) return false;
            return Math.abs(at.getY() - center.getY()) <= vertical;
        }

        double groundAt(double x, double z) {
            int bx = (int) Math.floor(x);
            int bz = (int) Math.floor(z);
            long key = (((long) bx) << 32) ^ (bz & 0xFFFFFFFFL);
            return ground.computeIfAbsent(key, k -> {
                World world = center.getWorld();
                int start = (int) Math.floor(center.getY()) + 3;
                int min = Math.max(world.getMinHeight(), (int) Math.floor(center.getY()) - 6);
                for (int by = start; by >= min; by--) {
                    if (!world.getBlockAt(bx, by, bz).isPassable()) return by + 1.0;
                }
                return center.getY();
            });
        }
    }

    private static final Map<UUID, Zone> zones = new ConcurrentHashMap<>();

    public LambsRespite(LeagueMechanics plugin) {
        super(plugin, "lambs-sprite", "Lamb's Respite", PlayerClassType.MARKSMAN,
                "passives.lambs_sprite",
                "leaguemechanics:lambs-sprite",
                "minecraft:white_wool");
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
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.LAMBS_RESPITE);
    }

    private double getRadius() {
        return Math.max(1.0, plugin.getConfig().getDouble(CFG + "radius", DEFAULT_RADIUS));
    }

    private double getHealPercent() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "heal-percent", DEFAULT_HEAL_PERCENT));
    }

    private double getVerticalRange() {
        return Math.max(1.0, plugin.getConfig().getDouble(CFG + "vertical-range", DEFAULT_VERTICAL_RANGE));
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "§7You mark a §f" + trim(getRadius()) + " block§7 circle on the ground for §f" + trim(getEffectSeconds()) + " seconds§7,",
                "§7granting all living things inside the zone a respite from death.§7",
                "§7When it ends, everything inside is healed for §f" + trim(getHealPercent()) + "% max health§7."
        );
    }


    @Override
    public boolean cast(Player caster) {
        double seconds = getEffectSeconds();
        long expiresAt = System.currentTimeMillis() + (long) (seconds * 1000.0);
        Zone zone = new Zone(caster.getLocation().clone(), getRadius(), getVerticalRange(), expiresAt);

        AbilityEffectManager.getInstance().activate(caster, AbilityEffectType.LAMBS_RESPITE);
        zones.put(caster.getUniqueId(), zone);

        World world = zone.center.getWorld();
        world.playSound(zone.center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.6f);
        world.playSound(zone.center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.5f);

        UUID uuid = caster.getUniqueId();
        long startedAt = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (System.currentTimeMillis() >= zone.expiresAt) {
                task.cancel();
                zones.remove(uuid, zone);
                world.playSound(zone.center, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.6f);
                world.playSound(zone.center, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
                healAll(zone);
                drawFinish(zone);
                return;
            }
            double elapsedTicks = (System.currentTimeMillis() - startedAt) / 50.0;
            draw(zone, elapsedTicks * SPIRAL_SPIN_PER_TICK);
        }, 1L, TICK_INTERVAL);
        return true;
    }

    public static void protect(LivingEntity entity, EntityDamageEvent event) {
        if (zones.isEmpty() || !isInside(entity)) return;

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

        double allowed = entity.getHealth() - minHealth;
        double incoming = event.getFinalDamage();
        if (incoming <= allowed) return;

        if (allowed <= 0.0) {
            event.setDamage(0.0);
            return;
        }
        event.setDamage(event.getDamage() * (allowed / incoming));
    }

    public static boolean isInside(LivingEntity entity) {
        long now = System.currentTimeMillis();
        Location at = entity.getLocation();
        for (Zone zone : zones.values()) {
            if (now < zone.expiresAt && zone.contains(at)) return true;
        }
        return false;
    }

    private void healAll(Zone zone) {
        double percent = getHealPercent() / 100.0;
        if (percent <= 0.0) return;
        World world = zone.center.getWorld();
        for (LivingEntity entity : world.getNearbyLivingEntities(zone.center, zone.radius, zone.vertical, zone.radius)) {
            if (entity instanceof ArmorStand || entity.isDead() || !zone.contains(entity.getLocation())) continue;
            if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) continue;
            var attr = entity.getAttribute(Attribute.MAX_HEALTH);
            if (attr == null) continue;
            entity.heal(attr.getValue() * percent);
            Location body = entity.getLocation().add(0, entity.getHeight() / 2.0, 0);
            world.spawnParticle(Particle.HAPPY_VILLAGER, body, 10, 0.3, 0.4, 0.3, 0);
            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, body, 8, 0.3, 0.5, 0.3, 0, MOTE_GREEN);
        }
    }

    private void drawFinish(Zone zone) {
        World world = zone.center.getWorld();
        Location c = zone.center;
        int points = 48;
        double[] scales = {0.35, 0.7, 1.0};
        for (int ringIndex = 0; ringIndex < scales.length; ringIndex++) {
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = c.getX() + Math.cos(angle) * zone.radius * scales[ringIndex];
                double z = c.getZ() + Math.sin(angle) * zone.radius * scales[ringIndex];
                world.spawnParticle(Particle.DUST, x, zone.groundAt(x, z) + 0.2, z, 1, 0, 0, 0, 0,
                        EDGE_COLORS[(ringIndex + i) % EDGE_COLORS.length]);
            }
        }
    }

    private void draw(Zone zone, double spin) {
        World world = zone.center.getWorld();
        Location c = zone.center;
        int shift = (int) Math.floor(spin * 2.0);

        int edgePoints = Math.max(12, (int) Math.round(2 * Math.PI * zone.radius / EDGE_SPACING));
        for (int i = 0; i < edgePoints; i++) {
            double angle = 2 * Math.PI * i / edgePoints;
            double x = c.getX() + Math.cos(angle) * zone.radius;
            double z = c.getZ() + Math.sin(angle) * zone.radius;
            double y = zone.groundAt(x, z);
            for (int j = 0; j < EDGE_HEIGHTS.length; j++) {
                world.spawnParticle(Particle.DUST, x, y + EDGE_HEIGHTS[j], z, 1, 0, 0, 0, 0,
                        EDGE_COLORS[Math.floorMod(i / 2 + j + shift, EDGE_COLORS.length)]);
            }
            if ((i + shift) % 5 == 0) {
                world.spawnParticle(Particle.DUST_COLOR_TRANSITION, x, y + 1.4, z, 1, 0.05, 0.3, 0.05, 0, MOTE_GREEN);
            }
        }

        double reach = zone.radius * SPIRAL_REACH;
        for (int arm = 0; arm < SPIRAL_ARMS; arm++) {
            for (int j = 1; j <= SPIRAL_POINTS; j++) {
                double r = reach * j / SPIRAL_POINTS;
                double angle = spin + arm * (2 * Math.PI / SPIRAL_ARMS) + (r / zone.radius) * SPIRAL_CURL * Math.PI * 2;
                double x = c.getX() + Math.cos(angle) * r;
                double z = c.getZ() + Math.sin(angle) * r;
                Particle.DustOptions color = j == SPIRAL_POINTS ? WHITE_DUST : ARM_COLORS[arm % ARM_COLORS.length];
                world.spawnParticle(Particle.DUST, x, zone.groundAt(x, z) + 0.15, z, 1, 0, 0, 0, 0, color);
            }
        }

        double centerY = zone.groundAt(c.getX(), c.getZ());
        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, c.getX(), centerY + 0.3, c.getZ(), 3, 0.25, 0.15, 0.25, 0, MOTE_GREEN);
        world.spawnParticle(Particle.DUST, c.getX(), centerY + 0.15, c.getZ(), 2, 0.2, 0.05, 0.2, 0, WHITE_DUST);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble(0, Math.PI * 2);
            double distance = Math.sqrt(random.nextDouble()) * zone.radius * 0.9;
            double x = c.getX() + Math.cos(angle) * distance;
            double z = c.getZ() + Math.sin(angle) * distance;
            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, x, zone.groundAt(x, z) + random.nextDouble(0.3, 2.0), z,
                    1, 0.05, 0.1, 0.05, 0, MOTE_PURPLE);
        }
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}