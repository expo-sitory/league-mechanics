package dev.ixpu.leaguemechanics.entity.player.abilities.tank;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.entity.player.abilities.support.SupportEffects;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Locale;

public class CosmicRadiance extends ClassAbilities {
    private static final String CFG = "skills.cosmic-radiance.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_INVULNERABLE_SECONDS = 2.0;
    private static final double DEFAULT_RADIUS = 5.0;
    private static final double DEFAULT_ABSORPTION_HEARTS = 40.0;

    private static final Particle.DustOptions WHITE = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.2f);
    private static final Particle.DustOptions WHITE_BIG = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 2.2f);
    private static final Particle.DustOptions BLUE = new Particle.DustOptions(Color.fromRGB(80, 160, 255), 1.3f);
    private static final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(170, 90, 255), 1.1f);

    private final NamespacedKey absorptionKey;

    public CosmicRadiance(LeagueMechanics plugin) {
        super(plugin, "cosmic-radiance", "Cosmic Radiance", PlayerClassType.TANK,
                "passives.cosmic_radiance",
                "leaguemechanics:cosmic-radiance",
                "minecraft:nether_star");
        this.absorptionKey = new NamespacedKey(plugin, "cosmic_radiance_absorption");
    }

    @Override
    public double getMinCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "min-cooldown-seconds", DEFAULT_MIN_COOLDOWN_SECONDS);
    }

    @Override
    public double getCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "cooldown-seconds", DEFAULT_COOLDOWN_SECONDS);
    }

    private double getWindupSeconds() {
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.COSMIC_RADIANCE);
    }

    private double getInvulnerableSeconds() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "invulnerable-seconds", DEFAULT_INVULNERABLE_SECONDS));
    }

    @Override
    public double getEffectSeconds() {
        return getWindupSeconds() + getInvulnerableSeconds();
    }

    private double getRadius() {
        return Math.max(1.0, plugin.getConfig().getDouble(CFG + "radius", DEFAULT_RADIUS));
    }

    private double getAbsorptionHearts() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "absorption-hearts", DEFAULT_ABSORPTION_HEARTS));
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "§7You pulse cosmic energy, shielding yourself and partied",
                "§7players within §f" + trim(getRadius()) + " blocks§7. After a §f" + trim(getWindupSeconds()) + " second§7 delay,§7",
                "§7everyone inside becomes §finvulnerable§7 for §f" + trim(getInvulnerableSeconds()) + " seconds§7."
        );
    }


    @Override
    public boolean cast(Player caster) {
        double radius = getRadius();
        List<Player> targets = SupportEffects.partyInRange(caster, radius);
        AbilityEffectManager effects = AbilityEffectManager.getInstance();

        int windupTicks = Math.max(1, (int) Math.round(getWindupSeconds() * 20.0));
        double invulnerableSeconds = getInvulnerableSeconds();
        int invulnerableTicks = Math.max(0, (int) Math.round(invulnerableSeconds * 20.0));
        double hearts = getAbsorptionHearts();

        for (Player target : targets) {
            effects.activate(target, AbilityEffectType.COSMIC_RADIANCE);
            SupportEffects.grantAbsorption(target, absorptionKey, hearts);
            target.getWorld().playSound(target.getLocation().add(0, 1.0, 0), Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.6f);
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.6f);

        new RadiantAura(caster, radius, windupTicks).runTaskTimer(plugin, 1L, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player target : targets) {
                if (!target.isOnline() || target.isDead()) continue;
                effects.activate(target, AbilityEffectType.INVULNERABLE, invulnerableSeconds);
                target.getWorld().playSound(target.getLocation().add(0, 1.0, 0), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
            }
            if (invulnerableTicks > 0) {
                new InvulnerableRings(targets, invulnerableTicks).runTaskTimer(plugin, 0L, 2L);
            }
        }, windupTicks);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player target : targets) {
                if (target.isOnline()) SupportEffects.revokeAbsorption(target, absorptionKey);
            }
        }, windupTicks + invulnerableTicks);
        return true;
    }

    private static final class RadiantAura extends BukkitRunnable {
        private static final double TWO_PI = Math.PI * 2.0;
        private static final double ORB_START_HEIGHT = 3.0;

        private final Player caster;
        private final double radius;
        private final int totalTicks;
        private int elapsed = 0;

        RadiantAura(Player caster, double radius, int totalTicks) {
            this.caster = caster;
            this.radius = radius;
            this.totalTicks = totalTicks;
        }

        @Override
        public void run() {
            if (!caster.isOnline() || caster.isDead() || elapsed >= totalTicks) {
                cancel();
                return;
            }

            Location center = SupportEffects.groundedCenter(caster);
            World world = center.getWorld();
            double y = center.getY() + 0.12;
            double progress = elapsed / (double) totalTicks;

            if (elapsed % 4 == 0) {
                drawEdge(world, center, y);
            }
            if (elapsed % 2 == 0) {
                drawClosingRing(world, center, y, radius * (1.0 - progress));
            }
            drawOrb(world, progress);
            elapsed++;
        }

        private void drawEdge(World world, Location c, double y) {
            int points = Math.max(24, (int) Math.round(radius * 8));
            for (int i = 0; i < points; i++) {
                double a = i * TWO_PI / points;
                world.spawnParticle(Particle.DUST, c.getX() + Math.cos(a) * radius, y, c.getZ() + Math.sin(a) * radius,
                        1, 0, 0, 0, 0, i % 2 == 0 ? WHITE : BLUE);
            }
        }

        private void drawClosingRing(World world, Location c, double y, double r) {
            if (r < 0.2) return;
            int points = Math.max(8, (int) Math.round(r * 7));
            for (int i = 0; i < points; i++) {
                double a = i * TWO_PI / points;
                world.spawnParticle(Particle.DUST, c.getX() + Math.cos(a) * r, y, c.getZ() + Math.sin(a) * r,
                        1, 0, 0, 0, 0, i % 2 == 0 ? WHITE : PURPLE);
            }
        }

        private void drawOrb(World world, double progress) {
            Location base = caster.getLocation();
            double y = base.getY() + caster.getHeight() + ORB_START_HEIGHT * (1.0 - progress);
            double x = base.getX();
            double z = base.getZ();
            world.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, WHITE_BIG);
            double d = 0.2;
            world.spawnParticle(Particle.DUST, x + d, y, z, 1, 0, 0, 0, 0, WHITE);
            world.spawnParticle(Particle.DUST, x - d, y, z, 1, 0, 0, 0, 0, WHITE);
            world.spawnParticle(Particle.DUST, x, y + d, z, 1, 0, 0, 0, 0, WHITE);
            world.spawnParticle(Particle.DUST, x, y - d, z, 1, 0, 0, 0, 0, WHITE);
            world.spawnParticle(Particle.DUST, x, y, z + d, 1, 0, 0, 0, 0, WHITE);
            world.spawnParticle(Particle.DUST, x, y, z - d, 1, 0, 0, 0, 0, WHITE);
        }
    }

    private static final class InvulnerableRings extends BukkitRunnable {
        private static final double TWO_PI = Math.PI * 2.0;
        private static final int POINTS = 24;
        private static final double RING_RADIUS = 1.1;

        private final List<Player> targets;
        private final int totalTicks;
        private int elapsed = 0;

        InvulnerableRings(List<Player> targets, int totalTicks) {
            this.targets = targets;
            this.totalTicks = totalTicks;
        }

        @Override
        public void run() {
            if (elapsed >= totalTicks) {
                cancel();
                return;
            }
            AbilityEffectManager effects = AbilityEffectManager.getInstance();
            for (Player target : targets) {
                if (!target.isOnline() || target.isDead() || !effects.isInvulnerable(target)) continue;
                Location c = target.getLocation().add(0, 1.0, 0);
                for (int i = 0; i < POINTS; i++) {
                    double a = i * TWO_PI / POINTS;
                    target.getWorld().spawnParticle(Particle.DUST,
                            c.getX() + Math.cos(a) * RING_RADIUS, c.getY(), c.getZ() + Math.sin(a) * RING_RADIUS,
                            1, 0, 0, 0, 0, BLUE);
                }
            }
            elapsed += 2;
        }
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}