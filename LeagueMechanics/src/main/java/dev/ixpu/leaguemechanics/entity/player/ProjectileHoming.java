package dev.ixpu.leaguemechanics.entity.player;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.abilities.fighter.HollowedMist;
import dev.ixpu.leaguemechanics.entity.player.abilities.mage.SanguinePool;
import dev.ixpu.leaguemechanics.entity.player.abilities.marksman.SprayAndPray;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Trident;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProjectileHoming {
    private static final boolean DEBUG = true;

    private static final boolean TARGET_MOBS = true;
    private static final int DEBUG_MAX_ENTRIES = 4;

    private static final boolean AIMLOCK = true;
    private static final double STRENGTH = 0.08;
    private static final double RANGE = 15.0;
    private static final double CONE_HALF_ANGLE_DEGREES = 45.0;
    private static final int MAX_LIFETIME_TICKS = 80;
    private static final int SCAN_INTERVAL_TICKS = 3;
    private static final int INITIAL_SCAN_TICKS = 8;
    private static final double MIN_SPEED = 0.2;
    private static final double MIN_STEER_DISTANCE = 1.0;

    private static final double CONE_COS = Math.cos(Math.toRadians(CONE_HALF_ANGLE_DEGREES));

    private static final class Tracked {
        final AbstractArrow arrow;
        final UUID shooter;
        UUID target;
        int age;
        boolean reportedScan;
        boolean reportedLock;

        Tracked(AbstractArrow arrow, UUID shooter) {
            this.arrow = arrow;
            this.shooter = shooter;
        }
    }

    private static final Map<UUID, Tracked> tracked = new ConcurrentHashMap<>();
    private static BukkitTask task;

    private ProjectileHoming() {}

    public static void onLaunch(Projectile projectile) {
        if (!(projectile instanceof AbstractArrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        PlayerClassType type = PlayerClass.getCachedPlayerClass(shooter);
        if (type != PlayerClassType.MARKSMAN && type != PlayerClassType.MAGE) return;
        if (SprayAndPray.isPiercing(arrow)) return;

        tracked.put(arrow.getUniqueId(), new Tracked(arrow, shooter.getUniqueId()));
        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(LeagueMechanics.getInstance(), ProjectileHoming::tick, 1L, 1L);
        }
    }

    private static void debug(Player shooter, String message) {
        if (DEBUG && shooter != null) shooter.sendActionBar(Component.text(message));
    }

    private static void tick() {
        for (Iterator<Tracked> it = tracked.values().iterator(); it.hasNext(); ) {
            Tracked t = it.next();
            if (!step(t)) it.remove();
        }
        if (tracked.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }

    private static boolean step(Tracked t) {
        AbstractArrow arrow = t.arrow;
        if (!arrow.isValid() || arrow.isDead() || arrow.isInBlock() || ++t.age > MAX_LIFETIME_TICKS) return false;
        if (arrow instanceof Trident trident && trident.hasDealtDamage()) return false;
        if (SprayAndPray.isPiercing(arrow)) return false;

        Player shooter = Bukkit.getPlayer(t.shooter);
        if (shooter == null || !shooter.isOnline()) return false;

        Vector velocity = arrow.getVelocity();
        double speed = velocity.length();
        if (speed < MIN_SPEED) return false;
        Vector heading = velocity.clone().multiply(1.0 / speed);

        LivingEntity target = t.target == null ? null
                : (Bukkit.getEntity(t.target) instanceof LivingEntity living ? living : null);
        if (target != null && !stillValid(arrow, shooter, target, t.age)) {
            target = null;
            t.target = null;
        }
        if (target == null && (t.age <= INITIAL_SCAN_TICKS || t.age % SCAN_INTERVAL_TICKS == 0)) {
            StringBuilder report = new StringBuilder();
            target = acquire(arrow, shooter, heading, report);
            t.target = target == null ? null : target.getUniqueId();
            if (target == null && !t.reportedScan) {
                t.reportedScan = true;
            }
        }
        if (target == null) return true;
        if (!t.reportedLock) {
            t.reportedLock = true;
        }

        Location from = arrow.getLocation();
        Vector toTarget = target.getBoundingBox().getCenter().subtract(from.toVector());
        double distance = toTarget.length();
        if (distance < MIN_STEER_DISTANCE) return true;
        toTarget.multiply(1.0 / distance);

        if (AIMLOCK) {
            arrow.setVelocity(toTarget.multiply(speed));
            return true;
        }

        Vector steered = heading.multiply(1.0 - STRENGTH).add(toTarget.multiply(STRENGTH));
        if (steered.lengthSquared() < 1.0E-6) return true;
        arrow.setVelocity(steered.normalize().multiply(speed));
        return true;
    }

    private static boolean stillValid(AbstractArrow arrow, Player shooter, LivingEntity target, int age) {
        if (rejectReason(shooter, target, arrow) != null) return false;
        if (!target.getWorld().equals(arrow.getWorld())) return false;
        if (target.getLocation().distanceSquared(arrow.getLocation()) > RANGE * RANGE) return false;
        return age % SCAN_INTERVAL_TICKS != 0 || hasClearPath(arrow, target);
    }

    private static LivingEntity acquire(AbstractArrow arrow, Player shooter, Vector heading, StringBuilder report) {
        Location from = arrow.getLocation();
        LivingEntity best = null;
        double bestDot = CONE_COS;
        int[] noted = {0};

        for (Entity entity : arrow.getNearbyEntities(RANGE, RANGE, RANGE)) {
            if (!(entity instanceof LivingEntity candidate)) continue;

            String rejected = rejectReason(shooter, candidate, arrow);
            if (rejected != null) {
                if (!(candidate instanceof ArmorStand)) note(report, noted, candidate, rejected);
                continue;
            }

            Vector toCandidate = candidate.getBoundingBox().getCenter().subtract(from.toVector());
            double distance = toCandidate.length();
            if (distance < 1.0E-4 || distance > RANGE) continue;

            double dot = heading.dot(toCandidate.multiply(1.0 / distance));
            if (dot <= CONE_COS) {
                note(report, noted, candidate, "outside " + (int) CONE_HALF_ANGLE_DEGREES + "° cone");
                continue;
            }
            if (!hasClearPath(arrow, candidate)) {
                note(report, noted, candidate, "blocked by terrain");
                continue;
            }
            if (dot <= bestDot) continue;

            best = candidate;
            bestDot = dot;
        }
        return best;
    }

    private static void note(StringBuilder report, int[] noted, LivingEntity entity, String reason) {
        noted[0]++;
        if (noted[0] > DEBUG_MAX_ENTRIES) return;
        if (report.length() > 0) report.append(", ");
        report.append(label(entity)).append(": ").append(reason);
    }

    private static String label(LivingEntity entity) {
        return entity instanceof Player player ? player.getName() : entity.getType().name().toLowerCase();
    }

    private static String rejectReason(Player shooter, LivingEntity target, AbstractArrow arrow) {
        if (target.equals(shooter)) return "you";
        if (target.isDead() || !target.isValid()) return "dead";
        if (target instanceof ArmorStand) return "armor stand";
        if (SprayAndPray.hasPierced(arrow, target)) return "already pierced";

        if (target instanceof Player other) {
            if (!other.isOnline()) return "offline";
            GameMode mode = other.getGameMode();
            if (mode == GameMode.SPECTATOR || mode == GameMode.CREATIVE) return mode.name().toLowerCase() + " mode";
            if (PartyManager.getInstance().isSameParty(shooter, other)) return "party member";

            var handler = LeagueMechanics.getInstance().getCommandHandler();
            if (handler != null && !handler.isPvpEnabled(shooter, other)) return "PvP not enabled for both";
            if (HollowedMist.blocks(other, arrow)) return "protected by Hollowed Mist";
            if (SanguinePool.isActive(other)) return "untargetable";
            return null;
        }

        if (!TARGET_MOBS) return "mobs disabled";
        if (target instanceof Tameable pet && pet.isTamed()) {
            AnimalTamer owner = pet.getOwner();
            if (owner instanceof Player ownerPlayer
                    && (ownerPlayer.equals(shooter) || PartyManager.getInstance().isSameParty(shooter, ownerPlayer))) {
                return "ally pet";
            }
        }
        return null;
    }

    private static boolean hasClearPath(AbstractArrow arrow, LivingEntity target) {
        Location from = arrow.getLocation();
        World world = from.getWorld();
        Vector to = target.getBoundingBox().getCenter().subtract(from.toVector());
        double distance = to.length();
        if (distance < 1.0E-4) return true;
        RayTraceResult hit = world.rayTraceBlocks(from, to.multiply(1.0 / distance), distance,
                FluidCollisionMode.NEVER, true);
        return hit == null;
    }
}