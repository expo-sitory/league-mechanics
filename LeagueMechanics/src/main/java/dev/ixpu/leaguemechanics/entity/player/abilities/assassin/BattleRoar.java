package dev.ixpu.leaguemechanics.entity.player.abilities.assassin;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClass;
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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BattleRoar extends ClassAbilities {
    private static final String CFG = "skills.battle-roar.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 45.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 15.0;
    private static final double DEFAULT_WINDOW_SECONDS = 3.0;
    private static final double DEFAULT_DOME_RADIUS = 5.0;
    private static final int DOME_TICKS = 10;

    private static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 221, 51), 1.4f);
    private static final Particle.DustOptions PALE_YELLOW = new Particle.DustOptions(Color.fromRGB(255, 245, 160), 1.1f);

    private record Window(double startHealth, long startMillis) {}

    private static final Map<UUID, Window> windows = new ConcurrentHashMap<>();

    public BattleRoar(LeagueMechanics plugin) {
        super(plugin, "battle-roar", "Battle Roar", PlayerClassType.ASSASSIN,
                "passives.battle_roar",
                "leaguemechanics:battle-roar",
                "minecraft:bell");
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
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.BATTLE_ROAR);
    }

    @Override
    public boolean canCastWhileCrowdControlled() {
        return true;
    }

    private double getWindowSeconds() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "window-seconds", DEFAULT_WINDOW_SECONDS));
    }

    private double getDomeRadius() {
        return Math.max(1.0, plugin.getConfig().getDouble(CFG + "dome-radius", DEFAULT_DOME_RADIUS));
    }

    @Override
    public List<String> getDescription() {
        AbilityEffectManager effects = AbilityEffectManager.getInstance();
        return List.of(
                "§7You let out a battle roar healing yourself for some of",
                "§7the damage that you receive within " + trim(getWindowSeconds()) + " seconds §7window.",
                "§7Additionally breaks out of crowd control effects."
        );
    }

    public static void trackDamage(Player player, double finalDamage) {
        if (finalDamage <= 0.0) return;
        if (PlayerClass.getCachedPlayerClass(player) != PlayerClassType.ASSASSIN) return;

        LeagueMechanics plugin = LeagueMechanics.getInstance();
        double windowSeconds = plugin == null ? DEFAULT_WINDOW_SECONDS
                : plugin.getConfig().getDouble(CFG + "window-seconds", DEFAULT_WINDOW_SECONDS);

        long now = System.currentTimeMillis();
        Window window = windows.get(player.getUniqueId());
        if (window != null && now - window.startMillis() <= (long) (windowSeconds * 1000.0)) return;

        windows.put(player.getUniqueId(), new Window(player.getHealth(), now));
    }

    @Override
    public boolean cast(Player caster) {
        DebuffManager.getInstance().clearCrowdControl(caster);

        double healed = healFromWindow(caster);

        AbilityEffectManager effects = AbilityEffectManager.getInstance();
        effects.activate(caster, AbilityEffectType.BATTLE_ROAR);
        refreshStats(caster);

        long durationTicks = Math.max(1L, Math.round(effects.getDurationSeconds(AbilityEffectType.BATTLE_ROAR) * 20.0));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (caster.isOnline()) refreshStats(caster);
        }, durationTicks + 2L);

        Location origin = caster.getLocation();
        if (healed > 0.0) {
            caster.getWorld().spawnParticle(Particle.HEART, origin.clone().add(0, 1.0, 0), 6, 0.4, 0.6, 0.4, 0.0);
        }
        spawnDome(origin, getDomeRadius());
        caster.getWorld().playSound(origin, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.2f);
        return true;
    }

    private double healFromWindow(Player player) {
        Window window = windows.remove(player.getUniqueId());
        if (window == null) return 0.0;
        if (System.currentTimeMillis() - window.startMillis() > (long) (getWindowSeconds() * 1000.0)) return 0.0;
        if (UndyingRage.isActive(player)) return 0.0;

        AttributeInstance maxAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxAttr == null) return 0.0;

        double max = maxAttr.getValue();
        double health = player.getHealth();
        double missing = Math.min(window.startHealth(), max) - health;
        if (missing <= 0.0) return 0.0;

        double multiplier = PlayerStats.getOrCreate(player).getEffectiveHealingMultiplier(player);
        double healed = Math.min(max - health, missing * multiplier);
        player.setHealth(health + healed);
        return healed;
    }

    private void refreshStats(Player player) {
        if (plugin.getPlayerEventListener() != null) {
            plugin.getPlayerEventListener().applyPlayerStats(player);
        }
    }

    private void spawnDome(Location origin, double radius) {
        World world = origin.getWorld();
        double golden = Math.PI * (3 - Math.sqrt(5));

        Bukkit.getScheduler().runTaskTimer(plugin, new java.util.function.Consumer<org.bukkit.scheduler.BukkitTask>() {
            int step = 0;

            @Override
            public void accept(org.bukkit.scheduler.BukkitTask task) {
                step++;
                double r = radius * step / DOME_TICKS;
                int points = Math.max(24, (int) Math.round(2 * Math.PI * r * r * 0.5));
                for (int i = 0; i < points; i++) {
                    double h = (i + 0.5) / points;
                    double flat = Math.sqrt(1 - h * h);
                    double angle = i * golden + step * 0.3;
                    world.spawnParticle(Particle.DUST,
                            origin.getX() + Math.cos(angle) * flat * r,
                            origin.getY() + 0.1 + h * r,
                            origin.getZ() + Math.sin(angle) * flat * r,
                            1, 0, 0, 0, 0, (i % 3 == 0) ? PALE_YELLOW : YELLOW);
                }
                int ring = Math.max(16, (int) Math.round(2 * Math.PI * r * 2.0));
                for (int i = 0; i < ring; i++) {
                    double angle = i * 2 * Math.PI / ring;
                    world.spawnParticle(Particle.DUST, origin.getX() + Math.cos(angle) * r, origin.getY() + 0.1,
                            origin.getZ() + Math.sin(angle) * r, 1, 0, 0, 0, 0, YELLOW);
                }
                if (step >= DOME_TICKS) task.cancel();
            }
        }, 0L, 1L);
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}