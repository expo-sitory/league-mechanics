package dev.ixpu.leaguemechanics.entity.player.abilities.tank;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.debuff.DebuffType;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.entity.player.abilities.support.SupportEffects;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class SeismicBastion extends ClassAbilities {
    private static final String CFG = "skills.seismic-bastion.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_RADIUS = 10.0;
    private static final double DEFAULT_ABSORPTION_HEARTS = 40.0;
    private static final double DEFAULT_SLOW_PERCENT = 40.0;

    private static final double SLOW_PERCENT_PER_LEVEL = 20.0;
    private static final int NAUSEA_AMPLIFIER = 1;

    private final NamespacedKey absorptionKey;

    public SeismicBastion(LeagueMechanics plugin) {
        super(plugin, "seismic-bastion", "Seismic Bastion", PlayerClassType.TANK,
                "passives.seismic_field",
                "leaguemechanics:seismic-field",
                "minecraft:mace");
        this.absorptionKey = new NamespacedKey(plugin, "seismic_bastion_absorption");
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
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.SEISMIC_BASTION);
    }

    private double getRadius() {
        return Math.max(1.0, plugin.getConfig().getDouble(CFG + "radius", DEFAULT_RADIUS));
    }

    private double getAbsorptionHearts() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "absorption-hearts", DEFAULT_ABSORPTION_HEARTS));
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
                "§7You gain a shield and create an earthquake within §f" + trim(getRadius()) + " blocks§7,",
                "§7damaging and slowing foes by inflicting §fNausea II§7 for §f"
                        + trim(getEffectSeconds()) + " seconds§7."
        );
    }


    @Override
    public boolean cast(Player caster) {
        double radius = getRadius();
        double radiusSq = radius * radius;
        int ticks = Math.max(1, (int) Math.round(getEffectSeconds() * 20.0));
        double slowPercent = getSlowPercent();
        int slowAmplifier = Math.max(1, (int) Math.round(slowPercent / SLOW_PERCENT_PER_LEVEL)) - 1;
        boolean requirePvp = requiresPvp();
        PartyManager parties = PartyManager.getInstance();
        AbilityEffectManager effects = AbilityEffectManager.getInstance();
        Location origin = caster.getLocation();

        effects.activate(caster, AbilityEffectType.SEISMIC_BASTION);
        SupportEffects.grantAbsorption(caster, absorptionKey, getAbsorptionHearts());

        for (Entity entity : caster.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target) || entity instanceof ArmorStand || target.isDead()) continue;
            if (target.getLocation().distanceSquared(origin) > radiusSq) continue;

            if (target instanceof Player other) {
                if (other.getGameMode() == GameMode.SPECTATOR || other.getGameMode() == GameMode.CREATIVE) continue;
                if (parties.isSameParty(caster, other)) continue;
                if (requirePvp && !isPvpEnabled(caster, other)) continue;
                DebuffManager.getInstance().applyDebuff(other, DebuffType.SLOW, ticks, slowPercent, caster);
                if (!effects.isCrowdControlImmune(other)) {
                    other.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, ticks, NAUSEA_AMPLIFIER, false, false));
                }
            } else {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, slowAmplifier, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, ticks, NAUSEA_AMPLIFIER, false, false));
            }
        }

        Location center = SupportEffects.groundedCenter(caster);
        World world = center.getWorld();
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
        world.playSound(center, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.0f, 0.6f);
        new Earthquake(center, radius, ticks).runTaskTimer(plugin, 0L, 2L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (caster.isOnline()) SupportEffects.revokeAbsorption(caster, absorptionKey);
        }, ticks);
        return true;
    }

    private boolean isPvpEnabled(Player attacker, Player target) {
        var handler = plugin.getCommandHandler();
        return handler != null && handler.isPvpEnabled(attacker, target);
    }

    private record Ground(Location location, BlockData data) {}

    private record CrackPoint(Ground ground, double distance) {}

    private static Ground groundAt(World world, double x, double y, double z) {
        RayTraceResult hit = world.rayTraceBlocks(new Location(world, x, y + 1.5, z), new Vector(0, -1, 0),
                4.0, FluidCollisionMode.NEVER, true);
        if (hit == null || hit.getHitBlock() == null) return null;
        return new Ground(new Location(world, x, hit.getHitPosition().getY(), z), hit.getHitBlock().getBlockData());
    }

    private static final class Earthquake extends BukkitRunnable {
        private static final double TWO_PI = Math.PI * 2.0;
        private static final int WAVE_TICKS = 12;
        private static final int CRACK_LINES = 8;
        private static final double CRACK_STEP = 0.8;
        private static final int RUMBLE_POINTS = 6;
        private static final Particle.DustOptions CRACK = new Particle.DustOptions(Color.fromRGB(60, 45, 35), 1.2f);
        private static final Particle.DustOptions EDGE = new Particle.DustOptions(Color.fromRGB(130, 90, 50), 1.3f);

        private final Location center;
        private final World world;
        private final double radius;
        private final int totalTicks;
        private final List<CrackPoint> cracks = new ArrayList<>();
        private int elapsed = 0;

        Earthquake(Location center, double radius, int totalTicks) {
            this.center = center;
            this.world = center.getWorld();
            this.radius = radius;
            this.totalTicks = totalTicks;
            buildCracks();
        }

        private void buildCracks() {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int line = 0; line < CRACK_LINES; line++) {
                double angle = line * TWO_PI / CRACK_LINES + random.nextDouble(-0.25, 0.25);
                for (double d = 1.0; d <= radius; d += CRACK_STEP) {
                    angle += random.nextDouble(-0.12, 0.12);
                    Ground ground = groundAt(world, center.getX() + Math.cos(angle) * d, center.getY(),
                            center.getZ() + Math.sin(angle) * d);
                    if (ground != null) cracks.add(new CrackPoint(ground, d));
                }
            }
        }

        @Override
        public void run() {
            if (elapsed >= totalTicks) {
                cancel();
                return;
            }

            double waveRadius = radius * Math.min(1.0, elapsed / (double) WAVE_TICKS);
            if (elapsed <= WAVE_TICKS) drawWave(waveRadius);
            if (elapsed % 4 == 0) {
                drawCracks(waveRadius);
                rumble();
            }
            if (elapsed % 10 == 0) drawEdge();
            if (elapsed % 12 == 0) {
                world.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 0.9f, 0.5f);
            }
            elapsed += 2;
        }

        private void drawWave(double r) {
            if (r < 0.5) return;
            int points = Math.max(16, (int) Math.round(r * 6));
            for (int i = 0; i < points; i++) {
                double a = i * TWO_PI / points;
                Ground ground = groundAt(world, center.getX() + Math.cos(a) * r, center.getY(), center.getZ() + Math.sin(a) * r);
                if (ground == null) continue;
                Location at = ground.location().add(0, 0.15, 0);
                world.spawnParticle(Particle.BLOCK, at, 3, 0.2, 0.1, 0.2, 0.08, ground.data());
            }
        }

        private void drawCracks(double waveRadius) {
            for (CrackPoint point : cracks) {
                if (point.distance() > waveRadius) continue;
                Location at = point.ground().location().clone().add(0, 0.1, 0);
                world.spawnParticle(Particle.DUST, at, 1, 0.05, 0, 0.05, 0, CRACK);
                world.spawnParticle(Particle.BLOCK, at, 1, 0.1, 0.05, 0.1, 0.03, point.ground().data());
            }
        }

        private void rumble() {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < RUMBLE_POINTS; i++) {
                double angle = random.nextDouble(0, TWO_PI);
                double distance = Math.sqrt(random.nextDouble()) * radius;
                Ground ground = groundAt(world, center.getX() + Math.cos(angle) * distance, center.getY(),
                        center.getZ() + Math.sin(angle) * distance);
                if (ground == null) continue;
                Location at = ground.location().add(0, 0.2, 0);
                world.spawnParticle(Particle.BLOCK, at, 8, 0.3, 0.15, 0.3, 0.12, ground.data());
            }
        }

        private void drawEdge() {
            int points = Math.max(24, (int) Math.round(radius * 6));
            double y = center.getY() + 0.12;
            for (int i = 0; i < points; i++) {
                double a = i * TWO_PI / points;
                world.spawnParticle(Particle.DUST, center.getX() + Math.cos(a) * radius, y,
                        center.getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0, EDGE);
            }
        }
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}