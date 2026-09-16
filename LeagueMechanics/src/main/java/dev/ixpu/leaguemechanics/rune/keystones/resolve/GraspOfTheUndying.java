package dev.ixpu.leaguemechanics.rune.keystones.resolve;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.util.DebugLogger;

import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import dev.ixpu.leaguemechanics.rune.StacksHandler;

import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;


import java.util.*;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;


public class GraspOfTheUndying extends StacksHandler {
    private double HEAL_PERCENT;

    int COOLDOWN_DURATION_SECONDS;

    private PlayerEventListener listener;

    private static final int ATTACK_WINDOW_TICKS = 100;
    private static final int ABSORPTION_DURATION_TICKS = Integer.MAX_VALUE;

    private final Map<UUID, Integer> activationState = new HashMap<>();
    private final Map<UUID, Integer> totalAbsorptionHearts = new HashMap<>();
    private final Map<UUID, Boolean> activeStateActive = new HashMap<>();

    public GraspOfTheUndying(org.bukkit.configuration.ConfigurationSection config, PlayerEventListener listener) {
        super("grasp-of-the-undying", RunePath.RESOLVE, RuneSlot.KEYSTONE, 4, 60);
        this.listener = listener;
        ConfigurationSection section = config.getConfigurationSection("runes.keystones.resolve.grasp-of-the-undying");

        if (section != null) {
            this.HEAL_PERCENT = section.getDouble("heal-percent", this.HEAL_PERCENT);
            this.COOLDOWN_DURATION_SECONDS = section.getInt("cooldown", COOLDOWN_DURATION_SECONDS);
        }
        this.setCooldownSeconds(COOLDOWN_DURATION_SECONDS);
    }

    @Override
    public void onEnable(Player player) {
        UUID uuid = player.getUniqueId();
        activationState.put(uuid, 0);
        totalAbsorptionHearts.put(uuid, 0);
        activeStateActive.put(uuid, false);
    }

    @Override
    public void onDisable(Player player) {
        UUID uuid = player.getUniqueId();
        super.onDisable(player);
        activationState.remove(uuid);
        totalAbsorptionHearts.remove(uuid);
        activeStateActive.remove(uuid);
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

        if (stacks >= maxStacks && attackWindow > 0 && !activeStateActive.getOrDefault(playerUUID, false)) {
            enterActiveState(player, target);
            resetStacks(player);
            activationState.put(playerUUID, 0);
            resetCooldown(player);
        }
    }

    private void enterActiveState(Player player, Entity target) {
        UUID playerUUID = player.getUniqueId();
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        int absorptionHearts = totalAbsorptionHearts.getOrDefault(playerUUID, 0) / 2;
        double damageToApply = keystoneDamage(player, target) * absorptionHearts * 0.2;

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

        DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] §f[§aGrasp Of The Undying§f] Keystone Damage = §d" + (keystoneDamage(player, target) * absorptionHearts * 0.2));
        DebugLogger.debug(player, "§7[Debug] §f[§dTarget§f] Target New HP = §d" + newHealth);

        if (livingTarget instanceof Player livingPlayer) {
            KillSourceTracker.getInstance().setSource(livingPlayer, player);
        }
        livingTarget.setHealth(newHealth);

        var maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double healPercent = HEAL_PERCENT / 100;
            double maxHealth = maxHealthAttr.getValue();
            double healAmount = maxHealth * healPercent;
            double currentHealth = player.getHealth();
            player.setHealth(Math.min(maxHealth, currentHealth + healAmount));
        }

        activateEffects(player);

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.0f);
    }

    private double keystoneDamage(Player player, Entity target) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        return damageManager.DamageCalculation(player, target, 0, 0, 0, 0);
    }

    private void activateEffects(Player player) {
        UUID playerUUID = player.getUniqueId();
        int currentAbsorptionHearts = totalAbsorptionHearts.getOrDefault(playerUUID, 0);
        currentAbsorptionHearts++;
        totalAbsorptionHearts.put(playerUUID, currentAbsorptionHearts);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,
                ABSORPTION_DURATION_TICKS,
                currentAbsorptionHearts - 1,
                false,
                false
        ));
    }

    public void resetAbsorption(Player player) {
        UUID playerUUID = player.getUniqueId();
        totalAbsorptionHearts.put(playerUUID, 0);
        player.removePotionEffect(PotionEffectType.ABSORPTION);
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

        if (player.getAbsorptionAmount() <= 0 && totalAbsorptionHearts.getOrDefault(playerUUID, 0) > 0) {
            resetAbsorption(player);
        }

        int attackWindow = activationState.getOrDefault(playerUUID, 0);
        if (attackWindow > 0) {
            attackWindow--;
            activationState.put(playerUUID, attackWindow);

            if (attackWindow == 0) {
                resetStacks(player);
                activeStateActive.put(playerUUID, false);
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
            case COOLDOWN -> "§7🥊 " + getCooldownDisplay(player);
            case ACTIVE -> {
                double remainingSeconds = attackWindow / 20.0;
                yield String.format("§a🥊 (%.1fs)", remainingSeconds);
            }
            case STACKING -> "§2🥊 " + stacks + "/" + maxStacks;
            case IDLE -> "§2🥊";
        };
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