package dev.ixpu.leaguemechanics.entity.player.abilities.support;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.entity.player.abilities.fighter.UndyingRage;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Wish extends ClassAbilities {
    private static final String CFG = "skills.wish.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_HEAL_PERCENT = 70.0;
    private static final double DEFAULT_RANGE = 0.0;

    public Wish(LeagueMechanics plugin) {
        super(plugin, "wish", "Wish", PlayerClassType.SUPPORT,
                "passives.soraka_wish",
                "leaguemechanics:wish",
                "minecraft:golden_apple");
    }

    @Override
    public double getMinCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "min-cooldown-seconds", DEFAULT_MIN_COOLDOWN_SECONDS);
    }

    @Override
    public double getCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "cooldown-seconds", DEFAULT_COOLDOWN_SECONDS);
    }

    private double getHealPercent() {
        return plugin.getConfig().getDouble(CFG + "heal-percent", DEFAULT_HEAL_PERCENT);
    }

    private double getRange() {
        return plugin.getConfig().getDouble(CFG + "range", DEFAULT_RANGE);
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "§7You fill your allies with hope, instantly restoring§7",
                "§f" + trim(getHealPercent()) + "% max health§7 to yourself and all partied players."
        );
    }


    @Override
    public boolean cast(Player caster) {
        double fraction = Math.max(0.0, getHealPercent()) / 100.0;
        double range = getRange();
        PartyManager parties = PartyManager.getInstance();

        List<Player> targets = new ArrayList<>();
        targets.add(caster);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(caster) || other.isDead()) continue;
            if (other.getGameMode() == GameMode.SPECTATOR) continue;
            if (!parties.isSameParty(caster, other)) continue;
            if (range > 0 && (!other.getWorld().equals(caster.getWorld())
                    || other.getLocation().distanceSquared(caster.getLocation()) > range * range)) continue;
            targets.add(other);
        }

        boolean healedAnyone = false;
        for (Player target : targets) {
            if (heal(target, fraction)) healedAnyone = true;
        }

        if (!healedAnyone) {
            caster.sendMessage(Component.text("§cWish: nobody needs healing"));
            return false;
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.6f);
        return true;
    }

    private boolean heal(Player target, double fraction) {
        AttributeInstance maxAttr = target.getAttribute(Attribute.MAX_HEALTH);
        if (maxAttr == null) return false;

        double max = maxAttr.getValue();
        double current = target.getHealth();
        if (current >= max) return false;
        if (UndyingRage.isActive(target)) return false;

        double multiplier = PlayerStats.getOrCreate(target).getEffectiveHealingMultiplier(target);
        double amount = max * fraction * multiplier;
        target.setHealth(Math.min(max, current + amount));

        Location sky = target.getLocation().add(0, target.getHeight() + WishFall.GAP, 0);
        target.getWorld().playSound(sky, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
        new WishFall(target).runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    private static final class WishFall extends BukkitRunnable {
        static final double GAP = 5.0;
        private static final int FALL_TICKS = 15;
        private static final double TWO_PI = Math.PI * 2.0;
        private static final Particle.DustOptions[] STRANDS = {
                new Particle.DustOptions(Color.fromRGB(0, 170, 40), 1.3f),     // green
                new Particle.DustOptions(Color.fromRGB(140, 255, 40), 1.3f),   // lime
                new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.1f)   // white
        };

        private final Player target;
        private int tick = 0;

        WishFall(Player target) {
            this.target = target;
        }

        @Override
        public void run() {
            if (!target.isOnline() || target.isDead()) {
                cancel();
                return;
            }

            World world = target.getWorld();
            Location base = target.getLocation();
            double height = target.getHeight();
            double travel = GAP + height * 0.55; // ends around chest height

            tick++;
            double progress = Math.min(1.0, tick / (double) FALL_TICKS);

            for (int trail = 0; trail < 4; trail++) {
                double p = progress - trail * 0.05;
                if (p < 0) continue;
                double fall = p * p; // speeds up as it drops
                double y = base.getY() + height + GAP - travel * fall;
                double radius = 0.55 * (1.0 - 0.6 * p);
                for (int k = 0; k < STRANDS.length; k++) {
                    double angle = p * Math.PI * 6.0 + k * TWO_PI / STRANDS.length;
                    world.spawnParticle(Particle.DUST,
                            base.getX() + Math.cos(angle) * radius, y, base.getZ() + Math.sin(angle) * radius,
                            1, 0, 0, 0, 0, STRANDS[k]);
                }
            }

            if (tick >= FALL_TICKS) {
                impact(world, base);
                cancel();
            }
        }

        private void impact(World world, Location base) {
            Location chest = base.clone().add(0, 1.0, 0);
            world.spawnParticle(Particle.HAPPY_VILLAGER, chest, 14, 0.5, 0.6, 0.5, 0.0);
            world.spawnParticle(Particle.HEART, chest, 6, 0.5, 0.6, 0.5, 0.0);

            int points = 20;
            for (int i = 0; i < points; i++) {
                double a = i * TWO_PI / points;
                world.spawnParticle(Particle.DUST, base.getX() + Math.cos(a) * 1.1, base.getY() + 0.1,
                        base.getZ() + Math.sin(a) * 1.1, 1, 0, 0, 0, 0, STRANDS[1 + (i % 2)]);
            }
            world.playSound(chest, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
        }
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}