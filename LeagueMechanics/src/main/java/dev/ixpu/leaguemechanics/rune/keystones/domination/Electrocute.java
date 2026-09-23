package dev.ixpu.leaguemechanics.rune.keystones.domination;

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

import java.util.UUID;

import dev.ixpu.leaguemechanics.util.ItemModifier;
import org.bukkit.Sound;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;


public class Electrocute extends StacksHandler {

    private double BASE_ADAPTIVE_DAMAGE;

    int COOLDOWN_DURATION_SECONDS;

    private static final int MAXIMUM_STACKS = 3;

    private PlayerEventListener listener;
    private boolean lastDamageWasMagic = false;


    public Electrocute(org.bukkit.configuration.ConfigurationSection config, PlayerEventListener listener) {
        super("electrocute", RunePath.DOMINATION, RuneSlot.KEYSTONE, 3, 60);
        this.listener = listener;
        enablePerTargetStacking();
        enablePerTargetExpiry();

        ConfigurationSection section = config.getConfigurationSection("runes.keystones.domination.electrocute");
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

    public void onProjectileHit(Player shooter, Entity target) {
        activateElectrocute(shooter, target);
    }

    public void onAttack(Player attacker, Entity target) {
        activateElectrocute(attacker, target);
    }


    private void activateElectrocute(Player player, Entity target) {
        UUID targetUUID = target.getUniqueId();

        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget.getMaxHealth() < 20) {
            return;
        }
        if (isOnCooldown(player)) {
            return;
        }
        if(!listener.letRunesThrough(player)) {
            return;
        }

        switchTarget(player, targetUUID);
        addStack(player, targetUUID);

        int stacks = getStacks(player, targetUUID);

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

        if (stacks >= MAXIMUM_STACKS) {
            target.getWorld().strikeLightning(target.getLocation());
            resetStacksForTarget(player, targetUUID);
            resetCooldown(player);

            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.2f);
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.0f);

            if (livingTarget instanceof Player livingTargetPlayer) {
                KillSourceTracker.getInstance().setSource(livingTargetPlayer, player);
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

            DebugLogger.debug(player, "§f[§dSource§f] §f[§cElectrocute§f] Keystone Damage = §d" + Math.ceil(keystoneDamage(player, target) * 100) / 100.0);
            DebugLogger.debug(player, "§f[§dSource§f] Keystone Damage Type = §d" + DamageType);
            DebugLogger.debug(player, "§f[§dTarget§f] Target New HP = §d" + Math.ceil(newHealth * 100) / 100.0);

            if (newHealth <= 0) {
                livingTarget.damage(livingTarget.getHealth());
            } else {
                livingTarget.setHealth(newHealth);
            }
        }
    }

    private double keystoneDamage(Player player, Entity target) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableAdaptiveDamage();
        double damage = damageManager.DamageCalculation(player, target, 0, BASE_ADAPTIVE_DAMAGE, 0, 0);
        this.lastDamageWasMagic = damageManager.isMagicDamage();
        return damage;
    }

    private int trackPerTargetStacks(Player player) {
        tickStackExpiry(player);
        UUID lastTargetUUID = lastTarget.getOrDefault(player.getUniqueId(), null);
        int maxStacks = 0;

        if (lastTargetUUID != null) {
            maxStacks = getStacks(player, lastTargetUUID);
        }
        return maxStacks;
    }

    @Override
    public void tick(Player player) {
        int currentStacks = trackPerTargetStacks(player);

        if (isOnCooldown(player)) {
            String runeDisplay = getRuneDisplay(player, RuneState.COOLDOWN, currentStacks);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        if (currentStacks > 0) {
            String runeDisplay = getRuneDisplay(player, RuneState.STACKING, currentStacks);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        String runeDisplay = getRuneDisplay(player, RuneState.IDLE, currentStacks);
        setPlayerDisplay(player, runeDisplay);
    }

    @Override
    public String getDisplaySection(Player player) {
        int currentStacks = trackPerTargetStacks(player);
        if (isOnCooldown(player)) {
            return getRuneDisplay(player, RuneState.COOLDOWN, currentStacks);
        }
        if (currentStacks > 0) {
            return getRuneDisplay(player, RuneState.STACKING, currentStacks);
        }
        return getRuneDisplay(player, RuneState.IDLE, currentStacks);
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableAdaptiveDamage();

        String statsDisplay = playerStats.getActionBarSections(player);
        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        COOLDOWN, STACKING, IDLE
    }

    private String getRuneDisplay(Player player, RuneState state, int currentStacks) {
        return switch (state) {
            case COOLDOWN -> "§7⚡ " + getCooldownDisplay(player);
            case STACKING -> "§c⚡ " + currentStacks + "/" + MAXIMUM_STACKS;
            case IDLE -> "§4⚡";
        };
    }
}