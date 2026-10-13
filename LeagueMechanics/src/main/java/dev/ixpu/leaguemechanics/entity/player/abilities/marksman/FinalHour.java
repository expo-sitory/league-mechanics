package dev.ixpu.leaguemechanics.entity.player.abilities.marksman;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FinalHour extends ClassAbilities {
    private static final String CFG = "skills.final-hour.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_VANISH_SECONDS = 1.5;

    private static final int NIGHT_VISION_BUFFER_TICKS = 400;
    private static final double UNTRACK_RADIUS = 48.0;

    private static final Map<UUID, BukkitTask> vanishTasks = new ConcurrentHashMap<>();
    private static final Set<UUID> nightVisionGranted = ConcurrentHashMap.newKeySet();

    public FinalHour(LeagueMechanics plugin) {
        super(plugin, "final-hour", "Final Hour", PlayerClassType.MARKSMAN,
                "passives.vayne_final_hour",
                "leaguemechanics:final-hour",
                "minecraft:clock");
    }

    @Override
    public double getMinCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "min-cooldown-seconds", DEFAULT_MIN_COOLDOWN_SECONDS);
    }


    @Override
    public double getCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "cooldown-seconds", DEFAULT_COOLDOWN_SECONDS);
    }

    private static double getVanishSeconds() {
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return DEFAULT_VANISH_SECONDS;
        return plugin.getConfig().getDouble(CFG + "vanish-seconds", DEFAULT_VANISH_SECONDS);
    }

    @Override
    public double getEffectSeconds() {
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.FINAL_HOUR);
    }

    @Override
    public List<String> getDescription() {
        AbilityEffectManager effects = AbilityEffectManager.getInstance();
        double attackSpeed = effects.getConfiguredAttackSpeed();
        double seconds = effects.getDurationSeconds(AbilityEffectType.FINAL_HOUR);
        return List.of(
                "§7Readying yourself for confrontation, you gain §f+" + trim(attackSpeed) + "% attack speed§7",
                "§7for §f" + trim(seconds) + " seconds§7. Dashing or double jumping vanishes you from",
                "§7enemies for §f" + trim(getVanishSeconds()) + " second§7 during this duration."
        );
    }


    @Override
    public boolean cast(Player caster) {
        AbilityEffectManager.getInstance().activate(caster, AbilityEffectType.FINAL_HOUR);

        Location origin = caster.getLocation().add(0, 1.0, 0);
        caster.getWorld().spawnParticle(Particle.CRIT, origin, 30, 0.5, 0.7, 0.5, 0.3);
        caster.getWorld().playSound(origin, Sound.ITEM_CROSSBOW_LOADING_END, 1.0f, 1.2f);
        caster.getWorld().playSound(origin, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.6f);
        return true;
    }

    public static void onMobility(Player player) {
        if (!AbilityEffectManager.getInstance().isActive(player, AbilityEffectType.FINAL_HOUR)) return;

        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return;

        UUID uuid = player.getUniqueId();
        int ticks = Math.max(1, (int) Math.round(getVanishSeconds() * 20.0));

        BukkitTask previous = vanishTasks.remove(uuid);
        if (previous != null) previous.cancel();

        PartyManager parties = PartyManager.getInstance();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player) || parties.isSameParty(player, viewer)) continue;
            viewer.hideEntity(plugin, player);
        }

        dropMobTargets(player);
        grantNightVision(player, ticks);

        Location loc = player.getLocation().add(0, 1.0, 0);
        player.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.8f);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            vanishTasks.remove(uuid);
            reveal(plugin, player);
        }, ticks);
        vanishTasks.put(uuid, task);
    }

    public static boolean isVanished(Player player) {
        return player != null && vanishTasks.containsKey(player.getUniqueId());
    }

    private static void dropMobTargets(Player player) {
        for (Entity entity : player.getNearbyEntities(UNTRACK_RADIUS, UNTRACK_RADIUS, UNTRACK_RADIUS)) {
            if (!(entity instanceof Mob mob)) continue;
            if (player.equals(mob.getTarget())) mob.setTarget(null);
            if (mob instanceof Warden warden) warden.clearAnger(player);
        }
    }

    private static void grantNightVision(Player player, int vanishTicks) {
        UUID uuid = player.getUniqueId();
        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION) && !nightVisionGranted.contains(uuid)) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, vanishTicks + NIGHT_VISION_BUFFER_TICKS, 0, false, false, false));
        nightVisionGranted.add(uuid);
    }

    private static void reveal(LeagueMechanics plugin, Player player) {
        if (nightVisionGranted.remove(player.getUniqueId())) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player)) continue;
            viewer.showEntity(plugin, player);
        }
    }

    public static void cleanup(Player player) {
        BukkitTask task = vanishTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();

        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin != null) reveal(plugin, player);
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}