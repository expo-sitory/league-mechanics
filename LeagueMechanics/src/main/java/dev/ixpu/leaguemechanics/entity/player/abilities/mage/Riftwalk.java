package dev.ixpu.leaguemechanics.entity.player.abilities.mage;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.manager.CombatStateManager;
import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import dev.ixpu.leaguemechanics.manager.StatScalingManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Riftwalk extends ClassAbilities {
    private static final String CFG = "skills.riftwalk.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 3.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 1.0;
    private static final double DEFAULT_RANGE = 10.0;
    private static final double DEFAULT_RADIUS = 3.0;
    private static final double DEFAULT_DAMAGE = 6.0;
    private static final double DEFAULT_DAMAGE_PER_EXTRA_CAST = 3.0;
    private static final double DEFAULT_AP_SCALING = 30.0;
    private static final double DEFAULT_COMBO_WINDOW_SECONDS = 5.0;
    private static final int DEFAULT_EXTRA_HUNGER_PER_CAST = 2;

    private static final double PLAYER_HEIGHT = 1.8;
    private static final double HALF_WIDTH = 0.3;
    private static final double WALL_CLEARANCE = 0.35;
    private static final double SEARCH_STEP = 0.25;
    private static final double MIN_BLINK_DISTANCE = 0.5;
    private static final long FALL_IMMUNITY_MS = 2000L;

    private static final double[] LIFT_ATTEMPTS = {0.0, 0.5, 1.0};
    private static final Particle.DustOptions RIFT = new Particle.DustOptions(Color.fromRGB(160, 60, 255), 1.2f);
    private static final Particle.DustOptions RIFT_LIGHT = new Particle.DustOptions(Color.fromRGB(205, 150, 255), 1.0f);
    private static final int RING_TICKS = 12;

    private record Combo(int count, long lastCast) {}

    private static final Map<UUID, Combo> combos = new ConcurrentHashMap<>();

    public Riftwalk(LeagueMechanics plugin) {
        super(plugin, "riftwalk", "Riftwalk", PlayerClassType.MAGE,
                "passives.mage_riftwalk",
                "leaguemechanics:riftwalk",
                "minecraft:ender_pearl");
    }

    @Override
    public double getMinCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "min-cooldown-seconds", DEFAULT_MIN_COOLDOWN_SECONDS);
    }

    @Override
    public double getCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "cooldown-seconds", DEFAULT_COOLDOWN_SECONDS);
    }

    private double getRange() {
        return plugin.getConfig().getDouble(CFG + "range", DEFAULT_RANGE);
    }

    private double getRadius() {
        return Math.max(1.0, plugin.getConfig().getDouble(CFG + "radius", DEFAULT_RADIUS));
    }

    private double getDamage() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "damage", DEFAULT_DAMAGE));
    }

    private double getDamagePerExtraCast() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "damage-per-extra-cast", DEFAULT_DAMAGE_PER_EXTRA_CAST));
    }

    private double getApScaling() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "ap-scaling", DEFAULT_AP_SCALING));
    }

    private long getComboWindowMs() {
        return (long) (Math.max(0.0, plugin.getConfig().getDouble(CFG + "combo-window-seconds", DEFAULT_COMBO_WINDOW_SECONDS)) * 1000.0);
    }

    private int getExtraHungerPerCast() {
        return Math.max(0, plugin.getConfig().getInt(CFG + "extra-hunger-per-cast", DEFAULT_EXTRA_HUNGER_PER_CAST));
    }

    private boolean requiresPvp() {
        return plugin.getConfig().getBoolean(CFG + "require-pvp", true);
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "§7You teleport to your cursor location up to §f" + trim(getRange()) + " blocks§7 away,§7",
                "§7dealing damage to nearby enemies. Multiple casts in a short§7",
                "§7period cost additional saturation but also deal additional damage."
        );
    }

    @Override
    public boolean cast(Player caster) {
        World world = caster.getWorld();
        Location eye = caster.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        double range = getRange();

        RayTraceResult hit = world.rayTraceBlocks(eye, dir, range, FluidCollisionMode.NEVER, true);
        Vector feet;
        double travel;
        if (hit != null) {
            Vector aim = hit.getHitPosition();
            Vector normal = hit.getHitBlockFace() != null ? hit.getHitBlockFace().getDirection() : new Vector(0, 1, 0);
            travel = aim.distance(eye.toVector());
            if (normal.getY() > 0.5) {
                feet = aim.clone().add(new Vector(0, 0.05, 0));
            } else if (normal.getY() < -0.5) {
                feet = aim.clone().subtract(new Vector(0, PLAYER_HEIGHT + 0.05, 0));
            } else {
                feet = aim.clone().add(normal.clone().multiply(WALL_CLEARANCE))
                        .subtract(new Vector(0, caster.getEyeHeight(), 0));
            }
        } else {
            Vector aim = eye.toVector().add(dir.clone().multiply(range));
            travel = range;
            feet = aim.subtract(new Vector(0, caster.getEyeHeight(), 0));
        }

        Vector destination = findFit(world, feet, dir, travel);
        Location from = caster.getLocation();
        if (destination == null || destination.distance(from.toVector()) < MIN_BLINK_DISTANCE) {
            caster.sendMessage(Component.text("§cRiftwalk: no room to blink there"));
            return false;
        }

        Location target = new Location(world, destination.getX(), destination.getY(), destination.getZ(),
                from.getYaw(), from.getPitch());

        UUID uuid = caster.getUniqueId();
        long now = System.currentTimeMillis();
        Combo previous = combos.get(uuid);
        int count = previous != null && now - previous.lastCast() <= getComboWindowMs() ? previous.count() + 1 : 1;
        int extraHunger = (count - 1) * getExtraHungerPerCast();
        int hungerNeeded = Math.max(getHungerRequired(), getHungerCost()) + extraHunger;
        if (extraHunger > 0 && !bypassesHunger(caster) && caster.getFoodLevel() < hungerNeeded) {
            caster.sendMessage(Component.text("§cRiftwalk: not enough hunger for another cast §7(needs " + hungerNeeded + ")"));
            return false;
        }

        spawnTrail(from.clone().add(0, 1.0, 0), target.clone().add(0, 1.0, 0));
        spawnBurst(from);
        double radius = getRadius();
        new RingAnimation(from, radius, true).runTaskTimer(plugin, 0L, 1L);
        world.playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);

        caster.teleport(target);
        caster.setVelocity(new Vector(0, 0, 0));
        caster.setFallDistance(0f);
        CombatStateManager.getInstance().markDashFallImmunity(caster.getUniqueId(), FALL_IMMUNITY_MS);

        spawnBurst(target);
        new RingAnimation(target, radius, false).runTaskTimer(plugin, 0L, 1L);
        world.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.6f);

        combos.put(uuid, new Combo(count, now));
        if (extraHunger > 0 && !bypassesHunger(caster)) {
            int level = Math.max(0, caster.getFoodLevel() - extraHunger);
            caster.setFoodLevel(level);
            if (caster.getSaturation() > level) caster.setSaturation(level);
        }
        damageNearby(caster, target, radius, count);
        return true;
    }

    private void damageNearby(Player caster, Location at, double radius, int count) {
        double radiusSq = radius * radius;
        boolean requirePvp = requiresPvp();
        PartyManager parties = PartyManager.getInstance();
        double base = getDamage() + (count - 1) * getDamagePerExtraCast();
        double scaled = new StatScalingManager().calculateScaledValue(caster, base, 0.0, getApScaling());

        for (LivingEntity entity : at.getNearbyLivingEntities(radius)) {
            if (entity.equals(caster) || entity instanceof ArmorStand || entity.isDead()) continue;
            if (entity.getLocation().distanceSquared(at) > radiusSq) continue;

            if (entity instanceof Player other) {
                if (other.getGameMode() == GameMode.SPECTATOR || other.getGameMode() == GameMode.CREATIVE) continue;
                if (parties.isSameParty(caster, other)) continue;
                if (requirePvp && !isPvpEnabled(caster, other)) continue;
                KillSourceTracker.getInstance().setSource(other, caster);
            } else {
                entity.setKiller(caster);
            }

            DamageManager damage = new DamageManager(plugin.getStatsManager());
            damage.enableOnlyAP();
            damage.enableItemProc();
            entity.damage(damage.DamageCalculation(caster, entity, 0, 0, 0, scaled));
            entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, entity.getHeight() / 2.0, 0),
                    12, 0.3, 0.4, 0.3, 0, RIFT);
        }
    }

    private boolean isPvpEnabled(Player attacker, Player target) {
        var handler = plugin.getCommandHandler();
        return handler != null && handler.isPvpEnabled(attacker, target);
    }

    private static boolean bypassesHunger(Player player) {
        GameMode mode = player.getGameMode();
        return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
    }

    public static void cleanup(UUID uuid) {
        combos.remove(uuid);
    }

    private static final class RingAnimation extends BukkitRunnable {
        private final Location center;
        private final double ringRadius;
        private final boolean closingIn;
        private int tick = 0;

        RingAnimation(Location center, double ringRadius, boolean closingIn) {
            this.center = center.clone();
            this.ringRadius = ringRadius;
            this.closingIn = closingIn;
        }

        @Override
        public void run() {
            tick++;
            double progress = Math.min(1.0, tick / (double) RING_TICKS);
            double radius = ringRadius * (closingIn ? 1.0 - progress : progress);

            if (radius > 0.05) {
                World world = center.getWorld();
                int points = Math.max(12, (int) Math.round(2 * Math.PI * radius * 3.0));
                double spin = tick * 0.25 * (closingIn ? 1 : -1);
                for (int i = 0; i < points; i++) {
                    double a = spin + i * 2 * Math.PI / points;
                    world.spawnParticle(Particle.DUST,
                            center.getX() + Math.cos(a) * radius, center.getY() + 0.1, center.getZ() + Math.sin(a) * radius,
                            1, 0, 0, 0, 0, (i % 2 == 0) ? RIFT : RIFT_LIGHT);
                }
            }

            if (tick >= RING_TICKS) cancel();
        }
    }

    private Vector findFit(World world, Vector feet, Vector dir, double travel) {
        for (double back = 0.0; back <= travel; back += SEARCH_STEP) {
            Vector base = feet.clone().subtract(dir.clone().multiply(back));
            for (double lift : LIFT_ATTEMPTS) {
                if (fits(world, base.getX(), base.getY() + lift, base.getZ())) {
                    return new Vector(base.getX(), base.getY() + lift, base.getZ());
                }
            }
        }
        return null;
    }

    private boolean fits(World world, double x, double y, double z) {
        if (y < world.getMinHeight()) return false;
        double[] heights = {0.0, 1.0, PLAYER_HEIGHT - 0.01};
        double[] sides = {-HALF_WIDTH, HALF_WIDTH};
        for (double h : heights) {
            for (double dx : sides) {
                for (double dz : sides) {
                    int bx = (int) Math.floor(x + dx);
                    int by = (int) Math.floor(y + h);
                    int bz = (int) Math.floor(z + dz);
                    if (!world.getBlockAt(bx, by, bz).isPassable()) return false;
                }
            }
        }
        return true;
    }

    private void spawnTrail(Location from, Location to) {
        World world = from.getWorld();
        Vector step = to.toVector().subtract(from.toVector());
        double length = step.length();
        if (length < 0.01) return;
        step.normalize().multiply(0.5);
        Location point = from.clone();
        for (double d = 0; d < length; d += 0.5) {
            world.spawnParticle(Particle.DUST, point, 1, 0.05, 0.05, 0.05, 0, RIFT);
            point.add(step);
        }
    }

    private void spawnBurst(Location at) {
        Location body = at.clone().add(0, 1.0, 0);
        at.getWorld().spawnParticle(Particle.PORTAL, body, 30, 0.3, 0.6, 0.3, 0.4);
        at.getWorld().spawnParticle(Particle.DUST, body, 15, 0.4, 0.6, 0.4, 0, RIFT);
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}