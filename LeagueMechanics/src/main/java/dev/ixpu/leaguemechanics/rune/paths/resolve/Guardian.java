package dev.ixpu.leaguemechanics.rune.paths.resolve;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.paths.RunePath;
import dev.ixpu.leaguemechanics.rune.slots.RuneSlot;

import dev.ixpu.leaguemechanics.util.Icons;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Guardian extends CooldownHandler {

    private static final double DEFAULT_RANGE = 5.0;
    private static final double DEFAULT_BONUS_ARMOR_PERCENT = 30.0;
    private static final double DEFAULT_BONUS_MAGIC_RESIST_PERCENT = 30.0;
    private static final double DEFAULT_BUFF_SECONDS = 5.0;
    private static final int DEFAULT_COOLDOWN_SECONDS = 75;

    private static final Particle GUARD_PARTICLE = Particle.GLOW;

    private static final double FOOT_RING_RADIUS = 0.6;
    private static final int RANGE_RING_INTERVAL_TICKS = 40;
    private static final int FOOT_RING_INTERVAL_TICKS = 20;
    private static final int PULSE_TICKS = 8;

    private double range = DEFAULT_RANGE;
    private double bonusArmorPercent = DEFAULT_BONUS_ARMOR_PERCENT;
    private double bonusMagicResistPercent = DEFAULT_BONUS_MAGIC_RESIST_PERCENT;
    private int buffTicks = (int) Math.round(DEFAULT_BUFF_SECONDS * 20.0);
    private int cooldownSeconds = DEFAULT_COOLDOWN_SECONDS;

    private final Set<UUID> holders = new HashSet<>();
    private final Set<UUID> raised = new HashSet<>();
    private final Map<UUID, Applied> applied = new HashMap<>();

    private static final class Applied {
        final UUID holder;
        final PlayerStats stats;
        final double armor;
        final double magicResist;
        final long expiresAt;
        BukkitTask task;

        Applied(UUID holder, PlayerStats stats, double armor, double magicResist, long expiresAt) {
            this.holder = holder;
            this.stats = stats;
            this.armor = armor;
            this.magicResist = magicResist;
            this.expiresAt = expiresAt;
        }
    }

    public Guardian(ConfigurationSection config) {
        super("guardian", RunePath.RESOLVE, RuneSlot.KEYSTONE);
        ConfigurationSection section = config.getConfigurationSection("runes.keystones.resolve.guardian");
        if (section != null) {
            this.range = section.getDouble("range", this.range);
            this.bonusArmorPercent = section.getDouble("bonus-armor-percent", this.bonusArmorPercent);
            this.bonusMagicResistPercent = section.getDouble("bonus-magic-resist-percent", this.bonusMagicResistPercent);
            double buffSeconds = section.getDouble("buff-duration-seconds", DEFAULT_BUFF_SECONDS);
            this.buffTicks = Math.max(1, (int) Math.round(buffSeconds * 20.0));
            this.cooldownSeconds = section.getInt("cooldown", this.cooldownSeconds);
        }
        this.setCooldownSeconds(cooldownSeconds);
    }

    @Override
    public void onEnable(Player player) {
        holders.add(player.getUniqueId());
    }

    @Override
    public void onDisable(Player player) {
        UUID uuid = player.getUniqueId();
        holders.remove(uuid);
        raised.remove(uuid);
        clearPlayerCooldown(player);

        for (UUID recipient : new ArrayList<>(applied.keySet())) {
            Applied entry = applied.get(recipient);
            if (entry != null && entry.holder.equals(uuid)) {
                clearBuff(recipient);
            }
        }
    }

    public void onGuardedPlayerDamaged(Player damaged) {
        if (damaged == null) return;
        var runeManager = LeagueMechanics.getInstance().getRuneManager();

        for (UUID holderId : new ArrayList<>(holders)) {
            Player holder = Bukkit.getPlayer(holderId);
            if (holder == null || !holder.isOnline() || holder.isDead()) continue;
            if (runeManager != null && !runeManager.hasActiveRunes(holder)) continue;
            if (isOnCooldown(holder)) continue;

            List<Player> guarded = getGuarded(holder);
            if (!guarded.contains(damaged)) continue;

            trigger(holder, guarded);
        }
    }

    private List<Player> getGuarded(Player holder) {
        List<Player> guarded = new ArrayList<>();
        double rangeSq = range * range;
        Location origin = holder.getLocation();

        for (Player mate : PartyManager.getInstance().getOnlineTeammates(holder)) {
            if (mate.isDead() || mate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!mate.getWorld().equals(holder.getWorld())) continue;
            if (mate.getLocation().distanceSquared(origin) > rangeSq) continue;
            guarded.add(mate);
        }

        if (!guarded.isEmpty()) {
            guarded.add(0, holder);
        }
        return guarded;
    }

    private void trigger(Player holder, List<Player> guarded) {
        resetCooldown(holder);
        raised.remove(holder.getUniqueId());

        for (Player player : guarded) {
            applyBuff(holder, player);
            Location body = player.getLocation().add(0, 1.0, 0);
            player.getWorld().spawnParticle(GUARD_PARTICLE, body, 10, 0.4, 0.6, 0.4, 0.05);
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.2f);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        }

        spawnPulse(holder.getLocation());
    }

    private void applyBuff(Player holder, Player recipient) {
        UUID recipientId = recipient.getUniqueId();
        clearBuff(recipientId);

        PlayerStats stats = PlayerStats.getOrCreate(recipient);
        double armor = Math.max(0.0, stats.getPlayerAR(recipient)) * bonusArmorPercent / 100.0;
        double magicResist = Math.max(0.0, stats.getPlayerMR(recipient)) * bonusMagicResistPercent / 100.0;

        stats.modifyAR(armor);
        stats.modifyMR(magicResist);

        Applied entry = new Applied(holder.getUniqueId(), stats, armor, magicResist,
                System.currentTimeMillis() + buffTicks * 50L);
        entry.task = Bukkit.getScheduler().runTaskLater(LeagueMechanics.getInstance(), () -> {
            if (applied.get(recipientId) == entry) {
                clearBuff(recipientId);
            }
        }, buffTicks);
        applied.put(recipientId, entry);
    }

    private void clearBuff(UUID recipientId) {
        Applied entry = applied.remove(recipientId);
        if (entry == null) return;
        if (entry.task != null) entry.task.cancel();
        // Reverting on the stored stats object is safe even if the player has since left.
        entry.stats.modifyAR(-entry.armor);
        entry.stats.modifyMR(-entry.magicResist);
    }

    private double getBuffRemainingSeconds(Player player) {
        Applied entry = applied.get(player.getUniqueId());
        if (entry == null) return 0.0;
        return Math.max(0.0, (entry.expiresAt - System.currentTimeMillis()) / 1000.0);
    }

    @Override
    public void tick(Player player) {
        UUID uuid = player.getUniqueId();
        if (player.isDead()) {
            raised.remove(uuid);
            return;
        }

        boolean onCooldown = isOnCooldown(player);
        List<Player> guarded = onCooldown ? new ArrayList<>() : getGuarded(player);
        boolean guardUp = !guarded.isEmpty();

        if (guardUp && raised.add(uuid)) {
            for (Player member : guarded) {
                member.playSound(member.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.6f);
            }
        } else if (!guardUp) {
            raised.remove(uuid);
        }

        if (!onCooldown) {
            int tick = Bukkit.getCurrentTick();
            if (tick % RANGE_RING_INTERVAL_TICKS == 0) {
                drawRing(player.getLocation(), range, tick);
            }
            if (guardUp && tick % FOOT_RING_INTERVAL_TICKS == 0) {
                for (Player member : guarded) {
                    drawRing(member.getLocation(), FOOT_RING_RADIUS, tick);
                }
            }
        }

        String display;
        double buffRemaining = getBuffRemainingSeconds(player);
        if (buffRemaining > 0 && applied.get(uuid) != null && applied.get(uuid).holder.equals(uuid)) {
            display = getRuneDisplay(RuneState.ACTIVE, player, 0);
        } else if (onCooldown) {
            display = getRuneDisplay(RuneState.COOLDOWN, player, 0);
        } else if (guardUp) {
            display = getRuneDisplay(RuneState.GUARDING, player, guarded.size());
        } else {
            display = getRuneDisplay(RuneState.IDLE, player, 0);
        }
        setPlayerDisplay(player, display);
    }

    private void drawRing(Location center, double radius, int tick) {
        World world = center.getWorld();
        if (world == null) return;

        double y = groundY(center) + 0.05;
        int points = Math.max(8, (int) Math.round(radius * 2.0 * Math.PI / 0.7));
        double step = 2.0 * Math.PI / points;
        double shimmer = ((tick / 5) % 3) * step / 3.0;

        for (int i = 0; i < points; i++) {
            double angle = i * step + shimmer;
            Location point = new Location(world,
                    center.getX() + Math.cos(angle) * radius, y, center.getZ() + Math.sin(angle) * radius);
            world.spawnParticle(GUARD_PARTICLE, point, 1, 0, 0, 0, 0);
        }
    }

    private double groundY(Location at) {
        World world = at.getWorld();
        RayTraceResult hit = world.rayTraceBlocks(at.clone().add(0, 0.5, 0), new Vector(0, -1, 0), 3.0,
                FluidCollisionMode.NEVER, true);
        return hit != null ? hit.getHitPosition().getY() : at.getY();
    }

    private void spawnPulse(Location origin) {
        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                step++;
                drawRing(origin, range * step / PULSE_TICKS, 0);
                if (step >= PULSE_TICKS) cancel();
            }
        }.runTaskTimer(LeagueMechanics.getInstance(), 0L, 1L);
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);
        player.sendActionBar(Component.text(runeDisplay + " " + statsDisplay));
    }

    enum RuneState {
        ACTIVE, COOLDOWN, GUARDING, IDLE
    }

    private String getRuneDisplay(RuneState state, Player player, int guardedCount) {
        return switch (state) {
            case ACTIVE -> "§f" + Icons.get("keystones.guardian") + " §a" + String.format(Locale.ROOT, " %.1fs", getBuffRemainingSeconds(player));
            case COOLDOWN -> "§7" + Icons.get("keystones.guardian") + " §7" + getCooldownDisplay(player);
            case GUARDING -> "§f" + Icons.get("keystones.guardian") + " §a(" + guardedCount + ")";
            case IDLE -> "§f" + Icons.get("keystones.guardian");
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        UUID uuid = player.getUniqueId();
        Applied own = applied.get(uuid);
        if (own != null && own.holder.equals(uuid) && getBuffRemainingSeconds(player) > 0) {
            return getRuneDisplay(RuneState.ACTIVE, player, 0);
        }
        if (isOnCooldown(player)) {
            return getRuneDisplay(RuneState.COOLDOWN, player, 0);
        }
        if (raised.contains(uuid)) {
            return getRuneDisplay(RuneState.GUARDING, player, getGuarded(player).size());
        }
        return getRuneDisplay(RuneState.IDLE, player, 0);
    }
}