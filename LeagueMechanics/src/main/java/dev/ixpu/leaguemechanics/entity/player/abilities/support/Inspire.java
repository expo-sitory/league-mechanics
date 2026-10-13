package dev.ixpu.leaguemechanics.entity.player.abilities.support;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
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
import java.util.concurrent.ThreadLocalRandom;

public class Inspire extends ClassAbilities {
    private static final String CFG = "skills.inspire.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_RADIUS = 7.0;
    private static final double DEFAULT_ABSORPTION_HEARTS = 20.0;

    private static final Particle.DustOptions DARK_GREEN = new Particle.DustOptions(Color.fromRGB(0, 100, 20), 1.3f);
    private static final Particle.DustOptions DARK_GREEN_THIN = new Particle.DustOptions(Color.fromRGB(0, 100, 20), 0.9f);
    private static final Particle.DustOptions WHITE = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);

    private final NamespacedKey absorptionKey;

    public Inspire(LeagueMechanics plugin) {
        super(plugin, "inspire", "Inspire", PlayerClassType.SUPPORT,
                "passives.inspire",
                "leaguemechanics:inspire",
                "minecraft:goat_horn");
        this.absorptionKey = new NamespacedKey(plugin, "inspire_absorption");
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
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.INSPIRE);
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
                "§7You summon a protective shield for yourself and partied players within§7",
                "§f" + trim(getRadius()) + " blocks§7, granting increased durability, §f+"
                        + trim(AbilityEffectManager.getInstance().getConfiguredInspireMovementSpeed()) + "% movement speed§7, and",
                "§f+" + trim(AbilityEffectManager.getInstance().getConfiguredInspireAttackSpeed()) + "% attack speed for §f"
                        + trim(getEffectSeconds()) + " seconds§7."
        );
    }


    @Override
    public boolean cast(Player caster) {
        List<Player> targets = SupportEffects.partyInRange(caster, getRadius());
        int ticks = Math.max(1, (int) Math.round(getEffectSeconds() * 20.0));
        double hearts = getAbsorptionHearts();

        for (Player target : targets) {
            AbilityEffectManager.getInstance().activate(target, AbilityEffectType.INSPIRE);
            SupportEffects.grantAbsorption(target, absorptionKey, hearts);
            plugin.getPlayerEventListener().applyPlayerStats(target);

            Location loc = target.getLocation().add(0, 1.0, 0);
            target.getWorld().spawnParticle(Particle.FIREWORK, loc, 15, 0.4, 0.7, 0.4, 0.05);
            target.getWorld().playSound(loc, Sound.EVENT_RAID_HORN, 0.5f, 1.6f);
        }

        playWindCast(caster.getLocation().add(0, 1.0, 0));
        new TomoeAura(caster, getRadius(), ticks).runTaskTimer(plugin, 1L, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player target : targets) {
                if (target.isOnline()) SupportEffects.revokeAbsorption(target, absorptionKey);
            }
        }, ticks);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player target : targets) {
                if (target.isOnline()) plugin.getPlayerEventListener().applyPlayerStats(target);
            }
        }, ticks + 2L);
        return true;
    }

    private static void playWindCast(Location at) {
        World world = at.getWorld();
        world.playSound(at, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.9f);
        world.playSound(at, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.8f, 1.2f);
        world.playSound(at, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 0.6f);
        world.playSound(at, Sound.ITEM_ELYTRA_FLYING, 0.7f, 1.3f);
    }

    private static void playWindGust(Location at) {
        World world = at.getWorld();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        switch (rnd.nextInt(3)) {
            case 0 -> world.playSound(at, Sound.ENTITY_BREEZE_IDLE_AIR, 0.5f, 0.6f + rnd.nextFloat() * 0.3f);
            case 1 -> world.playSound(at, Sound.ENTITY_PHANTOM_FLAP, 0.3f, 0.5f + rnd.nextFloat() * 0.3f);
            default -> world.playSound(at, Sound.ITEM_ELYTRA_FLYING, 0.25f, 0.7f + rnd.nextFloat() * 0.3f);
        }
    }

    private static final class TomoeAura extends BukkitRunnable {
        private static final double TWO_PI = Math.PI * 2.0;
        private static final double SPIN_PER_TICK = 0.15;

        private final Player caster;
        private final double radius;
        private final int totalTicks;
        private final double orbit;
        private final double head;
        private int elapsed = 0;

        TomoeAura(Player caster, double radius, int totalTicks) {
            this.caster = caster;
            this.radius = radius;
            this.totalTicks = totalTicks;
            this.orbit = radius * 0.55;
            this.head = Math.max(0.35, Math.min(0.9, radius * 0.085));
        }

        @Override
        public void run() {
            if (!caster.isOnline() || caster.isDead() || elapsed >= totalTicks) {
                if (caster.isOnline() && elapsed >= totalTicks) {
                    caster.getWorld().playSound(caster.getLocation().add(0, 1.0, 0), Sound.ENTITY_BREEZE_IDLE_AIR, 0.7f, 0.5f);
                }
                cancel();
                return;
            }

            Location center = SupportEffects.groundedCenter(caster);
            World world = center.getWorld();
            double y = center.getY() + 0.12;

            if (elapsed % 12 == 0) {
                playWindGust(caster.getLocation().add(0, 1.0, 0));
            }

            if (elapsed % 4 == 0) {
                drawCircle(world, center, y, radius, Math.max(24, (int) Math.round(radius * 7)), DARK_GREEN);
                drawCircle(world, center, y, orbit, Math.max(16, (int) Math.round(orbit * 6)), DARK_GREEN_THIN);
            }
            if (elapsed % 2 == 0) {
                drawTomoe(world, center, y, elapsed * SPIN_PER_TICK);
            }
            elapsed++;
        }

        private void drawCircle(World world, Location c, double y, double r, int points, Particle.DustOptions dust) {
            for (int i = 0; i < points; i++) {
                double a = i * TWO_PI / points;
                world.spawnParticle(Particle.DUST, c.getX() + Math.cos(a) * r, y, c.getZ() + Math.sin(a) * r,
                        1, 0, 0, 0, 0, dust);
            }
        }

        private void drawTomoe(World world, Location c, double y, double spin) {
            double tailStart = head / orbit;
            for (int k = 0; k < 3; k++) {
                double theta = spin + k * TWO_PI / 3.0;
                double hx = c.getX() + Math.cos(theta) * orbit;
                double hz = c.getZ() + Math.sin(theta) * orbit;

                world.spawnParticle(Particle.DUST, hx, y, hz, 1, 0, 0, 0, 0, WHITE);
                for (int j = 0; j < 6; j++) {
                    double a = j * TWO_PI / 6.0;
                    world.spawnParticle(Particle.DUST, hx + Math.cos(a) * head, y, hz + Math.sin(a) * head,
                            1, 0, 0, 0, 0, WHITE);
                }

                int steps = 12;
                for (int i = 1; i <= steps; i++) {
                    double t = i / (double) steps;
                    if (i % (1 + (int) (t * 2.5)) != 0) continue;
                    double angle = theta - tailStart - t * 1.2;
                    double r = orbit + head * 0.5 * (1.0 - 2.0 * t);
                    world.spawnParticle(Particle.DUST, c.getX() + Math.cos(angle) * r, y,
                            c.getZ() + Math.sin(angle) * r, 1, 0, 0, 0, 0, WHITE);
                }
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