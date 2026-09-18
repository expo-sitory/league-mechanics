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
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;


public class LethalTempo extends StacksHandler {

    private double ATTACK_SPEED;
    private double BASE_ADAPTIVE_DAMAGE;

    private PlayerEventListener listener;
    private boolean lastDamageWasMagic = false;

    private static final int MAXIMUM_STACKS = 6;
    private static final int ACTIVE_DURATION_TICKS = 60;
    private static final int STACK_DURATION_TICKS = 300;

    int COOLDOWN_DURATION_SECONDS ;

    private final Map<UUID, RuneState> playerState = new HashMap<>();
    private final Map<UUID, Integer> activeState = new HashMap<>();
    private final Map<UUID, Double> activeASBonus = new HashMap<>();
    private final Map<UUID, Map<UUID, List<Long>>> stackTimestamps = new HashMap<>();

    public LethalTempo(org.bukkit.configuration.ConfigurationSection config, PlayerEventListener listener) {
        super("lethal-tempo", RunePath.PRECISION, RuneSlot.KEYSTONE, 6, 120);
        this.listener = listener;
        enablePerTargetStacking();

        ConfigurationSection section = config.getConfigurationSection("runes.keystones.precision.lethal-tempo");

        if (section != null) {
            this.ATTACK_SPEED = section.getDouble("attack-speed", this.ATTACK_SPEED);
            this.BASE_ADAPTIVE_DAMAGE = section.getDouble("base-adaptive-damage", this.BASE_ADAPTIVE_DAMAGE);
            this.COOLDOWN_DURATION_SECONDS = section.getInt("cooldown", COOLDOWN_DURATION_SECONDS);
        }
        this.setCooldownSeconds(COOLDOWN_DURATION_SECONDS);
    }

    @Override
    public void onEnable(Player player) {
        UUID uuid = player.getUniqueId();
        playerState.put(uuid, RuneState.STACKING);
        activeState.put(uuid, 0);
        activeASBonus.put(uuid, 0.0);
    }

    @Override
    public void onDisable(Player player) {
        UUID uuid = player.getUniqueId();
        super.onDisable(player);
        playerState.remove(uuid);
        activeState.remove(uuid);
        activeASBonus.remove(uuid);
        stackTimestamps.remove(uuid);
        clearPlayerCooldown(player);
    }

    public void onProjectileHit(Player shooter, Entity target) {
        activateLethalTempo(shooter, target);
    }

    public void onAttack(Player attacker, Entity target) {
        activateLethalTempo(attacker, target);
    }

    public void activateLethalTempo(Player player, Entity target) {
        UUID targetUUID = target.getUniqueId();
        RuneState state = playerState.getOrDefault(player.getUniqueId(), RuneState.STACKING);

        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget.getMaxHealth() < 20) {
            return;
        }
        if (isOnCooldown(player)) {
            return;
        }
        if (!listener.letRunesThrough(player)) {
            return;
        }

        switchTarget(player, targetUUID);

        if (state == RuneState.ACTIVE) {
            double damageToApply = keystoneDamage(player, target, getValidStackCount(player, targetUUID));

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

            DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] §f[§eLethal Tempo§f] Keystone Damage = §d" + Math.ceil(keystoneDamage(player, target, getValidStackCount(player, targetUUID)) * 100) / 100.0);
            DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] Keystone Damage = §d" + DamageType);
            DebugLogger.debug(player, "§7[Debug] §f[§dTarget§f] Target New HP = §d" + Math.ceil(newHealth * 100) / 100.0);

            livingTarget.setHealth(newHealth);
            refreshActiveTimer(player);
            return;
        }

        if (state == RuneState.STACKING) {
            addStackForTarget(player, targetUUID);
        }
    }

    private double keystoneDamage(Player player, Entity target, int currentStacks) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableAdaptiveDamage();
        double damage = damageManager.DamageCalculation(player, target, currentStacks, BASE_ADAPTIVE_DAMAGE, 0, 0);
        this.lastDamageWasMagic = damageManager.isMagicDamage();
        return damage;
    }

    private void addStackForTarget(Player player, UUID targetUUID) {
        recordStackTimestamp(player, targetUUID);
        int currentStacks = getValidStackCount(player, targetUUID);

        if (currentStacks == MAXIMUM_STACKS) {
            enterActiveState(player);
        }
    }

    private void recordStackTimestamp(Player player, UUID targetUUID) {
        UUID playerUUID = player.getUniqueId();
        Map<UUID, List<Long>> playerTimestamps = stackTimestamps.computeIfAbsent(playerUUID, k -> new HashMap<>());
        List<Long> targetTimestamps = playerTimestamps.computeIfAbsent(targetUUID, k -> new ArrayList<>());
        targetTimestamps.add(System.currentTimeMillis());
    }

    private void expireOldStacks(Player player, UUID targetUUID) {
        UUID playerUUID = player.getUniqueId();
        Map<UUID, List<Long>> playerTimestamps = stackTimestamps.get(playerUUID);

        if (playerTimestamps == null) {
            return;
        }

        List<Long> targetTimestamps = playerTimestamps.get(targetUUID);
        if (targetTimestamps == null || targetTimestamps.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long stackDurationMs = STACK_DURATION_TICKS * 50L;

        targetTimestamps.removeIf(timestamp -> (currentTime - timestamp) > stackDurationMs);
    }

    private int getValidStackCount(Player player, UUID targetUUID) {
        expireOldStacks(player, targetUUID);
        UUID playerUUID = player.getUniqueId();
        Map<UUID, List<Long>> playerTimestamps = stackTimestamps.get(playerUUID);

        if (playerTimestamps == null) {
            return 0;
        }

        List<Long> targetTimestamps = playerTimestamps.get(targetUUID);
        if (targetTimestamps == null) {
            return 0;
        }

        return Math.min(targetTimestamps.size(), MAXIMUM_STACKS);
    }

    private void enterActiveState(Player player) {
        UUID playerUUID = player.getUniqueId();
        playerState.put(playerUUID, RuneState.ACTIVE);
        activeState.put(playerUUID, ACTIVE_DURATION_TICKS);
        activeASBonus.put(playerUUID, ATTACK_SPEED);
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.2f);
    }

    private void refreshActiveTimer(Player player) {
        UUID playerUUID = player.getUniqueId();
        activeState.put(playerUUID, ACTIVE_DURATION_TICKS);
    }

    public double getActiveASBonus(Player player) {
        return activeASBonus.getOrDefault(player.getUniqueId(), 0.0);
    }

    @Override
    public void tick(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (isOnCooldown(player)) {
            String runeDisplay = getRuneDisplay(RuneState.COOLDOWN, player, 0);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        RuneState state = playerState.getOrDefault(playerUUID, RuneState.STACKING);

        if (state == RuneState.STACKING) {
            UUID lastTargetUUID = lastTarget.getOrDefault(playerUUID, null);
            int currentStacks = 0;
            if (lastTargetUUID != null) {
                currentStacks = getValidStackCount(player, lastTargetUUID);
            }
            String runeDisplay = getRuneDisplay(RuneState.STACKING, player, currentStacks);
            setPlayerDisplay(player, runeDisplay);

        } else if (state == RuneState.ACTIVE) {
            int activeTime = activeState.get(playerUUID);
            activeTime--;
            activeState.put(playerUUID, activeTime);

            String runeDisplay = getRuneDisplay(RuneState.ACTIVE, player, activeTime);
            setPlayerDisplay(player, runeDisplay);

            if (activeTime == 0) {
                resetCooldown(player);
                playerState.put(playerUUID, RuneState.STACKING);
                activeASBonus.put(playerUUID, 0.0);
                clearPlayerTimestamps(player);
                player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.2f);
            }
        }
    }

    private void clearPlayerTimestamps(Player player) {
        stackTimestamps.remove(player.getUniqueId());
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);

        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        COOLDOWN, STACKING, ACTIVE
    }

    private String getRuneDisplay(RuneState state, Player player, int value) {
        return switch (state) {
            case COOLDOWN -> "§7⚚ " + getCooldownDisplay(player);
            case ACTIVE -> {
                double remainingSeconds = value / 20.0;
                yield String.format("§e⚚ (%.1fs)", remainingSeconds);
            }
            case STACKING -> {
                if (value == 0) {
                    yield "§6⚚";
                } else {
                    yield "§6⚚ " + value + "/6";
                }
            }
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (isOnCooldown(player)) {
            return getRuneDisplay(RuneState.COOLDOWN, player, 0);
        }
        RuneState state = playerState.getOrDefault(playerUUID, RuneState.STACKING);
        if (state == RuneState.STACKING) {
            UUID lastTargetUUID = lastTarget.getOrDefault(playerUUID, null);
            int currentStacks = 0;
            if (lastTargetUUID != null) {
                currentStacks = getValidStackCount(player, lastTargetUUID);
            }
            return getRuneDisplay(RuneState.STACKING, player, currentStacks);
        }
        if (state == RuneState.ACTIVE) {
            int activeTime = activeState.getOrDefault(playerUUID, 0);
            return getRuneDisplay(RuneState.ACTIVE, player, activeTime);
        }
        return getRuneDisplay(RuneState.STACKING, player, 0);
    }
}