package dev.ixpu.leaguemechanics.rune.keystones.sorcery;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.player.PlayerStats;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;

import dev.ixpu.leaguemechanics.manager.DamageManager;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;

public class StormRaiderSurge extends CooldownHandler {
    private double DAMAGE_THRESHOLD_PERCENTAGE = 30.0;
    private int MOVEMENT_SPEED_BONUS = 1;

    int COOLDOWN_SECONDS = 25;

    private PlayerEventListener listener;

    private static final int TRACKING_WINDOW_TICKS = 60;
    private static final int SPEED_DURATION_TICKS = 120;

    private final Map<UUID, Double> damageTracker = new HashMap<>();
    private final Map<UUID, Integer> windowTickCounter = new HashMap<>();
    private final Map<UUID, Integer> speedActiveDuration = new HashMap<>();
    private LeagueMechanics plugin;

    public StormRaiderSurge(ConfigurationSection config, PlayerEventListener listener) {
        super("storm-raider-surge", RunePath.SORCERY, RuneSlot.KEYSTONE);
        ConfigurationSection section = config.getConfigurationSection("runes.keystones.sorcery.storm-raider-surge");
        this.listener = listener;
        if (section != null) {
            this.DAMAGE_THRESHOLD_PERCENTAGE = section.getDouble("damage-percent-threshold", this.DAMAGE_THRESHOLD_PERCENTAGE);
            this.MOVEMENT_SPEED_BONUS = section.getInt("speed-level", this.MOVEMENT_SPEED_BONUS);
            this.COOLDOWN_SECONDS = section.getInt("cooldown", COOLDOWN_SECONDS);
        }
        this.setCooldownSeconds(COOLDOWN_SECONDS);
    }

    @Override
    public void onEnable(Player player) {
        UUID uuid = player.getUniqueId();
        damageTracker.put(uuid, 0.0);
        windowTickCounter.put(uuid, 0);
        speedActiveDuration.put(uuid, 0);
    }

    @Override
    public void onDisable(Player player) {
        UUID uuid = player.getUniqueId();
        clearPlayerCooldown(player);
        damageTracker.remove(uuid);
        windowTickCounter.remove(uuid);
        speedActiveDuration.remove(uuid);
    }

    public void onAttack(Player attacker, Entity target) {
        activateStormRaiderSurge(attacker, target);
    }

    private void activateStormRaiderSurge(Player player, Entity target) {
        UUID attackerUUID = player.getUniqueId();

        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget.getMaxHealth() < 20) {
            return;
        }
        if (isOnCooldown(player)) {
            return;
        }
        if (listener.isAnyHotbarOnCooldown(player)) {
            return;
        }

        double estimatedDamage = playerDamage(player, target);
        double currentDamage = damageTracker.getOrDefault(attackerUUID, 0.0);
        damageTracker.put(attackerUUID, currentDamage + estimatedDamage);

        windowTickCounter.put(attackerUUID, 0);
    }

    private double playerDamage(Player player, Entity target) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        return damageManager.DamageCalculation(player, target, 0, 0, 0);
    }

    private void enterActiveState(Player player) {
        UUID playerUUID = player.getUniqueId();
        speedActiveDuration.put(playerUUID, SPEED_DURATION_TICKS);
        damageTracker.put(playerUUID, 0.0);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                SPEED_DURATION_TICKS,
                MOVEMENT_SPEED_BONUS,
                false,
                false
        ));

        removeNegativeEffects(player);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 2f);
    }

    private void removeNegativeEffects(Player player) {
        PotionEffectType[] negativeEffects = {
                PotionEffectType.SLOWNESS,
                PotionEffectType.POISON,
                PotionEffectType.WITHER,
                PotionEffectType.BLINDNESS,
                PotionEffectType.DARKNESS,
                PotionEffectType.HUNGER
        };

        for (PotionEffectType effect : negativeEffects) {
            if (player.hasPotionEffect(effect)) {
                player.removePotionEffect(effect);
            }
        }
        dev.ixpu.leaguemechanics.manager.DebuffManager.getInstance().clearDebuffs(player);
    }

    @Override
    public void tick(Player player) {
        UUID playerUUID = player.getUniqueId();

        int windowTicks = windowTickCounter.getOrDefault(playerUUID, 0);
        windowTicks++;
        if (windowTicks >= TRACKING_WINDOW_TICKS) {
            damageTracker.put(playerUUID, 0.0);
            windowTicks = 0;
        }
        windowTickCounter.put(playerUUID, windowTicks);

        int speedDuration = speedActiveDuration.getOrDefault(playerUUID, 0);
        if (speedDuration > 0) {
            speedDuration--;
            speedActiveDuration.put(playerUUID, speedDuration);
            String runeDisplay = getRuneDisplay(RuneState.ACTIVE, player, speedDuration);
            setPlayerDisplay(player, runeDisplay);
            if (speedDuration == 0) {
                resetCooldown(player);
            }
            return;
        }

        if (isOnCooldown(player)) {
            String runeDisplay = getRuneDisplay(RuneState.COOLDOWN, player, 0);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        double maxHp = player.getMaxHealth();
        double damageThreshold = maxHp * (DAMAGE_THRESHOLD_PERCENTAGE / 100);
        double currentDamage = damageTracker.getOrDefault(playerUUID, 0.0);

        if (currentDamage >= damageThreshold) {
            enterActiveState(player);
            return;
        }

        String runeDisplay = getRuneDisplay(RuneState.IDLE, player, 0);
        setPlayerDisplay(player, runeDisplay);
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);

        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        ACTIVE, COOLDOWN, IDLE
    }

    private String getRuneDisplay(RuneState state, Player player, int remainingTicks) {
        return switch (state) {
            case COOLDOWN -> "§7👾 " + getCooldownDisplay(player);
            case ACTIVE -> {
                double remainingSeconds = remainingTicks / 20.0;
                yield String.format("§9👾 (%.1fs)", remainingSeconds);
            }
            case IDLE -> "§1👾";
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        UUID playerUUID = player.getUniqueId();
        int speedDuration = speedActiveDuration.getOrDefault(playerUUID, 0);
        if (speedDuration > 0) {
            return getRuneDisplay(RuneState.ACTIVE, player, speedDuration);
        }
        if (isOnCooldown(player)) {
            return getRuneDisplay(RuneState.COOLDOWN, player, 0);
        }
        return getRuneDisplay(RuneState.IDLE, player, 0);
    }

}