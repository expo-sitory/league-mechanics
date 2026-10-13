package dev.ixpu.leaguemechanics.entity.player.abilities.mage;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.debuff.DebuffType;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PetrifyingGaze extends ClassAbilities {
    private static final String CFG = "skills.petrifying-gaze.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 60.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 15.0;
    private static final double DEFAULT_RANGE = 7.0;
    private static final double DEFAULT_DURATION_SECONDS = 2.5;
    private static final double DEFAULT_CONE_ANGLE = 70.0;
    private static final double DEFAULT_FACING_ANGLE = 60.0;
    private static final double DEFAULT_SLOW_PERCENT = 40.0;

    private static final double SLOW_PERCENT_PER_LEVEL = 20.0;
    private static final int MOB_STUN_SLOW_AMPLIFIER = 6;
    private static final String STUN_TAG = "lm_stunned";

    private static final Map<UUID, Long> stunnedUntil = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> previousAi = new ConcurrentHashMap<>();
    private static final Particle.DustOptions STONE = new Particle.DustOptions(Color.fromRGB(140, 140, 150), 1.3f);
    private static final Particle.DustOptions[] GRAYS = {
            STONE,
            new Particle.DustOptions(Color.fromRGB(185, 185, 195), 1.2f),
            new Particle.DustOptions(Color.fromRGB(95, 95, 105), 1.4f)
    };

    public PetrifyingGaze(LeagueMechanics plugin) {
        super(plugin, "petrifying-gaze", "Petrifying Gaze", PlayerClassType.MAGE,
                "passives.petrifying_gaze",
                "leaguemechanics:petrifying-gaze",
                "minecraft:fermented_spider_eye");
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

    private double getDurationSeconds() {
        return plugin.getConfig().getDouble(CFG + "duration-seconds", DEFAULT_DURATION_SECONDS);
    }

    private double getConeAngle() {
        return clamp(plugin.getConfig().getDouble(CFG + "cone-angle", DEFAULT_CONE_ANGLE), 10.0, 180.0);
    }

    private double getFacingAngle() {
        return clamp(plugin.getConfig().getDouble(CFG + "facing-angle", DEFAULT_FACING_ANGLE), 0.0, 180.0);
    }

    private double getSlowPercent() {
        return Math.max(1.0, plugin.getConfig().getDouble(CFG + "slow-percent", DEFAULT_SLOW_PERCENT));
    }

    private boolean requiresPvp() {
        return plugin.getConfig().getBoolean(CFG + "require-pvp", true);
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "§7You blast enemies in a cone in your target direction,§7",
                "§7slowing them by §f40%§7 for §f" + trim(getDurationSeconds()) + " seconds§7. Enemies facing§7",
                "§7towards you are instead stunned for the same duration."
        );
    }



    @Override
    public boolean cast(Player caster) {
        World world = caster.getWorld();
        Location eye = caster.getEyeLocation();
        Vector look = eye.getDirection().normalize();
        double range = getRange();
        double coneCos = Math.cos(Math.toRadians(getConeAngle() / 2.0));
        double facingCos = Math.cos(Math.toRadians(getFacingAngle()));
        int ticks = Math.max(1, (int) Math.round(getDurationSeconds() * 20.0));
        double slowPercent = getSlowPercent();
        boolean requirePvp = requiresPvp();
        PartyManager parties = PartyManager.getInstance();

        int hits = 0;
        for (Entity entity : caster.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity target) || entity instanceof ArmorStand || target.isDead()) continue;

            if (target instanceof Player other) {
                if (other.getGameMode() == GameMode.SPECTATOR || other.getGameMode() == GameMode.CREATIVE) continue;
                if (parties.isSameParty(caster, other)) continue;
                if (requirePvp && !isPvpEnabled(caster, other)) continue;
            }

            Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
            Vector toTarget = center.toVector().subtract(eye.toVector());
            double distance = toTarget.length();
            if (distance > range || distance < 1.0E-4) continue;
            toTarget.multiply(1.0 / distance);
            if (look.dot(toTarget) < coneCos) continue;
            if (!caster.hasLineOfSight(target)) continue;

            Vector targetFacing = target.getEyeLocation().getDirection().normalize();
            Vector toCaster = eye.toVector().subtract(target.getEyeLocation().toVector()).normalize();
            boolean facingCaster = targetFacing.dot(toCaster) >= facingCos;

            if (target instanceof Player other) {
                DebuffManager debuffs = DebuffManager.getInstance();
                if (facingCaster) {
                    debuffs.applyDebuff(other, DebuffType.STUN, ticks, 0.0, caster);
                } else {
                    debuffs.applyDebuff(other, DebuffType.SLOW, ticks, slowPercent, caster);
                }
            } else {
                applyToEntity(target, facingCaster, ticks, slowPercent);
            }

            world.spawnParticle(Particle.DUST, center, 18, 0.3, 0.4, 0.3, 0, STONE);
            hits++;
        }

        if (hits == 0) {
            caster.sendMessage(Component.text("§cPetrifying Gaze: nothing in view"));
            return false;
        }

        drawCone(caster, eye, look, range);
        world.playSound(eye, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.7f, 1.3f);
        return true;
    }

    private void applyToEntity(LivingEntity target, boolean stun, int ticks, double slowPercent) {
        if (stun) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, MOB_STUN_SLOW_AMPLIFIER, false, true));
            stunEntity(target, ticks);
            return;
        }
        int amplifier = Math.max(1, (int) Math.round(slowPercent / SLOW_PERCENT_PER_LEVEL)) - 1;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, amplifier, false, true));
    }

    public static boolean isStunned(Entity entity) {
        return entity != null && stunnedUntil.containsKey(entity.getUniqueId());
    }

    private void stunEntity(LivingEntity target, int ticks) {
        UUID id = target.getUniqueId();
        long until = System.currentTimeMillis() + ticks * 50L;
        stunnedUntil.put(id, until);
        target.addScoreboardTag(STUN_TAG);
        target.setVelocity(new Vector());
        if (target instanceof Mob mob) {
            previousAi.putIfAbsent(id, mob.hasAI());
            mob.setAI(false);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> releaseStun(target, until), ticks);
    }

    private static void releaseStun(LivingEntity target, long until) {
        UUID id = target.getUniqueId();
        if (!Long.valueOf(until).equals(stunnedUntil.get(id))) return;
        stunnedUntil.remove(id);
        Boolean ai = previousAi.remove(id);
        if (!target.isValid()) return;
        target.removeScoreboardTag(STUN_TAG);
        if (Boolean.TRUE.equals(ai) && target instanceof Mob mob) {
            mob.setAI(true);
        }
    }

    private void drawCone(Player caster, Location eye, Vector look, double range) {
        World world = caster.getWorld();
        double half = Math.toRadians(Math.min(getConeAngle() / 2.0, 85.0));
        Location origin = eye.clone().add(look.clone().multiply(0.5));

        Vector helper = Math.abs(look.getY()) > 0.99 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        Vector right = look.clone().crossProduct(helper).normalize();
        Vector up = right.clone().crossProduct(look).normalize();

        double[] fractions = {0.0, 0.35, 0.7, 1.0};
        int index = 0;
        for (double d = 0.75; d <= range; d += 0.75) {
            double rimRadius = d * Math.tan(half);
            for (double fraction : fractions) {
                double r = rimRadius * fraction;
                int points = fraction == 0.0 ? 1 : Math.max(6, (int) Math.round(2 * Math.PI * r * 1.6));
                double twist = d * 0.9; // offset each ring so the points don't line up into spokes
                for (int i = 0; i < points; i++) {
                    double a = twist + i * 2 * Math.PI / points;
                    Vector offset = right.clone().multiply(Math.cos(a) * r).add(up.clone().multiply(Math.sin(a) * r));
                    Location point = origin.clone().add(look.clone().multiply(d)).add(offset);
                    world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, GRAYS[index++ % GRAYS.length]);
                }
            }
            Location mid = origin.clone().add(look.clone().multiply(d));
            world.spawnParticle(Particle.ASH, mid, 3, rimRadius * 0.5, rimRadius * 0.5, rimRadius * 0.5, 0.0);
        }
    }

    private boolean isPvpEnabled(Player attacker, Player target) {
        var handler = plugin.getCommandHandler();
        return handler != null && handler.isPvpEnabled(attacker, target);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}