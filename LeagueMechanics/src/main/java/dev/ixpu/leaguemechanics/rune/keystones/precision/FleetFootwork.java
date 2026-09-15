package dev.ixpu.leaguemechanics.rune.keystones.precision;

import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import dev.ixpu.leaguemechanics.rune.StacksHandler;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.manager.StatScalingManager;

import java.util.*;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;


public class FleetFootwork extends StacksHandler {

    private double BLOCKS_PER_STACK = 10.0;
    private int PROJECTILE_STACK_GAIN = 5;
    private double HEAL_PERCENT = 25.0;

    private double AD_PERCENTAGE_MULTIPLIER = 10.0;
    private double AP_PERCENTAGE_MULTIPLIER = 5.0;

    private static final int MAXIMUM_STACKS = 100;
    private static final double MOVEMENT_SPEED_BONUS = 0.20;
    private static final int SPEED_BUFF_DURATION_TICKS = 100;

    private final Map<UUID, org.bukkit.Location> lastLocation = new HashMap<>();
    private final Map<UUID, Double> distanceAccumulator = new HashMap<>();
    private final Map<UUID, Integer> speedBuffTicks = new HashMap<>();
    private final Map<UUID, List<AttributeModifier>> activeState = new HashMap<>();

    public FleetFootwork(ConfigurationSection config) {
        super("fleet-footwork", RunePath.PRECISION, RuneSlot.KEYSTONE, 100);

        ConfigurationSection section = config.getConfigurationSection("runes.keystones.precision.fleet-footwork");
        if (section != null) {
            this.BLOCKS_PER_STACK = section.getDouble("blocks-per-stack", this.BLOCKS_PER_STACK);
            this.PROJECTILE_STACK_GAIN = section.getInt("projectile-stack-gain", this.PROJECTILE_STACK_GAIN);
            this.HEAL_PERCENT = section.getDouble("base-heal-percent", this.HEAL_PERCENT);
            this.AD_PERCENTAGE_MULTIPLIER = section.getDouble("ad-percentage-multiplier", this.AD_PERCENTAGE_MULTIPLIER);
            this.AP_PERCENTAGE_MULTIPLIER = section.getDouble("ap-percentage-multiplier", this.AP_PERCENTAGE_MULTIPLIER);
        }
    }

    @Override
    public void onEnable(Player player) {
        UUID uuid = player.getUniqueId();
        lastLocation.put(uuid, player.getLocation().clone());
        distanceAccumulator.put(uuid, 0.0);
        speedBuffTicks.put(uuid, 0);
    }

    @Override
    public void onDisable(Player player) {
        UUID uuid = player.getUniqueId();
        super.onDisable(player);
        lastLocation.remove(uuid);
        distanceAccumulator.remove(uuid);
        speedBuffTicks.remove(uuid);
        removeAllModifiers(player);
    }
    
    public void onProjectileHit(Player shooter, Entity target) {
        activateFleetFootwork(shooter, target);
    }

    private void activateFleetFootwork(Player player, Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget.getMaxHealth() < 20) {
            return;
        }

        int currentStacks = getStacks(player);

        for (int i = 0; i < PROJECTILE_STACK_GAIN; i++) {
            addStack(player);
        }

        if (currentStacks >= MAXIMUM_STACKS) {
            double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
            double currentHealth = player.getHealth();
            double missingHealth = maxHealth - currentHealth;
            double scaledHealPercent = getScaledHealPercentage(player);
            double healAmount = missingHealth * scaledHealPercent;

            player.setHealth(Math.min(maxHealth, currentHealth + healAmount));
            resetStacks(player);
            activateEffects(player);
            distanceAccumulator.put(player.getUniqueId(), 0.0);
        } else if (speedBuffTicks.getOrDefault(player.getUniqueId(), 0) > 0) {
            refreshSpeedBuff(player);
        }
    }

    private double getScaledHealPercentage(Player player) {
        StatScalingManager statScalingManager = new StatScalingManager();
        return statScalingManager.calculateScaledValue(
                player,
                HEAL_PERCENT,
                AD_PERCENTAGE_MULTIPLIER,
                AP_PERCENTAGE_MULTIPLIER
        );
    }

    private void trackActiveTimer(Player player) {
        UUID playerUUID = player.getUniqueId();

        org.bukkit.Location currentLoc = player.getLocation();
        org.bukkit.Location prevLoc = lastLocation.get(playerUUID);

        if (prevLoc != null && currentLoc.getWorld().equals(prevLoc.getWorld())) {
            double distance = currentLoc.distance(prevLoc);
            double accumulated = distanceAccumulator.getOrDefault(playerUUID, 0.0);
            accumulated += distance;

            while (accumulated >= BLOCKS_PER_STACK) {
                addStack(player);
                accumulated -= BLOCKS_PER_STACK;
            }

            distanceAccumulator.put(playerUUID, accumulated);
        }

        lastLocation.put(playerUUID, currentLoc.clone());

        int buffTicks = speedBuffTicks.getOrDefault(playerUUID, 0);
        if (buffTicks > 0) {
            buffTicks--;
            speedBuffTicks.put(playerUUID, buffTicks);

            if (buffTicks == 0) {
                removeAllModifiers(player);
                resetStacks(player);
            }
        }
    }

    @SuppressWarnings("removal")
    private void activateEffects(Player player) {
        UUID playerUUID = player.getUniqueId();
        removeAllModifiers(player);

        var modifier = new AttributeModifier(
                UUID.randomUUID(),
                "fleet-footwork-speed",
                MOVEMENT_SPEED_BONUS,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
        );

        var movementAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementAttr != null) {
            movementAttr.addModifier(modifier);
            activeState.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(modifier);
        }

        speedBuffTicks.put(playerUUID, SPEED_BUFF_DURATION_TICKS);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BREEZE_IDLE_AIR, 1.0f, 1.2f);
    }

    private void refreshSpeedBuff(Player player) {
        UUID playerUUID = player.getUniqueId();
        speedBuffTicks.put(playerUUID, SPEED_BUFF_DURATION_TICKS);
    }

    private void removeAllModifiers(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<AttributeModifier> mods = activeState.getOrDefault(playerUUID, new ArrayList<>());
        for (AttributeModifier mod : mods) {
            try {
                Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).removeModifier(mod);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BREEZE_LAND, 1.0f, 1.2f);
            } catch (Exception e) {
                //
            }
        }
        activeState.put(playerUUID, new ArrayList<>());
    }

    @Override
    public void tick(Player player) {
        trackActiveTimer(player);

        int stacks = getStacks(player);
        int buffTicks = speedBuffTicks.getOrDefault(player.getUniqueId(), 0);

        if (buffTicks > 0) {
            String runeDisplay = getRuneDisplay(RuneState.ACTIVE, stacks, buffTicks);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        if (stacks > 0) {
            String runeDisplay = getRuneDisplay(RuneState.STACKING, stacks, buffTicks);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        String runeDisplay = getRuneDisplay(RuneState.IDLE, stacks, buffTicks);
        setPlayerDisplay(player, runeDisplay);
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);

        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        STACKING, ACTIVE, IDLE
    }

    private String getRuneDisplay(RuneState state, int stacks, int buffTicks) {
        return switch (state) {
            case ACTIVE -> {
                double remainingSeconds = buffTicks / 20.0;
                yield String.format("§e👣 (%.1fs)", remainingSeconds);
            }
            case STACKING -> "§6👣 " + stacks + "/" + MAXIMUM_STACKS;
            case IDLE -> "§6👣";
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        int stacks = getStacks(player);
        int buffTicks = speedBuffTicks.getOrDefault(player.getUniqueId(), 0);
        if (buffTicks > 0) {
            return getRuneDisplay(RuneState.ACTIVE, stacks, buffTicks);
        }
        if (stacks > 0) {
            return getRuneDisplay(RuneState.STACKING, stacks, buffTicks);
        }
        return getRuneDisplay(RuneState.IDLE, stacks, buffTicks);
    }
}