package dev.ixpu.leaguemechanics.rune.keystones.precision;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;

import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import dev.ixpu.leaguemechanics.rune.StacksHandler;

import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;

import java.util.UUID;

import dev.ixpu.leaguemechanics.util.DebugLogger;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;


public class Conqueror extends StacksHandler {

    private double BASE_ADAPTIVE_DAMAGE_PER_STACK;

    private static final int MAXIMUM_STACKS = 12;

    private PlayerEventListener listener;
    private boolean lastDamageWasMagic = false;

    public Conqueror(ConfigurationSection config, PlayerEventListener listener) {
        super("conqueror", RunePath.PRECISION, RuneSlot.KEYSTONE, 12, 100);
        this.listener = listener;
        enablePerTargetStacking();
        enablePerTargetExpiry();

        ConfigurationSection section = config.getConfigurationSection("runes.keystones.precision.conqueror");
        if (section != null) {
            this.BASE_ADAPTIVE_DAMAGE_PER_STACK = section.getDouble("base-adaptive-damage-per-stack", this.BASE_ADAPTIVE_DAMAGE_PER_STACK);
        }
    }

    @Override
    public void onEnable(Player player) {
        //
    }

    public void onProjectileHit(Player shooter, Entity target) {
        activateConqueror(shooter, target);
    }

    public void onAttack(Player attacker, Entity target) {
        activateConqueror(attacker, target);
    }

    private void activateConqueror(Player player, Entity target) {
        UUID targetUUID = target.getUniqueId();

        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget.getMaxHealth() < 20) {
            return;
        }
        if (!listener.letRunesThrough(player)) {
            return;
        }

        switchTarget(player, targetUUID);
        addStack(player, targetUUID);

        double damageToApply = keystoneDamage(player, target, getStacks(player, targetUUID));

        if (livingTarget instanceof Player livingPlayer) {
            KillSourceTracker.getInstance().setSource(livingPlayer, player);
        }
        livingTarget.damage(damageToApply);

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

        DebugLogger.debug(player, "§f[§dSource§f] [§eConqueror§f] Keystone Damage = §d" + Math.ceil(keystoneDamage(player, target, getStacks(player, targetUUID)) * 100) / 100.0);
        DebugLogger.debug(player, "§f[§dSource§f] Keystone Damage Type = §d" + DamageType);
    }

    private double keystoneDamage(Player player, Entity target, int currentStacks) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableAdaptiveDamage();
        damageManager.enableAdaptiveScaling();
        damageManager.enablePerStackScaling();
        double damage = damageManager.DamageCalculation(player, target, currentStacks, BASE_ADAPTIVE_DAMAGE_PER_STACK, 0, 0);
        this.lastDamageWasMagic = damageManager.isMagicDamage();
        return damage;}

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
    protected void onStackAdded(Player player, int newStackCount) {
        if (newStackCount == 1) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "conqueror-stack-sound " + player.getName());
        } else if (newStackCount == MAXIMUM_STACKS) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "conqueror-max-stack-sound " + player.getName());
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.5f);
        }
    }

    @Override
    protected void onStacksExpired(Player player) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "conqueror-expired-sound " + player.getName());
    }

    @Override
    public void tick(Player player) {
        int stacks = trackActiveStacks(player);

        if (stacks > 0) {
            String runeDisplay = getRuneDisplay(RuneState.STACKING, stacks);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        String runeDisplay = getRuneDisplay(RuneState.IDLE, stacks);
        setPlayerDisplay(player, runeDisplay);
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);

        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        STACKING, IDLE
    }

    private String getRuneDisplay(RuneState state, int currentStacks) {
        return switch (state) {
            case STACKING -> "§e🪓 " + currentStacks + "/" + MAXIMUM_STACKS;
            case IDLE -> "§6🪓";
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        int stacks = trackActiveStacks(player);
        if (stacks > 0) {
            return getRuneDisplay(RuneState.STACKING, stacks);
        }
        return getRuneDisplay(RuneState.IDLE, stacks);
    }
}