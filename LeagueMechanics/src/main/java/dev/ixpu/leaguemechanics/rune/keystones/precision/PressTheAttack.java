package dev.ixpu.leaguemechanics.rune.keystones.precision;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import dev.ixpu.leaguemechanics.util.DebugLogger;

import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import dev.ixpu.leaguemechanics.rune.StacksHandler;

import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;

import java.util.*;

import dev.ixpu.leaguemechanics.util.ItemModifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;


public class PressTheAttack extends StacksHandler {

    private double BASE_ADAPTIVE_DAMAGE ;

    int COOLDOWN_DURATION_SECONDS;

    private PlayerEventListener listener;
    private boolean lastDamageWasMagic = false;

    private static final int MAX_STACKS = 3;

    public PressTheAttack(org.bukkit.configuration.ConfigurationSection config, PlayerEventListener listener) {
        super("press-the-attack", RunePath.PRECISION, RuneSlot.KEYSTONE, 3, 60);
        this.listener = listener;
        enablePerTargetStacking();
        enablePerTargetExpiry();

        ConfigurationSection section = config.getConfigurationSection("runes.keystones.precision.press-the-attack");

        if (section != null) {
            this.BASE_ADAPTIVE_DAMAGE = section.getDouble("base-adaptive-damage", this.BASE_ADAPTIVE_DAMAGE);
            this.COOLDOWN_DURATION_SECONDS = section.getInt("cooldown", COOLDOWN_DURATION_SECONDS);
        }
        this.setCooldownSeconds(COOLDOWN_DURATION_SECONDS);
    }

    @Override
    public void onEnable(Player player) {
        //
    }

    public void onAttack(Player attacker, Entity target) {
        activatePressTheAttack(attacker, target);
    }

    private void activatePressTheAttack(Player player, Entity target) {
        UUID targetUUID = target.getUniqueId();

        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget.getMaxHealth() < 20) {
            return;
        }
        if (isOnCooldown(player)){
            return;
        }
        if (!listener.letRunesThrough(player)) {
            return;
        }

        switchTarget(player, targetUUID);
        int currentStacks = getStacks(player, targetUUID);

        if (currentStacks == 2) {
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
            String DamageType;
            boolean isMagic = lastDamageWasMagic;

            if (isMagic) {
                DamageType = "Magic Damage";
                for (ItemStack inv : player.getInventory().getContents()) {
                    if (inv == null || inv.getType().isAir()) continue;
                    String itemId = ItemModifier.getItemId(inv);
                    ItemPassive passive = ItemPassivesRegistry.getInstance().getPassive(itemId);
                    if (passive != null) {
                        passive.onDealDamage(player, livingTarget, damageToApply, false, true);
                    }
                }
            } else {
                DamageType = "Physical Damage";
            }

            DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] §f[§ePress The Attack§f] Keystone Damage = §d" + Math.ceil(keystoneDamage(player, target) * 100) / 100.0);
            DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] Keystone Damage Type = §d" + DamageType);
            DebugLogger.debug(player, "§7[Debug] §f[§dTarget§f] Target New HP = §d" + Math.ceil(newHealth * 100) / 100.0);

            livingTarget.setHealth(newHealth);
            resetStacksForTarget(player, targetUUID);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "press-the-attack-stack-sound " + player.getName());
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.5f);
            resetCooldown(player);
        } else {
            addStack(player, targetUUID);
        }
    }

    private double keystoneDamage(Player player, Entity target) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableAdaptiveDamage();
        double damage = damageManager.DamageCalculation(player, target, 0, BASE_ADAPTIVE_DAMAGE, 0, 0);
        this.lastDamageWasMagic = damageManager.isMagicDamage();
        return damage;
    }

    private int trackActiveStacks(Player player) {
        tickStackExpiry(player);
        UUID lastTargetUUID = lastTarget.getOrDefault(player.getUniqueId(), null);
        int currentStacks = 0;
        if (lastTargetUUID != null) {
            currentStacks = getStacks(player, lastTargetUUID);
        }
        return currentStacks;
    }

    @Override
    public void tick(Player player) {
        int currentStacks = trackActiveStacks(player);
        String cooldownDisplay = getCooldownDisplay(player);
        if (!cooldownDisplay.isEmpty()) {
            String runeDisplay = getRuneDisplay(RuneState.COOLDOWN, currentStacks, cooldownDisplay);
            setPlayerDisplay(player, runeDisplay);
            return;
        }
        RuneState state = currentStacks == 0 ? RuneState.IDLE : RuneState.ACTIVE;
        String runeDisplay = getRuneDisplay(state, currentStacks, cooldownDisplay);
        setPlayerDisplay(player, runeDisplay);
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);

        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        COOLDOWN, IDLE, ACTIVE
    }

    private String getRuneDisplay(RuneState state, int stacks, String cooldown) {
        return switch (state) {
            case COOLDOWN -> "§7✳ " + cooldown;
            case ACTIVE -> "§e✳ " + stacks + "/" + MAX_STACKS;
            case IDLE -> "§6✳";
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        int currentStacks = trackActiveStacks(player);
        String cooldown = getCooldownDisplay(player);
        if (!cooldown.isEmpty()) {
            return getRuneDisplay(RuneState.COOLDOWN, currentStacks, cooldown);
        }
        RuneState state = currentStacks == 0 ? RuneState.IDLE : RuneState.ACTIVE;
        return getRuneDisplay(state, currentStacks, cooldown);
    }
}