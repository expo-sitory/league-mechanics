package dev.ixpu.leaguemechanics.entity.player.abilities.assassin;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.debuff.DebuffType;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.entity.player.abilities.fighter.UndyingRage;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Meditate extends ClassAbilities {
    private static final String CFG = "skills.meditate.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 60.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_HEAL_PERCENT = 5.0;
    private static final int DEFAULT_HEAL_INTERVAL_TICKS = 20;

    private static final Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(30, 200, 70), 1.3f);
    private static final Particle.DustOptions LIME = new Particle.DustOptions(Color.fromRGB(150, 255, 60), 1.2f);
    private static final Particle.DustOptions PALE_LIME = new Particle.DustOptions(Color.fromRGB(210, 255, 150), 1.0f);

    private final Map<UUID, Session> sessions = new HashMap<>();

    private static final class Session {
        BukkitTask task;
        int elapsed = 0;
    }

    public Meditate(LeagueMechanics plugin) {
        super(plugin, "meditate", "Meditate", PlayerClassType.ASSASSIN,
                "passives.meditate",
                "leaguemechanics:meditate",
                "minecraft:amethyst_shard");
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
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.MEDITATE);
    }

    private double getHealPercent() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "heal-percent", DEFAULT_HEAL_PERCENT));
    }

    private int getHealIntervalTicks() {
        return Math.max(1, plugin.getConfig().getInt(CFG + "heal-interval-ticks", DEFAULT_HEAL_INTERVAL_TICKS));
    }

    @Override
    public List<String> getDescription() {
        double interval = getHealIntervalTicks() / 20.0;
        return List.of(
                "§7You rejuvenate your body by focus of mind for §f" + trim(getEffectSeconds()) + " seconds§7,",
                "§7restoring §f" + trim(getHealPercent()) + "% max health§7 every §f" + trim(interval) + "s §7and taking §f"
                        + trim(AbilityEffectManager.getInstance().getConfiguredMeditateDamageReduction()) + "% reduced§7",
                "§7damage. You can't move while channeling. Sneak to stop channeling."
        );
    }



    @Override
    public boolean isCharging(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    @Override
    public void interruptCharge(Player player) {
        stop(player);
    }

    public static void reduceDamage(Player player, EntityDamageEvent event) {
        AbilityEffectManager effects = AbilityEffectManager.getInstance();
        if (!effects.isActive(player, AbilityEffectType.MEDITATE)) return;

        switch (event.getCause()) {
            case VOID, SUICIDE, KILL -> {
                return;
            }
            default -> {
            }
        }

        double reduction = Math.max(0.0, Math.min(100.0, effects.getConfiguredMeditateDamageReduction()));
        event.setDamage(event.getDamage() * (1.0 - reduction / 100.0));
    }

    @Override
    public boolean cast(Player caster) {
        if (sessions.containsKey(caster.getUniqueId())) return false;

        AbilityEffectManager.getInstance().activate(caster, AbilityEffectType.MEDITATE);

        Session session = new Session();
        sessions.put(caster.getUniqueId(), session);
        session.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(caster, session), 1L, 1L);

        Location at = caster.getLocation().add(0, 1.0, 0);
        caster.getWorld().playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
        caster.getWorld().playSound(at, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 1.4f);
        startBurst(caster);
        return true;
    }

    private void tick(Player player, Session session) {
        if (!player.isOnline() || player.isDead()) {
            stop(player);
            return;
        }

        DebuffManager debuffs = DebuffManager.getInstance();
        if (debuffs.hasDebuff(player, DebuffType.STUN) || debuffs.hasDebuff(player, DebuffType.GROUNDED)) {
            stop(player);
            return;
        }

        session.elapsed++;

        if (session.elapsed % getHealIntervalTicks() == 0) {
            heal(player);
        }
        if (session.elapsed % 2 == 0) {
            drawAura(player, session.elapsed);
        }

        int durationTicks = Math.max(1, (int) Math.round(getEffectSeconds() * 20.0));
        if (session.elapsed >= durationTicks) {
            stop(player);
        }
    }

    private void stop(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        if (session.task != null) session.task.cancel();

        AbilityEffectManager.getInstance().end(player, AbilityEffectType.MEDITATE);
        if (player.isOnline()) {
            Location at = player.getLocation().add(0, 1.0, 0);
            player.getWorld().playSound(at, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.8f);
            player.getWorld().spawnParticle(Particle.DUST, at, 24, 0.5, 0.9, 0.5, 0.0, LIME);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, at, 10, 0.6, 0.9, 0.6, 0.0);
        }
    }

    private void heal(Player player) {
        AttributeInstance maxAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxAttr == null || UndyingRage.isActive(player)) return;

        double max = maxAttr.getValue();
        double health = player.getHealth();
        if (health >= max) return;

        double multiplier = PlayerStats.getOrCreate(player).getEffectiveHealingMultiplier(player);
        double amount = max * (getHealPercent() / 100.0) * multiplier;
        player.setHealth(Math.min(max, health + amount));
        healPulse(player);
    }

    private void ring(World world, Location c, double radius, double y, int points, Particle.DustOptions first,
                      Particle.DustOptions second, double offset) {
        for (int i = 0; i < points; i++) {
            double a = i * 2 * Math.PI / points + offset;
            world.spawnParticle(Particle.DUST, c.getX() + Math.cos(a) * radius, c.getY() + y, c.getZ() + Math.sin(a) * radius,
                    1, 0, 0, 0, 0, (i % 2 == 0) ? first : second);
        }
    }

    private void drawAura(Player player, int elapsed) {
        World world = player.getWorld();
        Location base = player.getLocation();
        double spin = elapsed * 0.12;

        double ringRadius = 1.5 + 0.25 * Math.sin(elapsed * 0.1);
        ring(world, base, ringRadius, 0.1, 28, GREEN, LIME, -spin);
        ring(world, base, ringRadius * 0.55, 0.12, 14, PALE_LIME, PALE_LIME, spin * 1.5);

        double cycle = (elapsed % 50) / 50.0;
        for (int strand = 0; strand < 2; strand++) {
            for (int k = 0; k < 3; k++) {
                double frac = (cycle + k / 3.0) % 1.0;
                double radius = 1.0 - 0.45 * frac;
                double angle = spin * 2 + strand * Math.PI + frac * Math.PI * 2;
                world.spawnParticle(Particle.DUST,
                        base.getX() + Math.cos(angle) * radius, base.getY() + 0.1 + frac * 2.3, base.getZ() + Math.sin(angle) * radius,
                        1, 0, 0, 0, 0, strand == 0 ? LIME : GREEN);
            }
        }

        world.spawnParticle(Particle.HAPPY_VILLAGER, base.getX(), base.getY() + 1.0, base.getZ(), 1, 0.8, 0.9, 0.8, 0.0);
    }

    private void healPulse(Player player) {
        World world = player.getWorld();
        Location base = player.getLocation();

        ring(world, base, 1.4, 0.15, 24, LIME, PALE_LIME, 0.0);
        for (int i = 0; i < 12; i++) {
            double angle = i * 0.9;
            world.spawnParticle(Particle.DUST, base.getX() + Math.cos(angle) * 0.6, base.getY() + i * 0.2,
                    base.getZ() + Math.sin(angle) * 0.6, 1, 0, 0, 0, 0, LIME);
        }
        world.spawnParticle(Particle.HAPPY_VILLAGER, base.clone().add(0, 1.0, 0), 6, 0.5, 0.7, 0.5, 0.0);
        world.playSound(base, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.4f);
    }

    private void startBurst(Player player) {
        World world = player.getWorld();
        Location base = player.getLocation();
        ring(world, base, 2.2, 0.15, 36, GREEN, LIME, 0.0);
        world.spawnParticle(Particle.HAPPY_VILLAGER, base.clone().add(0, 1.0, 0), 18, 0.6, 0.9, 0.6, 0.0);
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}