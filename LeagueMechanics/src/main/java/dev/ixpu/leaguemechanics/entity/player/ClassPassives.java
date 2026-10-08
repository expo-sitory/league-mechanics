package dev.ixpu.leaguemechanics.entity.player;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.debuff.DebuffType;
import dev.ixpu.leaguemechanics.manager.CombatStateManager;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassPassives {
    private static final double TANK_FALL_DAMAGE_MULTIPLIER = 0.5;
    private static final double TANK_SLAM_MIN_FALL_BLOCKS = 15.0;
    private static final double TANK_SLAM_RADIUS = 5.0;
    private static final int TANK_SLAM_SLOW_TICKS = 50;
    private static final double TANK_SLAM_SLOW_STRENGTH = 30.0;
    private static final double SLOW_PERCENT_PER_LEVEL = 20.0;
    private static final double TANK_SLAM_DAMAGE = 3.0;
    private static final double TANK_SLAM_KNOCKBACK = 0.6;
    private static final double TANK_SHIELD_MITIGATION_PERCENT = 90.0;
    private static final int WARDEN_TAUNT_ANGER = 150;

    private static final double MARKSMAN_KNOCKBACK_STRENGTH = 0.5;
    private static final double MARKSMAN_COOLDOWN_MULTIPLIER = 0.5;

    private static final double FIGHTER_PARRY_COOLDOWN_MULTIPLIER = 0.5;

    private static final double SUPPORT_TRANSFER_PERCENT = 0.4;
    private static final long SUPPORT_TRANSFER_COOLDOWN_MS = 4000L;
    private static final double SUPPORT_SPEED_BONUS = 30.0;
    private static final double SUPPORT_SPEED_RANGE = 10.0;
    private static final double SUPPORT_LOW_HEALTH_RATIO = 0.5;

    private static final long MAGE_RECENT_DAMAGE_MS = 5000L;
    private static final long MAGE_IMMUNITY_MS = 3000L;
    private static final long MAGE_COOLDOWN_MS = 30000L;
    private static final double MAGE_DASH_MULTIPLIER = 1.3;
    private static final double MAGE_DOUBLE_JUMP_MULTIPLIER = 1.25;

    private static final long ASSASSIN_RECALL_COOLDOWN_MS = 20000L;
    private static final long ASSASSIN_MARK_CLEAR_OFFSET_MS = 1000L;
    private static final long ASSASSIN_RECALL_FALL_IMMUNITY_MS = 2000L;
    private static final double PLAYER_HEIGHT = 1.8;

    private record ShadowMark(Location location, long expiresAt) {}

    private static final Map<UUID, ShadowMark> assassinMarks = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> assassinRecallCooldownUntil = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> supportTransferCooldown = new ConcurrentHashMap<>();
    private static final Set<UUID> supportSpeedActive = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> mageLastDamage = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> mageImmuneUntil = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> mageCooldownUntil = new ConcurrentHashMap<>();

    private ClassPassives() {}

    private static boolean is(Player player, PlayerClassType type) {
        return player != null && PlayerClass.getCachedPlayerClass(player) == type;
    }

    public static double getMobilityCooldownMultiplier(Player player) {
        return is(player, PlayerClassType.MARKSMAN) ? MARKSMAN_COOLDOWN_MULTIPLIER : 1.0;
    }

    public static double getParryCooldownMultiplier(Player player) {
        return is(player, PlayerClassType.FIGHTER) ? FIGHTER_PARRY_COOLDOWN_MULTIPLIER : 1.0;
    }

    public static boolean skipsMobilityHungerCost(Player player) {
        return is(player, PlayerClassType.FIGHTER);
    }

    public static double getDashStrengthMultiplier(Player player) {
        return is(player, PlayerClassType.MAGE) ? MAGE_DASH_MULTIPLIER : 1.0;
    }

    public static double getDoubleJumpStrengthMultiplier(Player player) {
        return is(player, PlayerClassType.MAGE) ? MAGE_DOUBLE_JUMP_MULTIPLIER : 1.0;
    }

    public static double getShieldMitigationPercent(Player player, double basePercent) {
        return is(player, PlayerClassType.TANK) ? TANK_SHIELD_MITIGATION_PERCENT : basePercent;
    }

    public static double getFallDamageMultiplier(Player player) {
        return is(player, PlayerClassType.TANK) ? TANK_FALL_DAMAGE_MULTIPLIER : 1.0;
    }

    public static double getMovementSpeedBonus(Player player) {
        return supportSpeedActive.contains(player.getUniqueId()) ? SUPPORT_SPEED_BONUS : 0.0;
    }

    public static void onFallLanding(Player player, double fallBlocks) {
        if (!is(player, PlayerClassType.TANK) || fallBlocks < TANK_SLAM_MIN_FALL_BLOCKS || player.isDead()) return;

        LeagueMechanics plugin = LeagueMechanics.getInstance();
        PartyManager parties = PartyManager.getInstance();
        Location center = player.getLocation();

        for (LivingEntity entity : center.getNearbyLivingEntities(TANK_SLAM_RADIUS)) {
            if (entity == player || entity instanceof ArmorStand || entity.isDead()) continue;
            if (entity.getLocation().distanceSquared(center) > TANK_SLAM_RADIUS * TANK_SLAM_RADIUS) continue;

            if (entity instanceof Player other) {
                if (other.getGameMode() == GameMode.CREATIVE || other.getGameMode() == GameMode.SPECTATOR) continue;
                if (parties.isSameParty(player, other)) continue;
                if (!plugin.getCommandHandler().isPvpEnabled(player, other)) continue;
                DebuffManager.getInstance().applyDebuff(other, DebuffType.SLOW, TANK_SLAM_SLOW_TICKS, TANK_SLAM_SLOW_STRENGTH, player);
                KillSourceTracker.getInstance().setSource(other, player);
            } else {
                int amplifier = Math.max(1, (int) Math.round(TANK_SLAM_SLOW_STRENGTH / SLOW_PERCENT_PER_LEVEL)) - 1;
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, TANK_SLAM_SLOW_TICKS, amplifier, false, true));
                entity.setKiller(player);
            }

            entity.damage(TANK_SLAM_DAMAGE);
            if (!entity.isDead()) {
                double dx = player.getLocation().getX() - entity.getLocation().getX();
                double dz = player.getLocation().getZ() - entity.getLocation().getZ();
                if (dx * dx + dz * dz < 1.0E-4) {
                    dx = Math.random() - 0.5;
                    dz = Math.random() - 0.5;
                }
                entity.knockback(TANK_SLAM_KNOCKBACK, dx, dz);
            }
        }

        player.getWorld().playSound(center, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        player.getWorld().spawnParticle(Particle.CLOUD, center, 24, TANK_SLAM_RADIUS / 3.0, 0.1, TANK_SLAM_RADIUS / 3.0, 0.05);
    }

    public static void onMeleeHit(Player attacker, LivingEntity target) {
        if (!is(attacker, PlayerClassType.TANK)) return;
        if (target instanceof Mob mob && !mob.isDead()) {
            taunt(mob, attacker);
        }
    }

    /**
     * Forces a mob to focus the given player. Wardens ignore Mob#setTarget because their target is
     * driven by their anger table, so for them we move the anger onto the taunter instead.
     */
    public static void taunt(Mob mob, Player taunter) {
        if (mob == null || taunter == null || mob.isDead() || !taunter.isOnline()) return;
        if (mob instanceof Warden warden) {
            Entity current = warden.getEntityAngryAt();
            if (current != null && !current.equals(taunter)) {
                warden.clearAnger(current);
            }
            warden.setAnger(taunter, WARDEN_TAUNT_ANGER);
        }
        mob.setTarget(taunter);
    }

    public static void onProjectileHit(Player shooter, LivingEntity target, Vector velocity) {
        if (!is(shooter, PlayerClassType.MARKSMAN) || target.isDead()) return;

        double x = velocity.getX();
        double z = velocity.getZ();
        double length = Math.sqrt(x * x + z * z);
        if (length < 1.0E-4) {
            Vector facing = shooter.getLocation().getDirection();
            x = facing.getX();
            z = facing.getZ();
            length = Math.sqrt(x * x + z * z);
            if (length < 1.0E-4) return;
        }
        double pushX = x / length;
        double pushZ = z / length;

        if (target instanceof Player) {
            target.knockback(MARKSMAN_KNOCKBACK_STRENGTH, -pushX, -pushZ);
            return;
        }

        Vector current = target.getVelocity();
        double pushY = target.isOnGround()
                ? Math.min(0.4, current.getY() / 2.0 + MARKSMAN_KNOCKBACK_STRENGTH)
                : current.getY();
        target.setVelocity(new Vector(
                current.getX() / 2.0 + pushX * MARKSMAN_KNOCKBACK_STRENGTH,
                pushY,
                current.getZ() / 2.0 + pushZ * MARKSMAN_KNOCKBACK_STRENGTH));
    }

    public static boolean refreshSupportSpeed(Player player) {
        UUID uuid = player.getUniqueId();
        boolean shouldBeActive = false;

        if (is(player, PlayerClassType.SUPPORT)) {
            PartyManager parties = PartyManager.getInstance();
            double rangeSquared = SUPPORT_SPEED_RANGE * SUPPORT_SPEED_RANGE;
            for (Entity nearby : player.getNearbyEntities(SUPPORT_SPEED_RANGE, SUPPORT_SPEED_RANGE, SUPPORT_SPEED_RANGE)) {
                if (!(nearby instanceof Player mate) || mate == player || mate.isDead()) continue;
                if (!parties.isSameParty(player, mate)) continue;
                if (mate.getLocation().distanceSquared(player.getLocation()) > rangeSquared) continue;
                if (mate.getHealth() < mate.getMaxHealth() * SUPPORT_LOW_HEALTH_RATIO) {
                    shouldBeActive = true;
                    break;
                }
            }
        }

        boolean wasActive = supportSpeedActive.contains(uuid);
        if (shouldBeActive == wasActive) return false;
        if (shouldBeActive) {
            supportSpeedActive.add(uuid);
        } else {
            supportSpeedActive.remove(uuid);
        }
        return true;
    }

    public static void transferHealth(Player supporter, Player target) {
        if (!is(supporter, PlayerClassType.SUPPORT) || supporter == target) return;
        if (!PartyManager.getInstance().isSameParty(supporter, target)) return;

        UUID uuid = supporter.getUniqueId();
        long now = System.currentTimeMillis();
        long readyAt = supportTransferCooldown.getOrDefault(uuid, 0L);
        if (now < readyAt) {
            return;
        }

        if (dev.ixpu.leaguemechanics.entity.player.abilities.fighter.UndyingRage.isActive(target)) {
            return;
        }

        double multiplier = PlayerStats.getOrCreate(supporter).getEffectiveHealingMultiplier(supporter);
        if (multiplier <= 0) {
            return;
        }

        double missing = target.getMaxHealth() - target.getHealth();
        double amount = Math.min(supporter.getHealth() * SUPPORT_TRANSFER_PERCENT, missing / multiplier);
        if (amount <= 0 || target.isDead()) {
            return;
        }

        supporter.setHealth(supporter.getHealth() - amount);
        target.setHealth(Math.min(target.getMaxHealth(), target.getHealth() + amount * multiplier));
        supportTransferCooldown.put(uuid, now + SUPPORT_TRANSFER_COOLDOWN_MS);

        Location loc = target.getLocation().add(0, 1.0, 0);
        target.getWorld().spawnParticle(Particle.HEART, loc, 6, 0.4, 0.4, 0.4, 0.0);
        target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
        supporter.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, supporter.getLocation().add(0, 1.0, 0), 8, 0.4, 0.5, 0.4, 0.0);
    }

    public static boolean absorbMageDamage(Player player) {
        if (!is(player, PlayerClassType.MAGE)) return false;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (now < mageImmuneUntil.getOrDefault(uuid, 0L)) return true;

        boolean offCooldown = now >= mageCooldownEnd(player);
        boolean notRecentlyDamaged = now - mageLastDamage.getOrDefault(uuid, 0L) >= MAGE_RECENT_DAMAGE_MS;
        if (offCooldown && notRecentlyDamaged) {
            mageImmuneUntil.put(uuid, now + MAGE_IMMUNITY_MS);
            mageCooldownUntil.put(uuid, now + MAGE_IMMUNITY_MS + MAGE_COOLDOWN_MS);

            Location loc = player.getLocation().add(0, 1.0, 0);
            player.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
            return true;
        }

        mageLastDamage.put(uuid, now);
        return false;
    }

    public static void tickMageRing(Player player) {
        if (player.isDead() || !is(player, PlayerClassType.MAGE)) return;
        Long until = mageImmuneUntil.get(player.getUniqueId());
        if (until == null || System.currentTimeMillis() >= until) return;
        spawnMageRing(player.getLocation().add(0, 1.0, 0));
    }

    private static void spawnMageRing(Location center) {
        Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(160, 60, 255), 1.3f);
        int points = 24;
        double radius = 1.1;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            Location point = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            center.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, purple);
        }
    }

    public static void onDash(Player player, Location start, long dashCooldownMs) {
        if (!is(player, PlayerClassType.ASSASSIN)) return;

        long lifetime = dashCooldownMs - ASSASSIN_MARK_CLEAR_OFFSET_MS;
        if (lifetime <= 0) {
            assassinMarks.remove(player.getUniqueId());
            return;
        }

        assassinMarks.put(player.getUniqueId(),
                new ShadowMark(start.clone(), System.currentTimeMillis() + lifetime));

        spawnShadowPuff(start);
        start.getWorld().playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.6f);
    }

    public static boolean hasShadowMark(Player player) {
        return activeShadowMark(player) != null;
    }

    public static boolean recallToShadowMark(Player player) {
        ShadowMark mark = activeShadowMark(player);
        if (mark == null) return false;
        if (assassinRecallReadyAt(player) > System.currentTimeMillis()) return false;
        assassinMarks.remove(player.getUniqueId());

        Location destination = mark.location();
        if (destination.getWorld() == null || !destination.getWorld().equals(player.getWorld())) return false;
        if (!isStandable(destination)) {
            player.sendMessage(net.kyori.adventure.text.Component.text("§cYour shadow mark was obstructed"));
            return false;
        }

        Location from = player.getLocation();
        Location target = destination.clone();
        target.setYaw(from.getYaw());
        target.setPitch(from.getPitch());

        spawnShadowPuff(from);
        player.teleport(target);
        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0f);
        CombatStateManager.getInstance().markDashFallImmunity(player.getUniqueId(), ASSASSIN_RECALL_FALL_IMMUNITY_MS);
        assassinRecallCooldownUntil.put(player.getUniqueId(), System.currentTimeMillis() + ASSASSIN_RECALL_COOLDOWN_MS);

        spawnShadowPuff(target);
        target.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.4f);
        return true;
    }

    public static void tickShadowMark(Player player) {
        ShadowMark mark = activeShadowMark(player);
        if (mark == null) return;

        Location center = mark.location();
        if (center.getWorld() == null || !center.getWorld().equals(player.getWorld())) return;

        boolean ready = assassinRecallReadyAt(player) <= System.currentTimeMillis();
        Particle.DustOptions shade = new Particle.DustOptions(
                ready ? Color.fromRGB(70, 0, 110) : Color.fromRGB(70, 70, 70), 1.2f);
        int points = 12;
        double radius = 0.6;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            player.spawnParticle(Particle.DUST,
                    center.getX() + Math.cos(angle) * radius, center.getY() + 0.1, center.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0, shade);
        }
        player.spawnParticle(Particle.PORTAL, center.getX(), center.getY() + 0.9, center.getZ(), 4, 0.25, 0.7, 0.25, 0.0);
    }

    public static void clearShadowMark(UUID uuid) {
        assassinMarks.remove(uuid);
    }

    private static ShadowMark activeShadowMark(Player player) {
        UUID uuid = player.getUniqueId();
        ShadowMark mark = assassinMarks.get(uuid);
        if (mark == null) return null;
        if (!is(player, PlayerClassType.ASSASSIN) || System.currentTimeMillis() >= mark.expiresAt()) {
            assassinMarks.remove(uuid);
            return null;
        }
        return mark;
    }

    public static double getAssassinRecallCooldownSeconds(Player player) {
        if (!is(player, PlayerClassType.ASSASSIN)) return 0;
        return remainingSeconds(assassinRecallReadyAt(player));
    }

    private static long assassinRecallReadyAt(Player player) {
        Long until = assassinRecallCooldownUntil.get(player.getUniqueId());
        if (until == null) return 0L;
        return until;
    }

    private static boolean isStandable(Location location) {
        for (double offset : new double[]{0.0, 1.0, PLAYER_HEIGHT}) {
            if (!location.clone().add(0, offset, 0).getBlock().isPassable()) return false;
        }
        return true;
    }

    private static void spawnShadowPuff(Location at) {
        Location body = at.clone().add(0, 1.0, 0);
        at.getWorld().spawnParticle(Particle.SQUID_INK, body, 14, 0.3, 0.5, 0.3, 0.02);
        at.getWorld().spawnParticle(Particle.PORTAL, body, 20, 0.3, 0.5, 0.3, 0.3);
    }

    public static double getSupportTransferCooldownSeconds(Player player) {
        if (!is(player, PlayerClassType.SUPPORT)) return 0;
        return remainingSeconds(supportTransferCooldown.get(player.getUniqueId()));
    }

    public static double getMageAbsorbCooldownSeconds(Player player) {
        if (!is(player, PlayerClassType.MAGE)) return 0;
        Long immuneUntil = mageImmuneUntil.get(player.getUniqueId());
        if (immuneUntil != null && System.currentTimeMillis() < immuneUntil) return 0;
        return remainingSeconds(mageCooldownEnd(player));
    }

    private static long mageCooldownEnd(Player player) {
        Long until = mageCooldownUntil.get(player.getUniqueId());
        if (until == null) return 0L;
        return until;
    }

    private static double remainingSeconds(Long readyAt) {
        if (readyAt == null) return 0;
        long remaining = readyAt - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000.0 : 0;
    }

    public static void cleanup(UUID uuid) {
        supportTransferCooldown.remove(uuid);
        supportSpeedActive.remove(uuid);
        mageLastDamage.remove(uuid);
        mageImmuneUntil.remove(uuid);
        mageCooldownUntil.remove(uuid);
        assassinMarks.remove(uuid);
        assassinRecallCooldownUntil.remove(uuid);
    }
}