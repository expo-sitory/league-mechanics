package dev.ixpu.leaguemechanics.rune.keystones.resolve;

import dev.ixpu.leaguemechanics.entity.player.PlayerStats;

import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import dev.ixpu.leaguemechanics.rune.StacksHandler;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;


import java.util.*;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;


public class GraspOfTheUndying extends StacksHandler {
    int COOLDOWN_DURATION_SECONDS;

    private PlayerEventListener listener;

    private static final int ATTACK_WINDOW_TICKS = 100;

    private final Map<UUID, Integer> activationState = new HashMap<>();

    public GraspOfTheUndying(org.bukkit.configuration.ConfigurationSection config, PlayerEventListener listener) {
        super("grasp-of-the-undying", RunePath.RESOLVE, RuneSlot.KEYSTONE, 4, 60);
        this.listener = listener;
        ConfigurationSection section = config.getConfigurationSection("runes.keystones.resolve.grasp-of-the-undying");

        if (section != null) {
            this.COOLDOWN_DURATION_SECONDS = section.getInt("cooldown", COOLDOWN_DURATION_SECONDS);
        }
        this.setCooldownSeconds(COOLDOWN_DURATION_SECONDS);
    }

    @Override
    public void onEnable(Player player) {
        UUID uuid = player.getUniqueId();
        activationState.put(uuid, 0);
    }

    @Override
    public void onDisable(Player player) {
        UUID uuid = player.getUniqueId();
        super.onDisable(player);
        PlayerStats.getOrCreate(player).setGraspHearts(0);
        activationState.remove(uuid);
    }

    public void onProjectileHit(Player shooter, Entity target) {
        activateGraspOfTheUndying(shooter, target);
    }

    public void onAttack(Player attacker, Entity target) {
        activateGraspOfTheUndying(attacker, target);
    }

    public void activateGraspOfTheUndying(Player player, Entity target) {
        UUID playerUUID = player.getUniqueId();
        int stacks = getStacks(player);
        int attackWindow = activationState.getOrDefault(playerUUID, 0);

        if (isOnCooldown(player)) {
            return;
        }
        if (getStacks(player) == maxStacks) {
            activationState.put(player.getUniqueId(), ATTACK_WINDOW_TICKS);
        }
        if(listener.isAnyHotbarOnCooldown(player) && !listener.letRunesThrough(player)) {
            return;
        }
        addStack(player);

        if (stacks >= maxStacks && attackWindow > 0) {
            enterActiveState(player, target);
            resetStacks(player);
            activationState.put(playerUUID, 0);
            resetCooldown(player);
        }
    }

    private void enterActiveState(Player player, Entity target) {
        if (!(target instanceof LivingEntity)) {
            return;
        }

        PlayerStats stats = PlayerStats.getOrCreate(player);
        if (stats.getGraspHearts() < 30) {
            stats.addGraspHearts(1);
            listener.applyHealthModifier(player);
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.0f);
    }

    @Override
    public void tick(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (isOnCooldown(player)) {
            String runeDisplay = getRuneDisplay(RuneState.COOLDOWN, player, 0, 0);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        tickStackExpiry(player);

        int attackWindow = activationState.getOrDefault(playerUUID, 0);
        if (attackWindow > 0) {
            attackWindow--;
            activationState.put(playerUUID, attackWindow);

            if (attackWindow == 0) {
                resetStacks(player);
            }
        }

        int stacks = getStacks(player);

        RuneState state;
        if (stacks >= maxStacks && attackWindow > 0) {
            state = RuneState.ACTIVE;
        } else if (stacks > 0) {
            state = RuneState.STACKING;
        } else {
            state = RuneState.IDLE;
        }

        String runeDisplay = getRuneDisplay(state, player, stacks, attackWindow);
        setPlayerDisplay(player, runeDisplay);
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);

        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        COOLDOWN, STACKING, ACTIVE, IDLE
    }

    private String getRuneDisplay(RuneState state, Player player, int stacks, int attackWindow) {
        return switch (state) {
            case COOLDOWN -> "§7🥊 " + getCooldownDisplay(player) + " [" + getCurrentGraspHearts(player) + "/30]";
            case ACTIVE -> {
                double remainingSeconds = attackWindow / 20.0;
                yield String.format("§a🥊 (%.1fs)", remainingSeconds) + " [" + getCurrentGraspHearts(player) + "/30]";
            }
            case STACKING -> "§2🥊 " + stacks + "/" + maxStacks + " [" + getCurrentGraspHearts(player) + "/30]";
            case IDLE -> "§2🥊 [" + getCurrentGraspHearts(player) + "/30]";
        };
    }

    public int getCurrentGraspHearts(Player player) {
        return PlayerStats.getOrCreate(player).getGraspHearts();
    }

    @Override
    public String getDisplaySection(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (isOnCooldown(player)) {
            return getRuneDisplay(RuneState.COOLDOWN, player, 0, 0);
        }
        int stacks = getStacks(player);
        int attackWindow = activationState.getOrDefault(playerUUID, 0);
        RuneState state;
        if (stacks >= maxStacks && attackWindow > 0) {
            state = RuneState.ACTIVE;
        } else if (stacks > 0) {
            state = RuneState.STACKING;
        } else {
            state = RuneState.IDLE;
        }
        return getRuneDisplay(state, player, stacks, attackWindow);
    }

}