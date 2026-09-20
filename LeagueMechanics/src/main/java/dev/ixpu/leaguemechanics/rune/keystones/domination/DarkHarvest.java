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

import dev.ixpu.leaguemechanics.util.ItemModifier;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;


public class DarkHarvest extends StacksHandler {

    private double BASE_ADAPTIVE_DAMAGE_PER_STACK;

    int COOLDOWN_DURATION_SECONDS;

    private static final double HEALTH_THRESHOLD = 50;
    private static final int REAP_DELAY_TICKS = 75;

    private LeagueMechanics plugin;
    private PlayerEventListener listener;
    private boolean lastDamageWasMagic = false;

    public DarkHarvest(ConfigurationSection config, PlayerEventListener listener) {
        super("dark-harvest", RunePath.DOMINATION, RuneSlot.KEYSTONE, 20);
        this.listener = listener;
        ConfigurationSection section = config.getConfigurationSection("runes.keystones.domination.dark-harvest");
        if (section != null) {
            this.BASE_ADAPTIVE_DAMAGE_PER_STACK = section.getDouble("adaptive-damage-per-stack", this.BASE_ADAPTIVE_DAMAGE_PER_STACK);
            this.COOLDOWN_DURATION_SECONDS = section.getInt("cooldown", COOLDOWN_DURATION_SECONDS);
        }
        this.setCooldownSeconds(COOLDOWN_DURATION_SECONDS);
    }

    @Override
    public void onEnable(Player player) {
        //
    }

    public void onProjectileHit(Player shooter, Entity target) {
        activateDarkHarvest(shooter, target);
    }

    public void onAttack(Player attacker, Entity target) {
        activateDarkHarvest(attacker, target);
    }

    private void activateDarkHarvest(Player player, Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget.getMaxHealth() < 20) {
            return;
        }
        if(!listener.letRunesThrough(player)) {
            return;
        }

        double targetHealth = livingTarget.getHealth();
        double maxHealth = livingTarget.getMaxHealth();
        double healthPercent = targetHealth / maxHealth;
        double threshold = HEALTH_THRESHOLD / 100;

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

        if (healthPercent >= threshold) {
            return;
        }
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

        DebugLogger.debug(player, "§f[§dSource§f] §f[§cDark Harvest§f] Keystone Damage = §d" + Math.ceil(keystoneDamage(player, target) * 100) / 100.0 + "§f | Type = §d" + DamageType);
        DebugLogger.debug(player, "§f[§dTarget§f] New Health = §d" + Math.ceil(newHealth * 100) / 100.0);

        livingTarget.setHealth(newHealth);

        if (isOnCooldown(player)) {
            return;
        }

        scheduleAddStack(player);
        resetCooldown(player);
    }

    private double keystoneDamage(Player player, Entity target) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enablePerStackScaling();
        damageManager.enableAdaptiveDamage();
        int currentStacks = getStacks(player);
        double damage = damageManager.DamageCalculation(player, target, currentStacks, BASE_ADAPTIVE_DAMAGE_PER_STACK, 0, 0);
        this.lastDamageWasMagic = damageManager.isMagicDamage();
        return damage;
    }

    private void scheduleAddStack(Player attacker) {
        if (plugin == null) {
            stackCost(attacker);
            return;
        }
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> stackCost(attacker), REAP_DELAY_TICKS);
    }

    private void stackCost(Player player) {
        if (getStacks(player) >= maxStacks) {
            return;
        }
        addStack(player);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);
    }

    @Override
    public void tick(Player player) {
        int currentStacks = getStacks(player);

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

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);

        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        COOLDOWN, STACKING, IDLE
    }

    private String getRuneDisplay(Player player, RuneState state, int currentStacks) {
        return switch (state) {
            case COOLDOWN -> "§7👻 " + currentStacks + "/" + maxStacks + " | " + getCooldownDisplay(player);
            case STACKING -> "§c👻 " + currentStacks + "/" + maxStacks;
            case IDLE -> "§4👻";
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        int currentStacks = getStacks(player);
        if (isOnCooldown(player)) {
            return getRuneDisplay(player, RuneState.COOLDOWN, currentStacks);
        }
        if (currentStacks > 0) {
            return getRuneDisplay(player, RuneState.STACKING, currentStacks);
        }
        return getRuneDisplay(player, RuneState.IDLE, currentStacks);
    }
}