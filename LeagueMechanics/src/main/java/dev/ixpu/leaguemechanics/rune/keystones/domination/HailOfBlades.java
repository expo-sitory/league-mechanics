package dev.ixpu.leaguemechanics.rune.keystones.domination;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.util.DebugLogger;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;

import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.manager.StatScalingManager;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;


public class HailOfBlades extends CooldownHandler {
    private double ATTACK_SPEED;
    private double TRUE_DAMAGE_PERCENT;

    private double AD_PERCENTAGE_MULTIPLIER = 6.0;
    private double AP_PERCENTAGE_MULTIPLIER = 4.0;

    int COOLDOWN_DURATION_SECONDS;

    private static final int WINDUP_TICKS = 200;
    private static final int STACK_DURATION_TICKS = 60;
    private static final int INACTIVITY_TIMEOUT_TICKS = 60;
    private static final int INITIAL_STACKS = 4;

    private PlayerEventListener listener;

    private final Set<UUID> windupActive = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> windupTicks = new HashMap<>();
    private final Map<UUID, Integer> lastWindupStage = new HashMap<>();
    private final Set<UUID> activeState = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Double> activeASBonus = new HashMap<>();
    private final Map<UUID, List<Integer>> stackDurationTicks = new HashMap<>();
    private final Map<UUID, Integer> lastAttackTick = new HashMap<>();
    private final Map<UUID, Integer> currentStacks = new HashMap<>();

    public HailOfBlades(ConfigurationSection config, PlayerEventListener listener) {
        super("hail-of-blades", RunePath.DOMINATION, RuneSlot.KEYSTONE);
        this.listener = listener;
        ConfigurationSection section = config.getConfigurationSection("runes.keystones.domination.hail-of-blades");
        if (section != null) {
            this.ATTACK_SPEED = section.getDouble("base-attack-speed", this.ATTACK_SPEED);
            this.TRUE_DAMAGE_PERCENT = section.getDouble("base-true-damage", this.TRUE_DAMAGE_PERCENT);
            this.AD_PERCENTAGE_MULTIPLIER = section.getDouble("ad-percentage-multiplier", this.AD_PERCENTAGE_MULTIPLIER);
            this.AP_PERCENTAGE_MULTIPLIER = section.getDouble("ap-percentage-multiplier", this.AP_PERCENTAGE_MULTIPLIER);
            this.COOLDOWN_DURATION_SECONDS = section.getInt("cooldown", COOLDOWN_DURATION_SECONDS);
        }
        this.setCooldownSeconds(COOLDOWN_DURATION_SECONDS);
    }

    @Override
    public void onEnable(Player player) {
        UUID uuid = player.getUniqueId();
        windupActive.remove(uuid);
        windupTicks.put(uuid, 0);
        lastWindupStage.put(uuid, 0);
        activeState.remove(uuid);
        activeASBonus.put(uuid, 0.0);
        stackDurationTicks.put(uuid, new ArrayList<>());
        lastAttackTick.put(uuid, 0);
        currentStacks.put(uuid, 0);
    }

    @Override
    public void onDisable(Player player) {
        UUID uuid = player.getUniqueId();
        clearPlayerCooldown(player);
        windupActive.remove(uuid);
        windupTicks.remove(uuid);
        lastWindupStage.remove(uuid);
        activeState.remove(uuid);
        activeASBonus.remove(uuid);
        stackDurationTicks.remove(uuid);
        lastAttackTick.remove(uuid);
        currentStacks.remove(uuid);
    }

    public void onProjectileHit(Player shooter, Entity target) {
        activateHailofBlades(shooter, target);
    }

    public void onAttack(Player attacker, Entity target) {
        activateHailofBlades(attacker, target);
    }

    @Override
    public void onPlayerDamage(Player player, double damage) {
        activateHailofBlades(player, null);
    }

    public void activateHailofBlades(Player player, Entity target) {
        UUID playerUUID = player.getUniqueId();

        if (target != null) {
            if (!(target instanceof LivingEntity livingTarget)) {
                return;
            }
            if (livingTarget.getMaxHealth() < 20) {
                return;
            }
            if(!listener.letRunesThrough(player)) {
                return;
            }

            double damageToApply = keystoneDamage(player, target) * getScaledTrueDamage(player);

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

            if (windupActive.contains(playerUUID) || isOnCooldown(player)) {
                return;
            }
            if (activeState.contains(playerUUID)) {
                lastAttackTick.put(playerUUID, 0);
                currentStacks.put(playerUUID, currentStacks.getOrDefault(playerUUID, 0) - 1);

                DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] §f[§cHail of Blades§f] Keystone Damage = §d" + Math.ceil(keystoneDamage(player, target) * 100) / 100.0);
                DebugLogger.debug(player, "§7[Debug] §f[§dTarget§f] Target New HP = §d" + Math.ceil(newHealth * 100) / 100.0);

                if (livingTarget instanceof Player livingPlayer) {
                    KillSourceTracker.getInstance().setSource(livingPlayer, player);
                }
                livingTarget.setHealth(newHealth);

                return;
            }
        } else {
            if (windupActive.contains(playerUUID) || isOnCooldown(player)) {
                return;
            }
            if (activeState.contains(playerUUID)) {
                return;
            }
        }
        windupActive.add(playerUUID);
        windupTicks.put(playerUUID, WINDUP_TICKS);
        lastWindupStage.put(playerUUID, 0);
    }

    private double keystoneDamage(Player player, Entity target) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableTrueDamage();
        return damageManager.DamageCalculation(player, target, 0, 0, TRUE_DAMAGE_PERCENT, 0);
    }

    private int trackActiveStacks(Player player) {
        UUID playerUUID = player.getUniqueId();

        int inactivityCount = lastAttackTick.getOrDefault(playerUUID, 0);
        inactivityCount++;
        lastAttackTick.put(playerUUID, inactivityCount);

        if (inactivityCount >= INACTIVITY_TIMEOUT_TICKS) {
            currentStacks.put(playerUUID, currentStacks.getOrDefault(playerUUID, 0) - 1);
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 0.5f);
            lastAttackTick.put(playerUUID, 0);
        }

        return currentStacks.getOrDefault(playerUUID, 0);
    }

    private double getScaledTrueDamage(Player player) {
        StatScalingManager statScalingManager = new StatScalingManager();
        return statScalingManager.calculateScaledValue(
                player,
                TRUE_DAMAGE_PERCENT,
                AD_PERCENTAGE_MULTIPLIER,
                AP_PERCENTAGE_MULTIPLIER
        );
    }

    private void deactivateEffect(Player player) {
        UUID playerUUID = player.getUniqueId();
        activeState.remove(playerUUID);
        activeASBonus.put(playerUUID, 0.0);
        currentStacks.put(playerUUID, 0);
        windupTicks.put(playerUUID, 0);
        windupActive.remove(playerUUID);
        lastWindupStage.put(playerUUID, 0);
        lastAttackTick.put(playerUUID, 0);
        stackDurationTicks.put(playerUUID, new ArrayList<>());

        resetCooldown(player);
    }

    public double getActiveASBonus(Player player) {
        return activeASBonus.getOrDefault(player.getUniqueId(), 0.0);
    }

    @Override
    public void tick(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (isOnCooldown(player)) {
            String runeDisplay = getRuneDisplay(player, RuneState.COOLDOWN, 0);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        if (windupActive.contains(playerUUID)) {
            int windupCount = windupTicks.getOrDefault(playerUUID, 0);
            windupCount--;
            windupTicks.put(playerUUID, windupCount);

            String runeDisplay = getRuneDisplay(player, RuneState.WINDUP, windupCount);
            setPlayerDisplay(player, runeDisplay);

            if (windupCount <= 0) {
                windupActive.remove(playerUUID);
                activateEffect(player);
            }
            return;
        }

        if (activeState.contains(playerUUID)) {
            int stacks = trackActiveStacks(player);

            if (stacks <= 0) {
                deactivateEffect(player);
                return;
            }

            String runeDisplay = getRuneDisplay(player, RuneState.ACTIVE, stacks);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        String runeDisplay = getRuneDisplay(player, RuneState.IDLE, 0);
        setPlayerDisplay(player, runeDisplay);
    }

    private void activateEffect(Player player) {
        UUID playerUUID = player.getUniqueId();
        activeState.add(playerUUID);
        activeASBonus.put(playerUUID, ATTACK_SPEED);
        lastAttackTick.put(playerUUID, 0);
        currentStacks.put(playerUUID, INITIAL_STACKS);

        List<Integer> durations = new ArrayList<>();
        for (int i = 0; i < INITIAL_STACKS; i++) {
            durations.add(STACK_DURATION_TICKS);
        }
        stackDurationTicks.put(playerUUID, durations);

        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_TRIDENT_THROW, 1.0f, 2.0f);
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);

        String statsDisplay = playerStats.getActionBarSections(player);

        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        COOLDOWN, WINDUP, ACTIVE, IDLE
    }

    private String getRuneDisplay(Player player, RuneState state, int value) {
        return switch (state) {
            case COOLDOWN -> "§7❛❟❛ " + getCooldownDisplay(player);
            case ACTIVE -> "§c❛❟❛ " + value + "/" + INITIAL_STACKS;
            case WINDUP -> getWindupDisplay(player, value);
            case IDLE -> "§4❛❟❛";
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (isOnCooldown(player)) {
            return getRuneDisplay(player, RuneState.COOLDOWN, 0);
        }
        if (windupActive.contains(playerUUID)) {
            return getRuneDisplay(player, RuneState.WINDUP, windupTicks.getOrDefault(playerUUID, 0));
        }
        if (activeState.contains(playerUUID)) {
            int stacks = trackActiveStacks(player);
            return getRuneDisplay(player, RuneState.ACTIVE, stacks);
        }
        return getRuneDisplay(player, RuneState.IDLE, 0);
    }

    private String getWindupDisplay(Player player, int remainingTicks) {
        UUID playerUUID = player.getUniqueId();

        int currentStage;
        String message;

        if (remainingTicks > (WINDUP_TICKS * 2 / 3)) {
            currentStage = 1;
            message = "§c❛§4❟❛";
        } else if (remainingTicks > (WINDUP_TICKS / 3)) {
            currentStage = 2;
            message = "§c❛❟§4❛";
        } else {
            currentStage = 3;
            message = "§c❛❟❛";
        }

        int lastStage = lastWindupStage.getOrDefault(playerUUID, 0);
        if (currentStage != lastStage) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_TRIDENT_HIT, 1.0f, 2.0f);
            lastWindupStage.put(playerUUID, currentStage);
        }

        return message;
    }
}