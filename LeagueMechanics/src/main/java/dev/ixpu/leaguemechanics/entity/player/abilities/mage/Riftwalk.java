package dev.ixpu.leaguemechanics.entity.player.abilities.mage;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.manager.CombatStateManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;

public class Riftwalk extends ClassAbilities {
    private static final String CFG = "skills.riftwalk.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 30.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 10.0;
    private static final double DEFAULT_RANGE = 10.0;

    private static final double PLAYER_HEIGHT = 1.8;
    private static final double HALF_WIDTH = 0.3;
    private static final double WALL_CLEARANCE = 0.35;
    private static final double SEARCH_STEP = 0.25;
    private static final double MIN_BLINK_DISTANCE = 0.5;
    private static final long FALL_IMMUNITY_MS = 2000L;

    private static final double[] LIFT_ATTEMPTS = {0.0, 0.5, 1.0};
    private static final Particle.DustOptions RIFT = new Particle.DustOptions(Color.fromRGB(160, 60, 255), 1.2f);

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

    @Override
    public List<String> getDescription() {
        return List.of(
                "§7Blink to your cursor location,",
                "§7up to §f" + trim(getRange()) + " blocks§7 away."
        );
    }

    @Override
    public boolean cast(Player caster) {
        World world = caster.getWorld();
        Location eye = caster.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        double range = getRange();

        // Where the crosshair lands, and the feet position that corresponds to it.
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

        spawnTrail(from.clone().add(0, 1.0, 0), target.clone().add(0, 1.0, 0));
        spawnBurst(from);
        world.playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);

        caster.teleport(target);
        caster.setVelocity(new Vector(0, 0, 0));
        caster.setFallDistance(0f);
        CombatStateManager.getInstance().markDashFallImmunity(caster.getUniqueId(), FALL_IMMUNITY_MS);

        spawnBurst(target);
        world.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.6f);
        return true;
    }

    /**
     * Starting from the wanted feet position, tries it (and a couple of small lifts for stairs/ledges),
     * then walks back toward the caster along the look ray until the player's hitbox fits.
     */
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