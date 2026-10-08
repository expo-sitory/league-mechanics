package dev.ixpu.leaguemechanics.entity.player.abilities.support;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.entity.player.abilities.fighter.UndyingRage;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

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
                "§7Restore §f" + trim(getHealPercent()) + "%§7 of max health to",
                "§7yourself and all partied players."
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

        Location loc = target.getLocation().add(0, 1.0, 0);
        target.getWorld().spawnParticle(Particle.HEART, loc, 10, 0.5, 0.6, 0.5, 0.0);
        target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 12, 0.5, 0.6, 0.5, 0.0);
        target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
        return true;
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}