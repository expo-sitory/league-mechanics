package dev.ixpu.leaguemechanics.rune.keystones.inspiration;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.util.DebugLogger;

import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;

public class FirstStrike extends CooldownHandler {
    private double INITIAL_XP;
    private double BUFF_DURATION_SECONDS;
    private double TRUE_DAMAGE_PERCENT;

    private int COOLDOWN_SECONDS;

    private double AD_PERCENTAGE;
    private double AP_PERCENTAGE;

    private PlayerEventListener listener;

    private static final double COMBAT_WINDOW_MS = 250;

    private final Map<UUID, Long> lastCombatTime = new HashMap<>();
    private final Set<UUID> firstStrikeActive = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Double> bonusDamageTracked = new HashMap<>();
    private final Map<UUID, Long> buffEndTime = new HashMap<>();
    private LeagueMechanics plugin;


    public FirstStrike(ConfigurationSection config, LeagueMechanics plugin, PlayerEventListener listener) {
        super("first-strike", RunePath.INSPIRATION, RuneSlot.KEYSTONE);
        this.listener = listener;
        this.plugin = plugin;
        ConfigurationSection section = config.getConfigurationSection("runes.keystones.inspiration.first-strike");
        if (section != null) {
            this.INITIAL_XP = section.getDouble("initial-xp", this.INITIAL_XP);
            this.BUFF_DURATION_SECONDS = section.getDouble("buff-duration", this.BUFF_DURATION_SECONDS);
            this.TRUE_DAMAGE_PERCENT = section.getDouble("true-damage-percent", this.TRUE_DAMAGE_PERCENT);
            this.COOLDOWN_SECONDS = section.getInt("cooldown", this.COOLDOWN_SECONDS);
            this.AD_PERCENTAGE = section.getDouble("ad-percentage", this.AD_PERCENTAGE);
            this.AP_PERCENTAGE = section.getDouble("ap-percentage", this.AP_PERCENTAGE);
        }
        this.setCooldownSeconds(COOLDOWN_SECONDS);
    }

    @Override
    public void onEnable(Player player) {
        UUID uuid = player.getUniqueId();
        lastCombatTime.put(uuid, 0L);
        firstStrikeActive.remove(uuid);
        bonusDamageTracked.put(uuid, 0.0);
        buffEndTime.put(uuid, 0L);
    }

    @Override
    public void onDisable(Player player) {
        UUID uuid = player.getUniqueId();
        lastCombatTime.remove(uuid);
        firstStrikeActive.remove(uuid);
        bonusDamageTracked.remove(uuid);
        buffEndTime.remove(uuid);
    }

    public void onProjectileHit(Player shooter, Entity target) {
        activateFirstStrike(shooter, target);
    }

    public void onAttack(Player attacker, Entity target) {
        activateFirstStrike(attacker, target);
    }

    private void activateFirstStrike(Player player, Entity target) {
        UUID attackerUUID = player.getUniqueId();

        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget.getMaxHealth() < 20) {
            return;
        }
        if(!listener.letRunesThrough(player)) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastCombat = lastCombatTime.getOrDefault(attackerUUID, 0L);

        if (currentTime - lastCombat > COMBAT_WINDOW_MS && !isOnCooldown(player)) {
            player.giveExp((int) INITIAL_XP);
            firstStrikeActive.add(attackerUUID);
            bonusDamageTracked.put(attackerUUID, 0.0);

            long buffEnd = System.currentTimeMillis() + (long) (BUFF_DURATION_SECONDS * 1000);
            buffEndTime.put(attackerUUID, buffEnd);

            lastCombatTime.put(attackerUUID, currentTime);
            resetCooldown(player);
        }

        double tracked = bonusDamageTracked.getOrDefault(attackerUUID, 0.0);
        boolean isActive = firstStrikeActive.contains(attackerUUID);

        if (isActive && System.currentTimeMillis() < buffEndTime.getOrDefault(attackerUUID, 0L)) {
            double damageToApply = keystoneDamage(player, target);

            if (livingTarget instanceof Player targetPlayer) {
                double absorption = targetPlayer.getAbsorptionAmount();
                if (damageToApply > absorption) {
                    damageToApply -= absorption;
                    targetPlayer.setAbsorptionAmount(0);
                } else {
                    targetPlayer.setAbsorptionAmount(absorption - damageToApply);
                    damageToApply = 0;
                }
            }

            double newHealth = Math.clamp(livingTarget.getHealth() - damageToApply, 0, livingTarget.getMaxHealth());
            if (livingTarget instanceof Player livingPlayer) {
                KillSourceTracker.getInstance().setSource(livingPlayer, player);
            }
            livingTarget.setHealth(newHealth);
            bonusDamageTracked.put(attackerUUID, tracked + damageToApply);
            spawnXPOrbs(player, livingTarget);
        }
    }

    private double keystoneDamage(Player player, Entity target) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableTrueDamage();
        return damageManager.DamageCalculation(player, target, 0, 0, TRUE_DAMAGE_PERCENT, 0);
    }

    private void spawnXPOrbs(Player attacker, LivingEntity target) {
        if (plugin == null) return;

        Location targetLoc = target.getLocation().add(0, 1, 0);
        Location attackerLoc = attacker.getLocation().add(0, 1, 0);

        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            for (int i = 0; i < 8; i++) {
                double offsetX = (Math.random() - 0.5) * 0.8;
                double offsetZ = (Math.random() - 0.5) * 0.8;
                Location spawnLoc = targetLoc.clone().add(offsetX, 0.5, offsetZ);

                target.getWorld().spawnParticle(
                        Particle.GLOW,
                        spawnLoc,
                        0,
                        0, 0, 0,
                        0.5
                );
            }

            animateXPOrbs(target, targetLoc, attackerLoc);
        });
    }

    private void animateXPOrbs(LivingEntity target, Location targetLoc, Location attackerLoc) {
        if (plugin == null) return;

        final int[] tickCount = {0};
        final int totalTicks = 15;
        final int[] taskId = {-1};

        taskId[0] = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (tickCount[0] >= totalTicks) {
                    plugin.getServer().getScheduler().cancelTask(taskId[0]);
                    return;
                }

                double progress = (double) tickCount[0] / totalTicks;
                Location currentLoc = targetLoc.clone().add(
                        (attackerLoc.getX() - targetLoc.getX()) * progress,
                        (attackerLoc.getY() - targetLoc.getY()) * progress,
                        (attackerLoc.getZ() - targetLoc.getZ()) * progress
                );

                target.getWorld().spawnParticle(
                        Particle.GLOW,
                        currentLoc,
                        3,
                        0.1, 0.1, 0.1,
                        0
                );

                tickCount[0]++;
            }
        }, 0, 1);
    }

    @Override
    public void tick(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long buffEnd = buffEndTime.getOrDefault(uuid, 0L);

        if (currentTime >= buffEnd && firstStrikeActive.contains(uuid)) {
            grantXPBonus(player);
            firstStrikeActive.remove(uuid);
            bonusDamageTracked.put(uuid, 0.0);
        }

        RuneState state;
        if (firstStrikeActive.contains(uuid)) {
            state = RuneState.ACTIVE;
        } else if (isOnCooldown(player)) {
            state = RuneState.COOLDOWN;
        } else {
            state = RuneState.IDLE;
        }

        String runeDisplay = getRuneDisplay(state, player);
        setPlayerDisplay(player, runeDisplay);
    }

    private void grantXPBonus(Player player) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        double totalAD = playerStats.getPlayerAD(player);
        double totalAP = playerStats.getPlayerAP(player);

        if (totalAD <= 0) totalAD = 2.0;
        if (totalAP <= 0) totalAP = 12.0;

        double adBonus = totalAD * AD_PERCENTAGE;
        double apBonus = totalAP * AP_PERCENTAGE;
        double statScaling = Math.max(adBonus, apBonus);

        double DamageCalculation = bonusDamageTracked.getOrDefault(player.getUniqueId(), 0.0);
        double finalXP = DamageCalculation * statScaling;
        int xpToGive = (int) finalXP;

        DebugLogger.debug(player, String.format("§7[DEBUG] §f[§dAttacker§f] [§3First Strike§f] " + "%.2f", finalXP) + " XP (damage: " + String.format("%.2f", DamageCalculation) + ", AD: " + String.format("%.2f", totalAD) + ", AP: " + String.format("%.2f", totalAP) + ")");
        DebugLogger.debug(player, "§7[DEBUG] §f[§dAttacker§f] [§3First Strike§f] Total XP: " + xpToGive);
        player.giveExp(xpToGive);
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);

        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    private String getRuneDisplay(RuneState state, Player player) {
        UUID uuid = player.getUniqueId();
        return switch (state) {
            case ACTIVE -> {
                long buffEnd = buffEndTime.getOrDefault(uuid, 0L);
                long remaining = Math.max(0, (buffEnd - System.currentTimeMillis()) / 1000);
                yield "§b✎ " + remaining + "s";
            }
            case COOLDOWN -> {
                double cooldownRemaining = getRemainingCooldown(player);
                yield "§7✎ " + String.format("%.1f", cooldownRemaining) + "s";
            }
            case IDLE -> "§3✎";
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        UUID uuid = player.getUniqueId();
        if (firstStrikeActive.contains(uuid)) {
            return getRuneDisplay(RuneState.ACTIVE, player);
        }
        if (isOnCooldown(player)) {
            return getRuneDisplay(RuneState.COOLDOWN, player);
        }
        return getRuneDisplay(RuneState.IDLE, player);
    }

    enum RuneState {
        ACTIVE, COOLDOWN, IDLE
    }
}